package com.pickuphelper.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.view.Gravity
import android.view.WindowManager
import androidx.core.app.NotificationCompat
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.lifecycleScope
import com.pickuphelper.MainActivity
import com.pickuphelper.ui.floating.FloatBallView
import com.pickuphelper.ui.floating.FloatCardView
import com.pickuphelper.viewmodel.FloatingUiState
import com.pickuphelper.viewmodel.FloatingViewModel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class FloatingWindowService : LifecycleService() {
    private lateinit var windowManager: WindowManager
    private var ballView: FloatBallView? = null
    private var cardView: FloatCardView? = null
    private lateinit var viewModel: FloatingViewModel

    companion object {
        const val CHANNEL_ID = "pickup_helper_floating"
        const val NOTIFICATION_ID = 1001
        fun start(context: android.content.Context) {
            val intent = Intent(context, FloatingWindowService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) context.startForegroundService(intent)
            else context.startService(intent)
        }
        fun stop(context: android.content.Context) { context.stopService(Intent(context, FloatingWindowService::class.java)) }
    }

    override fun onCreate() {
        super.onCreate()
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        viewModel = FloatingViewModel(application)
        createNotificationChannel()
        startForeground(NOTIFICATION_ID, buildNotification())
        showFloatingBall()
        observeState()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(CHANNEL_ID, "float window service", NotificationManager.IMPORTANCE_LOW).apply {
                description = "pickup helper floating service"
                setShowBadge(false)
            }
            getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }
    }

    private fun buildNotification(): Notification {
        val pendingIntent = PendingIntent.getActivity(this, 0, Intent(this, MainActivity::class.java), PendingIntent.FLAG_IMMUTABLE)
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("pickup helper")
            .setContentText("tap to manage")
            .setSmallIcon(android.R.drawable.ic_menu_send)
            .setContentIntent(pendingIntent).setOngoing(true).build()
    }

    private fun showFloatingBall() {
        if (ballView != null) return
        val ball = FloatBallView(this) { viewModel.onBallTapped(); viewModel.expand() }
        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT, WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT
        ).apply { gravity = Gravity.TOP or Gravity.START; x = 0; y = 300 }
        windowManager.addView(ball, params)
        ballView = ball
    }

    private fun observeState() {
        lifecycleScope.launch { viewModel.expanded.collectLatest { expanded -> if (expanded) showCard() else hideCard() } }
        lifecycleScope.launch { viewModel.uiState.collectLatest { state -> cardView?.update(state) } }
    }

    private fun showCard() {
        if (cardView != null) return
        val card = FloatCardView(
            this,
            { viewModel.collapse(); viewModel.reset() },
            { viewModel.collapse(); viewModel.reset(); startActivity(Intent(this, MainActivity::class.java).apply { flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP }) },
            { waybillNo -> viewModel.queryManual(waybillNo) }
        )
        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT, WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE, PixelFormat.TRANSLUCENT
        ).apply { gravity = Gravity.CENTER_HORIZONTAL or Gravity.CENTER_VERTICAL }
        windowManager.addView(card, params)
        cardView = card
        card.update(FloatingUiState.Idle)
    }

    private fun hideCard() {
        cardView?.let { try { windowManager.removeView(it) } catch(e: Exception) {}; cardView = null }
    }

    override fun onDestroy() {
        hideCard()
        ballView?.let { try { windowManager.removeView(it) } catch(e: Exception) {} }
        ballView = null
        super.onDestroy()
    }
}
