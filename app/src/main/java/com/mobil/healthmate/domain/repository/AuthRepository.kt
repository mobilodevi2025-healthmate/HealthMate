package com.mobil.healthmate.domain.repository

import com.google.firebase.auth.FirebaseUser
import com.mobil.healthmate.util.Resource

interface AuthRepository {
    suspend fun signInWithGoogle(idToken: String): Resource<FirebaseUser>

    fun signOut()

    fun getCurrentUser(): FirebaseUser?
}