package com.example.mobilelab01.data.remote

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException

sealed class RemoteTodosResult {
    data class Success(val todos: List<RemoteTodo>) : RemoteTodosResult()
    data class Error(val message: String) : RemoteTodosResult()
}

class RemoteTodoRepository(
    private val apiService: TodoApiService = RetrofitClient.todoApiService
) {

    suspend fun fetchTodos(): RemoteTodosResult = withContext(Dispatchers.IO) {
        try {
            val response = apiService.getTodos()
            if (response.isSuccessful) {
                val body = response.body()
                if (body != null) {
                    RemoteTodosResult.Success(body)
                } else {
                    RemoteTodosResult.Error("Сервер вернул пустой ответ.")
                }
            } else {
                RemoteTodosResult.Error(
                    "Ошибка HTTP ${response.code()}: ${response.message().ifBlank { "не удалось загрузить задачи" }}"
                )
            }
        } catch (e: IOException) {
            RemoteTodosResult.Error(
                "Нет подключения к интернету. Проверьте сеть и попробуйте снова."
            )
        } catch (e: Exception) {
            RemoteTodosResult.Error(
                e.message?.takeIf { it.isNotBlank() } ?: "Неизвестная ошибка при загрузке задач."
            )
        }
    }
}
