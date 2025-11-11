package com.track.infinitarlockin.ui.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.track.infinitarlockin.ui.navigation.Screen
import com.track.infinitarlockin.ui.viewmodels.AuthState
import com.track.infinitarlockin.ui.viewmodels.MainViewModel

@Composable
fun SplashScreen(navController: NavController, mainViewModel: MainViewModel = viewModel()) {
    val context = LocalContext.current
    val authState by mainViewModel.authState.collectAsState()

    LaunchedEffect(Unit) {
        mainViewModel.checkDeviceRegistration(context)
    }

    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        when (val state = authState) {
            is AuthState.Loading -> CircularProgressIndicator()
            is AuthState.Authenticated -> {
                LaunchedEffect(Unit) {
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.Splash.route) { inclusive = true }
                    }
                }
            }
            is AuthState.Unauthenticated -> {
                LaunchedEffect(Unit) {
                    navController.navigate(Screen.Register.route) {
                        popUpTo(Screen.Splash.route) { inclusive = true }
                    }
                }
            }
            is AuthState.Error -> Text(text = state.message)
        }
    }
}
