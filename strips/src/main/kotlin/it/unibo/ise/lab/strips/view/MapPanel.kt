package it.unibo.ise.lab.strips.view

import it.unibo.ise.lab.strips.model.*
import javafx.scene.canvas.Canvas
import javafx.scene.canvas.GraphicsContext
import javafx.scene.paint.Color
import javafx.scene.text.Font
import javafx.scene.text.FontWeight
import javafx.collections.ListChangeListener

class MapPanel(private val model: DroneSimulationModel) : Canvas() {
    
    init {
        widthProperty().bind(model.mapWidth)
        heightProperty().bind(model.mapHeight)
        
        // Redraw when model changes
        model.drones.addListener(ListChangeListener { _ -> redraw() })
        model.packages.addListener(ListChangeListener { _ -> redraw() })
        model.locations.addListener(ListChangeListener { _ -> redraw() })
        
        // Redraw when canvas size changes
        widthProperty().addListener { _, _, _ -> redraw() }
        heightProperty().addListener { _, _, _ -> redraw() }
        
        redraw()
    }
    
    fun redraw() {
        val gc = graphicsContext2D
        gc.clearRect(0.0, 0.0, width, height)
        
        drawBackground(gc)
        drawConnections(gc)
        drawLocations(gc)
        drawPackages(gc)
        drawDrones(gc)
        drawLegend(gc)
    }
    
    private fun drawBackground(gc: GraphicsContext) {
        gc.fill = Color.LIGHTBLUE.deriveColor(0.0, 0.3, 1.0, 0.1)
        gc.fillRect(0.0, 0.0, width, height)
        
        // Draw grid
        gc.stroke = Color.LIGHTGRAY
        gc.lineWidth = 0.5
        val gridSize = 50.0
        
        var x = 0.0
        while (x <= width) {
            gc.strokeLine(x, 0.0, x, height)
            x += gridSize
        }
        
        var y = 0.0
        while (y <= height) {
            gc.strokeLine(0.0, y, width, y)
            y += gridSize
        }
    }
    
    private fun drawConnections(gc: GraphicsContext) {
        gc.stroke = Color.DARKGRAY
        gc.lineWidth = 2.0
        
        // Define connections based on DroneWorld.pl
        val connections = listOf(
            "warehouse1" to "crossroad1",
            "warehouse2" to "crossroad4",
            "crossroad1" to "crossroad2",
            "crossroad1" to "crossroad3",
            "crossroad1" to "crossroad4",
            "crossroad2" to "crossroad3",
            "crossroad3" to "junction_north",
            "crossroad4" to "junction_north",
            "crossroad2" to "junction_south",
            "crossroad1" to "houseA",
            "crossroad2" to "houseB",
            "crossroad3" to "houseC",
            "crossroad4" to "houseD",
            "crossroad1" to "base",
            "crossroad2" to "base",
            "junction_north" to "junction_south",
            "houseA" to "houseB"
        )
        
        for ((from, to) in connections) {
            val fromPos = model.getLocationPosition(from)
            val toPos = model.getLocationPosition(to)
            gc.strokeLine(fromPos.x, fromPos.y, toPos.x, toPos.y)
        }
    }
    
    private fun drawLocations(gc: GraphicsContext) {
        for (location in model.locations) {
            val pos = location.position
            val size = when (location.type) {
                LocationType.WAREHOUSE -> 25.0
                LocationType.BASE -> 20.0
                LocationType.HOUSE -> 15.0
                LocationType.CROSSROAD -> 10.0
                LocationType.JUNCTION -> 12.0
            }
            
            val color = when (location.type) {
                LocationType.WAREHOUSE -> Color.BROWN
                LocationType.BASE -> Color.GREEN
                LocationType.HOUSE -> Color.BLUE
                LocationType.CROSSROAD -> Color.GRAY
                LocationType.JUNCTION -> Color.ORANGE
            }
            
            // Draw location
            gc.fill = color
            gc.fillOval(pos.x - size/2, pos.y - size/2, size, size)
            
            // Draw border
            gc.stroke = Color.BLACK
            gc.lineWidth = 1.0
            gc.strokeOval(pos.x - size/2, pos.y - size/2, size, size)
            
            // Draw label
            gc.fill = Color.BLACK
            gc.font = Font.font("Arial", FontWeight.BOLD, 10.0)
            gc.fillText(location.id, pos.x - 20, pos.y - size/2 - 5)
        }
    }
    
    private fun drawPackages(gc: GraphicsContext) {
        for (pkg in model.packages) {
            if (!pkg.isPickedUp) {
                val pos = pkg.position
                val size = 8.0
                
                // Draw package
                gc.fill = Color.PURPLE
                gc.fillRect(pos.x - size/2, pos.y - size/2, size, size)
                
                // Draw border
                gc.stroke = Color.BLACK
                gc.lineWidth = 1.0
                gc.strokeRect(pos.x - size/2, pos.y - size/2, size, size)
                
                // Draw label
                gc.fill = Color.BLACK
                gc.font = Font.font("Arial", FontWeight.NORMAL, 8.0)
                gc.fillText(pkg.id, pos.x + size/2 + 2, pos.y + 2)
            }
        }
    }
    
