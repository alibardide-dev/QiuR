package com.phoenix.qiur.di

import com.phoenix.qiur.screen.home.HomeViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val appModule = module {
    viewModel { HomeViewModel() }
}