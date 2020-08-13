package top.canyie.dreamland.manager.ui.activities;

import android.app.Activity;
import android.app.Service;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;

import androidx.annotation.IdRes;
import androidx.annotation.LayoutRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.appcompat.app.AppCompatActivity;

import top.canyie.dreamland.manager.utils.Threads;
import top.canyie.dreamland.manager.utils.ToastCompat;
import top.canyie.dreamland.manager.utils.ViewFinder;

/**
 * @author canyie
 */
public abstract class BaseActivity extends AppCompatActivity {
    private boolean mAutoSetContentView = true;
    private ViewFinder mViewFinder;
    private Handler mHandler;

    @Override protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mHandler = new Handler();
        if (mAutoSetContentView) {
            View contentView = createContentView();
            if (contentView != null) {
                setContentView(contentView);
            } else {
                setContentView(getLayoutResId());
            }
        }
        mViewFinder = ViewFinder.from(this);
        initLayout(savedInstanceState);
        loadData(savedInstanceState);
    }

    @Override protected void onDestroy() {
        super.onDestroy();
        mHandler.removeCallbacksAndMessages(null);
        mHandler = null;
    }

    public void setAutoSetContentView(boolean set) {
        mAutoSetContentView = set;
    }

    public Handler getHandlerForUI() {
        if (mHandler == null) {
            throw new IllegalStateException();
        }
        return mHandler;
    }

    public void execOnUIThread(Runnable action) {
        if (Threads.isMainThread()) {
            action.run();
        } else {
            getHandlerForUI().post(action);
        }
    }

    public void updateUIDelay(Runnable action, long delayMillis) {
        getHandlerForUI().postDelayed(action, delayMillis);
    }

    protected View createContentView() {
        return null;
    }

    @LayoutRes protected int getLayoutResId() {
        throw new UnsupportedOperationException();
    }

    protected void initLayout(Bundle savedInstanceState) {
    }

    protected void loadData(Bundle savedInstanceState) {
    }

    @Nullable
    public <T extends View> T findView(@IdRes int id) {
        return mViewFinder.findView(id);
    }

    @NonNull
    public <T extends View> T requireView(@IdRes int id) {
        return mViewFinder.requireView(id);
    }

    public void startActivity(Class<? extends Activity> clazz) {
        startActivity(new Intent(this, clazz));
    }

    public void startService(Class<? extends Service> clazz) {
        startService(new Intent(this, clazz));
    }

    public void toast(@StringRes int resId) {
        ToastCompat.showToast(this, resId);
    }

    public void toast(CharSequence content) {
        ToastCompat.showToast(this, content);
    }
}
