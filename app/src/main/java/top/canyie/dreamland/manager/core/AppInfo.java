package top.canyie.dreamland.manager.core;

import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;

import androidx.annotation.Keep;
import androidx.annotation.NonNull;

import java.util.Comparator;

import top.canyie.dreamland.manager.AppGlobals;

/**
 * @author canyie
 */
@Keep public class AppInfo {
    public static final Comparator<AppInfo> COMPARATOR = (a, b) -> a.enabled == b.enabled
            ? a.name.compareTo(b.name) : a.enabled ? -1 : 1;

    public String name;
    public String packageName;
    public Drawable icon;
    public boolean enabled;

    AppInfo (PackageManager pm, ApplicationInfo appInfo, boolean enabled) {
        name = appInfo.loadLabel(pm).toString();
        packageName = appInfo.packageName;
        icon = appInfo.loadIcon(pm);
        this.enabled = enabled;
    }

    public void setEnabled(boolean enable) {
        if (enable == enabled) return;
        enabled = enable;
        Dreamland.setAppEnabled(packageName, enable);
    }

}
