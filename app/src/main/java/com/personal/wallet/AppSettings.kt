package com.personal.wallet

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import java.security.MessageDigest
import java.security.SecureRandom

/** تنظیمات برنامه در SharedPreferences؛ مقدارها State هستند و UI خودکار به‌روز می‌شود. */
class AppSettings(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences("wallet_settings", Context.MODE_PRIVATE)

    var hideAmounts: Boolean by mutableStateOf(prefs.getBoolean("hide", false))
        private set
    var useJalali: Boolean by mutableStateOf(prefs.getBoolean("jalali", true))
        private set
    var baseCurrency: String by mutableStateOf(prefs.getString("base", "IRR") ?: "IRR")
        private set
    var fingerprint: Boolean by mutableStateOf(prefs.getBoolean("fingerprint", false))
        private set
    private var pinHash: String by mutableStateOf(prefs.getString("pin_hash", "") ?: "")

    val pinEnabled: Boolean get() = pinHash.isNotEmpty()

    init {
        CalendarPrefs.jalali = useJalali
    }

    fun updateHideAmounts(v: Boolean) { hideAmounts = v; prefs.edit().putBoolean("hide", v).apply() }
    fun updateJalali(v: Boolean) { useJalali = v; CalendarPrefs.jalali = v; prefs.edit().putBoolean("jalali", v).apply() }
    fun updateBaseCurrency(code: String) { baseCurrency = code; prefs.edit().putString("base", code).apply() }
    fun updateFingerprint(v: Boolean) { fingerprint = v; prefs.edit().putBoolean("fingerprint", v).apply() }

    fun setPin(pin: String) {
        val salt = ByteArray(8).also { SecureRandom().nextBytes(it) }.joinToString("") { "%02x".format(it) }
        val h = salt + ":" + hash(salt, pin)
        pinHash = h
        prefs.edit().putString("pin_hash", h).apply()
    }

    fun clearPin() {
        pinHash = ""
        fingerprint = false
        prefs.edit().remove("pin_hash").putBoolean("fingerprint", false).apply()
    }

    fun checkPin(pin: String): Boolean {
        if (pinHash.isEmpty()) return true
        val parts = pinHash.split(":")
        if (parts.size != 2) return false
        return hash(parts[0], pin) == parts[1]
    }

    private fun hash(salt: String, pin: String): String {
        val md = MessageDigest.getInstance("SHA-256")
        return md.digest((salt + pin).toByteArray()).joinToString("") { "%02x".format(it) }
    }
}

/** نوع تقویم برای تابع‌های قالب‌بندی که خارج از Compose صدا زده می‌شوند */
object CalendarPrefs {
    @Volatile var jalali: Boolean = true
}
