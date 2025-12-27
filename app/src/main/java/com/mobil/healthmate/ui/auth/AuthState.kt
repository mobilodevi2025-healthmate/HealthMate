package com.mobil.healthmate.ui.auth

import com.google.firebase.auth.FirebaseUser

data class AuthState(
    val isLoading: Boolean = false,
    val user: FirebaseUser? = null,
    val error: String? = null,
    val isUserExisting: Boolean? = null,
    val loadingMessage: String = "İşlem yapılıyor...", // YENİ: Kullanıcıya bilgi vermek için

)