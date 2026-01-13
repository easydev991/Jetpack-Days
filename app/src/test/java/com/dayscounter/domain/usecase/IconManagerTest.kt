package com.dayscounter.domain.usecase

import android.content.Context
import android.content.pm.PackageManager
import com.dayscounter.domain.model.AppIcon
import com.dayscounter.util.Logger
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

/** Unit-тесты для IconManager. */
@DisplayName("Тесты для IconManager")
class IconManagerTest {
    private lateinit var context: Context
    private lateinit var packageManager: PackageManager
    private lateinit var mockLogger: Logger
    private lateinit var iconManager: IconManager

    @BeforeEach
    fun setUp() {
        context = mockk()
        packageManager = mockk(relaxed = true)
        mockLogger = mockk(relaxed = true)
        every { context.packageName } returns "com.dayscounter"
        every { context.packageManager } returns packageManager
        iconManager = IconManager(context, mockLogger)
    }

    @Nested
    @DisplayName("Смена иконки")
    inner class ChangeIcon {
        @Test
        @DisplayName("Должен вызывать PackageManager для смены иконки (светлая тема)")
        fun changeIcon_shouldCallPackageManager_lightTheme() {
            // When
            iconManager.changeIcon(AppIcon.ICON_2, isDarkTheme = false)

            // Then - должно быть вызвано несколько раз (активация + деактивации)
            verify(atLeast = 1) { packageManager.setComponentEnabledSetting(any(), any(), any()) }
        }

        @Test
        @DisplayName("Должен вызывать PackageManager для смены иконки (тёмная тема)")
        fun changeIcon_shouldCallPackageManager_darkTheme() {
            // When
            iconManager.changeIcon(AppIcon.ICON_2, isDarkTheme = true)

            // Then - должно быть вызвано несколько раз (активация + деактивации)
            verify(atLeast = 1) { packageManager.setComponentEnabledSetting(any(), any(), any()) }
        }

        @Test
        @DisplayName("Не должен выбрасывать исключение при успешной смене иконки")
        fun changeIcon_shouldNotThrowOnSuccess() {
            // When
            iconManager.changeIcon(AppIcon.ICON_2, isDarkTheme = false)

            // Then - не должно быть исключений
            verify(atLeast = 1) { packageManager.setComponentEnabledSetting(any(), any(), any()) }
        }
    }
}
