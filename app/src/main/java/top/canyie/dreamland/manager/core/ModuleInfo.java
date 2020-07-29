package top.canyie.dreamland.manager.core;

import android.graphics.drawable.Drawable;

import androidx.annotation.Keep;

import java.util.Comparator;

/**
 * @author canyie
 */
@Keep public final class ModuleInfo {
    public static final Comparator<ModuleInfo> COMPARATOR = (a, b) -> a.name.compareTo(b.name);

    public String name;
    public String packageName;
    public String description;
    public String version;
    public Drawable icon;
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
}
