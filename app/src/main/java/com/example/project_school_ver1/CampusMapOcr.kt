package com.example.project_school_ver1

import android.graphics.Bitmap
import androidx.annotation.StringRes
import com.google.android.gms.tasks.Tasks
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.chinese.ChineseTextRecognizerOptions
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class FloorMapInfo(
    val code: String,
    val mapTarget: String,
    @StringRes val titleRes: Int,
    @StringRes val descriptionRes: Int
)

data class CampusMapOcrResult(
    val recognizedText: String,
    val matchedFloor: FloorMapInfo?,
    val errorMessage: String? = null
)

private val floorCatalog = listOf(
    FloorMapInfo("G/F", "1", R.string.floor_gf_title, R.string.floor_gf_desc),
    FloorMapInfo("1/F", "2", R.string.floor_1f_title, R.string.floor_1f_desc),
    FloorMapInfo("2/F", "3", R.string.floor_2f_title, R.string.floor_2f_desc),
    FloorMapInfo("3/F", "4", R.string.floor_3f_title, R.string.floor_3f_desc),
    FloorMapInfo("B1", "1", R.string.floor_b1_title, R.string.floor_b1_desc)
)

private val floorPatterns = mapOf(
    "B1" to listOf(
        Regex("""\bB\s*1\b""", RegexOption.IGNORE_CASE),
        Regex("""地庫一樓|地下一層|地庫""")
    ),
    "G/F" to listOf(
        Regex("""\bG\s*/?\s*F\b""", RegexOption.IGNORE_CASE),
        Regex("""\bGF\b""", RegexOption.IGNORE_CASE),
        Regex("""GROUND\s*FLOOR""", RegexOption.IGNORE_CASE),
        Regex("""地下|地面|大堂""")
    ),
    "1/F" to listOf(
        Regex("""\b1\s*/?\s*F\b""", RegexOption.IGNORE_CASE),
        Regex("""FIRST\s*FLOOR""", RegexOption.IGNORE_CASE),
        Regex("""一樓""")
    ),
    "2/F" to listOf(
        Regex("""\b2\s*/?\s*F\b""", RegexOption.IGNORE_CASE),
        Regex("""SECOND\s*FLOOR""", RegexOption.IGNORE_CASE),
        Regex("""二樓""")
    ),
    "3/F" to listOf(
        Regex("""\b3\s*/?\s*F\b""", RegexOption.IGNORE_CASE),
        Regex("""THIRD\s*FLOOR""", RegexOption.IGNORE_CASE),
        Regex("""三樓""")
    )
)

suspend fun analyzeCampusMapFloor(bitmap: Bitmap): CampusMapOcrResult = withContext(Dispatchers.IO) {
    val image = InputImage.fromBitmap(bitmap, 0)
    val chineseRecognizer = TextRecognition.getClient(ChineseTextRecognizerOptions.Builder().build())
    val latinRecognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

    try {
        val chineseText = runCatching { Tasks.await(chineseRecognizer.process(image)).text.trim() }.getOrDefault("")
        val latinText = runCatching { Tasks.await(latinRecognizer.process(image)).text.trim() }.getOrDefault("")
        val mergedText = listOf(chineseText, latinText)
            .filter { it.isNotBlank() }
            .distinct()
            .joinToString("\n")

        val matchedFloor = detectFloorFromText(mergedText)
        CampusMapOcrResult(
            recognizedText = mergedText,
            matchedFloor = matchedFloor
        )
    } catch (_: Exception) {
        CampusMapOcrResult(recognizedText = "", matchedFloor = null, errorMessage = null)
    } finally {
        chineseRecognizer.close()
        latinRecognizer.close()
    }
}

fun detectFloorFromText(rawText: String): FloorMapInfo? {
    if (rawText.isBlank()) return null
    val normalized = normalizeOcrText(rawText)

    val bestCode = floorPatterns
        .mapValues { (_, patterns) -> patterns.sumOf { regex -> regex.findAll(normalized).count() } }
        .maxByOrNull { it.value }
        ?.takeIf { it.value > 0 }
        ?.key

    return floorCatalog.firstOrNull { it.code == bestCode }
}

private fun normalizeOcrText(rawText: String): String {
    return rawText
        .uppercase()
        .replace('／', '/')
        .replace('（', '(')
        .replace('）', ')')
        .replace("I/F", "1/F")
        .replace("L/F", "1/F")
}
