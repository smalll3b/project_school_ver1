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
data class Announcement(val id: Int, var title: String, var content: String)
data class Course(val id: Int, var name: String, var time: String)
data class User(val id: Int, var name: String, var role: String)

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
                        popUpTo(navController.graph.startDestinationId) {
                            saveState = true
                        }
                        launchSingleTop = true
                        restoreState = true
                    }
                }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnnouncementManagementScreen() {
    val context = LocalContext.current
    val announcements = remember {
        mutableStateListOf(
            Announcement(1, "Announcement 1", "This is the first announcement."),
            Announcement(2, "Announcement 2", "This is the second announcement.")
        )
    }

    val editAnnouncementLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val data = result.data
            val id = data?.getIntExtra("ANNOUNCEMENT_ID", -1)
            val title = data?.getStringExtra("ANNOUNCEMENT_TITLE")
            val content = data?.getStringExtra("ANNOUNCEMENT_CONTENT")

            if (id != -1 && title != null && content != null) {
                val index = announcements.indexOfFirst { it.id == id }
                if (index != -1) {
                    announcements[index] = announcements[index].copy(title = title, content = content)
                }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Manage Announcements") },
                actions = {
                    IconButton(onClick = { announcements.add(Announcement(announcements.size + 1, "New Announcement", "")) }) {
                        Icon(Icons.Filled.Add, contentDescription = "Add Announcement")
                    }
                }
            )
        }
    ) { paddingValues ->
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
                    onDelete = { announcements.remove(announcement) }
                )
                Spacer(modifier = Modifier.height(8.dp))
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
                IconButton(onClick = onEdit) {
                    Icon(Icons.Filled.Edit, contentDescription = "Edit")
                }
                IconButton(onClick = onDelete) {
                    Icon(Icons.Filled.Delete, contentDescription = "Delete")
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CourseManagementScreen() {
    val context = LocalContext.current
    val courses = remember {
        mutableStateListOf(
            Course(1, "History", "9:00 AM - 10:30 AM"),
            Course(2, "Biology", "11:00 AM - 12:00 PM")
        )
    }

    val editCourseLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val data = result.data
            val id = data?.getIntExtra("COURSE_ID", -1)
            val name = data?.getStringExtra("COURSE_NAME")
            val time = data?.getStringExtra("COURSE_TIME")

            if (id != -1 && name != null && time != null) {
                val index = courses.indexOfFirst { it.id == id }
                if (index != -1) {
                    courses[index] = courses[index].copy(name = name, time = time)
                }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Manage Courses") },
                actions = {
                    IconButton(onClick = { courses.add(Course(courses.size + 1, "New Course", "")) }) {
                        Icon(Icons.Filled.Add, contentDescription = "Add Course")
                    }
                }
            )
        }
    ) { paddingValues ->
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
                    onDelete = { courses.remove(course) }
                )
                Spacer(modifier = Modifier.height(8.dp))
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
                IconButton(onClick = onEdit) {
                    Icon(Icons.Filled.Edit, contentDescription = "Edit")
                }
                IconButton(onClick = onDelete) {
                    Icon(Icons.Filled.Delete, contentDescription = "Delete")
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserManagementScreen() {
    val context = LocalContext.current
    val users = remember {
        mutableStateListOf(
            User(1, "Alice", "Student"),
            User(2, "Bob", "Student"),
            User(3, "Charlie", "Teacher")
        )
    }

    val editUserLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val data = result.data
            val id = data?.getIntExtra("USER_ID", -1)
            val name = data?.getStringExtra("USER_NAME")
            val role = data?.getStringExtra("USER_ROLE")

            if (id != -1 && name != null && role != null) {
                val index = users.indexOfFirst { it.id == id }
                if (index != -1) {
                    users[index] = users[index].copy(name = name, role = role)
                }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Manage Users") },
                actions = {
                    IconButton(onClick = { users.add(User(users.size + 1, "New User", "")) }) {
                        Icon(Icons.Filled.Add, contentDescription = "Add User")
                    }
                }
            )
        }
    ) { paddingValues ->
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
                    onDelete = { users.remove(user) }
                )
                Spacer(modifier = Modifier.height(8.dp))
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
                IconButton(onClick = onEdit) {
                    Icon(Icons.Filled.Edit, contentDescription = "Edit")
                }
                IconButton(onClick = onDelete) {
                    Icon(Icons.Filled.Delete, contentDescription = "Delete")
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationManagementScreen() {
    var title by remember { mutableStateOf("") }
    var content by remember { mutableStateOf("") }
    val context = LocalContext.current

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Send Notification") })
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
                modifier = Modifier.fillMaxWidth().weight(1f)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = {
                    // Simulate sending a notification
                    Toast.makeText(context, "Notification sent: $title", Toast.LENGTH_SHORT).show()
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Send")
            }
        }
    }
}


sealed class Screen(val route: String, val title: String, val icon: ImageVector) {
    object Classes : Screen("classes", "Classes", Icons.Filled.DateRange)
    object CampusMap : Screen("campus_map", "Campus Map", Icons.Filled.LocationOn)
    object Esents : Screen("esents", "Esents", Icons.Filled.Info)
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

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text("Campus App") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF2196F3),
                    titleContentColor = Color.White
                )
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
            composable(Screen.Classes.route) {
                HomeScreen()
            }
            composable(Screen.CampusMap.route) {
                CampusMapScreen()
            }
            composable(Screen.Esents.route) {
                EsentsScreen()
            }
            composable(Screen.News.route) {
                NewsScreen()
            }
        }
    }
}

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
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Today's Classes",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = Color.Black
            )
            Text(
                text = "History - 9:00 AM – 10:30 AM",
                color = Color(0xFF555555),
                modifier = Modifier.padding(top = 8.dp)
            )
            Text(
                text = "Biology - 11:00 AM – 12:00 PM",
                color = Color(0xFF555555),
                modifier = Modifier.padding(top = 4.dp)
            )
        }
    }
}

@Composable
fun AnnouncementsCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Announcements",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = Color.Black
            )
            Text(
                text = "Announcement: Lorem ipsum dolor sit amet",
                color = Color(0xFF555555),
                modifier = Modifier.padding(top = 8.dp)
            )
            Text(
                text = "Announcement: Lorem ipsum dolor sit amet",
                color = Color(0xFF555555),
                modifier = Modifier.padding(top = 4.dp)
            )
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
                        popUpTo(navController.graph.startDestinationId) {
                            saveState = true
                        }
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
