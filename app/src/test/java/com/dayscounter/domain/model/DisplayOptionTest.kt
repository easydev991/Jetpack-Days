package com.dayscounter.domain.model

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class DisplayOptionTest {
    @Test
    fun defaultValue_isDay() {
        // Then
        assertEquals(DisplayOption.DAY, DisplayOption.DEFAULT)
    }
}
