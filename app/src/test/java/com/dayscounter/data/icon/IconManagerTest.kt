package com.dayscounter.data.icon

import android.content.Context
import android.content.pm.PackageManager
import com.dayscounter.domain.model.AppIcon
import com.dayscounter.util.CrashReporter
import com.dayscounter.util.Logger
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.verify
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import java.util.stream.Stream

/**
 * Unit-тесты для [IconManager] без Robolectric.
 *
 * Приватный `disableComponent` тестируется через публичный [IconManager.changeIcon]:
 * сценарий «сбой деактивации» проверяет отчёт из общего catch `Exception`.
 */
class IconManagerTest {
    private val context: Context = mockk()
    private val packageManager: PackageManager = mockk()
    private val logger: Logger = mockk(relaxed = true)
    private val crashReporter = mockk<CrashReporter>(relaxed = true)

    @BeforeEach
    fun setUp() {
        every { context.packageName } returns "com.dayscounter"
        every { context.packageManager } returns packageManager
    }

    @ParameterizedTest(name = "changeIcon при активации {0} репортит в crash-канал")
    @MethodSource("enableExceptions")
    fun changeIcon_when_enable_throws_then_logs_to_crash_reporter(exception: Exception) {
        // Given
        every { packageManager.setComponentEnabledSetting(any(), any(), any()) } throws exception

        // When: throw-семантика сохранена
        assertEquals(
            exception,
            assertThrows<Exception> {
                IconManager(context, logger, crashReporter).changeIcon(AppIcon.ICON_2)
            },
            "Исключение должно быть доставлено вызывающему коду"
        )

        // Then: сбой попал в crash-канал
        verify(exactly = 1) {
            crashReporter.logException(
                eq(exception),
                match { it.contains("Ошибка смены иконки") }
            )
        }
    }

    @Test
    fun changeIcon_when_disable_component_fails_then_logs_to_crash_reporter() {
        // Given: включение целевой иконки успешно, первая деактивация падает
        // неожиданным исключением (общий catch Exception внутри disableComponent)
        every {
            packageManager.setComponentEnabledSetting(
                any(),
                eq(PackageManager.COMPONENT_ENABLED_STATE_ENABLED),
                any()
            )
        } just runs
        var disableCallCount = 0
        every {
            packageManager.setComponentEnabledSetting(
                any(),
                eq(PackageManager.COMPONENT_ENABLED_STATE_DISABLED),
                any()
            )
        } answers {
            disableCallCount += 1
            if (disableCallCount == 1) {
                // Не IllegalStateException: та ловится в disableComponent отдельной
                // веткой без non-fatal — тест проверяет generic-ветку с crash-каналом
                throw UnsupportedOperationException("disable failed")
            }
        }

        // When: результат смены иконки остаётся успешным (сбой сброса глотается)
        assertDoesNotThrow {
            IconManager(context, logger, crashReporter).changeIcon(AppIcon.ICON_2)
        }

        // Then: один non-fatal о сбросе
        verify(exactly = 1) {
            crashReporter.logException(
                ofType(RuntimeException::class),
                match { it.contains("Ошибка сброса иконки") }
            )
        }
    }

    private companion object {
        @JvmStatic
        fun enableExceptions(): Stream<Arguments> =
            Stream.of(
                Arguments.of(SecurityException("no permission")),
                Arguments.of(PackageManager.NameNotFoundException("component missing")),
                Arguments.of(IllegalArgumentException("bad argument"))
            )
    }
}
