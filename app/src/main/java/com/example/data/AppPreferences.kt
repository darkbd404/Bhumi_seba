package com.example.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.model.SavedMeasurementRecord
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "bhumi_survey_prefs")

class AppPreferences(private val context: Context) {

    private val json = Json { ignoreUnknownKeys = true }

    companion object {
        private val KEY_DARK_MODE = booleanPreferencesKey("dark_mode_enabled")
        private val KEY_DEV_PHOTO_URI = stringPreferencesKey("dev_profile_photo_uri")
        private val KEY_SAVED_RECORDS = stringPreferencesKey("saved_measurement_records")
        private val KEY_LAST_EXPORT_PATH = stringPreferencesKey("last_export_path")
    }

    val isDarkModeFlow: Flow<Boolean?> = context.dataStore.data.map { prefs ->
        prefs[KEY_DARK_MODE]
    }

    val devPhotoUriFlow: Flow<String?> = context.dataStore.data.map { prefs ->
        prefs[KEY_DEV_PHOTO_URI]
    }

    val savedRecordsFlow: Flow<List<SavedMeasurementRecord>> = context.dataStore.data.map { prefs ->
        val raw = prefs[KEY_SAVED_RECORDS] ?: return@map emptyList()
        runCatching { json.decodeFromString<List<SavedMeasurementRecord>>(raw) }.getOrDefault(emptyList())
    }

    suspend fun setDarkMode(enabled: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[KEY_DARK_MODE] = enabled
        }
    }

    suspend fun setDevPhotoUri(uriString: String) {
        context.dataStore.edit { prefs ->
            prefs[KEY_DEV_PHOTO_URI] = uriString
        }
    }

    suspend fun addMeasurementRecord(record: SavedMeasurementRecord) {
        context.dataStore.edit { prefs ->
            val current = prefs[KEY_SAVED_RECORDS]?.let {
                runCatching { json.decodeFromString<List<SavedMeasurementRecord>>(it) }.getOrNull()
            } ?: emptyList()
            val updated = (listOf(record) + current).take(30)
            prefs[KEY_SAVED_RECORDS] = json.encodeToString(updated)
        }
    }

    suspend fun clearHistory() {
        context.dataStore.edit { prefs ->
            prefs.remove(KEY_SAVED_RECORDS)
        }
    }
}
