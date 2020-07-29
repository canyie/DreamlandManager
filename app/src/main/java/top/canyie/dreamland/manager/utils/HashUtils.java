package top.canyie.dreamland.manager.utils;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * @author canyie
 */
@SuppressWarnings("WeakerAccess")
public final class HashUtils {
    public static final String ALGORITHM_MD5 = "md5";

    private HashUtils() {
    }

    public static MessageDigest md5() {
        try {
            return MessageDigest.getInstance(ALGORITHM_MD5);
        } catch (NoSuchAlgorithmException e) {
            throw new AssertionError("No MD5 Algorithm", e);
        }
    }

    public static String md5(File file) throws IOException {
        MessageDigest md = md5();
        updateMessageDigestWithFile(file, md);
        return getMessageDigestString(md);
    }

    public static String getMessageDigestString(MessageDigest md) {
        return bytes2HexString(md.digest());
    }

    private static void updateMessageDigestWithFile(File file, MessageDigest md) throws IOException {
        BufferedInputStream bis = null;
        byte[] buffer = new byte[8192];
        try {
            bis = new BufferedInputStream(new FileInputStream(file));
            int len;
            while ((len = bis.read(buffer)) != -1) {
                md.update(buffer, 0, len);
            }
        } finally {
            IOUtils.closeQuietly(bis);
        }
    }

    private static String bytes2HexString(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            int unsignedB = b & 0xff;
            if (unsignedB < 0x10)
                sb.append('0');
            sb.append(Integer.toHexString(unsignedB));
        }
        return sb.toString();
    }
}
