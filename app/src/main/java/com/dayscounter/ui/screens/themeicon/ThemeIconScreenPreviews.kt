@file:Suppress("MagicNumber")

package com.dayscounter.ui.screens.themeicon

import android.content.res.Configuration
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.dayscounter.ui.theme.JetpackDaysTheme

// ==================== PREVIEWS ====================

@Preview(showBackground = true, locale = "ru", name = "Тема и иконка")
@Preview(
    uiMode = Configuration.UI_MODE_NIGHT_YES,
    showBackground = true,
    locale = "ru",
    name = "Тема и иконка (тёмная)"
)
@Composable
fun ThemeIconScreenPreview() {
    JetpackDaysTheme {
        ThemeIconScreenContent()
    }
}

/** Сетка иконок на узком экране: колонок считается от ширины окна. */
@Preview(showBackground = true, widthDp = 360, heightDp = 640, locale = "ru", name = "Сетка иконок 360dp")
@Composable
fun IconGridPhonePreview() {
    JetpackDaysTheme {
        ThemeIconScreenContent()
    }
}

/** Сетка иконок на широком экране: ячейки растягиваются, колонок больше. */
@Preview(showBackground = true, widthDp = 1280, heightDp = 800, locale = "ru", name = "Сетка иконок 1280dp")
@Composable
fun IconGridWidePreview() {
    JetpackDaysTheme {
        ThemeIconScreenContent()
    }
}
