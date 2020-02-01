package com.canyie.dreamland.manager.core.installation;

import android.content.Context;
import android.os.Build;
import android.text.TextUtils;

import androidx.annotation.IntDef;
import androidx.annotation.NonNull;

import com.canyie.dreamland.manager.AppGlobals;
import com.canyie.dreamland.manager.BuildConfig;
import com.canyie.dreamland.manager.R;
import com.canyie.dreamland.manager.utils.DLog;
import com.canyie.dreamland.manager.utils.DeviceUtils;
import com.canyie.dreamland.manager.utils.FileUtils;
import com.canyie.dreamland.manager.utils.ForegroundThread;
import com.canyie.dreamland.manager.utils.HashUtils;
import com.canyie.dreamland.manager.utils.IOUtils;
import com.canyie.dreamland.manager.utils.Processes;
import com.canyie.dreamland.manager.utils.RootUtils;
import com.canyie.dreamland.manager.utils.SELinuxHelper;
import com.canyie.dreamland.manager.utils.Shell;
import com.canyie.dreamland.manager.utils.Threads;
import com.canyie.dreamland.manager.utils.callbacks.ExceptionCallback;
import com.google.gson.JsonSyntaxException;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.Arrays;

/**
 * @author canyie
 */
public final class Installer {
    private static final String TAG = "Installer";

    public static final int PHASE_UNZIPPING_FRAMEWORK_ZIP = 0;
    public static final int PHASE_CHECKING_ZIP = 1;
    public static final int PHASE_EXECUTING_INSTALL_SCRIPT = 2;
    public static final int PHASE_CLEANUP = 3;

    @Retention(RetentionPolicy.SOURCE)
    @IntDef({PHASE_UNZIPPING_FRAMEWORK_ZIP, PHASE_CHECKING_ZIP, PHASE_EXECUTING_INSTALL_SCRIPT, PHASE_CLEANUP})
    public @interface Phase {
    }

    public static final int ERROR_BAD_FRAMEWORK_ZIP = 0;
    public static final int ERROR_REQUIRED_MANAGER_VERSION_TOO_HIGH = 1;
    public static final int ERROR_CPU_NOT_SUPPORTED = 2;
    public static final int ERROR_ANDROID_VERSION_NOT_SUPPORTED = 3;
    public static final int ERROR_UNKNOWN_IO_ERROR = 4;
    public static final int ERROR_SCRIPT_ERROR = 5;
    public static final int ERROR_UNKNOWN_ERROR = 6;

    private static boolean hasInstance;

    private final File mBaseFrameworkZipDir;
    private final File mBaseOutputDir;
    private Thread mRunningThread;

    public Installer() {
        if (hasInstance) {
            throw new IllegalStateException("Already has a Installer instance");
        }
        hasInstance = true;
        Context context = AppGlobals.getApp();
        mBaseFrameworkZipDir = new File(context.getExternalFilesDir("downloads"), "framework");
        mBaseOutputDir = new File(context.getFilesDir(), "temp");
    }

    public File getOutputFile(@NonNull FrameworkZipInfo frameworkZipInfo) {
        return new File(mBaseFrameworkZipDir, "framework-" + frameworkZipInfo.version + ".zip");
    }

    public boolean isValidFrameworkZip(@NonNull File zip, @NonNull FrameworkZipInfo frameworkZipInfo) {
        if (!zip.exists()) {
            return false;
        }
        String expectMD5 = frameworkZipInfo.md5;
        if (!TextUtils.isEmpty(expectMD5)) {
            try {
                String gotMD5 = HashUtils.md5(zip);
                if (!expectMD5.equalsIgnoreCase(gotMD5)) {
                    DLog.i(TAG, "Found existing framework zip %s but md5 mismatch(expect %s, but got %s), ignore", zip.getAbsolutePath(), expectMD5, gotMD5);
                    // noinspection ResultOfMethodCallIgnored
                    zip.delete();
                    return false;
                }
            } catch (IOException e) {
                DLog.w(TAG, "getMD5 failed", e);
                // noinspection ResultOfMethodCallIgnored
                zip.delete();
                return false;
            }
        }
        return true;
    }

    public boolean hasRunningJob() {
        return mRunningThread != null && mRunningThread.isAlive();
    }

    public synchronized void startInstall(File zip, @NonNull InstallListener listener) {
        if (hasRunningJob()) {
            throw new IllegalThreadStateException("Already has a running install/uninstall job");
        }
        DLog.i(TAG, "Starting install framework zip %s", zip.getAbsolutePath());
        Runnable action = () -> {
            try {
                install(zip, listener);
            } finally {
                mRunningThread = null;
            }
        };
        start("DreamlandInstallerThread", action);
    }

