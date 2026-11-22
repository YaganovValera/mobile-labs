package com.example.mobilelab01.navigation

import android.Manifest
import android.content.Context
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.toMutableStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.mobilelab01.data.TaskRepository
import com.example.mobilelab01.data.TaskToggleResult
import com.example.mobilelab01.data.remote.RemoteTodoRepository
import com.example.mobilelab01.data.remote.RemoteTodosResult
import com.example.mobilelab01.data.remote.RemoteTodosUiState
import com.example.mobilelab01.model.Task
import com.example.mobilelab01.notifications.NotificationPermissionStore
import com.example.mobilelab01.notifications.TaskNotificationManager
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
    remoteRepository: RemoteTodoRepository = remember { RemoteTodoRepository() },
    notificationManager: TaskNotificationManager = remember { TaskNotificationManager(context) },
    permissionStore: NotificationPermissionStore = remember { NotificationPermissionStore(context) }
) {
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
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
    var pendingCompletedTask by remember {
        mutableStateOf<Pair<Long, String>?>(null)
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        permissionStore.hasRequestedPermission = true
        val pending = pendingCompletedTask
        pendingCompletedTask = null
        if (pending == null) return@rememberLauncherForActivityResult

        if (granted) {
            notificationManager.showTaskCompletedNotification(pending.first, pending.second)
        } else {
            scope.launch {
                snackbarHostState.showSnackbar(
                    message = "Задача сохранена, но уведомления отключены"
                )
            }
        }
    }

    fun notifyTaskCompleted(taskId: Long, taskTitle: String) {
        if (notificationManager.canPostNotifications()) {
            notificationManager.showTaskCompletedNotification(taskId, taskTitle)
            return
        }

        val shouldRequestRuntime =
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                notificationManager.needsRuntimePermission() &&
                !permissionStore.hasRequestedPermission

        if (shouldRequestRuntime) {
            pendingCompletedTask = taskId to taskTitle
            permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            return
        }

        scope.launch {
            snackbarHostState.showSnackbar(
                message = "Задача сохранена, но уведомления отключены"
            )
        }
    }

    fun onTaskStatusToggled(taskId: Long) {
        scope.launch {
            when (val result = repository.toggleCompleted(taskId)) {
                is TaskToggleResult.NotFound -> Unit
                is TaskToggleResult.Success -> {
                    if (result.becameCompleted) {
                        notifyTaskCompleted(result.taskId, result.taskTitle)
                    }
                }
            }
        }
    }

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

    Box(modifier = Modifier.fillMaxSize()) {
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
                    onToggleStatus = { onTaskStatusToggled(screen.taskId) },
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

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(16.dp)
        )
    }
}
