package com.mobil.healthmate.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mobil.healthmate.domain.repository.AuthRepository
import com.mobil.healthmate.domain.repository.SyncRepository
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
    private val authRepository: AuthRepository,
    private val syncRepository: SyncRepository
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
                        handleUserNavigationAndSync(user.uid)
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

    private fun handleUserNavigationAndSync(uid: String) {
        viewModelScope.launch {
            try {
                val isProfileRestored = syncRepository.downloadUserProfile(uid)

                if (isProfileRestored) {
                    syncRepository.triggerSync()
                }

                _state.update {
                    it.copy(
                        isLoading = false,
                        user = authRepository.getCurrentUser(),
                        isUserExisting = isProfileRestored
                    )
                }
            } catch (e: Exception) {
                _state.update {
                    it.copy(
                        isLoading = false,
                        error = "Senkronizasyon başlatılamadı: ${e.localizedMessage}"
                    )
                }
            }
        }
    }
}