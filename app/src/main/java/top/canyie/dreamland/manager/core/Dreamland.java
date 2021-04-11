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
import top.canyie.dreamland.manager.utils.DeviceUtils;
import top.canyie.dreamland.manager.utils.FileUtils;
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
    public static final String TAG = "DreamlandManager";
    private static final String SYSTEM_LIB_DIR = "/system/lib/";
    private static final String SYSTEM_LIB64_DIR = "/system/lib64/";
    private static final String CORE_LIB_NAME = "libriru_dreamland.so";
    public static final File CORE_JAR_FILE = new File("/system/framework/dreamland.jar");
    private static final String MANAGER_PACKAGE_NAME = BuildConfig.APPLICATION_ID;
    private static String versionName;
    private static int version = -1;
    private static IDreamlandManager service;
    private static Set<String> sEnabledApps;
    private static Set<String> sEnabledModules;

    private Dreamland() {}

    public static boolean isActive() {
        return version > 0;
    }

    public static boolean isInstalled() {
        if (CORE_JAR_FILE.exists()) return true;
        String soPath = (DeviceUtils.is64Bits() ? SYSTEM_LIB64_DIR : SYSTEM_LIB_DIR) + CORE_LIB_NAME;
        return FileUtils.isExisting(soPath);
    }

    public static boolean isCompleteInstalled() {
        return CORE_JAR_FILE.exists();
    }

    public static boolean isSupported() {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.N
                && Build.VERSION.SDK_INT <= Build.VERSION_CODES.R
                && DeviceUtils.isArmV7OrArm64();
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

            if (!ModuleInfo.isModule(packageInfo)) {
                continue;
            }

            String packageName = packageInfo.packageName;
            boolean enabled = service != null && isModuleEnabled(packageName);
            ModuleInfo module = new ModuleInfo(packageInfo, pm, enabled);
            modules.add(module);
        }
        modules.sort(ModuleInfo.COMPARATOR);
        return modules;
    }

    public static MasData getMasDataFor(String module, String[] defScope) throws InterruptedException {
        MasData data = new MasData();
        String[] remoteData = getEnabledAppsFor(module);
        Set<String> enabledFor;
        Set<String> defaultScopeSet = null;
        if (defScope != null) {
            defaultScopeSet = new HashSet<>(defScope.length);
            Collections.addAll(defaultScopeSet, defScope);
        }
        if (remoteData != null) {
            data.enabled = true;
            enabledFor = new HashSet<>(remoteData.length);
            Collections.addAll(enabledFor, remoteData);
        } else if (defaultScopeSet != null) {
            data.enabled = true;
            enabledFor = defaultScopeSet;
            Log.i(TAG, "Auto applying default scope config for module " + module);
            setEnabledAppsFor(module, defScope);
        } else {
            data.enabled = false;
            enabledFor = Collections.emptySet();
        }
        Threads.throwIfInterrupted();
        PackageManager pm = AppGlobals.getApp().getPackageManager();
        List<ApplicationInfo> rawAppInfos = pm.getInstalledApplications(0);
        List<MasData.AI> appInfos = new ArrayList<>(rawAppInfos.size());
        for (ApplicationInfo applicationInfo : rawAppInfos) {
            if (MANAGER_PACKAGE_NAME.equals(applicationInfo.packageName)) continue;
            MasData.AI ai = new MasData.AI(pm, applicationInfo, enabledFor.contains(applicationInfo.packageName));
            ai.required = defaultScopeSet != null && defaultScopeSet.contains(applicationInfo.packageName);
            appInfos.add(ai);
        }
        appInfos.sort(MasData.AI.COMPARATOR);
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

    public static int getVersion() {
        return version;
    }

    public static String getVersionName() { return versionName; }

    public static boolean cannotHookSystemServer() {
        try {
            if (version < 2002) return false;
            return service.cannotHookSystemServer();
        } catch (RemoteException e) {
            throw new RuntimeException("Failure from remote service", e);
        }
    }

    /**
     * Called by Dreamland framework.
     */
    @Keep private static void init(String versionName, int versionCode, IBinder service) {
        Dreamland.versionName = versionName;
        Dreamland.version = versionCode;
        Dreamland.service = IDreamlandManager.Stub.asInterface(service);
    }

    /**
     * Called by Dreamland framework.
     * @deprecated This API only for Dreamland 2.0 beta.
     */
    @Keep @Deprecated private static void init(int version, IBinder service) {
        init("2.0 beta", version, service);
    }
}
