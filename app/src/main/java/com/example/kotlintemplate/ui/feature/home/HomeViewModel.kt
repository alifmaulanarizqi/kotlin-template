package com.example.kotlintemplate.ui.feature.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.kotlintemplate.domain.usecase.SampleUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val sampleUseCase: SampleUseCase
) : ViewModel() {

    var state = androidx.compose.runtime.mutableStateOf(HomeState())
        private set

    init {
        load()
    }

    fun load() {
        viewModelScope.launch {
            state.value = state.value.copy(loading = true, error = null)

            runCatching { sampleUseCase() }
                .onSuccess { samples ->
                    state.value = state.value.copy(
                        loading = false,
                        samples = samples
                    )
                }
                .onFailure { e ->
                    state.value = state.value.copy(
                        loading = false,
                        error = e.message ?: "Unknown error"
                    )
                }
        }
    }
}