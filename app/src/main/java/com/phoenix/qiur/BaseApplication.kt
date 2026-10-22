package com.phoenix.qiur

import android.app.Application
import com.phoenix.qiur.di.dataModule
import com.phoenix.qiur.di.viewModelModule
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.GlobalContext.startKoin

class BaseApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        startKoin {
            androidContext(this@BaseApplication)
            modules(dataModule, viewModelModule)
        }
    }
}