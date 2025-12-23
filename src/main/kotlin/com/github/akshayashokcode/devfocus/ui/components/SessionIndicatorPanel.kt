package com.github.akshayashokcode.devfocus.ui.components

import java.awt.*
import javax.swing.JPanel

class SessionIndicatorPanel : JPanel() {

    private var currentSession: Int = 1
    private var totalSessions: Int = 4

    private val tomatoSize = 24
    private val spacing = 8

    init {
        isOpaque = false
        preferredSize = Dimension(200, 40)
    }

    fun updateSessions(current: Int, total: Int) {
        this.currentSession = current
        this.totalSessions = total
        repaint()
    }

    override fun paintComponent(g: Graphics) {
        super.paintComponent(g)
        val g2d = g as Graphics2D

        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)

        val totalWidth = (tomatoSize * totalSessions) + (spacing * (totalSessions - 1))
        val startX = (width - totalWidth) / 2
        val startY = (height - tomatoSize) / 2

        for (i in 1..totalSessions) {
            val x = startX + (i - 1) * (tomatoSize + spacing)

            if (i < currentSession) {
                // Completed session - filled tomato
                drawFilledTomato(g2d, x, startY)
            } else if (i == currentSession) {
                // Current session - outlined tomato with pulse effect
                drawCurrentTomato(g2d, x, startY)
            } else {
                // Future session - empty circle
                drawEmptyCircle(g2d, x, startY)
            }
        }
    }

    private fun drawFilledTomato(g2d: Graphics2D, x: Int, y: Int) {
        g2d.color = Color(231, 76, 60) // Tomato red
        g2d.fillOval(x, y, tomatoSize, tomatoSize)

        // Add small leaf/stem on top
        g2d.color = Color(46, 204, 113) // Green
        val leafSize = 6
        g2d.fillOval(x + tomatoSize / 2 - leafSize / 2, y - 2, leafSize, leafSize)
    }

    private fun drawCurrentTomato(g2d: Graphics2D, x: Int, y: Int) {
        // Outlined tomato for current session
        g2d.color = Color(231, 76, 60)
        g2d.stroke = BasicStroke(2.5f)
        g2d.drawOval(x + 2, y + 2, tomatoSize - 4, tomatoSize - 4)

        // Small filled center
        g2d.fillOval(x + tomatoSize / 2 - 3, y + tomatoSize / 2 - 3, 6, 6)
    }

    private fun drawEmptyCircle(g2d: Graphics2D, x: Int, y: Int) {
        g2d.color = Color(189, 195, 199) // Light gray
        g2d.stroke = BasicStroke(2f)
        g2d.drawOval(x + 2, y + 2, tomatoSize - 4, tomatoSize - 4)
    }
}