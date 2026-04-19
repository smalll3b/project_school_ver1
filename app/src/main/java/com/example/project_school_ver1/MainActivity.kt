package com.example.project_school_ver1

import android.graphics.Bitmap
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.contract.ActivityResultContracts.PickVisualMedia
import androidx.activity.result.contract.ActivityResultContracts.TakePicturePreview
import androidx.annotation.StringRes
import androidx.compose.foundation.Image
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
import androidx.compose.ui.graphics.asImageBitmap
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
import coil.compose.AsyncImage
import java.util.Locale
import kotlinx.coroutines.launch

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
data class Event(
    val id: String = "",
    var title: String = "",
    var date: String = "",
    var imageUrl: String = "",
    var posterText: String = "",
    var expiryDate: String = "",
    var posterStatus: String = ""
)

// Admin navigation
sealed class AdminScreenRoute(val route: String, @StringRes val titleRes: Int, val icon: ImageVector) {
    object Announcements : AdminScreenRoute("announcements", R.string.nav_announcements, Icons.Filled.Campaign)
    object Courses : AdminScreenRoute("courses", R.string.nav_courses, Icons.Filled.Book)
    object Events : AdminScreenRoute("events", R.string.nav_events, Icons.Filled.Event)
    object Users : AdminScreenRoute("users", R.string.nav_users, Icons.Filled.People)
    object Notifications : AdminScreenRoute("notifications", R.string.nav_notifications, Icons.Filled.Notifications)
}

val adminNavigationItems = listOf(
    AdminScreenRoute.Announcements,
    AdminScreenRoute.Courses,
    AdminScreenRoute.Events,
    AdminScreenRoute.Users,
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
            composable(AdminScreenRoute.Events.route) {
                EventManagementScreen()
            }
            composable(AdminScreenRoute.Users.route) {
                UserManagementScreen()
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
            val title = stringResource(screen.titleRes)
            NavigationBarItem(
                icon = { Icon(screen.icon, contentDescription = title) },
                label = { Text(title) },
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
                    Toast.makeText(context, "更新失敗", Toast.LENGTH_SHORT).show()
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
                    IconButton(onClick = {
                        db.collection("announcements").add(
                            mapOf("title" to "New Announcement", "content" to "")
                        )
                    }) {
                        Icon(Icons.Filled.Add, contentDescription = "Add Announcement", tint = Color.White)
                    }
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
                .addOnFailureListener { Toast.makeText(context, "更新失敗", Toast.LENGTH_SHORT).show() }
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
                    IconButton(onClick = {
                        db.collection("courses").add(mapOf("name" to "New Course", "time" to ""))
                    }) {
                        Icon(Icons.Filled.Add, contentDescription = "Add Course", tint = Color.White)
                    }
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
                .addOnFailureListener { Toast.makeText(context, "更新失敗", Toast.LENGTH_SHORT).show() }
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

// ─── Notification Management ───────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationManagementScreen() {
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
                        Toast.makeText(context, context.getString(R.string.notification_sent, title), Toast.LENGTH_SHORT).show()
                        title = ""
                        content = ""
                    }.addOnFailureListener {
                        Toast.makeText(context, context.getString(R.string.notification_send_failed), Toast.LENGTH_SHORT).show()
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(stringResource(R.string.send))
            }
        }
    }
}

// ─── Student Navigation ────────────────────────────────────────────────────

sealed class Screen(val route: String, @StringRes val titleRes: Int, val icon: ImageVector) {
    object Classes : Screen("classes", R.string.nav_classes, Icons.Filled.DateRange)
    object CampusMap : Screen("campus_map", R.string.nav_campus_map, Icons.Filled.LocationOn)
    object Esents : Screen("esents", R.string.nav_events, Icons.Filled.Info)
    object News : Screen("news", R.string.nav_news, Icons.AutoMirrored.Filled.List)
}

