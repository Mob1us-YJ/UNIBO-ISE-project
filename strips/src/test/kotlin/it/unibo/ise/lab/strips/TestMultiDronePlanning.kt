package it.unibo.ise.lab.strips

import it.unibo.ise.lab.strips.DronePlanner
import it.unibo.ise.lab.strips.World
import kotlin.test.Test
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class TestMultiDronePlanning {

    @Test
    fun testMultiplePackagesScenario() {
        val world = World.load("DroneWorld")
        val planner = DronePlanner(world)
        
        // Multiple Packages scenario - similar to GUI setup
        val initialState = "[at_drone(drone1,warehouse1), energy(drone1,100), at_drone(drone2,warehouse2), energy(drone2,120), at_package(pkg1,warehouse1), at_package(pkg2,warehouse2)]"
        val goalState = "[at_package(pkg1,houseA), at_package(pkg2,houseB)]"
        
        val plan = planner.plan(initialState, goalState, 25)
        
        assertNotNull(plan, "Plan should not be null")
        assertTrue(plan!!.isNotEmpty(), "Plan should not be empty")
        
        // Check that both drones are involved in the plan
        val drone1Actions = plan.filter { it.contains("drone1") }
        val drone2Actions = plan.filter { it.contains("drone2") }
        
        assertTrue(drone1Actions.isNotEmpty(), "Drone1 should have actions")
        assertTrue(drone2Actions.isNotEmpty(), "Drone2 should have actions")
        
        // Verify the specific actions for multi-drone coordination
        assertTrue(plan.any { it.contains("pickup(drone1, pkg1, warehouse1)") }, 
                  "Drone1 should pickup pkg1 from warehouse1")
        assertTrue(plan.any { it.contains("pickup(drone2, pkg2, warehouse2)") }, 
                  "Drone2 should pickup pkg2 from warehouse2")
        assertTrue(plan.any { it.contains("drop(drone1, pkg1, houseA)") }, 
                  "Drone1 should drop pkg1 at houseA")
        assertTrue(plan.any { it.contains("drop(drone2, pkg2, houseB)") }, 
                  "Drone2 should drop pkg2 at houseB")
        
        println("Multi-drone plan generated successfully:")
        plan.forEachIndexed { index, action ->
            println("${index + 1}. $action")
        }
    }
    
    @Test
    fun testSingleDroneFallback() {
        val world = World.load("DroneWorld")
        val planner = DronePlanner(world)
        
        // Test scenario with only one drone
        val initialState = "[at_drone(drone1,warehouse1), energy(drone1,100), at_package(pkg1,warehouse1), at_package(pkg2,warehouse1)]"
        val goalState = "[at_package(pkg1,houseA), at_package(pkg2,houseB)]"
        
        val plan = planner.plan(initialState, goalState, 25)
        
        assertNotNull(plan, "Plan should not be null")
        assertTrue(plan!!.isNotEmpty(), "Plan should not be empty")
        
        // Check that only drone1 is involved 
        val drone1Actions = plan.filter { it.contains("drone1") }
        val drone2Actions = plan.filter { it.contains("drone2") }
        
        assertTrue(drone1Actions.isNotEmpty(), "Drone1 should have actions")
        assertTrue(drone2Actions.isEmpty(), "Drone2 should not have actions in single-drone scenario")
        
        println("Single-drone fallback plan:")
        plan.forEachIndexed { index, action ->
            println("${index + 1}. $action")
        }
    }
} 