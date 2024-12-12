package io.hpkl.gradle.cli

import io.hpkl.gradle.codegen.JavaCodeGenerator
import io.hpkl.gradle.codegen.JavaCodeGeneratorException
import org.pkl.commons.cli.CliCommand
import org.pkl.commons.cli.CliException
import org.pkl.commons.createParentDirectories
import org.pkl.commons.writeString
import org.pkl.core.Closeables
import org.pkl.core.ModuleSource
import java.io.IOException

class CliJavaCodeGenerator(private val options: CliJavaCodeGeneratorOptions)
    : CliCommand(options.base) {

    override fun doRun() {
        val builder = evaluatorBuilder()
        try {
            builder.build().use { evaluator ->
                for (moduleUri in options.base.normalizedSourceModules) {
                    val schema = evaluator.evaluateSchema(ModuleSource.uri(moduleUri))
                    val codeGenerator = JavaCodeGenerator(schema, options.toJavaCodeGeneratorOptions())
                    try {
                        for ((fileName, fileContents) in codeGenerator.output) {
                            val outputFile = options.outputDir.resolve(fileName)
                            try {
                                outputFile.createParentDirectories().writeString(fileContents)
                            } catch (e: IOException) {
                                throw CliException("I/O error writing file `$outputFile`.\nCause: ${e.message}")
                            }
                        }
                    } catch (e: JavaCodeGeneratorException) {
                        throw CliException(e.message!!)
                    }
                }
            }
        } finally {
            Closeables.closeQuietly(builder.moduleKeyFactories)
            Closeables.closeQuietly(builder.resourceReaders)
        }
    }
}