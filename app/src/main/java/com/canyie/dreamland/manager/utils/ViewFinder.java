package com.canyie.dreamland.manager.utils;

import android.app.Activity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;


import androidx.annotation.IdRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager.widget.ViewPager;

import com.canyie.dreamland.manager.utils.collections.IntHashMap;

import java.lang.ref.WeakReference;

/**
 * @author canyie
 * @date 2019/12/10.
 */
public final class ViewFinder {
    private static final String TAG = "ViewFinder";
    private View mView;
    //private IntHashMap<WeakReference<View>> mViewCache = new IntHashMap<>();

    private ViewFinder(@NonNull View view) {
        mView = view;
        //addViewsToCache(view);
    }

    public static ViewFinder from(@NonNull Activity activity) {
        return from(activity.getWindow().getDecorView());
    }

    public static ViewFinder from(@NonNull Fragment fragment) {
        View root = fragment.getView();
        if (root == null) {
            throw new IllegalStateException("No view attached to the fragment");
        }
        return from(root);
    }

    public static ViewFinder from(@NonNull View view) {
        return new ViewFinder(view);
    }

    @Nullable
    public <T extends View> T findViewFromCache(@IdRes int id) {
        /*WeakReference<View> ref = mViewCache.get(id);
        return (T) (ref != null ? ref.get() : null);*/
        return null;
    }

    @NonNull
    public <T extends View> T requireViewFromCache(@IdRes int id) {
        T view = findViewFromCache(id);
        if (view == null) {
            throw new NullPointerException("No view found by id 0x" + Integer.toHexString(id) + " in cache");
        }
        return view;
    }

    @Nullable
    public <T extends View> T findView(@IdRes int id) {
        T view = findViewFromCache(id);
        if (view == null) {
            view = mView.findViewById(id);
        }
        return view;
    }

    @NonNull
    public <T extends View> T requireView(@IdRes int id) {
        T view = findView(id);
        if (view == null) {
            throw new NullPointerException("No view found by id 0x" + Integer.toHexString(id));
        }
        return view;
    }

    public void clear() {
        //mViewCache.clear();
    }

    public void refresh() {
        /*clear();
        addViewsToCache(mView);*/
    }

    public void removeViewFromCache(@IdRes int id) {
        //mViewCache.remove(id);
    }

    private void addViewsToCache(@NonNull View view) {
        /*int id = view.getId();
        if (id != View.NO_ID) {
            mViewCache.put(id, new WeakReference<>(view));
        }

        if (view instanceof ViewGroup) {
            if (view instanceof RecyclerView || view instanceof ViewPager || view instanceof AdapterView) {
                return;
            }
            ViewGroup group = (ViewGroup) view;
            for (int i = 0;i < group.getChildCount();i++) {
                View child = group.getChildAt(i);
                addViewsToCache(child);
            }
        }*/
    }
}
