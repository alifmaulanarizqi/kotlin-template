package com.example.kotlintemplate.ui.feature.home

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import com.example.kotlintemplate.core.common.UiState
import com.example.kotlintemplate.domain.model.Sample

@Composable
fun HomeScreen(
    state: UiState<List<Sample>>,
    onRetry: () -> Unit
) {
    when (state) {
        is UiState.Idle -> Text("Idle")
        is UiState.Loading -> CircularProgressIndicator()
        is UiState.Success -> {
            LazyColumn {
                items(state.data) { sample ->
                    Text(sample.title)
                }
            }
        }
        is UiState.Error -> {
            Column {
                Text("Error: ${state.message}")
                Button(onClick = onRetry) { Text("Retry") }
            }
        }
    }
}
