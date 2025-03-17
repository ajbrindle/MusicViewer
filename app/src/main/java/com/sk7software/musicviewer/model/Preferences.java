package com.sk7software.musicviewer.model;

import android.content.Context;
import android.content.SharedPreferences;

import com.sk7software.musicviewer.ApplicationContextProvider;

public class Preferences {
    public static final String APP_PREFERENCES_KEY = "SK7_MUSIC_VIEW_PREFS";
    public static final String LINE_WIDTH = "PREF_LINE_WIDTH";
    public static final String LINE_TRANSPARENCY = "PREF_LINE_TRANSPARENCY";
    public static final String LINE_COLOUR = "PREF_LINE_COLOR";
    private static Preferences instance;
    private final SharedPreferences prefs;

    private Preferences(Context context) {
        prefs = context.getSharedPreferences(APP_PREFERENCES_KEY, Context.MODE_PRIVATE);
    }

    public static synchronized void init(Context context) {
        if (instance == null) {
            instance = new Preferences(context);
        }
    }

    public static Preferences getInstance() {
        if (instance == null) {
            init(ApplicationContextProvider.getContext());
        }
        return instance;
    }

    public void addPreference(String name, String value) {
        prefs.edit().putString(name, value).commit();
    }

    public void addPreference(String name, int value) {
        prefs.edit().putInt(name, value).commit();
    }
    public void addPreference(String name, long value) {
        prefs.edit().putLong(name, value).commit();
    }

    public void addPreference(String name, boolean value) {
        prefs.edit().putBoolean(name, value).commit();
    }

    public String getStringPreference(String name) {
        return prefs.getString(name, "");
    }

    public int getIntPreference(String name) {
        return getIntPreference(name, 0);
    }

    public int getIntPreference(String name, int defVal) {
        return prefs.getInt(name, defVal);
    }
    public long getLongPreference(String name, long defVal) { return prefs.getLong(name, defVal); }
    public void clearAllPreferences() {
        prefs.edit().clear().commit();
    }

    public static void reset() {
        instance = null;
    }

    public boolean getBooleanPreference(String name) {
        return prefs.getBoolean(name, false);
    }

    public boolean getBooleanPreference(String name, boolean defVal) {
        return prefs.getBoolean(name, defVal);
    }

    public void clearStringPreference(String name) {
        prefs.edit().putString(name, "").commit();
    }

    public void clearIntPreference(String name) {
        prefs.edit().putInt(name, -1).commit();
    }
}
