package com.pickuphelper.data.provider

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "provider_config")

object ProviderConfig {
    const val ACTIVE_MOCK = "mock"
    const val ACTIVE_KDNIAO = "kdniao"

    private val ACTIVE_KEY = stringPreferencesKey("active_provider")
    private var cachedActive: String? = null

    private val FLOATING_BALL_KEY = booleanPreferencesKey("floating_ball_enabled")
    private var cachedFloatingBall: Boolean? = null

    // 收件人手机号（快递100 取件码接口硬要求）
    private val RECEIVER_PHONE_KEY = stringPreferencesKey("receiver_phone")
    private var cachedReceiverPhone: String? = null

    // 后端服务地址（Ktor 代理服务 URL）
    private val BACKEND_URL_KEY = stringPreferencesKey("backend_url")
    private var cachedBackendUrl: String? = null

    // EBusinessID / APP_KEY（已废弃，仅保留兼容，真实接入走后端代理）
    private val EBUSINESS_ID_KEY = stringPreferencesKey("ebusiness_id")
    private val APP_KEY_KEY = stringPreferencesKey("app_key")

    fun getActive(context: Context): String {
        cachedActive?.let { return it }
        return runBlocking {
            val prefs = context.dataStore.data.first()
            prefs[ACTIVE_KEY] ?: ACTIVE_MOCK
        }.also { cachedActive = it }
    }

    suspend fun setActive(context: Context, value: String) {
        cachedActive = value
        context.dataStore.edit { prefs -> prefs[ACTIVE_KEY] = value }
    }

    fun getFloatingBallEnabled(context: Context): Boolean {
        cachedFloatingBall?.let { return it }
        return runBlocking {
            val prefs = context.dataStore.data.first()
            prefs[FLOATING_BALL_KEY] ?: false
        }.also { cachedFloatingBall = it }
    }

    suspend fun setFloatingBallEnabled(context: Context, enabled: Boolean) {
        cachedFloatingBall = enabled
        context.dataStore.edit { prefs -> prefs[FLOATING_BALL_KEY] = enabled }
    }

    // ── 收件人手机号（Kuaidi100 取件码接口硬要求） ─────────────────────

    fun getReceiverPhone(context: Context): String? {
        cachedReceiverPhone?.let { return it }
        return runBlocking {
            context.dataStore.data.first()[RECEIVER_PHONE_KEY]
        }.also { cachedReceiverPhone = it }
    }

    suspend fun setReceiverPhone(context: Context, value: String) {
        cachedReceiverPhone = value
        context.dataStore.edit { prefs -> prefs[RECEIVER_PHONE_KEY] = value }
    }

    // ── 后端服务地址（Ktor 代理） ───────────────────────────────────

    fun getBackendUrl(context: Context): String? {
        cachedBackendUrl?.let { return it }
        return runBlocking {
            context.dataStore.data.first()[BACKEND_URL_KEY]
        }.also { cachedBackendUrl = it }
    }

    suspend fun setBackendUrl(context: Context, value: String) {
        cachedBackendUrl = value
        context.dataStore.edit { prefs -> prefs[BACKEND_URL_KEY] = value }
    }

    // ── EBusinessID / APP_KEY（已废弃兼容，真实接入走后端代理） ────────

    fun getEBusinessId(context: Context): String? {
        return runBlocking {
            context.dataStore.data.first()[EBUSINESS_ID_KEY]
        }
    }

    suspend fun setEBusinessId(context: Context, value: String) {
        context.dataStore.edit { prefs -> prefs[EBUSINESS_ID_KEY] = value }
    }

    fun getAppKey(context: Context): String? {
        return runBlocking {
            context.dataStore.data.first()[APP_KEY_KEY]
        }
    }

    suspend fun setAppKey(context: Context, value: String) {
        context.dataStore.edit { prefs -> prefs[APP_KEY_KEY] = value }
    }
}
