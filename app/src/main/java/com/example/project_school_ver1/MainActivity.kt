package com.example.project_school_ver1

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.project_school_ver1.ui.theme.Project_school_ver1Theme
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query

class MainActivity : ComponentActivity() {
    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(AppLanguage.wrap(newBase))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val userType = intent.getStringExtra("USER_TYPE")
        setContent {
            Project_school_ver1Theme {
                if (userType == "ADMIN") {
                    AdminScreen()
                } else {
                    CampusAppMainScreen()
                }
            }
        }
    }
}

// Data classes
data class Announcement(val id: String = "", var title: String = "", var content: String = "")
data class Course(val id: String = "", var name: String = "", var time: String = "")
data class User(val id: String = "", var name: String = "", var role: String = "")
data class NewsItem(val id: String = "", var title: String = "", var content: String = "", var timestamp: com.google.firebase.Timestamp? = null)

// Admin navigation
sealed class AdminScreenRoute(val route: String, val titleRes: Int, val icon: ImageVector) {
    object Announcements : AdminScreenRoute("announcements", R.string.nav_announcements, Icons.Filled.Campaign)
    object Courses : AdminScreenRoute("courses", R.string.nav_courses, Icons.Filled.Book)
    object Users : AdminScreenRoute("users", R.string.nav_users, Icons.Filled.People)
    object News : AdminScreenRoute("news_manage", R.string.nav_news, Icons.AutoMirrored.Filled.List)
    object Notifications : AdminScreenRoute("notifications", R.string.nav_notifications, Icons.Filled.Notifications)
}

val adminNavigationItems = listOf(
    AdminScreenRoute.Announcements,
    AdminScreenRoute.Courses,
    AdminScreenRoute.Users,
    AdminScreenRoute.News,
    AdminScreenRoute.Notifications
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminScreen() {
    val navController = rememberNavController()
    Scaffold(
        bottomBar = {
            AdminBottomNavigationBar(navController = navController)
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = AdminScreenRoute.Announcements.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(AdminScreenRoute.Announcements.route) {
                AnnouncementManagementScreen()
            }
            composable(AdminScreenRoute.Courses.route) {
                CourseManagementScreen()
            }
            composable(AdminScreenRoute.Users.route) {
                UserManagementScreen()
            }
            composable(AdminScreenRoute.News.route) {
                AdminNewsManagementScreen()
            }
            composable(AdminScreenRoute.Notifications.route) {
                NotificationManagementScreen()
            }
        }
    }
}

@Composable
fun AdminBottomNavigationBar(navController: NavHostController) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    NavigationBar {
        adminNavigationItems.forEach { screen ->
            NavigationBarItem(
                icon = { Icon(screen.icon, contentDescription = stringResource(screen.titleRes)) },
                label = { Text(stringResource(screen.titleRes)) },
                selected = currentRoute == screen.route,
                onClick = {
                    navController.navigate(screen.route) {
                        popUpTo(navController.graph.startDestinationId) { saveState = true }
                        launchSingleTop = true
                        restoreState = true
                    }
                }
            )
        }
    }
}

