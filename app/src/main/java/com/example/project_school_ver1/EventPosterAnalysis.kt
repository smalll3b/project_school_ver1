package com.example.project_school_ver1

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.core.net.toUri
import androidx.annotation.StringRes
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.android.gms.tasks.Tasks
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.chinese.ChineseTextRecognizerOptions
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

private val datePatterns = listOf(
    Regex("""(20\d{2})[./\-年](\d{1,2})[./\-月](\d{1,2})日?"""),
    Regex("""(\d{1,2})[./\-](\d{1,2})[./\-](20\d{2})"""),
    Regex("""(20\d{2})(\d{2})(\d{2})""")
)

private val monthTokens = mapOf(
    "jan" to 1, "january" to 1,
    "feb" to 2, "february" to 2,
    "mar" to 3, "march" to 3,
    "apr" to 4, "april" to 4,
    "may" to 5,
    "jun" to 6, "june" to 6,
    "jul" to 7, "july" to 7,
    "aug" to 8, "august" to 8,
    "sep" to 9, "sept" to 9, "september" to 9,
    "oct" to 10, "october" to 10,
    "nov" to 11, "november" to 11,
    "dec" to 12, "december" to 12
)

private val sponsorKeywords = listOf("foundation", "scholarship", "fund", "charity", "基金", "獎學金", "资助", "資助")
private val activityKeywords = listOf("課程", "活動", "體驗", "營", "航海", "workshop", "camp", "adventure", "training", "sailing")

private val isoDateFormatter = DateTimeFormatter.ISO_LOCAL_DATE

enum class EventPosterStatus(@StringRes val labelRes: Int) {
    AVAILABLE(R.string.event_status_available),
    EXPIRED(R.string.event_status_expired),
    UNKNOWN(R.string.event_status_pending_review)
}

data class EventPosterAnalysisResult(
    val recognizedText: String,
    val detectedDate: LocalDate?,
    val status: EventPosterStatus
)

fun inferPosterTitle(recognizedText: String): String {
    val lines = recognizedText
        .lineSequence()
        .map { it.trim() }
        .filter { it.isNotBlank() }
        .toList()

    if (lines.isEmpty()) return ""

    return lines
        .map { line -> line to scorePosterTitleLine(line) }
        .filter { it.second > 0 }
        .maxByOrNull { it.second }
        ?.first
        ?.trim()
        ?.trimEnd('：', ':', '•', '·')
        .orEmpty()
}

private fun scorePosterTitleLine(line: String): Int {
    var score = 0
    val normalized = normalizePosterText(line).trim()
    if (normalized.isBlank()) return Int.MIN_VALUE
    if (extractPosterDate(normalized) != null) return Int.MIN_VALUE
    if (Regex("""\d{1,2}[:：]\d{2}""").containsMatchIn(normalized)) return Int.MIN_VALUE
    if (isMonthOnlyLine(normalized)) return Int.MIN_VALUE

    val lowered = normalized.lowercase(Locale.US)
    val blacklist = listOf("date", "time", "venue", "location", "deadline", "admission", "register")
    val cjkBlacklist = listOf("日期", "時間", "地點", "地址", "報名", "截止", "入場")
    if (blacklist.any { lowered.contains(it) } || cjkBlacklist.any { normalized.contains(it) }) score -= 3
    if (sponsorKeywords.any { lowered.contains(it) || normalized.contains(it) }) score -= 3
    if (activityKeywords.any { lowered.contains(it) || normalized.contains(it) }) score += 3

    val digitRatio = normalized.count { it.isDigit() }.toDouble() / normalized.length.coerceAtLeast(1)
    if (digitRatio > 0.2) score -= 2 else score += 1

    val letterOrCjkCount = normalized.count { it.isLetter() || it.code >= 0x4E00 }
    if (letterOrCjkCount >= 3) score += 2
    // OCR posters often place the actual headline as a short line ending with ':' or '：'.
    if (isHeadingLikeTitleLine(normalized)) score += 3
    if (isYearPrefixedHeadline(normalized)) score += 4
    if (letterOrCjkCount in 1..4 && normalized.length <= 6 && !isYearPrefixedHeadline(normalized)) score -= 2
    if (normalized.length >= 18 && !isYearPrefixedHeadline(normalized) && !isHeadingLikeTitleLine(normalized)) score -= 2
    if (normalized.length in 4..30) score += 2 else if (normalized.length in 31..50) score += 1
    if (!normalized.contains(':') && !normalized.contains('：')) score += 1

    return score
}

