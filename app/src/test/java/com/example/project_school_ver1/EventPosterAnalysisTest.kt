package com.example.project_school_ver1

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

class EventPosterAnalysisTest {
    @Test
    fun extractPosterDate_parsesChineseDateFormat() {
        val result = extractPosterDate("活動日期：2026年4月19日")
        assertEquals(LocalDate.of(2026, 4, 19), result)
    }

    @Test
    fun inferPosterTitle_ignoresDateAndMetadataLines() {
        val text = """
            校園音樂會
            日期：2026/04/19
            時間：18:30
            地點：禮堂
        """.trimIndent()

        assertEquals("校園音樂會", inferPosterTitle(text))
    }

    @Test
    fun inferPosterTitle_picksEnglishTitleLine() {
        val text = """
            Open Day
            2026-04-19
            10:00 AM
            Main Hall
        """.trimIndent()

        assertEquals("Open Day", inferPosterTitle(text))
    }
}

