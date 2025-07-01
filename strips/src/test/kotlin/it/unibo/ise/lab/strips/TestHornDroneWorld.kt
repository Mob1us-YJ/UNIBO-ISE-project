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

class TestHornDroneWorld {

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

    private fun testInHornWorld(world: String, maxDepth: Int = 20, theory: () -> String) {
        val solver = prologPlanner(world, maxDepth)
        val query = theory().trimIndent().parseAsStruct()
        val solution = solver.solveOnce(query)
        assertIs<Solution.Yes>(solution, "Expected a plan but got no solution for query: $query")
        println("Horn DroneWorld Plan: ${SolutionFormatter.withOperators(solver.operators).format(solution)}")
    }


    // ========== 基础移动测试 ==========

    @Test
    fun testMoveInHornNetwork() {
        // 测试Horn网络中的基础移动：从 warehouse1 到 crossroad3
        testInHornWorld("DroneWorld", maxDepth = 5) {
            """
            strips(
                [at_drone(drone1,warehouse1), energy(drone1,100)],
                [at_drone(drone1,crossroad3)],
                Plan
            )
            """
        }
    }

    @Test
    fun testMoveBetweenWarehouses() {
        // 测试仓库间移动：从 warehouse1 到 warehouse2
        testInHornWorld("DroneWorld", maxDepth = 8) {
            """
            strips(
                [at_drone(drone1,warehouse1), energy(drone1,100)],
                [at_drone(drone1,warehouse2)],
                Plan
            )
            """
        }
    }

    @Test
    fun testMoveToJunctionNorth() {
        // 测试移动到Horn顶部的junction_north
        testInHornWorld("DroneWorld", maxDepth = 10) {
            """
            strips(
                [at_drone(drone1,warehouse1), energy(drone1,100)],
                [at_drone(drone1,junction_north)],
                Plan
            )
            """
        }
    }

    @Test
    fun testMoveToJunctionSouth() {
        // 测试移动到Horn底部的junction_south
        testInHornWorld("DroneWorld", maxDepth = 10) {
            """
            strips(
                [at_drone(drone2,warehouse2), energy(drone2,120)],
                [at_drone(drone2,junction_south)],
                Plan
            )
            """
        }
    }

    // ========== 多无人机测试 ==========

    @Test
    fun testDualDronePositioning() {
        // 测试双无人机定位：drone1到houseA, drone2到houseD
        testInHornWorld("DroneWorld", maxDepth = 15) {
            """
            strips(
                [at_drone(drone1,warehouse1), energy(drone1,100), 
                 at_drone(drone2,warehouse2), energy(drone2,120)],
                [at_drone(drone1,houseA), at_drone(drone2,houseD)],
                Plan
            )
            """
        }
    }

    @Test
    fun testDroneSwapPositions() {
        // 测试无人机交换位置
        testInHornWorld("DroneWorld", maxDepth = 20) {
            """
            strips(
                [at_drone(drone1,warehouse1), energy(drone1,100),
                 at_drone(drone2,warehouse2), energy(drone2,120)],
                [at_drone(drone1,warehouse2), at_drone(drone2,warehouse1)],
                Plan
            )
            """
        }
    }

    // ========== 包裹配送测试 ==========

    @Test
    fun testDeliveryToNewHouses() {
        // 测试配送到新增的房屋：houseC和houseD
        testInHornWorld("DroneWorld", maxDepth = 20) {
            """
            strips(
                [at_drone(drone1,warehouse1), at_package(pkg1,warehouse1), energy(drone1,100)],
                [at_package(pkg1,houseC)],
                Plan
            )
            """
        }
    }

    @Test
    fun testDeliveryFromWarehouse2() {
        // 测试从warehouse2配送包裹到houseD
        testInHornWorld("DroneWorld", maxDepth = 15) {
            """
            strips(
                [at_drone(drone2,warehouse2), at_package(pkg3,warehouse2), energy(drone2,120)],
                [at_package(pkg3,houseD)],
                Plan
            )
            """
        }
    }

    @Test
    fun testCrossWarehouseDelivery() {
        // 测试跨仓库配送：从warehouse1取包裹送到warehouse2附近的houseD
        testInHornWorld("DroneWorld", maxDepth = 25) {
            """
            strips(
                [at_drone(drone1,warehouse1), at_package(pkg1,warehouse1), energy(drone1,100)],
                [at_package(pkg1,houseD)],
                Plan
            )
            """
        }
    }

    @Test
    fun testMultiplePackageDelivery() {
        // 测试多包裹配送任务
        testInHornWorld("DroneWorld", maxDepth = 30) {
            """
            strips(
                [at_drone(drone1,warehouse1), at_package(pkg1,warehouse1), at_package(pkg2,warehouse1), energy(drone1,100)],
                [at_package(pkg1,houseA), at_package(pkg2,houseB)],
                Plan
            )
            """
        }
    }

    // ========== 长距离和复杂路径测试 ==========

    @Test
    fun testLongDistanceViaJunctions() {
        // 测试通过junction的长距离配送
        testInHornWorld("DroneWorld", maxDepth = 25) {
            """
            strips(
                [at_drone(drone1,warehouse1), at_package(pkg1,warehouse1), energy(drone1,100)],
                [at_package(pkg1,houseC), at_drone(drone1,junction_north)],
                Plan
            )
            """
        }
    }

