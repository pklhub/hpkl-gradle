package io.hpkl.gradle.codegen

import org.pkl.commons.cli.CliBaseOptions
import org.pkl.commons.cli.CliCommand
import org.pkl.core.Closeables
import org.pkl.core.ModuleSource
import org.pkl.core.PClass
import org.slf4j.Logger

class DefaultValueReader(private val logger: Logger) {

    fun findDefaultValues(
        options: CliBaseOptions,
        moduleSource: ModuleSource,
        pClass: PClass,
        isModuleClass: Boolean,
    ): Map<String, Any>? {
        val runner = Runner(options, moduleSource, pClass, isModuleClass, logger)
        runner.run()
        return runner.properties
    }

    class Runner(
        options: CliBaseOptions,
        val moduleSource: ModuleSource,
        private val pClass: PClass,
        private val isModuleClass: Boolean,
        private val logger: Logger,
    ) : CliCommand(options) {

        var properties: Map<String, Any>? = null

        override fun doRun() {
            val builder = evaluatorBuilder()
            try {
                builder.build().use { evaluator ->
                    try {
                        properties = evaluator.evaluateExpression(
                            moduleSource,
                            if (pClass.isModuleClass) {
                                String.format(
                                    """
                                import("pkl:reflect").Class(module.getClass())
                                    .properties.map((k,v) -> Pair(k, v.defaultValue))
                                    """.trimIndent(),
                                )
                            } else {
                                String.format(
                                    """
                                import("pkl:reflect").Class(%s).properties.map((k,v) -> Pair(k, v.defaultValue))
                                    """.trimIndent(),
                                    pClass.simpleName,
                                )
                            },
                        ) as Map<String, Any>
                    } catch (t: Throwable) {
                        logger.error(t.message, t)
                        // TODO: Skip evaluate errors
                    }
                }
            } finally {
                Closeables.closeQuietly(builder.moduleKeyFactories)
                Closeables.closeQuietly(builder.resourceReaders)
            }
        }
    }
}