// ─── Announcement Management ───────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnnouncementManagementScreen() {
    val context = LocalContext.current
    val db = FirebaseFirestore.getInstance()
    val announcements = remember { mutableStateListOf<Announcement>() }
    var isLoading by remember { mutableStateOf(true) }

    // Real-time listener
    DisposableEffect(Unit) {
        val listener = db.collection("announcements")
            .addSnapshotListener { snapshot, _ ->
                isLoading = false
                if (snapshot != null) {
                    announcements.clear()
                    snapshot.documents.forEach { doc ->
                        announcements.add(
                            Announcement(
                                id = doc.id,
                                title = doc.getString("title") ?: "",
                                content = doc.getString("content") ?: ""
                            )
                        )
                    }
                }
            }
        onDispose { listener.remove() }
    }

    val editAnnouncementLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val data = result.data
            val id = data?.getStringExtra("ANNOUNCEMENT_ID") ?: return@rememberLauncherForActivityResult
            val title = data.getStringExtra("ANNOUNCEMENT_TITLE") ?: return@rememberLauncherForActivityResult
            val content = data.getStringExtra("ANNOUNCEMENT_CONTENT") ?: return@rememberLauncherForActivityResult
            db.collection("announcements").document(id)
                .update("title", title, "content", content)
                .addOnFailureListener {
                    Toast.makeText(context, context.getString(R.string.update_failed), Toast.LENGTH_SHORT).show()
                }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.manage_announcements)) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF2196F3),
                    titleContentColor = Color.White
                ),
                actions = {
                    LanguageSwitcherMenu { languageTag ->
                        AppLanguage.setLanguage(context, languageTag)
                    }
                    IconButton(onClick = {
                        db.collection("announcements").add(
                            mapOf("title" to "New Announcement", "content" to "")
                        )
                    }) {
                        Icon(Icons.Filled.Add, contentDescription = "Add Announcement", tint = Color.White)
                    }
                    IconButton(onClick = {
                        FirebaseAuth.getInstance().signOut()
                        val intent = Intent(context, LoginActivity::class.java)
                        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                        context.startActivity(intent)
                    }) {
                        Icon(Icons.Filled.ExitToApp, contentDescription = stringResource(R.string.logout), tint = Color.White)
                    }
                }
            )
        }
    ) { paddingValues ->
        if (isLoading) {
            Box(Modifier.fillMaxSize().padding(paddingValues), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            LazyColumn(modifier = Modifier.padding(paddingValues).padding(16.dp)) {
                items(announcements) { announcement ->
                    AnnouncementCard(
                        announcement = announcement,
                        onEdit = {
                            val intent = Intent(context, EditAnnouncementActivity::class.java).apply {
                                putExtra("ANNOUNCEMENT_ID", announcement.id)
                                putExtra("ANNOUNCEMENT_TITLE", announcement.title)
                                putExtra("ANNOUNCEMENT_CONTENT", announcement.content)
                            }
                            editAnnouncementLauncher.launch(intent)
                        },
                        onDelete = {
                            db.collection("announcements").document(announcement.id).delete()
                        }
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
        }
    }
}

@Composable
fun AnnouncementCard(announcement: Announcement, onEdit: () -> Unit, onDelete: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(text = announcement.title, fontWeight = FontWeight.Bold, fontSize = 18.sp)
            Spacer(modifier = Modifier.height(4.dp))
            Text(text = announcement.content)
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                IconButton(onClick = onEdit) { Icon(Icons.Filled.Edit, contentDescription = "Edit") }
                IconButton(onClick = onDelete) { Icon(Icons.Filled.Delete, contentDescription = "Delete") }
            }
        }
    }
}

// ─── Course Management ─────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CourseManagementScreen() {
    val context = LocalContext.current
    val db = FirebaseFirestore.getInstance()
    val courses = remember { mutableStateListOf<Course>() }
    var isLoading by remember { mutableStateOf(true) }

    DisposableEffect(Unit) {
        val listener = db.collection("courses")
            .addSnapshotListener { snapshot, _ ->
                isLoading = false
                if (snapshot != null) {
                    courses.clear()
                    snapshot.documents.forEach { doc ->
                        courses.add(Course(id = doc.id, name = doc.getString("name") ?: "", time = doc.getString("time") ?: ""))
                    }
                }
            }
        onDispose { listener.remove() }
    }

    val editCourseLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val data = result.data
            val id = data?.getStringExtra("COURSE_ID") ?: return@rememberLauncherForActivityResult
            val name = data.getStringExtra("COURSE_NAME") ?: return@rememberLauncherForActivityResult
            val time = data.getStringExtra("COURSE_TIME") ?: return@rememberLauncherForActivityResult
            db.collection("courses").document(id).update("name", name, "time", time)
                .addOnFailureListener { Toast.makeText(context, context.getString(R.string.update_failed), Toast.LENGTH_SHORT).show() }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.manage_courses)) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF2196F3),
                    titleContentColor = Color.White
                ),
                actions = {
                    LanguageSwitcherMenu { languageTag ->
                        AppLanguage.setLanguage(context, languageTag)
                    }
                    IconButton(onClick = {
                        db.collection("courses").add(mapOf("name" to "New Course", "time" to ""))
                    }) {
                        Icon(Icons.Filled.Add, contentDescription = "Add Course", tint = Color.White)
                    }
                }
            )
        }
    ) { paddingValues ->
        if (isLoading) {
            Box(Modifier.fillMaxSize().padding(paddingValues), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            LazyColumn(modifier = Modifier.padding(paddingValues).padding(16.dp)) {
                items(courses) { course ->
                    CourseCard(
                        course = course,
                        onEdit = {
                            val intent = Intent(context, EditCourseActivity::class.java).apply {
                                putExtra("COURSE_ID", course.id)
                                putExtra("COURSE_NAME", course.name)
                                putExtra("COURSE_TIME", course.time)
                            }
                            editCourseLauncher.launch(intent)
                        },
                        onDelete = { db.collection("courses").document(course.id).delete() }
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
        }
    }
}

@Composable
fun CourseCard(course: Course, onEdit: () -> Unit, onDelete: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(text = course.name, fontWeight = FontWeight.Bold, fontSize = 18.sp)
            Spacer(modifier = Modifier.height(4.dp))
            Text(text = course.time)
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                IconButton(onClick = onEdit) { Icon(Icons.Filled.Edit, contentDescription = "Edit") }
                IconButton(onClick = onDelete) { Icon(Icons.Filled.Delete, contentDescription = "Delete") }
            }
        }
    }
}

