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

    @Test
    fun inferPosterTitle_skipsMonthOnlyLineInNoisyOcr() {
        val text = """
            2026建民章記/更新資料
            6月JUNE
            智方便麵
            百期
            2891 1001
        """.trimIndent()

        assertEquals("2026建民章記/更新資料", inferPosterTitle(text))
    }

    @Test
    fun inferPosterTitle_prioritizesYearPrefixedHeadlineOverShortLine() {
        val text = """
            2026建民章記/更新資料
            智方便麵
        """.trimIndent()

        assertEquals("2026建民章記/更新資料", inferPosterTitle(text))
    }

    @Test
    fun extractPosterDate_infersFirstDayFromYearAndMonth() {
        val text = """
            2026建民章記/更新資料
            6月JUNE
            智方便麵
        """.trimIndent()

        assertEquals(LocalDate.of(2026, 6, 1), extractPosterDate(text))
    }

    @Test
    fun inferPosterTitle_prefersHeadingLineOverDescription() {
        val text = """
            與ANGEIL PAWS（足印）一起感受勳物的的温暖店伴
            Paws心同行：
            s動物簡介雙相處學堂互動體驗深情分享
            •日期：2026年5月29日（星期五）
            時間聞：中午12:00一下午2:00
            •地點：HO58多用途室（Cantee)
        """.trimIndent()

        assertEquals("Paws心同行", inferPosterTitle(text))
    }

    @Test
    fun inferPosterTitle_prefersCourseHeadlineOverScholarshipSponsorLine() {
        val text = """
            鵬程慈善基金（外展訓練）獎學金
            Bright Fufure (outward Bound) Scholarship
            暑期青年航海課程
            5 DAYS
            YOUTH SAILING
            ADVENTURE
            3-7 AUG 2026
            全额資助FULL
        """.trimIndent()

        assertEquals("暑期青年航海課程", inferPosterTitle(text))
    }

    @Test
    fun extractPosterDate_prefersNumericRangeStartDateOverMonthFallback() {
        val text = """
            鵬程慈善基金（外展訓練）獎學金
            Bright Fufure (outward Bound) Scholarship
            暑期青年航海課程
            5 DAYS
            YOUTH SAILING
            ADVENTURE
            3-7 AUG 2026
            全额資助FULL
        """.trimIndent()

        assertEquals(LocalDate.of(2026, 7, 3), extractPosterDate(text))
    }
}

