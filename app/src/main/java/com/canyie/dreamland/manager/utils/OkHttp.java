package com.canyie.dreamland.manager.utils;

import android.text.TextUtils;

import androidx.annotation.IntRange;
import androidx.annotation.MainThread;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.Size;

import com.canyie.dreamland.manager.AppGlobals;
import com.google.gson.reflect.TypeToken;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.security.MessageDigest;
import java.util.concurrent.TimeUnit;

import okhttp3.Call;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;
import okio.BufferedSink;
import okio.BufferedSource;
import okio.Okio;

/**
 * @author canyie
 */
public final class OkHttp {
    private static final String TAG = "OkHttp";
    private static final OkHttpClient sOkHttpClient = new OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(1, TimeUnit.MINUTES)
            .writeTimeout(1, TimeUnit.MINUTES)
            .retryOnConnectionFailure(true)
            .build();

    private OkHttp() {
    }

    public static <T> Call get(String url, Callback<T> callback) {
        return get(url, null, callback);
    }

    public static <T> Call get(String url, @Nullable Object tag, Callback<T> callback) {
        Type type = getType(callback);
        Request request = new Request.Builder()
                .url(url)
                .tag(tag)
                .build();
        return request(request, type, callback);
    }

    public static DownloadRequest download() {
        return new DownloadRequest();
    }

    private static <T> Call request(Request request, Type type, Callback<T> callback) {
        Call call = sOkHttpClient.newCall(request);

        call.enqueue(new okhttp3.Callback() {
            @Override public void onResponse(@NonNull Call call, @NonNull Response response) {
                try {
                    T data = genBean(type, response);
                    Threads.execOnMainThread(() -> callback.onSucceed(call, data));
                } catch (IOException e) {
                    onFailure(call, e);
                }
            }

            @Override public void onFailure(@NonNull Call call, @NonNull IOException e) {
                if (call.isCanceled()) {
                    Threads.execOnMainThread(() -> callback.onCanceled(call));
                } else {
                    Threads.execOnMainThread(() -> callback.onFailed(call, e));
                }
            }
        });
        return call;
    }

    private static Type getType(Object o) {
        return ((ParameterizedType)o.getClass().getGenericInterfaces()[0]).getActualTypeArguments()[0];
    }

    @SuppressWarnings("unchecked") private static <T> T genBean(@NonNull Type type, Response response) throws IOException {
        Class<?> rawType = TypeToken.get(type).getRawType();
        if (rawType == Response.class) {
            return (T) response;
        }
        ResponseBody responseBody = response.body();
        if (rawType == ResponseBody.class) {
            return (T) responseBody;
        }
        if (responseBody == null) {
            throw new IllegalStateException("Unable to generate response bean: response body is null");
        }
        if (rawType == byte[].class) {
            return (T) responseBody.bytes();
        }
        String responseBodyContent = responseBody.string();
        if (rawType == String.class) {
            return (T) responseBodyContent;
        }
        return AppGlobals.getGson().fromJson(responseBodyContent, type);
    }

    public static void cancelAll() {
        sOkHttpClient.dispatcher().cancelAll();
    }

    public static void cancelCallByTag(@NonNull Object tag) {
        for (Call call : sOkHttpClient.dispatcher().queuedCalls()) {
            if (tag.equals(call.request().tag())) call.cancel();
        }

        for (Call call : sOkHttpClient.dispatcher().runningCalls()) {
            if (tag.equals(call.request().tag())) call.cancel();
        }
    }

    public static OkHttpClient getOkHttpClient() {
        return sOkHttpClient;
    }

    public static final class DownloadRequest {
        private Request.Builder base;
        private File dest;
        @Nullable private String expectMD5;

        DownloadRequest() {
            base = new Request.Builder();
        }

        public DownloadRequest url(String url) {
            base.url(url);
            return this;
        }

        public DownloadRequest tag(@Nullable Object tag) {
            base.tag(tag);
            return this;
        }

        public DownloadRequest dest(File dest) {
            this.dest = dest;
            return this;
        }

        public DownloadRequest expectMD5(@Nullable @Size(32) String md5) {
            if (!TextUtils.isEmpty(md5))
                this.expectMD5 = md5;
            else
                this.expectMD5 = null;
            return this;
        }

