package com.dayscounter.ui.screens.events

import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.platform.app.InstrumentationRegistry
import com.dayscounter.R
import com.dayscounter.ui.theme.JetpackDaysTheme
import org.junit.Rule
import org.junit.Test

/**
 * UI тесты для ColorTagFilterDialog.
 *
 * Проверяет логику активности кнопок Сбросить/Применить и поведение черновика.
 */
class ColorTagFilterDialogTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    private val availableColors =
        listOf(
            0xFFFF0000.toInt(), // Красный
            0xFF00FF00.toInt(), // Зелёный
            0xFF0000FF.toInt() // Синий
        )

    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private val colorDescription = context.getString(R.string.color)
    private val resetText = context.getString(R.string.reset)
    private val applyText = context.getString(R.string.apply)
    private val titleText = context.getString(R.string.filter_by_color)

    /**
     * Проверяет, что при выборе цвета в черновике кнопка "Сбросить" становится активной
     * даже если фильтр не применён.
     *
     * UX-требование: Сбросить активна когда `currentFilter != null || draftSelectedColor != null`.
     */
    @Test
    fun dialog_whenDraftSelectedAndNoCurrentFilter_thenResetButtonEnabled() {
        // Given - фильтр не установлен, открываем диалог
        val currentFilter: Int? = null

        composeTestRule.setContent {
            JetpackDaysTheme {
                ColorTagFilterDialog(
                    availableColors = availableColors,
                    currentFilter = currentFilter,
                    onApply = {},
                    onDismiss = {}
                )
            }
        }

        // Кликаем на красный цвет (выбираем в черновике)
        composeTestRule
            .onAllNodesWithContentDescription(colorDescription)[0]
            .performClick()

        // Then - кнопка "Сбросить" должна быть активной (стала enabled из-за черновика)
        composeTestRule
            .onNodeWithText(resetText)
            .assertIsEnabled()
    }

    /**
     * Проверяет, что при сбросе черновика (без применённого фильтра) диалог остаётся открытым.
     *
     * UX-требование: если фильтра нет, но в черновике что-то выбрано —
     * очистить только черновик, диалог остаётся открытым.
     */
    @Test
    fun dialog_whenResetWithoutCurrentFilterButWithDraft_thenDialogStaysOpen() {
        // Given - фильтр не установлен, выбран цвет в черновике
        val currentFilter: Int? = null
        var onApplyCalled = false

        composeTestRule.setContent {
            JetpackDaysTheme {
                ColorTagFilterDialog(
                    availableColors = availableColors,
                    currentFilter = currentFilter,
                    onApply = {
                        onApplyCalled = true
                    },
                    onDismiss = {}
                )
            }
        }

        // Выбираем красный цвет
        composeTestRule
            .onAllNodesWithContentDescription(colorDescription)[0]
            .performClick()

        // Кликаем на "Сбросить"
        composeTestRule
            .onNodeWithText(resetText)
            .performClick()

        // Then - onApply НЕ должен вызываться (черновик очищается без закрытия)
        composeTestRule.onNodeWithText(titleText).assertIsDisplayed()
        composeTestRule.runOnIdle {
            org.junit.Assert.assertFalse(
                "При сбросе черновика без фильтра onApply не должен вызываться",
                onApplyCalled
            )
        }
    }

    /**
     * Проверяет, что при сбросе когда есть применённый фильтр — onApply(null) вызывается.
     */
    @Test
    fun dialog_whenResetWithCurrentFilter_thenCallsOnApplyNull() {
        // Given - текущий фильтр установлен
        val currentFilter = 0xFFFF0000.toInt()
        var onApplyCalled = false
        var appliedColor: Int? = currentFilter

        composeTestRule.setContent {
            JetpackDaysTheme {
                ColorTagFilterDialog(
                    availableColors = availableColors,
                    currentFilter = currentFilter,
                    onApply = {
                        onApplyCalled = true
                        appliedColor = it
                    },
                    onDismiss = {}
                )
            }
        }

        // Кликаем на "Сбросить"
        composeTestRule
            .onNodeWithText(resetText)
            .performClick()

        // Then - должен вызваться onApply(null) для сброса фильтра
        composeTestRule.runOnIdle {
            org.junit.Assert.assertTrue(
                "При сбросе активного фильтра onApply должен вызываться",
                onApplyCalled
            )
            org.junit.Assert.assertNull("При сбросе фильтра должен передаваться null в onApply", appliedColor)
        }
    }

    /**
     * Проверяет, что при клике на тот же цвет который уже выбран — выделение снимается.
     */
    @Test
    fun dialog_whenSameColorClicked_thenDeselects() {
        // Given - фильтр не установлен
        val currentFilter: Int? = null

        composeTestRule.setContent {
            JetpackDaysTheme {
                ColorTagFilterDialog(
                    availableColors = availableColors,
                    currentFilter = currentFilter,
                    onApply = {},
                    onDismiss = {}
                )
            }
        }

        // Кликаем на красный цвет дважды — должен сняться
        composeTestRule
            .onAllNodesWithContentDescription(colorDescription)[0]
            .performClick()

        composeTestRule
            .onAllNodesWithContentDescription(colorDescription)[0]
            .performClick()

        // Повторное нажатие снимает выделение, поэтому кнопка "Сбросить"
        // должна стать неактивной (нет фильтра и нет черновика)
        composeTestRule
            .onNodeWithText(resetText)
            .assertIsNotEnabled()
    }

    /**
     * Проверяет, что при открытии диалога без изменений кнопка "Применить" неактивна.
     */
    @Test
    fun dialog_whenDraftEqualsCurrentFilter_thenApplyButtonDisabled() {
        // Given - текущий фильтр красный
        val currentFilter = 0xFFFF0000.toInt()

        composeTestRule.setContent {
            JetpackDaysTheme {
                ColorTagFilterDialog(
                    availableColors = availableColors,
                    currentFilter = currentFilter,
                    onApply = {},
                    onDismiss = {}
                )
            }
        }

        // При открытии диалога черновик равен текущему фильтру -> изменений нет
        composeTestRule
            .onNodeWithText(applyText)
            .assertIsNotEnabled()
    }

    @Test
    fun dialog_whenOpened_thenShowsOnlyAvailableColors() {
        composeTestRule.setContent {
            JetpackDaysTheme {
                ColorTagFilterDialog(
                    availableColors = availableColors,
                    currentFilter = null,
                    onApply = {},
                    onDismiss = {}
                )
            }
        }

        composeTestRule
            .onAllNodesWithContentDescription(colorDescription)
            .assertCountEquals(availableColors.size)
    }

    @Test
    fun dialog_whenDraftChanged_thenApplyButtonEnabled() {
        val currentFilter = 0xFFFF0000.toInt()

        composeTestRule.setContent {
            JetpackDaysTheme {
                ColorTagFilterDialog(
                    availableColors = availableColors,
                    currentFilter = currentFilter,
                    onApply = {},
                    onDismiss = {}
                )
            }
        }

        // Меняем выбор с красного на зелёный.
        composeTestRule
            .onAllNodesWithContentDescription(colorDescription)[1]
            .performClick()

        composeTestRule
            .onNodeWithText(applyText)
            .assertIsEnabled()
    }
}
