package com.example.project_school_ver1

import android.widget.Toast
import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.project_school_ver1.R

// Data class to represent an event
data class Event(
    val title: String,
    val date: String,
    @DrawableRes val imageRes: Int
)

// Sample data
val sampleEvents = listOf(
    Event("Campus Music Festival", "Dec 15, 2025", R.drawable.ic_event_placeholder),
    Event("Tech Conference 2025", "Nov 20, 2025", R.drawable.ic_event_placeholder),
    Event("Art Exhibition", "Oct 30, 2025", R.drawable.ic_event_placeholder)
)

@Composable
fun EsentsScreen() {
    LazyColumn(
        contentPadding = PaddingValues(8.dp)
    ) {
        items(sampleEvents) {
            event -> EventCard(event = event)
        }
    }
}

@Composable
fun EventCard(event: Event) {
    val context = LocalContext.current
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Image(
                painter = painterResource(id = event.imageRes),
                contentDescription = event.title,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp),
                contentScale = ContentScale.Crop
            )
            Text(
                text = event.title,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(top = 8.dp)
            )
            Text(
                text = event.date,
                fontSize = 14.sp,
                modifier = Modifier.padding(top = 4.dp)
            )
            Button(
                onClick = { 
                    Toast.makeText(context, "Registered for ${event.title}", Toast.LENGTH_SHORT).show()
                },
                modifier = Modifier.padding(top = 8.dp)
            ) {
                Text("Register")
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun EsentsScreenPreview() {
    EsentsScreen()
}
