package com.taskwidget.app;

/**
 * Represents a single task parsed from a markdown file.
 */
public class TaskItem implements Comparable<TaskItem> {
    private String text;
    private String date;
    private String filePath;
    private String fileName;
    private int priority; // 0=urgent, 1=highest(🔺), 2=high(🔼), 3=normal, 4=low(🔽), 5=lowest(⏬)
    private String priorityEmoji;

    public TaskItem(String text, String date, String filePath) {
        this.text = text;
        this.date = date;
        this.filePath = filePath;
        this.priority = parsePriority(text);
        this.priorityEmoji = getPriorityEmoji(this.priority);
        // Extract just the file name from the full path
        if (filePath != null && filePath.contains("/")) {
            this.fileName = filePath.substring(filePath.lastIndexOf("/") + 1);
        } else {
            this.fileName = filePath;
        }
    }

    private static int parsePriority(String text) {
        if (text == null) return 3;
        if (text.contains("#urgent")) return 0;
        if (text.contains("\uD83D\uDD3A")) return 1; // 🔺
        if (text.contains("\uD83D\uDD3C")) return 2; // 🔼
        if (text.contains("\uD83D\uDD3D")) return 4; // 🔽
        if (text.contains("\u23EC"))       return 5; // ⏬
        return 3; // normal
    }

    private static String getPriorityEmoji(int priority) {
        switch (priority) {
            case 0: return "\uD83D\uDD3A"; // 🔺 for #urgent
            case 1: return "\uD83D\uDD3A"; // 🔺
            case 2: return "\uD83D\uDD3C"; // 🔼
            case 4: return "\uD83D\uDD3D"; // 🔽
            case 5: return "\u23EC";        // ⏬
            default: return "";
        }
    }

    public String getText() { return text; }
    public String getDate() { return date; }
    public String getFilePath() { return filePath; }
    public String getFileName() { return fileName; }
    public int getPriority() { return priority; }
    public String getPriorityEmoji() { return priorityEmoji; }

    @Override
    public int compareTo(TaskItem other) {
        // Sort by priority first (lower number = higher priority)
        int priComp = Integer.compare(this.priority, other.priority);
        if (priComp != 0) return priComp;
        // Then by date ascending (oldest/most overdue first)
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
