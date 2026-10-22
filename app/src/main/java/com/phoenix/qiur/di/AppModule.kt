package com.phoenix.qiur.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore
import com.phoenix.qiur.data.PreferencesStore
import com.phoenix.qiur.screen.home.HomeViewModel
import com.phoenix.qiur.utils.Constants
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(Constants.KEY_PREFS)
val dataModule = module {
    single { PreferencesStore(androidContext().dataStore) }
}

val viewModelModule = module {
    viewModel { HomeViewModel(get<PreferencesStore>()) }
}