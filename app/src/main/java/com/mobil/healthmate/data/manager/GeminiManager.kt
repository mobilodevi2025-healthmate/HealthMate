package com.mobil.healthmate.data.manager

import com.google.ai.client.generativeai.GenerativeModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

class GeminiManager @Inject constructor() {

    private val apiKey = "your_gemini_api_key"

    private val generativeModel = GenerativeModel(
        modelName = "gemini-2.5-flash-lite",
        apiKey = apiKey
    )

    suspend fun generateDietRecommendation(
        userStats: String,
        mealsData: String
    ): String = withContext(Dispatchers.IO) {
        val prompt = """
            Sen profesyonel, motive edici ve samimi bir diyetisyensin.
            Aşağıda bir kullanıcının fiziksel özellikleri ve son 3 günde yediği yemeklerin listesi var.
            
            Kullanıcı Bilgileri:
            $userStats
            
            Yemek Geçmişi (Son 3 Gün):
            $mealsData
            
            Lütfen şunları yap:
            1. Kullanıcının beslenme düzenini kısaca analiz et (Örn: Karbonhidrat ağırlıklı mı, protein eksik mi?).
            2. Eğer yemek geçmişi boşsa, genel sağlıklı beslenme tavsiyeleri ver.
            3. Kullanıcının hedefine ulaşması için 3 adet kısa, uygulanabilir ve nokta atışı tavsiye ver.
            4. Cevabı Türkçe ver ve emoji kullanarak metni canlı tut.
            5. Cevap çok uzun olmasın, mobil ekranda okunabilir olsun.
        """.trimIndent()

        try {
            val response = generativeModel.generateContent(prompt)
            response.text ?: "Şu an tavsiye oluşturulamadı. Lütfen tekrar dene."
        } catch (e: Exception) {
            "Bağlantı hatası: ${e.localizedMessage}. İnternetini kontrol et."
        }
    }
}