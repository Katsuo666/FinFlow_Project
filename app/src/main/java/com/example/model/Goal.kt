package com.example.model

data class Goal(
    val id: Int?,
    val name: String,
    val targetAmount: Double,
    val currentAmount: Double,
    val deadline: String?
)
