package com.example.project_school_ver1

import android.content.Context
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
import androidx.compose.ui.res.stringResource
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
    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(AppLanguage.wrap(newBase))
    }

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
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                LanguageSwitcherMenu { languageTag ->
                    AppLanguage.setLanguage(context, languageTag)
                }
            }
            Text(
                text = stringResource(R.string.login_title),
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF2196F3),
                modifier = Modifier.padding(bottom = 8.dp)
            )
            Text(
                text = if (isRegisterMode) stringResource(R.string.register) else stringResource(R.string.login),
                fontSize = 18.sp,
                modifier = Modifier.padding(bottom = 24.dp)
            )

            OutlinedTextField(
                value = email,
                onValueChange = { email = it },
                label = { Text(stringResource(R.string.email)) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            Spacer(modifier = Modifier.height(12.dp))
            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                label = { Text(stringResource(R.string.password)) },
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
                            errorMessage = context.getString(R.string.enter_email_and_password)
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
                                    errorMessage = e.localizedMessage ?: context.getString(R.string.register_failed)
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
                                    errorMessage = e.localizedMessage ?: context.getString(R.string.login_failed)
                                }
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(if (isRegisterMode) stringResource(R.string.register) else stringResource(R.string.login))
                }
                Spacer(modifier = Modifier.height(12.dp))
                TextButton(onClick = { isRegisterMode = !isRegisterMode; errorMessage = "" }) {
                    Text(
                        if (isRegisterMode) {
                            stringResource(R.string.already_have_account)
                        } else {
                            stringResource(R.string.no_account_register)
                        }
                    )
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
