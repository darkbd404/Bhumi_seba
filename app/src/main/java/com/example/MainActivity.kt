package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.LocalOnBackPressedDispatcherOwner
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.outlined.Build
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Language
import androidx.compose.material.icons.outlined.Map
import androidx.compose.material.icons.outlined.PictureAsPdf
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.example.data.AppPreferences
import com.example.firebase.configureAppCheck
import com.example.model.AboutRoute
import com.example.model.AiAssistantRoute
import com.example.model.HomeRoute
import com.example.model.MouzaPdfRoute
import com.example.model.RealMapRoute
import com.example.model.SavedProjectType
import com.example.model.SavedSurveyProject
import com.example.model.ServicesRoute
import com.example.model.ToolsRoute
import com.example.model.WebBrowserRoute
import com.example.ui.components.ExitConfirmationDialog
import com.example.ui.screens.AboutScreen
import com.example.ui.screens.AiAssistantScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.InAppBrowserScreen
import com.example.ui.screens.MouzaPdfScreen
import com.example.ui.screens.RealMapScreen
import com.example.ui.screens.ServicesScreen
import com.example.ui.screens.ToolsScreen
import com.example.ui.theme.BhumiSurveyTheme
import com.example.ui.theme.LocalNeuPalette
import kotlinx.coroutines.launch

private data class TopLevelDest(
    val route: Any,
    val labelRes: Int,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val testTag: String
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // STEP 1: Mandatory App Check configuration with launch intent before UI/ViewModels
        configureAppCheck(this, intent)

        enableEdgeToEdge()

        setContent {
            val prefs = remember { AppPreferences(applicationContext) }
            val scope = rememberCoroutineScope()
            val storedDark by prefs.isDarkModeFlow.collectAsStateWithLifecycle(initialValue = null)
            val systemDark = isSystemInDarkTheme()
            val isDarkMode = storedDark ?: systemDark

            val savedRecords by prefs.savedRecordsFlow.collectAsStateWithLifecycle(initialValue = emptyList())
            val savedProjects by prefs.savedProjectsFlow.collectAsStateWithLifecycle(initialValue = emptyList())
            val devPhotoUri by prefs.devPhotoUriFlow.collectAsStateWithLifecycle(initialValue = null)

            BhumiSurveyTheme(darkTheme = isDarkMode) {
                BhumiSurveyApp(
                    isDarkMode = isDarkMode,
                    savedRecords = savedRecords,
                    savedProjects = savedProjects,
                    devPhotoUri = devPhotoUri,
                    onToggleTheme = {
                        scope.launch { prefs.setDarkMode(!isDarkMode) }
                    },
                    onSaveDevPhoto = { uriStr ->
                        scope.launch { prefs.setDevPhotoUri(uriStr) }
                    },
                    onDeleteProject = { projId ->
                        scope.launch { prefs.deleteSurveyProject(projId) }
                    },
                    onClearHistory = {
                        scope.launch { prefs.clearHistory() }
                    },
                    onExitApp = { finish() }
                )
            }
        }
    }
}

