package com.phoenix.qiur.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import com.phoenix.qiur.data.model.InputSource
import com.phoenix.qiur.data.model.UserSettings
import com.phoenix.qiur.utils.Constants
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class PreferencesStore(
    private val dataStore: DataStore<Preferences>
) {

    fun getSettings() : Flow<UserSettings> = dataStore.data.map { preferences ->
        val askCamera = preferences[Constants.PREFS_PERMISSION]
        val source = preferences[Constants.PREFS_SOURCE]

        UserSettings(
            askCamera = askCamera ?: true,
            source = if (source == null || source == 0) InputSource.PICTURE else InputSource.CAMERA
        )
    }

    suspend fun dismissCameraPopup() {
        dataStore.edit { preferences ->
            preferences[Constants.PREFS_PERMISSION] = false
        }
    }

    suspend fun changeInputSource(inputSource: InputSource) {
        dataStore.edit { preferences ->
            preferences[Constants.PREFS_SOURCE] = when (inputSource) {
                InputSource.STANDBY -> 0
                InputSource.PICTURE -> 0
                InputSource.CAMERA -> 1
            }
        }
    }

}