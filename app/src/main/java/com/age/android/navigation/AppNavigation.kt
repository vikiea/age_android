/*
 * Copyright (c) 2026 vikiea <vikiea@users.noreply.github.com>
 * This code is released under the MIT License.
 * See LICENSE for details.
 */
package com.age.android.navigation

import android.net.Uri
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.age.android.feature.decrypt.DecryptScreen
import com.age.android.feature.encrypt.EncryptScreen
import com.age.android.feature.history.HistoryDetailScreen
import com.age.android.feature.history.HistoryScreen
import com.age.android.feature.keys.KeyDetailScreen
import com.age.android.feature.keys.KeysScreen
import com.age.android.feature.settings.SettingsScreen

enum class TopLevelRoute(val route: String, val label: String, val icon: ImageVector) {
    ENCRYPT("encrypt", "加密", Icons.Default.Lock),
    DECRYPT("decrypt", "解密", Icons.Default.LockOpen),
    KEYS("keys", "密钥", Icons.Default.Key),
    HISTORY("history", "历史", Icons.Default.History)
}

@Composable
fun AppNavigation(
    sharedUris: List<Uri>? = null,
    onSharedUrisConsumed: () -> Unit = {}
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

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                NavigationBar {
                    TopLevelRoute.entries.forEach { route ->
                        NavigationBarItem(
                            icon = { Icon(route.icon, contentDescription = route.label) },
                            label = { Text(route.label) },
                            selected = currentRoute == route.route,
                            onClick = {
                                navController.navigate(route.route) {
                                    popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = TopLevelRoute.ENCRYPT.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(TopLevelRoute.ENCRYPT.route) {
                EncryptScreen(
                    onNavigateToSettings = { navController.navigate("settings") },
                    sharedUris = if (currentRoute == TopLevelRoute.ENCRYPT.route) sharedUris else null,
                    onSharedUrisConsumed = onSharedUrisConsumed
                )
            }
            composable(TopLevelRoute.DECRYPT.route) {
                DecryptScreen(
                    onNavigateToSettings = { navController.navigate("settings") },
                    sharedUris = if (currentRoute == TopLevelRoute.DECRYPT.route) sharedUris else null,
                    onSharedUrisConsumed = onSharedUrisConsumed
                )
            }
            composable(TopLevelRoute.KEYS.route) {
                KeysScreen(onKeyClick = { keyId -> navController.navigate("key_detail/$keyId") })
            }
            composable(TopLevelRoute.HISTORY.route) {
                HistoryScreen(onOperationClick = { recordId -> navController.navigate("history_detail/$recordId") })
            }
            composable(
                "key_detail/{keyId}",
                arguments = listOf(navArgument("keyId") { type = NavType.LongType })
            ) { backStackEntry ->
                val keyId = backStackEntry.arguments?.getLong("keyId") ?: 0L
                KeyDetailScreen(keyId = keyId, onBack = { navController.popBackStack() })
            }
            composable(
                "history_detail/{recordId}",
                arguments = listOf(navArgument("recordId") { type = NavType.LongType })
            ) { backStackEntry ->
                val recordId = backStackEntry.arguments?.getLong("recordId") ?: 0L
                HistoryDetailScreen(recordId = recordId, onBack = { navController.popBackStack() })
            }
            composable("settings") {
                SettingsScreen(onBack = { navController.popBackStack() })
            }
        }
    }
}
