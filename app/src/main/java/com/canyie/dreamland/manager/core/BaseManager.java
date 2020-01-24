package com.canyie.dreamland.manager.core;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.canyie.dreamland.manager.utils.DLog;
import com.canyie.dreamland.manager.utils.FileUtils;
import com.canyie.dreamland.manager.utils.Threads;

import java.io.File;
import java.io.IOException;

/**
 * @author canyie
 * @date 2020/1/2.
 */
@SuppressWarnings("WeakerAccess")
public abstract class BaseManager<T> {
    private static final String TAG = "BaseManager";
    private final File mFile;
    private final File mBackupFile;
    private final Object mLock = new Object();
    private final Object mWritingToDiskLock = new Object();
    private T mObject;
    private volatile boolean mLoaded;

    protected BaseManager(String filename) {
        mFile = new File(Dreamland.BASE_DIR, filename);
        mBackupFile = new File(Dreamland.BASE_DIR,filename + ".bak");
    }

    public File getFile() {
        return mFile;
    }

    public File getBackupFile() {
        return mBackupFile;
    }

    protected void startLoad() {
        new Thread(this::loadFromDisk, getClass().getName()  + "-load").start();
    }

    @NonNull protected T getRawObject() {
        ensureDataLoaded();
        return mObject;
    }

    @NonNull protected abstract String serialize(T obj);
    @Nullable protected abstract T deserialize(String str);
    @NonNull protected abstract T createEmptyObject();

    protected void notifyDataChanged() {
        enqueueDiskWrite(mObject);
    }

    private void loadFromDisk() {
        T obj = readObjectFromDisk();
        synchronized (mLock) {
            this.mObject = obj;
            mLoaded = true;
            mLock.notifyAll();
        }
    }

    @NonNull protected T readObjectFromDisk() {
        T obj = null;
        try {
            obj = readFromDisk();
        } catch (IOException e) {
            DLog.e(TAG, "!!! Failed to read " + mFile.getAbsolutePath() + "  !!!", e);
        }
        if (obj == null) {
            obj = createEmptyObject();
        }
        return obj;
    }

    @Nullable private T readFromDisk() throws IOException {
        restoreFile();
        if (!mFile.exists()) {
            return null;
        }
        String content = FileUtils.readAllString(mFile);
        return deserialize(content);
    }

    private void enqueueDiskWrite(final T obj) {
        Runnable action = () -> {
            synchronized (mWritingToDiskLock) {
                try {
                    writeToFile(obj);
                } catch (IOException e) {
                    DLog.e(TAG, "!!! Failed to write " + mFile.getAbsolutePath() + " !!!", e);
                }
            }
        };
        Threads.getDefaultExecutor().execute(action);
    }

    private void writeToFile(T obj) throws IOException {
        backup();
        String content = serialize(obj);
        FileUtils.write(mFile, content);
        makeWorldReadable(mFile);
        deleteBackupFile();
    }

    private void backup() {
        if (mBackupFile.exists()) {
            DLog.e(TAG, "Don't backup %s: backup file is already exists. Did the last write fail?", mFile.getAbsolutePath());
            makeWorldReadable(mBackupFile);
            mFile.delete();
            return;
        }
        if (!mFile.exists()) {
            return;
        }
        if (!mFile.renameTo(mBackupFile)) {
            DLog.e(TAG, "Failed to backup %s: rename file failed.", mFile.getAbsolutePath());
            return;
        }
        makeWorldReadable(mBackupFile);
    }

    private void deleteBackupFile() {
        if (mBackupFile.delete() || !mBackupFile.exists()) {
            return;
        }
        DLog.e(TAG, "Failed to delete backup file %s.", mBackupFile.getAbsolutePath());
    }

    private void restoreFile() {
        if (mBackupFile.exists()) {
            DLog.e(TAG, "backup file %s is exists, did the last write fail?", mBackupFile.getAbsolutePath());
            if (!mFile.delete() && mFile.exists()) {
                DLog.e(TAG, "Delete file failed... ");
            }
            if (!mBackupFile.renameTo(mFile)) {
                DLog.e(TAG, "Restore file failed: rename file failed.");
                return;
            }
            makeWorldReadable(mFile);
        }
    }

    public void ensureDataLoaded() {
        if (mLoaded) return;
        synchronized (mLock) {
            if (mLoaded) return;
            boolean interrupted = false;
            try {
                while (!mLoaded) {
                    try {
                        mLock.wait();
                    } catch (InterruptedException e) {
                        interrupted = true;
                    }
                }
            } finally {
                if (interrupted)
                    Thread.currentThread().interrupt();
            }
        }
    }

    private void makeWorldReadable(File file) {
        /* rw-r--r-- */
        final int mode = FileUtils.S_IRUSR | FileUtils.S_IWUSR
                | FileUtils.S_IRGRP
                | FileUtils.S_IROTH;

        FileUtils.setPermissions(file.getAbsolutePath(), mode);
    }
}