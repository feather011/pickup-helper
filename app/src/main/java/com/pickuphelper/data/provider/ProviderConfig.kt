package com.pickuphelper.data.provider

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "provider_config")

object ProviderConfig {
    const val ACTIVE_MOCK = "mock"
    const val ACTIVE_KDNIAO = "kdniao"

    // Default to mock for Phase 0
    private val ACTIVE_KEY = stringPreferencesKey("active_provider")
    private var cachedActive: String? = null

    fun getActive(context: Context): String {
        cachedActive?.let { return it }
        return runBlocking {
            val prefs = context.dataStore.data.first()
            prefs[ACTIVE_KEY] ?: ACTIVE_MOCK
        }.also { cachedActive = it }
    }

    suspend fun setActive(context: Context, value: String) {
        cachedActive = value
        context.dataStore.edit { prefs ->
            prefs[ACTIVE_KEY] = value
        }
    }

    fun getEBusinessId(context: Context): String? {
        return runBlocking {
            val prefs = context.dataStore.data.first()
            prefs[stringPreferencesKey("ebusiness_id")]
        }
    }

    suspend fun setEBusinessId(context: Context, value: String) {
        context.dataStore.edit { prefs ->
            prefs[stringPreferencesKey("ebusiness_id")] = value
        }
    }

    fun getAppKey(context: Context): String? {
        return runBlocking {
            val prefs = context.dataStore.data.first()
            prefs[stringPreferencesKey("app_key")]
        }
    }

    suspend fun setAppKey(context: Context, value: String) {
        context.dataStore.edit { prefs ->
            prefs[stringPreferencesKey("app_key")] = value
        }
    }
}
