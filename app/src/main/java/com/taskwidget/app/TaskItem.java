package com.taskwidget.app;

/**
 * Represents a single task parsed from a markdown file.
 */
public class TaskItem implements Comparable<TaskItem> {
    private String text;
    private String date;
    private String filePath;
    private String fileName;

    public TaskItem(String text, String date, String filePath) {
        this.text = text;
        this.date = date;
        this.filePath = filePath;
        // Extract just the file name from the full path
        if (filePath != null && filePath.contains("/")) {
            this.fileName = filePath.substring(filePath.lastIndexOf("/") + 1);
        } else {
            this.fileName = filePath;
        }
    }

    public String getText() { return text; }
    public String getDate() { return date; }
    public String getFilePath() { return filePath; }
    public String getFileName() { return fileName; }

    @Override
    public int compareTo(TaskItem other) {
        // Sort by date ascending (oldest/most overdue first)
        if (this.date == null && other.date == null) return 0;
        if (this.date == null) return 1;
        if (other.date == null) return -1;
        return this.date.compareTo(other.date);
    }

    @Override
    public String toString() {
        return text + " 📅 " + date + " [" + fileName + "]";
    }
}
