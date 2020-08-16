package top.canyie.dreamland.manager.core;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.Log;

import androidx.annotation.Keep;

import top.canyie.dreamland.ipc.IDreamlandManager;

import top.canyie.dreamland.manager.AppGlobals;
import top.canyie.dreamland.manager.BuildConfig;
import top.canyie.dreamland.manager.core.installation.Installer;
import top.canyie.dreamland.manager.utils.DeviceUtils;
import top.canyie.dreamland.manager.utils.FileUtils;
import top.canyie.dreamland.manager.utils.RuntimeHelper;
import top.canyie.dreamland.manager.utils.Threads;

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

            String packageName = packageInfo.packageName;
            ModuleInfo module = new ModuleInfo(packageName, service != null && isModuleEnabled(packageName));
            module.name = packageInfo.applicationInfo.loadLabel(pm).toString();
            {
                Object rawDescription = packageInfo.applicationInfo.metaData.get(XPOSED_MODULE_DESCRIPTION);
                String tmp = null;
                if (rawDescription instanceof CharSequence) {
                    tmp = rawDescription.toString().trim();
                } else if (rawDescription instanceof Integer) {
                    int resId = (int) rawDescription;
                    try {
                        tmp = pm.getResourcesForApplication(packageName).getString(resId).trim();
                    } catch (Exception e) {
                        Log.e(TAG, "Failed to parse description resource 0x"
                                + Integer.toHexString(resId) + " for module " + module.name, e);
                    }
                }
                module.description = tmp != null ? tmp : "";
            }
            module.supported = packageInfo.applicationInfo.metaData.getBoolean(DREAMLAND_MODULE_SUPPORTED, true);
            module.version = packageInfo.versionName;
            module.icon = packageInfo.applicationInfo.loadIcon(pm);
            module.flags = packageInfo.applicationInfo.flags;
            modules.add(module);
        }
        modules.sort(ModuleInfo.COMPARATOR);
        return modules;
    }

    public static MasData getMasDataFor(String module) throws InterruptedException {
        MasData data = new MasData();
        String[] remoteData = getEnabledAppsFor(module);
        Set<String> enabledFor;
        if (remoteData != null) {
            data.enabled = true;
            enabledFor = new HashSet<>(remoteData.length);
            Collections.addAll(enabledFor, remoteData);
        } else {
            data.enabled = false;
            enabledFor = Collections.emptySet();
        }
        Threads.throwIfInterrupted();
        PackageManager pm = AppGlobals.getApp().getPackageManager();
        List<ApplicationInfo> rawAppInfos = pm.getInstalledApplications(0);
        List<AppInfo> appInfos = new ArrayList<>(rawAppInfos.size());
        for (ApplicationInfo applicationInfo : rawAppInfos) {
            if (MANAGER_PACKAGE_NAME.equals(applicationInfo.packageName)) continue;
            appInfos.add(new AppInfo(pm, applicationInfo, enabledFor.contains(applicationInfo.packageName)));
        }
        appInfos.sort(AppInfo.COMPARATOR);
        Threads.throwIfInterrupted();
        data.apps = appInfos;
        return data;
    }

    public static void setMasDataFor(String module, MasData data) {
        String[] remoteData;
        if (data.enabled) {
            Set<String> set = new HashSet<>(Math.min(4, data.apps.size() / 100));
            for (AppInfo appInfo : data.apps) {
                if (appInfo.enabled) set.add(appInfo.packageName);
            }
            remoteData = set.toArray(new String[set.size()]);
        } else {
            remoteData = null;
        }
        setEnabledAppsFor(module, remoteData);
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

    public static boolean isResourcesHookEnabled() {
        try {
            return service.isResourcesHookEnabled();
        } catch (RemoteException e) {
            throw new RuntimeException("Failure from remote service", e);
        }
    }

    public static void setResourcesHookEnabled(boolean enabled) {
        try {
            service.setResourcesHookEnabled(enabled);
        } catch (RemoteException e) {
            throw new RuntimeException("Failure from remote service", e);
        }
    }

    public static boolean isGlobalMode() {
        try {
            return service.isGlobalModeEnabled();
        } catch (RemoteException e) {
            throw new RuntimeException("Failure from remote service", e);
        }
    }

    public static void setGlobalModeEnabled(boolean enabled) {
        try {
            service.setGlobalModeEnabled(enabled);
        } catch (RemoteException e) {
            throw new RuntimeException("Failure from remote service", e);
        }
    }

    public static String[] getEnabledAppsFor(String module) {
        try {
            return service.getEnabledAppsFor(module);
        } catch (RemoteException e) {
            throw new RuntimeException("Failure from remote service", e);
        }
    }

    public static void setEnabledAppsFor(String module, String[] apps) {
        try {
            service.setEnabledAppsFor(module, apps);
        } catch (RemoteException e) {
            throw new RuntimeException("Failure from remote service", e);
        }
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