        public DownloadInfo startAsync(@Nullable DownloadListener listener) {
            Preconditions.checkNotNull(dest, "dest == null");
            Request request = base.build();
            return download(request, dest, expectMD5, listener);
        }
    }

    static DownloadInfo download(Request request, File dest, @Nullable String expectMD5, @Nullable DownloadListener listener) {
        Call call = sOkHttpClient.newCall(request);
        DownloadInfo downloadInfo = new DownloadInfo();
        downloadInfo.call = call;
        downloadInfo.dest = dest;
        call.enqueue(new okhttp3.Callback() {
            @Override public void onResponse(@NonNull Call call, @NonNull Response response) {
                ResponseBody responseBody = response.body();
                if (responseBody == null) {
                    throw new IllegalStateException("Unable to download file: response body is null");
                }
                final boolean hasListener = listener != null;
                BufferedSource bufferedSource = Okio.buffer(responseBody.source());
                BufferedSink bufferedSink = null;
                MessageDigest md = expectMD5 != null ? HashUtils.md5() : null;
                try {
                    downloadInfo.totalSize = responseBody.contentLength();
                    downloadInfo.isUnknownSize = downloadInfo.totalSize == -1;
                    bufferedSink = Okio.buffer(Okio.sink(dest));
                    byte[] buffer = new byte[(downloadInfo.totalSize < 8192 &&
                            !downloadInfo.isUnknownSize) ? (int) downloadInfo.totalSize : 8192];
                    if (hasListener)
                        Threads.execOnMainThread(() -> listener.onStart(downloadInfo));
                    int len;
                    while ((len = bufferedSource.read(buffer)) != -1) {
                        bufferedSink.write(buffer, 0, len);
                        downloadInfo.downloadedSize += len;
                        if (hasListener && !downloadInfo.isUnknownSize) {
                            int progress = (int) (downloadInfo.downloadedSize * 1f / downloadInfo.totalSize * 100);
                            if (progress != downloadInfo.progress) {
                                downloadInfo.progress = progress;
                                Threads.execOnMainThread(() -> listener.onProgress(downloadInfo));
                            }
                        }
                        if (md != null) md.update(buffer, 0, len);
                    }
                } catch (IOException e) {
                    onFailure(call, e);
                    return;
                } finally {
                    IOUtils.closeQuietly(bufferedSink);
                    IOUtils.closeQuietly(bufferedSource);
                }

                if (md != null) {
                    String md5 = HashUtils.getMessageDigestString(md);
                    if (!expectMD5.equalsIgnoreCase(md5)) {
                        if (hasListener)
                            Threads.execOnMainThread(() -> listener.onVerifyFailed(downloadInfo, HashUtils.ALGORITHM_MD5, expectMD5, md5));
                        return;
                    }
                }
                if (hasListener)
                    Threads.execOnMainThread(() -> listener.onSucceed(downloadInfo));
            }

            @Override public void onFailure(@NonNull Call call, @NonNull IOException e) {
                if (listener != null) {
                    if (call.isCanceled()) {
                        Threads.execOnMainThread(() -> listener.onCanceled(downloadInfo));
                    } else {
                        Threads.execOnMainThread(() -> listener.onFailed(downloadInfo, e));
                    }
                }
            }
        });
        return downloadInfo;
    }

    @SuppressWarnings("WeakerAccess") public static final class DownloadInfo {
        public Call call;
        public File dest;
        @IntRange(from = 0, to = 100) public int progress;
        public long totalSize;
        public long downloadedSize;
        public boolean isUnknownSize;
    }

    @MainThread
    public interface Callback<T> {
        void onSucceed(Call call, T data);
        void onFailed(Call call, IOException e);
        default void onCanceled(Call call) { }
    }

    @MainThread
    public interface DownloadListener {
        default void onStart(DownloadInfo downloadInfo) {}
        void onProgress(DownloadInfo downloadInfo);
        void onSucceed(DownloadInfo downloadInfo);
        void onFailed(DownloadInfo downloadInfo, IOException e);
        default void onCanceled(DownloadInfo downloadInfo) {}
        default void onVerifyFailed(DownloadInfo downloadInfo, String algorithm, String expect, String got) {
            onFailed(downloadInfo, new IOException(algorithm + " verify failed: expect " + expect + ", but got " + got));
        }
    }
}
