package com.phoenix.qiur.di

import androidx.room3.Room
import androidx.sqlite.driver.AndroidSQLiteDriver
import com.phoenix.qiur.data.AppDatabase
import com.phoenix.qiur.data.DataStoreManager
import com.phoenix.qiur.data.dao.HistoryDao
import com.phoenix.qiur.screen.about.AboutViewModel
import com.phoenix.qiur.screen.home.HomeViewModel
import com.phoenix.qiur.utils.Constants
import com.phoenix.qiur.utils.UpdateStateHolder
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val dataModule = module {
    single {
        Room.databaseBuilder<AppDatabase>(androidContext(), Constants.DB_NAME)
            .setDriver(AndroidSQLiteDriver())
            .build()
    }
    single { get<AppDatabase>().historyDao() }
    single { DataStoreManager(androidContext()) }
    single { UpdateStateHolder(get<DataStoreManager>()) }
}

val viewModelModule = module {
    viewModel {
        HomeViewModel(
            get<DataStoreManager>(),
            get<UpdateStateHolder>(),
            get<HistoryDao>())
    }
    viewModel {
        AboutViewModel(get<UpdateStateHolder>())
    }
}