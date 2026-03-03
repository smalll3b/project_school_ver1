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

class EditCourseActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val courseId = intent.getStringExtra("COURSE_ID") ?: ""
        val courseName = intent.getStringExtra("COURSE_NAME") ?: ""
        val courseTime = intent.getStringExtra("COURSE_TIME") ?: ""

        setContent {
            Project_school_ver1Theme {
                EditCourseScreen(courseId, courseName, courseTime) {
                    setResult(Activity.RESULT_OK, it)
                    finish()
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditCourseScreen(id: String, name: String, time: String, onSave: (Intent) -> Unit) {
    var currentName by remember { mutableStateOf(name) }
    var currentTime by remember { mutableStateOf(time) }

    Scaffold(
        topBar = { TopAppBar(title = { Text("Edit Course") }) }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .padding(paddingValues)
                .padding(16.dp)
        ) {
            OutlinedTextField(
                value = currentName,
                onValueChange = { currentName = it },
                label = { Text("Course Name") },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = currentTime,
                onValueChange = { currentTime = it },
                label = { Text("Course Time") },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = {
                    val resultIntent = Intent().apply {
                        putExtra("COURSE_ID", id)
                        putExtra("COURSE_NAME", currentName)
                        putExtra("COURSE_TIME", currentTime)
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
