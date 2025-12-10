package com.example.project_school_ver1

import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.GpsFixed
import androidx.compose.material.icons.filled.Place
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.project_school_ver1.R

/**
 * A static map screen using an image, with clickable markers and buttons.
 */
@Composable
fun CampusMapScreen() {
    val context = LocalContext.current

    Box(modifier = Modifier.fillMaxSize()) {
        // Background Campus Map Image
        Image(
            painter = painterResource(id = R.drawable.campus_map),
            contentDescription = "Campus Map",
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )

        // Marker for the library
        Icon(
            imageVector = Icons.Filled.Place,
            contentDescription = "Library Marker",
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(start = 100.dp, top = 200.dp)
                .clickable {
                    Toast
                        .makeText(context, "這是圖書館", Toast.LENGTH_SHORT)
                        .show()
                },
            tint = Color.Red
        )

        // Floating Action Button
        FloatingActionButton(
            onClick = { 
                Toast.makeText(context, "定位到校園中心", Toast.LENGTH_SHORT).show()
            },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp)
        ) {
            Icon(
                imageVector = Icons.Filled.GpsFixed,
                contentDescription = "Location"
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun CampusMapScreenPreview() {
    CampusMapScreen()
}
