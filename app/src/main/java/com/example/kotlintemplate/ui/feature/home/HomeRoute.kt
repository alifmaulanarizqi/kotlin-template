package com.example.kotlintemplate.ui.feature.home

import androidx.compose.runtime.Composable
import androidx.hilt.navigation.compose.hiltViewModel

@Composable
fun HomeRoute(
    viewModel: HomeViewModel = hiltViewModel()
) {
    HomeScreen(
        state = viewModel.state.value,
        onRetry = { viewModel.load() }
    )
}