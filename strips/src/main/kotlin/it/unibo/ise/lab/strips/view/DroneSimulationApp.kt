package it.unibo.ise.lab.strips.view

import it.unibo.ise.lab.strips.DronePlanner
import it.unibo.ise.lab.strips.model.*
import javafx.application.Application
import javafx.application.Platform
import javafx.concurrent.Task
import javafx.geometry.Insets
import javafx.geometry.Orientation
import javafx.geometry.Pos
import javafx.scene.Scene
import javafx.scene.control.*
import javafx.scene.layout.*
import javafx.stage.Stage
import kotlinx.coroutines.*
import javafx.collections.ListChangeListener
import javafx.scene.Parent

class DroneSimulationApp : Application() {
    private lateinit var model: DroneSimulationModel
    private lateinit var mapPanel: MapPanel
    private lateinit var planner: DronePlanner
    
    // UI Components
    private lateinit var startButton: Button
    private lateinit var pauseButton: Button
    private lateinit var resetButton: Button
    private lateinit var stepButton: Button
    private lateinit var statusLabel: Label
    private lateinit var planListView: ListView<String>
    private lateinit var logTextArea: TextArea
    private lateinit var progressBar: ProgressBar
    
    // Scenario controls
    private lateinit var initialStateField: TextArea
    private lateinit var goalStateField: TextArea
    private lateinit var maxDepthSpinner: Spinner<Int>
    
    private var simulationJob: Job? = null
    private val simulationScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    
    override fun start(primaryStage: Stage) {
        model = DroneSimulationModel()
        planner = DronePlanner("DroneWorld")
        
        primaryStage.title = "Drone Delivery Planning Simulation System"
        primaryStage.scene = Scene(createMainLayout(), 1200.0, 800.0)
        primaryStage.show()
        
        setupEventHandlers()
        updateUIState()
        
        // Load default scenario
        loadScenario("Simple Move")
    }
    
    private fun createMainLayout(): Parent {
        val mainPane = BorderPane()
        
        // Top toolbar
        mainPane.top = createToolbar()
        
        // Center: Map and controls
        val centerSplit = SplitPane()
        centerSplit.orientation = Orientation.HORIZONTAL
        centerSplit.items.addAll(createMapPane(), createControlPane())
        centerSplit.setDividerPositions(0.6)
        mainPane.center = centerSplit
        
        // Bottom: Status and progress
        mainPane.bottom = createStatusPane()
        
        return mainPane
    }
    
    private fun createToolbar(): ToolBar {
        startButton = Button("Start")
        pauseButton = Button("Pause")
        resetButton = Button("Reset")
        stepButton = Button("Step")
        
        val separator1 = Separator()
        val separator2 = Separator()
        
        val scenarioLabel = Label("Preset Scenarios:")
        val scenarioCombo = ComboBox<String>().apply {
            items.addAll(
                "Simple Move",
                "Simple Delivery", 
                "Multiple Packages",
                "Horn Map Test",
                "Low Energy Scenario",
                "Custom"
            )
            value = "Simple Move"
            
            // Add scenario switching listener
            setOnAction {
                loadScenario(value)
            }
        }
        
        return ToolBar(
            startButton, pauseButton, stepButton, separator1,
            resetButton, separator2,
            scenarioLabel, scenarioCombo
        )
    }
    
    private fun createMapPane(): Pane {
        val mapContainer = VBox()
        mapContainer.spacing = 5.0
        mapContainer.padding = Insets(10.0)
        
        val mapTitle = Label("Horn Map Visualization")
        mapTitle.style = "-fx-font-size: 14px; -fx-font-weight: bold;"
        
        mapPanel = MapPanel(model)
        val mapScrollPane = ScrollPane(mapPanel)
        mapScrollPane.isFitToWidth = true
        mapScrollPane.isFitToHeight = true
        
        VBox.setVgrow(mapScrollPane, Priority.ALWAYS)
        mapContainer.children.addAll(mapTitle, mapScrollPane)
        
        return mapContainer
    }
    
