package com.mobil.healthmate.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mobil.healthmate.domain.repository.AuthRepository
import com.mobil.healthmate.util.Resource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val repository: AuthRepository
) : ViewModel() {

    private val _state = MutableStateFlow(AuthState())
    val state: StateFlow<AuthState> = _state.asStateFlow()

    init {
        val currentUser = repository.getCurrentUser()
        if (currentUser != null) {
            _state.update { it.copy(user = currentUser) }
        }
    }

    fun signIn(idToken: String) {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }

            when (val result = repository.signInWithGoogle(idToken)) {
                is Resource.Success -> {
                    _state.update {
                        it.copy(
                            isLoading = false,
                            user = result.data,
                            error = null
                        )
                    }
                }
                is Resource.Error -> {
                    _state.update {
                        it.copy(
                            isLoading = false,
                            error = result.message ?: "Bilinmeyen hata"
                        )
                    }
                }
                is Resource.Loading -> {
                    // Loading durumu zaten yukarıda set edildi
                }
            }
        }
    }
}