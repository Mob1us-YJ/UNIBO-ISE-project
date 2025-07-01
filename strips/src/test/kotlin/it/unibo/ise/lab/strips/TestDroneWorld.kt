package it.unibo.ise.lab.strips

import it.unibo.tuprolog.core.Clause
import it.unibo.tuprolog.core.Integer
import it.unibo.tuprolog.core.Struct
import it.unibo.tuprolog.core.parsing.parseAsStruct
import it.unibo.tuprolog.solve.Solution
import it.unibo.tuprolog.solve.SolutionFormatter
import it.unibo.tuprolog.solve.Solver
import it.unibo.tuprolog.solve.channel.OutputChannel
import it.unibo.tuprolog.solve.flags.TrackVariables
import it.unibo.tuprolog.theory.Theory
import kotlin.test.Test
import kotlin.test.assertIs

class TestDroneWorld {

    private fun prologPlanner(world: String, maxDepth: Int): Solver =
        Solver.prolog.newBuilder()
            .staticKb(World.load(world))
            .dynamicKb(Theory.of(
                Clause.of(Struct.of("max_depth", Integer.of(maxDepth)))
            ))
            .flag(TrackVariables) { ON }
            .standardOutput(OutputChannel.of { print(it) })
            .standardError(OutputChannel.of { /* silently ignore */ })
            .warnings(OutputChannel.of {
                // 忽略警告而不是抛出异常
                println("Warning: $it")
            })
            .buildMutable()

    private fun testInWorld(world: String, maxDepth: Int = 10, theory: () -> String) {
        val solver = prologPlanner(world, maxDepth)
        val query = theory().trimIndent().parseAsStruct()
        val solution = solver.solveOnce(query)
        assertIs<Solution.Yes>(solution, "Expected a plan but got no solution for query: $query")
        println("DroneWorld Plan: ${SolutionFormatter.withOperators(solver.operators).format(solution)}")
    }

    @Test
    fun testLoadDomainPrintAll() {
        val theory = World.load("DroneWorld")
        println("全部子句：")
        theory.clauses.forEach { println(it) }
    }

    @Test
    fun testMoveSimple() {
        // 测试简单移动：从 warehouse1 到 crossroad1
        testInWorld("DroneWorld", maxDepth = 3) {
            """
            strips(
                [at_drone(drone1,warehouse1), energy(drone1,100)],
                [at_drone(drone1,crossroad1)],
                Plan
            )
            """
        }
    }

    @Test
    fun testPickupSimple() {
        // 测试拾取包裹：在 warehouse1 拾取 pkg1
        testInWorld("DroneWorld", maxDepth = 5) {
            """
            strips(
                [at_drone(drone1,warehouse1), at_package(pkg1,warehouse1), energy(drone1,100)],
                [holding(drone1,pkg1)],
                Plan
            )
            """
        }
    }

    @Test
    fun testDropSimple() {
        // 测试放下包裹：在 houseA 放下 pkg1
        testInWorld("DroneWorld", maxDepth = 5) {
            """
            strips(
                [at_drone(drone1,houseA), holding(drone1,pkg1), energy(drone1,100)],
                [at_package(pkg1,houseA)],
                Plan
            )
            """
        }
    }

    @Test
    fun testRechargeSimple() {
        // 测试充电：从 10 能量充到 60 能量 (10 + 50)
        testInWorld("DroneWorld", maxDepth = 10) {
            """
            strips(
                [at_drone(drone1,base), energy(drone1,10)],
                [energy(drone1,60)],
                Plan
            )
            """
        }
    }

    @Test
    fun testRechargeToMaximum() {
        // 测试充电到最大值：从 40 充电到 90，再到 100
        testInWorld("DroneWorld", maxDepth = 8) {
            """
            strips(
                [at_drone(drone1,base), energy(drone1,30)],
                [energy(drone1,100)],
                Plan
            )
            """
        }
    }

