package com.mobil.healthmate.ui.ai_advice

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mobil.healthmate.domain.repository.HealthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AiAdviceViewModel @Inject constructor(
    private val repository: HealthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<AiUiState>(AiUiState.Idle)
    val uiState = _uiState.asStateFlow()

    fun getAdvice() {
        viewModelScope.launch {
            _uiState.value = AiUiState.Loading
            try {
                // Repository -> GeminiManager -> Google AI -> Cevap
                val response = repository.getAiRecommendation()
                _uiState.value = AiUiState.Success(response)
            } catch (e: Exception) {
                _uiState.value = AiUiState.Error(e.message ?: "Bilinmeyen hata")
            }
        }
    }
}

sealed class AiUiState {
    object Idle : AiUiState() // Beklemede
    object Loading : AiUiState() // Düşünüyor...
    data class Success(val message: String) : AiUiState() // Cevap geldi
    data class Error(val error: String) : AiUiState() // Hata
}