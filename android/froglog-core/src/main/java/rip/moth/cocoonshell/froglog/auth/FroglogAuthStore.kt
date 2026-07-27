package rip.moth.cocoonshell.froglog.auth

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import rip.moth.cocoonshell.froglog.FroglogAuthState

class FroglogAuthStore(context: Context) {
    private val app = context.applicationContext
    private val prefs by lazy {
        val masterKey = MasterKey.Builder(app)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
        EncryptedSharedPreferences.create(
            app,
            PREFS,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
        )
    }

    fun saveToken(token: String, username: String) {
        prefs.edit()
            .putString(KEY_TOKEN, token)
            .putString(KEY_USERNAME, username)
            .apply()
    }

    fun clear() {
        prefs.edit().clear().apply()
    }

    fun token(): String? = prefs.getString(KEY_TOKEN, null)

    fun username(): String? = prefs.getString(KEY_USERNAME, null)

    fun authState(): FroglogAuthState {
        val user = username()
        val token = token()
        return FroglogAuthState(
            isSignedIn = !token.isNullOrBlank() && !user.isNullOrBlank(),
            username = user,
        )
    }

    fun wifiOnly(): Boolean = prefs.getBoolean(KEY_WIFI_ONLY, true)

    fun setWifiOnly(value: Boolean) {
        prefs.edit().putBoolean(KEY_WIFI_ONLY, value).apply()
    }

    companion object {
        private const val PREFS = "froglog_auth"
        private const val KEY_TOKEN = "jwt"
        private const val KEY_USERNAME = "username"
        private const val KEY_WIFI_ONLY = "wifi_only"
    }
}