    @Test
    fun testDeliverySimple() {
        // 测试完整配送：warehouse1 拾取 -> 移动到 houseA -> 放下
        testInWorld("DroneWorld", maxDepth = 15) {
            """
            strips(
                [at_drone(drone1,warehouse1), at_package(pkg1,warehouse1), energy(drone1,100)],
                [at_package(pkg1,houseA)],
                Plan
            )
            """
        }
    }

    @Test
    fun testLowEnergyRequiresRechargeBeforeDelivery() {
        // 测试低能量配送：需要先充电再执行配送任务
        testInWorld("DroneWorld", maxDepth = 25) {
            """
            strips(
                [at_drone(drone1,base), at_package(pkg1,warehouse1), energy(drone1,10)],
                [at_package(pkg1,houseA)],
                Plan
            )
            """
        }
    }

    @Test
    fun testComplexDeliveryToHouseB() {
        // 测试到 houseB 的配送（距离更远）
        testInWorld("DroneWorld", maxDepth = 20) {
            """
            strips(
                [at_drone(drone1,warehouse1), at_package(pkg1,warehouse1), energy(drone1,100)],
                [at_package(pkg1,houseB)],
                Plan
            )
            """
        }
    }

    @Test
    fun testMoveToMultipleLocations() {
        // 测试多步移动：warehouse1 -> crossroad1 -> houseA
        testInWorld("DroneWorld", maxDepth = 10) {
            """
            strips(
                [at_drone(drone1,warehouse1), energy(drone1,100)],
                [at_drone(drone1,houseA)],
                Plan
            )
            """
        }
    }

    @Test
    fun testEnergyConstrainedDelivery() {
        // 测试能量约束下的配送（刚好够用的能量）
        testInWorld("DroneWorld", maxDepth = 15) {
            """
            strips(
                [at_drone(drone1,warehouse1), at_package(pkg1,warehouse1), energy(drone1,15)],
                [at_package(pkg1,houseA)],
                Plan
            )
            """
        }
    }

    @Test
    fun testMultiplePackagePositions() {
        // 测试多个包裹的初始状态
        testInWorld("DroneWorld", maxDepth = 20) {
            """
            strips(
                [at_drone(drone1,warehouse1), at_package(pkg1,warehouse1), at_package(pkg2,warehouse1), energy(drone1,100)],
                [at_package(pkg1,houseA)],
                Plan
            )
            """
        }
    }

    @Test
    fun testRechargeFromVeryLowEnergy() {
        // 测试从极低能量开始充电
        testInWorld("DroneWorld", maxDepth = 8) {
            """
            strips(
                [at_drone(drone1,base), energy(drone1,1)],
                [energy(drone1,51)],
                Plan
            )
            """
        }
    }

    @Test
    fun testLongDistanceDelivery() {
        // 测试长距离配送：通过 crossroad2 路径
        testInWorld("DroneWorld", maxDepth = 25) {
            """
            strips(
                [at_drone(drone1,warehouse1), at_package(pkg1,warehouse1), energy(drone1,100)],
                [at_package(pkg1,houseA), at_drone(drone1,crossroad2)],
                Plan
            )
            """
        }
    }

    @Test
    fun testPickupAndMoveWithoutDrop() {
        // 测试拾取包裹后移动但不放下
        testInWorld("DroneWorld", maxDepth = 15) {
            """
            strips(
                [at_drone(drone1,warehouse1), at_package(pkg1,warehouse1), energy(drone1,100)],
                [holding(drone1,pkg1), at_drone(drone1,houseA)],
                Plan
            )
            """
        }
    }

    @Test
    fun testMoveToBaseForRecharge() {
        // 测试移动到基地进行充电
        testInWorld("DroneWorld", maxDepth = 15) {
            """
            strips(
                [at_drone(drone1,warehouse1), energy(drone1,50)],
                [at_drone(drone1,base), energy(drone1,100)],
                Plan
            )
            """
        }
    }
}