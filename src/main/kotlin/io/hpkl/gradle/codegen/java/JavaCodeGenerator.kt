package io.hpkl.gradle.codegen.java

import com.palantir.javapoet.AnnotationSpec
import com.palantir.javapoet.ArrayTypeName
import com.palantir.javapoet.ClassName
import com.palantir.javapoet.CodeBlock
import com.palantir.javapoet.FieldSpec
import com.palantir.javapoet.JavaFile
import com.palantir.javapoet.MethodSpec
import com.palantir.javapoet.ParameterSpec
import com.palantir.javapoet.ParameterizedTypeName
import com.palantir.javapoet.TypeName
import com.palantir.javapoet.TypeSpec
import com.palantir.javapoet.WildcardTypeName
import io.hpkl.gradle.codegen.DefaultValueReader
import org.pkl.commons.NameMapper
import org.pkl.core.ModuleSchema
import org.pkl.core.ModuleSource
import org.pkl.core.PClass
import org.pkl.core.PClassInfo
import org.pkl.core.PModule
import org.pkl.core.PNull
import org.pkl.core.PObject
import org.pkl.core.PType
import org.pkl.core.TypeAlias
import org.pkl.core.Version
import org.pkl.core.util.CodeGeneratorUtils
import org.slf4j.Logger
import java.util.Objects
import java.util.regex.Pattern
import javax.lang.model.element.Modifier

class JavaCodeGeneratorException(message: String) : RuntimeException(message)

