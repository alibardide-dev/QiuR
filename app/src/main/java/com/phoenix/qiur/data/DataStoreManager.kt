package com.phoenix.qiur.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.phoenix.qiur.data.model.InputSource
import com.phoenix.qiur.data.model.UserSettings
import com.phoenix.qiur.utils.Constants
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(Constants.KEY_PREFS)

class DataStoreManager(private val context: Context) {

    companion object {
        val UPDATE_AVAILABLE = booleanPreferencesKey(Constants.SETTING_UPDATE_AVAILABLE)
        val UPDATE_VERSION = stringPreferencesKey(Constants.SETTING_UPDATE_VERSION)
        val ASK_CAMERA_PERMISSION = booleanPreferencesKey(Constants.SETTING_ASK_CAMERA_PERMISSION)
        val LAST_INPUT_SOURCE = intPreferencesKey(Constants.SETTING_LAST_INPUT_SOURCE)
    }

    val settings: Flow<UserSettings> =
        context.dataStore.data.map { preferences ->
            val askCameraPermission = preferences[ASK_CAMERA_PERMISSION]
            val lastInputSource = preferences[LAST_INPUT_SOURCE]

            UserSettings(
                askCamera = askCameraPermission ?: true,
                source =
                    lastInputSource?.let {
                        if (lastInputSource == 0) InputSource.PICTURE
                        else InputSource.CAMERA
                    } ?: InputSource.PICTURE
            )
        }

    suspend fun setUpdateState(available: Boolean, version: String?) {
        context.dataStore.edit { prefs ->
            prefs[UPDATE_AVAILABLE] = available
            version?.let { prefs[UPDATE_VERSION] = version }
        }
    }

    val isUpdateAvailable: Flow<Boolean> = context.dataStore.data
        .map { prefs -> prefs[UPDATE_AVAILABLE] ?: false }

    val nextUpdateVersion: Flow<String> = context.dataStore.data
        .map { prefs -> prefs[UPDATE_VERSION] ?: "" }

    suspend fun dismissCameraPopup() =
        context.dataStore.edit { preferences ->
            preferences[ASK_CAMERA_PERMISSION] = false
        }

    suspend fun changeInputSource(inputSource: InputSource) {
        context.dataStore.edit { preferences ->
            preferences[LAST_INPUT_SOURCE] = when (inputSource) {
                InputSource.STANDBY -> 0
                InputSource.PICTURE -> 0
                InputSource.CAMERA -> 1
            }
        }
    }

}