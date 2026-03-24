package com.example.project_school_ver1

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AdminPanelSettings
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

// ── 管理員密碼（可自行修改）──────────────────────────────
private const val ADMIN_SECRET = "admin1234"

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

    val loginLabel = stringResource(R.string.login)
    val registerLabel = stringResource(R.string.register)
    val adminLoginLabel = stringResource(R.string.admin_login)
    val adminPasswordLabel = stringResource(R.string.admin_password)
    val confirmLabel = stringResource(R.string.confirm)
    val cancelLabel = stringResource(R.string.cancel)
    val enterAdminPasswordLabel = stringResource(R.string.enter_admin_password)
    val incorrectPasswordLabel = stringResource(R.string.incorrect_password)
    val enterEmailAndPasswordLabel = stringResource(R.string.enter_email_and_password)
    val registerFailedLabel = stringResource(R.string.register_failed)
    val loginFailedLabel = stringResource(R.string.login_failed)

    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf("") }
    var isRegisterMode by remember { mutableStateOf(false) }

    // 管理員密碼 dialog
    var showAdminDialog by remember { mutableStateOf(false) }
    var adminCode by remember { mutableStateOf("") }
    var adminCodeError by remember { mutableStateOf("") }

    // 管理員密碼驗證 dialog
    if (showAdminDialog) {
        AlertDialog(
            onDismissRequest = { showAdminDialog = false; adminCode = ""; adminCodeError = "" },
            icon = { Icon(Icons.Filled.AdminPanelSettings, contentDescription = null, tint = Color(0xFF2196F3)) },
            title = { Text(adminLoginLabel) },
            text = {
                Column {
                    Text(enterAdminPasswordLabel, fontSize = 14.sp, modifier = Modifier.padding(bottom = 12.dp))
                    OutlinedTextField(
                        value = adminCode,
                        onValueChange = { adminCode = it; adminCodeError = "" },
                        label = { Text(adminPasswordLabel) },
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        isError = adminCodeError.isNotEmpty()
                    )
                    if (adminCodeError.isNotEmpty()) {
                        Text(adminCodeError, color = Color.Red, fontSize = 12.sp, modifier = Modifier.padding(top = 4.dp))
                    }
                }
            },
            confirmButton = {
                Button(onClick = {
                    if (adminCode == ADMIN_SECRET) {
                        showAdminDialog = false
                        adminCode = ""
                        val intent = Intent(context, MainActivity::class.java).apply {
                            putExtra("USER_TYPE", "ADMIN")
                        }
                        context.startActivity(intent)
                        (context as? LoginActivity)?.finish()
                    } else {
                        adminCodeError = incorrectPasswordLabel
                    }
                }) {
                    Text(confirmLabel)
                }
            },
            dismissButton = {
                TextButton(onClick = { showAdminDialog = false; adminCode = ""; adminCodeError = "" }) {
                    Text(cancelLabel)
                }
            }
        )
    }

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
                text = if (isRegisterMode) registerLabel else loginLabel,
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
                            errorMessage = enterEmailAndPasswordLabel
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
                                    errorMessage = e.localizedMessage ?: registerFailedLabel
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
                                    errorMessage = e.localizedMessage ?: loginFailedLabel
                                }
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(if (isRegisterMode) registerLabel else loginLabel)
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

                // ── 管理員入口 ──────────────────────────────────────
                Spacer(modifier = Modifier.height(24.dp))
                HorizontalDivider()
                Spacer(modifier = Modifier.height(16.dp))
                OutlinedButton(
                    onClick = { showAdminDialog = true },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        Icons.Filled.AdminPanelSettings,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(adminLoginLabel)
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
