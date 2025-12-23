package com.mobil.healthmate.data.local.manager

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ImageStorageManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    // Resmi kaydeder ve dosya yolunu (String) döner
    suspend fun saveProfileImage(uri: Uri, uid: String): String {
        return withContext(Dispatchers.IO) {
            try {
                // 1. Uri'den Bitmap oluştur (Galeriden okuma)
                val inputStream = context.contentResolver.openInputStream(uri)
                val bitmap = BitmapFactory.decodeStream(inputStream)
                inputStream?.close()

                // 2. Kaydedilecek dosya yolu: files/profile_images/user_{uid}.jpg
                val directory = File(context.filesDir, "profile_images")
                if (!directory.exists()) directory.mkdirs() // Klasör yoksa oluştur

                val fileName = "user_$uid.jpg"
                val file = File(directory, fileName)

                // 3. Dosyayı yaz (Sıkıştırma)
                val outputStream = FileOutputStream(file)
                bitmap.compress(Bitmap.CompressFormat.JPEG, 80, outputStream)
                outputStream.flush()
                outputStream.close()

                // 4. Dosyanın tam yolunu dön
                file.absolutePath
            } catch (e: Exception) {
                e.printStackTrace()
                ""
            }
        }
    }

    // Kayıtlı resmin dosyasını getirir
    fun getProfileImageFile(uid: String): File? {
        val directory = File(context.filesDir, "profile_images")
        val file = File(directory, "user_$uid.jpg")
        return if (file.exists()) file else null
    }
}