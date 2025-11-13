package com.track.infinitarlockin

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
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
import androidx.core.graphics.toColorInt
import androidx.navigation.compose.rememberNavController
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberPermissionState
import com.track.infinitarlockin.ui.navigation.AppNavHost
import com.track.infinitarlockin.ui.navigation.Screen
import com.track.infinitarlockin.ui.theme.InfinitarLockinTheme
import com.track.infinitarlockin.ui.viewmodels.AuthState
import com.track.infinitarlockin.ui.viewmodels.MainViewModel
import com.track.infinitarlockin.worker.DailyReminderWorker
import java.util.Calendar
import java.util.concurrent.TimeUnit

@OptIn(ExperimentalPermissionsApi::class)
class MainActivity : ComponentActivity() {

    private val mainViewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        scheduleDailyReminder()

        enableEdgeToEdge()
        setContent {
            InfinitarLockinTheme {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    val notificationPermissionState = rememberPermissionState(
                        Manifest.permission.POST_NOTIFICATIONS
                    )
                    LaunchedEffect(Unit) {
                        notificationPermissionState.launchPermissionRequest()
                    }
                }

                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    val navController = rememberNavController()
                    val authState by mainViewModel.authState.collectAsState()

                    LaunchedEffect(authState) {
                        when (authState) {
                            is AuthState.Authenticated -> {
                                navController.navigate(Screen.Home.route) {
                                    launchSingleTop = true
                                    popUpTo(navController.graph.startDestinationId) { inclusive = true }
                                }
                            }
                            is AuthState.Unauthenticated -> {
                                navController.navigate(Screen.Register.route) {
                                    launchSingleTop = true
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

    private fun scheduleDailyReminder() {
        createNotificationChannel()

        val workManager = WorkManager.getInstance(applicationContext)
        val workName = "DailyAttendanceReminder"

        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 9)
            set(Calendar.MINUTE, 5)
            set(Calendar.SECOND, 0)
        }
        
        if (calendar.before(Calendar.getInstance())) {
            calendar.add(Calendar.DAY_OF_YEAR, 1)
        }
        
        val initialDelay = calendar.timeInMillis - System.currentTimeMillis()

        val reminderRequest = PeriodicWorkRequestBuilder<DailyReminderWorker>(1, TimeUnit.DAYS)
            .setInitialDelay(initialDelay, TimeUnit.MILLISECONDS)
            .build()

        workManager.enqueueUniquePeriodicWork(
            workName,
            ExistingPeriodicWorkPolicy.KEEP, // Use KEEP to prevent rescheduling if already set
            reminderRequest
        )
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channelId = "daily_reminder_channel"
            val name = "Daily Reminders"
            val descriptionText = "Reminders to clock in for attendance."
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(channelId, name, importance).apply {
                description = descriptionText
                enableLights(true)
                lightColor = "#FF0000".toColorInt()
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 500, 250, 500, 250, 500, 250, 500)
            }
            val notificationManager: NotificationManager =
                getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }
}
