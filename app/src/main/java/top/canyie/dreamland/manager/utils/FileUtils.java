package top.canyie.dreamland.manager.utils;

import android.os.Build;
import android.system.ErrnoException;
import android.system.Os;

import top.canyie.dreamland.manager.AppGlobals;
import top.canyie.dreamland.manager.utils.callbacks.OnLineCallback;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * @author canyie
 */
@SuppressWarnings({"unused", "OctalInteger", "SpellCheckingInspection", "WeakerAccess"})
public final class FileUtils {
    private static final String TAG = "FileUtils";

    private static final byte[] ZIP_MAGIC_1 = {0x50, 0x4B, 0x03, 0x04};
    private static final byte[] ZIP_MAGIC_2 = {0x50, 0x4B, 0x05, 0x06};
    private static final byte[] ZIP_MAGIC_3 = {0x50, 0x4B, 0x07, 0x08};

    private static final byte[] DEX_MAGIC_035 = {0x64, 0x65, 0x78, 0x0A, 0x30, 0x33, 0x35, 0x00};
    private static final byte[] DEX_MAGIC_037 = {0x64, 0x65, 0x78, 0x0A, 0x30, 0x33, 0x37, 0x00};
    private static final byte[] DEX_MAGIC_038 = {0x64, 0x65, 0x78, 0x0A, 0x30, 0x33, 0x38, 0x00};
    private static final byte[] DEX_MAGIC_039 = {0x64, 0x65, 0x78, 0x0A, 0x30, 0x33, 0x39, 0x00};

    public static final int S_IRWXU = 00700;
    public static final int S_IRUSR = 00400;
    public static final int S_IWUSR = 00200;
    public static final int S_IXUSR = 00100;

    public static final int S_IRWXG = 00070;
    public static final int S_IRGRP = 00040;
    public static final int S_IWGRP = 00020;
    public static final int S_IXGRP = 00010;

    public static final int S_IRWXO = 00007;
    public static final int S_IROTH = 00004;
    public static final int S_IWOTH = 00002;
    public static final int S_IXOTH = 00001;

    private FileUtils() {
    }

    public static void setPermissions(String path, int mode) {
        try {
            Os.chmod(path, mode);
        } catch (ErrnoException e) {
            DLog.e(TAG, "Failed to chmod(" + path + ")", e);
        }
    }

    public static void setOwner(String path, int uid, int gid) {
        try {
            Os.chown(path, uid, gid);
        } catch (ErrnoException e) {
            DLog.e(TAG, "Failed to chown(" + path + ")", e);
        }
    }

    public static boolean isValidZip(byte[] content) {
        if (content.length < 4) return false;
        byte[] magic;
        if (content.length == 4) {
            magic = content;
        } else {
            magic = Arrays.copyOf(content, 4);
        }
        return Arrays.equals(magic, ZIP_MAGIC_1) || Arrays.equals(magic, ZIP_MAGIC_2) || Arrays.equals(magic, ZIP_MAGIC_3);
    }

    @SuppressWarnings("SimplifiableIfStatement") public static boolean isValidDex(byte[] content, int minSdkVersion) {
        if (content.length < 8) return false;
        byte[] magic;
        if (content.length == 8) {
            magic = content;
        } else {
            magic = Arrays.copyOf(content, 8);
        }
        if (Arrays.equals(magic, DEX_MAGIC_035))
            return true;
        if (minSdkVersion >= Build.VERSION_CODES.N
                && Arrays.equals(magic, DEX_MAGIC_037))
            return true;
        if (minSdkVersion >= Build.VERSION_CODES.O
                && Arrays.equals(magic, DEX_MAGIC_038))
            return true;
        if (minSdkVersion >= Build.VERSION_CODES.P &&
                Arrays.equals(magic, DEX_MAGIC_039))
            return true;

        return false;
    }

    public static boolean isExisting(String path) {
        return new File(path).exists();
    }

    public static void ensureParentExisting(File file) throws IOException {
        File parent = file.getParentFile();
        assert parent != null;
        ensureDirectroyExisting(parent);
    }

    public static void ensureDirectroyExisting(File directroy) throws IOException {
        if (directroy.exists() || directroy.mkdirs() || directroy.exists()) return;
        throw new IOException("Can't create directroy: " + directroy.getAbsolutePath());
    }

    public static void copyFromAssets(String sourceFilename, File destFile) throws IOException {
        InputStream is = null;
        OutputStream os = null;
        try {
            is = AppGlobals.getApp().getAssets().open(sourceFilename);
            os = new FileOutputStream(destFile);
            IOUtils.writeTo(is, os);
        } finally {
            IOUtils.closeQuietly(os);
            IOUtils.closeQuietly(is);
        }
    }

    public static void unzip(File zip, File to) throws IOException {
        if (!zip.isFile()) {
            throw new IllegalArgumentException("File " + zip.getAbsolutePath() + " is not a invalid file");
        }
        ensureDirectroyExisting(to);
        ZipInputStream zis = null;
        try {
            zis = new ZipInputStream(new BufferedInputStream(new FileInputStream(zip)));
            byte[] buffer = new byte[8192];
            ZipEntry zipEntry;
            while ((zipEntry = zis.getNextEntry()) != null) {
                File file = new File(to, zipEntry.getName());
                if (zipEntry.isDirectory()) {
                    ensureDirectroyExisting(file);
                } else {
                    writeNoCloseIn(file, zis);
                }
            }
        } finally {
            IOUtils.closeQuietly(zis);
        }
    }

    public static String readAllString(File parent, String subfilename) throws IOException {
        return readAllString(new File(parent, subfilename));
    }

    public static String readAllString(File file) throws IOException {
        return IOUtils.readAllString(new FileReader(file));
    }

    public static void readAllLines(String file, OnLineCallback callback) throws IOException {
        readAllLines(new File(file), callback);
    }

    public static void readAllLines(File file, OnLineCallback callback) throws IOException {
        IOUtils.readAllLines(new FileReader(file), callback);
    }

    public static void write(File file, String content) throws IOException {
        ensureParentExisting(file);
        IOUtils.writeTo(new FileWriter(file), content);
    }

    public static void write(File file, InputStream in) throws IOException {
        ensureParentExisting(file);
        IOUtils.writeTo(in, new FileOutputStream(file));
    }

    public static void writeNoCloseIn(File file, InputStream in) throws IOException {
        ensureParentExisting(file);
        BufferedOutputStream bos = null;
        try {
            bos = new BufferedOutputStream(new FileOutputStream(file));
            IOUtils.writeToDirectly(in, bos);
        } finally {
            IOUtils.closeQuietly(bos);
        }
    }

    public static void delete(File file) throws IOException {
        if (file.isDirectory()) {
            deleteSubfiles(file);
        }
        if (file.delete() || !file.exists()) return;
        throw new IOException("Can't delete file " + file.getAbsolutePath());
    }

    public static void deleteSubfiles(File directroy) throws IOException {
        File[] subfiles = directroy.listFiles();
        if (subfiles != null) {
            for (File subfile : subfiles) {
                delete(subfile);
            }
        }
    }

    public static String getFilenameNoSuffix(String filename) {
        int index = filename.lastIndexOf('.');
        // If index is 0, this is a hidden file without a suffix.
        return index > 0 ? filename.substring(0, index) : filename;
    }
}
