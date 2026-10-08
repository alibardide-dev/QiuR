package com.phoenix.qiur.screen.home

import androidx.compose.runtime.Composable
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun HomeRoute(
    homeViewModel: HomeViewModel = koinViewModel()
) {
    val uiState = homeViewModel.uiState.collectAsStateWithLifecycle()
    HomeScreen()
}