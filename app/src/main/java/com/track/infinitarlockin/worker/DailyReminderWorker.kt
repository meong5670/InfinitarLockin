package com.track.infinitarlockin.worker

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Build
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.track.infinitarlockin.R
import java.util.Calendar

class DailyReminderWorker(
    private val context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    companion object {
        const val PREFS_NAME = "AttendanceAppPrefs"
        const val LAST_PRESS_KEY = "last_attendance_press_timestamp"
    }

    override suspend fun doWork(): Result {
        val today = Calendar.getInstance()
        val dayOfWeek = today.get(Calendar.DAY_OF_WEEK)

        if (dayOfWeek == Calendar.SUNDAY) {
            return Result.success()
        }

        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val lastPressTimestamp = prefs.getLong(LAST_PRESS_KEY, 0L)

        if (isToday(lastPressTimestamp)) {
            return Result.success()
        }

        sendNotification()

        return Result.success()
    }

    private fun isToday(timestamp: Long): Boolean {
        if (timestamp == 0L) return false
        val today = Calendar.getInstance()
        val lastPressDate = Calendar.getInstance().apply { timeInMillis = timestamp }
        return today.get(Calendar.YEAR) == lastPressDate.get(Calendar.YEAR) &&
               today.get(Calendar.DAY_OF_YEAR) == lastPressDate.get(Calendar.DAY_OF_YEAR)
    }

    private fun sendNotification() {
        // Use the new, unique channel ID
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
