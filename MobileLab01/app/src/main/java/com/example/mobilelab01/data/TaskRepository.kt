package com.example.mobilelab01.data

import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.snapshots.SnapshotStateList
import com.example.mobilelab01.model.Task

class TaskRepository {

    private val _tasks: SnapshotStateList<Task> = mutableStateListOf()
    val tasks: List<Task> get() = _tasks

    private var nextId: Long = 1L

    init {
        seedDemoTasks()
    }

    fun getById(id: Long): Task? = _tasks.find { it.id == id }

    fun addTask(
        title: String,
        description: String,
        dueDate: String,
        isCompleted: Boolean
    ): Task {
        val task = Task(
            id = nextId++,
            title = title.trim(),
            description = description.trim(),
            dueDate = dueDate.trim(),
            isCompleted = isCompleted
        )
        _tasks.add(task)
        return task
    }

    fun updateTask(task: Task) {
        val index = _tasks.indexOfFirst { it.id == task.id }
        if (index >= 0) {
            _tasks[index] = task.copy(
                title = task.title.trim(),
                description = task.description.trim(),
                dueDate = task.dueDate.trim()
            )
        }
    }

    fun setCompleted(id: Long, isCompleted: Boolean) {
        val index = _tasks.indexOfFirst { it.id == id }
        if (index >= 0) {
            _tasks[index] = _tasks[index].copy(isCompleted = isCompleted)
        }
    }

    fun toggleCompleted(id: Long) {
        val task = getById(id) ?: return
        setCompleted(id, !task.isCompleted)
    }

    val totalCount: Int get() = _tasks.size
    val completedCount: Int get() = _tasks.count { it.isCompleted }
    val pendingCount: Int get() = _tasks.count { !it.isCompleted }

    private fun seedDemoTasks() {
        val demos = listOf(
            Task(
                id = nextId++,
                title = "Подготовить отчёт",
                description = "Собрать результаты лабораторной работы и оформить краткий отчёт по структуре приложения.",
                dueDate = "25.11.2026",
                isCompleted = false
            ),
            Task(
                id = nextId++,
                title = "Купить продукты",
                description = "Молоко, хлеб, овощи и фрукты на неделю. Не забыть кофе.",
                dueDate = "22.11.2026",
                isCompleted = true
            ),
            Task(
                id = nextId++,
                title = "Созвон с командой",
                description = "Обсудить прогресс по учебному проекту и распределить задачи на следующую неделю.",
                dueDate = "24.11.2026",
                isCompleted = false
            ),
            Task(
                id = nextId++,
                title = "Прочитать главу учебника",
                description = "Изучить раздел по проектированию пользовательских интерфейсов мобильных приложений.",
                dueDate = "28.11.2026",
                isCompleted = false
            ),
            Task(
                id = nextId++,
                title = "Обновить резюме",
                description = "Добавить новые навыки: Kotlin, Jetpack Compose и опыт учебных проектов.",
                dueDate = "30.11.2026",
                isCompleted = true
            ),
            Task(
                id = nextId++,
                title = "Записаться к врачу",
                description = "Записаться на плановый осмотр в поликлинику на ближайшую свободную дату.",
                dueDate = "05.12.2026",
                isCompleted = false
            )
        )
        _tasks.addAll(demos)
    }
}
