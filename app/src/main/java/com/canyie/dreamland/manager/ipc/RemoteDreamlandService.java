package com.canyie.dreamland.manager.ipc;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;

import androidx.annotation.Nullable;

import com.canyie.dreamland.manager.core.Dreamland;
import com.canyie.dreamland.manager.utils.DLog;

/**
 * Unused now...
 * @author canyie
 */
public final class RemoteDreamlandService extends Service {
    private static final String TAG = "RemoteDreamlandService";
    private RemoteDreamlandManager manager;

    @Override public void onCreate() {
        super.onCreate();
        manager = new RemoteDreamlandManager(this);
        DLog.d(TAG, "RemoteDreamlandService.onCreate()");
    }

    @Override public int onStartCommand(Intent intent, int flags, int startId) {
        DLog.i(TAG, "RemoteDreamlandService.onStartCommand()");
        super.onStartCommand(intent, flags, startId);
        Dreamland.onIPCServiceStart();
        return START_STICKY;
    }

    @Override public IBinder onBind(Intent intent) {
        return manager;
    }

    @Override public boolean onUnbind(Intent intent) {
        return super.onUnbind(intent);
    }

    @Override public void onDestroy() {
        Dreamland.onIPCServiceDestroy();
        super.onDestroy();
    }
}
