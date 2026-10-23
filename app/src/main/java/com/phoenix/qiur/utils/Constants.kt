package com.phoenix.qiur.utils

import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey

object Constants {

    const val KEY_PREFS = "QiuR-prefs"
    val PREFS_PERMISSION = booleanPreferencesKey("askCamera")
    val PREFS_SOURCE = intPreferencesKey("source")

    const val DATE_ALL_DAY = "yyyyMMdd"
    const val DATE_UTC = "yyyyMMdd'T'HHmmss'Z'"
    const val DATE_LOCAL = "yyyyMMdd'T'HHmmss"

}