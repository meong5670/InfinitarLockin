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
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
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
import com.track.infinitarlockin.ui.viewmodels.UiState
import com.track.infinitarlockin.worker.DailyReminderWorker
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.*
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
                        mainViewModel = mainViewModel,
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
    mainViewModel: MainViewModel,
    attendanceViewModel: AttendanceViewModel
) {
    val context = LocalContext.current
    val uiState by attendanceViewModel.uiState.collectAsState()
    var isVisible by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) { isVisible = true }

    val locationPermissions = rememberMultiplePermissionsState(
        permissions = listOf(
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.ACCESS_FINE_LOCATION,
        )
    )

    LaunchedEffect(uiState) {
        val state = uiState
        if (state is UiState.Success) {
            if (state.isVerification) {
                delay(500)
                navController.navigate(Screen.Camera.route)
                attendanceViewModel.resetState()
            } else {
                // This logic is now handled by CameraScreen for clock-in
                // and will be handled directly in the clock-out action.
                attendanceViewModel.resetState()
            }
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            attendanceViewModel.resetState()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(start = 32.dp, end = 32.dp, top = 64.dp),
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
            when (employee.attendanceStatus) {
                "NONE" -> AttendanceActionContent(
                    uiState = uiState,
                    onMarkAttendance = {
                        val prefs = context.getSharedPreferences(DailyReminderWorker.PREFS_NAME, Context.MODE_PRIVATE)
                        prefs.edit().putLong(DailyReminderWorker.LAST_PRESS_KEY, System.currentTimeMillis()).apply()
                        
                        if (locationPermissions.allPermissionsGranted) {
                            getCurrentLocation(context) { latitude, longitude ->
                                attendanceViewModel.verifyAttendance(context, latitude, longitude)
                            }
                        } else {
                            locationPermissions.launchMultiplePermissionRequest()
                        }
                    }
                )
                "CLOCKED_IN" -> ClockOutContent(
                    uiState = uiState,
                    onClockOut = {
                        if (locationPermissions.allPermissionsGranted) {
                            getCurrentLocation(context) { latitude, longitude ->
                                 attendanceViewModel.submitClockOut(employee.id, employee.deviceId, context, latitude, longitude)
                            }
                        } else {
                            locationPermissions.launchMultiplePermissionRequest()
                        }
                    },
                    mainViewModel = mainViewModel 
                )
                "COMPLETED" -> ClockedInContent()
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .navigationBarsPadding()
                .padding(bottom = 32.dp)
        ) {
            Text(
                text = "STATUS: ${employee.attendanceStatus ?: "NULL"}",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = Color.Red
            )
            
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
}

@Composable
private fun ClockedInContent() {
    var showEasterEgg by remember { mutableStateOf(false) }
    val easterEggImages = remember {
        listOf(
            R.drawable.kibby1, R.drawable.kibby2, R.drawable.kibby3, R.drawable.kibby4, R.drawable.kibby5,
            R.drawable.kibby6, R.drawable.kibby7, R.drawable.kibby8, R.drawable.kibby9, R.drawable.kibby10
        )
    }
    var currentImage by remember { mutableStateOf(easterEggImages.first()) }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier.fillMaxWidth().clickable {
            showEasterEgg = !showEasterEgg
            if (showEasterEgg) {
                currentImage = easterEggImages.random()
            }
        }
    ) {
        if (showEasterEgg) {
            Image(
                painter = painterResource(id = currentImage),
                contentDescription = "Easter Egg",
                contentScale = ContentScale.Crop,
                modifier = Modifier.size(200.dp)
            )
        } else {
            Icon(
                Icons.Outlined.CheckCircle,
                contentDescription = "Success",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(80.dp)
            )
        }
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = if (showEasterEgg) "Do it for her!" else "You have already worked today.",
            style = MaterialTheme.typography.titleLarge,
            textAlign = TextAlign.Center
        )
        if (!showEasterEgg) {
            Text(
                text = "Enjoy the rest of ur day ig",
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                color = Color.Gray
            )
        }
    }
}

@Composable
private fun ClockOutContent(
    uiState: UiState,
    onClockOut: () -> Unit,
    mainViewModel: MainViewModel
) {
    val context = LocalContext.current
    var showClockOutDialog by remember { mutableStateOf(false) }
    var clockOutTime by remember { mutableStateOf("") }
    
    LaunchedEffect(uiState) {
        if (uiState is UiState.Success && !uiState.isVerification) {
            mainViewModel.checkDeviceRegistration(context)
        }
    }

    if (showClockOutDialog) {
        AlertDialog(
            onDismissRequest = { showClockOutDialog = false },
            title = { Text("Confirm Clock Out") },
            text = {
                Column {
                    Text("Are you sure you want to clock out at this time?")
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(clockOutTime, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showClockOutDialog = false
                        onClockOut()
                    }
                ) {
                    Text("Confirm")
                }
            },
            dismissButton = {
                TextButton(onClick = { showClockOutDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier.fillMaxWidth().padding(16.dp)
    ) {
        when (uiState) {
            is UiState.Idle -> {
                Button(
                    onClick = {
                        val sdf = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
                        clockOutTime = sdf.format(Date())
                        showClockOutDialog = true
                    },
                    modifier = Modifier.fillMaxWidth().height(60.dp)
                ) {
                    Icon(Icons.Outlined.Logout, contentDescription = null, modifier = Modifier.size(28.dp))
                    Spacer(modifier = Modifier.width(12.dp))
                    Text("Clock Out", fontSize = 18.sp)
                }
            }
            is UiState.Loading -> {
                CircularProgressIndicator()
                Spacer(modifier = Modifier.height(16.dp))
                Text("Submitting...", style = MaterialTheme.typography.bodyLarge)
            }
            is UiState.Error -> {
                Text(uiState.message, color = MaterialTheme.colorScheme.error, textAlign = TextAlign.Center)
                Spacer(modifier = Modifier.height(16.dp))
                Button(onClick = onClockOut) { Text("Retry") }
            }
            is UiState.Success -> {
                 Text(uiState.message, color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.titleMedium)
            }
        }
    }
}

@Composable
private fun AttendanceActionContent(
    uiState: UiState,
    onMarkAttendance: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier.fillMaxWidth().padding(16.dp)
    ) {
        when (uiState) {
            is UiState.Idle -> {
                Button(
                    onClick = onMarkAttendance,
                    modifier = Modifier.fillMaxWidth().height(60.dp)
                ) {
                    Icon(Icons.Outlined.Fingerprint, contentDescription = null, modifier = Modifier.size(28.dp))
                    Spacer(modifier = Modifier.width(12.dp))
                    Text("Mark My Attendance", fontSize = 18.sp)
                }
            }
            is UiState.Loading -> {
                CircularProgressIndicator()
                Spacer(modifier = Modifier.height(16.dp))
                Text("Verifying conditions...", style = MaterialTheme.typography.bodyLarge)
            }
            is UiState.Error -> {
                Text(uiState.message, color = MaterialTheme.colorScheme.error, textAlign = TextAlign.Center)
                Spacer(modifier = Modifier.height(16.dp))
                Button(onClick = onMarkAttendance) {
                    Text("Retry")
                }
            }
            is UiState.Success -> {
                Text("Verified! Redirecting to camera...", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.titleMedium)
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