fun loadBitmapFromUri(context: Context, uri: Uri): Bitmap? {
    return try {
        context.contentResolver.openInputStream(uri)?.use { inputStream ->
            BitmapFactory.decodeStream(inputStream)
        }
    } catch (_: Exception) {
        null
    }
}

suspend fun analyzeEventPosterBitmap(bitmap: Bitmap): EventPosterAnalysisResult = withContext(Dispatchers.IO) {
    val image = InputImage.fromBitmap(bitmap, 0)
    val chineseRecognizer = TextRecognition.getClient(ChineseTextRecognizerOptions.Builder().build())
    val latinRecognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

    try {
        val chineseText = Tasks.await(chineseRecognizer.process(image)).text.trim()
        val finalText = if (chineseText.isNotBlank()) chineseText else Tasks.await(latinRecognizer.process(image)).text.trim()
        val detectedDate = extractPosterDate(finalText)
        val status = detectedDate?.let {
            if (it.isBefore(LocalDate.now())) EventPosterStatus.EXPIRED else EventPosterStatus.AVAILABLE
        } ?: EventPosterStatus.UNKNOWN
        EventPosterAnalysisResult(finalText, detectedDate, status)
    } catch (_: Exception) {
        EventPosterAnalysisResult("", null, EventPosterStatus.UNKNOWN)
    } finally {
        chineseRecognizer.close()
        latinRecognizer.close()
    }
}

fun normalizeEventImageUrl(rawUrl: String): String {
    val cleaned = rawUrl.trim()
    if (cleaned.isBlank()) return ""

    if (cleaned.contains("drive.google.com")) {
        val uri = cleaned.toUri()
        val fileId = uri.getQueryParameter("id")
            ?: uri.pathSegments.findLast { it.isNotBlank() && it != "view" && it != "file" && it != "d" }
        if (!fileId.isNullOrBlank()) {
            return "https://drive.google.com/uc?export=view&id=$fileId"
        }
    }

    return cleaned
}

fun resolveEventPosterStatus(rawStatus: String?, dateText: String): EventPosterStatus {
    val dateStatus = classifyEventPosterStatus(dateText)
    if (dateStatus != EventPosterStatus.UNKNOWN) return dateStatus

    return when (rawStatus?.trim()?.lowercase(Locale.US)) {
        "available" -> EventPosterStatus.AVAILABLE
        "expired" -> EventPosterStatus.EXPIRED
        else -> EventPosterStatus.UNKNOWN
    }
}

fun classifyEventPosterStatus(dateText: String): EventPosterStatus {
    val detectedDate = extractPosterDate(dateText) ?: return EventPosterStatus.UNKNOWN
    val today = LocalDate.now()
    return if (detectedDate.isBefore(today)) EventPosterStatus.EXPIRED else EventPosterStatus.AVAILABLE
}

fun extractPosterDate(text: String): LocalDate? {
    val normalizedText = normalizePosterText(text)

    // Handle OCR mixed range style like "3-7 AUG 2026" by using numeric range start.
    extractRangeStartDate(normalizedText)?.let { return it }

    for (pattern in datePatterns) {
        for (match in pattern.findAll(normalizedText)) {
            val candidate = when (match.groupValues.size) {
                4 -> {
                    val first = match.groupValues[1].toIntOrNull()
                    val second = match.groupValues[2].toIntOrNull()
                    val third = match.groupValues[3].toIntOrNull()
                    when {
                        first == null || second == null || third == null -> null
                        first >= 1000 -> createDate(first, second, third)
                        third >= 1000 -> createDate(third, second, first)
                        else -> null
                    }
                }
                else -> null
            }
            if (candidate != null) return candidate
        }
    }

    // Fallback: some posters only contain year + month (e.g. "2026\n6月 JUNE").
    val inferredYear = Regex("""(20\d{2})""")
        .find(normalizedText)
        ?.groupValues
        ?.getOrNull(1)
        ?.toIntOrNull()
    val inferredMonth = extractMonthFromText(normalizedText)
    if (inferredYear != null && inferredMonth != null) {
        return createDate(inferredYear, inferredMonth, 1)
    }

    return null
}

private fun extractRangeStartDate(text: String): LocalDate? {
    val rangeMatch = Regex("""\b(\d{1,2})\s*[-~至到]\s*(\d{1,2})\s*[A-Za-z]{3,12}\s*(20\d{2})\b""")
        .find(text)
        ?: return null

    val startDay = rangeMatch.groupValues[1].toIntOrNull() ?: return null
    val inferredMonth = rangeMatch.groupValues[2].toIntOrNull() ?: return null
    val year = rangeMatch.groupValues[3].toIntOrNull() ?: return null
    if (inferredMonth !in 1..12) return null

    return createDate(year, inferredMonth, startDay)
}

