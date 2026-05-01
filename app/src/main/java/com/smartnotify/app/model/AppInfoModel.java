package com.smartnotify.app.model;

import android.graphics.drawable.Drawable;

public class AppInfoModel {

    private final String packageName;
    private final String appName;
    private final Drawable appIcon;

    public AppInfoModel(String packageName, String appName, Drawable appIcon) {
        this.packageName = packageName;
        this.appName = appName;
        this.appIcon = appIcon;
    }

    public String getPackageName() {
        return packageName;
    }

    public String getAppName() {
        return appName;
    }

    public Drawable getAppIcon() {
        return appIcon;
    }
}