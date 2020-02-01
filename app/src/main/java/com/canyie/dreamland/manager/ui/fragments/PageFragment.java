package com.canyie.dreamland.manager.ui.fragments;

import androidx.annotation.Nullable;
import androidx.annotation.StringRes;

import com.canyie.dreamland.manager.AppGlobals;

/**
 * @author canyie
 */
public abstract class PageFragment extends BaseFragment {
    private CharSequence mTitle;
    public PageFragment() {
    }

    public PageFragment(@StringRes int titleId) {
        this(AppGlobals.getApp().getString(titleId));
    }

    public PageFragment(@Nullable CharSequence title) {
        this.mTitle = title;
    }

    @Nullable
    public CharSequence getTitle() {
        return mTitle;
    }

    public void setTitle(@Nullable CharSequence mTitle) {
        this.mTitle = mTitle;
    }
}
