package com.dayscounter.ui.viewmodel

import com.dayscounter.domain.model.UpdateInfo
import com.dayscounter.domain.usecase.AppUpdateException
import com.dayscounter.domain.usecase.CheckForAppUpdateUseCase
import com.dayscounter.ui.state.MoreScreenUiState
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import java.util.stream.Stream

@OptIn(ExperimentalCoroutinesApi::class)
class MoreScreenViewModelTest {
    private lateinit var testDispatcher: TestDispatcher

    @BeforeEach
    fun setUp() {
        testDispatcher = StandardTestDispatcher()
        Dispatchers.setMain(testDispatcher)
    }

    @AfterEach
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun createViewModel(result: Result<UpdateInfo?>): MoreScreenViewModel {
        // Given — use case замокан на фиксированный результат проверки
        val useCase = mockk<CheckForAppUpdateUseCase>()
        coEvery { useCase.invoke() } returns result
        return MoreScreenViewModel(useCase)
    }

    @Test
    fun initial_state_is_idle() =
        runTest {
            // Given
            val viewModel = createViewModel(Result.success(null))

            // When — действие не выполняется

            // Then
            assertEquals(
                MoreScreenUiState.Idle,
                viewModel.state.value,
                "Начальное состояние должно быть Idle"
            )
        }

    @Test
    fun checkForUpdate_on_success_with_update_then_state_is_update_available() =
        runTest {
            // Given
            val viewModel = createViewModel(Result.success(sampleUpdateInfo))

            // When
            viewModel.checkForUpdate()
            testDispatcher.scheduler.advanceUntilIdle()

            // Then
            assertEquals(
                MoreScreenUiState.UpdateAvailable(sampleUpdateInfo),
                viewModel.state.value,
                "После успешной проверки с обновлением состояние должно быть UpdateAvailable"
            )
        }

    @Test
    fun checkForUpdate_on_success_no_update_then_state_is_up_to_date() =
        runTest {
            // Given — GitHub вернул версию, равную текущей
            val viewModel = createViewModel(Result.success(null))

            // When
            viewModel.checkForUpdate()
            testDispatcher.scheduler.advanceUntilIdle()

            // Then
            assertEquals(
                MoreScreenUiState.UpToDate,
                viewModel.state.value,
                "После успешной проверки без обновления состояние должно быть UpToDate"
            )
        }

    @Test
    fun checkForUpdate_on_failure_then_state_is_error() =
        runTest {
            // Given
            val viewModel =
                createViewModel(Result.failure(AppUpdateException("Не удалось проверить обновления")))

            // When
            viewModel.checkForUpdate()
            testDispatcher.scheduler.advanceUntilIdle()

            // Then
            assertEquals(
                MoreScreenUiState.Error,
                viewModel.state.value,
                "После ошибки проверки состояние должно быть Error"
            )
        }

    @ParameterizedTest(name = "dismissDialog из состояния {0} возвращает в Idle")
    @MethodSource("dialogStates")
    fun dismissDialog_from_each_dialog_state_then_state_is_idle(
        expectedState: MoreScreenUiState,
        result: Result<UpdateInfo?>
    ) = runTest {
        // Given — проверка выполнена, ViewModel в диалоговом состоянии
        val viewModel = createViewModel(result)
        viewModel.checkForUpdate()
        testDispatcher.scheduler.advanceUntilIdle()
        assertEquals(expectedState, viewModel.state.value, "Должно установиться диалоговое состояние")

        // When
        viewModel.dismissDialog()

        // Then
        assertEquals(
            MoreScreenUiState.Idle,
            viewModel.state.value,
            "После dismissDialog() состояние должно быть Idle"
        )
    }

    companion object {
        private val sampleUpdateInfo =
            UpdateInfo(
                version = "1.2.3",
                releaseUrl = "https://github.com/easydev991/Jetpack-Days/releases/tag/v1.2.3",
                notes = "Исправлены ошибки"
            )

        /**
         * Пары (ожидаемое состояние, результат use case) для трёх диалоговых состояний:
         * UpToDate, UpdateAvailable, Error. Idle/Checking не участвуют — диалог для них закрыт.
         */
        @JvmStatic
        fun dialogStates(): Stream<Arguments> =
            Stream.of(
                Arguments.of(MoreScreenUiState.UpToDate, Result.success<UpdateInfo?>(null)),
                Arguments.of(
                    MoreScreenUiState.UpdateAvailable(sampleUpdateInfo),
                    Result.success<UpdateInfo?>(sampleUpdateInfo)
                ),
                Arguments.of(
                    MoreScreenUiState.Error,
                    Result.failure<UpdateInfo?>(AppUpdateException("HTTP 403"))
                )
            )
    }
}
