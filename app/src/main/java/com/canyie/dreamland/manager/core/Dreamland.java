package com.canyie.dreamland.manager.core;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.os.Build;

import androidx.annotation.Keep;

import com.canyie.dreamland.manager.BuildConfig;
import com.canyie.dreamland.manager.core.installation.Installer;
import com.canyie.dreamland.manager.utils.DLog;
import com.canyie.dreamland.manager.utils.DeviceUtils;
import com.canyie.dreamland.manager.utils.FileUtils;
import com.canyie.dreamland.manager.utils.Preconditions;
import com.canyie.dreamland.manager.utils.Processes;
import com.canyie.dreamland.manager.utils.RuntimeHelper;
import com.canyie.dreamland.manager.utils.reflect.Reflection;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * @author canyie
 */
public final class Dreamland {
    private static final String TAG = "Dreamland";
    static final File BASE_DIR = new File("/data/dreamland/");
    public static final File CORE_JAR_FILE = new File("/system/framework/dreamland.jar");
    private static final File DISABLE_FLAG_FILE = new File(BASE_DIR, "disable");
    private static final String MANAGER_PACKAGE_NAME = BuildConfig.APPLICATION_ID;
    private static final String XPOSED_MODULE = "xposedmodule";
    private static final String XPOSED_MODULE_DESCRIPTION = "xposeddescription";
    private static final String DREAMLAND_MODULE_SUPPORTED = "dreamland-supported";
    private static final String DREAMLAND_MODULE_MIN_VERSION = "dreamland-min-version";

    private static ModuleManager sModuleManager;
    private static AppManager sAppManager;
    private static Reflection.MethodWrapper getVersionInternal = Reflection.on(Dreamland.class).method("getVersionInternal");
    private static boolean isIPCServiceRunning;

    private Dreamland() {}

    public static void init() {
        sModuleManager = new ModuleManager();
        sModuleManager.startLoad();
        sAppManager = new AppManager();
        sAppManager.startLoad();
    }

    public static boolean isActive() {
        return getVersion() > 0;
    }

    public static boolean isDisabled() {
        return DISABLE_FLAG_FILE.exists();
    }

    public static boolean setEnabled(boolean enable) {
        if (enable) {
            return DISABLE_FLAG_FILE.delete();
        } else {
            try {
                return DISABLE_FLAG_FILE.createNewFile();
            } catch (IOException e) {
                DLog.e(TAG, "Failed to create disable flag file", e);
                return false;
            }
        }
    }

    public static boolean isInstalled() {
        String soPath;
        if (DeviceUtils.is64Bits()) {
            soPath = "/system/lib64/libdreamland.so";
        } else {
            soPath = "/system/lib/libdreamland.so";
        }
        return FileUtils.isExisting(soPath) || CORE_JAR_FILE.exists();
    }

    public static boolean isCompleteInstalled() {
        String soPath;
        if (DeviceUtils.is64Bits()) {
            soPath = "/system/lib64/libdreamland.so";
        } else {
            soPath = "/system/lib/libdreamland.so";
        }
        if (!FileUtils.isExisting(soPath)) {
            return false;
        }

        if (!CORE_JAR_FILE.exists()) {
            return false;
        }

        final boolean[] out = new boolean[] {false};
        try {
            FileUtils.readAllLines("/system/etc/public.libraries.txt", line -> {
                if ("libdreamland.so".equalsIgnoreCase(line.trim())) {
                    out[0] = true;
                    return true;
                }
                return false;
            });
        } catch (FileNotFoundException e) {
            DLog.w(TAG, "/system/etc/public.libraries.txt not found", e);
            return false;
        } catch (IOException e) {
            DLog.w(TAG, "Failed to read /system/etc/public.libraries.txt", e);
        }
        return out[0];
    }

    public static boolean isSupported() {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.N
                && DeviceUtils.isArmV7OrArm64()
                && RuntimeHelper.isArt();
    }

    public static List<AppInfo> getAppInfos(Context context) {
        PackageManager pm = context.getPackageManager();
        List<ApplicationInfo> rawAppInfos = pm.getInstalledApplications(0);
        List<AppInfo> appInfos = new ArrayList<>(rawAppInfos.size());
        AppManager manager = getAppManager();
        for (ApplicationInfo applicationInfo : rawAppInfos) {
            if (applicationInfo.uid == Processes.UID_SYSTEM) continue; // Can't hook java methods in the process now...
            if (MANAGER_PACKAGE_NAME.equals(applicationInfo.packageName)) continue;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P
                    && applicationInfo.targetSdkVersion >= Build.VERSION_CODES.P) {
                // In Android 9, if the app's targetSdkVersion >= P, it running in a
                // different SELinux sandbox (has a different SELinux context), so it
                // can't read our properties.
                // See https://source.android.com/security/app-sandbox or
                // https://source.android.google.cn/security/app-sandbox (For China user)
                continue;
            }
            appInfos.add(new AppInfo(pm, manager, applicationInfo));
        }
        return appInfos;
    }

    public static List<ModuleInfo> getModuleInfos(Context context) {
        PackageManager pm = context.getPackageManager();
        List<PackageInfo> appInfos = pm.getInstalledPackages(PackageManager.GET_META_DATA);
        List<ModuleInfo> modules = new ArrayList<>();
        ModuleManager manager = getModuleManager();
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

            ModuleInfo module = new ModuleInfo(packageName, manager);
            module.name = name.toString();
            module.description = description;
            module.path = packageInfo.applicationInfo.sourceDir;
            module.version = packageInfo.versionName;
            module.icon = icon;
            module.supported = supportDreamland;
            modules.add(module);
        }
        return modules;
    }

    public static ModuleManager getModuleManager() {
        Preconditions.checkState(sModuleManager != null, "Please call init() first.");
        return sModuleManager;
    }

    public static AppManager getAppManager() {
        Preconditions.checkState(sAppManager != null, "Please call init() first.");
        return sAppManager;
    }

    public static Installer getInstaller() {
        return IH.INSTALLER;
    }

    public static int getVersion() {
        return getVersionInternal.callStatic();
    }

    public static void onIPCServiceDestroy() {
        isIPCServiceRunning = false;
    }

    public static boolean isIPCServiceRunning() {
        return isIPCServiceRunning;
    }

    @Keep private static int getVersionInternal() {
        return -1;
    }

    public static void onIPCServiceStart() {
        isIPCServiceRunning = true;
    }
    private static final class IH {
        static final Installer INSTALLER = new Installer();
    }
}
