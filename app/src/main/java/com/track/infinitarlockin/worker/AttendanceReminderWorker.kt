package com.track.infinitarlockin.worker

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Build
import android.provider.Settings
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.track.infinitarlockin.R
import com.track.infinitarlockin.data.remote.RetrofitClient
import java.util.Calendar

class AttendanceReminderWorker(
    private val context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        val today = Calendar.getInstance()
        val dayOfWeek = today.get(Calendar.DAY_OF_WEEK)

        // Only run on workdays (Monday to Saturday)
        if (dayOfWeek == Calendar.SUNDAY) {
            return Result.success() // It's Sunday, do nothing.
        }

        try {
            val deviceId = Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID)
            val response = RetrofitClient.instance.checkEmployee(deviceId)

            // If the user is registered and has NOT clocked in today, send a notification
            if (response.registered && response.employee?.hasClockedInToday == false) {
                sendNotification()
            }
        } catch (e: Exception) {
            // If there's a network error, etc., just retry later.
            return Result.retry()
        }

        return Result.success()
    }

    private fun sendNotification() {
        val channelId = "attendance_reminder_channel"
        val notificationId = 1

        val builder = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.ic_launcher_foreground) // Use your app's launcher icon
            .setContentTitle("Attendance Reminder")
            .setContentText("Please don't forget to clock in for today!")
            .setPriority(NotificationCompat.PRIORITY_HIGH) // High priority to make it pop up
            .setVibrate(longArrayOf(0, 500, 200, 500, 200, 500)) // Strong, repeating vibration
            .setLights(Color.RED, 3000, 3000)
            .setAutoCancel(true)

        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            // This is a background task, so we can't request permission here.
            // The user must have already granted it. If not, we can't show the notification.
            return
        }

        with(NotificationManagerCompat.from(context)) {
            notify(notificationId, builder.build())
        }
    }
}
