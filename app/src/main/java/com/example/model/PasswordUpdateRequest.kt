package com.example.model

data class PasswordUpdateRequest(
    val oldPassword: String,
    val newPassword: String
)
