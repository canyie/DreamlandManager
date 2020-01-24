package com.canyie.dreamland.manager.utils;

import android.os.Handler;
import android.os.Looper;
import android.os.Process;

import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author canyie
 */
public final class Threads {
    private static final AtomicInteger threadCount = new AtomicInteger();
    private static final ThreadFactory sThreadFactory = action ->
            new Thread(action, "Dreamland Thread #" + threadCount.getAndIncrement());

    private static final ExecutorService sDefaultExecutor = Executors.newFixedThreadPool(4, sThreadFactory);

    private static final Handler sMainThreadHandler = new Handler(Looper.getMainLooper());

    private Threads() {
    }

    public static ThreadFactory getThreadFactory() {
        return sThreadFactory;
    }

    public static ExecutorService getDefaultExecutor() {
        return sDefaultExecutor;
    }

    public static boolean isMainThread() {
        return Looper.myLooper() == Looper.getMainLooper();
    }

    public static Handler getMainThreadHandler() {
        return sMainThreadHandler;
    }

    public static void execOnMainThread(Runnable action) {
        if (isMainThread()) {
            action.run();
        } else {
            sMainThreadHandler.post(action);
        }
    }

    public static void throwIfInterrupted() throws InterruptedException {
        if (Thread.interrupted()) {
            throw new InterruptedException("Thread interrupted");
        }
    }

    public static void clearDoneFuturesLocked(Set<Future<?>> futures) {
        if (futures.isEmpty()) return;
        for (Iterator<Future<?>> iterator = futures.iterator();iterator.hasNext();) {
            if (iterator.next().isDone()) iterator.remove();
        }
    }

    public static void cancelAllFuturesLocked(Set<Future<?>> futures) {
        if (futures.isEmpty()) return;
        for (Future<?> future : futures) {
            if (future.isDone()) continue;
            future.cancel(true);
        }
        futures.clear();
    }

    public static void setForeground() {
        Process.setThreadPriority(Process.THREAD_PRIORITY_FOREGROUND);
    }
}
