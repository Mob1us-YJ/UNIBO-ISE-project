@echo off
echo Starting Drone Delivery Planning Simulation GUI...
cd strips
..\gradlew run --args="-w DroneWorld --gui"
pause 