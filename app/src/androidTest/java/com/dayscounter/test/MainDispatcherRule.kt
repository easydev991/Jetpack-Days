package com.dayscounter.test

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.rules.TestWatcher
import org.junit.runner.Description

/**
 * Правило для замены Main dispatcher на тестовый.
 *
 * Используется для тестирования ViewModels, которые используют viewModelScope.
 * Правило автоматически заменяет Dispatchers.Main на StandardTestDispatcher
 * перед каждым тестом и восстанавливает после завершения.
 *
 * Использование:
 * ```kotlin
 * @get:Rule
 * val mainDispatcherRule = MainDispatcherRule()
 * ```
 */
@ExperimentalCoroutinesApi
class MainDispatcherRule(
    private val testDispatcher: TestDispatcher = StandardTestDispatcher(),
) : TestWatcher() {
    override fun starting(description: Description) {
        Dispatchers.setMain(testDispatcher)
    }

    override fun finished(description: Description) {
        Dispatchers.resetMain()
    }
}
