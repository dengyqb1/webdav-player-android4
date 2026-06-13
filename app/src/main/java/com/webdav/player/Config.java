package com.webdav.player;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;

public class Config {

    private static final String PREFS_NAME = "webdav_player_prefs";

    private static final String KEY_URL = "server_url";
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
