package top.canyie.dreamland.manager.core;

import android.content.pm.ApplicationInfo;
import android.graphics.drawable.Drawable;

import androidx.annotation.Keep;

import java.util.Comparator;

/**
 * @author canyie
 */
@Keep public final class ModuleInfo {
    public static final Comparator<ModuleInfo> COMPARATOR = (a, b) -> a.enabled == b.enabled
            ? a.name.compareTo(b.name) : a.enabled ? -1 : 1;

    public String name;
    public String packageName;
    public String description;
    public String version;
    public Drawable icon;
    public int flags;
    public boolean supported;
    public boolean enabled;

    ModuleInfo(String packageName, boolean enabled) {
        this.packageName = packageName;
        this.enabled = enabled;
    }

    public void setEnabled(boolean enable) {
        if (enable == enabled) return;
        enabled = enable;
        Dreamland.setModuleEnabled(packageName, enable);
    }

    public boolean isInstalledOnExternalStorage() {
        return (flags & ApplicationInfo.FLAG_EXTERNAL_STORAGE) != 0;
    }
}
