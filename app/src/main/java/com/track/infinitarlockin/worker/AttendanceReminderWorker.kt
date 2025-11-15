package com.track.infinitarlockin.worker

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Color
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

        if (dayOfWeek == Calendar.SUNDAY) {
            return Result.success()
        }

        try {
            val deviceId = Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID)
            val response = RetrofitClient.instance.checkEmployee(deviceId)

            // Use the new status field
            if (response.registered && response.employee?.attendanceStatus == "NONE") {
                sendNotification()
            }
        } catch (e: Exception) {
            return Result.retry()
        }

        return Result.success()
    }

    private fun sendNotification() {
        val channelId = "daily_reminder_channel_v2"
        val notificationId = 101

        val builder = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("Attendance Reminder")
            .setContentText("Clock-in reminder! It's past 9:05 AM.")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setVibrate(longArrayOf(0, 500, 250, 500, 250, 500, 250, 500))
            .setLights(Color.RED, 3000, 3000)
            .setAutoCancel(true)

        if (ActivityCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }

        with(NotificationManagerCompat.from(context)) {
            notify(notificationId, builder.build())
        }
    }
}