    private fun createControlPane(): Pane {
        val controlPane = VBox()
        controlPane.spacing = 10.0
        controlPane.padding = Insets(10.0)
        controlPane.prefWidth = 400.0
        
        // Scenario configuration
        val scenarioGroup = createScenarioConfigGroup()
        
        // Plan display
        val planGroup = createPlanDisplayGroup()
        
        // Drone status
        val droneGroup = createDroneStatusGroup()
        
        // Log display
        val logGroup = createLogDisplayGroup()
        
        controlPane.children.addAll(scenarioGroup, planGroup, droneGroup, logGroup)
        VBox.setVgrow(logGroup, Priority.ALWAYS)
        
        return controlPane
    }
    
    private fun createScenarioConfigGroup(): TitledPane {
        val content = VBox()
        content.spacing = 5.0
        
        // Initial state
        val initialLabel = Label("Initial State:")
        initialStateField = TextArea()
        initialStateField.prefRowCount = 3
        // Initial text will be set through loadScenario
        
        // Goal state
        val goalLabel = Label("Goal State:")
        goalStateField = TextArea()
        goalStateField.prefRowCount = 2
        // Goal text will be set through loadScenario
        
        // Max depth
        val depthLabel = Label("Max Search Depth:")
        maxDepthSpinner = Spinner(5, 50, 25)
        maxDepthSpinner.isEditable = true
        
        val planButton = Button("Generate Plan")
        planButton.setOnAction { generatePlan() }
        
        content.children.addAll(
            initialLabel, initialStateField,
            goalLabel, goalStateField,
            depthLabel, maxDepthSpinner,
            planButton
        )
        
        return TitledPane("Scenario Configuration", content)
    }
    
    private fun createPlanDisplayGroup(): TitledPane {
        val content = VBox()
        content.spacing = 5.0
        
        planListView = ListView()
        planListView.prefHeight = 150.0
        
        val planControlsHBox = HBox()
        planControlsHBox.spacing = 5.0
        planControlsHBox.alignment = Pos.CENTER_LEFT
        
        val executeButton = Button("Execute Plan")
        executeButton.setOnAction { executePlan() }
        
        val clearButton = Button("Clear")
        clearButton.setOnAction { planListView.items.clear() }
        
        planControlsHBox.children.addAll(executeButton, clearButton)
        content.children.addAll(planListView, planControlsHBox)
        
        return TitledPane("Execution Plan", content)
    }
    
    private fun createDroneStatusGroup(): TitledPane {
        val content = VBox()
        content.spacing = 5.0
        
        val droneTable = TableView<DroneInfo>()
        droneTable.prefHeight = 120.0
        droneTable.items = model.drones
        
        val idCol = TableColumn<DroneInfo, String>("ID")
        idCol.setCellValueFactory { it.value.id.let { id -> javafx.beans.property.SimpleStringProperty(id) } }
        idCol.prefWidth = 60.0
        
        val posCol = TableColumn<DroneInfo, String>("Position")
        posCol.setCellValueFactory { it.value.position.let { pos -> javafx.beans.property.SimpleStringProperty("(${pos.x.toInt()},${pos.y.toInt()})") } }
        posCol.prefWidth = 80.0
        
        val energyCol = TableColumn<DroneInfo, String>("Energy")
        energyCol.setCellValueFactory { it.value.let { drone -> javafx.beans.property.SimpleStringProperty("${drone.energy}/${drone.maxEnergy}") } }
        energyCol.prefWidth = 60.0
        
        val actionCol = TableColumn<DroneInfo, String>("Action")
        actionCol.setCellValueFactory { it.value.currentAction.let { action -> javafx.beans.property.SimpleStringProperty(action) } }
        actionCol.prefWidth = 100.0
        
        droneTable.columns.addAll(idCol, posCol, energyCol, actionCol)
        content.children.add(droneTable)
        
        return TitledPane("Drone Status", content)
    }
    
