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

class EditUserActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val userId = intent.getIntExtra("USER_ID", -1)
        val userName = intent.getStringExtra("USER_NAME") ?: ""
        val userRole = intent.getStringExtra("USER_ROLE") ?: ""

        setContent {
            Project_school_ver1Theme {
                EditUserScreen(userId, userName, userRole) {
                    setResult(Activity.RESULT_OK, it)
                    finish()
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditUserScreen(id: Int, name: String, role: String, onSave: (Intent) -> Unit) {
    var currentName by remember { mutableStateOf(name) }
    var currentRole by remember { mutableStateOf(role) }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Edit User") })
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .padding(paddingValues)
                .padding(16.dp)
        ) {
            OutlinedTextField(
                value = currentName,
                onValueChange = { currentName = it },
                label = { Text("User Name") },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = currentRole,
                onValueChange = { currentRole = it },
                label = { Text("User Role") },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = {
                    val resultIntent = Intent().apply {
                        putExtra("USER_ID", id)
                        putExtra("USER_NAME", currentName)
                        putExtra("USER_ROLE", currentRole)
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
