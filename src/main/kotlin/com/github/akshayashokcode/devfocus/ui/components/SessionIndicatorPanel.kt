package com.github.akshayashokcode.devfocus.ui.components

import java.awt.*
import javax.swing.JPanel

class SessionIndicatorPanel : JPanel() {

    private var currentSession: Int = 1
    private var totalSessions: Int = 4
    private var isBreakTime: Boolean = false

    private val completedColor = Color(39, 174, 96)
    private val workColor = Color(74, 144, 226)
    private val breakColor = Color(243, 156, 18)
    private val upcomingColor = Color(149, 165, 166)

    private val indicatorSize = 30
    private val spacing = 12

    init {
        isOpaque = false
        preferredSize = Dimension(200, 50)
    }

    fun updateSessions(current: Int, total: Int, isBreak: Boolean = false) {
        this.currentSession = current
        this.totalSessions = total
        this.isBreakTime = isBreak
        repaint()
    }

    override fun paintComponent(g: Graphics) {
        super.paintComponent(g)
        val g2d = g as Graphics2D

        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)

        val totalWidth = (indicatorSize * totalSessions) + (spacing * (totalSessions - 1))
        val startX = (width - totalWidth) / 2
        val startY = (height - indicatorSize) / 2

        for (i in 1..totalSessions) {
            val x = startX + (i - 1) * (indicatorSize + spacing)

            if (i < currentSession) {
                // Completed session
                drawCompletedSession(g2d, x, startY)
            } else if (i == currentSession) {
                // Current session
                drawCurrentSession(g2d, x, startY)
            } else {
                // Upcoming session
                drawUpcomingSession(g2d, x, startY)
            }
        }
    }

    private fun drawCompletedSession(g2d: Graphics2D, x: Int, y: Int) {
        g2d.color = completedColor
        g2d.fillOval(x, y, indicatorSize, indicatorSize)

        // Draw white checkmark
        g2d.color = Color.WHITE
        g2d.stroke = BasicStroke(2.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND)

        val checkSize = indicatorSize / 2
        val centerX = x + indicatorSize / 2
        val centerY = y + indicatorSize / 2

        val x1 = centerX - checkSize / 3
        val y1 = centerY
        val x2 = centerX - checkSize / 6
        val y2 = centerY + checkSize / 3
        g2d.drawLine(x1, y1, x2, y2)

        val x3 = centerX + checkSize / 2
        val y3 = centerY - checkSize / 3
        g2d.drawLine(x2, y2, x3, y3)
    }

    private fun drawCurrentSession(g2d: Graphics2D, x: Int, y: Int) {
        g2d.color = if (isBreakTime) breakColor else workColor
        g2d.fillOval(x, y, indicatorSize, indicatorSize)
    }

    private fun drawUpcomingSession(g2d: Graphics2D, x: Int, y: Int) {
        g2d.color = upcomingColor
        g2d.stroke = BasicStroke(2.5f)
        g2d.drawOval(x + 2, y + 2, indicatorSize - 4, indicatorSize - 4)
    }
}