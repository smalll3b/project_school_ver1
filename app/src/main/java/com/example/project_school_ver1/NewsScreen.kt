package com.example.project_school_ver1

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import java.text.SimpleDateFormat
import java.util.Locale

// Reuse NewsItem from MainActivity.kt.
data class MessageItem(
    val id: String,
    val title: String,
    val content: String,
    val createdAt: Timestamp?
)

@Composable
fun NewsScreen(
    onGoToLeaveMessage: () -> Unit = {},
    onGoToMessageFeed: () -> Unit = {}
) {
    val db = FirebaseFirestore.getInstance()
    val newsItems = remember { mutableStateListOf<NewsItem>() }

    DisposableEffect(Unit) {
        val listener = db.collection("news")
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, _ ->
                newsItems.clear()
                snapshot?.documents?.forEach { doc ->
                    newsItems.add(
                        NewsItem(
                            id = doc.id,
                            title = doc.getString("title") ?: "",
                            content = doc.getString("content") ?: "",
                            timestamp = doc.getTimestamp("timestamp")
                        )
                    )
                }
            }
        onDispose { listener.remove() }
    }

    LazyColumn(
        contentPadding = PaddingValues(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item {
            Button(
                onClick = onGoToLeaveMessage,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(stringResource(R.string.go_to_messages))
            }
        }
        item {
            Button(
                onClick = onGoToMessageFeed,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(stringResource(R.string.view_messages))
            }
        }

        if (newsItems.isEmpty()) {
            item {
                Text(
                    text = stringResource(R.string.no_news_yet),
                    modifier = Modifier.padding(8.dp),
                    color = Color(0xFF555555)
                )
            }
        } else {
            items(newsItems) { newsItem ->
                NewsCard(newsItem = newsItem)
            }
        }
    }
}

@Composable
fun NewsCard(newsItem: NewsItem) {
    val formatter = remember { SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()) }
    val timeText = newsItem.timestamp?.toDate()?.let { formatter.format(it) } ?: ""

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = newsItem.title,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 4.dp)
            )
            if (timeText.isNotBlank()) {
                Text(
                    text = timeText,
                    fontSize = 12.sp,
                    color = Color(0xFF555555),
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            }
            Text(
                text = newsItem.content,
                fontSize = 14.sp,
                color = Color(0xFF333333)
            )
        }
    }
}

@Composable
fun MessageFeedScreen() {
    val db = FirebaseFirestore.getInstance()
    val messages = remember { mutableStateListOf<MessageItem>() }

    DisposableEffect(Unit) {
        val listener = db.collection("messages")
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, _ ->
                messages.clear()
                snapshot?.documents?.forEach { doc ->
                    messages.add(
                        MessageItem(
                            id = doc.id,
                            title = doc.getString("title") ?: "",
                            content = doc.getString("content") ?: "",
                            createdAt = doc.getTimestamp("createdAt")
                        )
                    )
                }
            }
        onDispose { listener.remove() }
    }

    if (messages.isEmpty()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            Text(stringResource(R.string.no_messages_yet), fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
            Spacer(modifier = Modifier.height(8.dp))
            Text(stringResource(R.string.message_hint_from_news))
        }
    } else {
        LazyColumn(
            contentPadding = PaddingValues(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(messages) { item ->
                MessageCard(item)
            }
        }
    }
}

@Composable
private fun MessageCard(item: MessageItem) {
    val formatter = remember { SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()) }
    val timeText = item.createdAt?.toDate()?.let { formatter.format(it) } ?: ""

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(text = item.title, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            if (timeText.isNotBlank()) {
                Text(
                    text = timeText,
                    fontSize = 12.sp,
                    color = Color(0xFF555555),
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
            if (item.content.isNotBlank()) {
                Text(
                    text = item.content,
                    fontSize = 14.sp,
                    color = Color(0xFF333333),
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun NewsScreenPreview() {
    NewsScreen()
}
