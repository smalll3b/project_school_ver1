package com.example.project_school_ver1

import android.annotation.SuppressLint
import android.os.Build
import android.graphics.Bitmap
import android.view.ViewGroup
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import kotlinx.coroutines.launch
import java.io.ByteArrayInputStream
import java.io.InputStream

private const val CAMPUS_MAP_URL = "file:///android_asset/campus_map/ive_shatin_360.html"

private sealed interface CampusMapRecognitionUiState {
    data object Idle : CampusMapRecognitionUiState
    data object Recognizing : CampusMapRecognitionUiState
    data class Matched(val info: FloorMapInfo, val recognizedText: String) : CampusMapRecognitionUiState
    data class NoMatch(val recognizedText: String) : CampusMapRecognitionUiState
    data class Error(val message: String) : CampusMapRecognitionUiState
}

private fun emptyResponse(mimeType: String, encoding: String = "utf-8"): WebResourceResponse {
    return WebResourceResponse(mimeType, encoding, ByteArrayInputStream(ByteArray(0)))
}

private fun assetResponse(context: android.content.Context, assetPath: String, mimeType: String, encoding: String = "utf-8"): WebResourceResponse? {
    return try {
        val stream: InputStream = context.assets.open(assetPath)
        WebResourceResponse(mimeType, encoding, stream)
    } catch (_: Exception) {
        null
    }
}

private fun interceptCampusMapRequest(context: android.content.Context, requestUrl: String): WebResourceResponse? {
    var assetPath: String? = null
    if (requestUrl == "https://aframe.io/releases/1.0.4/aframe.min.js") {
        assetPath = "campus_map/js/aframe.min.js"
    } else if (requestUrl == "https://unpkg.com/aframe-event-set-component@5/dist/aframe-event-set-component.min.js") {
        assetPath = "campus_map/js/aframe-event-set-component.min.js"
    } else if (requestUrl == "https://unpkg.com/aframe-layout-component@5.3.0/dist/aframe-layout-component.min.js") {
        assetPath = "campus_map/js/aframe-layout-component.min.js"
    } else if (requestUrl == "https://unpkg.com/aframe-template-component@3.2.1/dist/aframe-template-component.min.js") {
        assetPath = "campus_map/js/aframe-template-component.min.js"
    } else if (requestUrl == "https://unpkg.com/aframe-proxy-event-component@2.1.0/dist/aframe-proxy-event-component.min.js") {
        assetPath = "campus_map/js/aframe-proxy-event-component.min.js"
    } else if (requestUrl == "https://code.jquery.com/jquery-3.3.1.slim.min.js") {
        assetPath = "campus_map/js/jquery-slim.min.js"
    } else if (requestUrl == "https://cdnjs.cloudflare.com/ajax/libs/popper.js/1.14.7/umd/popper.min.js") {
        assetPath = "campus_map/js/popper.min.js"
    } else if (requestUrl == "https://stackpath.bootstrapcdn.com/bootstrap/4.3.1/js/bootstrap.min.js") {
        assetPath = "campus_map/js/bootstrap.min.js"
    } else if (requestUrl == "http://www.frontendgurus.com/tutorials/frontend/css/frontendgurustop.css") {
        assetPath = "campus_map/css/frontendgurustop.css"
    } else if (requestUrl == "http://w.sharethis.com/button/buttons.js") {
        assetPath = "campus_map/js/sharethis-empty.js"
    }

    if (assetPath != null) {
        val mime = if (assetPath.endsWith(".js")) "application/javascript" else if (assetPath.endsWith(".css")) "text/css" else "text/plain"
        return assetResponse(context, assetPath, mime)
            ?: if (assetPath.endsWith(".js")) emptyResponse("application/javascript")
            else if (assetPath.endsWith(".css")) emptyResponse("text/css")
            else emptyResponse("text/plain")
    }

    return if (requestUrl.contains("frontendgurus.com") && requestUrl.endsWith(".css")) {
        emptyResponse("text/css")
    } else if (requestUrl.contains("sharethis.com") && requestUrl.endsWith(".js")) {
        emptyResponse("application/javascript")
    } else if (requestUrl.contains("code.jquery.com") && requestUrl.endsWith(".js")) {
        emptyResponse("application/javascript")
    } else if (requestUrl.contains("cdnjs.cloudflare.com") && requestUrl.endsWith(".js")) {
        emptyResponse("application/javascript")
    } else if (requestUrl.contains("stackpath.bootstrapcdn.com") && requestUrl.endsWith(".js")) {
        emptyResponse("application/javascript")
    } else {
        null
    }
}

