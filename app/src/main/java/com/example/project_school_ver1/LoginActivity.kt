package com.example.project_school_ver1

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.project_school_ver1.ui.theme.Project_school_ver1Theme
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class LoginActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // If already logged in, go directly to MainActivity
        val currentUser = FirebaseAuth.getInstance().currentUser
        if (currentUser != null) {
            checkUserRoleAndNavigate(currentUser.uid)
            return
        }
        setContent {
            Project_school_ver1Theme {
                LoginScreen()
            }
        }
    }

    private fun checkUserRoleAndNavigate(uid: String) {
        FirebaseFirestore.getInstance().collection("users").document(uid).get()
            .addOnSuccessListener { doc ->
                val role = doc.getString("role") ?: "STUDENT"
                val intent = Intent(this, MainActivity::class.java).apply {
                    putExtra("USER_TYPE", if (role == "ADMIN") "ADMIN" else "STUDENT")
                }
                startActivity(intent)
                finish()
            }
            .addOnFailureListener {
                val intent = Intent(this, MainActivity::class.java).apply {
                    putExtra("USER_TYPE", "STUDENT")
                }
                startActivity(intent)
                finish()
            }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen() {
    val context = LocalContext.current
    val auth = FirebaseAuth.getInstance()
    val db = FirebaseFirestore.getInstance()

    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf("") }
    var isRegisterMode by remember { mutableStateOf(false) }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "校園 App",
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF2196F3),
                modifier = Modifier.padding(bottom = 8.dp)
            )
            Text(
                text = if (isRegisterMode) "建立帳號" else "登入",
                fontSize = 18.sp,
                modifier = Modifier.padding(bottom = 24.dp)
            )

            OutlinedTextField(
                value = email,
                onValueChange = { email = it },
                label = { Text("Email") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            Spacer(modifier = Modifier.height(12.dp))
            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                label = { Text("密碼") },
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            if (errorMessage.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(text = errorMessage, color = Color.Red, fontSize = 13.sp)
            }

            Spacer(modifier = Modifier.height(24.dp))

            if (isLoading) {
                CircularProgressIndicator()
            } else {
                Button(
                    onClick = {
                        if (email.isBlank() || password.isBlank()) {
                            errorMessage = "請輸入 Email 和密碼"
                            return@Button
                        }
                        isLoading = true
                        errorMessage = ""
                        if (isRegisterMode) {
                            auth.createUserWithEmailAndPassword(email.trim(), password)
                                .addOnSuccessListener { result ->
                                    val uid = result.user!!.uid
                                    db.collection("users").document(uid).set(
                                        mapOf("email" to email.trim(), "role" to "STUDENT", "name" to email.trim().substringBefore("@"))
                                    ).addOnCompleteListener {
                                        isLoading = false
                                        val intent = Intent(context, MainActivity::class.java).apply {
                                            putExtra("USER_TYPE", "STUDENT")
                                        }
                                        context.startActivity(intent)
                                        (context as? LoginActivity)?.finish()
                                    }
                                }
                                .addOnFailureListener { e ->
                                    isLoading = false
                                    errorMessage = e.localizedMessage ?: "註冊失敗"
                                }
                        } else {
                            auth.signInWithEmailAndPassword(email.trim(), password)
                                .addOnSuccessListener { result ->
                                    val uid = result.user!!.uid
                                    db.collection("users").document(uid).get()
                                        .addOnSuccessListener { doc ->
                                            isLoading = false
                                            val role = doc.getString("role") ?: "STUDENT"
                                            val intent = Intent(context, MainActivity::class.java).apply {
                                                putExtra("USER_TYPE", if (role == "ADMIN") "ADMIN" else "STUDENT")
                                            }
                                            context.startActivity(intent)
                                            (context as? LoginActivity)?.finish()
                                        }
                                        .addOnFailureListener {
                                            isLoading = false
                                            val intent = Intent(context, MainActivity::class.java).apply {
                                                putExtra("USER_TYPE", "STUDENT")
                                            }
                                            context.startActivity(intent)
                                            (context as? LoginActivity)?.finish()
                                        }
                                }
                                .addOnFailureListener { e ->
                                    isLoading = false
                                    errorMessage = e.localizedMessage ?: "登入失敗，請檢查帳號密碼"
                                }
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(if (isRegisterMode) "建立帳號" else "登入")
                }
                Spacer(modifier = Modifier.height(12.dp))
                TextButton(onClick = { isRegisterMode = !isRegisterMode; errorMessage = "" }) {
                    Text(if (isRegisterMode) "已有帳號？返回登入" else "還沒有帳號？立即註冊")
                }
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
