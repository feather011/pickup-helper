package com.pickuphelper.ui.floating

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.widget.Button
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import com.pickuphelper.viewmodel.FloatingUiState

@SuppressLint("InflateParams")
class FloatCardView(
    context: Context,
    private val onDismiss: () -> Unit,
    private val onOpenApp: () -> Unit,
    private val onQueryManual: (String) -> Unit
) : FrameLayout(context) {
    private val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager

    /** 卡片最大高度（dp）：防止可聚焦浮窗被软键盘 insets 撑满全屏 */
    private val maxCardHeight = dpToPx(400)

    init {
        layoutParams = LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT)
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val cappedHeight = MeasureSpec.makeMeasureSpec(maxCardHeight, MeasureSpec.AT_MOST)
        super.onMeasure(widthMeasureSpec, cappedHeight)
    }

    private val cardBackground = GradientDrawable().apply {
        shape = GradientDrawable.RECTANGLE
        cornerRadius = dpToPx(16).toFloat()
        setColor(Color.WHITE)
        setElevation(12f)
    }

    private val cardWrapper = FrameLayout(context).apply {
        background = cardBackground
        layoutParams = LayoutParams(dpToPx(300), LayoutParams.WRAP_CONTENT)
        setPadding(dpToPx(16), dpToPx(16), dpToPx(16), dpToPx(16))
    }.also { addView(it) }

    fun update(state: FloatingUiState) {
        cardWrapper.removeAllViews()
        val content = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        }
        when (state) {
            is FloatingUiState.Loading -> content.addView(TextView(context).apply { text = "查询中..."; textSize = 16f; setTextColor(Color.DKGRAY) })
            is FloatingUiState.Success -> buildSuccess(content, state)
            is FloatingUiState.Failure -> buildFailure(content, state.message, state.waybillNo)
            FloatingUiState.Idle -> buildIdle(content)
        }
        cardWrapper.addView(content)
    }

    private fun buildIdle(content: LinearLayout) {
        val ctx = context
        content.addView(TextView(ctx).apply {
            text = "\uD83D\uDCE6 快递取件码查询"
            textSize = 15f
            setTextColor(Color.parseColor("#333333"))
            typeface = android.graphics.Typeface.DEFAULT_BOLD
        })
        val input = EditText(ctx).apply {
            hint = "请输入或粘贴运单号"
            textSize = 14f
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { bottomMargin = dpToPx(8) }
            setPadding(dpToPx(8), dpToPx(8), dpToPx(8), dpToPx(8))
            background = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = dpToPx(8).toFloat()
                setColor(Color.parseColor("#F5F5F5"))
            }
        }
        content.addView(input)
        val row = LinearLayout(ctx).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            gravity = Gravity.END
        }
        row.addView(Button(ctx).apply {
            text = "取消"
            layoutParams = LinearLayout.LayoutParams(dpToPx(96), dpToPx(44))
            setOnClickListener { onDismiss() }
        })
        spacerH(row, 8)
        row.addView(Button(ctx).apply {
            text = "查询"
            layoutParams = LinearLayout.LayoutParams(dpToPx(96), dpToPx(44))
            setOnClickListener {
                val text = input.text.toString().trim()
                if (text.isNotEmpty()) {
                    onQueryManual(text)
                }
            }
        })
        content.addView(row)
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
        val row = LinearLayout(ctx).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        }
        row.addView(Button(ctx).apply {
            text = "忽略"
            layoutParams = LinearLayout.LayoutParams(dpToPx(96), dpToPx(44))
            setOnClickListener { onDismiss() }
        })
        row.addView(Button(ctx).apply {
            text = "查看详情"
            layoutParams = LinearLayout.LayoutParams(dpToPx(96), dpToPx(44))
            setOnClickListener { onOpenApp() }
        })
        content.addView(row)
    }

    private fun buildFailure(content: LinearLayout, msg: String, waybillNo: String?) {
        val ctx = context
        content.addView(TextView(ctx).apply { text = "\u274C 查询失败"; textSize = 16f; setTextColor(Color.RED) })
        content.addView(TextView(ctx).apply { text = msg; textSize = 13f; setTextColor(Color.DKGRAY); maxLines = 3 })
        spacer(content, 12)
        val input = EditText(ctx).apply {
            hint = "重新输入运单号"
            textSize = 14f
            setText(waybillNo ?: "")
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { bottomMargin = dpToPx(8) }
            setPadding(dpToPx(8), dpToPx(8), dpToPx(8), dpToPx(8))
            background = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = dpToPx(8).toFloat()
                setColor(Color.parseColor("#F5F5F5"))
            }
        }
        content.addView(input)
        val row = LinearLayout(ctx).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            gravity = Gravity.END
        }
        row.addView(Button(ctx).apply {
            text = "关闭"
            layoutParams = LinearLayout.LayoutParams(dpToPx(96), dpToPx(44))
            setOnClickListener { onDismiss() }
        })
        spacerH(row, 8)
        row.addView(Button(ctx).apply {
            text = "重试"
            layoutParams = LinearLayout.LayoutParams(dpToPx(96), dpToPx(44))
            setOnClickListener {
                val text = input.text.toString().trim()
                if (text.isNotEmpty()) {
                    onQueryManual(text)
                }
            }
        })
        content.addView(row)
    }

    /** 垂直间隔：用 LinearLayout.LayoutParams 的 topMargin，避免 FrameLayout.LayoutParams 语义歧义 */
    private fun spacer(parent: LinearLayout, h: Int) {
        parent.addView(View(context).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dpToPx(h)
            )
        })
    }

    /** 水平按钮间隔 */
    private fun spacerH(parent: LinearLayout, w: Int) {
        parent.addView(View(context).apply {
            layoutParams = LinearLayout.LayoutParams(dpToPx(w), dpToPx(1))
        })
    }
    private fun dpToPx(dp: Int) = (dp * context.resources.displayMetrics.density).toInt()
}