// ─── User Management ───────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserManagementScreen() {
    val context = LocalContext.current
    val db = FirebaseFirestore.getInstance()
    val users = remember { mutableStateListOf<User>() }
    var isLoading by remember { mutableStateOf(true) }

    DisposableEffect(Unit) {
        val listener = db.collection("users")
            .addSnapshotListener { snapshot, _ ->
                isLoading = false
                if (snapshot != null) {
                    users.clear()
                    snapshot.documents.forEach { doc ->
                        users.add(User(id = doc.id, name = doc.getString("name") ?: "", role = doc.getString("role") ?: ""))
                    }
                }
            }
        onDispose { listener.remove() }
    }

    val editUserLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val data = result.data
            val id = data?.getStringExtra("USER_ID") ?: return@rememberLauncherForActivityResult
            val name = data.getStringExtra("USER_NAME") ?: return@rememberLauncherForActivityResult
            val role = data.getStringExtra("USER_ROLE") ?: return@rememberLauncherForActivityResult
            db.collection("users").document(id).update("name", name, "role", role)
                .addOnFailureListener { Toast.makeText(context, context.getString(R.string.update_failed), Toast.LENGTH_SHORT).show() }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.manage_users)) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF2196F3),
                    titleContentColor = Color.White
                ),
                actions = {
                    LanguageSwitcherMenu { languageTag ->
                        AppLanguage.setLanguage(context, languageTag)
                    }
                }
            )
        }
    ) { paddingValues ->
        if (isLoading) {
            Box(Modifier.fillMaxSize().padding(paddingValues), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            LazyColumn(modifier = Modifier.padding(paddingValues).padding(16.dp)) {
                items(users) { user ->
                    UserCard(
                        user = user,
                        onEdit = {
                            val intent = Intent(context, EditUserActivity::class.java).apply {
                                putExtra("USER_ID", user.id)
                                putExtra("USER_NAME", user.name)
                                putExtra("USER_ROLE", user.role)
                            }
                            editUserLauncher.launch(intent)
                        },
                        onDelete = { db.collection("users").document(user.id).delete() }
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
        }
    }
}

@Composable
fun UserCard(user: User, onEdit: () -> Unit, onDelete: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(text = user.name, fontWeight = FontWeight.Bold, fontSize = 18.sp)
            Spacer(modifier = Modifier.height(4.dp))
            Text(text = user.role)
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                IconButton(onClick = onEdit) { Icon(Icons.Filled.Edit, contentDescription = "Edit") }
                IconButton(onClick = onDelete) { Icon(Icons.Filled.Delete, contentDescription = "Delete") }
            }
        }
    }
}