    public synchronized void startUninstall(File zip, @NonNull InstallListener listener) {
        if (hasRunningJob()) {
            throw new IllegalThreadStateException("Already has a running install/uninstall job");
        }
        DLog.i(TAG, "Starting uninstall framework (zip %s)", zip.getAbsolutePath());
        Runnable action = () -> {
            try {
                uninstall(zip, listener);
            } finally {
                mRunningThread = null;
            }
        };
        start("DreamlandUninstallerThread", action);
    }

    private void start(String threadName, Runnable action) {
        mRunningThread = new ForegroundThread(threadName, action);
        mRunningThread.start();
    }

    void install(File zip, InstallListener listener) {
        try {
            File output = unzipFrameworkZip(zip, listener);
            try {
                installImpl(output, listener);
            } finally {
                try {
                    FileUtils.delete(output);
                } catch (IOException ex) {
                    DLog.e(TAG, "Failed to clean output directory " + output.getAbsolutePath(), ex);
                }
            }
            listener.onDone();
        } catch (InstallationException e) {
            DLog.e(TAG, "Failed to install " + zip.getAbsolutePath(), e);
            if (e.error == ERROR_BAD_FRAMEWORK_ZIP) {
                DLog.e(TAG, "install returned ERROR_BAD_FRAMEWORK_ZIP, delete it.");
                // noinspection ResultOfMethodCallIgnored
                zip.delete();
            }
            listener.onError(e);
        }
    }

    private File unzipFrameworkZip(File zip, InstallListener listener) throws InstallationException {
        File output = new File(mBaseOutputDir, FileUtils.getFilenameNoSuffix(zip.getName()));
        listener.onPhase(PHASE_UNZIPPING_FRAMEWORK_ZIP);
        try {
            FileUtils.deleteSubfiles(output);
            FileUtils.unzip(zip, output);
            return output;
        } catch (IOException e) {
            throw new InstallationException(e);
        }
    }

    public void uninstall(File zip, InstallListener listener) {
        try {
            File output = unzipFrameworkZip(zip, listener);
            try {
                uninstallImpl(output, listener);
            } finally {
                try {
                    FileUtils.delete(output);
                } catch (IOException ex) {
                    DLog.e(TAG, "Failed to clean output directory " + output.getAbsolutePath(), ex);
                }
            }
            listener.onDone();
        } catch (InstallationException e) {
            DLog.e(TAG, "Failed to uninstall " + zip.getAbsolutePath(), e);
            if (e.error == ERROR_BAD_FRAMEWORK_ZIP) {
                DLog.e(TAG, "uninstall returned ERROR_BAD_FRAMEWORK_ZIP, delete it.");
                // noinspection ResultOfMethodCallIgnored
                zip.delete();
            }
            listener.onError(e);
        }
    }

    private void installImpl(File base, InstallListener listener) throws InstallationException {
        listener.onPhase(PHASE_CHECKING_ZIP);
        FrameworkZipProperties properties = getProperties(base);

        File installScriptFile = new File(base, "install.sh");
        if (!installScriptFile.exists()) {
            throw new InstallationException(ERROR_BAD_FRAMEWORK_ZIP, "Missing install script(install.sh)");
        }

        File uninstallScriptFile = new File(base, "uninstall.sh");
        if (!uninstallScriptFile.exists()) {
            throw new InstallationException(ERROR_BAD_FRAMEWORK_ZIP, "Missing uninstall script(uninstall.sh)");
        }

        execScript(properties, installScriptFile, true, listener);
    }

    private void uninstallImpl(File base, InstallListener listener) throws InstallationException {
        listener.onPhase(PHASE_CHECKING_ZIP);
        FrameworkZipProperties properties = getProperties(base);

        File uninstallScriptFile = new File(base, "uninstall.sh");
        if (!uninstallScriptFile.exists()) {
            throw new InstallationException(ERROR_BAD_FRAMEWORK_ZIP, "Missing uninstall script(uninstall.sh)");
        }

        execScript(properties, uninstallScriptFile, false, listener);
    }

