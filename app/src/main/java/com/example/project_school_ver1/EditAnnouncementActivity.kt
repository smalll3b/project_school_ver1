package com.example.project_school_ver1

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.project_school_ver1.ui.theme.Project_school_ver1Theme

class EditAnnouncementActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val announcementId = intent.getIntExtra("ANNOUNCEMENT_ID", -1)
        val announcementTitle = intent.getStringExtra("ANNOUNCEMENT_TITLE") ?: ""
        val announcementContent = intent.getStringExtra("ANNOUNCEMENT_CONTENT") ?: ""

        setContent {
            Project_school_ver1Theme {
                EditAnnouncementScreen(announcementId, announcementTitle, announcementContent) {
                    setResult(Activity.RESULT_OK, it)
                    finish()
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditAnnouncementScreen(id: Int, title: String, content: String, onSave: (Intent) -> Unit) {
    var currentTitle by remember { mutableStateOf(title) }
    var currentContent by remember { mutableStateOf(content) }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Edit Announcement") })
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .padding(paddingValues)
                .padding(16.dp)
        ) {
            OutlinedTextField(
                value = currentTitle,
                onValueChange = { currentTitle = it },
                label = { Text("Title") },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = currentContent,
                onValueChange = { currentContent = it },
                label = { Text("Content") },
                modifier = Modifier.fillMaxWidth().weight(1f)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = {
                    val resultIntent = Intent().apply {
                        putExtra("ANNOUNCEMENT_ID", id)
                        putExtra("ANNOUNCEMENT_TITLE", currentTitle)
                        putExtra("ANNOUNCEMENT_CONTENT", currentContent)
                    }
                    onSave(resultIntent)
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Save")
            }
        }
    }
}
