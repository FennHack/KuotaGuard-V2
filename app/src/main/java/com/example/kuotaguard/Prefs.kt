package com.example.kuotaguard

import android.content.Context

object Prefs {
    private fun sp(c: Context) = c.getSharedPreferences("kg", Context.MODE_PRIVATE)

    fun sensKb(c: Context) = sp(c).getInt("sens", 2)
    fun seconds(c: Context) = sp(c).getInt("secs", 10)
    fun vibrate(c: Context) = sp(c).getBoolean("vibrate", true)
    fun boot(c: Context) = sp(c).getBoolean("boot", true)
    fun enabled(c: Context) = sp(c).getBoolean("enabled", false)

    fun setSensKb(c: Context, v: Int) = sp(c).edit().putInt("sens", v).apply()
    fun setSeconds(c: Context, v: Int) = sp(c).edit().putInt("secs", v).apply()
    fun setVibrate(c: Context, v: Boolean) = sp(c).edit().putBoolean("vibrate", v).apply()
    fun setBoot(c: Context, v: Boolean) = sp(c).edit().putBoolean("boot", v).apply()
    fun setEnabled(c: Context, v: Boolean) = sp(c).edit().putBoolean("enabled", v).apply()
}
