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
        @DisplayName("Должен вызывать PackageManager для смены иконки")
        fun changeIcon_shouldCallPackageManager() {
            // Given
            every { packageManager.setComponentEnabledSetting(any(), any(), any()) } returns Unit

            // When
            iconManager.changeIcon(AppIcon.ICON_2)

            // Then
            verify(atLeast = 1) { packageManager.setComponentEnabledSetting(any(), any(), any()) }
        }

        @Test
        @DisplayName("Должен выбрасывать SecurityException при ошибке PackageManager")
        fun changeIcon_shouldThrowSecurityException() {
            // Given
            every { packageManager.setComponentEnabledSetting(any(), any(), any()) } throws
                SecurityException("Нет прав для изменения состояния компонента")

            // When & Then
            org.junit.jupiter.api.assertThrows<SecurityException> {
                iconManager.changeIcon(AppIcon.ICON_2)
            }
        }
    }
}
