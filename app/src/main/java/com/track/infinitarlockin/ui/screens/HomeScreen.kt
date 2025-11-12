package com.track.infinitarlockin.ui.screens

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import com.google.android.gms.location.LocationServices
import com.track.infinitarlockin.data.remote.dto.Employee
import com.track.infinitarlockin.ui.navigation.Screen
import com.track.infinitarlockin.ui.viewmodels.AttendanceViewModel
import com.track.infinitarlockin.ui.viewmodels.AuthState
import com.track.infinitarlockin.ui.viewmodels.MainViewModel
import com.track.infinitarlockin.ui.viewmodels.VerificationState

@Composable
fun HomeScreen(
    navController: NavController,
    mainViewModel: MainViewModel = viewModel(),
    attendanceViewModel: AttendanceViewModel = viewModel()
) {
    val authState by mainViewModel.authState.collectAsState()
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                mainViewModel.checkDeviceRegistration(context)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        when (val state = authState) {
            is AuthState.Authenticated -> {
                HomeScreenContent(
                    navController = navController,
                    employee = state.employee,
                    attendanceViewModel = attendanceViewModel
                )
            }
            is AuthState.Loading -> {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            }
            is AuthState.Error -> {
                Column(
                    modifier = Modifier.align(Alignment.Center),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(state.message, color = Color.Red, textAlign = TextAlign.Center)
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(onClick = { mainViewModel.checkDeviceRegistration(context, isRetry = true) }) {
                        Text("Retry")
                    }
                }
            }
            is AuthState.Unauthenticated -> {
                // This can happen if the user's registration is somehow revoked.
                // The main LaunchedEffect will navigate them back to the register screen.
                Text("Authentication lost. Redirecting...", modifier = Modifier.align(Alignment.Center))
            }
        }
    }
}


@OptIn(ExperimentalPermissionsApi::class)
@Composable
private fun HomeScreenContent(
    navController: NavController,
    employee: Employee,
    attendanceViewModel: AttendanceViewModel
) {
    val context = LocalContext.current
    val verificationState by attendanceViewModel.verificationState.collectAsState()
    val hasClockedIn = employee.hasClockedInToday ?: false

    val locationPermissions = rememberMultiplePermissionsState(
        permissions = listOf(
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.ACCESS_FINE_LOCATION,
        )
    )

    LaunchedEffect(verificationState) {
        if (verificationState is VerificationState.Success) {
            navController.navigate(Screen.Camera.route)
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            attendanceViewModel.resetVerificationState()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text = "Welcome, ${employee.name}", fontSize = 24.sp)
        Spacer(modifier = Modifier.height(32.dp))

        if (hasClockedIn) {
            Text(
                text = "You have already clocked in for today.",
                fontSize = 18.sp,
                textAlign = TextAlign.Center
            )
        } else {
            when (val state = verificationState) {
                is VerificationState.Idle -> {
                    Button(onClick = {
                        if (locationPermissions.allPermissionsGranted) {
                            getCurrentLocation(context) { latitude, longitude ->
                                attendanceViewModel.verifyAttendance(context, latitude, longitude)
                            }
                        } else {
                            locationPermissions.launchMultiplePermissionRequest()
                        }
                    }) {
                        Text("Mark Attendance")
                    }
                }
                is VerificationState.Verifying -> {
                    CircularProgressIndicator()
                    Text("Verifying conditions...")
                }
                is VerificationState.Error -> {
                    Text(state.message, color = Color.Red)
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(onClick = {
                        if (locationPermissions.allPermissionsGranted) {
                             getCurrentLocation(context) { latitude, longitude ->
                                attendanceViewModel.verifyAttendance(context, latitude, longitude)
                            }
                        } else {
                            locationPermissions.launchMultiplePermissionRequest()
                        }
                    }) {
                        Text("Retry")
                    }
                }
                is VerificationState.Success -> {
                    Text("Verified! Redirecting to camera...")
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
        Button(onClick = {
            navController.currentBackStackEntry?.savedStateHandle?.set("employee", employee)
            navController.navigate(Screen.AttendanceHistory.route)
        }) {
            Text("View My Attendance")
        }
    }
}

@SuppressLint("MissingPermission")
private fun getCurrentLocation(context: Context, onLocationFetched: (Double, Double) -> Unit) {
    val fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)
    fusedLocationClient.lastLocation.addOnSuccessListener { location ->
        if (location != null) {
            onLocationFetched(location.latitude, location.longitude)
        }
    }
}
