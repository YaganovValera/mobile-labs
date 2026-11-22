package com.example.mobilelab01.data.remote

sealed class RemoteTodosUiState {
    data object Loading : RemoteTodosUiState()
    data class Success(val todos: List<RemoteTodo>) : RemoteTodosUiState()
    data class Error(val message: String) : RemoteTodosUiState()
}
