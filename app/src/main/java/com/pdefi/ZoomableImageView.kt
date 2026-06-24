package com.pdefi

import android.content.Context
import android.graphics.Matrix
import android.graphics.PointF
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import androidx.appcompat.widget.AppCompatImageView
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

/**
 * ZoomableImageView — supports pinch-to-zoom and pan.
 * Constrains pan to image bounds and resets to fit on double-tap.
 */
class ZoomableImageView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyle: Int = 0
) : AppCompatImageView(context, attrs, defStyle) {

    private val matrix = Matrix()
    private val matrixValues = FloatArray(9)

    private var minScale = 1f
    private var maxScale = 5f
    private var currentScale = 1f

    private var lastTouchX = 0f
    private var lastTouchY = 0f
    private var lastPointerCount = 0

    private val scaleDetector = ScaleGestureDetector(context,
        object : ScaleGestureDetector.SimpleOnScaleGestureListener() {
            override fun onScale(detector: ScaleGestureDetector): Boolean {
                val scaleFactor = detector.scaleFactor
                val newScale = (currentScale * scaleFactor).coerceIn(minScale, maxScale)
                val scaleBy = newScale / currentScale
                matrix.postScale(scaleBy, scaleBy, detector.focusX, detector.focusY)
                currentScale = newScale
                constrainMatrix()
                imageMatrix = matrix
                return true
            }
        })

    private var lastDoubleTapTime = 0L

    init {
        scaleType = ScaleType.MATRIX
    }

    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        super.onLayout(changed, left, top, right, bottom)
        if (changed) resetMatrix()
    }

    private fun resetMatrix() {
        val d = drawable ?: return
        val dW = d.intrinsicWidth.toFloat()
        val dH = d.intrinsicHeight.toFloat()
        val vW = width.toFloat()
        val vH = height.toFloat()
        if (dW <= 0 || dH <= 0 || vW <= 0 || vH <= 0) return

        val scale = min(vW / dW, vH / dH)
        minScale = scale
        currentScale = scale

        matrix.reset()
        matrix.postScale(scale, scale)
        // Center
        matrix.postTranslate((vW - dW * scale) / 2f, (vH - dH * scale) / 2f)
        imageMatrix = matrix
    }

    private fun constrainMatrix() {
        val d = drawable ?: return
        matrix.getValues(matrixValues)
        val transX = matrixValues[Matrix.MTRANS_X]
        val transY = matrixValues[Matrix.MTRANS_Y]
        val scale = matrixValues[Matrix.MSCALE_X]

        val scaledW = d.intrinsicWidth * scale
        val scaledH = d.intrinsicHeight * scale
        val vW = width.toFloat()
        val vH = height.toFloat()

        var dx = 0f
        var dy = 0f

        // Horizontal
        if (scaledW <= vW) {
            dx = (vW - scaledW) / 2f - transX
        } else {
            if (transX > 0) dx = -transX
            else if (transX + scaledW < vW) dx = vW - (transX + scaledW)
        }

        // Vertical
        if (scaledH <= vH) {
            dy = (vH - scaledH) / 2f - transY
        } else {
            if (transY > 0) dy = -transY
            else if (transY + scaledH < vH) dy = vH - (transY + scaledH)
        }

        if (abs(dx) > 0.5f || abs(dy) > 0.5f) matrix.postTranslate(dx, dy)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        scaleDetector.onTouchEvent(event)

        val pointerCount = event.pointerCount
        val x = event.x
        val y = event.y

        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                val now = System.currentTimeMillis()
                if (now - lastDoubleTapTime < 300) {
                    // Double tap: toggle zoom
                    if (currentScale > minScale + 0.1f) {
                        resetMatrix()
                    } else {
                        val targetScale = minScale * 2.5f
                        val scaleBy = targetScale / currentScale
                        matrix.postScale(scaleBy, scaleBy, x, y)
                        currentScale = targetScale
                        constrainMatrix()
                        imageMatrix = matrix
                    }
                }
                lastDoubleTapTime = now
                lastTouchX = x
                lastTouchY = y
            }
            MotionEvent.ACTION_MOVE -> {
                if (!scaleDetector.isInProgress && pointerCount == 1 && currentScale > minScale) {
                    val dx = x - lastTouchX
                    val dy = y - lastTouchY
                    matrix.postTranslate(dx, dy)
                    constrainMatrix()
                    imageMatrix = matrix
                }
                lastTouchX = x
                lastTouchY = y
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                lastPointerCount = 0
            }
        }
        lastPointerCount = pointerCount

        // Allow parent (ViewPager2) to handle horizontal swipes only when at min zoom
        if (currentScale <= minScale + 0.05f) {
            parent?.requestDisallowInterceptTouchEvent(false)
        } else {
            parent?.requestDisallowInterceptTouchEvent(true)
        }

        return true
    }

    override fun setImageBitmap(bm: android.graphics.Bitmap?) {
        super.setImageBitmap(bm)
        if (bm != null) post { resetMatrix() }
    }
}
