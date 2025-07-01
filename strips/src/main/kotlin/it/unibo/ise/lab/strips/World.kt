package it.unibo.ise.lab.strips

import it.unibo.tuprolog.theory.Theory
import it.unibo.tuprolog.theory.parsing.ClausesReader
import java.net.URL

object World {
    /**
     * 加载指定的 Prolog 世界文件
     * 专门为 DroneWorld 优化，不加载可能冲突的 Strips.pl
     */
    fun load(name: String): Theory {
        val mainFile = "/$name.pl"
        var theory = loadTheoryFromResource(mainFile)
            ?: error("World file '$name.pl' not found in resources")

        if (verbose) {
            println("Loaded main theory from $mainFile")
        }

        // 总是加载Strips.pl作为规划引擎
        loadOptionalTheory("/Strips.pl")?.let { strips ->
            theory += strips
            if (verbose) {
                println("Added Strips.pl to theory")
            }
        }
        
        // 只有在不是 DroneWorld 时才尝试加载额外文件
        if (name != "DroneWorld") {
            loadOptionalTheory("/Utils.pl")?.let { utils ->
                theory += utils
                if (verbose) {
                    println("Added Utils.pl to theory")
                }
            }
        }

        return theory
    }

    /**
     * 加载多个世界文件并合并
     */
    fun loadMultiple(vararg names: String): Theory {
        return names.map { load(it) }.reduce { acc, theory -> acc + theory }
    }

    /**
     * 专门为 Horn 地图加载预定义的测试场景
     */
    fun loadHornTestScenario(): Theory {
        return load("DroneWorld")
    }

    private fun loadTheoryFromResource(resourcePath: String): Theory? {
        return try {
            val resource = World::class.java.getResource(resourcePath) ?: return null
            parseFileAsTheory(resource)
        } catch (e: Exception) {
            println("Warning: Failed to load $resourcePath: ${e.message}")
            null
        }
    }

    private fun loadOptionalTheory(resourcePath: String): Theory? {
        return try {
            loadTheoryFromResource(resourcePath)
        } catch (e: Exception) {
            // 静默忽略可选文件的加载错误
            null
        }
    }

    private fun parseFileAsTheory(file: URL): Theory {
        return file.openStream().use { inputStream ->
            ClausesReader.withDefaultOperators().readTheory(inputStream)
        }
    }

    private val verbose = System.getProperty("world.verbose", "false").toBoolean()
}