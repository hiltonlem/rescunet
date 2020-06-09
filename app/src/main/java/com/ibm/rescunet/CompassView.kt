package com.ibm.rescunet

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.util.AttributeSet
import android.view.View
import kotlin.math.cos
import kotlin.math.sin

class CompassView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {
    var heading = 0f
    set(value) {field = value; invalidate()}
    override fun onDraw(canvas: Canvas?) {
        val x = (width / 2).toFloat()
        val y = (height / 2).toFloat()
        val density = context.resources.displayMetrics.density
        val paint = Paint()
        paint.style = Paint.Style.FILL
        paint.color = Color.argb(205, 153, 153, 255)
        paint.textSize = 30f

        val compassDirection = (Math.PI / 180 * heading).toFloat()
        val l1 = 12
        val l2 = 10
        val d1 = (Math.PI * 4 / 5 + compassDirection).toFloat()
        val d2 = (-Math.PI * 4 / 5 + compassDirection).toFloat()
        paint.color = Color.argb(50, 255, 255, 255)
        paint.style = Paint.Style.FILL

        val path = Path()
        path.moveTo(x + l1 * density * sin(compassDirection), y - l1 * density * cos(compassDirection))
        path.lineTo(x + l2 * density * sin(d1), y - l2 * density * cos(d1))
        path.lineTo(x + l2 * density * sin(d2), y - l2 * density * cos(d2))
        path.close()

        canvas?.drawPath(path, paint)
    }
}