package com.taskwidget.app;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Holds filter configuration for a widget instance.
 * Persisted in SharedPreferences keyed by widget ID.
 */
public class FilterConfig {
    private static final String PREFS_NAME = "com.taskwidget.app.prefs";
    private static final String KEY_VAULT = "_vault";
    private static final String KEY_FOLDER = "_folder";
    private static final String KEY_INCLUDE_TEXT = "_include_text";
    private static final String KEY_EXCLUDE_TEXT = "_exclude_text";
    private static final String KEY_INCLUDE_PATH = "_include_path";
    private static final String KEY_EXCLUDE_PATH = "_exclude_path";

    private String vaultName;
    private String folderPath;
    private List<String> includeTexts;
    private List<String> excludeTexts;
    private List<String> includePaths;
    private List<String> excludePaths;

    public FilterConfig() {
        this.vaultName = "2";
        this.folderPath = "/storage/emulated/0/Download/yandexSync/2/main/";
        this.includeTexts = new ArrayList<>();
        this.excludeTexts = new ArrayList<>();
        this.includePaths = new ArrayList<>();
        this.excludePaths = new ArrayList<>();
    }

    public String getVaultName() { return vaultName; }
    public void setVaultName(String vaultName) { this.vaultName = vaultName; }

    public String getFolderPath() { return folderPath; }
    public void setFolderPath(String folderPath) { this.folderPath = folderPath; }

    public List<String> getIncludeTexts() { return includeTexts; }
    public void setIncludeTexts(List<String> includeTexts) { this.includeTexts = includeTexts; }

    public List<String> getExcludeTexts() { return excludeTexts; }
    public void setExcludeTexts(List<String> excludeTexts) { this.excludeTexts = excludeTexts; }

    public List<String> getIncludePaths() { return includePaths; }
    public void setIncludePaths(List<String> includePaths) { this.includePaths = includePaths; }

    public List<String> getExcludePaths() { return excludePaths; }
    public void setExcludePaths(List<String> excludePaths) { this.excludePaths = excludePaths; }

    /**
     * Save config for a specific widget ID.
     */
    public void save(Context context, int appWidgetId) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        String prefix = String.valueOf(appWidgetId);

        editor.putString(prefix + KEY_VAULT, vaultName);
        editor.putString(prefix + KEY_FOLDER, folderPath);
        editor.putString(prefix + KEY_INCLUDE_TEXT, joinList(includeTexts));
        editor.putString(prefix + KEY_EXCLUDE_TEXT, joinList(excludeTexts));
        editor.putString(prefix + KEY_INCLUDE_PATH, joinList(includePaths));
        editor.putString(prefix + KEY_EXCLUDE_PATH, joinList(excludePaths));
        editor.apply();
    }

    /**
     * Load config for a specific widget ID.
     */
    public static FilterConfig load(Context context, int appWidgetId) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        String prefix = String.valueOf(appWidgetId);

        FilterConfig config = new FilterConfig();
        config.vaultName = prefs.getString(prefix + KEY_VAULT, config.vaultName);
        config.folderPath = prefs.getString(prefix + KEY_FOLDER, config.folderPath);
        config.includeTexts = splitList(prefs.getString(prefix + KEY_INCLUDE_TEXT, ""));
        config.excludeTexts = splitList(prefs.getString(prefix + KEY_EXCLUDE_TEXT, ""));
        config.includePaths = splitList(prefs.getString(prefix + KEY_INCLUDE_PATH, ""));
        config.excludePaths = splitList(prefs.getString(prefix + KEY_EXCLUDE_PATH, ""));
        return config;
    }

    /**
     * Delete config for a specific widget ID.
     */
    public static void delete(Context context, int appWidgetId) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        String prefix = String.valueOf(appWidgetId);

        editor.remove(prefix + KEY_VAULT);
        editor.remove(prefix + KEY_FOLDER);
        editor.remove(prefix + KEY_INCLUDE_TEXT);
        editor.remove(prefix + KEY_EXCLUDE_TEXT);
        editor.remove(prefix + KEY_INCLUDE_PATH);
        editor.remove(prefix + KEY_EXCLUDE_PATH);
        editor.apply();
    }

    private static String joinList(List<String> list) {
        if (list == null || list.isEmpty()) return "";
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < list.size(); i++) {
            if (i > 0) sb.append(",");
            sb.append(list.get(i).trim());
        }
        return sb.toString();
    }

    private static List<String> splitList(String str) {
        List<String> result = new ArrayList<>();
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
