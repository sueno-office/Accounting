package com.example.receiptscanner.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

@Singleton
class SettingsRepository @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        val KEY_SOUND_ENABLED = booleanPreferencesKey("sound_enabled")
        val KEY_SKIP_REVIEW = booleanPreferencesKey("skip_review")
        val KEY_GEMINI_API_KEY = stringPreferencesKey("gemini_api_key")
        val KEY_STABILITY_DURATION = floatPreferencesKey("stability_duration")
        val KEY_CATEGORIES = stringPreferencesKey("categories")
    }

    val soundEnabled: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[KEY_SOUND_ENABLED] ?: true
    }

    val skipReview: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[KEY_SKIP_REVIEW] ?: false
    }

    val geminiApiKey: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[KEY_GEMINI_API_KEY] ?: ""
    }

    val stabilityDuration: Flow<Float> = context.dataStore.data.map { prefs ->
        prefs[KEY_STABILITY_DURATION] ?: 1.5f
    }

    val categories: Flow<List<String>> = context.dataStore.data.map { prefs ->
        val stored = prefs[KEY_CATEGORIES] ?: ""
        if (stored.isBlank()) {
            listOf("交際費", "交通費", "消耗品費", "通信費", "会議費",
                   "広告費", "雑費", "食費", "光熱費", "備品費", "その他")
        } else {
            stored.split(",").map { it.trim() }.filter { it.isNotEmpty() }
        }
    }

    suspend fun setSoundEnabled(enabled: Boolean) {
        context.dataStore.edit { it[KEY_SOUND_ENABLED] = enabled }
    }

    suspend fun setSkipReview(skip: Boolean) {
        context.dataStore.edit { it[KEY_SKIP_REVIEW] = skip }
    }

    suspend fun setGeminiApiKey(key: String) {
        context.dataStore.edit { it[KEY_GEMINI_API_KEY] = key }
    }

    suspend fun setStabilityDuration(seconds: Float) {
        context.dataStore.edit { it[KEY_STABILITY_DURATION] = seconds }
    }

    suspend fun setCategories(cats: List<String>) {
        context.dataStore.edit { it[KEY_CATEGORIES] = cats.joinToString(",") }
    }
}