    private fun createLogDisplayGroup(): TitledPane {
        val content = VBox()
        content.spacing = 5.0
        
        logTextArea = TextArea()
        logTextArea.isEditable = false
        logTextArea.prefRowCount = 8
        
        // Bind log messages to text area
        model.logMessages.addListener(ListChangeListener { _ ->
            Platform.runLater {
                logTextArea.text = model.logMessages.joinToString("\n")
                logTextArea.positionCaret(logTextArea.text.length)
            }
        })
        
        val clearLogButton = Button("Clear Log")
        clearLogButton.setOnAction { model.logMessages.clear() }
        
        content.children.addAll(logTextArea, clearLogButton)
        VBox.setVgrow(logTextArea, Priority.ALWAYS)
        
        return TitledPane("Execution Log", content)
    }
    
    private fun createStatusPane(): Pane {
        val statusPane = HBox()
        statusPane.spacing = 10.0
        statusPane.padding = Insets(5.0, 10.0, 5.0, 10.0)
        statusPane.alignment = Pos.CENTER_LEFT
        
        statusLabel = Label("Ready")
        statusLabel.style = "-fx-font-weight: bold;"
        
        progressBar = ProgressBar(0.0)
        progressBar.prefWidth = 200.0
        
        val spacer = Region()
        HBox.setHgrow(spacer, Priority.ALWAYS)
        
        val versionLabel = Label("STRIPS Drone Planner v1.0")
        versionLabel.style = "-fx-text-fill: gray;"
        
        statusPane.children.addAll(statusLabel, progressBar, spacer, versionLabel)
        return statusPane
    }
    
    private fun setupEventHandlers() {
        startButton.setOnAction { startSimulation() }
        pauseButton.setOnAction { pauseSimulation() }
        resetButton.setOnAction { resetSimulation() }
        stepButton.setOnAction { stepSimulation() }
        
        // Bind UI properties to model
        statusLabel.textProperty().bind(model.statusMessage)
        progressBar.progressProperty().bind(
            model.currentStep.divide(model.totalSteps.add(1))
        )
    }
    
    private fun generatePlan() {
        val initialState = initialStateField.text.trim()
        val goalState = goalStateField.text.trim()
        
        if (initialState.isEmpty() || goalState.isEmpty()) {
            showAlert("Error", "Please fill in initial state and goal state")
            return
        }
        
        // Validate format
        if (!initialState.startsWith("[") || !initialState.endsWith("]")) {
            showAlert("Format Error", "Initial state must be in list format, e.g.: [at_drone(drone1,warehouse1), energy(drone1,100)]")
            return
        }
        
        if (!goalState.startsWith("[") || !goalState.endsWith("]")) {
            showAlert("Format Error", "Goal state must be in list format, e.g.: [at_package(pkg1,houseA)]")
            return
        }
        
        model.statusMessage.set("Generating plan...")
        model.addLogMessage("Starting plan generation...")
        model.addLogMessage("Initial state: $initialState")
        model.addLogMessage("Goal state: $goalState")
        model.addLogMessage("Max depth: ${maxDepthSpinner.value}")
        
        val task = object : Task<List<String>?>() {
            override fun call(): List<String>? {
                return try {
                    // Clear previous plan steps
                    Platform.runLater { model.planSteps.clear() }
                    
                    val result = planner.plan(initialState, goalState, maxDepthSpinner.value)
                    Platform.runLater {
                        if (result != null) {
                            model.addLogMessage("Prolog returned plan: $result")
                        } else {
                            model.addLogMessage("Prolog returned: null (no plan found)")
                        }
                    }
                    result
                } catch (e: Exception) {
                    Platform.runLater {
                        model.addLogMessage("Plan generation failed: ${e.message}")
                        model.addLogMessage("Exception type: ${e.javaClass.simpleName}")
                        if (e.cause != null) {
                            model.addLogMessage("Cause: ${e.cause?.message}")
                        }
                    }
                    null
                }
            }
            
            override fun succeeded() {
                val plan = value
                if (plan != null && plan.isNotEmpty()) {
                    planListView.items.clear()
                    model.planSteps.clear()
                    plan.forEachIndexed { index, action ->
                        planListView.items.add("${index + 1}. $action")
                        model.planSteps.add(PlanStep(index + 1, action, action))
                    }
                    model.totalSteps.set(plan.size)
                    model.addLogMessage("Plan generated successfully, ${plan.size} steps")
                    model.statusMessage.set("Plan generated")
                } else {
                    model.addLogMessage("Unable to find valid plan - Possible causes:")
                    model.addLogMessage("1. Goal state unreachable")
                    model.addLogMessage("2. Initial state format error")
                    model.addLogMessage("3. Insufficient search depth")
                    model.addLogMessage("4. Prolog rule issues")
                    model.statusMessage.set("Plan generation failed")
                }
            }
            
            override fun failed() {
                val exception = exception
                model.addLogMessage("Task execution failed: ${exception?.message}")
                model.statusMessage.set("Plan generation failed")
            }
        }
        
        Thread(task).start()
    }
    
