package top.canyie.dreamland.manager.utils;

import androidx.annotation.Nullable;
import androidx.preference.PreferenceDataStore;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * @author canyie
 */
public class CacheablePreferenceDataStore extends PreferenceDataStore {
    private PreferenceDataStore base;
    private Map<String, Object> cache = new HashMap<>(4);

    public CacheablePreferenceDataStore(PreferenceDataStore base) {
        this.base = base;
    }

    @Nullable @Override public synchronized String getString(String key, @Nullable String defValue) {
        if (cache.containsKey(key)) {
            return (String) cache.get(key);
        } else {
            String result = base.getString(key, defValue);
            cache.put(key, result);
            return result;
        }
    }

    @Override public synchronized void putString(String key, @Nullable String value) {
        cache.put(key, value);
        base.putString(key, value);
    }

    @Override public synchronized int getInt(String key, int defValue) {
        if (cache.containsKey(key)) {
            return (int) cache.get(key);
        } else {
            int result = base.getInt(key, defValue);
            cache.put(key, result);
            return result;
        }
    }

    @Override public synchronized void putInt(String key, int value) {
        cache.put(key, value);
        base.putInt(key, value);
    }

    @Override public synchronized boolean getBoolean(String key, boolean defValue) {
        if (cache.containsKey(key)) {
            return (boolean) cache.get(key);
        } else {
            boolean result = base.getBoolean(key, defValue);
            cache.put(key, result);
            return result;
        }
    }

    @Override public synchronized void putBoolean(String key, boolean value) {
        cache.put(key, value);
        base.putBoolean(key, value);
    }

    @Override public synchronized long getLong(String key, long defValue) {
        if (cache.containsKey(key)) {
            return (long) cache.get(key);
        } else {
            long result = base.getLong(key, defValue);
            cache.put(key, result);
            return result;
        }
    }

    @Override public synchronized void putLong(String key, long value) {
        cache.put(key, value);
        base.putLong(key, value);
    }

    @Override public synchronized float getFloat(String key, float defValue) {
        if (cache.containsKey(key)) {
            return (float) cache.get(key);
        } else {
            float result = base.getFloat(key, defValue);
            cache.put(key, result);
            return result;
        }
    }

    @Override public synchronized void putFloat(String key, float value) {
        cache.put(key, value);
        base.putFloat(key, value);
    }

    @Nullable @Override
    public synchronized Set<String> getStringSet(String key, @Nullable Set<String> defValues) {
        if (cache.containsKey(key)) {
            return (Set<String>) cache.get(key);
        } else {
            Set<String> result = base.getStringSet(key, defValues);
            cache.put(key, result);
            return result;
        }
    }

    @Override public synchronized void putStringSet(String key, @Nullable Set<String> values) {
        cache.put(key, values);
        base.putStringSet(key, values);
    }
}
