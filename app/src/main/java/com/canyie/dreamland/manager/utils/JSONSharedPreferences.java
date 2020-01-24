package com.canyie.dreamland.manager.utils;

import android.content.SharedPreferences;

import androidx.annotation.Nullable;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

import java.io.File;
import java.util.Map;
import java.util.Set;

/**
 * @author canyie
 * @date 2019/12/16.
 */
public class JSONSharedPreferences implements SharedPreferences {
    private static final Gson GSON = new Gson();
    private Map<String, ValueBean> mMap;

    public JSONSharedPreferences(String json) {
        mMap = GSON.fromJson(json, new TypeToken<Map<String, ValueBean>>() {}.getType());
    }

    public Map<String, ?> getAll() {
        return null;
    }

    @Nullable @Override public String getString(String key, @Nullable String defValue) {
        return defValue;
    }

    @Nullable @Override
    public Set<String> getStringSet(String key, @Nullable Set<String> defValues) {
        return null;
    }

    @Override public int getInt(String key, int defValue) {
        ValueBean v = mMap.get(key);
        return v != null ? v.getInt() : defValue;
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
        return new EditorImpl();
    }

    @Override
    public void registerOnSharedPreferenceChangeListener(OnSharedPreferenceChangeListener listener) {

    }

    @Override
    public void unregisterOnSharedPreferenceChangeListener(OnSharedPreferenceChangeListener listener) {

    }

    class EditorImpl implements SharedPreferences.Editor {

        @Override public Editor putString(String key, @Nullable String value) {
            return null;
        }

        @Override public Editor putStringSet(String key, @Nullable Set<String> values) {
            unsupport();
            return null;
        }

        @Override public Editor putInt(String key, int value) {
            unsupport();
            return null;
        }

        @Override public Editor putLong(String key, long value) {
            unsupport();
            return null;
        }

        @Override public Editor putFloat(String key, float value) {
            unsupport();
            return null;
        }

        @Override public Editor putBoolean(String key, boolean value) {
            unsupport();
            return null;
        }

        @Override public Editor remove(String key) {
            unsupport();
            return null;
        }

        @Override public Editor clear() {
            unsupport();
            return null;
        }

        @Override public boolean commit() {
            return false;
        }

        @Override public void apply() {
            unsupport();
        }
    }

    static class ValueBean {
        public String type;
        public String value;
        private transient Object rawValue;
        private transient boolean resolved;

        public int getInt() {
            ensureValueResolved();
            return (Integer) rawValue;
        }

        private void ensureValueResolved() {
            if (!resolved) {
                resolveValue();
            }
        }

        private void resolveValue() {
            switch (type) {
                case "null":
                    rawValue = null;
                    break;
                case "int":
                    rawValue = Integer.valueOf(value);
                    break;
                case "string":
                    rawValue = value;
                    break;
                default:
                    throw new UnsupportedOperationException("Unsupported type " + type);
            }
            resolved = true;
        }
    }

    private static UnsupportedOperationException unsupport() {
        throw new UnsupportedOperationException("Not supported now");
    }
}
