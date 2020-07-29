package top.canyie.dreamland.manager.utils;

import android.os.Build;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Lody
 */
public final class ArtDexOptimizer {
    private ArtDexOptimizer() {
    }

    /**
     * Optimize the dex in compile mode.
     *
     * @param dexFilePath dex file path
     * @param oatFilePath oat file path
     * @throws IOException
     */
    public static void compile(String dexFilePath, String oatFilePath) throws IOException {
        DLog.i("ArtDexOptimizer", ">>> Starting dex2oat for dex %s, oat %s <<<", dexFilePath, oatFilePath);
        final File oatFile = new File(oatFilePath);
        if (!oatFile.exists()) {
            oatFile.getParentFile().mkdirs();
        }

        final List<String> commandAndParams = new ArrayList<>();
        commandAndParams.add("dex2oat");
        // for 7.1.1, duplicate class fix

        //if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) { // always true
        commandAndParams.add("--runtime-arg");
        commandAndParams.add("-classpath");
        commandAndParams.add("--runtime-arg");
        commandAndParams.add("&");
        //}

        commandAndParams.add("--dex-file=" + dexFilePath);
        commandAndParams.add("--oat-file=" + oatFilePath);
        commandAndParams.add("--instruction-set=" + DeviceUtils.CPU_ISA);
        commandAndParams.add("--compiler-filter=everything");
        if (/*Build.VERSION.SDK_INT >= 22 && */Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            commandAndParams.add("--compile-pic");
        }
        final Process dex2oatProcess = new ProcessBuilder(commandAndParams)
                .redirectErrorStream(true)
                .start();
        consumeInputStream(dex2oatProcess.getInputStream());
        consumeInputStream(dex2oatProcess.getErrorStream());
        try {
            final int ret = dex2oatProcess.waitFor();
            if (ret != 0) {
                throw new IOException("dex2oat works unsuccessfully, exit code: " + ret);
            }
        } catch (InterruptedException e) {
            throw new IOException("dex2oat is interrupted, msg: " + e.getMessage(), e);
        }
    }

    public static String getOatPathInDalvikCache(String oatName) {
        return "/data/dalvik-cache/" + DeviceUtils.CPU_ISA + "/" + oatName;
    }

    public static String getOatNameInDalvikCache(String dexPath) {
        String dexPathWithoutRootDir = dexPath.substring(1); // skip leading slash
        String cacheFilename = dexPathWithoutRootDir.replace('/', '@');
        if (!(dexPath.endsWith(".dex") || dexPath.endsWith(".art") || dexPath.endsWith(".oat"))) {
            // Suppose it's a zip file with classes.dex.
            cacheFilename += "@classes.dex";
        }
        return cacheFilename;
    }

    private static void consumeInputStream(InputStream in) {
        Threads.getDefaultExecutor().execute(() -> {
            byte[] buffer = new byte[256];
            try {
                // noinspection StatementWithEmptyBody
                while ((in.read(buffer) > 0)) ;
            } catch (IOException ignored) {
            } finally {
                try {
                    in.close();
                } catch (IOException ignored) {
                }
            }
        });
    }
}
