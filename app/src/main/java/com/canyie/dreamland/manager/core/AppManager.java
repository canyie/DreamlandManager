package com.canyie.dreamland.manager.core;

import android.text.TextUtils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.canyie.dreamland.manager.utils.collections.ConcurrentHashSet;

import java.util.Set;
import java.util.StringTokenizer;

/**
 * @author canyie
 */
public final class AppManager extends BaseManager<ConcurrentHashSet<String>> {
    AppManager() {
        super("apps.list");
    }

    public boolean isEnabled(String packageName) {
        return getRawObject().contains(packageName);
    }

    public void setAppEnabled(String packageName, boolean enable) {
        Set<String> set = getRawObject();
        if (enable) {
            set.add(packageName);
        } else {
            set.remove(packageName);
        }
        notifyDataChanged();
    }

    @NonNull @Override protected String serialize(ConcurrentHashSet<String> set) {
        StringBuilder sb = new StringBuilder();
        for (String packageName : set) {
            if (packageName == null || packageName.trim().isEmpty()) continue;
            sb.append(packageName).append('\n');
        }
        return sb.toString();
    }

    @Override protected ConcurrentHashSet<String> deserialize(String str) {
        StringTokenizer st = new StringTokenizer(str, "\n");
        ConcurrentHashSet<String> set = new ConcurrentHashSet<>(Math.max((int) (st.countTokens() / .75f) + 1, 16));
        while (st.hasMoreTokens()) {
            String packageName = st.nextToken();
            if (packageName == null || packageName.trim().isEmpty()) continue;
            set.add(packageName);
        }
        return set;
    }

    @NonNull @Override protected ConcurrentHashSet<String> createEmptyObject() {
        return new ConcurrentHashSet<>();
    }
}
