package com.taskwidget.app;

import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.Settings;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

/**
 * Main launcher activity — configure filters for all widget instances.
 * Also used when tapping the widget header.
 */
public class ConfigActivity extends AppCompatActivity {

    private static final int STORAGE_PERMISSION_CODE = 100;
    private static final int MANAGE_STORAGE_CODE = 101;

    private EditText editVaultName;
    private EditText editFolderPath;
    private EditText editIncludeText;
    private EditText editExcludeText;
    private EditText editIncludePath;
    private EditText editExcludePath;

    private int appWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_config);

        // Get widget ID if launched from widget
        Intent intent = getIntent();
        if (intent != null) {
            appWidgetId = intent.getIntExtra(
                    AppWidgetManager.EXTRA_APPWIDGET_ID,
                    AppWidgetManager.INVALID_APPWIDGET_ID);
        }

        editVaultName = findViewById(R.id.edit_vault_name);
        editFolderPath = findViewById(R.id.edit_folder_path);
        editIncludeText = findViewById(R.id.edit_include_text);
        editExcludeText = findViewById(R.id.edit_exclude_text);
        editIncludePath = findViewById(R.id.edit_include_path);
        editExcludePath = findViewById(R.id.edit_exclude_path);

        Button btnSave = findViewById(R.id.btn_save);
        btnSave.setOnClickListener(v -> saveConfig());

        // Load existing config
        loadConfig();

        // Request storage permissions
        requestStoragePermission();
    }

    private void loadConfig() {
        // Use widget ID 0 as default for global config
        int id = (appWidgetId != AppWidgetManager.INVALID_APPWIDGET_ID) ? appWidgetId : 0;
        FilterConfig config = FilterConfig.load(this, id);

        editVaultName.setText(config.getVaultName());
        editFolderPath.setText(config.getFolderPath());
        editIncludeText.setText(joinList(config.getIncludeTexts()));
        editExcludeText.setText(joinList(config.getExcludeTexts()));
        editIncludePath.setText(joinList(config.getIncludePaths()));
        editExcludePath.setText(joinList(config.getExcludePaths()));
    }

    private void saveConfig() {
        FilterConfig config = new FilterConfig();
        config.setVaultName(editVaultName.getText().toString().trim());
        config.setFolderPath(editFolderPath.getText().toString().trim());
        config.setIncludeTexts(splitList(editIncludeText.getText().toString()));
        config.setExcludeTexts(splitList(editExcludeText.getText().toString()));
        config.setIncludePaths(splitList(editIncludePath.getText().toString()));
        config.setExcludePaths(splitList(editExcludePath.getText().toString()));

        // Save for this widget and also as global default (id=0)
        if (appWidgetId != AppWidgetManager.INVALID_APPWIDGET_ID) {
            config.save(this, appWidgetId);
        }
        config.save(this, 0); // Global default

        // Refresh all widgets
        AppWidgetManager manager = AppWidgetManager.getInstance(this);
        ComponentName widget = new ComponentName(this, TaskWidgetProvider.class);
        int[] ids = manager.getAppWidgetIds(widget);
        for (int id : ids) {
            // Also save config for each widget that uses default
            FilterConfig existing = FilterConfig.load(this, id);
            if (existing.getFolderPath().equals(new FilterConfig().getFolderPath()) ||
                    appWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
                config.save(this, id);
            }
            TaskWidgetProvider.updateWidget(this, manager, id);
        }

        Toast.makeText(this, "Settings saved! Widgets refreshed.", Toast.LENGTH_SHORT).show();

        // If launched from widget config, send result
        if (appWidgetId != AppWidgetManager.INVALID_APPWIDGET_ID) {
            Intent resultIntent = new Intent();
            resultIntent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);
            setResult(RESULT_OK, resultIntent);
        }

        finish();
    }

    private void requestStoragePermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            // Android 11+ needs MANAGE_EXTERNAL_STORAGE
            if (!Environment.isExternalStorageManager()) {
                try {
                    Intent intent = new Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION);
                    intent.setData(Uri.parse("package:" + getPackageName()));
                    startActivityForResult(intent, MANAGE_STORAGE_CODE);
                } catch (Exception e) {
                    Intent intent = new Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION);
                    startActivityForResult(intent, MANAGE_STORAGE_CODE);
                }
            }
        } else {
            // Android 10 and below
            if (ContextCompat.checkSelfPermission(this,
                    android.Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this,
                        new String[]{android.Manifest.permission.READ_EXTERNAL_STORAGE},
                        STORAGE_PERMISSION_CODE);
            }
        }
    }

    private String joinList(java.util.List<String> list) {
        if (list == null || list.isEmpty()) return "";
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < list.size(); i++) {
            if (i > 0) sb.append(", ");
            sb.append(list.get(i));
        }
        return sb.toString();
    }

    private java.util.List<String> splitList(String str) {
        java.util.List<String> result = new java.util.ArrayList<>();
        if (str == null || str.trim().isEmpty()) return result;
        String[] parts = str.split(",");
        for (String part : parts) {
            String trimmed = part.trim();
            if (!trimmed.isEmpty()) {
                result.add(trimmed);
            }
        }
        return result;
    }
}
