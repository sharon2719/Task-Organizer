package com.example.myapplication.worker

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import androidx.core.app.NotificationCompat
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.example.myapplication.R

class ReminderWorker(appContext: Context, workerParams: WorkerParameters) : Worker(appContext, workerParams) {

    override fun doWork(): Result {
        val taskTitle = inputData.getString("task_title") ?: return Result.failure()
        val taskId = inputData.getLong("task_id", -1)

        // Create notification channel if it doesn't exist
        val notificationManager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channelId = "task_reminder_channel"

        // For API >= 26, create the notification channel
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Task Reminders",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notifications for task reminders"
            }
            notificationManager.createNotificationChannel(channel)
        }

        // Show the notification
        val notification = NotificationCompat.Builder(applicationContext, channelId)
            .setContentTitle("Reminder: $taskTitle")
            .setContentText("Your task is due!")
            .setSmallIcon(R.drawable.ic_task_reminder)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()

        notificationManager.notify(taskId.toInt(), notification)

        return Result.success()
    }
}
