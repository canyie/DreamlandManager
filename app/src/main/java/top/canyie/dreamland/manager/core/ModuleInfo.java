package top.canyie.dreamland.manager.core;

import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.util.Log;

import androidx.annotation.Keep;
import androidx.core.util.Pair;

import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Set;

import top.canyie.dreamland.manager.utils.LazyInit;

/**
 * @author canyie
 */
@Keep public final class ModuleInfo {
    private static final String XPOSED_MODULE = "xposedmodule";
    private static final String XPOSED_MIN_VERSION = "xposedminversion";
    private static final String XPOSED_MODULE_DESCRIPTION = "xposeddescription";
    private static final String XPOSED_DEFAULT_SCOPE = "xposedscope"; // Added by LSPosed
    private static final String DREAMLAND_MODULE_SUPPORTED = "dreamland-supported";
    private static final String DREAMLAND_MODULE_MIN_VERSION = "dreamland-min-version";
    public static final Comparator<ModuleInfo> COMPARATOR = (a, b) -> a.enabled == b.enabled
            ? a.name.compareTo(b.name) : a.enabled ? -1 : 1;

    public static boolean isModule(PackageInfo pi) {
        return pi.applicationInfo.metaData.containsKey(XPOSED_MIN_VERSION);
    }

    public static boolean badModule(PackageInfo pi) {
        // Official doc requires `xposedmodule`=`true` but in the previous implementation there was
        // no judgment on what the value was, even if `false`.
        // But, if a module not strictly following the doc, we think its quality is questionable.

        Object isModule = pi.applicationInfo.metaData.get(XPOSED_MODULE);
        if (!(isModule instanceof Boolean)) return true;
        return !(Boolean) isModule;
    }

    public String name;
    public String packageName;
    public String description;
    public String version;
    public Drawable icon;
    public int flags;
    public boolean supported;
    public boolean enabled;
    public String[] defaultScope;
    public boolean maybeLowQuality;

    ModuleInfo(String packageName, boolean enabled) {
        this.packageName = packageName;
        this.enabled = enabled;
    }

    ModuleInfo(PackageInfo pi, PackageManager pm, boolean enabled) {
        packageName = pi.packageName;
        this.enabled = enabled;
        name = pi.applicationInfo.loadLabel(pm).toString();
        LazyInit<Resources, Void, PackageManager.NameNotFoundException> res = new LazyInit<>(
                arg -> pm.getResourcesForApplication(packageName));

        supported = pi.applicationInfo.metaData.getBoolean(DREAMLAND_MODULE_SUPPORTED, true);
        version = pi.versionName;
        icon = pi.applicationInfo.loadIcon(pm);
        flags = pi.applicationInfo.flags;

        {
            Object rawDescription = pi.applicationInfo.metaData.get(XPOSED_MODULE_DESCRIPTION);
            String tmp = null;
            if (rawDescription instanceof CharSequence) {
                tmp = rawDescription.toString().trim();
            } else if (rawDescription instanceof Integer) {
                int resId = (int) rawDescription;
                try {
                    tmp = res.get(null).getString(resId).trim();
                } catch (Exception e) {
                    Log.e(Dreamland.TAG, "Failed to parse description resource 0x"
                            + Integer.toHexString(resId) + " for module " + name, e);
                }
            }
            description = tmp != null ? tmp : "";
        }

        {
            int defScopeResId = pi.applicationInfo.metaData.getInt(XPOSED_DEFAULT_SCOPE, 0);
            if (defScopeResId != 0) {
                try {
                    defaultScope = res.get(null).getStringArray(defScopeResId);
                } catch (Exception e) {
                    Log.e(Dreamland.TAG, "Failed to parse default scope resource 0x"
                            + Integer.toHexString(defScopeResId) + " for module " + name, e);
                }
            } else {
                String scopeListString = pi.applicationInfo.metaData.getString(XPOSED_DEFAULT_SCOPE);
                if (scopeListString != null)
                    defaultScope = scopeListString.split(";");
            }
        }

        maybeLowQuality = badModule(pi);
    }

    public void setEnabled(boolean enable) {
        if (enable == enabled) return;
        enabled = enable;
        Dreamland.setModuleEnabled(packageName, enable);
        if (enable && defaultScope != null && Dreamland.getEnabledAppsFor(packageName) == null) {
            Log.i(Dreamland.TAG, "Auto applying default scope config for module " + packageName);
            Set<String> set = getDefaultScopeSet();
            Dreamland.setEnabledAppsFor(packageName, set.toArray(new String[set.size()]));
        }
    }

    public boolean isInstalledOnExternalStorage() {
        return (flags & ApplicationInfo.FLAG_EXTERNAL_STORAGE) != 0;
    }

    public Set<String> getDefaultScopeSet() {
        if (defaultScope == null) return null;
        Set<String> set = new HashSet<>();
        Collections.addAll(set, defaultScope);
        set.add(packageName);
        return set;
    }
}
