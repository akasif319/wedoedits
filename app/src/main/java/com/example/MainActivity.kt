package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.engine.VideoEditorViewModel
import com.example.ui.screens.*
import com.example.ui.theme.*

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()
    setContent {
      MyApplicationTheme(darkTheme = true) {
        MainAppLayout()
      }
    }
  }
}

@Composable
fun MainAppLayout() {
  val navController = rememberNavController()
  val viewModel: VideoEditorViewModel = viewModel()
  val navBackStackEntry by navController.currentBackStackEntryAsState()
  val currentRoute = navBackStackEntry?.destination?.route

  // Determine whether to show the bottom navigation bar (hide in editor mode)
  val showBottomBar = currentRoute != "editor"

  Scaffold(
    modifier = Modifier.fillMaxSize(),
    bottomBar = {
      if (showBottomBar) {
        NavigationBar(
          containerColor = GeoBackground,
          contentColor = GeoText
        ) {
          NavigationBarItem(
            icon = { Icon(Icons.Default.Home, contentDescription = "Home") },
            label = { Text("Home", fontSize = 11.sp) },
            selected = currentRoute == "home" || currentRoute == null,
            onClick = {
              navController.navigate("home") {
                popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                launchSingleTop = true
                restoreState = true
              }
            },
            colors = NavigationBarItemDefaults.colors(
              selectedIconColor = GeoPrimary,
              selectedTextColor = GeoPrimary,
              indicatorColor = Color(0xFF1C1F26),
              unselectedIconColor = GeoTextMuted,
              unselectedTextColor = GeoTextMuted
            )
          )

          NavigationBarItem(
            icon = { Icon(Icons.Default.AutoAwesome, contentDescription = "Templates") },
            label = { Text("Templates", fontSize = 11.sp) },
            selected = currentRoute == "templates",
            onClick = {
              navController.navigate("templates") {
                popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                launchSingleTop = true
                restoreState = true
              }
            },
            colors = NavigationBarItemDefaults.colors(
              selectedIconColor = GeoPrimary,
              selectedTextColor = GeoPrimary,
              indicatorColor = Color(0xFF1C1F26),
              unselectedIconColor = GeoTextMuted,
              unselectedTextColor = GeoTextMuted
            )
          )

          NavigationBarItem(
            icon = { Icon(Icons.Default.Movie, contentDescription = "Drafts") },
            label = { Text("Edits", fontSize = 11.sp) },
            selected = currentRoute == "drafts",
            onClick = {
              navController.navigate("drafts") {
                popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                launchSingleTop = true
                restoreState = true
              }
            },
            colors = NavigationBarItemDefaults.colors(
              selectedIconColor = GeoPrimary,
              selectedTextColor = GeoPrimary,
              indicatorColor = Color(0xFF1C1F26),
              unselectedIconColor = GeoTextMuted,
              unselectedTextColor = GeoTextMuted
            )
          )

          NavigationBarItem(
            icon = { Icon(Icons.Default.Person, contentDescription = "Profile") },
            label = { Text("Profile", fontSize = 11.sp) },
            selected = currentRoute == "profile",
            onClick = {
              navController.navigate("profile") {
                popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                launchSingleTop = true
                restoreState = true
              }
            },
            colors = NavigationBarItemDefaults.colors(
              selectedIconColor = GeoPrimary,
              selectedTextColor = GeoPrimary,
              indicatorColor = Color(0xFF1C1F26),
              unselectedIconColor = GeoTextMuted,
              unselectedTextColor = GeoTextMuted
            )
          )
        }
      }
    }
  ) { innerPadding ->
    NavHost(
      navController = navController,
      startDestination = "home",
      modifier = Modifier.padding(innerPadding)
    ) {
      composable("home") {
        HomeScreen(
          viewModel = viewModel,
          onNavigateToEditor = { navController.navigate("editor") },
          onNavigateToDrafts = { navController.navigate("drafts") }
        )
      }
      composable("templates") {
        TemplatesScreen(
          viewModel = viewModel,
          onNavigateToEditor = { navController.navigate("editor") }
        )
      }
      composable("drafts") {
        DraftsScreen(
          viewModel = viewModel,
          onNavigateToEditor = { navController.navigate("editor") }
        )
      }
      composable("profile") {
        ProfileScreen()
      }
      composable("editor") {
        EditorScreen(
          viewModel = viewModel,
          onNavigateBack = { navController.popBackStack() }
        )
      }
    }
  }
}
