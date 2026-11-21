package com.example.mobilelab01.navigation

sealed class Screen {
    data object Home : Screen()
    data object TaskList : Screen()
    data class TaskDetail(val taskId: Long) : Screen()
    data class TaskForm(val taskId: Long? = null) : Screen()
}
