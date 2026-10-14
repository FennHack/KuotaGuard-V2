package com.example.kuotaguard

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.provider.Settings
import androidx.core.app.NotificationCompat

object AlertHelper {
    const val CH_STATUS = "status_v2"
    const val CH_ALERT = "alert_v2"
    private const val NOTIF_ALERT = 2

    // getar: tunda, getar, jeda, getar, jeda, getar panjang
    private val PATTERN = longArrayOf(0, 400, 150, 400, 150, 900)

    fun ensureChannels(ctx: Context) {
        val nm = ctx.getSystemService(NotificationManager::class.java)
        nm.createNotificationChannel(
            NotificationChannel(CH_STATUS, "Status pemantau", NotificationManager.IMPORTANCE_MIN)
        )
        nm.createNotificationChannel(
            NotificationChannel(CH_ALERT, "Peringatan kuota", NotificationManager.IMPORTANCE_HIGH).apply {
                enableVibration(false) // getar diatur sendiri lewat tombol di aplikasi
            }
        )
    }

    fun fire(ctx: Context, text: String) {
        ensureChannels(ctx)

        val panel = Intent(Settings.Panel.ACTION_INTERNET_CONNECTIVITY)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        val pi = PendingIntent.getActivity(
            ctx, 0, panel, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val n = NotificationCompat.Builder(ctx, CH_ALERT)
            .setSmallIcon(android.R.drawable.stat_notify_error)
            .setContentTitle("Kamu sedang memakai kuota!")
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setContentIntent(pi)
            .addAction(0, "Buka panel koneksi", pi)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .build()

        ctx.getSystemService(NotificationManager::class.java).notify(NOTIF_ALERT, n)

        if (Prefs.vibrate(ctx)) vibrate(ctx)
    }

    private fun vibrate(ctx: Context) {
        val v: Vibrator = if (Build.VERSION.SDK_INT >= 31) {
            ctx.getSystemService(VibratorManager::class.java).defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            ctx.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        }
        v.vibrate(VibrationEffect.createWaveform(PATTERN, -1))
    }
}
