package com.example.project_school_ver1

import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.firebase.firestore.FirebaseFirestore

// Event model is defined in MainActivity.kt and reused here.

@Composable
fun EsentsScreen() {
    val db = FirebaseFirestore.getInstance()
    val events = remember { mutableStateListOf<Event>() }

    DisposableEffect(Unit) {
        val listener = db.collection("events").addSnapshotListener { snapshot, _ ->
            if (snapshot != null) {
                events.clear()
                snapshot.documents.forEach { doc ->
                    events.add(
                        Event(
                            id = doc.id,
                            title = doc.getString("title") ?: "",
                            date = doc.getString("date") ?: ""
                        )
                    )
                }
            }
        }
        onDispose { listener.remove() }
    }

    if (events.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
    } else {
        LazyColumn(contentPadding = PaddingValues(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            items(events, key = { it.id }) { event ->
                EventCard(event = event)
            }
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
                painter = painterResource(id = R.drawable.ic_event_placeholder),
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
