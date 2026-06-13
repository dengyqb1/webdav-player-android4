package com.webdav.player;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import java.net.URI;

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
     * Build full URL with port, using Java URI for safe parsing.
     * - Adds https:// if no protocol
     * - Uses separate port field if set
     * - Falls back to URL's embedded port if no separate port
     */
    public String getFullUrl() {
        String raw = getServerUrl();
        if (raw == null || raw.trim().isEmpty()) return "";

        raw = raw.trim();
        int port = getPort();

        // Step 1: remove any duplicate protocol prefix
        while (raw.startsWith("https://https://") || raw.startsWith("http://http://")) {
            raw = raw.substring(raw.indexOf("://") + 3);
        }

        // Step 2: add protocol if missing
        if (!raw.startsWith("http://") && !raw.startsWith("https://")) {
            raw = "https://" + raw;
        }

        try {
            URI uri = new URI(raw);
            String scheme = uri.getScheme();
            String host = uri.getHost();

            if (host == null || host.isEmpty()) {
                // Can't parse host, return as-is
                if (!raw.endsWith("/")) raw = raw + "/";
                return raw;
            }

            int embeddedPort = uri.getPort();
            String path = uri.getPath();

            // Separate port field takes priority, otherwise use embedded
            int finalPort = (port > 0) ? port : embeddedPort;

            StringBuilder sb = new StringBuilder();
            sb.append(scheme).append("://").append(host);
            if (finalPort > 0) {
                sb.append(":").append(finalPort);
            }
            sb.append(path == null || path.isEmpty() ? "/" : path);

            return sb.toString();

        } catch (Exception e) {
            // Fallback: just ensure trailing slash
            if (!raw.endsWith("/")) raw = raw + "/";
            return raw;
        }
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