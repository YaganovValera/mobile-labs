package com.example.mobilelab01.data

import android.content.Context
import com.example.mobilelab01.data.local.AppDatabase
import com.example.mobilelab01.data.local.TaskDao
import com.example.mobilelab01.model.Task
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

class TaskRepository private constructor(
    private val taskDao: TaskDao,
    context: Context
) {

    private val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    val tasks: Flow<List<Task>> = taskDao.observeAll()

    fun observeTask(id: Long): Flow<Task?> = taskDao.observeById(id)

    val totalCount: Flow<Int> = tasks.map { it.size }
    val completedCount: Flow<Int> = tasks.map { list -> list.count { it.isCompleted } }
    val pendingCount: Flow<Int> = tasks.map { list -> list.count { !it.isCompleted } }

    suspend fun ensureDemoDataInitialized() = withContext(Dispatchers.IO) {
        if (prefs.getBoolean(KEY_DEMO_INITIALIZED, false)) {
            return@withContext
        }
        taskDao.insertAll(demoTasks())
        prefs.edit().putBoolean(KEY_DEMO_INITIALIZED, true).apply()
    }

    suspend fun getById(id: Long): Task? = withContext(Dispatchers.IO) {
        taskDao.getById(id)
    }

    suspend fun addTask(
        title: String,
        description: String,
        dueDate: String,
        isCompleted: Boolean
    ): Long = withContext(Dispatchers.IO) {
        taskDao.insert(
            Task(
                title = title.trim(),
                description = description.trim(),
                dueDate = dueDate.trim(),
                isCompleted = isCompleted
            )
        )
    }

    suspend fun updateTask(task: Task) = withContext(Dispatchers.IO) {
        taskDao.update(
            task.copy(
                title = task.title.trim(),
                description = task.description.trim(),
                dueDate = task.dueDate.trim()
            )
        )
    }

    suspend fun toggleCompleted(id: Long) = withContext(Dispatchers.IO) {
        val task = taskDao.getById(id) ?: return@withContext
        taskDao.update(task.copy(isCompleted = !task.isCompleted))
    }

    suspend fun deleteTask(id: Long): Boolean = withContext(Dispatchers.IO) {
        taskDao.deleteById(id) > 0
    }

    private fun demoTasks(): List<Task> = listOf(
        Task(
            title = "Подготовить отчёт",
            description = "Собрать результаты лабораторной работы и оформить краткий отчёт по структуре приложения.",
            dueDate = "25.11.2026",
            isCompleted = false
        ),
        Task(
            title = "Купить продукты",
            description = "Молоко, хлеб, овощи и фрукты на неделю. Не забыть кофе.",
            dueDate = "22.11.2026",
            isCompleted = true
        ),
        Task(
            title = "Созвон с командой",
            description = "Обсудить прогресс по учебному проекту и распределить задачи на следующую неделю.",
            dueDate = "24.11.2026",
            isCompleted = false
        ),
        Task(
            title = "Прочитать главу учебника",
            description = "Изучить раздел по проектированию пользовательских интерфейсов мобильных приложений.",
            dueDate = "28.11.2026",
            isCompleted = false
        ),
        Task(
            title = "Обновить резюме",
            description = "Добавить новые навыки: Kotlin, Jetpack Compose и опыт учебных проектов.",
            dueDate = "30.11.2026",
            isCompleted = true
        ),
        Task(
            title = "Записаться к врачу",
            description = "Записаться на плановый осмотр в поликлинику на ближайшую свободную дату.",
            dueDate = "05.12.2026",
            isCompleted = false
        )
    )

    companion object {
        private const val PREFS_NAME = "task_manager_prefs"
        private const val KEY_DEMO_INITIALIZED = "demo_data_initialized"

        @Volatile
        private var instance: TaskRepository? = null

        fun getInstance(context: Context): TaskRepository {
            return instance ?: synchronized(this) {
                instance ?: TaskRepository(
                    taskDao = AppDatabase.getInstance(context).taskDao(),
                    context = context
                ).also { instance = it }
            }
        }
    }
}
