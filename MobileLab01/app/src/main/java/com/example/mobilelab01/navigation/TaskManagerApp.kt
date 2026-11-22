package com.example.mobilelab01.navigation

import android.content.Context
import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.toMutableStateList
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.mobilelab01.data.TaskRepository
import com.example.mobilelab01.data.remote.RemoteTodoRepository
import com.example.mobilelab01.data.remote.RemoteTodosResult
import com.example.mobilelab01.data.remote.RemoteTodosUiState
import com.example.mobilelab01.model.Task
import com.example.mobilelab01.ui.screens.HomeScreen
import com.example.mobilelab01.ui.screens.RemoteTodosScreen
import com.example.mobilelab01.ui.screens.TaskDetailScreen
import com.example.mobilelab01.ui.screens.TaskFormScreen
import com.example.mobilelab01.ui.screens.TaskListScreen
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch

@Composable
fun TaskManagerApp(
    context: Context = LocalContext.current,
    repository: TaskRepository = remember { TaskRepository.getInstance(context) },
    remoteRepository: RemoteTodoRepository = remember { RemoteTodoRepository() }
) {
    val scope = rememberCoroutineScope()
    val backStack = remember { listOf<Screen>(Screen.Home).toMutableStateList() }
    val current = backStack.last()

    val tasks by repository.tasks.collectAsStateWithLifecycle(initialValue = emptyList())
    val totalCount by repository.totalCount.collectAsStateWithLifecycle(initialValue = 0)
    val completedCount by repository.completedCount.collectAsStateWithLifecycle(initialValue = 0)
    val pendingCount by repository.pendingCount.collectAsStateWithLifecycle(initialValue = 0)

    val detailTaskId = (current as? Screen.TaskDetail)?.taskId
    val formTaskId = (current as? Screen.TaskForm)?.taskId
    val observedTaskId = detailTaskId ?: formTaskId
    val observedTask by remember(observedTaskId, repository) {
        if (observedTaskId != null) {
            repository.observeTask(observedTaskId)
        } else {
            flowOf<Task?>(null)
        }
    }.collectAsStateWithLifecycle(initialValue = null)

    var remoteUiState by remember { mutableStateOf<RemoteTodosUiState>(RemoteTodosUiState.Loading) }

    LaunchedEffect(repository) {
        repository.ensureDemoDataInitialized()
    }

    fun navigate(screen: Screen) {
        backStack.add(screen)
    }

    fun goBack(): Boolean {
        if (backStack.size <= 1) return false
        backStack.removeAt(backStack.lastIndex)
        return true
    }

    fun openTaskListAvoidingDuplicates() {
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
        if (last !is Screen.TaskDetail) {
            backStack.add(Screen.TaskDetail(taskId))
        }
    }

    fun afterDelete() {
        while (
            backStack.lastOrNull() is Screen.TaskDetail ||
            backStack.lastOrNull() is Screen.TaskForm
        ) {
            backStack.removeAt(backStack.lastIndex)
        }
        if (backStack.lastOrNull() !is Screen.TaskList) {
            backStack.add(Screen.TaskList)
        }
    }

    fun loadRemoteTodos() {
        remoteUiState = RemoteTodosUiState.Loading
        scope.launch {
            remoteUiState = when (val result = remoteRepository.fetchTodos()) {
                is RemoteTodosResult.Success -> RemoteTodosUiState.Success(result.todos)
                is RemoteTodosResult.Error -> RemoteTodosUiState.Error(result.message)
            }
        }
    }

    BackHandler(enabled = backStack.size > 1) {
        goBack()
    }

    when (val screen = current) {
        is Screen.Home -> {
            HomeScreen(
                totalCount = totalCount,
                completedCount = completedCount,
                pendingCount = pendingCount,
                onOpenTasks = { navigate(Screen.TaskList) },
                onAddTask = { navigate(Screen.TaskForm()) },
                onOpenRemoteTasks = { navigate(Screen.RemoteTodos) }
            )
        }

        is Screen.TaskList -> {
            TaskListScreen(
                tasks = tasks,
                onBack = { goBack() },
                onTaskClick = { id -> navigate(Screen.TaskDetail(id)) },
                onAddTask = { navigate(Screen.TaskForm()) }
            )
        }

        is Screen.TaskDetail -> {
            val task = observedTask?.takeIf { it.id == screen.taskId }
                ?: tasks.find { it.id == screen.taskId }
            TaskDetailScreen(
                task = task,
                onBack = { goBack() },
                onEdit = { navigate(Screen.TaskForm(taskId = screen.taskId)) },
                onToggleStatus = {
                    scope.launch { repository.toggleCompleted(screen.taskId) }
                },
                onDeleteConfirmed = {
                    scope.launch {
                        repository.deleteTask(screen.taskId)
                        afterDelete()
                    }
                }
            )
        }

        is Screen.TaskForm -> {
            val existing = if (screen.taskId != null) {
                observedTask?.takeIf { it.id == screen.taskId }
                    ?: tasks.find { it.id == screen.taskId }
            } else {
                null
            }
            TaskFormScreen(
                existingTask = existing,
                onSave = { title, description, dueDate, isCompleted ->
                    scope.launch {
                        if (screen.taskId == null) {
                            repository.addTask(title, description, dueDate, isCompleted)
                            openTaskListAvoidingDuplicates()
                        } else {
                            val currentTask = repository.getById(screen.taskId)
                            if (currentTask != null) {
                                repository.updateTask(
                                    currentTask.copy(
                                        title = title,
                                        description = description,
                                        dueDate = dueDate,
                                        isCompleted = isCompleted
                                    )
                                )
                                afterEditSave(currentTask.id)
                            } else {
                                goBack()
                            }
                        }
                    }
                },
                onCancel = { goBack() }
            )
        }

        is Screen.RemoteTodos -> {
            LaunchedEffect(Unit) {
                loadRemoteTodos()
            }
            RemoteTodosScreen(
                uiState = remoteUiState,
                onBack = { goBack() },
                onRefresh = { loadRemoteTodos() }
            )
        }
    }
}
