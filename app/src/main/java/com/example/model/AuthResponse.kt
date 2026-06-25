package com.example.model

data class AuthResponse(
    val token: String?,
    val name: String?,
    val email: String?,
    val avatarUrl: String? = null
)
