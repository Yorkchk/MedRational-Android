package com.example.medrational_android.data.auth

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

class TokenManager(context: Context) {

    private val masterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()

    private val sharedPreferences = EncryptedSharedPreferences.create(
        context,
        "medrational_secure_prefs",
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    fun saveToken(token: String) {
        sharedPreferences.edit().putString(KEY_JWT_TOKEN, token).apply()
    }

    fun saveAuth(token: String, role: String, userId: Long, email: String, fullName: String?) {
        sharedPreferences.edit()
            .putString(KEY_JWT_TOKEN, token)
            .putString(KEY_USER_ROLE, role)
            .putLong(KEY_USER_ID, userId)
            .putString(KEY_USER_EMAIL, email)
            .putString(KEY_USER_NAME, fullName ?: "")
            .apply()
    }

    fun getToken(): String? {
        return sharedPreferences.getString(KEY_JWT_TOKEN, null)
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

    fun clearToken() {
        sharedPreferences.edit().clear().apply()
    }

    companion object {
        private const val KEY_JWT_TOKEN = "jwt_token"
        private const val KEY_USER_ROLE = "user_role"
        private const val KEY_USER_ID = "user_id"
        private const val KEY_USER_EMAIL = "user_email"
        private const val KEY_USER_NAME = "user_name"
    }
}