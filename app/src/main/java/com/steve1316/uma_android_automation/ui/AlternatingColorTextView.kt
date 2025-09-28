package com.steve1316.uma_android_automation.ui

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import androidx.appcompat.widget.AppCompatTextView

class AlternatingColorTextView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : AppCompatTextView(context, attrs, defStyleAttr) {

    private val linePaint1 = Paint().apply { color = Color.parseColor("#F5F5F5") } // Light Gray
    private val linePaint2 = Paint().apply { color = Color.parseColor("#E0E0E0") } // Darker Gray

    override fun onDraw(canvas: Canvas) {
        val layout = layout ?: return

        for (i in 0 until layout.lineCount) {
            val paint = if (i % 2 == 0) linePaint1 else linePaint2
            val top = layout.getLineTop(i).toFloat()
            val bottom = layout.getLineBottom(i).toFloat()
            canvas.drawRect(0f, top, width.toFloat(), bottom, paint)
        }

        super.onDraw(canvas)
    }
}