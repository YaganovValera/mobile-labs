package com.example.mobilelab01.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.mobilelab01.model.Task
import com.example.mobilelab01.util.DateValidation

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskFormScreen(
    existingTask: Task?,
    onSave: (title: String, description: String, dueDate: String, isCompleted: Boolean) -> Unit,
    onCancel: () -> Unit
) {
    val isEdit = existingTask != null

    var title by rememberSaveable(existingTask?.id) {
        mutableStateOf(existingTask?.title.orEmpty())
    }
    var description by rememberSaveable(existingTask?.id) {
        mutableStateOf(existingTask?.description.orEmpty())
    }
    var dueDate by rememberSaveable(existingTask?.id) {
        mutableStateOf(existingTask?.dueDate.orEmpty())
    }
    var isCompleted by rememberSaveable(existingTask?.id) {
        mutableStateOf(existingTask?.isCompleted ?: false)
    }
    var titleError by rememberSaveable(existingTask?.id) { mutableStateOf<String?>(null) }
    var dateError by rememberSaveable(existingTask?.id) { mutableStateOf<String?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = if (isEdit) "Редактирование задачи" else "Новая задача",
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onCancel) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Назад"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedTextField(
                value = title,
                onValueChange = {
                    title = it
                    if (titleError != null && it.isNotBlank()) {
                        titleError = null
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Название") },
                singleLine = true,
                isError = titleError != null,
                supportingText = {
                    if (titleError != null) {
                        Text(titleError!!)
                    }
                }
            )

            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(160.dp),
                label = { Text("Описание") },
                minLines = 4,
                maxLines = 8
            )

            OutlinedTextField(
                value = dueDate,
                onValueChange = {
                    dueDate = it
                    if (dateError != null && DateValidation.isValid(it)) {
                        dateError = null
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Срок выполнения") },
                placeholder = { Text(DateValidation.DATE_HINT) },
                singleLine = true,
                isError = dateError != null,
                supportingText = {
                    Text(dateError ?: "Формат даты: ${DateValidation.DATE_PATTERN}")
                }
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Задача выполнена",
                    style = MaterialTheme.typography.bodyLarge
                )
                Switch(
                    checked = isCompleted,
                    onCheckedChange = { isCompleted = it }
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Button(
                onClick = {
                    val trimmedTitle = title.trim()
                    val trimmedDate = dueDate.trim()
                    var hasError = false

                    if (trimmedTitle.isEmpty()) {
                        titleError = "Введите название задачи"
                        hasError = true
                    } else {
                        titleError = null
                    }

                    if (!DateValidation.isValid(trimmedDate)) {
                        dateError = "Укажите корректную дату в формате ${DateValidation.DATE_PATTERN}"
                        hasError = true
                    } else {
                        dateError = null
                    }

                    if (!hasError) {
                        onSave(trimmedTitle, description.trim(), trimmedDate, isCompleted)
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Сохранить")
            }

            OutlinedButton(
                onClick = onCancel,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Отмена")
            }
        }
    }
}
