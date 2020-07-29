package top.canyie.dreamland.manager.core;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.IBinder;
import android.os.RemoteException;

import androidx.annotation.Keep;

import top.canyie.dreamland.ipc.IDreamlandManager;

import top.canyie.dreamland.manager.BuildConfig;
import top.canyie.dreamland.manager.core.installation.Installer;
import top.canyie.dreamland.manager.utils.DeviceUtils;
import top.canyie.dreamland.manager.utils.FileUtils;
import top.canyie.dreamland.manager.utils.RuntimeHelper;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * @author canyie
 */
public final class Dreamland {
    private static final String TAG = "Dreamland";
    private static final String SYSTEM_LIB_DIR = "/system/lib/";
    private static final String SYSTEM_LIB64_DIR = "/system/lib64/";
    private static final String CORE_LIB_NAME = "libriru_dreamland.so";
    public static final File CORE_JAR_FILE = new File("/system/framework/dreamland.jar");
    private static final String MANAGER_PACKAGE_NAME = BuildConfig.APPLICATION_ID;
    private static final String XPOSED_MODULE = "xposedmodule";
    private static final String XPOSED_MODULE_DESCRIPTION = "xposeddescription";
    private static final String DREAMLAND_MODULE_SUPPORTED = "dreamland-supported";
    private static final String DREAMLAND_MODULE_MIN_VERSION = "dreamland-min-version";
    private static int version = -1;
    private static IDreamlandManager service;
    private static Set<String> sEnabledApps;
    private static Set<String> sEnabledModules;

    private Dreamland() {}

    public static boolean isActive() {
        return version > 0;
    }

    public static boolean isInstalled() {
        String soPath = (DeviceUtils.is64Bits() ? SYSTEM_LIB64_DIR : SYSTEM_LIB_DIR) + CORE_LIB_NAME;
        return FileUtils.isExisting(soPath) || CORE_JAR_FILE.exists();
    }

    public static boolean isCompleteInstalled() {
        String soPath = (DeviceUtils.is64Bits() ? SYSTEM_LIB64_DIR : SYSTEM_LIB_DIR) + CORE_LIB_NAME;
        return FileUtils.isExisting(soPath) && CORE_JAR_FILE.exists();
    }

    public static boolean isSupported() {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.N
                && Build.VERSION.SDK_INT <= Build.VERSION_CODES.Q
                && DeviceUtils.isArmV7OrArm64()
                && RuntimeHelper.isArt();
    }

    public static boolean isSafeMode() {
        try {
            return service.isSafeModeEnabled();
        } catch (RemoteException e) {
            throw new RuntimeException("Failure from remote service", e);
        }
    }

    public static void setSafeMode(boolean enabled) {
        try {
            service.setSafeModeEnabled(enabled);
        } catch (RemoteException e) {
            throw new RuntimeException("Failure from remote service", e);
        }
    }

    public static List<AppInfo> getAppInfos(Context context) {
        PackageManager pm = context.getPackageManager();
        List<ApplicationInfo> rawAppInfos = pm.getInstalledApplications(0);
        List<AppInfo> appInfos = new ArrayList<>(rawAppInfos.size());
        for (ApplicationInfo applicationInfo : rawAppInfos) {
            if (MANAGER_PACKAGE_NAME.equals(applicationInfo.packageName)) continue;
            appInfos.add(new AppInfo(pm, applicationInfo, service != null && isAppEnabled(applicationInfo.packageName)));
        }
        appInfos.sort(AppInfo.COMPARATOR);
        return appInfos;
    }

    public static List<ModuleInfo> getModuleInfos(Context context) {
        PackageManager pm = context.getPackageManager();
        List<PackageInfo> appInfos = pm.getInstalledPackages(PackageManager.GET_META_DATA);
        List<ModuleInfo> modules = new ArrayList<>();
        for (PackageInfo packageInfo : appInfos) {
            if (packageInfo.applicationInfo.metaData == null) {
                continue;
            }

            boolean isXposedModule = packageInfo.applicationInfo.metaData.getBoolean(XPOSED_MODULE, false);

            if (!isXposedModule) {
                continue;
            }

            CharSequence name = packageInfo.applicationInfo.loadLabel(pm);
            String description = packageInfo.applicationInfo.metaData.getString(XPOSED_MODULE_DESCRIPTION, "");
            boolean supportDreamland = packageInfo.applicationInfo.metaData.getBoolean(DREAMLAND_MODULE_SUPPORTED, true);
            Drawable icon = packageInfo.applicationInfo.loadIcon(pm);
            String packageName = packageInfo.packageName;

            ModuleInfo module = new ModuleInfo(packageName, service != null && isModuleEnabled(packageName));
            module.name = name.toString();
            module.description = description;
            module.version = packageInfo.versionName;
            module.icon = icon;
            module.supported = supportDreamland;
            modules.add(module);
        }
        modules.sort(ModuleInfo.COMPARATOR);
        return modules;
    }

    public static boolean isAppEnabled(String packageName) {
        ensureEnabledAppDataLoaded();
        return sEnabledApps.contains(packageName);
    }

    public static void setAppEnabled(String packageName, boolean enabled) {
        try {
            service.setAppEnabled(packageName, enabled);
        } catch (RemoteException e) {
            throw new RuntimeException("Failure from remote service", e);
        }
        sEnabledApps = null;
    }

    public static void ensureEnabledAppDataLoaded() {
        if (sEnabledApps == null) {
            sEnabledApps = new HashSet<>();
            String[] remoteData;
            try {
                remoteData = service.getEnabledApps();
            } catch (RemoteException e) {
                throw new RuntimeException("Failure from remote service", e);
            }
            Collections.addAll(sEnabledApps, remoteData);
        }
    }

    public static boolean isModuleEnabled(String packageName) {
        ensureEnabledModuleDataLoaded();
        return sEnabledModules.contains(packageName);
    }

    public static void setModuleEnabled(String packageName, boolean enabled) {
        try {
            service.setModuleEnabled(packageName, enabled);
        } catch (RemoteException e) {
            throw new RuntimeException("Failure from remote service", e);
        }
        sEnabledModules = null;
    }

    public static void ensureEnabledModuleDataLoaded() {
        if (sEnabledModules == null) {
            sEnabledModules = new HashSet<>();
            String[] remoteData;
            try {
                remoteData = service.getAllEnabledModules();
            } catch (RemoteException e) {
                throw new RuntimeException("Failure from remote service", e);
            }
            Collections.addAll(sEnabledModules, remoteData);
        }
    }

    /**
     * Get instance for {@link Installer}
     * @deprecated Not supported install manually now, please use Magisk.
     * @return Never return
     */
    @Deprecated public static Installer getInstaller() {
        throw new UnsupportedOperationException("Unsupported");
    }

    public static int getVersion() {
        return version;
    }

    /**
     * Called by Dreamland framework.
     */
    @Keep private static void init(int version, IBinder service) {
        Dreamland.version = version;
        Dreamland.service = IDreamlandManager.Stub.asInterface(service);
    }
}