private fun switchWebViewToFloor(webView: WebView?, floorInfo: FloorMapInfo) {
    if (webView == null) return
    val levelClass = "level--${floorInfo.mapTarget}"
    val script = """
        (function() {
            var levels = document.querySelectorAll('.level');
            if (!levels || levels.length === 0) return false;
            for (var i = 0; i < levels.length; i++) {
                levels[i].style.display = 'none';
            }
            var target = document.querySelector('.' + '$levelClass');
            if (!target) return false;
            target.style.display = 'block';
            return true;
        })();
    """.trimIndent()
    webView.evaluateJavascript(script, null)
}

private fun applyEnglishCampusMapData(webView: WebView?) {
    if (webView == null) return
    val script = """
        (function () {
          var titleMap = {
            "023": "One-stop Student Service Center",
            "022": "Centre for Independent Language Learning (CILL)",
            "041": "Rotary District 3450 Learning Resources Centre (LRC)",
            "039": "Student Development Office (SDO)",
            "cafe": "Grove Cafe",
            "canteen": "Canteen",
            "059": "Student Union",
            "H069": "PE Unit - SDO",
            "116": "Department of Child, Elderly and Community Services",
            "hall": "Hall",
            "H143-146": "Multi-purpose Activity Rooms",
            "216": "Department of Health and Life Sciences",
            "244": "Campus Secretariat",
            "H202": "Gym Room",
            "316": "Department of Business Administration",
            "417": "Department of Information Technology",
            "506": "Department of Engineering",
            "015": "IT Room 015 - Metaverse Innovation Centre",
            "009": "CECS Room 009 - Child Education Centre",
            "smartfarm": "HLS - Smart Farm",
            "111C": "HLS 111C - Urban Farm",
            "lrc-roof-top": "LRC Green Roof",
            "210B": "HLS 210B - Analytical Testing Training Centre",
            "210C": "HLS 210C - Scientific Instrumental Laboratory",
            "222": "CECS Room 222 - Elderly Service Centre",
            "318": "BA Room 318 - BOCHK Financial Services Learning Centre",
            "336": "BA Room 336 - Real Estate Management Academy",
            "339": "BA Room 339 - Centre of Business Innovation",
            "N308": "ENG Room N308 - BIM Studio",
            "311A": "HLS Room 311A - Centre of Biomedical Science",
            "409A": "HLS 409A - Anechoic Chamber",
            "411": "ENG Room 411 - Piped Services and Fire Safety Lab",
            "427B": "IT 427B - Mobile App Development Lab"
          };

          var listMap = {
            "022": "<font size='1'>022</font><br/>CILL",
            "023": "<font size='1'>023</font><br/>Student Service Center",
            "039": "<font size='1'>039</font><br/>Student Development Office",
            "041": "<font size='1'>041</font><br/>Learning Resources Centre",
            "hall": "Hall",
            "lrc-roof-top": "LRC Green Roof",
            "244": "<font size='1'>244</font><br/>Campus Secretariat",
            "116": "<font size='1'>116</font><br/>CECS",
            "216": "<font size='1'>216</font><br/>HLS",
            "316": "<font size='1'>316</font><br/>BA",
            "417": "<font size='1'>417</font><br/>IT",
            "506": "<font size='1'>506</font><br/>ENG",
            "cafe": "Grove Cafe",
            "canteen": "Canteen",
            "H069": "<font size='1'>H069</font><br/>PE Unit",
            "H202": "<font size='1'>H202</font><br/>Gym",
            "H143-146": "<font size='1'>H143</font><br/>Multi-purpose Rooms",
            "009": "<font size='1'>009</font><br/>Child Education Centre (CECS)",
            "015": "<font size='1'>015</font><br/>Metaverse Innovation Centre (IT)",
            "smartfarm": "<font size='1'>Campus</font><br/>Smart Farm (HLS)",
            "111C": "<font size='1'>111C</font><br/>Urban Farm (HLS)",
            "210B": "<font size='1'>210B</font><br/>Analytical Testing Centre (HLS)",
            "210C": "<font size='1'>210C</font><br/>Scientific Instrumental Lab (HLS)",
            "222": "<font size='1'>222</font><br/>Elderly Service Centre (CECS)",
            "318": "<font size='1'>318</font><br/>BOCHK Financial Services Centre (BA)",
            "336": "<font size='1'>336</font><br/>Real Estate Academy (BA)",
            "339": "<font size='1'>339</font><br/>Business Innovation Space (BA)",
            "N308": "<font size='1'>N308</font><br/>BIM Studio (ENG)",
            "311A": "<font size='1'>311A</font><br/>Centre of Biomedical Science (HLS)",
            "409A": "<font size='1'>409A</font><br/>Anechoic Chamber (HLS)",
            "411": "<font size='1'>411</font><br/>Piped Services and Fire Safety Lab (ENG)",
            "427B": "<font size='1'>427B</font><br/>Mobile App Development Lab (IT)"
          };

          var descMap = {
            "023": "The One-stop Student Service Center provides enquiry support, form processing, lost and found services, and basic nursing/first-aid dressing support.",
            "022": "CILL offers a spacious and comfortable self-learning environment to improve language proficiency. Learning resources include CDs, books, newspapers, magazines, games, and worksheets. Flexible multi-purpose rooms are also available for language activities such as workshops, parties, performances, and competitions.",
            "041": "The Rotary District 3450 Learning Resources Centre (LRC), with a floor area of around 2,600 sqm across two levels, provides integrated facilities and services including computer facilities, multimedia workshops, library services, seminar rooms, meeting rooms, and AV rooms. Students can search and print coursework, create digital media, borrow books, use inter-library services, study in reading areas, use self-learning AV resources, and conduct group discussions in seminar/meeting rooms. Visit the LRC website for details.",
            "039": "The Student Development Office develops and manages student services, including counselling, career guidance, whole-person development programs, sports and recreation, facility management, student development activities, volunteer services, and student financial assistance applications.",
            "cafe": "Campus cafe providing drinks and light refreshments.",
            "canteen": "Campus canteen for daily meals.",
            "059": "The Student Union provides item borrowing, stationery sales, society activity consultation, and venue booking services.",
            "H069": "Provides sports and recreation services, including PE electives, representative teams, Hong Kong Award for Young People activities, and borrowing of sports facilities.",
            "015": "Department of Information Technology.",
            "009": "Department of Child, Elderly and Community Services (CECS).",
            "smartfarm": "Department of Health and Life Sciences (HLS).",
            "111C": "Indoor vertical farming practices are used to support food security for the future.",
            "116": "The Department of Child, Elderly and Community Services offers professional training in early childhood education, social services, and community education. Through practicum, visits, exchange activities, workshops, and volunteer services, students build professional knowledge, skills, and attitudes.",
            "hall": "The campus hall supports large ceremonies and stage performances, and can also be configured as badminton or basketball courts.",
            "H143-146": "The campus provides multi-purpose sports facilities for training and fitness, including dance room, squash courts, table tennis room, badminton courts, volleyball courts, basketball courts, gym room, and adventure-based activity spaces.",
            "216": "The Department of Health and Life Sciences focuses on disciplines such as chemical technology, laboratory science, environmental protection, healthcare, biomedical science, arboriculture, and tree management. Programs emphasize practical professional training to support further studies and career development.",
            "lrc-roof-top": "Outdoor umbrellas and seating are available for rest and group discussion.",
            "244": "Campus Secretariat works closely with academic departments to provide quality services, including student and program administration, general administration, and human resources management.",
            "H202": "Gym room with a range of fitness equipment.",
            "210B": "Analytical Testing: students receive training in laboratory testing technologies, including drug analysis using GC and HPLC techniques.",
            "210C": "Consumer product heavy-metal safety testing using AAS, ICP-OES, and high-definition XRF to measure metal content in consumer products.",
            "222": "Department of Child, Elderly and Community Services (CECS).",
            "316": "The Department of Business Administration develops students with balanced growth in professional knowledge, industry practice, and work attitudes through rigorous curriculum design, personal development, and practical opportunities.",
            "318": "Department of Business Administration (BA).",
            "336": "Department of Business Administration (BA).",
            "339": "Department of Business Administration (BA).",
            "N308": "Department of Engineering (ENG).",
            "311A": "Department of Health and Life Sciences (HLS).",
            "417": "The Department of Information Technology offers Higher Diploma programs in mobile app development, multimedia creation, digital entertainment, web design, and web development. Facilities include professional HD video equipment, interactive media setups, latest smartphones, stereoscopic development systems, game development tools, and industry-standard networking tools.",
            "409A": "The anechoic chamber supports environmental industry applications, including green product testing, environmental acoustics study, and hearing tests.",
            "411": "Department of Engineering (ENG).",
            "427B": "Department of Information Technology.",
            "506": "The Department of Engineering was established at Sha Tin campus in 1986 and offers Higher Diploma programs in Computer and Electronic Engineering, Building Services Engineering, and Building Technology and Design, as well as Diploma of Foundation Studies (Engineering stream). The department maintains close ties with industry, higher-education institutions, and professional bodies to support curriculum development."
          };

          Object.keys(titleMap).forEach(function(space) {
            var title = document.querySelector('.content__item[data-space="' + space + '"] .content__item-title');
            if (title) title.innerHTML = titleMap[space];
          });

          Object.keys(descMap).forEach(function(space) {
            var desc = document.querySelector('.content__item[data-space="' + space + '"] .content__desc');
            if (desc) desc.innerHTML = descMap[space];
          });

          Object.keys(listMap).forEach(function(space) {
            var link = document.querySelector('.list__item[data-space="' + space + '"] .list__link');
            if (link) link.innerHTML = listMap[space];
          });

          var allSpaces = Array.prototype.slice.call(document.querySelectorAll('.content__item[data-space]'))
            .map(function(node) { return node.getAttribute('data-space'); });
          allSpaces.forEach(function(space) {
            if (!descMap.hasOwnProperty(space)) {
              console.warn('[CampusMap i18n] Missing English description for data-space:', space);
            }
          });

          var languageLabel = document.querySelector('.language');
          if (languageLabel) {
            languageLabel.textContent = 'English / Chinese';
          }

          var searchInput = document.querySelector('.search__input');
          if (searchInput) {
            searchInput.setAttribute('placeholder', 'Search...');
          }
        })();
    """.trimIndent()
    webView.evaluateJavascript(script, null)
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun CampusMapScreen() {
    var isLoading by remember { mutableStateOf(true) }
    var webViewRef by remember { mutableStateOf<WebView?>(null) }
    var zoomLevel by remember { mutableStateOf(1.0f) }
    var recognitionState by remember { mutableStateOf<CampusMapRecognitionUiState>(CampusMapRecognitionUiState.Idle) }
    var lastAutoSwitchedFloorCode by remember { mutableStateOf<String?>(null) }
    var showOcrDetails by remember { mutableStateOf(false) }
    var isOcrPanelExpanded by remember { mutableStateOf(true) }

    val context = LocalContext.current
    val configuration = LocalConfiguration.current
    val languageTag = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
        configuration.locales[0]?.toLanguageTag().orEmpty()
    } else {
        @Suppress("DEPRECATION")
        configuration.locale.toLanguageTag()
    }
    val isEnglishUi = languageTag.startsWith("en", ignoreCase = true)
    val scope = rememberCoroutineScope()

    fun analyzeFloor(bitmap: Bitmap?) {
        if (bitmap == null) {
            recognitionState = CampusMapRecognitionUiState.Error("")
            return
        }
        scope.launch {
            recognitionState = CampusMapRecognitionUiState.Recognizing
            val result = analyzeCampusMapFloor(bitmap)
            recognitionState = when {
                result.errorMessage != null -> CampusMapRecognitionUiState.Error(result.errorMessage)
                result.matchedFloor != null -> CampusMapRecognitionUiState.Matched(result.matchedFloor, result.recognizedText)
                else -> CampusMapRecognitionUiState.NoMatch(result.recognizedText)
            }
        }
    }

    val takePhotoLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicturePreview()
    ) { bitmap ->
        analyzeFloor(bitmap)
    }

    val pickImageLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        val bitmap = uri?.let { loadBitmapFromUri(context, it) }
        analyzeFloor(bitmap)
    }

    LaunchedEffect(recognitionState) {
        showOcrDetails = false
    }

    val matchedFloor = (recognitionState as? CampusMapRecognitionUiState.Matched)?.info
    LaunchedEffect(matchedFloor?.code, webViewRef, isLoading) {
        if (!isLoading && matchedFloor != null && matchedFloor.code != lastAutoSwitchedFloorCode) {
            switchWebViewToFloor(webViewRef, matchedFloor)
            lastAutoSwitchedFloorCode = matchedFloor.code
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        AndroidView(
            factory = { viewContext ->
                WebView(viewContext).apply {
                    layoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                    settings.javaScriptEnabled = true
                    settings.domStorageEnabled = true
                    settings.allowFileAccess = true
                    settings.allowContentAccess = true
                    settings.loadWithOverviewMode = true
                    settings.useWideViewPort = true
                    settings.builtInZoomControls = false
                    settings.displayZoomControls = false
                    settings.setSupportZoom(true)
                    settings.mediaPlaybackRequiresUserGesture = false
                    isScrollbarFadingEnabled = true
                    scrollBarStyle = android.view.View.SCROLLBARS_INSIDE_OVERLAY
                    setLayerType(android.view.View.LAYER_TYPE_HARDWARE, null)
                    webViewClient = object : WebViewClient() {
                        override fun shouldInterceptRequest(view: WebView?, request: WebResourceRequest?): WebResourceResponse? {
                            val url = request?.url?.toString() ?: return null
                            return interceptCampusMapRequest(view?.context ?: context, url)
                        }

                        override fun onPageFinished(view: WebView?, url: String?) {
                            isLoading = false
                            if (isEnglishUi) {
                                applyEnglishCampusMapData(view)
                            }
                        }

                        override fun shouldOverrideUrlLoading(
                            view: WebView?,
                            request: WebResourceRequest?
                        ): Boolean = false
                    }
                    webChromeClient = WebChromeClient()
                    loadUrl(CAMPUS_MAP_URL)
                }
            },
            update = { webView ->
                webViewRef = webView
            },
            modifier = Modifier.fillMaxSize()
        )

        if (isLoading) {
            CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
        }

        if (isOcrPanelExpanded) {
            // Expanded OCR Panel
            Card(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .fillMaxWidth()
                    .padding(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f))
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = stringResource(R.string.map_ocr_title), style = MaterialTheme.typography.titleMedium)
                        IconButton(onClick = { isOcrPanelExpanded = false }) {
                            Icon(Icons.Filled.Close, contentDescription = "Collapse OCR Panel")
                        }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(onClick = { takePhotoLauncher.launch(null) }) {
                            Text(stringResource(R.string.capture_floor_photo))
                        }
                        OutlinedButton(onClick = {
                            pickImageLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                        }) {
                            Text(stringResource(R.string.pick_floor_from_gallery))
                        }
                    }

                    when (val state = recognitionState) {
                        CampusMapRecognitionUiState.Idle -> Text(stringResource(R.string.map_ocr_hint))
                        CampusMapRecognitionUiState.Recognizing -> Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                            Text(stringResource(R.string.map_ocr_processing))
                        }

                        is CampusMapRecognitionUiState.Matched -> {
                            Text(
                                text = stringResource(R.string.map_detected_floor, state.info.code, stringResource(state.info.titleRes)),
                                color = MaterialTheme.colorScheme.primary
                            )
                            OutlinedButton(onClick = { showOcrDetails = !showOcrDetails }) {
                                Text(stringResource(if (showOcrDetails) R.string.map_view_less else R.string.map_view_more))
                            }
                            if (showOcrDetails) {
                                Text(text = stringResource(state.info.descriptionRes), style = MaterialTheme.typography.bodyMedium)
                                if (state.recognizedText.isNotBlank()) {
                                    Text(
                                        text = stringResource(R.string.map_ocr_raw_text, state.recognizedText.take(120)),
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                }
                            }
                        }

                        is CampusMapRecognitionUiState.NoMatch -> {
                            Text(text = stringResource(R.string.map_ocr_no_match))
                            if (state.recognizedText.isNotBlank()) {
                                OutlinedButton(onClick = { showOcrDetails = !showOcrDetails }) {
                                    Text(stringResource(if (showOcrDetails) R.string.map_view_less else R.string.map_view_more))
                                }
                            }
                            if (showOcrDetails && state.recognizedText.isNotBlank()) {
                                Text(
                                    text = stringResource(R.string.map_ocr_raw_text, state.recognizedText.take(120)),
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        }

                        is CampusMapRecognitionUiState.Error -> {
                            Text(text = if (state.message.isBlank()) stringResource(R.string.map_ocr_failed) else state.message)
                        }
                    }
                }
            }
        } else {
            // Collapsed OCR Panel - Small Button
            FloatingActionButton(
                onClick = { isOcrPanelExpanded = true },
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(12.dp)
            ) {
                Icon(Icons.Filled.Search, contentDescription = "Expand OCR Panel")
            }
        }

        // 放大縮小按鈕（右下角）
        Column(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FloatingActionButton(
                onClick = {
                    zoomLevel = (zoomLevel + 0.25f).coerceAtMost(3.0f)
                    webViewRef?.setInitialScale((zoomLevel * 100).toInt())
                    webViewRef?.evaluateJavascript(
                        "document.body.style.transform='scale(${zoomLevel})'; document.body.style.transformOrigin='0 0';",
                        null
                    )
                },
                modifier = Modifier.size(48.dp)
            ) {
                Text("+", fontSize = 20.sp)
            }
            FloatingActionButton(
                onClick = {
                    zoomLevel = (zoomLevel - 0.25f).coerceAtLeast(0.25f)
                    webViewRef?.setInitialScale((zoomLevel * 100).toInt())
                    webViewRef?.evaluateJavascript(
                        "document.body.style.transform='scale(${zoomLevel})'; document.body.style.transformOrigin='0 0';",
                        null
                    )
                },
                modifier = Modifier.size(48.dp)
            ) {
                Text("-", fontSize = 20.sp)
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun CampusMapScreenPreview() {
    CampusMapScreen()
}
