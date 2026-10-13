package com.pickuphelper.ui.floating

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.widget.Button
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import com.pickuphelper.viewmodel.FloatingUiState

@SuppressLint("InflateParams")
class FloatCardView(context: Context, private val onDismiss: () -> Unit, private val onOpenApp: () -> Unit) : FrameLayout(context) {
    private val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    // Bug3 fix: white rounded card background using GradientDrawable (no theme)
    private val cardBackground = GradientDrawable().apply {
        shape = GradientDrawable.RECTANGLE
        cornerRadius = dpToPx(16).toFloat()
        setColor(Color.WHITE)
        setElevation(12f)
    }

    // Wrapper FrameLayout with card background + padding
    private val cardWrapper = FrameLayout(context).apply {
        background = cardBackground
        layoutParams = LayoutParams(dpToPx(300), LayoutParams.WRAP_CONTENT)
        setPadding(dpToPx(16), dpToPx(16), dpToPx(16), dpToPx(16))
    }.also { addView(it) }

    fun update(state: FloatingUiState) {
        cardWrapper.removeAllViews()
        val content = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT)
        }
        when (state) {
            is FloatingUiState.Loading -> content.addView(TextView(context).apply { text = "查询中..."; textSize = 16f; setTextColor(Color.DKGRAY) })
            is FloatingUiState.Success -> buildSuccess(content, state)
            is FloatingUiState.Failure -> buildFailure(content, state.message)
            FloatingUiState.Idle -> {}
        }
        cardWrapper.addView(content)
    }

    private fun buildSuccess(content: LinearLayout, s: FloatingUiState.Success) {
        val ctx = context
        content.addView(TextView(ctx).apply { text = "运单尾号: " + s.waybillNo.takeLast(4) + "  |  " + s.shipperName; textSize = 14f; setTextColor(Color.parseColor("#555555")) })
        spacer(content, 8)
        val statusTxt = if (s.pickupCode != null) "\u2705 " + s.statusText + "  取件码: " + s.pickupCode else "\uD83D\uDCCB " + s.statusText
        content.addView(TextView(ctx).apply { text = statusTxt; textSize = 16f; setTextColor(Color.BLACK); typeface = android.graphics.Typeface.DEFAULT_BOLD })
        s.stationName?.let { spacer(content, 4); content.addView(TextView(ctx).apply { text = "\uD83D\uDCCD " + it; textSize = 13f; setTextColor(Color.parseColor("#666666")) }) }
        if (s.lastTrace.isNotBlank()) { spacer(content, 4); content.addView(TextView(ctx).apply { text = s.lastTrace; textSize = 12f; setTextColor(Color.parseColor("#888888")); maxLines = 2 }) }
        spacer(content, 12)
        val row = LinearLayout(ctx).apply { orientation = LinearLayout.HORIZONTAL; layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT) }
        // Bug3 fix: use standard android.widget.Button instead of MaterialButton
        row.addView(Button(ctx).apply { text = "忽略"; setOnClickListener { onDismiss() } })
        row.addView(Button(ctx).apply { text = "查看详情"; setOnClickListener { onOpenApp() } })
        content.addView(row)
    }

    private fun buildFailure(content: LinearLayout, msg: String) {
        val ctx = context
        content.addView(TextView(ctx).apply { text = "\u274C 查询失败"; textSize = 16f; setTextColor(Color.RED) })
        spacer(content, 4)
        content.addView(TextView(ctx).apply { text = msg; textSize = 13f; setTextColor(Color.DKGRAY); maxLines = 3 })
        spacer(content, 12)
        content.addView(Button(ctx).apply { text = "关闭"; setOnClickListener { onDismiss() } })
    }

    private fun spacer(parent: LinearLayout, h: Int) { parent.addView(View(context).apply { layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, dpToPx(h)) }) }
    private fun dpToPx(dp: Int) = (dp * context.resources.displayMetrics.density).toInt()
}