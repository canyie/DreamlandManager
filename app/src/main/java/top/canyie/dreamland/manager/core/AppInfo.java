package top.canyie.dreamland.manager.core;

import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;

import androidx.annotation.Keep;

import java.util.Comparator;

/**
 * @author canyie
 */
@Keep public class AppInfo {
    public static final Comparator<AppInfo> COMPARATOR = (a, b) -> {
        if (a.enabled != b.enabled)
            return a.enabled ? -1 : 1;
        if ("android".equals(a.packageName)) {
            return "android".equals(b.packageName) ? 0 : -1;
        } else if ("android".equals(b.packageName)) {
            return 1;
        }
        return a.name.compareTo(b.name);
    };

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