    private FrameworkZipProperties getProperties(File base) throws InstallationException {
        FrameworkZipProperties properties;
        try {
            properties = AppGlobals.getGson().fromJson(FileUtils.readAllString(base, "properties.json"), FrameworkZipProperties.class);
        } catch (FileNotFoundException e) {
            throw new InstallationException(ERROR_BAD_FRAMEWORK_ZIP, "Missing properties.json", e);
        } catch (JsonSyntaxException e) {
            throw new InstallationException(ERROR_BAD_FRAMEWORK_ZIP, "Syntax error in properties.json", e);
        } catch (IOException e) {
            throw new InstallationException("Unknown error when reading properties.json", e);
        }

        if (properties.minManagerVersionCode > BuildConfig.VERSION_CODE) {
            throw new InstallationException(ERROR_REQUIRED_MANAGER_VERSION_TOO_HIGH,
                    String.format("Framework required minimum manager version too high (require %s, current %s)",
                            properties.minManagerVersionName, BuildConfig.VERSION_NAME));
        }

        boolean supportThisAbi = false;
        for (String supportedAbi : properties.supportedAbis) {
            if (DeviceUtils.CPU_ABI.equalsIgnoreCase(supportedAbi)) {
                supportThisAbi = true;
                break;
            }
        }
        if (!supportThisAbi) {
            throw new InstallationException(ERROR_CPU_NOT_SUPPORTED, String.format("Framework not supported current abi %s (supported %s)", DeviceUtils.CPU_ABI, Arrays.toString(properties.supportedAbis)));
        }

        if (properties.minAndroidVersion > Build.VERSION.SDK_INT) {
            throw new InstallationException(ERROR_ANDROID_VERSION_NOT_SUPPORTED, String.format("Framework required minimum android sdk version too high(required %d, current %d)", properties.minAndroidVersion, Build.VERSION.SDK_INT));
        }

        return properties;
    }

    private void execScript(FrameworkZipProperties properties, File scriptFile, boolean isInstall, InstallListener listener) throws InstallationException {
        DLog.i(TAG, "Starting execute script; scriptFile=%s action=%s", scriptFile.getAbsolutePath(), (isInstall ? "install" : "uninstall"));
        listener.onPhase(PHASE_EXECUTING_INSTALL_SCRIPT);

        Shell.Builder shellBuilder = Shell.su()
                .workDirectory(scriptFile.getParentFile())
                .add("sh " + scriptFile.getName())
                .ensureEnvMapCapacity(8)
                .env("cpu_abi", DeviceUtils.CPU_ABI)
                .env("android_version", Integer.toString(Build.VERSION.SDK_INT))
                .env("manager_uid", Integer.toString(Processes.MY_UID))
                .env("manager_gid", Integer.toString(Processes.MY_GID))
                .env("framework_version", Integer.toString(properties.versionCode));
        boolean isSELinuxEnabled = SELinuxHelper.isEnabled();
        shellBuilder.env("is_selinux_enabled", isSELinuxEnabled ? "1" : "0");
        if (isSELinuxEnabled) {
            String seContext = SELinuxHelper.getContext();
            if (TextUtils.isEmpty(seContext)) {
                throw new InstallationException(ERROR_UNKNOWN_ERROR, "Failed to get selinux context of manager.");
            }
            String fileSEContext = SELinuxHelper.getFileContext(AppGlobals.getApp().getFilesDir().getAbsolutePath());
            if (TextUtils.isEmpty(fileSEContext)) {
                throw new InstallationException(ERROR_UNKNOWN_ERROR, "Failed to get selinux context of file");
            }

            shellBuilder.env("manager_secontext", seContext);
            shellBuilder.env("manager_file_secontext", fileSEContext);
        }

        try {
            boolean enforced = SELinuxHelper.isEnforcing();
            if (enforced)
                RootUtils.setSELinuxEnforce(false);
            try {
                Shell.Result r = shellBuilder.start();
                ExceptionCallback<IOException> exceptionCallback = e -> {
                    r.killProcess();
                    listener.onError(new InstallationException("Failed to read remote shell process's stdout/stderr", e));
                };

                IOUtils.readAllLinesAsync(r.getInput(), line -> {
                    listener.onLine(line);
                    return false;
                }, exceptionCallback);

                IOUtils.readAllLinesAsync(r.getError(), line -> {
                    listener.onErrorLine(line);
                    return false;
                }, exceptionCallback);

                int exitCode = r.waitInterruptible();
                DLog.i(TAG, "Script executed. exitCode %d", exitCode);
                Context context = AppGlobals.getApp();
                if (exitCode == Shell.EXIT_STATUS_SUCCESS) {
                    listener.onLine(context.getString(R.string.shell_process_normal_exit));
                } else {
                    listener.onErrorLine(context.getString(R.string.shell_process_unexpected_exit, exitCode));
                    throw new InstallationException(ERROR_SCRIPT_ERROR, "Unexpected exitCode " + exitCode);
                }
            } finally {
                listener.onPhase(PHASE_CLEANUP);
                try {
                    if (enforced)
                        RootUtils.setSELinuxEnforce(true);
                } catch (IOException e) {
                    DLog.e(TAG, "Failed to restore SELinux mode", e);
                }
            }
        } catch (IOException e) {
            throw new InstallationException(e);
        }
    }
}
