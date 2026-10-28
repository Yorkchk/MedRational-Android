package com.example.medrational_android.data.auth

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import java.io.IOException
import java.security.GeneralSecurityException
import java.security.KeyStore

class TokenManager(context: Context) : SessionStore {

    private val sharedPreferences: SharedPreferences = openRecovering(
        open = { openEncryptedPrefs(context) },
        // The stored keyset no longer matches its Keystore key, e.g. the prefs file came back from
        // a backup without the key. Dropping the file only signs the user out
        {
            Log.w(TAG, "Session storage can't be decrypted; resetting it")
            context.deleteSharedPreferences(PREFS_NAME)
        },
        // The Keystore key itself is unusable: start over with a new one
        {
            Log.w(TAG, "Session storage key is unusable; recreating it")
            KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
                .deleteEntry(MasterKey.DEFAULT_MASTER_KEY_ALIAS)
            context.deleteSharedPreferences(PREFS_NAME)
        }
    )

    private fun openEncryptedPrefs(context: Context): SharedPreferences {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()

        return EncryptedSharedPreferences.create(
            context,
            PREFS_NAME,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    fun saveToken(token: String) {
        sharedPreferences.edit().putString(KEY_JWT_TOKEN, token).apply()
    }

    /**
     * After signing in. [refreshToken] keeps the device signed in once the short-lived [token]
     * expires; null when the backend started no session.
     */
    fun saveAuth(token: String, refreshToken: String?, role: String, userId: Long, email: String, fullName: String?) {
        sharedPreferences.edit()
            .putString(KEY_JWT_TOKEN, token)
            .putString(KEY_REFRESH_TOKEN, refreshToken)
            .putString(KEY_USER_ROLE, role)
            .putLong(KEY_USER_ID, userId)
            .putString(KEY_USER_EMAIL, email)
            .putString(KEY_USER_NAME, fullName ?: "")
            .apply()
    }

    // After a profile edit, so the side menu shows the new name
    fun saveUserName(fullName: String?) {
        sharedPreferences.edit().putString(KEY_USER_NAME, fullName ?: "").apply()
    }

    override fun getToken(): String? {
        return sharedPreferences.getString(KEY_JWT_TOKEN, null)
    }

    override fun getRefreshToken(): String? {
        return sharedPreferences.getString(KEY_REFRESH_TOKEN, null)
    }

    // A renewal or a password change: same user, new tokens. The role is the server's current one
    override fun saveRenewedSession(token: String, refreshToken: String, role: String) {
        sharedPreferences.edit()
            .putString(KEY_JWT_TOKEN, token)
            .putString(KEY_REFRESH_TOKEN, refreshToken)
            .putString(KEY_USER_ROLE, role)
            .apply()
    }

    fun getRole(): String? {
        return sharedPreferences.getString(KEY_USER_ROLE, null)
    }

    fun getUserId(): Long {
        return sharedPreferences.getLong(KEY_USER_ID, -1L)
    }

    fun getEmail(): String? {
        return sharedPreferences.getString(KEY_USER_EMAIL, null)
    }

    fun getUserName(): String? {
        return sharedPreferences.getString(KEY_USER_NAME, null)
    }

    fun isAdmin(): Boolean {
        val role = getRole()
        return role != null && (role.equals("ROLE_ADMIN", ignoreCase = true) || role.equals("ADMIN", ignoreCase = true))
    }

    fun isLoggedIn(): Boolean {
        return !getToken().isNullOrBlank()
    }

    override fun clearToken() {
        sharedPreferences.edit().clear().apply()
    }

    companion object {
        // Excluded from backups in res/xml/backup_rules.xml and data_extraction_rules.xml
        const val PREFS_NAME = "medrational_secure_prefs"
        private const val TAG = "TokenManager"
        private const val KEY_JWT_TOKEN = "jwt_token"
        private const val KEY_REFRESH_TOKEN = "refresh_token"
        private const val KEY_USER_ROLE = "user_role"
        private const val KEY_USER_ID = "user_id"
        private const val KEY_USER_EMAIL = "user_email"
        private const val KEY_USER_NAME = "user_name"
    }
}

/**
 * Runs [open]; when it fails because stored data can't be decrypted, runs each of [resets] in turn,
 * retrying [open] after each one. Rethrows the last failure once every reset has been tried.
 */
internal fun <T> openRecovering(open: () -> T, vararg resets: () -> Unit): T {
    var failure: Exception = try {
        return open()
    } catch (e: GeneralSecurityException) {
        e
    } catch (e: IOException) {
        e
    }
    for (reset in resets) {
        reset()
        failure = try {
            return open()
        } catch (e: GeneralSecurityException) {
            e
        } catch (e: IOException) {
            e
        }
    }
    throw failure
}