    private fun executePlan() {
        if (model.planSteps.isEmpty()) {
            showAlert("Error", "Please generate an execution plan first")
            return
        }
        startSimulation()
    }
    
    private fun startSimulation() {
        if (model.isRunning.get()) return
        
        model.isRunning.set(true)
        model.isPaused.set(false)
        model.statusMessage.set("Simulation running...")
        updateUIState()
        
        simulationJob = simulationScope.launch {
            try {
                for (step in model.planSteps) {
                    if (!model.isRunning.get()) break
                    
                    while (model.isPaused.get()) {
                        delay(100)
                    }
                    
                    executeStep(step)
                    model.currentStep.set(step.stepNumber)
                    model.addLogMessage("Executing step ${step.stepNumber}: ${step.action}")
                    
                    delay(1000) // Animation delay
                }
                
                if (model.isRunning.get()) {
                    model.statusMessage.set("Simulation completed")
                    model.addLogMessage("Simulation execution completed")
                }
            } finally {
                model.isRunning.set(false)
                model.isPaused.set(false)
                updateUIState()
            }
        }
    }
    
    private fun pauseSimulation() {
        model.isPaused.set(!model.isPaused.get())
        model.statusMessage.set(if (model.isPaused.get()) "Simulation paused" else "Simulation running...")
        updateUIState()
    }
    
    private fun resetSimulation() {
        simulationJob?.cancel()
        model.reset()
        planListView.items.clear()
        updateUIState()
        model.addLogMessage("Simulation reset")
    }
    
    private fun stepSimulation() {
        // Execute one step of the plan
        val currentStepIndex = model.currentStep.get()
        if (currentStepIndex < model.planSteps.size) {
            val step = model.planSteps[currentStepIndex]
            executeStep(step)
            model.currentStep.set(currentStepIndex + 1)
            model.addLogMessage("Step execution: ${step.action}")
        }
    }
    
    private fun executeStep(step: PlanStep) {
        // Parse and execute the step action
        // This is a simplified version - you would need to parse the actual action
        val action = step.action
        
        when {
            action.contains("move") -> executeMove(action)
            action.contains("pickup") -> executePickup(action)
            action.contains("drop") -> executeDrop(action)
            action.contains("recharge") -> executeRecharge(action)
        }
        
        step.isCompleted = true
        Platform.runLater { mapPanel.redraw() }
    }
    
    private fun executeMove(action: String) {
        // Parse move action and update drone position
        val regex = "move\\((\\w+),\\s*(\\w+),\\s*(\\w+)\\)".toRegex()
        val match = regex.find(action)
        if (match != null) {
            val (droneId, from, to) = match.destructured
            val newPosition = model.getLocationPosition(to)
            model.updateDronePosition(droneId, newPosition)
            model.updateDroneAction(droneId, "moving to $to")
            
            // Simulate energy consumption
            val drone = model.drones.find { it.id == droneId }
            if (drone != null) {
                model.updateDroneEnergy(droneId, maxOf(0, drone.energy - 5))
            }
        }
    }
    
