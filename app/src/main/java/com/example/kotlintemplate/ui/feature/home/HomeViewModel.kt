package com.example.kotlintemplate.ui.feature.home

import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.kotlintemplate.core.common.UiState
import com.example.kotlintemplate.domain.model.Sample
import com.example.kotlintemplate.domain.usecase.SampleUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val sampleUseCase: SampleUseCase
) : ViewModel() {

    var state = mutableStateOf<UiState<List<Sample>>>(UiState.Idle)
        private set

    init {
        load()
    }

    fun load() {
        viewModelScope.launch {
            state.value = UiState.Loading

            runCatching { sampleUseCase() }
                .onSuccess { data ->
                    state.value = UiState.Success(data)
                }
                .onFailure { e ->
                    state.value = UiState.Error(e.message ?: "Unknown error")
                }
        }
    }
}