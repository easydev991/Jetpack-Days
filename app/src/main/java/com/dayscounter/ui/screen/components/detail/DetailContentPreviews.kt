package com.dayscounter.ui.screen.components.detail

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.dayscounter.domain.model.DisplayOption
import com.dayscounter.ui.theme.jetpackDaysTheme

// ==================== PREVIEWS ====================

@Preview(showBackground = true, name = "Секция цветовой метки")
@Composable
fun colorTagSectionPreview() {
    jetpackDaysTheme {
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            colorTagSection(android.graphics.Color.RED)
        }
    }
}

@Preview(showBackground = true, name = "Секция заголовка")
@Composable
fun titleSectionPreview() {
    jetpackDaysTheme {
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            titleSection("День рождения")
        }
    }
}

@Preview(showBackground = true, name = "Секция деталей")
@Composable
fun detailsSectionPreview() {
    jetpackDaysTheme {
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(16.dp),
            verticalArrangement = Arrangement.Center,
        ) {
            detailsSection("Праздничный день с друзьями")
        }
    }
}

@Preview(showBackground = true, name = "Информация об опции отображения")
@Composable
fun displayOptionInfoSectionPreview() {
    jetpackDaysTheme {
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            displayOptionInfoSection(DisplayOption.DAY)
        }
    }
}
