package com.mobil.healthmate.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.mobil.healthmate.domain.repository.HealthRepository
import com.mobil.healthmate.util.NotificationHelper
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@AndroidEntryPoint
class NotificationReceiver : BroadcastReceiver() {

    @Inject
    lateinit var repository: HealthRepository

    @Inject
    lateinit var auth: FirebaseAuth

    override fun onReceive(context: Context, intent: Intent) {
        val type = intent.getStringExtra("TYPE") ?: return

        Log.d("ALARM_TEST", "Alarm Alındı! Tür: $type")

        val notificationHelper = NotificationHelper(context)
        val uid = auth.currentUser?.uid

        if (uid == null) {
            Log.e("ALARM_TEST", "Kullanıcı giriş yapmamış, bildirim iptal.")
            return
        }

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val summary = try { repository.getTodaySummary(uid) } catch (e: Exception) { null }
                val goal = try { repository.getCurrentGoal(uid) } catch (e: Exception) { null }

                withContext(Dispatchers.Main) {
                    when (type) {
                        "MEAL_CHECK" -> {
                            if (summary != null && goal != null) {
                                val consumed = summary.totalCaloriesConsumed
                                val target = goal.dailyCalorieTarget ?: 2000
                                // Hedefin %20'sinden az tüketildiyse uyar
                                if (consumed < (target * 0.20)) {
                                    notificationHelper.showNotification(
                                        "Enerjin Düşüyor ⚡",
                                        "Öğün atlamış gibi görünüyorsun. Hedefin için yakıt almayı unutma!",
                                        101
                                    )
                                }
                            }
                        }

                        "STEP_CHECK" -> {
                            if (summary != null && goal != null) {
                                val steps = summary.totalSteps
                                val targetSteps = goal.dailyStepTarget ?: 10000

                                if (steps in (targetSteps / 2) until targetSteps) {
                                    notificationHelper.showNotification(
                                        "Çok Az Kaldı! 👣",
                                        "Hedefine ulaşmana ${(targetSteps - steps)} adım kaldı. Küçük bir yürüyüş?",
                                        102
                                    )
                                } else if (steps < (targetSteps / 2)) {
                                    notificationHelper.showNotification(
                                        "Harekete Geç 🏃‍♂️",
                                        "Bugün biraz hareketsiz kaldın. Sağlığın için kısa bir yürüyüş yapabilirsin.",
                                        102
                                    )
                                }
                            }
                        }

                        "WATER" -> {
                            // Su bildirimi veritabanı verisine (summary/goal) bağlı DEĞİLDİR.
                            // Bu yüzden direkt gösteriyoruz.
                            notificationHelper.showNotification(
                                "Su İçme Vakti 💧",
                                "Metabolizmanı canlı tutmak için bir bardak su iç.",
                                103
                            )
                        }

                        "SLEEP" -> {
                            notificationHelper.showNotification(
                                "Uyku Vakti 😴",
                                "Yarın zinde uyanmak için şimdi uyuma zamanı.",
                                104
                            )
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e("ALARM_TEST", "Receiver Hatası: ${e.message}")
                e.printStackTrace()
            }
        }
    }
}