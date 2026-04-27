package com.example.project_school_ver1

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class CampusMapOcrTest {
    @Test
    fun detectFloorFromText_detectsChineseFloor() {
        val result = detectFloorFromText("請到二樓 203 室報到")
        assertEquals("2/F", result?.code)
    }

    @Test
    fun detectFloorFromText_detectsEnglishFloor() {
        val result = detectFloorFromText("Event at Ground Floor Lobby")
        assertEquals("G/F", result?.code)
    }

    @Test
    fun detectFloorFromText_returnsNullWhenNoFloor() {
        val result = detectFloorFromText("Welcome to Campus Open Day")
        assertNull(result)
    }
}

