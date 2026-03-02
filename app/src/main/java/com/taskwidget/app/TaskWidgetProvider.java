package com.taskwidget.app;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.widget.RemoteViews;

import java.util.List;

/**
 * Widget provider that manages the task list widget lifecycle.
 */
public class TaskWidgetProvider extends AppWidgetProvider {

    public static final String ACTION_REFRESH = "com.taskwidget.app.ACTION_REFRESH";

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        for (int appWidgetId : appWidgetIds) {
            updateWidget(context, appWidgetManager, appWidgetId);
        }
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        super.onReceive(context, intent);

        if (ACTION_REFRESH.equals(intent.getAction())) {
            AppWidgetManager manager = AppWidgetManager.getInstance(context);
            ComponentName widget = new ComponentName(context, TaskWidgetProvider.class);
            int[] ids = manager.getAppWidgetIds(widget);
            // Notify data changed so RemoteViewsFactory reloads
            for (int id : ids) {
                manager.notifyAppWidgetViewDataChanged(id, R.id.widget_list);
                updateWidget(context, manager, id);
            }
        }
    }

    @Override
    public void onDeleted(Context context, int[] appWidgetIds) {
        for (int id : appWidgetIds) {
            FilterConfig.delete(context, id);
        }
    }

    static void updateWidget(Context context, AppWidgetManager appWidgetManager, int appWidgetId) {
        RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.widget_layout);

        // Set up the RemoteViews adapter for the list
        Intent serviceIntent = new Intent(context, TaskWidgetService.class);
        serviceIntent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);
        serviceIntent.setData(Uri.parse(serviceIntent.toUri(Intent.URI_INTENT_SCHEME)));
        views.setRemoteAdapter(R.id.widget_list, serviceIntent);
        views.setEmptyView(R.id.widget_list, R.id.widget_empty);

        // Set task count in header
        FilterConfig config = FilterConfig.load(context, appWidgetId);
        List<TaskItem> tasks = TaskParser.parseTasks(config.getFolderPath(), config);
        views.setTextViewText(R.id.widget_count, tasks.size() + " tasks");

        // Refresh button intent
        Intent refreshIntent = new Intent(context, TaskWidgetProvider.class);
        refreshIntent.setAction(ACTION_REFRESH);
        PendingIntent refreshPending = PendingIntent.getBroadcast(
                context, 0, refreshIntent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        views.setOnClickPendingIntent(R.id.widget_refresh, refreshPending);

        // Click on header opens config
        Intent configIntent = new Intent(context, ConfigActivity.class);
        configIntent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);
        PendingIntent configPending = PendingIntent.getActivity(
                context, appWidgetId, configIntent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        views.setOnClickPendingIntent(R.id.widget_title, configPending);

        // Click on list item opens file in Obsidian
        Intent itemIntent = new Intent(Intent.ACTION_VIEW);
        itemIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        PendingIntent itemPending = PendingIntent.getActivity(
                context, appWidgetId + 1000, itemIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_MUTABLE);
        views.setPendingIntentTemplate(R.id.widget_list, itemPending);

        appWidgetManager.updateAppWidget(appWidgetId, views);
        appWidgetManager.notifyAppWidgetViewDataChanged(appWidgetId, R.id.widget_list);
    }
}
