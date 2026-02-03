
package com.example.project_school_ver1

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.project_school_ver1.ui.theme.Project_school_ver1Theme

class LoginActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            Project_school_ver1Theme {
                LoginScreen()
            }
        }
    }
}

@Composable
fun LoginScreen() {
    val context = LocalContext.current
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Button(onClick = {
                // Navigate to AdminActivity
                val intent = Intent(context, MainActivity::class.java).apply {
                    putExtra("USER_TYPE", "ADMIN")
                }
                context.startActivity(intent)
            }) {
                Text("Login as Admin")
            }
            Spacer(modifier = Modifier.height(16.dp))
            Button(onClick = {
                // Navigate to StudentActivity
                val intent = Intent(context, MainActivity::class.java).apply {
                    putExtra("USER_TYPE", "STUDENT")
                }
                context.startActivity(intent)
            }) {
                Text("Login as Student")
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun LoginScreenPreview() {
    Project_school_ver1Theme {
        LoginScreen()
    }
}
