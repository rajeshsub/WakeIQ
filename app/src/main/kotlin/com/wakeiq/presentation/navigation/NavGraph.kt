@file:Suppress("MatchingDeclarationName")

package com.wakeiq.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.wakeiq.presentation.edit.EditAlarmScreen
import com.wakeiq.presentation.home.HomeScreen
import com.wakeiq.presentation.permissions.PermissionsScreen
import com.wakeiq.presentation.permissions.areCriticalPermissionsGranted
import com.wakeiq.presentation.settings.PrivacyPolicyScreen
import com.wakeiq.presentation.settings.SettingsScreen

sealed class Destination(val route: String) {
    data object Permissions : Destination("permissions")
    data object Home : Destination("home")
    data object EditAlarm : Destination("edit_alarm?id={id}") {
        fun routeFor(id: Long? = null) = if (id != null) "edit_alarm?id=$id" else "edit_alarm"
    }
    data object Settings : Destination("settings")
    data object PrivacyPolicy : Destination("privacy_policy")
}

@Composable
fun AlarmNavGraph() {
    val context = LocalContext.current
    val startDestination = remember {
        if (areCriticalPermissionsGranted(context)) {
            Destination.Home.route
        } else {
            Destination.Permissions.route
        }
    }
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = startDestination) {
        composable(Destination.Permissions.route) {
            PermissionsScreen(
                onDone = {
                    if (navController.previousBackStackEntry == null) {
                        navController.navigate(Destination.Home.route) {
                            popUpTo(Destination.Permissions.route) { inclusive = true }
                        }
                    } else {
                        navController.popBackStack()
                    }
                },
            )
        }

        composable(Destination.Home.route) {
            HomeScreen(
                onAddAlarm = { navController.navigate(Destination.EditAlarm.routeFor()) },
                onEditAlarm = { id -> navController.navigate(Destination.EditAlarm.routeFor(id)) },
                onOpenSettings = { navController.navigate(Destination.Settings.route) },
                onOpenPermissions = { navController.navigate(Destination.Permissions.route) },
            )
        }

        composable(
            route = Destination.EditAlarm.route,
            arguments = listOf(
                navArgument("id") {
                    type = NavType.LongType
                    defaultValue = -1L
                },
            ),
        ) {
            EditAlarmScreen(
                onSaved = { navController.popBackStack() },
                onDeleted = { navController.popBackStack() },
                onBack = { navController.popBackStack() },
            )
        }

        composable(Destination.Settings.route) {
            SettingsScreen(
                onBack = { navController.popBackStack() },
                onOpenPrivacyPolicy = { navController.navigate(Destination.PrivacyPolicy.route) },
            )
        }

        composable(Destination.PrivacyPolicy.route) {
            PrivacyPolicyScreen(onBack = { navController.popBackStack() })
        }
    }
}
