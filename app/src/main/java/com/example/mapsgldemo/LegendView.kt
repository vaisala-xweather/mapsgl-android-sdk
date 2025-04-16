package com.example.mapsgldemo

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Shader
import android.util.AttributeSet
import android.view.View

class LegendView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val paint = Paint()
    private val colors = intArrayOf(Color.BLUE, Color.GREEN, Color.RED)
    private val positions = floatArrayOf(0f, .5f, 1f)
    private lateinit var gradient: LinearGradient
    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        gradient = LinearGradient(
            0f, 0f, width.toFloat(), 0f,
            colors,
            positions,
            Shader.TileMode.CLAMP
        )

        paint.shader = gradient
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), paint)
    }
}