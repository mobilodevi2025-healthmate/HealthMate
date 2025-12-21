package com.mobil.healthmate.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.firestore.FirebaseFirestore
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
    private val repository: AuthRepository,
    private val firestore: FirebaseFirestore // AppModule'da tanımlıydı, buraya enjekte ediyoruz
) : ViewModel() {

    private val _state = MutableStateFlow(AuthState())
    val state: StateFlow<AuthState> = _state.asStateFlow()

    // Init bloğunu kaldırdık veya değiştirdik.
    // Çünkü uygulama açılış kontrolünü zaten MainActivity'de yapıyoruz.
    // Login ekranına düştüyse sıfırdan giriş yapmalı.

    fun signIn(idToken: String) {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }

            when (val result = repository.signInWithGoogle(idToken)) {
                is Resource.Success -> {
                    val user = result.data
                    if (user != null) {
                        checkUserInFirestore(user.uid)
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

    private fun checkUserInFirestore(uid: String) {
        firestore.collection("users").document(uid).get()
            .addOnSuccessListener { document ->
                val exists = document.exists()

                _state.update {
                    it.copy(
                        isLoading = false,
                        user = repository.getCurrentUser(),
                        isUserExisting = exists
                    )
                }
            }
            .addOnFailureListener { e ->
                _state.update {
                    it.copy(isLoading = false, error = "Profil kontrolü başarısız: ${e.message}")
                }
            }
    }
}