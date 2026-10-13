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

    fun getEBusinessId(context: Context): String? {
        return runBlocking {
            context.dataStore.data.first()[stringPreferencesKey("ebusiness_id")]
        }
    }

    suspend fun setEBusinessId(context: Context, value: String) {
        context.dataStore.edit { prefs -> prefs[stringPreferencesKey("ebusiness_id")] = value }
    }

    fun getAppKey(context: Context): String? {
        return runBlocking {
            context.dataStore.data.first()[stringPreferencesKey("app_key")]
        }
    }

    suspend fun setAppKey(context: Context, value: String) {
        context.dataStore.edit { prefs -> prefs[stringPreferencesKey("app_key")] = value }
    }
}
