package com.webdav.player;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;

public class Config {

    private static final String PREFS_NAME = "webdav_player_prefs";

    private static final String KEY_URL = "server_url";
    private static final String KEY_PORT = "server_port";
    private static final String KEY_USER = "username";
    private static final String KEY_PASS = "password";

    private final SharedPreferences prefs;

    public Config(Context context) {
        prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    public String getServerUrl() {
        return prefs.getString(KEY_URL, "");
    }

    public void setServerUrl(String url) {
        Editor e = prefs.edit();
        e.putString(KEY_URL, url);
        e.apply();
    }

    public int getPort() {
        return prefs.getInt(KEY_PORT, 0);
    }

    public void setPort(int port) {
        Editor e = prefs.edit();
        e.putInt(KEY_PORT, port);
        e.apply();
    }

    /**
     * Build full URL with port.
     * e.g. https://dav.example.com + 8080 = https://dav.example.com:8080/
     */
    public String getFullUrl() {
        String url = getServerUrl();
        if (url.isEmpty()) return "";
        int port = getPort();
        if (port > 0 && port != 443) {
            // Insert port into URL
            if (!url.endsWith("/")) url = url + "/";
            // Remove trailing parts to insert port properly
            int slashIdx = url.indexOf("//");
            if (slashIdx >= 0) {
                int pathIdx = url.indexOf("/", slashIdx + 3);
                String hostPart = pathIdx > 0 ? url.substring(0, pathIdx) : url;
                return hostPart + ":" + port + (pathIdx > 0 ? url.substring(pathIdx) : "/");
            }
        }
        return url;
    }

    public String getUsername() {
        return prefs.getString(KEY_USER, "");
    }

    public void setUsername(String user) {
        Editor e = prefs.edit();
        e.putString(KEY_USER, user);
        e.apply();
    }

    public String getPassword() {
        return prefs.getString(KEY_PASS, "");
    }

    public void setPassword(String pass) {
        Editor e = prefs.edit();
        e.putString(KEY_PASS, pass);
        e.apply();
    }

    public boolean isConfigured() {
        String url = getServerUrl();
        return url != null && url.length() > 0;
    }
}
