package com.taskwidget.app;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.widget.RemoteViews;
import android.widget.RemoteViewsService;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Service that provides the RemoteViewsFactory for the widget ListView.
 */
public class TaskWidgetService extends RemoteViewsService {

    @Override
    public RemoteViewsFactory onGetViewFactory(Intent intent) {
        return new TaskRemoteViewsFactory(this.getApplicationContext(), intent);
    }

    static class TaskRemoteViewsFactory implements RemoteViewsFactory {
        private Context context;
        private int appWidgetId;
        private String folderPath;
        private String vaultName;
        private List<TaskItem> tasks = new ArrayList<>();

        TaskRemoteViewsFactory(Context context, Intent intent) {
            this.context = context;
            this.appWidgetId = intent.getIntExtra(
                    android.appwidget.AppWidgetManager.EXTRA_APPWIDGET_ID,
                    android.appwidget.AppWidgetManager.INVALID_APPWIDGET_ID);
        }

        @Override
        public void onCreate() {
            loadTasks();
        }

        @Override
        public void onDataSetChanged() {
            loadTasks();
        }

        private void loadTasks() {
            FilterConfig config = FilterConfig.load(context, appWidgetId);
            folderPath = config.getFolderPath();
            vaultName = config.getVaultName();
            tasks = TaskParser.parseTasks(folderPath, config);
        }

        @Override
        public void onDestroy() {
            tasks.clear();
        }

        @Override
        public int getCount() {
            return tasks.size();
        }

        @Override
        public RemoteViews getViewAt(int position) {
            if (position >= tasks.size()) {
                return null;
            }

            TaskItem task = tasks.get(position);
            RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.widget_task_item);

            String displayText = task.getPriorityEmoji().isEmpty()
                    ? task.getText()
                    : task.getPriorityEmoji() + " " + task.getText();
            views.setTextViewText(R.id.item_task_text, displayText);
            views.setTextViewText(R.id.item_task_date, "📅 " + task.getDate());
            views.setTextViewText(R.id.item_task_file, task.getFileName());

            // Color the date based on whether it's today or past
            try {
                LocalDate taskDate = LocalDate.parse(task.getDate());
                LocalDate today = LocalDate.now();
                if (taskDate.isEqual(today)) {
                    views.setTextColor(R.id.item_task_date,
                            context.getResources().getColor(R.color.task_date_today, null));
                } else {
                    views.setTextColor(R.id.item_task_date,
                            context.getResources().getColor(R.color.task_date_past, null));
                }
            } catch (Exception e) {
                // Default color
            }

            // Build Obsidian Advanced URI for clicking to open file
            // obsidian://adv-uri?vault=VAULT&filepath=RELATIVE_PATH
            // Compute filepath relative to the vault root
            // e.g. folder=/storage/.../2/main/ vault=2 => vault root ends at /2/
            // filepath = main/ + relative file path
            String vaultSuffix = "/" + vaultName + "/";
            String filepath;
            if (folderPath.contains(vaultSuffix)) {
                // Get the part after the vault name in the folder path
                int vaultEnd = folderPath.indexOf(vaultSuffix) + vaultSuffix.length();
                String pathAfterVault = folderPath.substring(vaultEnd);
                // task.getFilePath() is relative and may start with /
                String relPath = task.getFilePath();
                if (relPath.startsWith("/")) relPath = relPath.substring(1);
                filepath = pathAfterVault + relPath;
            } else {
                // Fallback: just use relative path
                String relPath = task.getFilePath();
                if (relPath.startsWith("/")) relPath = relPath.substring(1);
                filepath = relPath;
            }

            String obsidianUri = "obsidian://adv-uri?vault=" + Uri.encode(vaultName)
                    + "&filepath=" + Uri.encode(filepath);

            // Fill-in intent for opening in Obsidian (click on task text)
            Intent openIntent = new Intent();
            openIntent.setAction(TaskWidgetProvider.ACTION_OPEN_OBSIDIAN);
            openIntent.putExtra(TaskWidgetProvider.EXTRA_OBSIDIAN_URI, obsidianUri);
            views.setOnClickFillInIntent(R.id.item_task_text, openIntent);

            // Fill-in intent for completing task (click on checkbox)
            String absoluteFilePath = folderPath;
            if (!absoluteFilePath.endsWith("/")) absoluteFilePath += "/";
            String fileRelPath = task.getFilePath();
            if (fileRelPath.startsWith("/")) fileRelPath = fileRelPath.substring(1);
            absoluteFilePath += fileRelPath;

            Intent completeIntent = new Intent();
            completeIntent.setAction(TaskWidgetProvider.ACTION_COMPLETE);
            completeIntent.putExtra(TaskWidgetProvider.EXTRA_FILE_PATH, absoluteFilePath);
            completeIntent.putExtra(TaskWidgetProvider.EXTRA_RAW_LINE, task.getRawLine());
            views.setOnClickFillInIntent(R.id.item_task_complete, completeIntent);

            return views;
        }

        @Override
        public RemoteViews getLoadingView() {
            return null;
        }

        @Override
        public int getViewTypeCount() {
            return 1;
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public boolean hasStableIds() {
            return false;
        }
    }
}
