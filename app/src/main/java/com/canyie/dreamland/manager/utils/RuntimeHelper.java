package com.canyie.dreamland.manager.utils;

import mirror.dalvik.system.VMRuntime;

/**
 * @author canyie
 * @date 2019/12/26.
 */
public final class RuntimeHelper {
    private static final int GC_MIN_TIME = 60 * 1000;

    private static final Runtime sRuntime = Runtime.getRuntime();
    private static final Object sVMRuntime = VMRuntime.getRuntime.callStatic();
    private static final boolean art;
    private static final String vmVersion;

    private static long lastRequestGCTime;

    static {
        vmVersion = System.getProperty("java.vm.version");
        art = vmVersion != null && vmVersion.startsWith("2");
    }

    private RuntimeHelper() {
    }

    public static boolean isArt() {
        return art;
    }

    public static String getVMVersion() {
        return vmVersion;
    }

    public static void requestGC() {
        long now = System.currentTimeMillis();
        if (lastRequestGCTime > 0) {
            if (now < (lastRequestGCTime + GC_MIN_TIME)) {
                return;
            }
        }
        lastRequestGCTime = now;
        sRuntime.gc();
    }

    public static void requestHeapTrim() {
        VMRuntime.requestHeapTrim.call(sVMRuntime);
    }
}
