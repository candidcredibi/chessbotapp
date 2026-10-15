package com.example.settings

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.engine.EngineStyle
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "chess_helper_prefs")

data class AppSettings(
    val engineDepth: Int = 8,
    val multiPv: Int = 3,
    val style: EngineStyle = EngineStyle.SAFE,
    val thinkFirstSeconds: Int = 0,
    val powerSavingEnabled: Boolean = false,
    val onboardingCompleted: Boolean = false,
    val arrowThickness: Int = 6
)

class SettingsRepository(private val context: Context) {

    private object Keys {
        val ENGINE_DEPTH = intPreferencesKey("engine_depth")
        val MULTI_PV = intPreferencesKey("multi_pv")
        val STYLE = stringPreferencesKey("style")
        val THINK_FIRST_SECONDS = intPreferencesKey("think_first_seconds")
        val POWER_SAVING = booleanPreferencesKey("power_saving")
        val ONBOARDING_COMPLETED = booleanPreferencesKey("onboarding_completed")
        val ARROW_THICKNESS = intPreferencesKey("arrow_thickness")
    }

    val settingsFlow: Flow<AppSettings> = context.dataStore.data.map { prefs ->
        AppSettings(
            engineDepth = prefs[Keys.ENGINE_DEPTH] ?: 8,
            multiPv = prefs[Keys.MULTI_PV] ?: 3,
            style = EngineStyle.fromName(prefs[Keys.STYLE] ?: EngineStyle.SAFE.name),
            thinkFirstSeconds = prefs[Keys.THINK_FIRST_SECONDS] ?: 0,
            powerSavingEnabled = prefs[Keys.POWER_SAVING] ?: false,
            onboardingCompleted = prefs[Keys.ONBOARDING_COMPLETED] ?: false,
            arrowThickness = prefs[Keys.ARROW_THICKNESS] ?: 6
        )
    }

    suspend fun updateEngineDepth(depth: Int) {
        context.dataStore.edit { it[Keys.ENGINE_DEPTH] = depth }
    }

    suspend fun updateMultiPv(count: Int) {
        context.dataStore.edit { it[Keys.MULTI_PV] = count }
    }

    suspend fun updateStyle(style: EngineStyle) {
        context.dataStore.edit { it[Keys.STYLE] = style.name }
    }

    suspend fun updateThinkFirstSeconds(seconds: Int) {
        context.dataStore.edit { it[Keys.THINK_FIRST_SECONDS] = seconds }
    }

    suspend fun updatePowerSaving(enabled: Boolean) {
        context.dataStore.edit { it[Keys.POWER_SAVING] = enabled }
    }

    suspend fun setOnboardingCompleted(completed: Boolean) {
        context.dataStore.edit { it[Keys.ONBOARDING_COMPLETED] = completed }
    }

    suspend fun updateArrowThickness(thickness: Int) {
        context.dataStore.edit { it[Keys.ARROW_THICKNESS] = thickness }
    }
}
