package com.track.infinitarlockin.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.track.infinitarlockin.data.remote.dto.Employee
import com.track.infinitarlockin.ui.screens.AttendanceHistoryScreen
import com.track.infinitarlockin.ui.screens.CameraScreen
import com.track.infinitarlockin.ui.screens.HomeScreen
import com.track.infinitarlockin.ui.screens.RegisterScreen
import com.track.infinitarlockin.ui.screens.SplashScreen
import com.track.infinitarlockin.ui.viewmodels.MainViewModel

sealed class Screen(val route: String) {
    object Splash : Screen("splash")
    object Register : Screen("register")
    object Home : Screen("home")
    object Camera : Screen("camera")
    object AttendanceHistory : Screen("history")
}

@Composable
fun AppNavHost(navController: NavHostController, mainViewModel: MainViewModel) {
    NavHost(navController = navController, startDestination = Screen.Splash.route) {
        composable(Screen.Splash.route) {
            SplashScreen(navController = navController, mainViewModel = mainViewModel)
        }
        composable(Screen.Register.route) {
            RegisterScreen(navController = navController, mainViewModel = mainViewModel)
        }
        composable(Screen.Home.route) {
            HomeScreen(navController = navController, mainViewModel = mainViewModel)
        }
        composable(Screen.Camera.route) {
            CameraScreen(navController = navController, mainViewModel = mainViewModel)
        }
        composable(Screen.AttendanceHistory.route) {
            val employee = navController.previousBackStackEntry?.savedStateHandle?.get<Employee>("employee")
            if (employee != null) {
                AttendanceHistoryScreen(navController = navController, employee = employee)
            }
        }
    }
}
