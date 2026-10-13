package com.pickuphelper.ui.floating

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Color
import android.view.Gravity
import android.view.MotionEvent
import android.view.WindowManager
import android.widget.FrameLayout
import android.widget.TextView
import com.google.android.material.card.MaterialCardView
import kotlin.math.abs

@SuppressLint("ClickableViewAccessibility")
class FloatBallView(context: Context, private val onTap: () -> Unit) : FrameLayout(context) {
    private var initialX = 0; private var initialY = 0
    private var initialTouchX = 0f; private var initialTouchY = 0f
    private val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    private val card = MaterialCardView(context).apply {
        setCardBackgroundColor(Color.parseColor("#4CAF50"))
        cardElevation = 8f
        shapeAppearanceModel = com.google.android.material.shape.ShapeAppearanceModel.builder()
            .setAllCornerSizes(dpToPx(24).toFloat()).build()
        layoutParams = LayoutParams(dpToPx(48), dpToPx(48))
    }
    init {
        card.addView(TextView(context).apply {
            text = "\uD83D\uDCE6"
            textSize = 20f; gravity = Gravity.CENTER
            layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT)
        })
        addView(card)
        setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> { val p = layoutParams as WindowManager.LayoutParams; initialX = p.x; initialY = p.y; initialTouchX = event.rawX; initialTouchY = event.rawY; true }
                MotionEvent.ACTION_MOVE -> { val p = layoutParams as WindowManager.LayoutParams; p.x = initialX + (event.rawX - initialTouchX).toInt(); p.y = initialY + (event.rawY - initialTouchY).toInt(); windowManager.updateViewLayout(this, p); true }
                MotionEvent.ACTION_UP -> { if (abs(event.rawX - initialTouchX) < 10 && abs(event.rawY - initialTouchY) < 10) onTap(); true }
                else -> false
            }
        }
    }
    private fun dpToPx(dp: Int) = (dp * context.resources.displayMetrics.density).toInt()
}