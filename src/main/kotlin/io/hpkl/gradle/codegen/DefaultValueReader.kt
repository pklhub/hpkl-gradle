package io.hpkl.gradle.utils

import org.pkl.commons.cli.CliBaseOptions
import org.pkl.commons.cli.CliCommand
import org.pkl.core.Closeables
import org.pkl.core.ModuleSource
import org.pkl.core.PClass

class PropertyDefaultValueReader {

    fun findFefaultValues(options: CliBaseOptions, moduleSource: ModuleSource, pClass: PClass?): Map<String, Any>? {
        val runner = Runner(options, moduleSource, pClass)
        runner.run()
        return runner.properties
    }

    class Runner(options: CliBaseOptions,
                 val moduleSource: ModuleSource,
                 private val pClass: PClass?) : CliCommand(options) {

        var properties: Map<String, Any>? = null

        override fun doRun() {
            val builder = evaluatorBuilder()
            try {
                builder.build().use { evaluator ->
                    properties = evaluator.evaluateExpression(
                        moduleSource,
                        String.format(
                            """
                                import("pkl:reflect").Class(%s).properties.map((k,v) -> Pair(k, v.defaultValue))
                            """.trimIndent(),
                            pClass?.simpleName ?: "module.getClass()"
                        )
                    ) as Map<String, Any>
                }
            } finally {
                Closeables.closeQuietly(builder.moduleKeyFactories)
                Closeables.closeQuietly(builder.resourceReaders)
            }
        }

    }

}