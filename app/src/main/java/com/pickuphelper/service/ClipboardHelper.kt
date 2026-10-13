package com.pickuphelper.service

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Build

object ClipboardHelper {
    fun readOnce(context: Context): String? {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        return try {
            val clip = clipboard.primaryClip
            if (clip == null || clip.itemCount == 0) null
            else clip.getItemAt(0).text?.toString()?.takeIf { it.isNotBlank() }
        } catch (e: Exception) { null }
    }
    fun clear(context: Context) {
        try {
            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) clipboard.clearPrimaryClip()
            else clipboard.setPrimaryClip(ClipData.newPlainText("", ""))
        } catch (e: Exception) {}
    }
}