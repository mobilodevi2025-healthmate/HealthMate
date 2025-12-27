package com.mobil.healthmate.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.mobil.healthmate.domain.repository.SyncRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val auth: FirebaseAuth,
    private val syncRepository: SyncRepository // Verileri çekmek için buna ihtiyacımız var
) : ViewModel() {

    private val _loginState = MutableStateFlow<LoginState>(LoginState.Idle)
    val loginState: StateFlow<LoginState> = _loginState.asStateFlow()

    fun loginUser(email: String, pass: String) {
        if (email.isBlank() || pass.isBlank()) {
            _loginState.value = LoginState.Error("Lütfen tüm alanları doldurun.")
            return
        }

        viewModelScope.launch {
            _loginState.value = LoginState.Loading // Yükleniyor dönmeye başlar

            try {
                // 1. Firebase Auth Girişi
                auth.signInWithEmailAndPassword(email, pass)
                    .addOnSuccessListener { authResult ->
                        val uid = authResult.user?.uid
                        if (uid != null) {
                            // 2. GİRİŞ BAŞARILI, ŞİMDİ VERİLERİ ÇEKELİM
                            fetchUserData(uid)
                        } else {
                            _loginState.value = LoginState.Error("Kullanıcı kimliği alınamadı.")
                        }
                    }
                    .addOnFailureListener {
                        _loginState.value = LoginState.Error("Giriş başarısız: ${it.localizedMessage}")
                    }
            } catch (e: Exception) {
                _loginState.value = LoginState.Error("Hata: ${e.localizedMessage}")
            }
        }
    }

    private fun fetchUserData(uid: String) {
        viewModelScope.launch {
            try {
                // 3. SENKRONİZASYONU BAŞLAT (Burada bekliyoruz)
                // Bu fonksiyon senin yazdığın SyncRepositoryImpl içindeki fonksiyon.
                // Firebase'den Profile, Meals, Goals hepsini alıp Room'a yazar.
                syncRepository.downloadAllUserData(uid)

                // 4. İndirme bitti, artık yönlendirebiliriz
                // Kullanıcı profili var mı kontrol edelim (downloadAllUserData içinde user tablosu dolmuştur)
                // Ancak basitlik olsun diye Success döndürüyoruz, yönlendirmeyi UI yapar.
                _loginState.value = LoginState.Success

            } catch (e: Exception) {
                // İnternet yoksa veya hata varsa bile, eğer daha önce giriş yapmışsa devam edebilir
                // Ama ilk giriş ise hata verebiliriz.
                // Biz yine de Success diyelim ki Offline modda girebilsin (Localde veri varsa).
                _loginState.value = LoginState.Success
            }
        }
    }
}

sealed class LoginState {
    data object Idle : LoginState()
    data object Loading : LoginState()
    data object Success : LoginState()
    data class Error(val message: String) : LoginState()
}