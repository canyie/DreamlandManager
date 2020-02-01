package com.canyie.dreamland.manager;

import android.app.Application;
import android.content.Context;
import android.view.Choreographer;

import com.canyie.dreamland.manager.core.Dreamland;
import com.canyie.dreamland.manager.utils.HiddenApis;
import com.canyie.dreamland.manager.utils.RuntimeHelper;

/**
 * @author canyie
 */
public final class DreamlandApp extends Application {
    @Override protected void attachBaseContext(Context base) {
        super.attachBaseContext(base);
        AppGlobals.setApp(this);
        CrashHandler.install();
        HiddenApis.exemptAll();
    }

    @Override public void onCreate() {
        super.onCreate();
        AppGlobals.initOnce();
        Dreamland.init();
    }

    /*@Override public void onTrimMemory(int level) {
        super.onTrimMemory(level);
        switch (level) {
            case TRIM_MEMORY_UI_HIDDEN :
                break;
            case TRIM_MEMORY_RUNNING_MODERATE :
            case TRIM_MEMORY_RUNNING_LOW :
            case TRIM_MEMORY_RUNNING_CRITICAL :
                RuntimeHelper.requestGC();
                break;
            case TRIM_MEMORY_BACKGROUND :
            case TRIM_MEMORY_MODERATE :
            case TRIM_MEMORY_COMPLETE:
                RuntimeHelper.requestGC();
                RuntimeHelper.requestHeapTrim();
                break;
        }
    }*/
}
