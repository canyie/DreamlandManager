package com.canyie.dreamland.manager.utils;

import androidx.annotation.Nullable;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

import com.canyie.dreamland.manager.utils.reflect.ReflectiveException;

import mirror.android.os.SELinux;

/**
 * @author canyie
 */
public final class SELinuxHelper {
    private static final String TAG = "SELinuxHelper";
    private static final File SELINUX_MNT = new File("/sys/fs/selinux/");
    private static final File SELINUX_STATUS_FILE = new File(SELINUX_MNT, "enforce");

    private SELinuxHelper() {}

    /**
     * Returns whether SELinux is enabled.
     */
    public static boolean isEnabled() {
        if (SELINUX_MNT.exists()) return true;
        // On Android 4.3+, the platform provides the android.os.SELinux API.
        try {
            return SELinux.isSELinuxEnabled.callStatic();
        } catch (ReflectiveException e) {
            DLog.e(TAG, "Failed to call android.os.SELinux.isSELinuxEnabled()", e);
        }
        return false;
    }

    /**
     * Returns whether SELinux is in enforced mode.
     */
    public static boolean isEnforcing() {
        boolean isSELinuxStatusFileExists = SELINUX_STATUS_FILE.exists();
        if (isSELinuxStatusFileExists) {
            FileInputStream fis = null;
            try {
                fis = new FileInputStream(SELINUX_STATUS_FILE);
                int c = fis.read(); // 0=permissive 1=enforcing
                switch (c) {
                    case '1':
                        return true;
                    case '0':
                        return false;
                }
                DLog.e(TAG, "Unexpected byte %c(%d) in /sys/fs/selinux/enforce", c, c);
            } catch (IOException e) {
                DLog.e(TAG, "Error while reading " + SELINUX_STATUS_FILE.getAbsolutePath(), e);
            } finally {
                IOUtils.closeQuietly(fis);
            }
        }

        // On Android 4.3+, the platform provides the android.os.SELinux API.
        try {
            return SELinux.isSELinuxEnforced.callStatic();
        } catch (ReflectiveException e) {
            DLog.e(TAG, "Failed to call android.os.SELinux.isSELinuxEnforced()", e);
        }

        return isSELinuxStatusFileExists;
    }

    /**
     * Returns the SELinux context of the current process.
     * If SELinux is disabled or the acquisition fails, null is returned.
     */
    @Nullable public static String getContext() {
        if (!isEnabled()) return null;
        // Why I can't find the implementation of the getcon function?
        // Seems to be available by reading the file /proc/self/attr/current
        try {
            return SELinux.getContext.callStatic();
        } catch (ReflectiveException e) {
            DLog.e(TAG, "Failed to get the SELinux context of the current process", e);
        }
        return null;
    }

    /**
     * Returns the SELinux context of the file.
     * If SELinux is disabled or the acquisition fails, null is returned.
     */
    @Nullable public static String getFileContext(String path) {
        if (!isEnabled()) return null;
        try {
            return SELinux.getFileContext.callStatic(path);
        } catch (ReflectiveException e) {
            DLog.e(TAG, "Failed to get the SELinux context of file " + path, e);
        }
        return null;
    }
}
