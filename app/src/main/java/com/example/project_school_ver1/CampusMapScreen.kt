package com.example.project_school_ver1

import android.annotation.SuppressLint
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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun CampusMapScreen() {
    var isLoading by remember { mutableStateOf(true) }
    var webViewRef by remember { mutableStateOf<WebView?>(null) }
    var zoomLevel by remember { mutableStateOf(1.0f) }
    var recognitionState by remember { mutableStateOf<CampusMapRecognitionUiState>(CampusMapRecognitionUiState.Idle) }
    var lastAutoSwitchedFloorCode by remember { mutableStateOf<String?>(null) }
    var showOcrDetails by remember { mutableStateOf(false) }

    val context = LocalContext.current
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
                Text(text = stringResource(R.string.map_ocr_title), style = MaterialTheme.typography.titleMedium)
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
