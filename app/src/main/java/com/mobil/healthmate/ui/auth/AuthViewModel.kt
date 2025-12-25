package com.mobil.healthmate.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mobil.healthmate.domain.repository.AuthRepository
import com.mobil.healthmate.util.Resource
import dagger.hilt.android.lifecycle.HiltViewModel
import com.mobil.healthmate.domain.repository.HealthRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val healthRepository: HealthRepository
) : ViewModel() {

    private val _state = MutableStateFlow(AuthState())
    val state: StateFlow<AuthState> = _state.asStateFlow()

    fun signIn(idToken: String) {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }

            when (val result = authRepository.signInWithGoogle(idToken)) {
                is Resource.Success -> {
                    val user = result.data
                    if (user != null) {
                        checkUserAndRestore(user.uid)
                    } else {
                        _state.update { it.copy(isLoading = false, error = "Kullanıcı bilgisi alınamadı") }
                    }
                }
                is Resource.Error -> {
                    _state.update {
                        it.copy(
                            isLoading = false,
                            error = result.message ?: "Giriş başarısız"
                        )
                    }
                }
                is Resource.Loading -> {}
            }
        }
    }

    private fun checkUserAndRestore(uid: String) {
        viewModelScope.launch {
            try {
                val isRestored = healthRepository.restoreUserProfileFromCloud(uid)

                _state.update {
                    it.copy(
                        isLoading = false,
                        user = authRepository.getCurrentUser(),
                        isUserExisting = isRestored // True ise Home, False ise CreateProfile
                    )
                }
            } catch (e: Exception) {
                _state.update {
                    it.copy(
                        isLoading = false,
                        error = "Profil senkronizasyon hatası: ${e.localizedMessage}"
                    )
                }
            }
        }
    }
}