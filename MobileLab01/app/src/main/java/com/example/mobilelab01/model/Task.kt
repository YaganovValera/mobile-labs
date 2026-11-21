package com.example.mobilelab01.model

data class Task(
    val id: Long,
    val title: String,
    val description: String,
    val dueDate: String,
    val isCompleted: Boolean
)
