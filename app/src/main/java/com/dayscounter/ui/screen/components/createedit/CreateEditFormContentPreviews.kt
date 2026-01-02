package com.dayscounter.ui.screen.components.createedit

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.dayscounter.ui.theme.jetpackDaysTheme

// ==================== PREVIEWS ====================

@Preview(showBackground = true, name = "Секция заголовка")
@Composable
fun titleSectionPreview() {
    jetpackDaysTheme {
        val title = remember { mutableStateOf("День рождения") }
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(16.dp),
            verticalArrangement = Arrangement.Center,
        ) {
            OutlinedTextField(
                value = title.value,
                onValueChange = { title.value = it },
                label = { Text("Название") },
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Preview(showBackground = true, name = "Секция деталей")
@Composable
fun detailsSectionPreview() {
    jetpackDaysTheme {
        val details = remember { mutableStateOf("Праздничный день с друзьями") }
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(16.dp),
            verticalArrangement = Arrangement.Center,
        ) {
            OutlinedTextField(
                value = details.value,
                onValueChange = { details.value = it },
                label = { Text("Описание") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 3,
            )
        }
    }
}

@Preview(showBackground = true, name = "Секция даты")
@Composable
fun dateSectionPreview() {
    jetpackDaysTheme {
        val selectedDate = remember { mutableStateOf(java.time.LocalDate.now()) }
        val showDatePicker = remember { mutableStateOf(false) }
        val formatter =
            java.time.format.DateTimeFormatter
                .ofPattern("d MMMM yyyy", java.util.Locale("ru"))

        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(16.dp),
            verticalArrangement = Arrangement.Center,
        ) {
            OutlinedTextField(
                value = selectedDate.value?.format(formatter) ?: "",
                onValueChange = { },
                label = { Text("Дата") },
                readOnly = true,
                modifier = Modifier.fillMaxWidth(),
                trailingIcon = {
                    IconButton(onClick = { showDatePicker.value = true }) {
                        Icon(
                            imageVector = Icons.Filled.DateRange,
                            contentDescription = "Выбрать дату",
                        )
                    }
                },
            )
        }
    }
}
