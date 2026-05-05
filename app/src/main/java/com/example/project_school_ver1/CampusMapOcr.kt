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
    FloorMapInfo("4/F", "5", R.string.floor_4f_title, R.string.floor_4f_desc),
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

// Map of room/space codes on each floor
private val floorSpaces = mapOf(
    "G/F" to listOf("022", "023", "039", "041", "cafe", "canteen", "H069", "009", "015", "smartfarm"),
    "1/F" to listOf("116", "hall", "H143-146", "111C"),
    "2/F" to listOf("244", "lrc-roof-top", "216", "H202", "210B", "210C", "222"),
    "3/F" to listOf("316", "318", "336", "339", "N308", "311A"),
    "4/F" to listOf("417", "409A", "411", "427B"),
    "5/F" to listOf("506")
)

// Room code patterns for OCR recognition
private val roomPatterns = mapOf(
    "023" to listOf(Regex("""\b023\b"""), Regex("""023""")),
    "022" to listOf(Regex("""\b022\b"""), Regex("""022""")),
    "041" to listOf(Regex("""\b041\b"""), Regex("""041""")),
    "039" to listOf(Regex("""\b039\b"""), Regex("""039""")),
    "059" to listOf(Regex("""\b059\b"""), Regex("""059""")),
    "H069" to listOf(Regex("""\bH\s*069\b""", RegexOption.IGNORE_CASE)),
    "cafe" to listOf(Regex("""cafe|CAFE|咖啡"""), Regex("""Grove\s*Cafe""", RegexOption.IGNORE_CASE)),
    "canteen" to listOf(Regex("""canteen|CANTEEN|食堂"""), Regex("""cafeteria""", RegexOption.IGNORE_CASE)),
    "015" to listOf(Regex("""\b015\b"""), Regex("""015""")),
    "009" to listOf(Regex("""\b009\b"""), Regex("""009""")),
    "smartfarm" to listOf(Regex("""smartfarm|SMART\s*FARM|smart\s*farm|農場""", RegexOption.IGNORE_CASE)),
    "hall" to listOf(Regex("""禮堂|hall|HALL""", RegexOption.IGNORE_CASE)),
    "111C" to listOf(Regex("""\b111\s*C\b""", RegexOption.IGNORE_CASE)),
    "116" to listOf(Regex("""\b116\b"""), Regex("""116""")),
    "216" to listOf(Regex("""\b216\b"""), Regex("""216""")),
    "244" to listOf(Regex("""\b244\b"""), Regex("""244""")),
    "316" to listOf(Regex("""\b316\b"""), Regex("""316""")),
    "417" to listOf(Regex("""\b417\b"""), Regex("""417""")),
    "506" to listOf(Regex("""\b506\b"""), Regex("""506""")),
    "H202" to listOf(Regex("""\bH\s*202\b""", RegexOption.IGNORE_CASE)),
    "H143-146" to listOf(Regex("""\b(H\s*)?143|146\b""", RegexOption.IGNORE_CASE)),
    "210B" to listOf(Regex("""\b210\s*B\b""", RegexOption.IGNORE_CASE)),
    "210C" to listOf(Regex("""\b210\s*C\b""", RegexOption.IGNORE_CASE)),
    "222" to listOf(Regex("""\b222\b"""), Regex("""222""")),
    "318" to listOf(Regex("""\b318\b"""), Regex("""318""")),
    "336" to listOf(Regex("""\b336\b"""), Regex("""336""")),
    "339" to listOf(Regex("""\b339\b"""), Regex("""339""")),
    "N308" to listOf(Regex("""\bN\s*308\b""", RegexOption.IGNORE_CASE)),
    "311A" to listOf(Regex("""\b311\s*A\b""", RegexOption.IGNORE_CASE)),
    "409A" to listOf(Regex("""\b409\s*A\b""", RegexOption.IGNORE_CASE)),
    "411" to listOf(Regex("""\b411\b"""), Regex("""411""")),
    "427B" to listOf(Regex("""\b427\s*B\b""", RegexOption.IGNORE_CASE))
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

    // First, try to detect floor from explicit floor patterns (G/F, 1/F, etc.)
    val floorScore = floorPatterns
        .mapValues { (_, patterns) -> patterns.sumOf { regex -> regex.findAll(normalized).count() } }
    val bestFloorCode = floorScore
        .maxByOrNull { it.value }
        ?.takeIf { it.value > 0 }
        ?.key

    if (bestFloorCode != null) {
        return floorCatalog.firstOrNull { it.code == bestFloorCode }
    }

    // If no explicit floor found, try to detect from room codes
    val detectedRoom = detectRoomFromText(normalized)
    if (detectedRoom != null) {
        val floorCode = floorSpaces.entries
            .firstOrNull { (_, rooms) -> rooms.contains(detectedRoom) }
            ?.key
        
        return floorCode?.let { code -> floorCatalog.firstOrNull { it.code == code } }
    }

    return null
}

private fun detectRoomFromText(normalizedText: String): String? {
    val roomScores = roomPatterns.mapValues { (_, patterns) ->
        patterns.sumOf { regex -> regex.findAll(normalizedText).count() }
    }

    return roomScores
        .maxByOrNull { it.value }
        ?.takeIf { it.value > 0 }
        ?.key
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
