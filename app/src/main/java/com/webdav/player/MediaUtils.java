package com.webdav.player;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class MediaUtils {

    private static final Set<String> AUDIO_EXT = new HashSet<String>(Arrays.asList(
            "mp3", "wav", "ogg", "flac", "aac", "m4a", "wma", "ape", "opus"
    ));

    private static final Set<String> VIDEO_EXT = new HashSet<String>(Arrays.asList(
            "mp4", "mkv", "avi", "webm", "3gp", "mov", "wmv", "flv", "m4v"
    ));

    public static String getExtension(String filename) {
        if (filename == null) return "";
        int dot = filename.lastIndexOf('.');
        if (dot < 0 || dot >= filename.length() - 1) return "";
        return filename.substring(dot + 1).toLowerCase();
    }

    public static boolean isAudio(String filename) {
        return AUDIO_EXT.contains(getExtension(filename));
    }

    public static boolean isVideo(String filename) {
        return VIDEO_EXT.contains(getExtension(filename));
    }

    public static boolean isMedia(String filename) {
        return isAudio(filename) || isVideo(filename);
    }

    public static boolean isAudioOrVideo(String filename) {
        return isMedia(filename);
    }

    public static String formatSize(long bytes) {
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1024 * 1024) return String.format("%.1f KB", bytes / 1024.0);
        if (bytes < 1024 * 1024 * 1024) return String.format("%.1f MB", bytes / (1024.0 * 1024));
        return String.format("%.2f GB", bytes / (1024.0 * 1024 * 1024));
    }

    public static String formatTime(long ms) {
        long totalSec = ms / 1000;
        long min = totalSec / 60;
        long sec = totalSec % 60;
        return String.format("%02d:%02d", min, sec);
    }
}
