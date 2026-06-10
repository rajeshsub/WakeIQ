@file:Suppress("MatchingDeclarationName")

package com.wakeiq.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.wakeiq.presentation.edit.EditAlarmScreen
import com.wakeiq.presentation.home.HomeScreen
import com.wakeiq.presentation.settings.SettingsScreen

sealed class Destination(val route: String) {
    data object Home : Destination("home")
    data object EditAlarm : Destination("edit_alarm?id={id}") {
        fun routeFor(id: Long? = null) = if (id != null) "edit_alarm?id=$id" else "edit_alarm"
    }
    data object Settings : Destination("settings")
}

@Composable
fun AlarmNavGraph() {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = Destination.Home.route) {
        composable(Destination.Home.route) {
            HomeScreen(
                onAddAlarm = { navController.navigate(Destination.EditAlarm.routeFor()) },
                onEditAlarm = { id -> navController.navigate(Destination.EditAlarm.routeFor(id)) },
                onOpenSettings = { navController.navigate(Destination.Settings.route) },
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
            SettingsScreen(onBack = { navController.popBackStack() })
        }
    }
}
