package com.track.infinitarlockin.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.track.infinitarlockin.ui.navigation.Screen
import com.track.infinitarlockin.ui.viewmodels.AuthState
import com.track.infinitarlockin.ui.viewmodels.MainViewModel

@Composable
fun SplashScreen(navController: NavController, mainViewModel: MainViewModel = viewModel()) {
    val context = LocalContext.current
    val authState by mainViewModel.authState.collectAsState()

    // This LaunchedEffect will run once when the SplashScreen is first displayed.
    LaunchedEffect(Unit) {
        mainViewModel.checkDeviceRegistration(context)
    }

    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        when (val state = authState) {
            is AuthState.Loading -> CircularProgressIndicator()
            
            // On successful authentication or unauthentication, the MainActivity's
            // LaunchedEffect will handle the navigation, so we don't need to do anything here.
            is AuthState.Authenticated, is AuthState.Unauthenticated -> {
                 // You can keep the progress indicator here for a smoother transition
                 // as the navigation takes a moment to trigger.
                 CircularProgressIndicator()
            }

            is AuthState.Error -> {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(text = state.message, color = Color.Red)
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(onClick = { 
                        // On retry, explicitly tell the view model this is a user-initiated retry
                        mainViewModel.checkDeviceRegistration(context, isRetry = true) 
                    }) {
                        Text("Retry")
                    }
                }
            }
        }
    }
}
