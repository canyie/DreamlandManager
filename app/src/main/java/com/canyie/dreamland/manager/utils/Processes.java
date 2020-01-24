package com.canyie.dreamland.manager.utils;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.content.pm.PackageInfo;
import android.os.Process;
import android.system.Os;

/**
 * @author canyie
 */
public final class Processes {
    private static final String TAG = "Processes";

    @SuppressLint("InlinedApi")
    public static final int UID_ROOT = Process.ROOT_UID;
    public static final int UID_SYSTEM = Process.SYSTEM_UID;
    public static final int UID_PHONE = Process.PHONE_UID;
    @SuppressLint("InlinedApi")
    public static final int UID_SHELL = Process.SHELL_UID;
    public static final int UID_FIRST_APPLICATION = Process.FIRST_APPLICATION_UID;
    public static final int UID_LAST_APPLICATION = Process.LAST_APPLICATION_UID;

    public static final int MY_UID = Process.myUid();
    public static final int MY_GID = Os.getgid();

    private Processes() {
    }

    public static void suicide() {
        int pid = Process.myPid();
        try {
            DLog.w(TAG, "Process %d requesting suicide...", pid);
        } catch (Throwable ignored) {
        }
        System.exit(10);
        Process.killProcess(pid);
        Runtime.getRuntime().halt(10);
        Process.sendSignal(pid, 6/*SIGABRT*/);
        throw new AssertionError();
    }
}
