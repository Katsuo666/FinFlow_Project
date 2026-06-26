package com.example.model

data class AuthResponse(
    val token: String?,
    val name: String?,
    val firstName: String? = null,
    val lastName: String? = null,
    val email: String?,
    val bio: String? = null,
    val avatarUrl: String? = null
)
