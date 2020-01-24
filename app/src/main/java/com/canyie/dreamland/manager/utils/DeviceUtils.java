package com.canyie.dreamland.manager.utils;

import android.os.Build;
import android.text.TextUtils;

import mirror.android.os.SystemProperties;

/**
 * @author canyie\
 */
public final class DeviceUtils {
    @SuppressWarnings("deprecation")
    public static final String CPU_ABI = Build.CPU_ABI;
    public static final String CPU_ARCH = getCPUArch(CPU_ABI);
    public static final String UNKNOWN = "unknown";

    private DeviceUtils() {
    }

    @SuppressWarnings("deprecation")
    private static String getCPUArch(String abi) {
        if ("arm64-v8a".equals(abi)) {
            return "arm64";
        } else if ("x86_64".equals(abi)) {
            return "x86_64";
        } else if ("mips64".equals(abi)) {
            return "mips64";
        } else if (abi.startsWith("armeabi-v7")) {
            return "arm-v7";
        } else if (abi.startsWith("x86") || Build.CPU_ABI2.startsWith("x86")) {
            return "x86";
        } else if (abi.startsWith("mips")) {
            return "mips";
        } else if (Build.CPU_ABI.startsWith("armeabi-v5") || Build.CPU_ABI.startsWith("armeabi-v6")) {
            return "arm-v5";
        } else if (abi.startsWith("armeabi")) {
            return "arm";
        } else {
            return UNKNOWN;
        }
    }

    public static String getUIFramework() {
        StringBuilder sb = new StringBuilder();
        sb.append(Character.toUpperCase(Build.MANUFACTURER.charAt(0)));
        sb.append(Build.MANUFACTURER.substring(1));
        if (!Build.BRAND.equals(Build.MANUFACTURER)) {
            sb.append(' ').append(Character.toUpperCase(Build.BRAND.charAt(0))).append(Build.BRAND.substring(1));
        }
        sb.append(' ').append(Build.MODEL).append(' ');
        boolean isMIUI = false;
        boolean isTouchWiz = false;
        if ("xiaomi".equalsIgnoreCase(Build.MANUFACTURER) &&
                FileUtils.isExisting("/system/framework/framework-miui-res.apk")) {
            isMIUI = true;
        } else if ("Samsung".equalsIgnoreCase(Build.MANUFACTURER) &&
                FileUtils.isExisting("/system/framework/twframework.jar")) {
            isTouchWiz = true;
        }

        if (isMIUI) {
            sb.append("(MIUI)");
        } else if (isTouchWiz) {
            sb.append("(TouchWiz)");
        } else {
            sb.append("(AOSP-based ROM)");
        }

        return sb.toString();
    }

    public static boolean detectVerifiedBoot() {
        return !TextUtils.isEmpty(SystemProperties.get.callStatic("ro.boot.verifiedbootstate", ""))
                || FileUtils.isExisting("/sys/module/dm_verity");
    }

    public static boolean isVerifiedBootActive() {
        String state = SystemProperties.get.callStatic("partition.system.verified", "0");
        return !"0".equals(state);
    }

    public static boolean is64Bits() {
        return "arm64".equalsIgnoreCase(CPU_ARCH)
                || "x86_64".equalsIgnoreCase(CPU_ARCH)
                || "mips64".equalsIgnoreCase(CPU_ARCH);
    }

    public static boolean isSony() {
        return "sony".equalsIgnoreCase(android.os.Build.BRAND);
    }
}
