package com.github.javiersantos.appupdater;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

class LibraryPreferences {
    private SharedPreferences sharedPreferences;
    private SharedPreferences.Editor editor;

    static final String KeySuccessfulChecks = "prefSuccessfulChecks";

    public LibraryPreferences(Context context) {
        this.sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        this.editor = sharedPreferences.edit();
    }

    public Boolean getAppUpdaterShow() {
        return sharedPreferences.getBoolean(AppUpdater.PREF_CHECK_UPDATES, true);
    }

    public void setAppUpdaterShow(Boolean res) {
        editor.putBoolean(AppUpdater.PREF_CHECK_UPDATES, res);
        editor.commit();
    }

    public Integer getSuccessfulChecks() {
        return sharedPreferences.getInt(KeySuccessfulChecks, 0);
    }

    public void setSuccessfulChecks(Integer checks) {
        editor.putInt(KeySuccessfulChecks, checks);
        editor.commit();
    }

}
