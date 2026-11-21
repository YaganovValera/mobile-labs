package com.example.mobilelab01.navigation

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.toMutableStateList
import com.example.mobilelab01.data.TaskRepository
import com.example.mobilelab01.model.Task
import com.example.mobilelab01.ui.screens.HomeScreen
import com.example.mobilelab01.ui.screens.TaskDetailScreen
import com.example.mobilelab01.ui.screens.TaskFormScreen
import com.example.mobilelab01.ui.screens.TaskListScreen

@Composable
fun TaskManagerApp(
    repository: TaskRepository = remember { TaskRepository() }
) {
    val backStack = remember { listOf<Screen>(Screen.Home).toMutableStateList() }
    val current = backStack.last()

    fun navigate(screen: Screen) {
        backStack.add(screen)
    }

    fun goBack(): Boolean {
        if (backStack.size <= 1) return false
        backStack.removeAt(backStack.lastIndex)
        return true
    }

    fun openTaskListAvoidingDuplicates() {
        // Убираем форму, если она сверху, и не копим лишние экраны списка.
        while (backStack.lastOrNull() is Screen.TaskForm) {
            backStack.removeAt(backStack.lastIndex)
        }
        if (backStack.lastOrNull() !is Screen.TaskList) {
            backStack.add(Screen.TaskList)
        }
    }

    fun afterEditSave(taskId: Long) {
        while (backStack.lastOrNull() is Screen.TaskForm) {
            backStack.removeAt(backStack.lastIndex)
        }
        val last = backStack.lastOrNull()
        if (last is Screen.TaskDetail && last.taskId == taskId) {
            return
        }
        // На случай нестандартного стека — показываем карточку задачи.
        if (last !is Screen.TaskDetail) {
            backStack.add(Screen.TaskDetail(taskId))
        }
    }

    BackHandler(enabled = backStack.size > 1) {
        goBack()
    }

    when (val screen = current) {
        is Screen.Home -> {
            HomeScreen(
                totalCount = repository.totalCount,
                completedCount = repository.completedCount,
                pendingCount = repository.pendingCount,
                onOpenTasks = { navigate(Screen.TaskList) },
                onAddTask = { navigate(Screen.TaskForm()) }
            )
        }

        is Screen.TaskList -> {
            TaskListScreen(
                tasks = repository.tasks,
                onBack = { goBack() },
                onTaskClick = { id -> navigate(Screen.TaskDetail(id)) },
                onAddTask = { navigate(Screen.TaskForm()) }
            )
        }

        is Screen.TaskDetail -> {
            val task = repository.getById(screen.taskId)
            TaskDetailScreen(
                task = task,
                onBack = { goBack() },
                onEdit = { navigate(Screen.TaskForm(taskId = screen.taskId)) },
                onToggleStatus = { repository.toggleCompleted(screen.taskId) }
            )
        }

        is Screen.TaskForm -> {
            val existing: Task? = screen.taskId?.let { repository.getById(it) }
            TaskFormScreen(
                existingTask = existing,
                onSave = { title, description, dueDate, isCompleted ->
                    if (existing == null) {
                        repository.addTask(title, description, dueDate, isCompleted)
                        openTaskListAvoidingDuplicates()
                    } else {
                        repository.updateTask(
                            existing.copy(
                                title = title,
                                description = description,
                                dueDate = dueDate,
                                isCompleted = isCompleted
                            )
                        )
                        afterEditSave(existing.id)
                    }
                },
                onCancel = { goBack() }
            )
        }
    }
}
