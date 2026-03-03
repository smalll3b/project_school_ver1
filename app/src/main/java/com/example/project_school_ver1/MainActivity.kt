package com.example.project_school_ver1

import android.app.Activity
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

class MainActivity : ComponentActivity() {
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

// Admin navigation
sealed class AdminScreenRoute(val route: String, val title: String, val icon: ImageVector) {
    object Announcements : AdminScreenRoute("announcements", "Announcements", Icons.Filled.Campaign)
    object Courses : AdminScreenRoute("courses", "Courses", Icons.Filled.Book)
    object Users : AdminScreenRoute("users", "Users", Icons.Filled.People)
    object Notifications : AdminScreenRoute("notifications", "Notifications", Icons.Filled.Notifications)
}

val adminNavigationItems = listOf(
    AdminScreenRoute.Announcements,
    AdminScreenRoute.Courses,
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
            NavigationBarItem(
                icon = { Icon(screen.icon, contentDescription = screen.title) },
                label = { Text(screen.title) },
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
                title = { Text("Manage Announcements") },
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
                    IconButton(onClick = {
                        FirebaseAuth.getInstance().signOut()
                        val intent = Intent(context, LoginActivity::class.java)
                        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                        context.startActivity(intent)
                    }) {
                        Icon(Icons.Filled.ExitToApp, contentDescription = "Logout", tint = Color.White)
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
                title = { Text("Manage Courses") },
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
                title = { Text("Manage Users") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF2196F3),
                    titleContentColor = Color.White
                )
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
                title = { Text("Send Notification") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF2196F3),
                    titleContentColor = Color.White
                )
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
                label = { Text("Title") },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = content,
                onValueChange = { content = it },
                label = { Text("Content") },
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = {
                    if (title.isBlank()) {
                        Toast.makeText(context, "請輸入標題", Toast.LENGTH_SHORT).show()
                        return@Button
                    }
                    db.collection("notifications").add(
                        mapOf(
                            "title" to title,
                            "content" to content,
                            "timestamp" to com.google.firebase.Timestamp.now()
                        )
                    ).addOnSuccessListener {
                        Toast.makeText(context, "通知已發送：$title", Toast.LENGTH_SHORT).show()
                        title = ""
                        content = ""
                    }.addOnFailureListener {
                        Toast.makeText(context, "發送失敗", Toast.LENGTH_SHORT).show()
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Send")
            }
        }
    }
}

// ─── Student Navigation ────────────────────────────────────────────────────

sealed class Screen(val route: String, val title: String, val icon: ImageVector) {
    object Classes : Screen("classes", "Classes", Icons.Filled.DateRange)
    object CampusMap : Screen("campus_map", "Campus Map", Icons.Filled.LocationOn)
    object Esents : Screen("esents", "Events", Icons.Filled.Info)
    object News : Screen("news", "News", Icons.AutoMirrored.Filled.List)
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
                title = { Text("Campus App") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF2196F3),
                    titleContentColor = Color.White
                ),
                actions = {
                    IconButton(onClick = {
                        FirebaseAuth.getInstance().signOut()
                        val intent = Intent(context, LoginActivity::class.java)
                        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                        context.startActivity(intent)
                    }) {
                        Icon(Icons.Filled.ExitToApp, contentDescription = "Logout", tint = Color.White)
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
            composable(Screen.News.route) { NewsScreen() }
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
            Text(text = "Today's Classes", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.Black)
            if (courses.isEmpty()) {
                Text(text = "No classes today.", color = Color(0xFF555555), modifier = Modifier.padding(top = 8.dp))
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
            Text(text = "Announcements", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.Black)
            if (announcements.isEmpty()) {
                Text(text = "No announcements.", color = Color(0xFF555555), modifier = Modifier.padding(top = 8.dp))
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
                icon = { Icon(screen.icon, contentDescription = screen.title) },
                label = { Text(screen.title) },
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