// ─── Admin News Management ────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminNewsManagementScreen() {
    val context = LocalContext.current
    val db = FirebaseFirestore.getInstance()
    val newsItems = remember { mutableStateListOf<NewsItem>() }
    var title by remember { mutableStateOf("") }
    var content by remember { mutableStateOf("") }

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

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.manage_news)) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF2196F3),
                    titleContentColor = Color.White
                ),
                actions = {
                    LanguageSwitcherMenu { languageTag ->
                        AppLanguage.setLanguage(context, languageTag)
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
        ) {
            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text(stringResource(R.string.news_title)) },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = content,
                onValueChange = { content = it },
                label = { Text(stringResource(R.string.news_content)) },
                modifier = Modifier.fillMaxWidth(),
                minLines = 3
            )
            Spacer(modifier = Modifier.height(12.dp))
            Button(
                onClick = {
                    if (title.isBlank()) {
                        Toast.makeText(context, context.getString(R.string.news_title_required), Toast.LENGTH_SHORT).show()
                        return@Button
                    }
                    db.collection("news").add(
                        mapOf(
                            "title" to title,
                            "content" to content,
                            "timestamp" to com.google.firebase.Timestamp.now()
                        )
                    ).addOnSuccessListener {
                        Toast.makeText(context, context.getString(R.string.news_added), Toast.LENGTH_SHORT).show()
                        title = ""
                        content = ""
                    }.addOnFailureListener {
                        Toast.makeText(context, context.getString(R.string.add_failed), Toast.LENGTH_SHORT).show()
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(stringResource(R.string.add_news))
            }

            Spacer(modifier = Modifier.height(12.dp))

            if (newsItems.isEmpty()) {
                Text(stringResource(R.string.no_news_admin))
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(newsItems) { item ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(text = item.title, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                                if (item.content.isNotBlank()) {
                                    Text(text = item.content, modifier = Modifier.padding(top = 6.dp))
                                }
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.End
                                ) {
                                    IconButton(onClick = {
                                        db.collection("news").document(item.id).delete()
                                    }) {
                                        Icon(Icons.Filled.Delete, contentDescription = "Delete News")
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// ─── Notification Management ───────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationManagementScreen(onNotificationSent: (() -> Unit)? = null) {
    var title by remember { mutableStateOf("") }
    var content by remember { mutableStateOf("") }
    val context = LocalContext.current
    val db = FirebaseFirestore.getInstance()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.send_notification)) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF2196F3),
                    titleContentColor = Color.White
                ),
                actions = {
                    LanguageSwitcherMenu { languageTag ->
                        AppLanguage.setLanguage(context, languageTag)
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .padding(paddingValues)
                .padding(16.dp)
        ) {
            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text(stringResource(R.string.title_label)) },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = content,
                onValueChange = { content = it },
                label = { Text(stringResource(R.string.content_label)) },
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = {
                    if (title.isBlank()) {
                        Toast.makeText(context, context.getString(R.string.notification_title_required), Toast.LENGTH_SHORT).show()
                        return@Button
                    }
                    db.collection("notifications").add(
                        mapOf(
                            "title" to title,
                            "content" to content,
                            "timestamp" to com.google.firebase.Timestamp.now()
                        )
                    ).addOnSuccessListener {
                        Toast.makeText(
                            context,
                            context.getString(R.string.notification_sent, title),
                            Toast.LENGTH_SHORT
                        ).show()
                        title = ""
                        content = ""
                        onNotificationSent?.invoke()
                    }.addOnFailureListener {
                        Toast.makeText(context, context.getString(R.string.notification_send_failed), Toast.LENGTH_SHORT).show()
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(stringResource(R.string.send_notification))
            }
        }
    }
}

// ─── Student Message Compose ───────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StudentMessageComposeScreen(onMessageSent: (() -> Unit)? = null) {
    var title by remember { mutableStateOf("") }
    var content by remember { mutableStateOf("") }
    val context = LocalContext.current
    val db = FirebaseFirestore.getInstance()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.leave_message)) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF2196F3),
                    titleContentColor = Color.White
                ),
                actions = {
                    LanguageSwitcherMenu { languageTag ->
                        AppLanguage.setLanguage(context, languageTag)
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .padding(paddingValues)
                .padding(16.dp)
        ) {
            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text(stringResource(R.string.optional_title)) },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = content,
                onValueChange = { content = it },
                label = { Text(stringResource(R.string.message_content)) },
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = {
                    if (content.isBlank()) {
                        Toast.makeText(context, context.getString(R.string.message_content_required), Toast.LENGTH_SHORT).show()
                        return@Button
                    }
                    db.collection("messages").add(
                        mapOf(
                            "title" to title,
                            "content" to content,
                            "createdAt" to com.google.firebase.Timestamp.now()
                        )
                    ).addOnSuccessListener {
                        Toast.makeText(context, context.getString(R.string.message_sent), Toast.LENGTH_SHORT).show()
                        title = ""
                        content = ""
                        onMessageSent?.invoke()
                    }.addOnFailureListener {
                        Toast.makeText(context, context.getString(R.string.message_send_failed), Toast.LENGTH_SHORT).show()
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(stringResource(R.string.submit_message))
            }
        }
    }
}

// ─── Student Navigation ────────────────────────────────────────────────────

sealed class Screen(val route: String, val titleRes: Int, val icon: ImageVector) {
    object Classes : Screen("classes", R.string.nav_classes, Icons.Filled.DateRange)
    object CampusMap : Screen("campus_map", R.string.nav_campus_map, Icons.Filled.LocationOn)
    object Esents : Screen("esents", R.string.nav_events, Icons.Filled.Info)
    object News : Screen("news", R.string.nav_news, Icons.AutoMirrored.Filled.List)
    object LeaveMessage : Screen("leave_message", R.string.nav_leave_message, Icons.Filled.Send)
    object MessageFeed : Screen("message_feed", R.string.nav_messages, Icons.Filled.Forum)
}