    private fun executePickup(action: String) {
        val regex = "pickup\\((\\w+),\\s*(\\w+),\\s*(\\w+)\\)".toRegex()
        val match = regex.find(action)
        if (match != null) {
            val (droneId, packageId, location) = match.destructured
            model.pickupPackage(droneId, packageId)
            model.updateDroneAction(droneId, "picked up $packageId")
        }
    }
    
    private fun executeDrop(action: String) {
        val regex = "drop\\((\\w+),\\s*(\\w+),\\s*(\\w+)\\)".toRegex()
        val match = regex.find(action)
        if (match != null) {
            val (droneId, packageId, location) = match.destructured
            val dropPosition = model.getLocationPosition(location)
            model.dropPackage(droneId, packageId, dropPosition)
            model.updateDroneAction(droneId, "dropped $packageId")
        }
    }
    
    private fun executeRecharge(action: String) {
        val regex = "recharge(?:_full)?\\((\\w+)(?:,\\s*(\\w+))?\\)".toRegex()
        val match = regex.find(action)
        if (match != null) {
            val droneId = match.groupValues[1]
            val drone = model.drones.find { it.id == droneId }
            if (drone != null) {
                model.updateDroneEnergy(droneId, drone.maxEnergy)
                model.updateDroneAction(droneId, "recharging")
            }
        }
    }
    
    private fun updateUIState() {
        Platform.runLater {
            val isRunning = model.isRunning.get()
            val isPaused = model.isPaused.get()
            
            startButton.isDisable = isRunning && !isPaused
            startButton.text = if (isPaused) "Resume" else "Start"
            pauseButton.isDisable = !isRunning
            stepButton.isDisable = isRunning && !isPaused
            resetButton.isDisable = false
        }
    }
    
    private fun loadScenario(scenario: String) {
        when (scenario) {
            "Simple Move" -> {
                initialStateField.text = "[at_drone(drone1,warehouse1), energy(drone1,100)]"
                goalStateField.text = "[at_drone(drone1,crossroad1)]"
                maxDepthSpinner.valueFactory.value = 10
            }
            "Simple Delivery" -> {
                initialStateField.text = "[at_drone(drone1,warehouse1), energy(drone1,100), at_package(pkg1,warehouse1)]"
                goalStateField.text = "[at_package(pkg1,houseA)]"
                maxDepthSpinner.valueFactory.value = 15
            }
            "Multiple Packages" -> {
                initialStateField.text = "[at_drone(drone1,warehouse1), energy(drone1,100), at_drone(drone2,warehouse2), energy(drone2,120), at_package(pkg1,warehouse1), at_package(pkg2,warehouse2)]"
                goalStateField.text = "[at_package(pkg1,houseA), at_package(pkg2,houseB)]"
                maxDepthSpinner.valueFactory.value = 25
            }
            "Horn Map Test" -> {
                initialStateField.text = "[at_drone(drone1,warehouse1), energy(drone1,100), at_drone(drone2,warehouse2), energy(drone2,120), at_package(pkg1,warehouse1), at_package(pkg2,warehouse1)]"
                goalStateField.text = "[at_package(pkg1,houseA), at_package(pkg2,houseB)]"
                maxDepthSpinner.valueFactory.value = 30
            }
            "Low Energy Scenario" -> {
                initialStateField.text = "[at_drone(drone1,warehouse1), energy(drone1,15), at_package(pkg1,warehouse1)]"
                goalStateField.text = "[at_package(pkg1,houseA)]"
                maxDepthSpinner.valueFactory.value = 35
            }
        }
    }

    private fun showAlert(title: String, message: String) {
        Platform.runLater {
            val alert = Alert(Alert.AlertType.ERROR)
            alert.title = title
            alert.headerText = null
            alert.contentText = message
            alert.showAndWait()
        }
    }
    
    override fun stop() {
        simulationJob?.cancel()
        simulationScope.cancel()
    }
    
    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            launch(*args)
        }
    }
} 