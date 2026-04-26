package com.lango.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.compose.*
import com.lango.app.ui.screens.*
import com.lango.app.ui.theme.*
import com.lango.app.viewmodel.*

class MainActivity : ComponentActivity() {

    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            if (checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) !=
                android.content.pm.PackageManager.PERMISSION_GRANTED) {
                requestPermissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
            }
        }
        setContent { LangoApp() }
    }
}

@Composable
fun LangoApp() {
    LangoTheme {
        val vmFactory = ViewModelProvider.AndroidViewModelFactory.getInstance(
            androidx.compose.ui.platform.LocalContext.current.applicationContext as android.app.Application
        )
        val authViewModel: AuthViewModel = viewModel(factory = vmFactory)
        val authState by authViewModel.state.collectAsState()

        AnimatedContent(
            targetState = authState is AuthState.Success,
            transitionSpec = { fadeIn(tween(300)) togetherWith fadeOut(tween(300)) },
            label = "auth"
        ) { isLoggedIn ->
            if (isLoggedIn) {
                MainNav(authViewModel = authViewModel)
            } else {
                AuthScreen(viewModel = authViewModel)
            }
        }
    }
}

@Composable
fun MainNav(authViewModel: AuthViewModel) {
    val vmFactory = ViewModelProvider.AndroidViewModelFactory.getInstance(
        androidx.compose.ui.platform.LocalContext.current.applicationContext as android.app.Application
    )
    val decksViewModel: DecksViewModel = viewModel(factory = vmFactory)
    val deckDetailViewModel: DeckDetailViewModel = viewModel(factory = vmFactory)
    val trainingViewModel: TrainingViewModel = viewModel(factory = vmFactory)
    val catalogViewModel: CatalogViewModel = viewModel(factory = vmFactory)

    val authState by remember { derivedStateOf { authViewModel.currentUser?.uid } }

    LaunchedEffect(authState) {
        if (authState != null) decksViewModel.syncFromCloud()
    }

    val navController = rememberNavController()
    val currentBackStack by navController.currentBackStackEntryAsState()
    val currentRoute = currentBackStack?.destination?.route

    val showBottomBar = currentRoute in listOf("decks", "catalog", "profile")

    Scaffold(
        containerColor = DarkBg,
        bottomBar = {
            if (showBottomBar) {
                LangoBottomBar(navController = navController, currentRoute = currentRoute)
            }
        }
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = "decks",
            modifier = Modifier.padding(padding)
        ) {
            composable("decks",
                enterTransition = { fadeIn(tween(180)) },
                exitTransition = { fadeOut(tween(180)) }
            ) {
                DecksScreen(
                    viewModel = decksViewModel,
                    onDeckClick = { deckId ->
                        deckDetailViewModel.setDeckId(deckId)
                        navController.navigate("deck/$deckId")
                    }
                )
            }

            composable("deck/{deckId}",
                enterTransition = { slideInHorizontally { it } + fadeIn() },
                exitTransition = { slideOutHorizontally { it } + fadeOut() }
            ) { backStack ->
                val deckId = backStack.arguments?.getString("deckId")?.toLongOrNull() ?: return@composable
                DeckDetailScreen(
                    deckId = deckId,
                    viewModel = deckDetailViewModel,
                    onBack = { navController.popBackStack() },
                    onStartTraining = { navController.navigate("training/$deckId") }
                )
            }

            composable("training/{deckId}",
                enterTransition = { slideInVertically { it } + fadeIn() },
                exitTransition = { slideOutVertically { it } + fadeOut() }
            ) { backStack ->
                val deckId = backStack.arguments?.getString("deckId")?.toLongOrNull() ?: return@composable
                TrainingScreen(
                    deckId = deckId,
                    viewModel = trainingViewModel,
                    onBack = { navController.popBackStack() }
                )
            }

            composable("catalog",
                enterTransition = { fadeIn(tween(180)) },
                exitTransition = { fadeOut(tween(180)) }
            ) {
                CatalogScreen(viewModel = catalogViewModel)
            }

            composable("profile",
                enterTransition = { fadeIn(tween(180)) },
                exitTransition = { fadeOut(tween(180)) }
            ) {
                ProfileScreen(authViewModel = authViewModel, decksViewModel = decksViewModel)
            }
        }
    }
}

data class BottomNavItem(val route: String, val icon: ImageVector, val label: String)

@Composable
fun LangoBottomBar(navController: NavController, currentRoute: String?) {
    val items = listOf(
        BottomNavItem("decks", Icons.Filled.LibraryBooks, "Колоды"),
        BottomNavItem("catalog", Icons.Filled.Public, "Каталог"),
        BottomNavItem("profile", Icons.Filled.Person, "Профиль")
    )

    NavigationBar(
        containerColor = DarkSurface,
        tonalElevation = 0.dp
    ) {
        items.forEach { item ->
            val selected = currentRoute == item.route
            NavigationBarItem(
                selected = selected,
                onClick = {
                    if (currentRoute != item.route) {
                        navController.navigate(item.route) {
                            popUpTo("decks") { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                },
                icon = { Icon(item.icon, item.label) },
                label = { Text(item.label, fontSize = 10.sp, fontWeight = FontWeight.Medium) },
                alwaysShowLabel = selected,
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = Primary,
                    selectedTextColor = Primary,
                    unselectedIconColor = TextSecondary,
                    unselectedTextColor = TextSecondary,
                    indicatorColor = Primary.copy(0.12f)
                )
            )
        }
    }
}
