package com.taskwidget.app;

import android.appwidget.AppWidgetManager;
import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

/**
 * Widget configuration activity shown when adding the widget to home screen.
 */
public class WidgetConfigActivity extends AppCompatActivity {

    private int appWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID;

    private EditText editVaultName;
    private EditText editFolderPath;
    private EditText editIncludeText;
    private EditText editExcludeText;
    private EditText editIncludePath;
    private EditText editExcludePath;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_config);

        // Set cancel result in case user backs out
        setResult(RESULT_CANCELED);

        // Get the widget ID from the intent
        Intent intent = getIntent();
        Bundle extras = intent.getExtras();
        if (extras != null) {
            appWidgetId = extras.getInt(
                    AppWidgetManager.EXTRA_APPWIDGET_ID,
                    AppWidgetManager.INVALID_APPWIDGET_ID);
        }

        if (appWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
            finish();
            return;
        }

        editVaultName = findViewById(R.id.edit_vault_name);
        editFolderPath = findViewById(R.id.edit_folder_path);
        editIncludeText = findViewById(R.id.edit_include_text);
        editExcludeText = findViewById(R.id.edit_exclude_text);
        editIncludePath = findViewById(R.id.edit_include_path);
        editExcludePath = findViewById(R.id.edit_exclude_path);

        // Load defaults from global config (id=0)
        FilterConfig defaults = FilterConfig.load(this, 0);
        editVaultName.setText(defaults.getVaultName());
        editFolderPath.setText(defaults.getFolderPath());
        editIncludeText.setText(joinList(defaults.getIncludeTexts()));
        editExcludeText.setText(joinList(defaults.getExcludeTexts()));
        editIncludePath.setText(joinList(defaults.getIncludePaths()));
        editExcludePath.setText(joinList(defaults.getExcludePaths()));

        Button btnSave = findViewById(R.id.btn_save);
        btnSave.setOnClickListener(v -> saveAndFinish());
    }

    private void saveAndFinish() {
        FilterConfig config = new FilterConfig();
        config.setVaultName(editVaultName.getText().toString().trim());
        config.setFolderPath(editFolderPath.getText().toString().trim());
        config.setIncludeTexts(splitList(editIncludeText.getText().toString()));
        config.setExcludeTexts(splitList(editExcludeText.getText().toString()));
        config.setIncludePaths(splitList(editIncludePath.getText().toString()));
        config.setExcludePaths(splitList(editExcludePath.getText().toString()));

        config.save(this, appWidgetId);

        // Trigger widget update
        AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(this);
        TaskWidgetProvider.updateWidget(this, appWidgetManager, appWidgetId);

        // Return success
        Intent resultValue = new Intent();
        resultValue.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);
        setResult(RESULT_OK, resultValue);

        Toast.makeText(this, "Widget configured!", Toast.LENGTH_SHORT).show();
        finish();
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
