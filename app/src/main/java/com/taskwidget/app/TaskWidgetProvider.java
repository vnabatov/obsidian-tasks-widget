package com.taskwidget.app;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.widget.RemoteViews;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Widget provider that manages the task list widget lifecycle.
 */
public class TaskWidgetProvider extends AppWidgetProvider {

    public static final String ACTION_REFRESH = "com.taskwidget.app.ACTION_REFRESH";
    public static final String ACTION_COMPLETE = "com.taskwidget.app.ACTION_COMPLETE";
    public static final String ACTION_OPEN_OBSIDIAN = "com.taskwidget.app.ACTION_OPEN_OBSIDIAN";
    public static final String EXTRA_FILE_PATH = "extra_file_path";
    public static final String EXTRA_RAW_LINE = "extra_raw_line";
    public static final String EXTRA_OBSIDIAN_URI = "extra_obsidian_uri";

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        for (int appWidgetId : appWidgetIds) {
            updateWidget(context, appWidgetManager, appWidgetId);
        }
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        super.onReceive(context, intent);
        String action = intent.getAction();

        if (ACTION_REFRESH.equals(action)) {
            AppWidgetManager manager = AppWidgetManager.getInstance(context);
            ComponentName widget = new ComponentName(context, TaskWidgetProvider.class);
            int[] ids = manager.getAppWidgetIds(widget);
            for (int id : ids) {
                manager.notifyAppWidgetViewDataChanged(id, R.id.widget_list);
                updateWidget(context, manager, id);
            }
        } else if (ACTION_COMPLETE.equals(action)) {
            String filePath = intent.getStringExtra(EXTRA_FILE_PATH);
            String rawLine = intent.getStringExtra(EXTRA_RAW_LINE);
            if (filePath != null && rawLine != null) {
                boolean success = completeTask(filePath, rawLine);
                if (success) {
                    Toast.makeText(context, "✅ Task completed", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(context, "❌ Could not complete task", Toast.LENGTH_SHORT).show();
                }
                // Refresh all widgets
                AppWidgetManager manager = AppWidgetManager.getInstance(context);
                ComponentName widget = new ComponentName(context, TaskWidgetProvider.class);
                int[] ids = manager.getAppWidgetIds(widget);
                for (int id : ids) {
                    manager.notifyAppWidgetViewDataChanged(id, R.id.widget_list);
                    updateWidget(context, manager, id);
                }
            }
        } else if (ACTION_OPEN_OBSIDIAN.equals(action)) {
            String obsidianUri = intent.getStringExtra(EXTRA_OBSIDIAN_URI);
            if (obsidianUri != null) {
                Intent viewIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(obsidianUri));
                viewIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                context.startActivity(viewIntent);
            }
        }
    }

    /**
     * Complete a task by replacing "- [ ]" with "- [x]" in the file.
     */
    private static boolean completeTask(String absoluteFilePath, String rawLine) {
        File file = new File(absoluteFilePath);
        if (!file.exists()) return false;

        try {
            List<String> lines = new ArrayList<>();
            boolean found = false;
            try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    if (!found && line.equals(rawLine)) {
                        // Replace first occurrence of "[ ]" with "[x]"
                        line = line.replaceFirst("\\[\\s*\\]", "[x]");
                        found = true;
                    }
                    lines.add(line);
                }
            }
            if (!found) return false;

            try (BufferedWriter writer = new BufferedWriter(new FileWriter(file))) {
                for (int i = 0; i < lines.size(); i++) {
                    writer.write(lines.get(i));
                    if (i < lines.size() - 1) writer.newLine();
                }
            }
            return true;
        } catch (IOException e) {
            return false;
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

        // List item click template (broadcast to this provider)
        Intent itemIntent = new Intent(context, TaskWidgetProvider.class);
        PendingIntent itemPending = PendingIntent.getBroadcast(
                context, appWidgetId + 1000, itemIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_MUTABLE);
        views.setPendingIntentTemplate(R.id.widget_list, itemPending);

        appWidgetManager.updateAppWidget(appWidgetId, views);
        appWidgetManager.notifyAppWidgetViewDataChanged(appWidgetId, R.id.widget_list);
    }
}
