package com.dayscounter.ui.viewmodel

import android.content.ActivityNotFoundException
import com.dayscounter.domain.model.ExactAlarmPermissionState
import com.dayscounter.reminder.ExactAlarmPermissionHelper
import com.dayscounter.util.Logger
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.util.concurrent.atomic.AtomicInteger

class ExactAlarmPermissionViewModelTest {
    @Test
    fun initial_state_when_helper_allowed_then_state_allowed() {
        // Given
        val helper =
            FakeExactAlarmPermissionHelper().apply {
                currentState = ExactAlarmPermissionState.Allowed
            }
        val logger: Logger = NoOpLoggerForVmTest

        // When
        val viewModel = ExactAlarmPermissionViewModel(helper, logger)

        // Then
        assertEquals(
            ExactAlarmPermissionState.Allowed,
            viewModel.state.value,
            "Начальное состояние должно быть Allowed"
        )
    }

    @Test
    fun initial_state_when_helper_denied_then_state_denied() {
        // Given
        val helper =
            FakeExactAlarmPermissionHelper().apply {
                currentState = ExactAlarmPermissionState.Denied(canRequest = true)
            }
        val logger: Logger = NoOpLoggerForVmTest

        // When
        val viewModel = ExactAlarmPermissionViewModel(helper, logger)

        // Then
        assertEquals(
            ExactAlarmPermissionState.Denied(canRequest = true),
            viewModel.state.value,
            "Начальное состояние должно быть Denied(canRequest=true)"
        )
    }

    @Test
    fun refresh_when_called_then_state_updated_from_helper() {
        // Given
        val helper =
            FakeExactAlarmPermissionHelper().apply {
                currentState = ExactAlarmPermissionState.Allowed
            }
        val viewModel = ExactAlarmPermissionViewModel(helper, NoOpLoggerForVmTest)

        // When
        helper.currentState = ExactAlarmPermissionState.Denied(canRequest = true)
        viewModel.refresh()

        // Then
        assertEquals(
            ExactAlarmPermissionState.Denied(canRequest = true),
            viewModel.state.value,
            "refresh() должен подтянуть свежее состояние из helper"
        )
    }

    @Test
    fun onRequestPermission_when_helper_throws_then_logged_with_throwable() {
        // Given
        val logger: Logger = mockk(relaxed = true)
        val helper =
            FakeExactAlarmPermissionHelper().apply {
                nextException = ActivityNotFoundException("test")
            }
        val viewModel = ExactAlarmPermissionViewModel(helper, logger)

        // When
        viewModel.onRequestPermission()

        // Then
        verify {
            logger.w(
                "ExactAlarmPermissionVM",
                match { it.contains("SCHEDULE_EXACT_ALARM") },
                ofType(ActivityNotFoundException::class)
            )
        }
        assertEquals(
            1,
            helper.requestCount.get(),
            "requestSettings() должен быть вызван даже если он бросил"
        )
    }

    @Test
    fun onRequestPermission_when_helper_succeeds_then_state_refreshed() {
        // Given
        val helper =
            FakeExactAlarmPermissionHelper().apply {
                currentState = ExactAlarmPermissionState.Denied(canRequest = true)
            }
        val viewModel = ExactAlarmPermissionViewModel(helper, NoOpLoggerForVmTest)

        // When: между вызовами getState() пользователь включил permission
        helper.currentState = ExactAlarmPermissionState.Allowed
        viewModel.onRequestPermission()

        // Then
        assertEquals(
            ExactAlarmPermissionState.Allowed,
            viewModel.state.value,
            "После успешного onRequestPermission() state должен обновиться"
        )
        assertEquals(
            1,
            helper.requestCount.get(),
            "requestSettings() должен быть вызван один раз"
        )
    }
}

private class FakeExactAlarmPermissionHelper : ExactAlarmPermissionHelper {
    var currentState: ExactAlarmPermissionState = ExactAlarmPermissionState.Allowed
    var nextException: Throwable? = null
    val requestCount = AtomicInteger(0)

    override fun getState(): ExactAlarmPermissionState = currentState

    override fun requestSettings() {
        requestCount.incrementAndGet()
        nextException?.let { throw it }
    }
}

private object NoOpLoggerForVmTest : Logger {
    override fun d(
        tag: String,
        message: String
    ) = Unit

    override fun w(
        tag: String,
        message: String,
        throwable: Throwable?
    ) = Unit

    override fun e(
        tag: String,
        message: String,
        throwable: Throwable?
    ) = Unit
}