private fun normalizePosterText(text: String): String {
    return text
        .replace('／', '/')
        .replace('－', '-')
        .replace('—', '-')
        .replace('：', ':')
        .replace('\u3000', ' ')
        .replace(Regex("\\s+"), " ")
        .trim()
}

private fun isMonthOnlyLine(line: String): Boolean {
    if (Regex("""^\s*(?:0?[1-9]|1[0-2])\s*月\s*[A-Za-z]{0,12}\s*$""").matches(line)) return true
    val lowered = line.lowercase(Locale.US)
    return monthTokens.keys.any { token ->
        Regex("""^\s*$token\s*$""").matches(lowered)
    }
}

private fun isYearPrefixedHeadline(line: String): Boolean {
    val startsWithYear = Regex("""^\s*20\d{2}[^\d].+""").matches(line)
    if (!startsWithYear) return false
    val nonDigitContent = line.dropWhile { it.isWhitespace() }.drop(4)
    val meaningfulCharCount = nonDigitContent.count { it.isLetter() || it.code >= 0x4E00 }
    return meaningfulCharCount >= 4
}

private fun isHeadingLikeTitleLine(line: String): Boolean {
    val endsWithHeadingMark = line.endsWith(':') || line.endsWith('：')
    if (!endsWithHeadingMark) return false
    val core = line.trimEnd(':', '：').trim()
    if (core.isBlank()) return false
    if (isMonthOnlyLine(core)) return false
    val lowered = core.lowercase(Locale.US)
    if (listOf("date", "time", "venue", "location").any { lowered.contains(it) }) return false
    if (listOf("日期", "時間", "地點", "地址", "活動日期").any { core.contains(it) }) return false
    return core.length in 3..16
}

private fun extractMonthFromText(text: String): Int? {
    val monthFromCjk = Regex("""(?:^|\D)(1[0-2]|0?[1-9])\s*月""")
        .find(text)
        ?.groupValues
        ?.getOrNull(1)
        ?.toIntOrNull()
    if (monthFromCjk != null) return monthFromCjk

    val lowered = text.lowercase(Locale.US)
    monthTokens.forEach { (token, month) ->
        if (Regex("""\b$token\b""").containsMatchIn(lowered)) {
            return month
        }
    }
    return null
}

private fun createDate(year: Int, month: Int, day: Int): LocalDate? {
    return try {
        LocalDate.of(year, month, day)
    } catch (_: Exception) {
        null
    }
}

fun formatPosterDate(date: LocalDate): String = isoDateFormatter.format(date)

@Composable
fun EventStatusBadge(status: EventPosterStatus, modifier: Modifier = Modifier) {
    val colors = when (status) {
        EventPosterStatus.AVAILABLE -> Color(0xFFE8F5E9) to Color(0xFF2E7D32)
        EventPosterStatus.EXPIRED -> Color(0xFFFFEBEE) to Color(0xFFC62828)
        EventPosterStatus.UNKNOWN -> MaterialTheme.colorScheme.surfaceVariant to MaterialTheme.colorScheme.onSurfaceVariant
    }

    Surface(
        color = colors.first,
        contentColor = colors.second,
        shape = RoundedCornerShape(999.dp),
        modifier = modifier
    ) {
        Text(
            text = androidx.compose.ui.res.stringResource(status.labelRes),
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

suspend fun analyzeEventPosterImage(imageUrl: String): EventPosterAnalysisResult = withContext(Dispatchers.IO) {
    val bitmap = downloadBitmapFromUrl(normalizeEventImageUrl(imageUrl))
        ?: return@withContext EventPosterAnalysisResult("", null, EventPosterStatus.UNKNOWN)
    analyzeEventPosterBitmap(bitmap)
}

private fun downloadBitmapFromUrl(imageUrl: String): Bitmap? {
    if (imageUrl.isBlank()) return null

    return try {
        val connection = (URL(imageUrl).openConnection() as HttpURLConnection).apply {
            connectTimeout = 10_000
            readTimeout = 10_000
            instanceFollowRedirects = true
        }
        connection.inputStream.use { inputStream ->
            BitmapFactory.decodeStream(inputStream)
        }
    } catch (_: Exception) {
        null
    }
}
