# Drone Delivery Planning System

An intelligent drone delivery path planning and simulation system based on STRIPS algorithm, implemented with Kotlin + Prolog hybrid programming and featuring a modern JavaFX graphical interface.

## 📋 Project Overview

This project implements a complete drone delivery planning simulation system, including:

- **🧠 Intelligent Planning Engine**: AI automatic planning based on STRIPS algorithm
- **🗺️ Horn Map Network**: Complex delivery network topology modeling
- **🖥️ Visualization Interface**: Modern JavaFX GUI with real-time planning process display
- **⚡ Multi-scenario Support**: Simple movement, package delivery, multi-drone collaboration, energy management
- **🔄 Real-time Simulation**: Animated demonstration of drone execution planning process

## 🛠️ System Requirements

### Required Environment
- **Java**: JDK 17 or higher
- **Operating System**: Windows 10/11
- **Memory**: At least 4GB RAM
- **Display**: 1200x800 resolution or higher

### Verify Java Installation
```cmd
java -version
REM Should display: openjdk version "17.x.x" or higher
```

If Java 17+ is not installed, download from:
- [Eclipse Temurin JDK 17](https://adoptium.net/temurin/releases/)
- [Oracle JDK 17](https://www.oracle.com/java/technologies/downloads/)

## 🚀 Quick Start

### 1. Clone the Project
```cmd
git clone <repository-url>
cd drone-delivery-project
```

### 2. Check Project Structure
```
drone-delivery-project/
├── README.md                    # This file
├── GUI-README.md               # GUI usage guide
├── GUI-TROUBLESHOOTING.md      # Troubleshooting guide
├── run-gui.bat                 # Windows startup script
├── build.gradle.kts            # Root project build configuration
├── settings.gradle.kts         # Gradle settings
├── gradlew                     # Gradle Wrapper (Unix)
├── gradlew.bat                 # Gradle Wrapper (Windows)
├── simaquarium/                # Fish tank simulation reference project
└── strips/                     # Main drone planning module
    ├── build.gradle.kts        # Module build configuration
    ├── src/main/kotlin/        # Kotlin source code
    │   └── it/unibo/ise/lab/strips/
    │       ├── Main.kt         # Program entry point
    │       ├── DronePlanner.kt # Planner core
    │       ├── World.kt        # World model loader
    │       ├── model/          # Data models
    │       └── view/           # GUI interface
    └── src/main/resources/     # Prolog knowledge base
        ├── DroneWorld.pl       # Drone world definition
        ├── Strips.pl           # STRIPS planning engine
        └── *.pl               # Other Prolog files
```

### 3. Build the Project

```cmd
REM Use the project's built-in Gradle Wrapper
.\gradlew.bat build
```

**Build Success Indicator**:
```
BUILD SUCCESSFUL in Xs
X actionable tasks: X executed
```

### 4. Launch GUI Interface

#### Method 1: Using Batch Script (Recommended)
```cmd
.\run-gui.bat
```

#### Method 2: Using Gradle Commands
```cmd
cd strips
..\gradlew.bat run --args="--gui"
```

#### Method 3: Specify World File
```cmd
cd strips
..\gradlew run --args="-w DroneWorld --gui"
```

## 🎮 GUI Usage Guide

### Interface Layout
- **Left Side**: Horn map visualization panel showing drones, packages, and connection lines
- **Right Side**: Control panel with scenario configuration, plan display, and status monitoring
- **Bottom**: Status bar and progress indicator

### Basic Operation Workflow

#### 1. Select Preset Scenarios
Choose from the toolbar dropdown menu:
- **Simple Move**: Drone moves from warehouse to crossroad
- **Simple Delivery**: Pick up package and deliver to target house
- **Multiple Package Delivery**: Simultaneously deliver multiple packages
- **Low Energy Scenario**: Complex planning including recharging

#### 2. Generate Execution Plan
1. Check "Initial State" and "Goal State" text boxes
2. Adjust "Max Search Depth" (optional)
3. Click "Generate Plan" button
4. View generated action sequence in "Execution Plan" list

#### 3. Execute Simulation
1. Click "Execute Plan" to start simulation
2. Use "Pause", "Step" to control execution
3. Observe drone movement animation on the map
4. Monitor position and energy changes in "Drone Status" table

#### 4. View Logs
Check detailed execution information and debug output in the "Execution Log" area.

### Example: Simple Move Scenario
1. Select "Simple Move"
2. Initial State: `[at_drone(drone1,warehouse1), energy(drone1,100)]`
3. Goal State: `[at_drone(drone1,crossroad1)]`
4. Generated Plan: `move(drone1, warehouse1, crossroad1)`
5. After execution, drone moves from warehouse1 to crossroad1

## 🔧 Command Line Mode

Besides GUI, the system also supports command line mode for planning:

### Basic Planning
```cmd
cd strips
..\gradlew.bat run --args="-w DroneWorld -is [at_drone(drone1,warehouse1),energy(drone1,100)] -g [at_drone(drone1,houseA)]"
```

### Interactive Mode
```cmd
cd strips
..\gradlew.bat run --args="-w DroneWorld"
REM Enter Prolog interactive environment for direct queries
```

### Test Mode
```cmd
cd strips
..\gradlew.bat run --args="-w DroneWorld -t"
REM Run built-in Horn map tests
```

### Display Map
```cmd
cd strips
..\gradlew.bat run --args="-w DroneWorld -m"
REM Display Horn map network structure
```

## 🧪 Testing and Verification

### 1. Run Unit Tests
```cmd
gradlew.bat test
```

### 2. View Test Reports
```cmd
REM Test report location:
strips\build\reports\tests\test\index.html
```

### 3. Verify Core Functionality
```cmd
cd strips
..\gradlew.bat run --args="-w DroneWorld -is [at_drone(drone1,warehouse1),energy(drone1,100)] -g [at_drone(drone1,crossroad1)]"

REM Expected output:
REM Plan found:
REM 1. move(drone1, warehouse1, crossroad1)
```

## 📁 Project Architecture

### Core Components

#### Kotlin Layer (strips/src/main/kotlin/)
- **Main.kt**: Program entry point, command line argument parsing
- **DronePlanner.kt**: Planner core, Prolog interaction
- **World.kt**: World model loader
- **model/DroneSimulationModel.kt**: GUI data model
- **view/DroneSimulationApp.kt**: Main GUI application
- **view/MapPanel.kt**: Map visualization component

#### Prolog Layer (strips/src/main/resources/)
- **DroneWorld.pl**: Drone world definition (locations, connections, actions)
- **Strips.pl**: STRIPS planning algorithm implementation
- **Utils.pl**: Common utility predicates

### Technology Stack
- **Kotlin**: System framework and GUI development
- **JavaFX**: Modern graphical user interface
- **tuProlog**: Prolog reasoning engine
- **Gradle**: Build and dependency management

## 🔍 Troubleshooting

### Common Issues

#### 1. Build Failure
```cmd
REM Clean and rebuild
gradlew.bat clean build
```

#### 2. Java Version Error
```cmd
REM Check Java version
java -version
echo %JAVA_HOME%
```

#### 3. GUI Launch Failure
- Ensure using JDK 17+ (includes JavaFX modules)
- Check specific error messages in error logs
- Try running as administrator if permissions are an issue

#### 4. Plan Generation Failure
- System implements automatic fallback mechanism, generating simulation plans for demonstration
- For detailed diagnosis, refer to: [GUI-TROUBLESHOOTING.md](GUI-TROUBLESHOOTING.md)

#### 5. Gradle Wrapper Issues
```cmd
REM If gradlew.bat is not found, check current directory
dir gradlew*
REM Make sure you're in the project root directory
```

### Detailed Troubleshooting
Refer to: [GUI-TROUBLESHOOTING.md](GUI-TROUBLESHOOTING.md)

## 📖 Related Documentation

- **[GUI Usage Guide](GUI-README.md)**: Detailed GUI functionality description
- **[Troubleshooting Guide](GUI-TROUBLESHOOTING.md)**: Common problem solutions
- **[Project Report](ISE_REPORT.pdf)**: Academic report and technical details

## 🏗️ Development Environment Setup

### IDE Configuration (Recommended: IntelliJ IDEA)
1. Import Gradle project
2. Set JDK 17+
3. Enable Kotlin plugin
4. Configure run configuration:
   - Main class: `it.unibo.ise.lab.strips.Main`
   - Program arguments: `-w DroneWorld --gui`

### Adding New Scenarios
1. Add new when branch in `loadScenario` method in `DroneSimulationApp.kt`
2. Add scenario name to dropdown menu
3. Define corresponding initial state and goal state

### Extending Prolog Knowledge Base
1. Edit `DroneWorld.pl` to add new locations, connections, or actions
2. Ensure compliance with STRIPS action definition format
3. Recompile and test



## 👥 Authors

- **Project Developer**: Tianyu Qu, Yiming Li, Jing Yang
- **Academic Institution**: University of Bologna (UNIBO)
- **Course**: Intelligent Systems Engineering


