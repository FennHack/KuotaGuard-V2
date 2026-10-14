package com.example.kuotaguard

import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.net.TrafficStats
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import androidx.core.app.NotificationCompat

class MonitorService : Service() {

    companion object {
        @Volatile var running = false
        private const val NOTIF_STATUS = 1
        private const val COOLDOWN_MS = 60_000L
    }

    private val handler = Handler(Looper.getMainLooper())
    private var lastBytes = 0L
    private var streak = 0
    private var streakBytes = 0L
    private var lastAlert = 0L

    private val tick = object : Runnable {
        override fun run() {
            val now = TrafficStats.getMobileRxBytes() + TrafficStats.getMobileTxBytes()
            if (now < 0) { stopSelf(); return }

            val delta = now - lastBytes
            lastBytes = now

            val threshold = Prefs.sensKb(this@MonitorService) * 1024L
            val needed = Prefs.seconds(this@MonitorService)

            if (delta > threshold) {
                streak++
                streakBytes += delta
            } else {
                streak = 0
                streakBytes = 0
            }

            val t = System.currentTimeMillis()
            if (streak >= needed && t - lastAlert > COOLDOWN_MS) {
                val avgKb = streakBytes / streak / 1024.0
                AlertHelper.fire(
                    this@MonitorService,
                    "Data seluler terpakai sekitar %.0f KB/detik selama %d detik. Matikan data dan pakai Wi-Fi?"
                        .format(avgKb, streak)
                )
                lastAlert = t
                streak = 0
                streakBytes = 0
            }
            handler.postDelayed(this, 1000)
        }
    }

    override fun onCreate() {
        super.onCreate()
        running = true
        AlertHelper.ensureChannels(this)

        val open = PendingIntent.getActivity(
            this, 1, Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        val status = NotificationCompat.Builder(this, AlertHelper.CH_STATUS)
            .setSmallIcon(android.R.drawable.stat_sys_download)
            .setContentTitle("KuotaGuard aktif")
            .setContentText("Memantau pemakaian data seluler")
            .setContentIntent(open)
            .setOngoing(true)
            .build()
        startForeground(NOTIF_STATUS, status)

        lastBytes = TrafficStats.getMobileRxBytes() + TrafficStats.getMobileTxBytes()
        handler.postDelayed(tick, 1000)
    }

    override fun onDestroy() {
        running = false
        handler.removeCallbacks(tick)
        super.onDestroy()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int) = START_STICKY
    override fun onBind(intent: Intent?): IBinder? = null
}
