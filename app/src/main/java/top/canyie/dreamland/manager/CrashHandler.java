package top.canyie.dreamland.manager;

import android.os.Process;

import androidx.annotation.NonNull;

import top.canyie.dreamland.manager.utils.DLog;
import top.canyie.dreamland.manager.utils.Processes;

/**
 * @author canyie
 */
public final class CrashHandler implements Thread.UncaughtExceptionHandler {
    private static final String TAG = "CrashHandler";
    private static final CrashHandler INSTANCE = new CrashHandler();
    private static final Thread.UncaughtExceptionHandler defaultUncaughtExceptionHandler = Thread.getDefaultUncaughtExceptionHandler();

    private boolean crashing;
    private CrashHandler() {
    }

    @Override public void uncaughtException(@NonNull Thread thread, @NonNull Throwable e) {
        boolean ignore = false;
        try {
            ignore = handleException(thread, e);
        } catch (Throwable e2) {
            try {
                DLog.e(TAG, "Crash handler crash", e2);
            } catch (Throwable ignored) {
            }
        } finally {
            if (!ignore) {
                try {
                    if (defaultUncaughtExceptionHandler != null) {
                        defaultUncaughtExceptionHandler.uncaughtException(thread, e);
                    } else {
                        DLog.e(TAG, "No default uncaught exception handler, aborting...");
                    }
                } finally {
                    Processes.suicide();
                }
            }
        }
    }

    private boolean handleException(Thread thread, Throwable e) throws Throwable {
        // Crash, crash, crash, crash, crash...
        // What can I do? No one knows crash.
        // Who knows? everybody "knows".
        DLog.e(TAG, "*** UNCAUGHT EXCEPTION THROWN IN PROCESS %d THREAD %s", Process.myPid(), thread.toString(), e);

        if (crashing) return false;
        crashing = true;

        return false;
    }

    public static void install() {
        Thread.setDefaultUncaughtExceptionHandler(INSTANCE);
    }
}
