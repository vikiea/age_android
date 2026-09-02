/*
 * Copyright (c) 2026 vikiea <vikiea@users.noreply.github.com>
 * This code is released under the MIT License.
 * See LICENSE for details.
 */
package io.github.vikiea.age.navigation

import android.net.Uri
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.annotation.StringRes
import androidx.compose.ui.res.stringResource
import io.github.vikiea.age.R
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffold
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import io.github.vikiea.age.feature.decrypt.DecryptScreen
import io.github.vikiea.age.feature.encrypt.EncryptScreen
import io.github.vikiea.age.feature.history.HistoryDetailScreen
import io.github.vikiea.age.feature.history.HistoryScreen
import io.github.vikiea.age.feature.keys.KeyDetailScreen
import io.github.vikiea.age.feature.keys.KeysScreen
import io.github.vikiea.age.feature.settings.SettingsScreen
import io.github.vikiea.age.ui.glass.GlassBackdropHost

enum class TopLevelRoute(val route: String, @StringRes val labelRes: Int, val icon: ImageVector) {
    ENCRYPT("encrypt", R.string.nav_encrypt, Icons.Default.Lock),
    DECRYPT("decrypt", R.string.nav_decrypt, Icons.Default.LockOpen),
    KEYS("keys", R.string.nav_keys, Icons.Default.Key),
    HISTORY("history", R.string.nav_history, Icons.Default.History)
}

@Composable
fun AppNavigation(
    sharedUris: List<Uri>? = null,
    onSharedUrisConsumed: () -> Unit = {},
    glassEffectEnabled: Boolean = true
) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val showBottomBar = currentRoute in TopLevelRoute.entries.map { it.route }

    // Route shared URIs to encrypt or decrypt screen based on file extension
    LaunchedEffect(sharedUris) {
        val uris = sharedUris ?: return@LaunchedEffect
        if (uris.isEmpty()) return@LaunchedEffect
        val isDecrypt = uris.any { uri ->
            val path = uri.lastPathSegment ?: ""
            path.endsWith(".age", ignoreCase = true)
        }
        val route = if (isDecrypt) TopLevelRoute.DECRYPT.route else TopLevelRoute.ENCRYPT.route
        navController.navigate(route) {
            popUpTo(navController.graph.findStartDestination().id) { saveState = false }
            launchSingleTop = true
        }
    }

    GlassBackdropHost(glassEffectEnabled = glassEffectEnabled) { backdrop ->
        val content = @Composable {
            NavHost(
                navController = navController,
                startDestination = TopLevelRoute.ENCRYPT.route,
                modifier = Modifier.fillMaxSize()
            ) {
                composable(TopLevelRoute.ENCRYPT.route) {
                    EncryptScreen(
                        onNavigateToSettings = { navController.navigate("settings") },
                        sharedUris = if (currentRoute == TopLevelRoute.ENCRYPT.route) sharedUris else null,
                        onSharedUrisConsumed = onSharedUrisConsumed,
                        backdrop = backdrop
                    )
                }
                composable(TopLevelRoute.DECRYPT.route) {
                    DecryptScreen(
                        onNavigateToSettings = { navController.navigate("settings") },
                        sharedUris = if (currentRoute == TopLevelRoute.DECRYPT.route) sharedUris else null,
                        onSharedUrisConsumed = onSharedUrisConsumed,
                        backdrop = backdrop
                    )
                }
                composable(TopLevelRoute.KEYS.route) {
                    KeysScreen(
                        onKeyClick = { keyId -> navController.navigate("key_detail/$keyId") },
                        backdrop = backdrop
                    )
                }
                composable(TopLevelRoute.HISTORY.route) {
                    HistoryScreen(
                        onOperationClick = { recordId -> navController.navigate("history_detail/$recordId") },
                        backdrop = backdrop
                    )
                }
                composable(
                    "key_detail/{keyId}",
                    arguments = listOf(navArgument("keyId") { type = NavType.LongType })
                ) { backStackEntry ->
                    val keyId = backStackEntry.arguments?.getLong("keyId") ?: 0L
                    KeyDetailScreen(
                        keyId = keyId,
                        onBack = { navController.popBackStack() },
                        backdrop = backdrop
                    )
                }
                composable(
                    "history_detail/{recordId}",
                    arguments = listOf(navArgument("recordId") { type = NavType.LongType })
                ) { backStackEntry ->
                    val recordId = backStackEntry.arguments?.getLong("recordId") ?: 0L
                    HistoryDetailScreen(
                        recordId = recordId,
                        onBack = { navController.popBackStack() },
                        backdrop = backdrop
                    )
                }
                composable("settings") {
                    SettingsScreen(
                        onBack = { navController.popBackStack() },
                        backdrop = backdrop
                    )
                }
            }
        }
        if (showBottomBar) {
            val routeLabels = TopLevelRoute.entries.associateWith { stringResource(it.labelRes) }
            NavigationSuiteScaffold(
                navigationSuiteItems = {
                    TopLevelRoute.entries.forEach { route ->
                        val label = routeLabels.getValue(route)
                        item(
                            selected = currentRoute == route.route,
                            onClick = {
                                navController.navigate(route.route) {
                                    popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = { androidx.compose.material3.Icon(route.icon, contentDescription = label) },
                            label = { androidx.compose.material3.Text(label) }
                        )
                    }
                }
            ) {
                content()
            }
        } else {
            content()
        }
    }
}
