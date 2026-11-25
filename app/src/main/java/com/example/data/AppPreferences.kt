package com.example.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.model.SavedMeasurementRecord
import com.example.model.SavedSurveyProject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "bhumi_survey_prefs")

class AppPreferences(private val context: Context) {

    private val json = Json { ignoreUnknownKeys = true }

    companion object {
        private val KEY_DARK_MODE = booleanPreferencesKey("dark_mode_enabled")
        private val KEY_DEV_PHOTO_URI = stringPreferencesKey("dev_profile_photo_uri")
        private val KEY_SAVED_RECORDS = stringPreferencesKey("saved_measurement_records")
        private val KEY_SAVED_PROJECTS = stringPreferencesKey("saved_survey_projects")
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

    val savedProjectsFlow: Flow<List<SavedSurveyProject>> = context.dataStore.data.map { prefs ->
        val raw = prefs[KEY_SAVED_PROJECTS] ?: return@map emptyList()
        runCatching { json.decodeFromString<List<SavedSurveyProject>>(raw) }.getOrDefault(emptyList())
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

    suspend fun saveSurveyProject(project: SavedSurveyProject) {
        context.dataStore.edit { prefs ->
            val current = prefs[KEY_SAVED_PROJECTS]?.let {
                runCatching { json.decodeFromString<List<SavedSurveyProject>>(it) }.getOrNull()
            } ?: emptyList()
            val updated = (listOf(project) + current.filterNot { it.id == project.id }).take(50)
            prefs[KEY_SAVED_PROJECTS] = json.encodeToString(updated)
        }
    }

    suspend fun getSurveyProjectById(id: String): SavedSurveyProject? {
        return savedProjectsFlow.first().firstOrNull { it.id == id }
    }

    suspend fun deleteSurveyProject(id: String) {
        var orphanDocPath: String? = null
        context.dataStore.edit { prefs ->
            val current = prefs[KEY_SAVED_PROJECTS]?.let {
                runCatching { json.decodeFromString<List<SavedSurveyProject>>(it) }.getOrNull()
            } ?: emptyList()
            val target = current.firstOrNull { it.id == id }
            val remaining = current.filterNot { it.id == id }
            if (target?.docFilePath != null && remaining.none { it.docFilePath == target.docFilePath }) {
                orphanDocPath = target.docFilePath
            }
            prefs[KEY_SAVED_PROJECTS] = json.encodeToString(remaining)
        }
        orphanDocPath?.let { path ->
            runCatching { File(path).delete() }
        }
    }

    suspend fun clearHistory() {
        context.dataStore.edit { prefs ->
            prefs.remove(KEY_SAVED_RECORDS)
        }
    }
}
