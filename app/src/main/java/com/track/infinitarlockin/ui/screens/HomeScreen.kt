package com.track.infinitarlockin.ui.screens

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Fingerprint
import androidx.compose.material.icons.outlined.History
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.painterResource
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
import com.track.infinitarlockin.R
import com.track.infinitarlockin.data.remote.dto.Employee
import com.track.infinitarlockin.ui.navigation.Screen
import com.track.infinitarlockin.ui.viewmodels.AttendanceViewModel
import com.track.infinitarlockin.ui.viewmodels.AuthState
import com.track.infinitarlockin.ui.viewmodels.MainViewModel
import com.track.infinitarlockin.ui.viewmodels.VerificationState
import kotlinx.coroutines.delay
import kotlin.random.Random

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

    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
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
                        modifier = Modifier.align(Alignment.Center).padding(32.dp),
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
                    Text("Authentication lost. Redirecting...", modifier = Modifier.align(Alignment.Center))
                }
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
    var isVisible by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        isVisible = true
    }

    val locationPermissions = rememberMultiplePermissionsState(
        permissions = listOf(
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.ACCESS_FINE_LOCATION,
        )
    )

    LaunchedEffect(verificationState) {
        if (verificationState is VerificationState.Success) {
            delay(500)
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
            .padding(start = 32.dp, end = 32.dp, bottom = 32.dp, top = 64.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        AnimatedVisibility(
            visible = isVisible,
            enter = slideInVertically(initialOffsetY = { -it }, animationSpec = tween(800)) + fadeIn(animationSpec = tween(800))
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(text = "Welcome,", style = MaterialTheme.typography.headlineMedium)
                Text(
                    text = employee.name,
                    style = MaterialTheme.typography.displaySmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        AnimatedVisibility(
            visible = isVisible,
            enter = fadeIn(animationSpec = tween(800, delayMillis = 300))
        ) {
            if (hasClockedIn) {
                ClockedInContent()
            } else {
                AttendanceActionContent(
                    state = verificationState,
                    onMarkAttendance = {
                        if (locationPermissions.allPermissionsGranted) {
                            getCurrentLocation(context) { latitude, longitude ->
                                attendanceViewModel.verifyAttendance(context, latitude, longitude)
                            }
                        } else {
                            locationPermissions.launchMultiplePermissionRequest()
                        }
                    }
                )
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        AnimatedVisibility(
            visible = isVisible,
            enter = slideInVertically(initialOffsetY = { it }, animationSpec = tween(800, delayMillis = 200)) + fadeIn(animationSpec = tween(800, delayMillis = 200))
        ) {
            TextButton(onClick = {
                navController.currentBackStackEntry?.savedStateHandle?.set("employee", employee)
                navController.navigate(Screen.AttendanceHistory.route)
            }) {
                Icon(Icons.Outlined.History, contentDescription = "History", modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("View My Attendance History")
            }
        }
    }
}

@Composable
private fun ClockedInContent() {
    var showEasterEgg by remember { mutableStateOf(false) }

    // --- EASTER EGG IMAGE LOGIC ---
    val easterEggImages = remember {
        listOf(
            R.drawable.kibby1,
            R.drawable.kibby2,
            R.drawable.kibby3,
            R.drawable.kibby4,
            R.drawable.kibby5,
            R.drawable.kibby6,
            R.drawable.kibby7,
            R.drawable.kibby8,
            R.drawable.kibby9,
            R.drawable.kibby10
        )
    }
    // Hold the currently selected random image in a state
    var currentImage by remember { mutableStateOf(easterEggImages.first()) }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier
            .fillMaxWidth()
            .clickable {
                showEasterEgg = !showEasterEgg
                // Pick a new random image every time it's clicked
                if (showEasterEgg) {
                    currentImage = easterEggImages.random()
                }
            }
    ) {
        if (showEasterEgg) {
            Image(
                painter = painterResource(id = currentImage),
                contentDescription = "Easter Egg",
                contentScale = ContentScale.Crop, // Use Crop to fill the space nicely
                modifier = Modifier
                    .size(200.dp) // Make the image much bigger
            )
        } else {
            Icon(
                Icons.Outlined.CheckCircle,
                contentDescription = "Success",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(200.dp) // Make the icon the same bigger size
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = if (showEasterEgg) "Do it for her!" else "You have already clocked in for today.",
            style = MaterialTheme.typography.titleLarge,
            textAlign = TextAlign.Center
        )

        if (!showEasterEgg) {
            Text(
                text = "See you tomorrow!",
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                color = Color.Gray
            )
        }
    }
}

@Composable
private fun AttendanceActionContent(
    state: VerificationState,
    onMarkAttendance: () -> Unit
) {
    // ... (This function remains unchanged)
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier.fillMaxWidth().padding(16.dp)
    ) {
        when (state) {
            is VerificationState.Idle -> {
                Button(
                    onClick = onMarkAttendance,
                    modifier = Modifier.fillMaxWidth().height(60.dp)
                ) {
                    Icon(Icons.Outlined.Fingerprint, contentDescription = null, modifier = Modifier.size(28.dp))
                    Spacer(modifier = Modifier.width(12.dp))
                    Text("Mark My Attendance", fontSize = 18.sp)
                }
            }
            is VerificationState.Verifying -> {
                CircularProgressIndicator()
                Spacer(modifier = Modifier.height(16.dp))
                Text("Verifying conditions...", style = MaterialTheme.typography.bodyLarge)
            }
            is VerificationState.Error -> {
                Text(state.message, color = MaterialTheme.colorScheme.error, textAlign = TextAlign.Center)
                Spacer(modifier = Modifier.height(16.dp))
                Button(onClick = onMarkAttendance) {
                    Text("Retry")
                }
            }
            is VerificationState.Success -> {
                Text("Verified!", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.titleMedium)
            }
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
