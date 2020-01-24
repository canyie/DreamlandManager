package com.canyie.dreamland.manager;

import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;

import androidx.annotation.NonNull;

import com.canyie.dreamland.manager.utils.Preconditions;
import com.google.gson.Gson;

/**
 * @author canyie
 */
public final class AppGlobals {
    private static final Gson GSON = new Gson();
    private static Application APP;
    private static SharedPreferences defaultConfigSP;
    private AppGlobals() {
    }

    public static Gson getGson() {
        return GSON;
    }

    public static void setApp (@NonNull Application app) {
        APP = app;
    }

    public static Application getApp() {
        Preconditions.checkState(APP != null, "App context not set");
        return APP;
    }

    public static void initOnce() {
        Preconditions.checkState(APP != null, "The application context must be set before AppGlobals.initOnce().");
        defaultConfigSP = APP.getSharedPreferences("configs", Context.MODE_PRIVATE);
    }

    @NonNull public static SharedPreferences getDefaultConfigSP() {
        Preconditions.checkState(defaultConfigSP != null);
        return defaultConfigSP;
    }
}
