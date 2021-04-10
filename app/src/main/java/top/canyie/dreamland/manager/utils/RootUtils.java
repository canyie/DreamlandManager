package top.canyie.dreamland.manager.utils;

import android.os.Build;

import androidx.annotation.WorkerThread;

import java.io.IOException;

import top.canyie.dreamland.manager.utils.callbacks.ExceptionCallback;
import top.canyie.dreamland.manager.utils.callbacks.ResultCallback;

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
            return exec("svc power reboot || reboot", true) == Shell.EXIT_STATUS_SUCCESS;
        } catch (IOException e) {
            DLog.e(TAG, "reboot failed", e);
        }
        return false;
    }

    public static boolean softReboot() {
        try {
            String[] commands = {
                    // After Riru v22, it needs to set "ro.dalvik.vm.native.bridge" to riru loader
                    // for inject into zygote. For hide Riru itself,
                    // it will reset the system property to 0 after boot complete; and when zygote restarts,
                    // the property won't automatically change to riru loader, riru won't be loaded.
                    // Just manual reset the property to riru loader to make riru works.
                    "[[ -f /system/lib/libriruloader.so ]] && resetprop ro.dalvik.vm.native.bridge libriruloader.so",
                    "setprop ctl.restart zygote"
            };
            return exec(commands, true) == Shell.EXIT_STATUS_SUCCESS;
        } catch (IOException e) {
            DLog.e(TAG, "soft reboot failed", e);
        }
        return false;
    }

    public static boolean rebootToRecovery() {
        try {
            String[] commands = {
                    // Create a flag used by some kernels to boot into recovery.
                    "[[ -d /cache/recovery ]] || mkdir /cache/recovery",
                    "touch /cache/recovery/boot",
                    "svc power reboot recovery || reboot recovery"
            };
            return exec(commands, true) == Shell.EXIT_STATUS_SUCCESS;
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

    public static void moveFile(String src, String dest) throws IOException {
        src = escape(src);
        dest = escape(dest);
        int code = exec("mv -f \"" + src + "\" \"" + dest + "\"", false);
        if (code != Shell.EXIT_STATUS_SUCCESS) {
            throw new IOException("mv failed: code " + code);
        }
    }

    public static void execDex(String dex, String className, ResultCallback<Shell.Result> resultCallback, ExceptionCallback<IOException> exceptionCallback) {
        Shell.su()
                .add("app_process /system/bin " + className)
                .env("CLASSPATH", dex)
                .startAsync(resultCallback, exceptionCallback);
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
}
