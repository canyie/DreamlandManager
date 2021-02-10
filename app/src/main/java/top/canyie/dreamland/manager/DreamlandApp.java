package top.canyie.dreamland.manager;

import android.app.Application;
import android.content.Context;

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
}
