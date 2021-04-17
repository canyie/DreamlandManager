package top.canyie.dreamland.manager.utils;

import android.os.Build;
import android.text.TextUtils;

import mirror.android.os.SystemProperties;

/**
 * @author canyie
 */
@SuppressWarnings("WeakerAccess") public final class DeviceUtils {
    @SuppressWarnings("deprecation")
    public static final String CPU_ABI = Build.CPU_ABI;
    public static final String CPU_ARCH = getCPUArch();
    public static final String CPU_ISA = getCPUInstructionSet();

    public static final String UNKNOWN = "unknown";

    private DeviceUtils() {
    }

    @SuppressWarnings("deprecation")
    private static String getCPUArch() {
        final String abi = CPU_ABI;
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

    private static String getCPUInstructionSet() {
        switch (CPU_ARCH) {
            case "arm-v7":
            case "arm-v5":
                return "arm";
            default:
                return CPU_ARCH;
        }
    }

    public static String getUIFramework() {
        StringBuilder sb = new StringBuilder();
        sb.append(Character.toUpperCase(Build.MANUFACTURER.charAt(0)));
        sb.append(Build.MANUFACTURER.substring(1));
        if (!Build.BRAND.equals(Build.MANUFACTURER)) {
            sb.append(' ').append(Character.toUpperCase(Build.BRAND.charAt(0))).append(Build.BRAND.substring(1));
        }
        sb.append(' ').append(Build.MODEL);
        return sb.toString();
    }

    public static boolean detectVerifiedBoot() {
        return FileUtils.isExisting("/sys/module/dm_verity")
                || !TextUtils.isEmpty(SystemProperties.get.callStatic("ro.boot.verifiedbootstate", ""));
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

    public static boolean isArmV7OrArm64() {
        return "arm64".equals(CPU_ARCH) || "arm-v7".equals(CPU_ARCH);
    }
}
