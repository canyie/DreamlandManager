package top.canyie.dreamland.manager.ui.fragments;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.view.ContextMenu;
import android.view.MenuItem;
import android.view.View;
import android.widget.CheckBox;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.SearchView;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import top.canyie.dreamland.manager.AppConstants;
import top.canyie.dreamland.manager.AppGlobals;
import top.canyie.dreamland.manager.R;
import top.canyie.dreamland.manager.core.AppInfo;
import top.canyie.dreamland.manager.core.Dreamland;
import top.canyie.dreamland.manager.ui.adapters.AppListAdapter;
import top.canyie.dreamland.manager.ui.widgets.CMRecyclerView;
import top.canyie.dreamland.manager.utils.Dialogs;
import top.canyie.dreamland.manager.utils.Intents;
import top.canyie.dreamland.manager.utils.RootUtils;

import java.util.List;

/**
 * @author canyie
 */
public class AppManagerFragment extends PageFragment implements SearchView.OnQueryTextListener, AppListAdapter.OnAppStateChangedListener {
    private AppListAdapter mAdapter;
    private boolean mLoading;
    private AppInfo mCurrentSelectedApp;

    public AppManagerFragment() {
        super(R.string.applications);
    }

    @Override protected int getLayoutResId() {
        return R.layout.fragment_appslist;
    }

    @Override protected void initView(@NonNull View view) {
        Context context = requireContext();
        RecyclerView recyclerView = requireView(R.id.apps_list);
        recyclerView.setLayoutManager(new LinearLayoutManager(context));
        mAdapter = new AppListAdapter(context);
        mAdapter.setOnAppStateChangedListener(this);
        recyclerView.setAdapter(mAdapter);
        registerForContextMenu(recyclerView);
    }

    @Override protected void beforeLoadData() {
        mLoading = true;
    }

    @Override protected List<AppInfo> loadDataImpl() {
        return Dreamland.getAppInfos(requireContext());
    }

    @SuppressWarnings("unchecked") @Override protected void updateUIForData(Object data) {
        mAdapter.setApps((List<AppInfo>) data);
        super.updateUIForData(data);
        mLoading = false;
    }

    @Override public boolean onSearchViewOpen(View v) {
        if (mLoading) {
            toast(R.string.alert_wait_loading_complete);
            return true;
        }
        return super.onSearchViewOpen(v);
    }

    @Override public boolean onQueryTextChange(String newText) {
        mAdapter.getFilter().filter(newText);
        return true;
    }

    @Override public boolean onQueryTextSubmit(String query) {
        mAdapter.getFilter().filter(query);
        return true;
    }

    @Override
    public void onCreateContextMenu(@NonNull ContextMenu menu, @NonNull View v, @Nullable ContextMenu.ContextMenuInfo menuInfo) {
        assert menuInfo != null;
        CMRecyclerView.CMContextMenuInfo rawMenuInfo = (CMRecyclerView.CMContextMenuInfo) menuInfo;
        mCurrentSelectedApp = mAdapter.getAppInfoForPosition(rawMenuInfo.position);
        menu.setHeaderTitle(mCurrentSelectedApp.name);
        requireActivity().getMenuInflater().inflate(R.menu.menu_app_manage, menu);
    }

    @Override public boolean onContextItemSelected(@NonNull MenuItem item) {
        switch (item.getItemId()) {
            case R.id.app_action_launch:
                if (!Intents.openAppUserInterface(requireContext(), mCurrentSelectedApp.packageName)) {
                    toast(R.string.alert_app_cannot_open);
                }
                return true;
            case R.id.app_action_info:
                Intents.openAppDetailsSettings(requireContext(), mCurrentSelectedApp.packageName);
                return true;
            case R.id.app_action_force_stop:
                RootUtils.forceStopAppAsync(mCurrentSelectedApp.packageName);
                return true;
        }
        return super.onContextItemSelected(item);
    }

    @Override public void onAppStateChanged() {
        Activity activity = getActivity();
        if (activity != null) {
            Dialogs.alertForConfig(activity, R.string.app_state_changed_alert_content,
                    AppConstants.SP_KEY_SHOW_DIALOG_WHEN_APP_STATE_CHANGED);
        }
    }
}