val navigationItems = listOf(
    Screen.Classes,
    Screen.CampusMap,
    Screen.Esents,
    Screen.News
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CampusAppMainScreen() {
    val navController = rememberNavController()
    val context = LocalContext.current

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.campus_app)) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF2196F3),
                    titleContentColor = Color.White
                ),
                actions = {
                    LanguageSwitcherMenu { languageTag ->
                        AppLanguage.setLanguage(context, languageTag)
                    }
                    IconButton(onClick = {
                        FirebaseAuth.getInstance().signOut()
                        val intent = Intent(context, LoginActivity::class.java)
                        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                        context.startActivity(intent)
                    }) {
                        Icon(Icons.Filled.ExitToApp, contentDescription = stringResource(R.string.logout), tint = Color.White)
                    }
                }
            )
        },
        bottomBar = {
            BottomNavigationBar(navController = navController)
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Classes.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(Screen.Classes.route) { HomeScreen() }
            composable(Screen.CampusMap.route) { CampusMapScreen() }
            composable(Screen.Esents.route) { EsentsScreen() }
            composable(Screen.News.route) {
                NewsScreen(
                    onGoToLeaveMessage = { navController.navigate(Screen.LeaveMessage.route) },
                    onGoToMessageFeed = { navController.navigate(Screen.MessageFeed.route) }
                )
            }
            composable(Screen.LeaveMessage.route) {
                StudentMessageComposeScreen(
                    onMessageSent = {
                        navController.navigate(Screen.MessageFeed.route) {
                            launchSingleTop = true
                        }
                    }
                )
            }
            composable(Screen.MessageFeed.route) {
                MessageFeedScreen()
            }
        }
    }
}

// ─── Student Home ──────────────────────────────────────────────────────────

@Composable
fun HomeScreen() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF5F5F5))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        TodayClassesCard()
        AnnouncementsCard()
    }
}

@Composable
fun TodayClassesCard() {
    val db = FirebaseFirestore.getInstance()
    val courses = remember { mutableStateListOf<Course>() }

    DisposableEffect(Unit) {
        val listener = db.collection("courses").addSnapshotListener { snapshot, _ ->
            if (snapshot != null) {
                courses.clear()
                snapshot.documents.forEach { doc ->
                    courses.add(Course(id = doc.id, name = doc.getString("name") ?: "", time = doc.getString("time") ?: ""))
                }
            }
        }
        onDispose { listener.remove() }
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(text = stringResource(R.string.todays_classes), fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.Black)
            if (courses.isEmpty()) {
                Text(text = stringResource(R.string.no_classes_today), color = Color(0xFF555555), modifier = Modifier.padding(top = 8.dp))
            } else {
                courses.forEach { course ->
                    Text(text = "${course.name} - ${course.time}", color = Color(0xFF555555), modifier = Modifier.padding(top = 4.dp))
                }
            }
        }
    }
}

@Composable
fun AnnouncementsCard() {
    val db = FirebaseFirestore.getInstance()
    val announcements = remember { mutableStateListOf<Announcement>() }

    DisposableEffect(Unit) {
        val listener = db.collection("announcements").addSnapshotListener { snapshot, _ ->
            if (snapshot != null) {
                announcements.clear()
                snapshot.documents.forEach { doc ->
                    announcements.add(Announcement(id = doc.id, title = doc.getString("title") ?: "", content = doc.getString("content") ?: ""))
                }
            }
        }
        onDispose { listener.remove() }
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(text = stringResource(R.string.announcements_title), fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.Black)
            if (announcements.isEmpty()) {
                Text(text = stringResource(R.string.no_announcements), color = Color(0xFF555555), modifier = Modifier.padding(top = 8.dp))
            } else {
                announcements.take(3).forEach { item ->
                    Text(text = item.title, color = Color(0xFF555555), modifier = Modifier.padding(top = 4.dp))
                }
            }
        }
    }
}

@Composable
fun BottomNavigationBar(navController: NavHostController) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    NavigationBar(containerColor = Color.White) {
        navigationItems.forEach { screen ->
            NavigationBarItem(
                icon = { Icon(screen.icon, contentDescription = stringResource(screen.titleRes)) },
                label = { Text(stringResource(screen.titleRes)) },
                selected = currentRoute == screen.route,
                onClick = {
                    navController.navigate(screen.route) {
                        popUpTo(navController.graph.startDestinationId) { saveState = true }
                        launchSingleTop = true
                        restoreState = true
                    }
                }
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun CampusAppMainScreenPreview() {
    Project_school_ver1Theme {
        CampusAppMainScreen()
    }
}
