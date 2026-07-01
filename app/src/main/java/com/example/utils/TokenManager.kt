package com.example.utils

import android.content.Context
import android.content.SharedPreferences
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Base64

class TokenManager(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("FinFlowPrefs", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_TOKEN = "jwt_token"
        private const val KEY_USER_NAME = "user_name"
        private const val KEY_USER_EMAIL = "user_email"
        private const val KEY_USER_BIO = "user_bio"
        private const val KEY_PROFILE_PIC = "profile_picture"
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

    fun getUserName(): String {
        return prefs.getString(KEY_USER_NAME, "Harrison") ?: "Harrison"
    }

    fun getUserEmail(): String {
        return prefs.getString(KEY_USER_EMAIL, "primeass.harrison@gmail.com") ?: "primeass.harrison@gmail.com"
    }

    fun getUserBio(): String {
        return prefs.getString(KEY_USER_BIO, "") ?: ""
    }

    fun saveProfilePicture(uriString: String?) {
        prefs.edit().remove(KEY_PROFILE_PIC).commit()
        prefs.edit().putString(KEY_PROFILE_PIC, uriString).commit()
    }

    fun getProfilePicture(): String? {
        return prefs.getString(KEY_PROFILE_PIC, null)
    }

    fun getProfileBitmap(): Bitmap? {
        val base64 = getProfilePicture() ?: return null
        return try {
            val pureBase64 = if (base64.contains(",")) base64.split(",")[1] else base64
            val bytes = Base64.decode(pureBase64, Base64.DEFAULT)
            BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
        } catch (e: Exception) {
            null
        }
    }

    fun clear() {
        prefs.edit().clear().apply()
    }
}
