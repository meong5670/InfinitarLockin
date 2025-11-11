package com.track.infinitarlockin

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.compose.rememberNavController
import com.track.infinitarlockin.ui.navigation.AppNavHost
import com.track.infinitarlockin.ui.navigation.Screen
import com.track.infinitarlockin.ui.theme.InfinitarLockinTheme
import com.track.infinitarlockin.ui.viewmodels.AuthState
import com.track.infinitarlockin.ui.viewmodels.MainViewModel

class MainActivity : ComponentActivity() {

    private val mainViewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            InfinitarLockinTheme {
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    val navController = rememberNavController()
                    val authState by mainViewModel.authState.collectAsState()

                    LaunchedEffect(authState) {
                        when (authState) {
                            is AuthState.Authenticated -> {
                                navController.navigate(Screen.Home.route) {
                                    launchSingleTop = true // Prevents creating a new instance if already on Home
                                    popUpTo(navController.graph.startDestinationId) { inclusive = true }
                                }
                            }
                            is AuthState.Unauthenticated -> {
                                navController.navigate(Screen.Register.route) {
                                    launchSingleTop = true // Prevents creating a new instance if already on Register
                                    popUpTo(navController.graph.startDestinationId) { inclusive = true }
                                }
                            }
                            else -> Unit
                        }
                    }

                    AppNavHost(navController = navController, mainViewModel = mainViewModel)
                }
            }
        }
    }
}
