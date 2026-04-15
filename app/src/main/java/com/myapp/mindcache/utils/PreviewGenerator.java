package com.myapp.mindcache.utils;

public class PreviewGenerator {
    private PreviewGenerator() { }

    private static final int MAX_PREVIEW_LENGTH = 100;
    private static final String ELLIPSIS = "…";

    public static String getTitle(String content) {
        int newLineIndex = content.indexOf('\n');
        if (newLineIndex != -1) {
            return content.substring(0, newLineIndex);
        }
        return content;
    }

    public static String getPreview(String text) {
        if (text == null || text.isBlank())
            return "";

        String content = text;
        int newLineIndex = text.indexOf('\n');
        if (newLineIndex != -1) {
            content = text.substring(newLineIndex + 1).trim();
        }

        if (content.isBlank())
            return "";

        return content.length() <= MAX_PREVIEW_LENGTH
                ? content
                : content.substring(0, MAX_PREVIEW_LENGTH) + ELLIPSIS;
    }

}
