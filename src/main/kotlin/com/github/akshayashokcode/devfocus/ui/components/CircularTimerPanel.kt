package com.github.akshayashokcode.devfocus.ui.components

import java.awt.*
import javax.swing.JPanel
import javax.swing.UIManager

class CircularTimerPanel : JPanel() {

    private var timeText: String = "25:00"
    private var progress: Float = 1.0f // 0.0 to 1.0 (1.0 = full, 0.0 = empty)
    private var isBreakTime: Boolean = false

    var focusColor: Color = Color(74, 144, 226)
    var breakColor: Color = Color(243, 156, 18)
    private val backgroundColor: Color
        get() = UIManager.getColor("Separator.separatorColor")
            ?: UIManager.getColor("Component.borderColor")
            ?: Color(200, 200, 200)

    init {
        minimumSize = Dimension(80, 80)
        preferredSize = Dimension(200, 200)
        maximumSize = Dimension(Int.MAX_VALUE, Int.MAX_VALUE)
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

        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
        g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON)

        // Derive all dimensions from actual panel size so it scales at any size
        val padding = 16
        val diameter = (minOf(width, height) - padding).coerceIn(60, 180)
        val strokeWidth = (diameter * 0.065f).coerceIn(5f, 15f)
        val fontSize = (diameter * 0.19f).coerceIn(12f, 44f).toInt()

        val centerX = width / 2
        val centerY = height / 2
        val radius = diameter / 2

        val arcX = centerX - radius
        val arcY = centerY - radius

        // Background track
        g2d.stroke = BasicStroke(strokeWidth, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND)
        g2d.color = backgroundColor
        g2d.drawArc(arcX, arcY, diameter, diameter, 0, 360)

        // Progress arc (clockwise from top, depleting)
        val arcAngle = (360 * progress).toInt()
        g2d.color = if (isBreakTime) breakColor else focusColor
        g2d.stroke = BasicStroke(strokeWidth, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND)
        g2d.drawArc(arcX, arcY, diameter, diameter, 90, -arcAngle)

        // Time text centered inside arc
        g2d.color = UIManager.getColor("Label.foreground") ?: Color.BLACK
        g2d.font = Font("SansSerif", Font.BOLD, fontSize)
        val metrics = g2d.fontMetrics
        val textX = centerX - metrics.stringWidth(timeText) / 2
        val textY = centerY + metrics.height / 4
        g2d.drawString(timeText, textX, textY)
    }
}
