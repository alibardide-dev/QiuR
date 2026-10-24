package com.phoenix.qiur.di

import com.phoenix.qiur.data.DataStoreManager
import com.phoenix.qiur.screen.about.AboutViewModel
import com.phoenix.qiur.screen.home.HomeViewModel
import com.phoenix.qiur.screen.setting.SettingViewModel
import com.phoenix.qiur.utils.UpdateStateHolder
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val dataModule = module {
    single { DataStoreManager(androidContext()) }
    single { UpdateStateHolder(get<DataStoreManager>()) }
}

val viewModelModule = module {
    viewModel { HomeViewModel(get<DataStoreManager>(), get<UpdateStateHolder>()) }
    viewModel { AboutViewModel(get<UpdateStateHolder>()) }
    viewModel { SettingViewModel(get<DataStoreManager>(), get<UpdateStateHolder>()) }
}