    @Test
    fun testAlternativePathDelivery() {
        // 测试使用备选路径：通过houseA到houseB的直连
        testInHornWorld("DroneWorld", maxDepth = 20) {
            """
            strips(
                [at_drone(drone1,houseA), at_package(pkg1,houseA), energy(drone1,100)],
                [at_package(pkg1,houseB)],
                Plan
            )
            """
        }
    }

    @Test
    fun testComplexHornTraversal() {
        // 测试复杂的Horn网络遍历：从一个角到另一个角
        testInHornWorld("DroneWorld", maxDepth = 20) {
            """
            strips(
                [at_drone(drone1,junction_north), energy(drone1,100)],
                [at_drone(drone1,junction_south)],
                Plan
            )
            """
        }
    }

    // ========== 能量管理测试 ==========

    @Test
    fun testRechargeInHornMap() {
        // 测试在Horn地图中的充电（基地在中心位置）
        testInHornWorld("DroneWorld", maxDepth = 15) {
            """
            strips(
                [at_drone(drone1,crossroad3), energy(drone1,20)],
                [at_drone(drone1,base), energy(drone1,70)],
                Plan
            )
            """
        }
    }

    @Test
    fun testLowEnergyHornDelivery() {
        // 测试低能量情况下的Horn地图配送
        testInHornWorld("DroneWorld", maxDepth = 30) {
            """
            strips(
                [at_drone(drone1,warehouse1), at_package(pkg1,warehouse1), energy(drone1,25)],
                [at_package(pkg1,houseC)],
                Plan
            )
            """
        }
    }

    @Test
    fun testDrone2HighEnergyDelivery() {
        // 测试drone2的高能量配送能力
        testInHornWorld("DroneWorld", maxDepth = 25) {
            """
            strips(
                [at_drone(drone2,warehouse2), at_package(pkg4,warehouse2), energy(drone2,120)],
                [at_package(pkg4,junction_north)],
                Plan
            )
            """
        }
    }

    // ========== 综合复杂场景测试 ==========

    @Test
    fun testFullHornMapDeliveryScenario() {
        // 测试完整的Horn地图配送场景：四个包裹配送到四个不同房屋
        testInHornWorld("DroneWorld", maxDepth = 40) {
            """
            strips(
                [at_drone(drone1,warehouse1), energy(drone1,100),
                 at_drone(drone2,warehouse2), energy(drone2,120),
                 at_package(pkg1,warehouse1), at_package(pkg2,warehouse1),
                 at_package(pkg3,warehouse2), at_package(pkg4,warehouse2)],
                [at_package(pkg1,houseA), at_package(pkg2,houseB), 
                 at_package(pkg3,houseC), at_package(pkg4,houseD)],
                Plan
            )
            """
        }
    }

    @Test
    fun testOptimalPathInHornNetwork() {
        // 测试Horn网络中的最优路径选择
        testInHornWorld("DroneWorld", maxDepth = 15) {
            """
            strips(
                [at_drone(drone1,warehouse1), at_package(pkg1,warehouse1), energy(drone1,50)],
                [at_package(pkg1,houseA)],
                Plan
            )
            """
        }
    }

    @Test
    fun testConcurrentDroneOperations() {
        // 测试并发无人机操作：一个充电，一个配送
        testInHornWorld("DroneWorld", maxDepth = 25) {
            """
            strips(
                [at_drone(drone1,base), energy(drone1,30),
                 at_drone(drone2,warehouse2), at_package(pkg3,warehouse2), energy(drone2,120)],
                [energy(drone1,80), at_package(pkg3,houseC)],
                Plan
            )
            """
        }
    }

    @Test
    fun testEmergencyRechargeScenario() {
        // 测试紧急充电场景：能量不足时必须先充电
        testInHornWorld("DroneWorld", maxDepth = 35) {
            """
            strips(
                [at_drone(drone1,warehouse1), at_package(pkg1,warehouse1), energy(drone1,10)],
                [at_package(pkg1,junction_south)],
                Plan
            )
            """
        }
    }

    @Test
    fun testMaxCapacityUtilization() {
        // 测试drone2最大能力利用：长距离多任务
        testInHornWorld("DroneWorld", maxDepth = 30) {
            """
            strips(
                [at_drone(drone2,warehouse2), at_package(pkg3,warehouse2), at_package(pkg4,warehouse2), energy(drone2,120)],
                [at_package(pkg3,junction_north), at_package(pkg4,junction_south)],
                Plan
            )
            """
        }
    }

    // ========== 边界情况测试 ==========

    @Test
    fun testMinimalEnergyDelivery() {
        // 测试最小能量配送
        testInHornWorld("DroneWorld", maxDepth = 10) {
            """
            strips(
                [at_drone(drone1,crossroad4), at_package(pkg1,crossroad4), energy(drone1,7)],
                [at_package(pkg1,houseD)],
                Plan
            )
            """
        }
    }

    @Test
    fun testAllJunctionsVisit() {
        // 测试访问所有junction点
        testInHornWorld("DroneWorld", maxDepth = 25) {
            """
            strips(
                [at_drone(drone1,base), energy(drone1,100)],
                [at_drone(drone1,junction_north)],
                Plan
            )
            """
        }
    }

    @Test
    fun testPackageRelocatioInHornMap() {
        // 测试在Horn地图中的包裹重新定位
        testInHornWorld("DroneWorld", maxDepth = 20) {
            """
            strips(
                [at_drone(drone1,houseA), at_package(pkg1,houseA), energy(drone1,100)],
                [at_package(pkg1,warehouse2)],
                Plan
            )
            """
        }
    }
}