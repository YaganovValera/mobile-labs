package com.example.mobilelab01.data

sealed class TaskToggleResult {
    data object NotFound : TaskToggleResult()

    data class Success(
        val taskId: Long,
        val taskTitle: String,
        val becameCompleted: Boolean
    ) : TaskToggleResult()
}