private object StudentExtraRoute {
    const val LeaveMessage = "leave_message"
    const val MessageFeed = "message_feed"
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
                    onGoToLeaveMessage = { navController.navigate(StudentExtraRoute.LeaveMessage) },
                    onGoToMessageFeed = { navController.navigate(StudentExtraRoute.MessageFeed) }
                )
            }
            composable(StudentExtraRoute.LeaveMessage) {
                LeaveMessageScreen(
                    onMessageSubmitted = {
                        navController.navigate(StudentExtraRoute.MessageFeed) {
                            popUpTo(StudentExtraRoute.LeaveMessage) { inclusive = true }
                        }
                    }
                )
            }
            composable(StudentExtraRoute.MessageFeed) { MessageFeedScreen() }
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
            val title = stringResource(screen.titleRes)
            NavigationBarItem(
                icon = { Icon(screen.icon, contentDescription = title) },
                label = { Text(title) },
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

// ─── Event Management ─────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EventManagementScreen() {
    val context = LocalContext.current
    val db = FirebaseFirestore.getInstance()
    val scope = rememberCoroutineScope()
    val events = remember { mutableStateListOf<Event>() }
    var isLoading by remember { mutableStateOf(true) }

    var showEditDialog by remember { mutableStateOf(false) }
    var editingEventId by remember { mutableStateOf("") }
    var editingTitle by remember { mutableStateOf("") }
    var editingDate by remember { mutableStateOf("") }
    var editingImageUrl by remember { mutableStateOf("") }
    var editingPosterText by remember { mutableStateOf("") }
    var editingPosterStatus by remember { mutableStateOf("") }
    var analysisMessage by remember { mutableStateOf("") }
    var isAnalyzingPoster by remember { mutableStateOf(false) }
    var isSaving by remember { mutableStateOf(false) }
    var selectedPosterBitmap by remember { mutableStateOf<Bitmap?>(null) }

    fun applyPosterAnalysis(result: EventPosterAnalysisResult) {
        editingPosterText = result.recognizedText
        editingPosterStatus = result.status.name.lowercase(Locale.US)
        val inferredTitle = inferPosterTitle(result.recognizedText)
        if (inferredTitle.isNotBlank() && editingTitle.isBlank()) {
            editingTitle = inferredTitle
        }
        if (result.detectedDate != null) {
            val detectedDate = formatPosterDate(result.detectedDate)
            editingDate = detectedDate
            analysisMessage = context.getString(
                R.string.poster_analysis_success,
                context.getString(result.status.labelRes),
                detectedDate
            )
        } else {
            analysisMessage = if (result.recognizedText.isNotBlank()) {
                context.getString(R.string.poster_analysis_pending_review)
            } else {
                context.getString(R.string.poster_analysis_failed)
            }
        }
    }

    val galleryPosterLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        val bitmap = loadBitmapFromUri(context, uri)
        if (bitmap == null) {
            analysisMessage = context.getString(R.string.poster_analysis_failed)
            return@rememberLauncherForActivityResult
        }
        selectedPosterBitmap = bitmap
        isAnalyzingPoster = true
        analysisMessage = ""
        scope.launch {
            applyPosterAnalysis(analyzeEventPosterBitmap(bitmap))
            isAnalyzingPoster = false
        }
    }

    val cameraPosterLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicturePreview()
    ) { bitmap ->
        if (bitmap == null) {
            analysisMessage = context.getString(R.string.poster_analysis_failed)
            return@rememberLauncherForActivityResult
        }
        selectedPosterBitmap = bitmap
        isAnalyzingPoster = true
        analysisMessage = ""
        scope.launch {
            applyPosterAnalysis(analyzeEventPosterBitmap(bitmap))
            isAnalyzingPoster = false
        }
    }

    DisposableEffect(Unit) {
        val listener = db.collection("events").addSnapshotListener { snapshot, _ ->
            isLoading = false
            if (snapshot != null) {
                events.clear()
                snapshot.documents.forEach { doc ->
                    events.add(
                        Event(
                            id = doc.id,
                            title = doc.getString("title") ?: "",
                            date = doc.getString("date") ?: "",
                            imageUrl = doc.getString("imageUrl") ?: "",
                            posterText = doc.getString("posterText") ?: "",
                            expiryDate = doc.getString("expiryDate") ?: "",
                            posterStatus = doc.getString("posterStatus") ?: ""
                        )
                    )
                }
            }
        }
        onDispose { listener.remove() }
    }

    if (showEditDialog) {
        AlertDialog(
            onDismissRequest = {
                if (!isSaving) showEditDialog = false
            },
            title = { Text(if (editingEventId.isEmpty()) "Add Event" else "Edit Event") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = editingTitle,
                        onValueChange = { editingTitle = it },
                        label = { Text("Title") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = editingDate,
                        onValueChange = { editingDate = it },
                        label = { Text("Date") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = editingImageUrl,
                        onValueChange = { editingImageUrl = it },
                        label = { Text("Image URL") },
                        placeholder = { Text("Paste a direct image URL") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    if (editingImageUrl.isNotBlank()) {
                        AsyncImage(
                            model = editingImageUrl,
                            contentDescription = "Event image preview",
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(160.dp)
                        )
                    }
                    selectedPosterBitmap?.let { bitmap ->
                        Image(
                            bitmap = bitmap.asImageBitmap(),
                            contentDescription = "Selected poster preview",
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(160.dp)
                        )
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                        Button(
                            onClick = {
                                galleryPosterLauncher.launch(
                                    PickVisualMediaRequest(PickVisualMedia.ImageOnly)
                                )
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(stringResource(R.string.pick_poster_from_gallery))
                        }
                        Button(
                            onClick = { cameraPosterLauncher.launch(null) },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(stringResource(R.string.capture_poster_photo))
                        }
                    }
                     Button(
                         onClick = {
                             if (editingImageUrl.isBlank() || isAnalyzingPoster) return@Button
                             isAnalyzingPoster = true
                             analysisMessage = ""
                             scope.launch {
                                applyPosterAnalysis(analyzeEventPosterImage(editingImageUrl))
                                 isAnalyzingPoster = false
                             }
                         },
                         enabled = editingImageUrl.isNotBlank() && !isAnalyzingPoster,
                         modifier = Modifier.fillMaxWidth()
                     ) {
                         Text(stringResource(R.string.analyze_poster))
                     }
                     if (isAnalyzingPoster) {
                         CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally))
                     }
                     if (analysisMessage.isNotBlank()) {
                         Text(text = analysisMessage)
                     }
                     if (editingPosterText.isNotBlank()) {
                         Text(text = "${stringResource(R.string.poster_text)}: ${editingPosterText.take(120)}")
                     }
                     EventStatusBadge(
                         status = resolveEventPosterStatus(editingPosterStatus, editingDate),
                         modifier = Modifier.align(Alignment.Start)
                     )
                    if (isSaving) {
                        CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally))
                    }
                 }
             },
            confirmButton = {
                TextButton(
                    enabled = !isSaving,
                    onClick = {
                        if (editingTitle.isBlank() || editingDate.isBlank()) {
                            Toast.makeText(context, "請填寫完整資料", Toast.LENGTH_SHORT).show()
                            return@TextButton
                        }

                        isSaving = true
                        val eventId = if (editingEventId.isBlank()) {
                            db.collection("events").document().id
                        } else {
                            editingEventId
                        }

                        val payload = mapOf(
                            "title" to editingTitle.trim(),
                            "date" to editingDate.trim(),
                            "imageUrl" to editingImageUrl.trim(),
                            "posterText" to editingPosterText.trim(),
                            "expiryDate" to extractPosterDate(editingDate.trim())?.let(::formatPosterDate).orEmpty(),
                            "posterStatus" to classifyEventPosterStatus(editingDate.trim()).name.lowercase(Locale.US)
                        )

                        db.collection("events").document(eventId).set(payload)
                            .addOnSuccessListener {
                                isSaving = false
                                showEditDialog = false
                            }
                            .addOnFailureListener {
                                isSaving = false
                                Toast.makeText(context, "儲存失敗", Toast.LENGTH_SHORT).show()
                            }
                    }
                ) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(
                    enabled = !isSaving,
                    onClick = { showEditDialog = false }
                ) {
                    Text("Cancel")
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.manage_events)) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF2196F3),
                    titleContentColor = Color.White
                ),
                actions = {
                    IconButton(onClick = {
                        editingEventId = ""
                        editingTitle = ""
                        editingDate = ""
                        editingImageUrl = ""
                        editingPosterText = ""
                        editingPosterStatus = ""
                        analysisMessage = ""
                        selectedPosterBitmap = null
                        showEditDialog = true
                    }) {
                        Icon(Icons.Filled.Add, contentDescription = "Add Event", tint = Color.White)
                    }
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
                items(events) { event ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            if (event.imageUrl.isNotBlank()) {
                                AsyncImage(
                                    model = event.imageUrl,
                                    contentDescription = event.title,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(140.dp)
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                            }
                            EventStatusBadge(
                                status = resolveEventPosterStatus(event.posterStatus, event.expiryDate.ifBlank { event.date })
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(text = event.title, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(text = event.date)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.End
                            ) {
                                IconButton(onClick = {
                                    editingEventId = event.id
                                    editingTitle = event.title
                                    editingDate = event.date
                                    editingImageUrl = event.imageUrl
                                    editingPosterText = event.posterText
                                    editingPosterStatus = event.posterStatus
                                    analysisMessage = ""
                                    selectedPosterBitmap = null
                                    showEditDialog = true
                                }) {
                                    Icon(Icons.Filled.Edit, contentDescription = "Edit")
                                }
                                IconButton(onClick = {
                                    db.collection("events").document(event.id).delete()
                                }) {
                                    Icon(Icons.Filled.Delete, contentDescription = "Delete")
                                }
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
        }
    }
}
