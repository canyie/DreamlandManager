package top.canyie.dreamland.manager.utils;

import android.system.ErrnoException;
import android.system.OsConstants;

import top.canyie.dreamland.manager.utils.callbacks.ExceptionCallback;
import top.canyie.dreamland.manager.utils.callbacks.OnLineCallback;
import top.canyie.dreamland.manager.utils.callbacks.ResultCallback;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.Reader;
import java.io.Writer;

/**
 * I/O utils.
 * @author canyie
 */
public final class IOUtils {
    private static final String TAG = "IOUtils";

    private IOUtils() {}

    public static String readAllString(InputStream input) throws IOException {
        return readAllString(new InputStreamReader(input));
    }

    public static String readAllString(Reader input) throws IOException {
        BufferedReader br = new BufferedReader(input);
        try {
            StringBuilder sb = new StringBuilder();
            String line;
            while((line = br.readLine()) != null) {
                sb.append(line).append('\n');
            }
            return sb.toString();
        } finally {
            closeQuietly(br);
        }
    }

    public static void readAllStringAsync(final InputStream input, final ResultCallback<String> resultCallback, final ExceptionCallback<IOException> exceptionCallback) {
        Threads.getDefaultExecutor().execute(() -> {
            try {
                String contents = readAllString(input);
                resultCallback.onDone(contents);
            } catch (IOException e) {
                exceptionCallback.onException(e);
            }
        });
    }

    public static void readAllLines(InputStream input, OnLineCallback callback) throws IOException {
        readAllLines(new InputStreamReader(input), callback);
    }

    public static void readAllLines(Reader input, OnLineCallback callback) throws IOException {
        BufferedReader br = new BufferedReader(input);
        try {
            String line;
            while ((line = br.readLine()) != null) {
                if (callback.onLine(line)) {
                    break;
                }
            }
        } finally {
            closeQuietly(br);
        }
    }

    public static void readAllLinesAsync(InputStream in, OnLineCallback onLineCallback, ExceptionCallback<IOException> exceptionCallback) {
        readAllLinesAsync(new InputStreamReader(in), onLineCallback, exceptionCallback);
    }

    public static void readAllLinesAsync(Reader in, OnLineCallback onLineCallback, ExceptionCallback<IOException> exceptionCallback) {
        Threads.getDefaultExecutor().execute(() -> {
            try {
                readAllLines(in, onLineCallback);
            } catch (IOException e) {
                exceptionCallback.onException(e);
            }
        });
    }

    public static void writeTo(Writer out, String content) throws IOException {
        BufferedWriter bw = null;
        try {
            bw = new BufferedWriter(out);
            bw.write(content);
        } finally {
            closeQuietly(bw);
        }
    }

    public static void writeTo(InputStream in, OutputStream out) throws IOException {
        BufferedOutputStream bos = null;
        BufferedInputStream bis = null;
        try {
            bis = new BufferedInputStream(in);
            bos = new BufferedOutputStream(out);
            writeToDirectly(bis, bos);
        } finally {
            closeQuietly(bos);
            closeQuietly(bis);
        }
    }

    public static void writeToDirectly(InputStream in, OutputStream out) throws IOException {
        byte[] buffer = new byte[8192];
        int len;
        while ((len = in.read(buffer)) != -1) {
            out.write(buffer, 0, len);
        }
    }

    public static void closeQuietly(Closeable closeable) {
        if(closeable != null) {
            try {
                closeable.close();
            } catch(IOException e) {
                DLog.e(TAG, "Error while closing Closeable " + closeable, e);
            }
        }
    }

    public static IOException wrapIOException(IOException e, IOException e2) {
        if (e == null) {
            return e2;
        }
        e.addSuppressed(e2);
        return e;
    }

    public static int getErrno(IOException e) {
        Throwable cause = e;
        do {
            if (cause instanceof ErrnoException) {
                return ((ErrnoException) cause).errno;
            }
        } while ((cause = cause.getCause()) != null);
        return 0;
    }

    public static boolean isPipeBroken(IOException e) {
        String message = e.getMessage();
        if (message != null && message.contains("EPIPE")) return true;

        return getErrno(e) == OsConstants.EPIPE;
    }
}
