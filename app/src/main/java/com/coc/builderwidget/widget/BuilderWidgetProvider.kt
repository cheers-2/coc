package com.coc.builderwidget.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.view.View
import android.widget.RemoteViews
import com.coc.builderwidget.MainActivity
import com.coc.builderwidget.R
import com.coc.builderwidget.data.StorageManager
import com.coc.builderwidget.data.TimeFormatter
import com.coc.builderwidget.worker.WidgetUpdateWorker

class BuilderWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        WidgetUpdateWorker.schedule(context)
        updateAllWidgets(context, appWidgetManager, appWidgetIds)
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        if (intent.action == ACTION_REFRESH) {
            val manager = AppWidgetManager.getInstance(context)
            val ids = manager.getAppWidgetIds(ComponentName(context, BuilderWidgetProvider::class.java))
            updateAllWidgets(context, manager, ids)
        }
    }

    companion object {
        const val ACTION_REFRESH = "com.coc.builderwidget.ACTION_REFRESH"

        fun triggerUpdate(context: Context) {
            val manager = AppWidgetManager.getInstance(context)
            val ids = manager.getAppWidgetIds(ComponentName(context, BuilderWidgetProvider::class.java))
            if (ids.isNotEmpty()) {
                updateAllWidgets(context, manager, ids)
            }
        }

        fun updateAllWidgets(
            context: Context,
            appWidgetManager: AppWidgetManager,
            appWidgetIds: IntArray
        ) {
            val storage = StorageManager(context)
            val onlyBuilders = storage.getOnlyHomeBuilders()
            val allTasks = storage.getAllBuilderTasksSorted(onlyHomeBuilders = onlyBuilders)
            val now = System.currentTimeMillis()

            for (widgetId in appWidgetIds) {
                val views = RemoteViews(context.packageName, R.layout.widget_builder)

                // Open app on widget click
                val openAppIntent = PendingIntent.getActivity(
                    context,
                    0,
                    Intent(context, MainActivity::class.java),
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
                views.setOnClickPendingIntent(R.id.widget_root, openAppIntent)

                // Refresh button click
                val refreshIntent = PendingIntent.getBroadcast(
                    context,
                    1,
                    Intent(context, BuilderWidgetProvider::class.java).apply { action = ACTION_REFRESH },
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
                views.setOnClickPendingIntent(R.id.btn_refresh, refreshIntent)

                views.removeAllViews(R.id.widget_list_container)

                if (allTasks.isEmpty()) {
                    views.setViewVisibility(R.id.txt_empty, View.VISIBLE)
                } else {
                    views.setViewVisibility(R.id.txt_empty, View.GONE)
                    // Show up to 5 soonest builders across both accounts
                    allTasks.take(5).forEach { task ->
                        val row = RemoteViews(context.packageName, R.layout.widget_builder_row)
                        row.setTextViewText(R.id.row_account_badge, task.accountName)
                        row.setInt(
                            R.id.row_account_badge,
                            "setBackgroundResource",
                            if (task.accountSlot == 1) R.drawable.badge_acc1 else R.drawable.badge_acc2
                        )

                        val title = "${task.itemName} (Lv.${task.currentLevel + 1})"
                        row.setTextViewText(R.id.row_item_name, title)

                        val rem = task.remainingMillis(now)
                        val timeText = if (rem <= 0L) {
                            "SELESAI!"
                        } else {
                            "${TimeFormatter.formatRemaining(rem)} • ${TimeFormatter.formatFinishClock(task.finishTimeMillis)}"
                        }
                        row.setTextViewText(R.id.row_time_left, timeText)
                        row.setTextColor(
                            R.id.row_time_left,
                            if (rem <= 0L) Color.parseColor("#4ADE80")
                            else if (rem <= 3_600_000L) Color.parseColor("#FACC15")
                            else Color.parseColor("#E2E8F0")
                        )

                        views.addView(R.id.widget_list_container, row)
                    }
                }

                appWidgetManager.updateAppWidget(widgetId, views)
            }
        }
    }
}
