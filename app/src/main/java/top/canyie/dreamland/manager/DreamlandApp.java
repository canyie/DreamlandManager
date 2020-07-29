package top.canyie.dreamland.manager;

import android.app.Application;
import android.content.Context;

import top.canyie.dreamland.manager.core.Dreamland;
import top.canyie.dreamland.manager.utils.HiddenApis;

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
