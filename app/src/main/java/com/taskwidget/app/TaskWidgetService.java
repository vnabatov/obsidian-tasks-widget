package com.taskwidget.app;

import android.content.Context;
import android.content.Intent;
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
            tasks = TaskParser.parseTasks(config.getFolderPath(), config);
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

            views.setTextViewText(R.id.item_task_text, task.getText());
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
