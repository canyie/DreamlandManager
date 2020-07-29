package top.canyie.dreamland.manager.utils;

import android.os.Build;

import androidx.annotation.WorkerThread;

import java.io.IOException;

/**
 * @author canyie
 */
@SuppressWarnings("WeakerAccess")
public final class RootUtils {
    private static final String TAG = "RootUtils";
    private static final int SYSTEM_PROP_NAME_MAX = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O ? Integer.MAX_VALUE : 31;
    private static final int SYSTEM_PROP_VALUE_MAX = 91;

    private RootUtils() {
    }

    public static boolean reboot() {
        try {
            boolean success = exec("reboot", true) == Shell.EXIT_STATUS_SUCCESS;
            if (success) onRebootSuccess();
            return success;
        } catch (IOException e) {
            DLog.e(TAG, "reboot failed", e);
        }
        return false;
    }

    public static boolean softReboot() {
        try {
            boolean success =  restartInitService("zygote");
            if (success) onRebootSuccess();
            return success;
        } catch (IOException e) {
            DLog.e(TAG, "soft reboot failed", e);
        }
        return false;
    }

    public static boolean rebootToRecovery() {
        try {
            String[] commands = {
                    // Create a flag used by some kernels to boot into recovery.
                    "if [ -d /cache/recovery ]; then",
                    "mkdir /cache/recovery",
                    "fi",
                    "touch /cache/recovery/boot",
                    "reboot recovery"
            };
            boolean success = exec(commands, true) == Shell.EXIT_STATUS_SUCCESS;
            if (success) onRebootSuccess();
            return success;
        } catch (IOException e) {
            DLog.e(TAG, "reboot to recovery failed", e);
        }
        return false;
    }

//    private static boolean deepSoftReboot() {
//        // FIXME
//        final String[] commands = {
//                "setprop ctl.restart servicemanager",
//                "setprop ctl.restart keystore"
//        };
//        try {
//            return exec(commands, true) == Shell.EXIT_STATUS_SUCCESS;
//        } catch (IOException e) {
//            DLog.e(TAG, "deep soft reboot failed", e);
//        }
//        return false;
//    }

    public static boolean restartInitService(String name) throws IOException {
        Preconditions.checkNotEmpty(name, "name is empty");
        return setSystemProperty("ctl.restart", name, true);
    }

    public static boolean setSystemProperty(String name, String value, boolean allowExecOnMainThread) throws IOException {
        if (name.length() > SYSTEM_PROP_NAME_MAX) {
            throw new IllegalArgumentException("key.length > " + SYSTEM_PROP_NAME_MAX);
        }
        if (value.length() > SYSTEM_PROP_VALUE_MAX) {
            throw new IllegalArgumentException("value.length > " + SYSTEM_PROP_VALUE_MAX);
        }
        String command = "setprop " + name + " " + value;
        return exec(command, allowExecOnMainThread) == Shell.EXIT_STATUS_SUCCESS;
    }

    @SuppressWarnings("UnusedReturnValue") @WorkerThread public static boolean forceStopApp(String packageName) {
        try {
            return exec("am force-stop " + packageName, false) == Shell.EXIT_STATUS_SUCCESS;
        } catch (IOException e) {
            DLog.e(TAG, "Failed to force stop application " + packageName, e);
        }
        return false;
    }

    public static void forceStopAppAsync(String packageName) {
        Threads.getDefaultExecutor().execute(() -> RootUtils.forceStopApp(packageName));
    }

    public static void setSELinuxEnforce(boolean enforce) throws IOException {
        final String command = "setenforce " + (enforce ? 1 : 0);
        int code = exec(command, false);
        if (code != Shell.EXIT_STATUS_SUCCESS) {
            IOException ioException = new IOException("Failed to set SELinux mode, exitCode " + code);
            DLog.e(TAG, ioException);
            throw ioException;
        }
    }

    public static void moveFile(String src, String dest) throws IOException {
        src = escape(src);
        dest = escape(dest);
        int code = exec("mv -f \"" + src + "\" \"" + dest + "\"", false);
        if (code != Shell.EXIT_STATUS_SUCCESS) {
            throw new IOException("mv failed: code " + code);
        }
    }

    private static String escape(String str) {
        return str.replace("\"", "\\\"");
    }

    private static int exec(String command, boolean allowExecOnMainThread) throws IOException {
        return exec(new String[]{command}, allowExecOnMainThread);
    }

    private static int exec(String[] commands, boolean allowExecOnMainThread) throws IOException {
        Shell.Result r = Shell.su().add(commands).allowExecOnMainThread(allowExecOnMainThread).start();
        return r.waitInterruptible();
    }

    private static void onRebootSuccess() {
        // The device is rebooting. This process will be killed by the system,
        // so we voluntarily exit, no need to bother the system. :)
        DLog.i(TAG, "Exiting dreamland manager process because device is rebooting...");
        System.exit(0);
    }
}