class JavaCodeGenerator(
    private val schema: ModuleSchema,
    private val moduleSource: ModuleSource,
    private val codegenOptions: JavaCodeGeneratorOptions,
    private val logger: Logger,
) {

    private val nameMapper = NameMapper(codegenOptions.renames)
    private val defaultValueReader = DefaultValueReader(logger)

    private val defaultValueGenerator: JavaValueGenerator = JavaValueGenerator(
        codegenOptions,
        nameMapper,
    )

    private val dataSizeClass = ClassName.bestGuess(codegenOptions.dataSizeClass)

    private val durationClass = ClassName.bestGuess(codegenOptions.durationClass)

    private val durationUnitClass = ClassName.get(
        Class.forName(
            codegenOptions.durationUnitClass,
        ),
    )

    private val dataSizeUnitClass = ClassName.get(
        Class.forName(
            codegenOptions.dataSizeUnitClass,
        ),
    )

    private val pairClass = ClassName.get(
        Class.forName(
            codegenOptions.pairClass,
        ),
    )

    companion object {
        private val OBJECT = ClassName.get(Object::class.java)
        private val STRING = ClassName.get(String::class.java)
        private val COLLECTION = ClassName.get(java.util.Collection::class.java)
        private val LIST = ClassName.get(java.util.List::class.java)
        private val SET = ClassName.get(java.util.Set::class.java)
        private val MAP = ClassName.get(java.util.Map::class.java)
        private val PMODULE = ClassName.get(PModule::class.java)
        private val PCLASS = ClassName.get(PClass::class.java)
        private val PATTERN = ClassName.get(Pattern::class.java)
        private val URI = ClassName.get(java.net.URI::class.java)
        private val VERSION = ClassName.get(Version::class.java)

        private fun toClassName(fqn: String): ClassName {
            val index = fqn.lastIndexOf(".")
            if (index == -1) {
                throw JavaCodeGeneratorException(
                    """
            Annotation `$fqn` is not a valid Java class.
            The name of the annotation should be the canonical Java name of the class, for example, `com.example.Foo`.
          """
                        .trimIndent(),
                )
            }
            val packageName = fqn.substring(0, index)
            val classParts = fqn.substring(index + 1).split('$')
            return if (classParts.size == 1) {
                ClassName.get(packageName, classParts.first())
            } else {
                ClassName.get(packageName, classParts.first(), *classParts.drop(1).toTypedArray())
            }
        }
    }

    val output: Map<String, String>
        get() {
            return mapOf(javaFileName to javaFile)
        }

    private val nonNullAnnotation: AnnotationSpec
        get() {
            val annotation = codegenOptions.nonNullAnnotation
            val className =
                if (annotation == null) {
                    ClassName.get("org.pkl.config.java.mapper", "NonNull")
                } else {
                    toClassName(annotation)
                }
            return AnnotationSpec.builder(className).build()
        }

    private val javaFileName: String
        get() {
            val (packageName, className) = nameMapper.map(schema.moduleName)
            val dirPath = packageName.replace('.', '/')
            return if (dirPath.isEmpty()) {
                "java/$className.java"
            } else {
                "java/$dirPath/$className.java"
            }
        }

    val javaFile: String
        get() {
            if (schema.moduleUri.scheme == "pkl") {
                throw JavaCodeGeneratorException(
                    "Cannot generate Java code for a Pkl standard library module (`${schema.moduleUri}`).",
                )
            }

            val pModuleClass = schema.moduleClass
            val moduleClass = generateTypeSpec(pModuleClass, schema)

            for (pClass in schema.classes.values) {
                if (!codegenOptions.generateAnnotationClasses && isAnnotation(pClass)) {
                    continue
                }
                moduleClass.addType(generateTypeSpec(pClass, schema).build())
            }

            for (typeAlias in schema.typeAliases.values) {
                val stringLiterals = mutableSetOf<String>()
                if (CodeGeneratorUtils.isRepresentableAsEnum(typeAlias.aliasedType, stringLiterals)) {
                    moduleClass.addType(generateEnumTypeSpec(typeAlias, stringLiterals).build())
                }
            }
            // generate static append method for module classes w/o parent class; reuse in subclasses and
            // nested classes
            if (pModuleClass.superclass!!.info == PClassInfo.Module) {
                val modifier =
                    if (pModuleClass.isOpen || pModuleClass.isAbstract) {
                        Modifier.PROTECTED
                    } else {
                        Modifier.PRIVATE
                    }
                moduleClass.addMethod(appendPropertyMethod().addModifiers(modifier).build())
            }

            val (packageName, _) = nameMapper.map(schema.moduleName)

            return JavaFile.builder(packageName, moduleClass.build())
                .indent(codegenOptions.indent)
                .build()
                .toString()
        }

    private fun generateTypeSpec(pClass: PClass, schema: ModuleSchema): TypeSpec.Builder {
        val isModuleClass = pClass == schema.moduleClass
        val javaPoetClassName = pClass.toJavaPoetName()
        val superclass =
            pClass.superclass?.takeIf { it.info != PClassInfo.Typed && it.info != PClassInfo.Module }
        val superProperties =
            superclass?.let { renameIfReservedWord(it.allProperties) }?.filterValues { !it.isHidden }
                ?: mapOf()
        val properties = renameIfReservedWord(pClass.properties).filterValues { !it.isHidden }
        val allProperties = superProperties + properties

        fun PClass.Property.isRegex(): Boolean =
            (this.type as? PType.Class)?.pClass?.info == PClassInfo.Regex

        fun addCtorParameter(
            builder: MethodSpec.Builder,
            propJavaName: String,
            property: PClass.Property,
        ) {
            val paramBuilder = ParameterSpec.builder(property.type.toJavaPoetName(), propJavaName)
            if (namedAnnotationName != null) {
                paramBuilder.addAnnotation(
                    AnnotationSpec.builder(namedAnnotationName)
                        .addMember("value", "\$S", property.simpleName)
                        .build(),
                )
            }
            builder.addParameter(paramBuilder.build())
        }

        fun generateConstructor(isInstantiable: Boolean): MethodSpec {
            val builder =
                MethodSpec.constructorBuilder()
                    // choose most restrictive access modifier possible
                    .addModifiers(
                        when {
                            isInstantiable -> Modifier.PUBLIC
                            pClass.isAbstract || pClass.isOpen -> Modifier.PROTECTED
                            else -> Modifier.PRIVATE
                        },
                    )

            if (superProperties.isNotEmpty()) {
                for ((name, property) in superProperties) {
                    if (properties.containsKey(name)) continue
                    addCtorParameter(builder, name, property)
                }
                // $W inserts space or newline (automatic line wrapping)
                val callArgsStr = superProperties.keys.joinToString(",\$W")
                // use kotlin interpolation rather than javapoet $L interpolation
                // as otherwise the $W won't get processed
                builder.addStatement("super($callArgsStr)")
            }

            for ((name, property) in properties) {
                addCtorParameter(builder, name, property)
                builder.addStatement("this.\$N = \$N", name, name)
            }

            return builder.build()
        }

        fun generateEmptyConstructor(isInstantiable: Boolean): MethodSpec {
            val builder = MethodSpec.constructorBuilder()
                // choose most restrictive access modifier possible
                .addModifiers(
                    when {
                        isInstantiable -> Modifier.PUBLIC
                        pClass.isAbstract || pClass.isOpen -> Modifier.PROTECTED
                        else -> Modifier.PRIVATE
                    },
                )

            return builder.build()
        }

        fun generateEqualsMethod(): MethodSpec {
            val builder =
                MethodSpec.methodBuilder("equals")
                    .addModifiers(Modifier.PUBLIC)
                    .addAnnotation(Override::class.java)
                    .addParameter(Object::class.java, "obj")
                    .returns(Boolean::class.java)
                    .addStatement("if (this == obj) return true")
                    .addStatement("if (obj == null) return false")
                    // generating this.getClass() instead of class literal avoids a SpotBugs warning
                    .addStatement("if (this.getClass() != obj.getClass()) return false")
                    .addStatement("\$T other = (\$T) obj", javaPoetClassName, javaPoetClassName)

            for ((propertyName, property) in allProperties) {
                val accessor = if (property.isRegex()) "\$N.pattern()" else "\$N"
                builder.addStatement(
                    "if (!\$T.equals(this.$accessor, other.$accessor)) return false",
                    Objects::class.java,
                    propertyName,
                    propertyName,
                )
            }

            builder.addStatement("return true")
            return builder.build()
        }

        fun generateHashCodeMethod(): MethodSpec {
            val builder =
                MethodSpec.methodBuilder("hashCode")
                    .addModifiers(Modifier.PUBLIC)
                    .addAnnotation(Override::class.java)
                    .returns(Int::class.java)
                    .addStatement("int result = 1")

            for ((propertyName, property) in allProperties) {
                val accessor = if (property.isRegex()) "this.\$N.pattern()" else "this.\$N"
                builder.addStatement(
                    "result = 31 * result + \$T.hashCode($accessor)",
                    Objects::class.java,
                    propertyName,
                )
            }

            builder.addStatement("return result")
            return builder.build()
        }

        fun generateToStringMethod(): MethodSpec {
            val builder =
                MethodSpec.methodBuilder("toString")
                    .addModifiers(Modifier.PUBLIC)
                    .addAnnotation(Override::class.java)
                    .returns(String::class.java)

            var builderSize = 50
            val appendBuilder = CodeBlock.builder()
            for (propertyName in allProperties.keys) {
                builderSize += 50
                appendBuilder.addStatement(
                    "appendProperty(builder, \$S, this.\$N)",
                    propertyName,
                    propertyName,
                )
            }

            builder
                .addStatement(
                    "\$T builder = new \$T(\$L)",
                    StringBuilder::class.java,
                    StringBuilder::class.java,
                    builderSize,
                )
                .addStatement("builder.append(\$T.class.getSimpleName()).append(\" {\")", javaPoetClassName)
                .addCode(appendBuilder.build())
                // not using $S here because it generates `"\n" + "{"`
                // with a line break in the generated code after `+`
                .addStatement("builder.append(\"\\n}\")")
                .addStatement("return builder.toString()")

            return builder.build()
        }

        // do the minimum work necessary to avoid (most) java compile errors
        // generating idiomatic Javadoc would require parsing doc comments, converting member links,
        // etc.
        fun renderAsJavadoc(docComment: String): String {
            val escaped = docComment.replace("*/", "*&#47;")
            return if (escaped[escaped.length - 1] != '\n') escaped + '\n' else escaped
        }

        fun generateDeprecation(
            annotations: Collection<PObject>,
            hasJavadoc: Boolean,
            addAnnotation: (Class<*>) -> Unit,
            addJavadoc: (String) -> Unit,
        ) {
            annotations
                .firstOrNull { it.classInfo == PClassInfo.Deprecated }
                ?.let { deprecation ->
                    addAnnotation(Deprecated::class.java)
                    if (codegenOptions.generateJavadoc) {
                        (deprecation["message"] as String?)?.let {
                            if (hasJavadoc) {
                                addJavadoc("\n")
                            }
                            addJavadoc(renderAsJavadoc("@deprecated $it"))
                        }
                    }
                }
        }

        fun generateField(propertyName: String, property: PClass.Property, defaultValues: Map<String, Any>?): FieldSpec {
            val builder = FieldSpec.builder(property.type.toJavaPoetName(), propertyName)

            val docComment = property.docComment
            val hasJavadoc =
                docComment != null && codegenOptions.generateJavadoc && !codegenOptions.generateGetters
            if (hasJavadoc) {
                builder.addJavadoc(renderAsJavadoc(docComment!!))
            }

            if (codegenOptions.generateGetters) {
                builder.addModifiers(
                    if (pClass.isAbstract || pClass.isOpen) Modifier.PROTECTED else Modifier.PRIVATE,
                )
            } else {
                generateDeprecation(
                    property.annotations,
                    hasJavadoc,
                    { builder.addAnnotation(it) },
                    { builder.addJavadoc(it) },
                )
                builder.addModifiers(Modifier.PUBLIC)
            }

            if (!codegenOptions.generateSetters) {
                builder.addModifiers(Modifier.FINAL)
            }

            if (codegenOptions.setDefaultValues) {
                defaultValues?.get(propertyName)?.takeIf { it !is PNull }?.let {
                    builder.initializer(defaultValueGenerator.generate(it, property.type))
                }
            }

            return builder.build()
        }

        @Suppress("DuplicatedCode")
        fun generateGetter(
            propertyName: String,
            property: PClass.Property,
            isOverridden: Boolean,
        ): MethodSpec {
            val propertyType = property.type
            val isBooleanProperty =
                propertyType is PType.Class && propertyType.pClass.info == PClassInfo.Boolean
            val methodName =
                (if (isBooleanProperty) "is" else "get") +
                    // can use original name here (property.name rather than propertyName)
                    // because getter name cannot possibly conflict with reserved words
                    property.simpleName.replaceFirstChar { it.titlecaseChar() }

            val builder =
                MethodSpec.methodBuilder(methodName)
                    .addModifiers(Modifier.PUBLIC)
                    .returns(propertyType.toJavaPoetName())
                    .addStatement("return \$N", propertyName)
            if (isOverridden) {
                builder.addAnnotation(Override::class.java)
            }

            val docComment = property.docComment
            val hasJavadoc = docComment != null && codegenOptions.generateJavadoc
            if (hasJavadoc) {
                builder.addJavadoc(renderAsJavadoc(docComment!!))
            }

            generateDeprecation(
                property.annotations,
                hasJavadoc,
                { builder.addAnnotation(it) },
                { builder.addJavadoc(it) },
            )

            return builder.build()
        }

        @Suppress("DuplicatedCode")
        fun generateSetter(
            propertyName: String,
            property: PClass.Property,
            isOverridden: Boolean,
        ): MethodSpec {
            val methodName =
                "set" + property.simpleName.replaceFirstChar { it.titlecaseChar() }

            val builder =
                MethodSpec.methodBuilder(methodName)
                    .addModifiers(Modifier.PUBLIC)
                    .addParameter(property.type.toJavaPoetName(), propertyName)
                    .addStatement("this.\$N = \$N", propertyName, propertyName)
                    .returns(TypeName.VOID)
            if (isOverridden) {
                builder.addAnnotation(Override::class.java)
            }

            val docComment = property.docComment
            val hasJavadoc = docComment != null && codegenOptions.generateJavadoc
            if (hasJavadoc) {
                builder.addJavadoc(renderAsJavadoc(docComment!!))
            }

            generateDeprecation(
                property.annotations,
                hasJavadoc,
                { builder.addAnnotation(it) },
                { builder.addJavadoc(it) },
            )

            return builder.build()
        }

        fun generateWithMethod(propertyName: String, property: PClass.Property): MethodSpec {
            val methodName = "with" + property.simpleName.replaceFirstChar { it.titlecaseChar() }

            val methodBuilder =
                MethodSpec.methodBuilder(methodName)
                    .addModifiers(Modifier.PUBLIC)
                    .addParameter(property.type.toJavaPoetName(), propertyName)
                    .returns(javaPoetClassName)

            generateDeprecation(
                property.annotations,
                false,
                { methodBuilder.addAnnotation(it) },
                { methodBuilder.addJavadoc(it) },
            )

            if (!codegenOptions.generateSetters) {
                val codeBuilder = CodeBlock.builder()
                codeBuilder.add("return new \$T(", javaPoetClassName)
                var firstProperty = true
                for (name in superProperties.keys) {
                    if (name in properties) continue
                    if (firstProperty) {
                        firstProperty = false
                        codeBuilder.add("\$N", name)
                    } else {
                        codeBuilder.add(", \$N", name)
                    }
                }
                for (name in properties.keys) {
                    if (firstProperty) {
                        firstProperty = false
                        codeBuilder.add("\$N", name)
                    } else {
                        codeBuilder.add(", \$N", name)
                    }
                }
                codeBuilder.add(");\n")

                methodBuilder.addCode(codeBuilder.build())
            } else {
                val codeBuilder = CodeBlock.builder()
                codeBuilder.addStatement("this.\$N = \$N", propertyName, propertyName)
                codeBuilder.addStatement("return this")
                methodBuilder.addCode(codeBuilder.build())
            }

            return methodBuilder.build()
        }

        fun generateSpringBootAnnotations(builder: TypeSpec.Builder) {
            if (codegenOptions.generateSpringBootConfig) {
                val configAnnotation = pClass.annotations.firstOrNull {
                    it.classInfo.simpleName == codegenOptions.springConfigAnnotation &&
                        it.properties.containsKey("prefix")
                }

                if (configAnnotation != null) {
                    val prefix = configAnnotation.properties["prefix"]
                    builder.addAnnotation(
                        AnnotationSpec.builder(
                            ClassName.get("org.springframework.boot.context.properties", "ConfigurationProperties"),
                        ).addMember(
                            "prefix",
                            "\$S",
                            prefix,
                        ).build(),
                    )
                }
            }
        }

        @Suppress("DuplicatedCode")
        fun generateClass(): TypeSpec.Builder {
            val defaultValues: Map<String, Any>? =
                if (codegenOptions.setDefaultValues) {
                    defaultValueReader.findDefaultValues(
                        codegenOptions.baseCliBaseOptions,
                        moduleSource,
                        pClass,
                        isModuleClass,
                    )
                } else {
                    null
                }

            val builder =
                TypeSpec.classBuilder(javaPoetClassName.simpleName()).addModifiers(Modifier.PUBLIC)

            // stateless final module classes are non-instantiable by choice
            val isInstantiable =
                !(pClass.isAbstract || (isModuleClass && !pClass.isOpen && allProperties.isEmpty()))

            if (codegenOptions.implementSerializable && isInstantiable) {
                builder.addSuperinterface(java.io.Serializable::class.java)
                builder.addField(generateSerialVersionUIDField())
            }

            val docComment = pClass.docComment
            val hasJavadoc = docComment != null && codegenOptions.generateJavadoc
            if (hasJavadoc) {
                builder.addJavadoc(renderAsJavadoc(docComment!!))
            }

            generateDeprecation(
                pClass.annotations,
                hasJavadoc,
                { builder.addAnnotation(it) },
                { builder.addJavadoc(it) },
            )

            if (!isModuleClass) {
                builder.addModifiers(Modifier.STATIC)
            }

            if (pClass.isAbstract) {
                builder.addModifiers(Modifier.ABSTRACT)
            } else if (!pClass.isOpen) {
                builder.addModifiers(Modifier.FINAL)
            }

            if (codegenOptions.generateSpringBootConfig) {
                generateSpringBootAnnotations(builder)
            }

            if (properties.isNotEmpty()) {
                builder.addMethod(generateConstructor(isInstantiable))
            }

            if (codegenOptions.generateEmptyConstructor) {
                builder.addMethod(generateEmptyConstructor(isInstantiable))
            }

            superclass?.let { builder.superclass(it.toJavaPoetName()) }

            // generate fields, plus getter methods and either setters or `with` methods in alternating
            // order
            // `with` methods also need to be generated for superclass properties so that return type is
            // self type
            for ((name, property) in allProperties) {
                if (name in properties) {
                    builder.addField(generateField(name, property, defaultValues))
                    if (codegenOptions.generateGetters) {
                        val isOverridden = name in superProperties
                        builder.addMethod(generateGetter(name, property, isOverridden))
                    }
                    if (codegenOptions.generateSetters) {
                        val isOverridden = name in superProperties
                        builder.addMethod(generateSetter(name, property, isOverridden))
                    }
                }
                if (isInstantiable) {
                    builder.addMethod(generateWithMethod(name, property))
                }
            }

            if (isInstantiable) {
                builder
                    .addMethod(generateEqualsMethod())
                    .addMethod(generateHashCodeMethod())
                    .addMethod(generateToStringMethod())
            }

            return builder
        }

        return generateClass()
    }

    private fun generateSerialVersionUIDField(): FieldSpec {
        return FieldSpec.builder(Long::class.java, "serialVersionUID", Modifier.PRIVATE)
            .addModifiers(Modifier.STATIC, Modifier.FINAL)
            .initializer("0L")
            .build()
    }

    private fun generateEnumTypeSpec(
        typeAlias: TypeAlias,
        stringLiterals: Set<String>,
    ): TypeSpec.Builder {
        val enumConstantToPklNames =
            stringLiterals
                .groupingBy { literal ->
                    CodeGeneratorUtils.toEnumConstantName(literal)
                        ?: throw JavaCodeGeneratorException(
                            "Cannot generate Java enum class for Pkl type alias `${typeAlias.displayName}` " +
                                "because string literal type \"$literal\" cannot be converted to a valid enum constant name.",
                        )
                }
                .reduce { enumConstantName, firstLiteral, secondLiteral ->
                    throw JavaCodeGeneratorException(
                        "Cannot generate Java enum class for Pkl type alias `${typeAlias.displayName}` " +
                            "because string literal types \"$firstLiteral\" and \"$secondLiteral\" " +
                            "would both be converted to enum constant name `$enumConstantName`.",
                    )
                }

        val builder =
            TypeSpec.enumBuilder(typeAlias.simpleName)
                .addModifiers(Modifier.PUBLIC)
                .addField(String::class.java, "value", Modifier.PRIVATE)
                .addMethod(
                    MethodSpec.constructorBuilder()
                        .addModifiers(Modifier.PRIVATE)
                        .addParameter(String::class.java, "value")
                        .addStatement("this.value = value")
                        .build(),
                )
                .addMethod(
                    MethodSpec.methodBuilder("toString")
                        .addModifiers(Modifier.PUBLIC)
                        .addAnnotation(Override::class.java)
                        .returns(String::class.java)
                        .addStatement("return this.value")
                        .build(),
                )

        for ((enumConstantName, pklName) in enumConstantToPklNames) {
            builder.addEnumConstant(
                enumConstantName,
                TypeSpec.anonymousClassBuilder("\$S", pklName).build(),
            )
        }

        return builder
    }

    private val namedAnnotationName =
        if (codegenOptions.namedAnnotation != null) {
            toClassName(codegenOptions.namedAnnotation)
        } else {
            null
        }

    private fun appendPropertyMethod() =
        MethodSpec.methodBuilder("appendProperty")
            .addModifiers(Modifier.STATIC)
            .addParameter(StringBuilder::class.java, "builder")
            .addParameter(String::class.java, "name")
            .addParameter(Object::class.java, "value")
            .addStatement("builder.append(\"\\n  \").append(name).append(\" = \")")
            .addStatement(
                "\$T lines = \$T.toString(value).split(\"\\n\")",
                ArrayTypeName.of(String::class.java),
                Objects::class.java,
            )
            .addStatement("builder.append(lines[0])")
            .beginControlFlow("for (int i = 1; i < lines.length; i++)")
            .addStatement("builder.append(\"\\n  \").append(lines[i])")
            .endControlFlow()

    private fun PClass.toJavaPoetName(): ClassName {
        val (packageName, moduleClassName) = nameMapper.map(moduleName)
        return if (isModuleClass) {
            ClassName.get(packageName, moduleClassName)
        } else {
            ClassName.get(packageName, moduleClassName, simpleName)
        }
    }

    // generated type is a nested enum class
    private fun TypeAlias.toJavaPoetName(): ClassName {
        val (packageName, moduleClassName) = nameMapper.map(moduleName)
        return ClassName.get(packageName, moduleClassName, simpleName)
    }

    /** Generate `List<? extends Foo>` if `Foo` is `abstract` or `open`, to allow subclassing. */
    private fun PType.toJavaPoetTypeArgumentName(): TypeName {
        val baseName = toJavaPoetName(nullable = false, boxed = true, typeArgument = true)
        return if (this is PType.Class && (pClass.isAbstract || pClass.isOpen)) {
            WildcardTypeName.subtypeOf(baseName)
        } else {
            baseName
        }
    }

    private fun PType.toJavaPoetName(nullable: Boolean = false, boxed: Boolean = false, typeArgument: Boolean = false): TypeName =
        when (this) {
            PType.UNKNOWN -> OBJECT.nullableIf(nullable, typeArgument)
            PType.NOTHING -> TypeName.VOID
            is PType.StringLiteral -> STRING.nullableIf(nullable, typeArgument)
            is PType.Class -> {
                // if in doubt, spell it out
                when (val classInfo = pClass.info) {
                    PClassInfo.Any -> OBJECT
                    PClassInfo.Typed,
                    PClassInfo.Dynamic,
                    -> OBJECT.nullableIf(nullable, typeArgument)

                    PClassInfo.Boolean -> TypeName.BOOLEAN.boxIf(boxed).nullableIf(nullable, typeArgument)
                    PClassInfo.String -> STRING.nullableIf(nullable, typeArgument)
                    // seems more useful to generate `double` than `java.lang.Number`
                    PClassInfo.Number -> TypeName.DOUBLE.boxIf(boxed).nullableIf(nullable, typeArgument)
                    PClassInfo.Int -> TypeName.LONG.boxIf(boxed).nullableIf(nullable, typeArgument)
                    PClassInfo.Float -> TypeName.DOUBLE.boxIf(boxed).nullableIf(nullable, typeArgument)
                    PClassInfo.Duration -> durationClass.nullableIf(nullable, typeArgument)
                    PClassInfo.DataSize -> dataSizeClass.nullableIf(nullable, typeArgument)
                    PClassInfo.Pair ->
                        ParameterizedTypeName.get(
                            pairClass,
                            if (typeArguments.isEmpty()) {
                                OBJECT
                            } else {
                                typeArguments[0].toJavaPoetTypeArgumentName()
                            },
                            if (typeArguments.isEmpty()) {
                                OBJECT
                            } else {
                                typeArguments[1].toJavaPoetTypeArgumentName()
                            },
                        )
                            .nullableIf(nullable, typeArgument)

                    PClassInfo.Collection ->
                        ParameterizedTypeName.get(
                            COLLECTION,
                            if (typeArguments.isEmpty()) {
                                OBJECT
                            } else {
                                typeArguments[0].toJavaPoetTypeArgumentName()
                            },
                        )
                            .nullableIf(nullable, typeArgument)

                    PClassInfo.List,
                    PClassInfo.Listing,
                    -> {
                        ParameterizedTypeName.get(
                            LIST,
                            if (typeArguments.isEmpty()) {
                                OBJECT
                            } else {
                                typeArguments[0].toJavaPoetTypeArgumentName()
                            },
                        )
                            .nullableIf(nullable, typeArgument)
                    }

                    PClassInfo.Set ->
                        ParameterizedTypeName.get(
                            SET,
                            if (typeArguments.isEmpty()) {
                                OBJECT
                            } else {
                                typeArguments[0].toJavaPoetTypeArgumentName()
                            },
                        )
                            .nullableIf(nullable, typeArgument)

                    PClassInfo.Map,
                    PClassInfo.Mapping,
                    ->
                        ParameterizedTypeName.get(
                            MAP,
                            if (typeArguments.isEmpty()) {
                                OBJECT
                            } else {
                                typeArguments[0].toJavaPoetTypeArgumentName()
                            },
                            if (typeArguments.isEmpty()) {
                                OBJECT
                            } else {
                                typeArguments[1].toJavaPoetTypeArgumentName()
                            },
                        )
                            .nullableIf(nullable, typeArgument)

                    PClassInfo.Module -> PMODULE.nullableIf(nullable, typeArgument)
                    PClassInfo.Class -> PCLASS.nullableIf(nullable, typeArgument)
                    PClassInfo.Regex -> PATTERN.nullableIf(nullable, typeArgument)
                    PClassInfo.Version -> VERSION.nullableIf(nullable, typeArgument)
                    else ->
                        when {
                            !classInfo.isStandardLibraryClass -> pClass.toJavaPoetName().nullableIf(nullable, typeArgument)
                            else ->
                                throw JavaCodeGeneratorException(
                                    "Standard library class `${pClass.qualifiedName}` is not supported by Java code generator. " +
                                        "If you think this is an omission, please let us know.",
                                )
                        }
                }
            }

            is PType.Nullable -> baseType.toJavaPoetName(nullable = true, boxed = true, typeArgument)
            is PType.Constrained -> baseType.toJavaPoetName(nullable = nullable, boxed = boxed, typeArgument)
            is PType.Alias ->
                when (typeAlias.qualifiedName) {
                    "pkl.base#NonNull" -> OBJECT.nullableIf(nullable, typeArgument)
                    "pkl.base#Int8" -> TypeName.BYTE.boxIf(boxed).nullableIf(nullable, typeArgument)
                    "pkl.base#Int16",
                    "pkl.base#UInt8",
                    -> TypeName.SHORT.boxIf(boxed).nullableIf(nullable, typeArgument)

                    "pkl.base#Int32",
                    "pkl.base#UInt16",
                    -> TypeName.INT.boxIf(boxed).nullableIf(nullable, typeArgument)

                    "pkl.base#UInt",
                    "pkl.base#UInt32",
                    -> TypeName.LONG.boxIf(boxed).nullableIf(nullable, typeArgument)

                    "pkl.base#DurationUnit" -> durationUnitClass.nullableIf(nullable, typeArgument)
                    "pkl.base#DataSizeUnit" -> dataSizeUnitClass.nullableIf(nullable, typeArgument)
                    "pkl.base#Uri" -> URI.nullableIf(nullable, typeArgument)
                    else -> {
                        if (CodeGeneratorUtils.isRepresentableAsEnum(aliasedType, null)) {
                            if (typeAlias.isStandardLibraryMember) {
                                throw JavaCodeGeneratorException(
                                    "Standard library typealias `${typeAlias.qualifiedName}` is not supported by Java code generator. " +
                                        "If you think this is an omission, please let us know.",
                                )
                            } else {
                                // reference generated enum class
                                typeAlias.toJavaPoetName().nullableIf(nullable, typeArgument)
                            }
                        } else {
                            // inline type alias
                            aliasedType.toJavaPoetName(nullable, typeArgument)
                        }
                    }
                }

            is PType.Function ->
                throw JavaCodeGeneratorException(
                    "Pkl function types are not supported by the Java code generator.",
                )

            is PType.Union ->
                if (CodeGeneratorUtils.isRepresentableAsString(this)) {
                    STRING.nullableIf(nullable, typeArgument)
                } else {
                    throw JavaCodeGeneratorException(
                        "Pkl union types are not supported by the Java code generator.",
                    )
                }

            else ->
                // should never encounter PType.TypeVariableNode because it can only occur in stdlib classes
                throw AssertionError("Encountered unexpected PType subclass: $this")
        }

    private fun TypeName.nullableIf(isNullable: Boolean, typeArgument: Boolean): TypeName =
        if (typeArgument) {
            box()
        } else if (isPrimitive && isNullable) {
            box()
        } else if (isPrimitive || isNullable) this else annotated(nonNullAnnotation)

    private fun TypeName.boxIf(shouldBox: Boolean): TypeName = if (shouldBox) box() else this

    private fun <T> renameIfReservedWord(map: Map<String, T>): Map<String, T> {
        return map.mapKeys { (key, _) ->
            if (key in javaReservedWords) {
                generateSequence("_$key") { "_$it" }.first { it !in map.keys }
            } else {
                key
            }
        }
    }

    private fun isAnnotation(pClass: PClass): Boolean {
        var clazz: PClass? = pClass
        while (clazz != null) {
            val (packageName, _) = nameMapper.map(clazz.moduleName)
            if (packageName == "pkl" && clazz.simpleName == "Annotation") {
                return true
            }
            clazz = clazz.superclass
        }
        return false
    }
}

internal val javaReservedWords =
    setOf(
        "_", // java 9+
        "abstract",
        "assert",
        "boolean",
        "break",
        "byte",
        "case",
        "catch",
        "char",
        "class",
        "const",
        "continue",
        "default",
        "double",
        "do",
        "else",
        "enum",
        "extends",
        "false",
        "final",
        "finally",
        "float",
        "for",
        "goto",
        "if",
        "implements",
        "import",
        "instanceof",
        "int",
        "interface",
        "long",
        "native",
        "new",
        "null",
        "package",
        "private",
        "protected",
        "public",
        "return",
        "short",
        "static",
        "strictfp",
        "super",
        "switch",
        "synchronized",
        "this",
        "throw",
        "throws",
        "transient",
        "true",
        "try",
        "void",
        "volatile",
        "while",
    )
