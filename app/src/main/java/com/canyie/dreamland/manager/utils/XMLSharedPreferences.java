package com.canyie.dreamland.manager.utils;

import android.content.SharedPreferences;
import android.util.Xml;

import androidx.annotation.Nullable;

import org.xmlpull.v1.XmlPullParser;

import java.util.Map;
import java.util.Set;

/**
 * @author canyie
 * @date 2019/12/17.
 */
public class XMLSharedPreferences implements SharedPreferences {

    static {
        XmlPullParser parser = Xml.newPullParser();

    }

    @Override public Map<String, ?> getAll() {
        return null;
    }

    @Nullable @Override public String getString(String key, @Nullable String defValue) {
        return null;
    }

    @Nullable @Override
    public Set<String> getStringSet(String key, @Nullable Set<String> defValues) {
        return null;
    }

    @Override public int getInt(String key, int defValue) {
        return 0;
    }

    @Override public long getLong(String key, long defValue) {
        return 0;
    }

    @Override public float getFloat(String key, float defValue) {
        return 0;
    }

    @Override public boolean getBoolean(String key, boolean defValue) {
        return false;
    }

    @Override public boolean contains(String key) {
        return false;
    }

    @Override public Editor edit() {
        return null;
    }

    @Override
    public void registerOnSharedPreferenceChangeListener(OnSharedPreferenceChangeListener listener) {

    }

    @Override
    public void unregisterOnSharedPreferenceChangeListener(OnSharedPreferenceChangeListener listener) {

    }
}
