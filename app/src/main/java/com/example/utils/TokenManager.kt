package com.example.utils

import android.content.Context
import android.content.SharedPreferences

class TokenManager(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("FinFlowPrefs", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_TOKEN = "jwt_token"
        private const val KEY_USER_NAME = "user_name"
        private const val KEY_USER_EMAIL = "user_email"
        private const val KEY_USER_BIO = "user_bio"
    }

    fun saveToken(token: String?) {
        prefs.edit().putString(KEY_TOKEN, token).apply()
    }

    fun getToken(): String? {
        val token = prefs.getString(KEY_TOKEN, null)
        return token?.replace("\"", "")?.trim()
    }

    fun saveUser(name: String?, email: String?, bio: String? = null) {
        prefs.edit().apply {
            putString(KEY_USER_NAME, name)
            putString(KEY_USER_EMAIL, email)
            if (bio != null) putString(KEY_USER_BIO, bio)
            apply()
        }
    }

    fun getUserBio(): String {
        return prefs.getString(KEY_USER_BIO, "") ?: ""
    }

    fun getUserName(): String {
        return prefs.getString(KEY_USER_NAME, "Harrison") ?: "Harrison"
    }

    fun getUserEmail(): String {
        return prefs.getString(KEY_USER_EMAIL, "primeass.harrison@gmail.com") ?: "primeass.harrison@gmail.com"
    }

    fun saveProfilePicture(uriString: String?) {
        prefs.edit().putString("profile_picture", uriString).apply()
    }

    fun getProfilePicture(): String? {
        return prefs.getString("profile_picture", null)
    }

    fun clear() {
        prefs.edit().clear().apply()
    }
}
