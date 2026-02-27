package com.dayscounter.ui.screens.createedit

import androidx.compose.ui.graphics.Color

/**
 * Проверяет, является ли выбранный цвет кастомным (не из списка предустановленных).
 *
 * @param selectedColor Выбранный цвет (может быть null)
 * @param presetColors Список предустановленных цветов
 * @return true, если цвет не null и не содержится в списке предустановленных
 */
fun isCustomColor(
    selectedColor: Color?,
    presetColors: List<Color>,
): Boolean {
    if (selectedColor == null) return false
    return !presetColors.contains(selectedColor)
}
