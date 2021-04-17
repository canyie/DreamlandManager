package top.canyie.dreamland.manager.troubleshoot;

import android.os.Process;
import android.system.ErrnoException;
import android.system.Os;
import android.system.StructStat;
import android.util.Log;

import androidx.annotation.Keep;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import mirror.android.os.SELinux;
import mirror.android.os.SystemProperties;
import top.canyie.dreamland.manager.utils.Shell;

import static android.os.Process.SYSTEM_UID;

/**
 * @author canyie
 */
public class Troubleshooter {
    private static void abort(Alert alert, Object... args) {
        Alert.POUNDS.showAsError();
        alert.showAsError(args);
        System.exit(1);
    }

    private static String readMagiskTmp() {
        try {
            Shell.Result r = Shell.sh().add("magisk --path").allowExecOnMainThread().start();
            int status = r.waitFor();
            if (status != Shell.EXIT_STATUS_SUCCESS) {
                String error = r.readAllError();
                abort(Alert.MAGISK_ERROR, error);
            }
            return r.readAll().trim();
        } catch (Throwable e) {
            abort(Alert.MAGISK_ERROR, Log.getStackTraceString(e));
            throw new RuntimeException("Unreachable");
        }
    }

    private static File detectMagiskModules(String tmp, List<AlertList> problems) {
        if (!new File(tmp).canRead()) {
            problems.add(AlertList.MAGISK_TMP_UNREADABLE);
            return null;
        }

        File magisk = new File(tmp, ".magisk");
        if (!magisk.canRead()) {
            problems.add(AlertList.MAGISK_TMP_UNREADABLE);
            return null;
        }
        File liteModules = new File(magisk, "lite_modules"); // magisk-lite
        File modules = liteModules.exists() ? liteModules : new File(magisk, "modules");
        if (!new File(modules, "riru-core").exists()) {
            problems.add(AlertList.NO_RIRU);
        }
        if (!new File(modules, "riru_dreamland").exists()) {
            problems.add(AlertList.NO_DREAMLAND);
        }
        return modules;
    }

    private static boolean checkPermission(String path, int mode, int uid, int gid,
                                           String context, List<AlertList> out, AlertList error) {
        try {
            StructStat stat = Os.stat(path);
            if ((stat.st_mode & mode) != mode
                    || stat.st_uid != uid
                    || stat.st_gid != gid
                    || !context.equals(SELinux.getFileContext.callStatic(path))) {
                out.add(error);
                return true;
            }
        } catch (ErrnoException e) {
            e.printStackTrace(System.err);
        }
        return false;
    }

    private static boolean checkPermissionRecursive(File file, int dirMode, int fileMode, int uid, int gid,
                                           String context, List<AlertList> out, AlertList error) {
        if (checkPermission(file.getAbsolutePath(), file.isDirectory() ? dirMode : fileMode, uid, gid, context, out, error))
            return true;
        File[] children = file.listFiles();
        if (children != null) {
            for (File child : children) {
                if (checkPermissionRecursive(child, dirMode, fileMode, uid, gid, context, out, error))
                    return true;
            }
        }
        return false;
    }

    private static void detectDreamland(List<AlertList> problems) {
        final String JAR_PATH = "/system/framework/dreamland.jar";
        if (!new File(JAR_PATH).exists())
            problems.add(AlertList.CORE_JAR_MISSING);
        final String CONFIG_DIR = "/data/misc/dreamland";
        if (!new File(CONFIG_DIR).isDirectory()) {
            problems.add(AlertList.CONFIG_DIR_BROKEN);
            return;
        }
        if (new File(CONFIG_DIR, "disable").exists()) {
            problems.add(AlertList.DISABLED);
        }
        checkPermission(CONFIG_DIR, 0700, SYSTEM_UID, SYSTEM_UID, "u:object_r:system_data_file:s0", problems, AlertList.CONFIG_DIR_BROKEN);
    }

    private static void checkSEPolicy(List<AlertList> problems) {
        final String SYSTEM_SERVER_CONTEXT = "u:r:system_server:s0";
        boolean loaded = SELinux.checkSELinuxAccess.callStatic(SYSTEM_SERVER_CONTEXT, SYSTEM_SERVER_CONTEXT, "process", "execmem");
        if (!loaded) {
            problems.add(AlertList.SEPOLICY_NOT_LOADED);
        }
    }

    // https://github.com/RikkaApps/Riru/issues/203
    private static void checkSEContext(File modules, List<AlertList> problems) {
        File dreamland = new File(modules, "riru_dreamland");
        File riruLib = new File(dreamland, "riru");
        if (!riruLib.exists()) return; // secontext won't be reset on pre Riru V25
        checkPermissionRecursive(riruLib, 0755, 0644, 0, 0, "u:object_r:system_file:s0", problems, AlertList.WRONG_RIRU_SECONTEXT);
    }

    // If Huawei maple is enabled,
    // system server is forked from "mygote" (a special zygote process) that can't be controlled by Riru V22+
    private static void detectMaple(List<AlertList> problems) {
        // TODO: Maybe we should check persist.mygote.disable and init.svc.mygote here?
        if ("1".equals(SystemProperties.get.callStatic("ro.maple.enable", "0"))) {
            problems.add(AlertList.MAPLE_ENABLED);
        }
    }

    @Keep public static void main(String[] args) {
        if (Process.myUid() != 0) {
            abort(Alert.RUN_AS_ROOT);
        }

        String magiskPath = readMagiskTmp();
        Alert.MAGISK_TMP_PATH_IS.show(magiskPath);
        List<AlertList> problems = new ArrayList<>();
        File modules = detectMagiskModules(magiskPath, problems);
        detectDreamland(problems);
        checkSEPolicy(problems);
        if (modules != null) checkSEContext(modules, problems);
        detectMaple(problems);

        if (problems.isEmpty()) {
            Alert.POUNDS.show();
            Alert.NO_PROBLEM.show();
        } else {
            Alert.FOUND_PROBLEM.showAsError(problems.size());
            for (AlertList alertList : problems) {
                alertList.showAsError();
            }
        }
    }
}
