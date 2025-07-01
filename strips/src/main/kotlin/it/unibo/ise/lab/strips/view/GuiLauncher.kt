package it.unibo.ise.lab.strips.view

import javafx.application.Application

class GuiLauncher {
    companion object {
        @JvmStatic
        fun launchGui() {
            Application.launch(DroneSimulationApp::class.java)
        }
    }
} 