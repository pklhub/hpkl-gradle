package io.hpkl.gradle.cli

import io.hpkl.gradle.codegen.java.JavaCodeGenerator
import io.hpkl.gradle.codegen.java.JavaCodeGeneratorException
import org.pkl.commons.cli.CliCommand
import org.pkl.commons.cli.CliException
import org.pkl.commons.createParentDirectories
import org.pkl.commons.writeString
import org.pkl.core.Closeables
import org.pkl.core.ModuleSource
import org.slf4j.Logger
import java.io.IOException

class CliJavaCodeGenerator(
    private val options: CliJavaCodeGeneratorOptions,
    private val logger: Logger,
) :
    CliCommand(options.base) {

    override fun doRun() {
        val builder = evaluatorBuilder()
        try {
            builder.build().use { evaluator ->
                for (moduleUri in options.base.normalizedSourceModules) {
                    val moduleSource = ModuleSource.uri(moduleUri)
                    val schema = evaluator.evaluateSchema(moduleSource)
                    val codeGenerator = JavaCodeGenerator(schema, moduleSource, options.toJavaCodeGeneratorOptions(), logger)
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
