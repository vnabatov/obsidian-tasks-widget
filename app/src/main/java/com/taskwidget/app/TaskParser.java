package com.taskwidget.app;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Parses markdown files for uncompleted tasks with dates and applies filters.
 */
public class TaskParser {

    // Matches lines like: - [ ] task text 📅 2026-02-09
    // Also handles optional tags like #e01 etc.
    private static final Pattern TASK_PATTERN = Pattern.compile(
            "^\\s*-\\s*\\[\\s*\\]\\s+(.+?)\\s*📅\\s*(\\d{4}-\\d{2}-\\d{2})\\s*$"
    );

    // Fallback pattern without emoji - matches: - [ ] task text due:2026-02-09
    private static final Pattern TASK_PATTERN_ALT = Pattern.compile(
            "^\\s*-\\s*\\[\\s*\\]\\s+(.+?)\\s*(?:due:|📅)\\s*(\\d{4}-\\d{2}-\\d{2})\\s*$"
    );

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    /**
     * Scan folder recursively for .md files and extract tasks.
     */
    public static List<TaskItem> parseTasks(String folderPath, FilterConfig filter) {
        List<TaskItem> tasks = new ArrayList<>();
        File folder = new File(folderPath);

        if (!folder.exists() || !folder.isDirectory()) {
            return tasks;
        }

        scanFolder(folder, folder.getAbsolutePath(), tasks, filter);
        Collections.sort(tasks);
        return tasks;
    }

    private static void scanFolder(File folder, String basePath, List<TaskItem> tasks, FilterConfig filter) {
        File[] files = folder.listFiles();
        if (files == null) return;

        for (File file : files) {
            if (file.isDirectory()) {
                scanFolder(file, basePath, tasks, filter);
            } else if (file.getName().endsWith(".md")) {
                String relativePath = file.getAbsolutePath().substring(basePath.length());

                // Apply path filters
                if (!matchesPathFilter(relativePath, filter)) {
                    continue;
                }

                parseFile(file, relativePath, tasks, filter);
            }
        }
    }

    private static void parseFile(File file, String relativePath, List<TaskItem> tasks, FilterConfig filter) {
        LocalDate today = LocalDate.now();

        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = reader.readLine()) != null) {
                Matcher matcher = TASK_PATTERN.matcher(line);
                if (!matcher.matches()) {
                    matcher = TASK_PATTERN_ALT.matcher(line);
                }

                if (matcher.matches()) {
                    String taskText = matcher.group(1).trim();
                    String dateStr = matcher.group(2).trim();

                    // Parse and filter by date
                    try {
                        LocalDate taskDate = LocalDate.parse(dateStr, DATE_FORMAT);
                        if (taskDate.isAfter(today)) {
                            continue; // Skip future tasks
                        }
                    } catch (DateTimeParseException e) {
                        continue; // Skip unparseable dates
                    }

                    // Apply text filters
                    if (!matchesTextFilter(taskText, filter)) {
                        continue;
                    }

                    tasks.add(new TaskItem(taskText, dateStr, relativePath));
                }
            }
        } catch (IOException e) {
            // Skip unreadable files
        }
    }

    /**
     * Check if task text passes include/exclude text filters.
     */
    private static boolean matchesTextFilter(String taskText, FilterConfig filter) {
        String lowerText = taskText.toLowerCase();

        // Include filter: if set, task must contain at least one of the include terms
        List<String> includeTexts = filter.getIncludeTexts();
        if (includeTexts != null && !includeTexts.isEmpty()) {
            boolean found = false;
            for (String inc : includeTexts) {
                if (lowerText.contains(inc.toLowerCase().trim())) {
                    found = true;
                    break;
                }
            }
            if (!found) return false;
        }

        // Exclude filter: if set, task must NOT contain any of the exclude terms
        List<String> excludeTexts = filter.getExcludeTexts();
        if (excludeTexts != null && !excludeTexts.isEmpty()) {
            for (String exc : excludeTexts) {
                if (lowerText.contains(exc.toLowerCase().trim())) {
                    return false;
                }
            }
        }

        return true;
    }

    /**
     * Check if file path passes include/exclude path filters.
     */
    private static boolean matchesPathFilter(String filePath, FilterConfig filter) {
        String lowerPath = filePath.toLowerCase();

        // Include path filter
        List<String> includePaths = filter.getIncludePaths();
        if (includePaths != null && !includePaths.isEmpty()) {
            boolean found = false;
            for (String inc : includePaths) {
                if (lowerPath.contains(inc.toLowerCase().trim())) {
                    found = true;
                    break;
                }
            }
            if (!found) return false;
        }

        // Exclude path filter
        List<String> excludePaths = filter.getExcludePaths();
        if (excludePaths != null && !excludePaths.isEmpty()) {
            for (String exc : excludePaths) {
                if (lowerPath.contains(exc.toLowerCase().trim())) {
                    return false;
                }
            }
        }

        return true;
    }
}
