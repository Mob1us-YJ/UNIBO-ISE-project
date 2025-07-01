package it.unibo.ise.lab.strips.model

import javafx.beans.property.*
import javafx.collections.FXCollections
import javafx.collections.ObservableList

data class Position(val x: Double, val y: Double) {
    override fun toString() = "($x, $y)"
}

data class DroneInfo(
    val id: String,
    var position: Position,
    var energy: Int,
    val maxEnergy: Int,
    var isActive: Boolean = true,
    var currentAction: String = "idle"
)

data class PackageInfo(
    val id: String,
    var position: Position,
    var isPickedUp: Boolean = false,
    var carriedBy: String? = null,
    var targetLocation: String? = null
)

data class LocationInfo(
    val id: String,
    val position: Position,
    val type: LocationType,
    val isBase: Boolean = false
)

enum class LocationType {
    WAREHOUSE, HOUSE, CROSSROAD, JUNCTION, BASE
}

data class PlanStep(
    val stepNumber: Int,
    val action: String,
    val description: String,
    var isCompleted: Boolean = false
)

class DroneSimulationModel {
    // Observable properties for UI binding
    val isRunning = SimpleBooleanProperty(false)
    val isPaused = SimpleBooleanProperty(false)
    val currentStep = SimpleIntegerProperty(0)
    val totalSteps = SimpleIntegerProperty(0)
    val statusMessage = SimpleStringProperty("Ready")
    
    // Observable collections
    val drones: ObservableList<DroneInfo> = FXCollections.observableArrayList()
    val packages: ObservableList<PackageInfo> = FXCollections.observableArrayList()
    val locations: ObservableList<LocationInfo> = FXCollections.observableArrayList()
    val planSteps: ObservableList<PlanStep> = FXCollections.observableArrayList()
    val logMessages: ObservableList<String> = FXCollections.observableArrayList()
    
    // Map dimensions
    val mapWidth = SimpleDoubleProperty(800.0)
    val mapHeight = SimpleDoubleProperty(600.0)
    
    init {
        initializeHornMap()
    }
    
    private fun initializeHornMap() {
        // Initialize Horn map locations based on DroneWorld.pl
        locations.addAll(listOf(
            LocationInfo("warehouse1", Position(100.0, 500.0), LocationType.WAREHOUSE),
            LocationInfo("warehouse2", Position(700.0, 500.0), LocationType.WAREHOUSE),
            LocationInfo("base", Position(400.0, 300.0), LocationType.BASE, true),
            LocationInfo("crossroad1", Position(200.0, 350.0), LocationType.CROSSROAD),
            LocationInfo("crossroad2", Position(600.0, 350.0), LocationType.CROSSROAD),
            LocationInfo("crossroad3", Position(300.0, 200.0), LocationType.CROSSROAD),
            LocationInfo("crossroad4", Position(500.0, 200.0), LocationType.CROSSROAD),
            LocationInfo("houseA", Position(150.0, 100.0), LocationType.HOUSE),
            LocationInfo("houseB", Position(650.0, 100.0), LocationType.HOUSE),
            LocationInfo("houseC", Position(250.0, 50.0), LocationType.HOUSE),
            LocationInfo("houseD", Position(550.0, 50.0), LocationType.HOUSE),
            LocationInfo("junction_north", Position(400.0, 100.0), LocationType.JUNCTION),
            LocationInfo("junction_south", Position(500.0, 450.0), LocationType.JUNCTION)
        ))
        
        // Initialize default drones
        drones.addAll(listOf(
            DroneInfo("drone1", getLocationPosition("warehouse1"), 100, 100),
            DroneInfo("drone2", getLocationPosition("warehouse2"), 120, 120)
        ))
        
        // Initialize default packages
        packages.addAll(listOf(
            PackageInfo("pkg1", getLocationPosition("warehouse1"), false),
            PackageInfo("pkg2", getLocationPosition("warehouse1"), false),
            PackageInfo("pkg3", getLocationPosition("warehouse2"), false),
            PackageInfo("pkg4", getLocationPosition("warehouse2"), false)
        ))
    }
    
    fun getLocationPosition(locationId: String): Position {
        return locations.find { it.id == locationId }?.position ?: Position(0.0, 0.0)
    }
    
    fun updateDronePosition(droneId: String, newPosition: Position) {
        drones.find { it.id == droneId }?.position = newPosition
    }
    
    fun updateDroneEnergy(droneId: String, newEnergy: Int) {
        drones.find { it.id == droneId }?.energy = newEnergy
    }
    
    fun updateDroneAction(droneId: String, action: String) {
        drones.find { it.id == droneId }?.currentAction = action
    }
    
    fun pickupPackage(droneId: String, packageId: String) {
        val pkg = packages.find { it.id == packageId }
        val drone = drones.find { it.id == droneId }
        if (pkg != null && drone != null) {
            pkg.isPickedUp = true
            pkg.carriedBy = droneId
            pkg.position = drone.position
        }
    }
    
    fun dropPackage(droneId: String, packageId: String, location: Position) {
        val pkg = packages.find { it.id == packageId }
        if (pkg != null && pkg.carriedBy == droneId) {
            pkg.isPickedUp = false
            pkg.carriedBy = null
            pkg.position = location
        }
    }
    
    fun addLogMessage(message: String) {
        logMessages.add("[${java.time.LocalTime.now()}] $message")
        // Keep only last 100 messages
        if (logMessages.size > 100) {
            logMessages.removeAt(0)
        }
    }
    
    fun reset() {
        isRunning.set(false)
        isPaused.set(false)
        currentStep.set(0)
        totalSteps.set(0)
        statusMessage.set("Ready")
        planSteps.clear()
        logMessages.clear()
        
        // Reset drones and packages to initial positions
        drones.clear()
        packages.clear()
        initializeHornMap()
    }
} 