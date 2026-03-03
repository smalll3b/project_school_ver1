package com.example.project_school_ver1

import android.annotation.SuppressLint
import android.view.ViewGroup
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView

private const val CAMPUS_MAP_URL = "https://st.vtc.edu.hk/360tour/index_ch.html"

/**
 * A static map screen using an image, with clickable markers and buttons.
 */
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun CampusMapScreen() {
    var isLoading by remember { mutableStateOf(true) }
    var webViewRef by remember { mutableStateOf<WebView?>(null) }

    var zoomLevel by remember { mutableStateOf(1.0f) }

    Box(modifier = Modifier.fillMaxSize()) {
        AndroidView(
            factory = { context ->
                WebView(context).apply {
                    layoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                    settings.javaScriptEnabled = true
                    settings.domStorageEnabled = true
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
                        override fun onPageFinished(view: WebView?, url: String?) {
                            isLoading = false
                        }
                        override fun shouldOverrideUrlLoading(
                            view: WebView?,
                            request: android.webkit.WebResourceRequest?
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
