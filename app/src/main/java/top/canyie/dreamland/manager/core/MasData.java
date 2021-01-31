package top.canyie.dreamland.manager.core;

import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;

import java.util.Comparator;
import java.util.List;

/**
 * @author canyie
 */
public final class MasData {
    public boolean enabled;
    public List<AI> apps;

    public static class AI extends AppInfo {
        public static final Comparator<AI> COMPARATOR = (a, b) -> {
            if (a.enabled != b.enabled)
                return a.enabled ? -1 : 1;
            if (a.required != b.required)
                return a.required ? -1 : 1;
            return a.name.compareTo(b.name);
        };

        /** true if this app is required by module */
        public boolean required;
        AI (PackageManager pm, ApplicationInfo appInfo, boolean enabled) {
            super(pm, appInfo, enabled);
        }
    }
}