@Composable
private fun BhumiSurveyApp(
    isDarkMode: Boolean,
    savedRecords: List<com.example.model.SavedMeasurementRecord>,
    savedProjects: List<SavedSurveyProject>,
    devPhotoUri: String?,
    onToggleTheme: () -> Unit,
    onSaveDevPhoto: (String) -> Unit,
    onDeleteProject: (String) -> Unit,
    onClearHistory: () -> Unit,
    onExitApp: () -> Unit
) {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentDest = backStackEntry?.destination
    var showExitDialog by remember { mutableStateOf(false) }

    val topDestinations = remember {
        listOf(
            TopLevelDest(HomeRoute, R.string.nav_home, Icons.Filled.Home, Icons.Outlined.Home, "nav_home"),
            TopLevelDest(MouzaPdfRoute(), R.string.nav_mouza, Icons.Filled.PictureAsPdf, Icons.Outlined.PictureAsPdf, "nav_mouza"),
            TopLevelDest(RealMapRoute(), R.string.nav_map, Icons.Filled.Map, Icons.Outlined.Map, "nav_map"),
            TopLevelDest(ServicesRoute, R.string.nav_services, Icons.Filled.Language, Icons.Outlined.Language, "nav_services"),
            TopLevelDest(ToolsRoute(), R.string.nav_tools, Icons.Filled.Build, Icons.Outlined.Build, "nav_tools")
        )
    }

    val isInBrowser = currentDest?.hasRoute<WebBrowserRoute>() == true
    val isAtHomeRoot = currentDest?.hasRoute<HomeRoute>() == true

    // Exit confirmation dialog ONLY triggers when user tries to leave the app from Home root!
    // Sub-screens pop cleanly back to Home without triggering Exit dialog.
    BackHandler(enabled = isAtHomeRoot) {
        showExitDialog = true
    }

    val backDispatcher = LocalOnBackPressedDispatcherOwner.current?.onBackPressedDispatcher
    val density = LocalDensity.current
    var edgeSwipeProgressPx by remember { mutableFloatStateOf(0f) }

    fun navigateTopLevel(route: Any) {
        navController.navigate(route) {
            popUpTo(navController.graph.findStartDestination().id) {
                saveState = true
            }
            launchSingleTop = true
            restoreState = true
        }
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val useNavRail = maxWidth >= 600.dp

        Scaffold(
            containerColor = LocalNeuPalette.current.canvas,
            contentWindowInsets = WindowInsets.safeDrawing,
            bottomBar = {
                if (!useNavRail && !isInBrowser) {
                    NavigationBar(
                        containerColor = LocalNeuPalette.current.surface
                    ) {
                        topDestinations.forEach { dest ->
                            val selected = when (dest.route) {
                                is HomeRoute -> currentDest?.hasRoute<HomeRoute>() == true
                                is MouzaPdfRoute -> currentDest?.hasRoute<MouzaPdfRoute>() == true
                                is RealMapRoute -> currentDest?.hasRoute<RealMapRoute>() == true
                                is ServicesRoute -> currentDest?.hasRoute<ServicesRoute>() == true
                                is ToolsRoute -> currentDest?.hasRoute<ToolsRoute>() == true
                                else -> false
                            }
                            NavigationBarItem(
                                selected = selected,
                                onClick = { navigateTopLevel(dest.route) },
                                icon = {
                                    Icon(
                                        imageVector = if (selected) dest.selectedIcon else dest.unselectedIcon,
                                        contentDescription = stringResource(dest.labelRes)
                                    )
                                },
                                label = { Text(stringResource(dest.labelRes), maxLines = 1) },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = MaterialTheme.colorScheme.primary,
                                    selectedTextColor = MaterialTheme.colorScheme.primary,
                                    indicatorColor = MaterialTheme.colorScheme.primaryContainer
                                ),
                                modifier = Modifier.testTag(dest.testTag)
                            )
                        }
                    }
                }
            }
        ) { innerPadding ->
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                if (useNavRail && !isInBrowser) {
                    NavigationRail(
                        containerColor = LocalNeuPalette.current.surface
                    ) {
                        topDestinations.forEach { dest ->
                            val selected = when (dest.route) {
                                is HomeRoute -> currentDest?.hasRoute<HomeRoute>() == true
                                is MouzaPdfRoute -> currentDest?.hasRoute<MouzaPdfRoute>() == true
                                is RealMapRoute -> currentDest?.hasRoute<RealMapRoute>() == true
                                is ServicesRoute -> currentDest?.hasRoute<ServicesRoute>() == true
                                is ToolsRoute -> currentDest?.hasRoute<ToolsRoute>() == true
                                else -> false
                            }
                            NavigationRailItem(
                                selected = selected,
                                onClick = { navigateTopLevel(dest.route) },
                                icon = {
                                    Icon(
                                        imageVector = if (selected) dest.selectedIcon else dest.unselectedIcon,
                                        contentDescription = stringResource(dest.labelRes)
                                    )
                                },
                                label = { Text(stringResource(dest.labelRes), maxLines = 1) },
                                modifier = Modifier.testTag(dest.testTag)
                            )
                        }
                    }
                }

                Box(modifier = Modifier.weight(1f).fillMaxSize()) {
                    NavHost(
                        navController = navController,
                        startDestination = HomeRoute,
                        enterTransition = {
                            slideIntoContainer(
                                AnimatedContentTransitionScope.SlideDirection.Left,
                                animationSpec = tween(260, easing = FastOutSlowInEasing)
                            ) + fadeIn(tween(220))
                        },
                        exitTransition = {
                            slideOutOfContainer(
                                AnimatedContentTransitionScope.SlideDirection.Left,
                                animationSpec = tween(260, easing = FastOutSlowInEasing)
                            ) + fadeOut(tween(180))
                        },
                        popEnterTransition = {
                            slideIntoContainer(
                                AnimatedContentTransitionScope.SlideDirection.Right,
                                animationSpec = tween(260, easing = FastOutSlowInEasing)
                            ) + fadeIn(tween(220))
                        },
                        popExitTransition = {
                            slideOutOfContainer(
                                AnimatedContentTransitionScope.SlideDirection.Right,
                                animationSpec = tween(260, easing = FastOutSlowInEasing)
                            ) + fadeOut(tween(180))
                        }
                    ) {
                    composable<HomeRoute> {
                        HomeScreen(
                            isDarkMode = isDarkMode,
                            savedRecords = savedRecords,
                            savedProjects = savedProjects,
                            onToggleTheme = onToggleTheme,
                            onOpenMouzaPdf = { navigateTopLevel(MouzaPdfRoute()) },
                            onOpenRealMap = { navigateTopLevel(RealMapRoute()) },
                            onOpenServices = { navigateTopLevel(ServicesRoute) },
                            onOpenTools = { tab -> navController.navigate(ToolsRoute(initialTab = tab)) },
                            onOpenAiAssistant = { navController.navigate(AiAssistantRoute) },
                            onOpenAbout = { navController.navigate(AboutRoute) },
                            onOpenUrlInApp = { url, title ->
                                navController.navigate(WebBrowserRoute(url = url, title = title))
                            },
                            onRestoreProject = { proj ->
                                val target = when (proj.type) {
                                    SavedProjectType.MOUZA_PDF -> MouzaPdfRoute(savedProjectId = proj.id)
                                    SavedProjectType.REAL_MAP -> RealMapRoute(savedProjectId = proj.id)
                                }
                                navController.navigate(target) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = false
                                    }
                                    launchSingleTop = true
                                }
                            },
                            onDeleteProject = onDeleteProject,
                            onClearHistory = onClearHistory
                        )
                    }

                    composable<MouzaPdfRoute> { entry ->
                        val args = entry.toRoute<MouzaPdfRoute>()
                        MouzaPdfScreen(
                            savedProjectId = args.savedProjectId,
                            onNavigateBack = { navController.popBackStack() }
                        )
                    }

                    composable<RealMapRoute> { entry ->
                        val args = entry.toRoute<RealMapRoute>()
                        RealMapScreen(
                            savedProjectId = args.savedProjectId,
                            onNavigateBack = { navController.popBackStack() }
                        )
                    }

                    composable<ServicesRoute> {
                        ServicesScreen(
                            onNavigateBack = { navController.popBackStack() },
                            onOpenUrlInApp = { url, title ->
                                navController.navigate(WebBrowserRoute(url = url, title = title))
                            }
                        )
                    }

                    composable<ToolsRoute> { entry ->
                        val args = entry.toRoute<ToolsRoute>()
                        ToolsScreen(
                            initialTabKey = args.initialTab,
                            onNavigateBack = { navController.popBackStack() }
                        )
                    }

                    composable<AiAssistantRoute> {
                        AiAssistantScreen(
                            onNavigateBack = { navController.popBackStack() },
                            onOpenUrlInApp = { url, title ->
                                navController.navigate(WebBrowserRoute(url = url, title = title))
                            }
                        )
                    }

                    composable<WebBrowserRoute> { entry ->
                        val args = entry.toRoute<WebBrowserRoute>()
                        InAppBrowserScreen(
                            initialUrl = args.url,
                            initialTitle = args.title,
                            onNavigateBack = { navController.popBackStack() }
                        )
                    }

                    composable<AboutRoute> {
                        AboutScreen(
                            savedPhotoUri = devPhotoUri,
                            onPhotoPicked = onSaveDevPhoto,
                            onNavigateBack = { navController.popBackStack() },
                            onOpenUrlInApp = { url, title ->
                                navController.navigate(WebBrowserRoute(url = url, title = title))
                            }
                        )
                    }
                }

                // Universal Left-Edge Swipe-Back Gesture for Phone, Tablet & PC mouse drag
                if (!isAtHomeRoot) {
                    val thresholdPx = with(density) { 56.dp.toPx() }
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .width(26.dp)
                            .align(Alignment.CenterStart)
                            .pointerInput(currentDest?.route) {
                                detectHorizontalDragGestures(
                                    onDragStart = { edgeSwipeProgressPx = 0f },
                                    onHorizontalDrag = { _, dragAmount ->
                                        edgeSwipeProgressPx = (edgeSwipeProgressPx + dragAmount).coerceIn(0f, thresholdPx * 1.6f)
                                    },
                                    onDragEnd = {
                                        if (edgeSwipeProgressPx >= thresholdPx) {
                                            backDispatcher?.onBackPressed()
                                        }
                                        edgeSwipeProgressPx = 0f
                                    },
                                    onDragCancel = { edgeSwipeProgressPx = 0f }
                                )
                            }
                    )

                    androidx.compose.animation.AnimatedVisibility(
                        visible = edgeSwipeProgressPx > 8f,
                        enter = fadeIn(tween(100)) + scaleIn(tween(120)),
                        exit = fadeOut(tween(120)) + scaleOut(tween(120)),
                        modifier = Modifier
                            .align(Alignment.CenterStart)
                            .offset { IntOffset((edgeSwipeProgressPx * 0.45f).roundToInt(), 0) }
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.92f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                                contentDescription = "Swipe Back",
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }
        }
    }
    }

    if (showExitDialog) {
        ExitConfirmationDialog(
            onDismiss = { showExitDialog = false },
            onConfirmExit = {
                showExitDialog = false
                onExitApp()
            }
        )
    }
}
