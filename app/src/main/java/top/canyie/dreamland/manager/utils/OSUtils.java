package top.canyie.dreamland.manager.utils;

import android.os.Build;

import java.lang.reflect.Method;

import mirror.android.os.SystemProperties;

/**
 * @author canyie
 */
public final class OSUtils {
    public static final String UNKNOWN = DeviceUtils.UNKNOWN;

    private static boolean isFlyme;
    private static String miuiVersion;
    private static boolean isEMUI;
    private static boolean isVIVO;
    private static boolean isOPPO;

    private OSUtils() {
    }

    static {
        staticInit();
    }

    private static void staticInit() {
        miuiVersion = SystemProperties.get.callStatic("ro.miui.ui.version.name", UNKNOWN);
        if (!UNKNOWN.equalsIgnoreCase(miuiVersion)) {
            return;
        }

        String emuiVersion = SystemProperties.get.callStatic("ro.build.version.emui", UNKNOWN);
        if (!UNKNOWN.equalsIgnoreCase(emuiVersion)) {
            isEMUI = true;
            return;
        }

        String vivoOSVersion = SystemProperties.get.callStatic("ro.vivo.os.version", UNKNOWN);
        if (!UNKNOWN.equalsIgnoreCase(vivoOSVersion)) {
            isVIVO = true;
            return;
        }

        String oppoOSVersion = SystemProperties.get.callStatic("ro.build.version.opporom", UNKNOWN);
        if (!UNKNOWN.equalsIgnoreCase(oppoOSVersion)) {
            isOPPO = true;
            return;
        }

        Method hasSmartBar;
        try {
            // noinspection JavaReflectionMemberAccess
            hasSmartBar = Build.class.getMethod("hasSmartBar");
        } catch (NoSuchMethodException ignored) {
            hasSmartBar = null;
        }
        if (hasSmartBar != null) {
            isFlyme = true;
        }
    }

    public static boolean isFlyme() {
        return isFlyme;
    }

    public static boolean isMIUI() {
        return !UNKNOWN.equalsIgnoreCase(miuiVersion);
    }

    public static String getMIUIVersion() {
        return miuiVersion;
    }

    public static boolean isEMUI() {
        return isEMUI;
    }

    public static boolean isVIVO() {
        return isVIVO;
    }

    public static boolean isOPPO() {
        return isOPPO;
    }
}
