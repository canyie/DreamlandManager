package top.canyie.dreamland.manager.ui.fragments;

import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.CallSuper;
import androidx.annotation.IdRes;
import androidx.annotation.LayoutRes;
import androidx.annotation.MainThread;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.annotation.WorkerThread;
import androidx.fragment.app.Fragment;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import top.canyie.dreamland.manager.R;
import top.canyie.dreamland.manager.ui.activities.BaseActivity;
import top.canyie.dreamland.manager.utils.Preconditions;
import top.canyie.dreamland.manager.utils.Threads;
import top.canyie.dreamland.manager.utils.ToastCompat;
import top.canyie.dreamland.manager.utils.ViewFinder;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.Future;

/**
 * @author canyie
 */
public abstract class BaseFragment extends Fragment implements SwipeRefreshLayout.OnRefreshListener {
    private View mView;
    private SwipeRefreshLayout mSwipeRefreshLayout;
    private ViewFinder mViewFinder;
    private final Set<Future<?>> mFutures = new HashSet<>();

    @Nullable @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        if (mView == null || shouldRecreateView(mView)) {
            mView = createView(inflater, container, savedInstanceState);
            if (mView == null) {
                return null;
            }
            mViewFinder = ViewFinder.from(mView);
            mSwipeRefreshLayout = mViewFinder.findView(R.id.refresh_layout);
            if (mSwipeRefreshLayout != null) {
                mSwipeRefreshLayout.setColorSchemeResources(R.color.colorPrimary);
                mSwipeRefreshLayout.setOnRefreshListener(this);
            }
            initView(mView);
            loadData(mView);
        }
        return mView;
    }

    @Override public void onDetach() {
        synchronized (mFutures) {
            Threads.cancelAllFuturesLocked(mFutures);
        }
        super.onDetach();
    }

    @Nullable
    protected View createView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(getLayoutResId(), container, false);
    }

    @LayoutRes protected int getLayoutResId() {
        throw new UnsupportedOperationException();
    }

    protected boolean shouldRecreateView(View cachedView) {
        return false;
    }

    @Override public void onRefresh() {
        loadData(mView);
    }

    @NonNull public SwipeRefreshLayout getSwipeRefreshLayout() {
        Preconditions.checkState(mSwipeRefreshLayout != null, "No SwipeRefreshLayout");
        return mSwipeRefreshLayout;
    }

    public <T extends View> T findView(@IdRes int id) {
        return mViewFinder.findView(id);
    }

    public <T extends View> T requireView(@IdRes int id) {
        return mViewFinder.requireView(id);
    }

    protected void initView(@NonNull View view) {
    }

    protected void loadData(@NonNull View view) {
        if (mSwipeRefreshLayout != null) mSwipeRefreshLayout.setRefreshing(true);
        beforeLoadData();
        Runnable action = () -> {
            Object data = loadDataImpl();
            BaseActivity activity = (BaseActivity) getActivity();
            if (activity != null)
                activity.execOnUIThread(() -> updateUIForData(data));
        };
        Future<?> future = Threads.getDefaultExecutor().submit(action);
        synchronized (mFutures) {
            Threads.clearDoneFuturesLocked(mFutures);
            mFutures.add(future);
        }
    }

    protected void clearCachedView() {
        mView = null;
        mViewFinder = null;
    }

    @MainThread protected void beforeLoadData() {
    }

    @WorkerThread protected Object loadDataImpl() {
        return null;
    }

    @MainThread @CallSuper protected void updateUIForData(Object data) {
        if (mSwipeRefreshLayout != null) mSwipeRefreshLayout.setRefreshing(false);
    }

    @MainThread protected void toast(@StringRes int resId) {
        ToastCompat.showToast(requireContext(), resId);
    }

    @MainThread protected void toast(CharSequence content) {
        ToastCompat.showToast(requireContext(), content);
    }

    public boolean checkSelfPermission(String permission) {
        return requireActivity().checkSelfPermission(permission) == PackageManager.PERMISSION_GRANTED;
    }

    public boolean onSearchViewOpen(View v) {
        return false;
    }

    @Override public boolean onContextItemSelected(@NonNull MenuItem item) {
        //noinspection deprecation
        if (getUserVisibleHint()) {
            return onContextItemSelectedImpl(item);
        }
        return false;
    }

    protected boolean onContextItemSelectedImpl(@NonNull MenuItem item) {
        return super.onContextItemSelected(item);
    }
}
