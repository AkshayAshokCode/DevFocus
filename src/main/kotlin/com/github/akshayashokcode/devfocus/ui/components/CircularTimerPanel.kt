package com.github.akshayashokcode.devfocus.ui.components

import java.awt.*
import javax.swing.JPanel
import javax.swing.UIManager

class CircularTimerPanel : JPanel() {

    private var timeText: String = "25:00"
    private var progress: Float = 1.0f // 0.0 to 1.0 (1.0 = full, 0.0 = empty)
    private var isBreakTime: Boolean = false

    // Colors following UX best practices
    private val workColor = Color(74, 144, 226) // Blue for focus/work
    private val breakColor = Color(80, 200, 120) // Green for rest
    private val backgroundColor = Color(224, 224, 224) // Light gray

    private val diameter = 180
    private val strokeWidth = 10f

    init {
        preferredSize = Dimension(diameter + 40, diameter + 40)
        isOpaque = false
    }

    fun updateTimer(timeText: String, progress: Float, isBreak: Boolean = false) {
        this.timeText = timeText
        this.progress = progress.coerceIn(0f, 1f)
        this.isBreakTime = isBreak
        repaint()
    }

    override fun paintComponent(g: Graphics) {
        super.paintComponent(g)
        val g2d = g as Graphics2D

        // Enable antialiasing for smooth circles
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
        g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON)

        val centerX = width / 2
        val centerY = height / 2
        val radius = diameter / 2

        // Calculate bounds for the arc
        val arcX = centerX - radius
        val arcY = centerY - radius
        val arcSize = diameter

        // Draw background circle (full circle in light gray)
        g2d.stroke = BasicStroke(strokeWidth, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND)
        g2d.color = backgroundColor
        g2d.drawArc(arcX, arcY, arcSize, arcSize, 0, 360)

        // Draw progress arc (clockwise from top, depleting)
        // Start at 90 degrees (top of circle) and go clockwise
        val arcAngle = (360 * progress).toInt()
        g2d.color = if (isBreakTime) breakColor else workColor
        g2d.stroke = BasicStroke(strokeWidth, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND)
        g2d.drawArc(arcX, arcY, arcSize, arcSize, 90, -arcAngle) // Negative for clockwise

        // Draw time text in center
        g2d.color = UIManager.getColor("Label.foreground") ?: Color.BLACK
        val font = Font("SansSerif", Font.BOLD, 36)
        g2d.font = font

        val metrics = g2d.fontMetrics
        val textWidth = metrics.stringWidth(timeText)
        val textHeight = metrics.height

        val textX = centerX - textWidth / 2
        val textY = centerY + textHeight / 4

        g2d.drawString(timeText, textX, textY)
    }
}