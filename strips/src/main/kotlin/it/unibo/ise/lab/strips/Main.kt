package it.unibo.ise.lab.strips

import it.unibo.tuprolog.core.Integer
import it.unibo.tuprolog.core.Struct
import it.unibo.tuprolog.core.operators.OperatorSet
import it.unibo.tuprolog.core.parsing.TermParser
import it.unibo.tuprolog.solve.*
import it.unibo.tuprolog.solve.channel.OutputChannel
import it.unibo.tuprolog.solve.exception.Warning
import it.unibo.tuprolog.solve.flags.TrackVariables

object Main {
    private var verbose = false
    private var initialState: String? = null
    private var goal: String? = null
    private var worldFile: String? = null
    private var maxDepth: String? = "15" // 增加默认深度以处理复杂Horn地图
    private var testMode = false
    private var showMap = false
    private var findPath: Pair<String, String>? = null
    private var guiMode = false

    @JvmStatic
    fun main(args: Array<String>) {
        parseArgs(args)

        val planner = DronePlanner(worldFile!!)

        when {
            guiMode -> executeGuiMode()
            testMode -> executeTestMode(planner)
            showMap -> executeShowMapMode(planner)
            findPath != null -> executeFindPathMode(planner)
            initialState != null && goal != null -> executePlanningMode(planner)
            else -> executeInteractiveMode(planner)
        }
    }

    private fun executeGuiMode() {
        println("Starting GUI mode...")
        try {
            it.unibo.ise.lab.strips.view.GuiLauncher.launchGui()
        } catch (e: Exception) {
            println("Error starting GUI: ${e.message}")
            e.printStackTrace()
        }
    }

    private fun parseArgs(args: Array<String>) {
        verbose = hasFlag(args, "-v", "--verbose")
        initialState = getArg(args, initialState, "-is", "--initialState")
        goal = getArg(args, goal, "-g", "--goal")
        worldFile = getArg(args, worldFile, "-w", "--world")
            ?: error("Missing world file. Use -w or --world to specify the world file.")
        maxDepth = getArg(args, maxDepth, "-md", "--maxDepth")
        testMode = hasFlag(args, "-t", "--test")
        showMap = hasFlag(args, "-m", "--map")
        guiMode = hasFlag(args, "-gui", "--gui")

        // 解析查找路径参数
        val pathArg = getArg(args, null, "-p", "--path")
        if (pathArg != null) {
            val parts = pathArg.split(",")
            if (parts.size == 2) {
                findPath = parts[0].trim() to parts[1].trim()
            } else {
                error("Path argument should be in format: start,end")
            }
        }
    }

    private fun executeTestMode(planner: DronePlanner) {
        println("Running Horn delivery test...")
        val result = planner.testHornDelivery()
        if (result != null) {
            result.forEach { println(it) }
        } else {
            println("Test failed.")
        }
    }

    private fun executeShowMapMode(planner: DronePlanner) {
        println("Displaying Horn map network...")
        val result = planner.showHornMap()
        result.forEach { println(it) }
    }

    private fun executeFindPathMode(planner: DronePlanner) {
        val (start, end) = findPath!!
        println("Finding all paths from $start to $end...")
        val paths = planner.findAllPaths(start, end)
        if (paths != null && paths.isNotEmpty()) {
            println("Found ${paths.size} path(s):")
            paths.forEachIndexed { index, path ->
                println("Path ${index + 1}: ${path.joinToString(" -> ")}")
            }
        } else {
            println("No paths found from $start to $end.")
        }
    }

    private fun executePlanningMode(planner: DronePlanner) {
        println("Planning from '$initialState' to '$goal'...")
        val plan = planner.plan(initialState!!, goal!!)

        if (plan != null) {
            println("Plan found:")
            plan.forEachIndexed { index, action ->
                println("${index + 1}. $action")
            }
        } else {
            println("No plan found.")
        }
    }

    private fun executeInteractiveMode(planner: DronePlanner) {
        println("Interactive Drone Planner for Horn Map")
        println("Available commands:")
        println("  strips([initial_state], [goals], Plan). - Find a plan")
        println("  test_horn_delivery. - Run Horn delivery test")
        println("  show_horn_map. - Display map network")
        println("  find_all_paths(start, end, Paths). - Find all paths")
        println("  init_horn_world. - Initialize Horn world")
        println("Type queries or press Ctrl+C to exit.")


        val agent = setupInteractiveSolver(planner.getSolver())
        agent.assertA(Struct.of("max_depth", Integer.of(maxDepth!!)))

        while (true) {
            print("?- ")
            val line = readLine() ?: break

            if (line.trim().isEmpty()) continue
            if (line.trim().lowercase() in listOf("quit", "exit", "bye")) break

            try {
                val goalTerm = TermParser.withOperators(agent.operators).parseStruct(line)
                agent.solve(goalTerm).forEach { solution ->
                    printSolution(agent.operators, solution)
                    print("Press Enter for next solution or 'q' to quit: ")
                    val input = readLine()
                    if (input?.lowercase() == "q") return@forEach
                }
            } catch (e: Exception) {
                println("Error parsing query: ${e.message}")
            }
        }
        println("Goodbye!")
    }

    private fun setupInteractiveSolver(baseSolver: MutableSolver): MutableSolver {
        return Solver.prolog.newBuilder()
            .staticKb(baseSolver.staticKb)
            .dynamicKb(baseSolver.dynamicKb)
            .flag(TrackVariables) { ON }
            .standardOutput(OutputChannel.of(this::printOutput))
            .standardError(OutputChannel.of(this::printError))
            .warnings(OutputChannel.of(this::printWarning))
            .buildMutable()
    }

    private fun printSolution(operators: OperatorSet, current: Solution) {
        println(SolutionFormatter.withOperators(operators).format(current))
    }

    private fun printOutput(message: String) {
        if (verbose) print(message)
    }

    private fun printError(message: String) {
        if (verbose) System.err.print(message)
    }

    private fun printWarning(warning: Warning) {
        System.err.println("Warning: ${warning.message}")
    }

    private fun hasFlag(args: Array<String>, vararg flags: String): Boolean =
        args.any { it in flags }

    private fun getArg(args: Array<String>, defaultArg: String?, vararg keys: String): String? {
        for (i in args.indices) {
            if (args[i] in keys && i + 1 < args.size) {
                return args[i + 1]
            }
        }
        return defaultArg
    }
}