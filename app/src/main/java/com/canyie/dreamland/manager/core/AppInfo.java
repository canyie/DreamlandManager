package com.canyie.dreamland.manager.core;

import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;

/**
 * @author canyie
 * @date 2019/12/17.
 */
public final class AppInfo {
    public String name;
    public String packageName;
    public Drawable icon;
    public boolean enabled;
    private AppManager am;

    AppInfo (PackageManager pm, AppManager am, ApplicationInfo appInfo) {
        name = appInfo.loadLabel(pm).toString();
        packageName = appInfo.packageName;
        icon = appInfo.loadIcon(pm);
        this.am = am;
        enabled = am.isEnabled(packageName);
    }

    public void setEnabled(boolean enable) {
        if (enable == enabled) return;
        enabled = enable;
        am.setAppEnabled(packageName, enable);
    }
}
