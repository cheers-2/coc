package com.coc.builderwidget.worker

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.coc.builderwidget.MainActivity
import com.coc.builderwidget.data.StorageManager
import com.coc.builderwidget.widget.BuilderWidgetProvider
import java.util.concurrent.TimeUnit

class WidgetUpdateWorker(
    private val context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val appWidgetManager = AppWidgetManager.getInstance(context)
        val component = ComponentName(context, BuilderWidgetProvider::class.java)
        val ids = appWidgetManager.getAppWidgetIds(component)
        if (ids.isNotEmpty()) {
            BuilderWidgetProvider.updateAllWidgets(context, appWidgetManager, ids)
        }
        checkAndNotifyFinishedBuilders()
        return Result.success()
    }

    private fun checkAndNotifyFinishedBuilders() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val granted = ContextCompat.checkSelfPermission(
                context,
                android.Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
            if (!granted) return
        }

        val storage = StorageManager(context)
        val prefs = context.getSharedPreferences("coc_notified", Context.MODE_PRIVATE)
        val now = System.currentTimeMillis()
        val tasks = storage.getAllBuilderTasksSorted(onlyHomeBuilders = storage.getOnlyHomeBuilders())

        val newlyFinished = tasks.filter { task ->
            val key = "notified_${task.accountSlot}_${task.itemId}_${task.finishTimeMillis}"
            task.isFinished(now) && !prefs.getBoolean(key, false)
        }

        if (newlyFinished.isEmpty()) return

        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channelId = "coc_builder_finish"
        val channel = NotificationChannel(
            channelId,
            "Tukang CoC Selesai",
            NotificationManager.IMPORTANCE_DEFAULT
        )
        manager.createNotificationChannel(channel)

        val openIntent = PendingIntent.getActivity(
            context,
            0,
            Intent(context, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        newlyFinished.take(3).forEachIndexed { idx, task ->
            val key = "notified_${task.accountSlot}_${task.itemId}_${task.finishTimeMillis}"
            prefs.edit().putBoolean(key, true).apply()

            val notification = NotificationCompat.Builder(context, channelId)
                .setSmallIcon(android.R.drawable.ic_menu_recent_history)
                .setContentTitle("Tukang Selesai! (${task.accountName})")
                .setContentText("${task.itemName} sudah selesai di-upgrade.")
                .setContentIntent(openIntent)
                .setAutoCancel(true)
                .build()

            manager.notify(1000 + idx + task.accountSlot * 100, notification)
        }
    }

    companion object {
        private const val WORK_NAME = "coc_builder_widget_periodic_update"

        fun schedule(context: Context) {
            val request = PeriodicWorkRequestBuilder<WidgetUpdateWorker>(15, TimeUnit.MINUTES)
                .build()
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                request
            )
        }
    }
}
