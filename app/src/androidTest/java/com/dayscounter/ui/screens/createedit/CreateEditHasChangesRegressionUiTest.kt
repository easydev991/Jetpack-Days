package com.dayscounter.ui.screens.createedit

import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.lifecycle.SavedStateHandle
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.dayscounter.R
import com.dayscounter.analytics.AnalyticsService
import com.dayscounter.analytics.NoopAnalyticsProvider
import com.dayscounter.data.database.DaysDatabase
import com.dayscounter.data.repository.ItemRepositoryImpl
import com.dayscounter.domain.model.DisplayOption
import com.dayscounter.domain.model.Item
import com.dayscounter.ui.theme.JetpackDaysTheme
import com.dayscounter.ui.viewmodel.CreateEditScreenViewModel
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId

/**
 * Регрессионный UI-тест: при открытии экрана редактирования hasChanges не должен
 * ложно срабатывать из-за time-of-day в timestamp.
 *
 * Баг: checkHasChanges сравнивал original.timestamp (с time-of-day)
 * с selectedDate.atStartOfDay() (00:00:00) — всегда получал разницу.
 * Фикс: сравнение LocalDate vs LocalDate, time-of-day игнорируется.
 */
@RunWith(AndroidJUnit4::class)
class CreateEditHasChangesRegressionUiTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private lateinit var database: DaysDatabase
    private lateinit var repository: ItemRepositoryImpl

    @Before
    fun setUp() {
        database =
            Room
                .inMemoryDatabaseBuilder(
                    ApplicationProvider.getApplicationContext(),
                    DaysDatabase::class.java
                ).allowMainThreadQueries()
                .build()
        repository = ItemRepositoryImpl(database.itemDao())
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun checkHasChanges_afterLoadItem_doesNotFalselyDetectTimeOfDayChange() {
        // Given: событие с известным временем суток (14:30)
        val date = LocalDate.of(2026, 5, 1)
        val time = LocalTime.of(14, 30)
        val timestamp =
            date
                .atTime(time)
                .atZone(ZoneId.systemDefault())
                .toInstant()
                .toEpochMilli()
        val item =
            Item(
                id = 0L,
                title = "Тестовое событие",
                details = "",
                timestamp = timestamp,
                colorTag = null,
                displayOption = DisplayOption.DAY
            )
        val insertedId =
            runBlocking {
                repository.insertItem(item)
            }

        val viewModel =
            CreateEditScreenViewModel(
                deps =
                    CreateEditScreenViewModel.Deps(
                        repository = repository,
                        resourceProvider = createTestResourceProvider(),
                        analyticsService = AnalyticsService(listOf(NoopAnalyticsProvider()))
                    ),
                savedStateHandle = SavedStateHandle(mapOf("itemId" to insertedId))
            )

        // When: открываем экран редактирования без изменения данных
        composeTestRule.setContent {
            JetpackDaysTheme {
                CreateEditScreen(
                    itemId = insertedId,
                    viewModel = viewModel,
                    analyticsService = AnalyticsService(listOf(NoopAnalyticsProvider()))
                )
            }
        }

        // Ждём завершения loadItem + loadItemData + рекомпозиции
        composeTestRule.waitForIdle()

        // Then: кнопка "Сохранить" неактивна — hasChanges == false,
        // хотя time-of-day в timestamp отличается от 00:00:00
        composeTestRule
            .onNodeWithText(context.getString(R.string.save))
            .assertIsNotEnabled()
    }
}
