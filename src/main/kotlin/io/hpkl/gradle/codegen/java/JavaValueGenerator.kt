package io.hpkl.gradle.codegen.java

import com.palantir.javapoet.ClassName
import com.palantir.javapoet.CodeBlock
import org.pkl.commons.NameMapper
import org.pkl.core.*
import org.pkl.core.util.CodeGeneratorUtils
import java.util.*

class JavaValueGenerator(
    private val options: JavaCodeGeneratorOptions,
    private val nameMapper: NameMapper
) {

    fun generate(value: Any, type: PType): CodeBlock {
        return when (value) {
            is PNull -> return CodeBlock.of("null")
            is String -> when(type) {
                is PType.Alias -> {
                    if (CodeGeneratorUtils.isRepresentableAsEnum(type, null)) {
                        val className = type.typeAlias.toJavaPoetName()
                        val builder = CodeBlock.builder()
                        builder.add(className.canonicalName())
                        builder.add("."+value.toString().uppercase(Locale.getDefault()))
                        return builder.build()
                    }
                    return CodeBlock.of("\"${value}\"")
                }
                is PType.StringLiteral -> CodeBlock.of("\"${value}\"")
                else -> CodeBlock.of("\"${value}\"")
            }
            is Boolean -> CodeBlock.of("$value")
            is Int -> CodeBlock.of("$value")
            is Float -> CodeBlock.of("$value")
            is Double -> CodeBlock.of("$value")
            is Long -> CodeBlock.of("$value")
            is List<*> -> generateList(value, type)
            is Map<*, *> -> generateMap(value, type)
            is Duration -> generateDuration(value)
            is DataSize -> generateDataSize(value)
            is PObject -> generateObject(value, type as PType.Class)
            else -> return CodeBlock.of("null")
        }
    }

    private fun generateList(value: List<*>, type: PType): CodeBlock {
        val argumentType = if (type.typeArguments.isNotEmpty())
            type.typeArguments[0] else null

        val transformer = argumentTransformer(argumentType)

        val builder = CodeBlock.builder()
        builder.add("List.of(")
        value.filterNotNull().forEachIndexed { index, v ->
            if (index > 0) {
                builder.add(",")
            }
            builder.add(transformer(v))
        }
        builder.add(")")
        return builder.build()
    }

    private fun generateDuration(value: Duration): CodeBlock {
        return when (options.durationClass) {
            null, "org.pkl.core.Duration" ->
                CodeBlock.of("new Duration(${value.value}, DurationUnit.${value.unit.name})")

            "java.time.Duration" -> CodeBlock.of("Duration.parse(\"${value.toIsoString()}\")")
            else -> if (options.durationClassConverter != null) {
                CodeBlock.of("${options.durationClassConverter}(${value.toIsoString()})")
            } else {
                CodeBlock.of("null")
            }
        }
    }

    private fun generateDataSize(value: DataSize): CodeBlock {
        return when (options.dataSizeClass) {
            null, "org.pkl.core.DataSize" ->
                CodeBlock.of("new DataSize(${value.value}, DataSizeUnit.${value.unit.name})")

            else -> if (options.dataSizeConverter != null) {
                CodeBlock.of("${options.dataSizeConverter}(${value.inWholeBytes()})")
            } else {
                CodeBlock.of("null")
            }
        }
    }

    private fun generateMap(value: Map<*, *>, type: PType): CodeBlock {
        val keyArgumentType = if (type.typeArguments.isNotEmpty())
            type.typeArguments[0] else null
        val valueArgumentType = if (type.typeArguments.isNotEmpty() && type.typeArguments.size > 1)
            type.typeArguments[1] else null


        val keyTransformer = argumentTransformer(keyArgumentType)
        val valueTransformer = argumentTransformer(valueArgumentType)
        val entryTransformer = { it : Map.Entry<Any?, Any?> ->
            val builder = CodeBlock.builder()
            builder.add(keyTransformer(it.key))
            builder.add(",")
            builder.add(valueTransformer(it.value))
            builder.build()
        }

        val builder = CodeBlock.builder()
        builder.add("Map.of(")
        value.entries.forEachIndexed { index, v ->
            if (index > 0) {
                builder.add(",")
            }
            builder.add(entryTransformer(v))
        }
        builder.add(")")
        return builder.build()
    }

    private fun generateObject(value: PObject, type: PType.Class): CodeBlock {
        val className = type.pClass.toJavaPoetName()

        val codeBuilder = CodeBlock.builder()
        codeBuilder.add("new \$T(", className)

        codeBuilder.add(
            type.pClass.allProperties.map { (k, p) ->
                value.properties[k]?.let {
                    generate(it, p.type)
                } ?: CodeBlock.of("null")
            }.joinToString(", ")
        )

        codeBuilder.add(")")

        return codeBuilder.build()
    }

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

    private fun argumentTransformer(argumentType: PType?): (Any?) -> CodeBlock {
        return nullable(when (argumentType) {
            is PType.Class -> when (argumentType.pClass.info) {
                PClassInfo.Typed -> { it -> generateObject(it as PObject, argumentType)}
                PClassInfo.String -> { it -> CodeBlock.of("\"$it\"") }
                PClassInfo.Duration -> { it -> generateDuration(it as Duration) }
                PClassInfo.DataSize -> { it -> generateDataSize(it as DataSize) }
                PClassInfo.List,
                PClassInfo.Listing -> { it -> generateList(it as List<*>, argumentType) }
                PClassInfo.Int -> { it -> CodeBlock.of(it.toString() + "L") }
                PClassInfo.Float,
                PClassInfo.Boolean -> { it -> CodeBlock.of(it.toString()) }
                else -> {
                    { generateObject(it as PObject, argumentType) }
                }
            }

            is PType.Alias ->
                when (argumentType.typeAlias.qualifiedName) {
                    "pkl.base#Int8" -> { it -> CodeBlock.of("(byte)" + it.toString()) }
                    "pkl.base#Int16",
                    "pkl.base#UInt8" -> { it -> CodeBlock.of("(short)" + it.toString()) }

                    "pkl.base#Int32",
                    "pkl.base#UInt16" -> { it -> CodeBlock.of(it.toString()) }

                    "pkl.base#UInt",
                    "pkl.base#UInt32" -> { it -> CodeBlock.of(it.toString() + "L") }

                    else -> { it -> CodeBlock.of(it.toString()) }
                }
            else -> { it -> CodeBlock.of(it.toString()) }
        })
    }

    private fun nullable(transformer: (Any?) -> CodeBlock): (Any?) -> CodeBlock = { v ->
        if (v != null) {
            transformer(v)
        } else {
            CodeBlock.of("null")
        }
    }
}