    private fun drawDrones(gc: GraphicsContext) {
        for (drone in model.drones) {
            val pos = drone.position
            val size = 16.0
            
            // Draw drone body
            val color = if (drone.isActive) Color.RED else Color.DARKRED
            gc.fill = color
            gc.fillOval(pos.x - size/2, pos.y - size/2, size, size)
            
            // Draw border
            gc.stroke = Color.BLACK
            gc.lineWidth = 2.0
            gc.strokeOval(pos.x - size/2, pos.y - size/2, size, size)
            
            // Draw energy bar
            val barWidth = 20.0
            val barHeight = 4.0
            val energyRatio = drone.energy.toDouble() / drone.maxEnergy
            
            gc.fill = Color.WHITE
            gc.fillRect(pos.x - barWidth/2, pos.y + size/2 + 2, barWidth, barHeight)
            
            val energyColor = when {
                energyRatio > 0.6 -> Color.GREEN
                energyRatio > 0.3 -> Color.YELLOW
                else -> Color.RED
            }
            gc.fill = energyColor
            gc.fillRect(pos.x - barWidth/2, pos.y + size/2 + 2, barWidth * energyRatio, barHeight)
            
            gc.stroke = Color.BLACK
            gc.lineWidth = 1.0
            gc.strokeRect(pos.x - barWidth/2, pos.y + size/2 + 2, barWidth, barHeight)
            
            // Draw drone ID and action
            gc.fill = Color.BLACK
            gc.font = Font.font("Arial", FontWeight.BOLD, 10.0)
            gc.fillText(drone.id, pos.x - 15, pos.y - size/2 - 5)
            
            gc.font = Font.font("Arial", FontWeight.NORMAL, 8.0)
            gc.fillText(drone.currentAction, pos.x - 20, pos.y - size/2 - 15)
            
            // Draw carried packages
            val carriedPackages = model.packages.filter { it.carriedBy == drone.id }
            carriedPackages.forEachIndexed { index, pkg ->
                val pkgSize = 6.0
                val offsetX = (index - carriedPackages.size/2.0) * pkgSize
                gc.fill = Color.PURPLE
                gc.fillRect(pos.x + offsetX - pkgSize/2, pos.y + size/2 + 8, pkgSize, pkgSize)
                gc.stroke = Color.BLACK
                gc.lineWidth = 1.0
                gc.strokeRect(pos.x + offsetX - pkgSize/2, pos.y + size/2 + 8, pkgSize, pkgSize)
            }
        }
    }
    
    private fun drawLegend(gc: GraphicsContext) {
        val legendX = 10.0
        var legendY = 20.0
        val lineHeight = 20.0
        
        gc.fill = Color.WHITE.deriveColor(0.0, 1.0, 1.0, 0.8)
        gc.fillRect(legendX - 5, legendY - 15, 200.0, 180.0)
        
        gc.stroke = Color.BLACK
        gc.lineWidth = 1.0
        gc.strokeRect(legendX - 5, legendY - 15, 200.0, 180.0)
        
        gc.fill = Color.BLACK
        gc.font = Font.font("Arial", FontWeight.BOLD, 12.0)
        gc.fillText("图例 Legend", legendX, legendY)
        legendY += lineHeight
        
        gc.font = Font.font("Arial", FontWeight.NORMAL, 10.0)
        
        // Drones
        gc.fill = Color.RED
        gc.fillOval(legendX, legendY - 8, 12.0, 12.0)
        gc.fill = Color.BLACK
        gc.fillText("无人机 Drone", legendX + 20, legendY)
        legendY += lineHeight
        
        // Packages
        gc.fill = Color.PURPLE
        gc.fillRect(legendX, legendY - 8, 10.0, 10.0)
        gc.fill = Color.BLACK
        gc.fillText("包裹 Package", legendX + 20, legendY)
        legendY += lineHeight
        
        // Warehouse
        gc.fill = Color.BROWN
        gc.fillOval(legendX, legendY - 8, 12.0, 12.0)
        gc.fill = Color.BLACK
        gc.fillText("仓库 Warehouse", legendX + 20, legendY)
        legendY += lineHeight
        
        // Base
        gc.fill = Color.GREEN
        gc.fillOval(legendX, legendY - 8, 12.0, 12.0)
        gc.fill = Color.BLACK
        gc.fillText("基地 Base", legendX + 20, legendY)
        legendY += lineHeight
        
        // House
        gc.fill = Color.BLUE
        gc.fillOval(legendX, legendY - 8, 12.0, 12.0)
        gc.fill = Color.BLACK
        gc.fillText("房屋 House", legendX + 20, legendY)
        legendY += lineHeight
        
        // Crossroad
        gc.fill = Color.GRAY
        gc.fillOval(legendX, legendY - 8, 8.0, 8.0)
        gc.fill = Color.BLACK
        gc.fillText("路口 Crossroad", legendX + 20, legendY)
        legendY += lineHeight
        
        // Junction
        gc.fill = Color.ORANGE
        gc.fillOval(legendX, legendY - 8, 10.0, 10.0)
        gc.fill = Color.BLACK
        gc.fillText("连接点 Junction", legendX + 20, legendY)
    }
} 