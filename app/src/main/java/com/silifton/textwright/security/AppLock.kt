package com.silifton.textwright.security

import android.content.Context
import android.content.SharedPreferences
import android.util.Base64
import androidx.biometric.BiometricManager
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

/**
 * Optional lock in front of the app: a fingerprint, a pattern, or both (either one then unlocks).
 * Only a salted hash of the pattern is stored. This keeps someone holding the unlocked phone out of the app;
 * it does not encrypt messages, which stay in the system SMS store.
 */
object AppLock {

    const val MIN_DOTS = 4
    private const val MAX_FAILS = 5
    private const val LOCKOUT_MS = 30_000L

    private const val PREFS = "app_lock"
    private const val KEY_SALT = "salt"
    private const val KEY_HASH = "hash"
    private const val KEY_BIOMETRIC = "biometric"
    private const val KEY_FAILS = "fails"
    private const val KEY_RETRY_AT = "retry_at"

    var locked by mutableStateOf(false)
        private set

    /** True once the fingerprint prompt has been offered for the current lock, so it is not shown twice. */
    var prompted = false

    /** Set when the user asks for the pattern pad although fingerprint unlock is on. */
    var patternRequested by mutableStateOf(false)

    private var launched = false

    /** Locks on the first activity start of the process; later starts (rotation) keep the current state. */
    fun onLaunch(context: Context) {
        if (launched) return
        launched = true
        locked = enabled(context)
    }

    fun lock(context: Context) {
        if (!enabled(context)) return
        locked = true
        prompted = false
        patternRequested = false
    }

    fun unlock() {
        locked = false
    }

    /** The lock is on when there is at least one way to unlock: a usable fingerprint or a pattern. */
    fun enabled(context: Context): Boolean = hasPattern(context) || biometricEnabled(context)

    fun hasPattern(context: Context): Boolean = prefs(context).contains(KEY_HASH)

    fun setPattern(context: Context, pattern: List<Int>) {
        val salt = ByteArray(16).also { SecureRandom().nextBytes(it) }
        prefs(context).edit()
            .putString(KEY_SALT, encode(salt))
            .putString(KEY_HASH, encode(hash(pattern, salt)))
            .remove(KEY_FAILS)
            .remove(KEY_RETRY_AT)
            .apply()
    }

    /** Forgets the pattern. The lock stays on if fingerprint unlock is on. */
    fun removePattern(context: Context) {
        prefs(context).edit().remove(KEY_SALT).remove(KEY_HASH).remove(KEY_FAILS).remove(KEY_RETRY_AT).apply()
    }

    /** Slow on purpose (key stretching): call off the main thread. Wrong attempts count towards a lockout. */
    fun verify(context: Context, pattern: List<Int>): Boolean {
        val prefs = prefs(context)
        if (System.currentTimeMillis() < retryAt(context)) return false
        val salt = decode(prefs.getString(KEY_SALT, null)) ?: return false
        val stored = decode(prefs.getString(KEY_HASH, null)) ?: return false
        if (MessageDigest.isEqual(hash(pattern, salt), stored)) {
            prefs.edit().remove(KEY_FAILS).remove(KEY_RETRY_AT).commit()
            return true
        }
        val fails = prefs.getInt(KEY_FAILS, 0) + 1
        if (fails >= MAX_FAILS) {
            prefs.edit().putInt(KEY_FAILS, 0).putLong(KEY_RETRY_AT, System.currentTimeMillis() + LOCKOUT_MS).commit()
        } else {
            prefs.edit().putInt(KEY_FAILS, fails).commit()
        }
        return false
    }

    /** Wall-clock time before which pattern attempts are refused, or 0. */
    fun retryAt(context: Context): Long = prefs(context).getLong(KEY_RETRY_AT, 0)

    /** False again if every fingerprint is later removed from the phone, so that cannot lock the user out. */
    fun biometricEnabled(context: Context): Boolean =
        prefs(context).getBoolean(KEY_BIOMETRIC, false) && canUseBiometric(context)

    fun setBiometricEnabled(context: Context, enabled: Boolean) {
        prefs(context).edit().putBoolean(KEY_BIOMETRIC, enabled).apply()
    }

    /** True when the device has a fingerprint (or other biometric) enrolled. */
    fun canUseBiometric(context: Context): Boolean =
        BiometricManager.from(context).canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_WEAK) ==
            BiometricManager.BIOMETRIC_SUCCESS

    private fun hash(pattern: List<Int>, salt: ByteArray): ByteArray {
        val spec = PBEKeySpec(pattern.joinToString("").toCharArray(), salt, 60_000, 256)
        return SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).encoded
    }

    private fun prefs(context: Context): SharedPreferences =
        context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    private fun encode(bytes: ByteArray): String = Base64.encodeToString(bytes, Base64.NO_WRAP)

    private fun decode(text: String?): ByteArray? = text?.let { Base64.decode(it, Base64.NO_WRAP) }
}
