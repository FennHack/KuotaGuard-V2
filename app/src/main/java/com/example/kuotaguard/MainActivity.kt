package com.example.kuotaguard

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.TrafficStats
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.google.android.material.button.MaterialButton
import com.google.android.material.materialswitch.MaterialSwitch
import com.google.android.material.slider.Slider

class MainActivity : AppCompatActivity() {

    private val handler = Handler(Looper.getMainLooper())
    private var last = -1L

    private lateinit var tvState: TextView
    private lateinit var tvSpeed: TextView
    private lateinit var tvTotal: TextView
    private lateinit var tvSens: TextView
    private lateinit var tvSecs: TextView
    private lateinit var btnToggle: MaterialButton

    private val askNotif =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { startMonitor() }

    private val ticker = object : Runnable {
        override fun run() {
            updateStats()
            refreshState()
            handler.postDelayed(this, 1000)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        tvState = findViewById(R.id.tvState)
        tvSpeed = findViewById(R.id.tvSpeed)
        tvTotal = findViewById(R.id.tvTotal)
        tvSens = findViewById(R.id.tvSens)
        tvSecs = findViewById(R.id.tvSecs)
        btnToggle = findViewById(R.id.btnToggle)

        val sliderSens = findViewById<Slider>(R.id.sliderSens)
        val sliderSecs = findViewById<Slider>(R.id.sliderSecs)
        val swVibrate = findViewById<MaterialSwitch>(R.id.swVibrate)
        val swBoot = findViewById<MaterialSwitch>(R.id.swBoot)

        sliderSens.value = Prefs.sensKb(this).toFloat()
        sliderSecs.value = Prefs.seconds(this).toFloat()
        swVibrate.isChecked = Prefs.vibrate(this)
        swBoot.isChecked = Prefs.boot(this)
        updateLabels()

        sliderSens.addOnChangeListener { _, v, _ -> Prefs.setSensKb(this, v.toInt()); updateLabels() }
        sliderSecs.addOnChangeListener { _, v, _ -> Prefs.setSeconds(this, v.toInt()); updateLabels() }
        swVibrate.setOnCheckedChangeListener { _, on -> Prefs.setVibrate(this, on) }
        swBoot.setOnCheckedChangeListener { _, on -> Prefs.setBoot(this, on) }

        btnToggle.setOnClickListener {
            if (MonitorService.running) {
                Prefs.setEnabled(this, false)
                stopService(Intent(this, MonitorService::class.java))
            } else {
                ensurePermissionThenStart()
            }
            refreshState()
        }

        findViewById<MaterialButton>(R.id.btnTest).setOnClickListener {
            val granted = ContextCompat.checkSelfPermission(
                this, Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
            if (granted) {
                AlertHelper.fire(this, "Ini hanya tes notifikasi dan getar.")
            } else {
                askNotif.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        last = -1L
        handler.post(ticker)
    }

    override fun onPause() {
        handler.removeCallbacks(ticker)
        super.onPause()
    }

    private fun updateLabels() {
        tvSens.text = "Sensitivitas: di atas ${Prefs.sensKb(this)} KB/detik dianggap memakai kuota"
        tvSecs.text = "Durasi: ${Prefs.seconds(this)} detik berturut-turut"
    }

    private fun updateStats() {
        val total = TrafficStats.getMobileRxBytes() + TrafficStats.getMobileTxBytes()
        if (total < 0) {
            tvSpeed.text = "Statistik data seluler tidak didukung"
            tvTotal.text = ""
            return
        }
        val d = if (last < 0) 0L else total - last
        last = total
        tvSpeed.text = "Kecepatan saat ini: %.1f KB/detik".format(d / 1024.0)
        tvTotal.text = "Total data seluler sejak HP menyala: %.1f MB".format(total / 1048576.0)
    }

    private fun refreshState() {
        if (MonitorService.running) {
            tvState.text = "● AKTIF - sedang memantau"
            tvState.setTextColor(0xFF00BFA5.toInt())
            btnToggle.text = "Matikan pemantau"
        } else {
            tvState.text = "● MATI"
            tvState.setTextColor(0xFFE53935.toInt())
            btnToggle.text = "Aktifkan pemantau"
        }
    }

    private fun ensurePermissionThenStart() {
        val granted = ContextCompat.checkSelfPermission(
            this, Manifest.permission.POST_NOTIFICATIONS
        ) == PackageManager.PERMISSION_GRANTED
        if (granted) startMonitor() else askNotif.launch(Manifest.permission.POST_NOTIFICATIONS)
    }

    private fun startMonitor() {
        Prefs.setEnabled(this, true)
        ContextCompat.startForegroundService(this, Intent(this, MonitorService::class.java))
        refreshState()
    }
}
