package com.example.project_school_ver1

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// Data class to represent a news item
data class NewsItem(
    val title: String,
    val date: String,
    val content: String
)

// Sample data for the news screen
val sampleNews = listOf(
    NewsItem("Exam Schedule Released", "Dec 12, 2025", "The final exam schedule has been published. Please check the student portal."),
    NewsItem("Library Holiday Hours", "Dec 10, 2025", "The library will have reduced hours during the upcoming winter break."),
    NewsItem("Scholarship Applications Open", "Dec 8, 2025", "Applications for the Spring 2026 academic scholarship are now open.")
)

@Composable
fun NewsScreen() {
    LazyColumn(
        contentPadding = PaddingValues(8.dp)
    ) {
        items(sampleNews) {
            newsItem -> NewsCard(newsItem = newsItem)
        }
    }
}

@Composable
fun NewsCard(newsItem: NewsItem) {
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
            Text(
                text = newsItem.date,
                fontSize = 12.sp,
                color = Color(0xFF555555),
                modifier = Modifier.padding(bottom = 8.dp)
            )
            Text(
                text = newsItem.content,
                fontSize = 14.sp,
                color = Color(0xFF333333)
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun NewsScreenPreview() {
    NewsScreen()
}
