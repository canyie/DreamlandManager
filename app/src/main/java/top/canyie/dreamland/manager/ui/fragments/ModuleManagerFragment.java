package top.canyie.dreamland.manager.ui.fragments;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.util.Log;
import android.view.ContextMenu;
import android.view.MenuItem;
import android.view.View;
import android.widget.CheckBox;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.SearchView;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import top.canyie.dreamland.manager.R;
import top.canyie.dreamland.manager.core.Dreamland;
import top.canyie.dreamland.manager.core.ModuleInfo;
import top.canyie.dreamland.manager.ui.activities.MaseActivity;
import top.canyie.dreamland.manager.ui.adapters.ModuleListAdapter;
import top.canyie.dreamland.manager.ui.widgets.CMRecyclerView;
import top.canyie.dreamland.manager.AppConstants;
import top.canyie.dreamland.manager.AppGlobals;
import top.canyie.dreamland.manager.utils.Dialogs;
import top.canyie.dreamland.manager.utils.Intents;

import java.util.List;
import java.util.Set;

/**
 * @author canyie
 */
public class ModuleManagerFragment extends PageFragment implements SearchView.OnQueryTextListener, ModuleListAdapter.OnModuleStateChangedListener {
    private ModuleListAdapter mAdapter;
    private boolean mLoading;
    private ModuleInfo mCurrentSelectedModule;
    public ModuleManagerFragment() {
        super(R.string.modules);
    }

    @Override protected int getLayoutResId() {
        return R.layout.fragment_moduleslist;
    }

    @Override protected void initView(@NonNull View view) {
        Context context = requireContext();
        RecyclerView recyclerView = requireView(R.id.modules_list);
        recyclerView.setLayoutManager(new LinearLayoutManager(context));
        mAdapter = new ModuleListAdapter(context);
        mAdapter.setOnModuleStateChangedListener(this);
        recyclerView.setAdapter(mAdapter);
        registerForContextMenu(recyclerView);
    }

    @Override protected void beforeLoadData() {
        mLoading = true;
    }

    @Override protected Object loadDataImpl() {
        return Dreamland.getModuleInfos(requireContext());
    }

    @SuppressWarnings("unchecked") @Override protected void updateUIForData(Object data) {
        mAdapter.setModules((List<ModuleInfo>) data);
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
        mCurrentSelectedModule = mAdapter.getModuleInfoForPosition(rawMenuInfo.position);
        menu.setHeaderTitle(mCurrentSelectedModule.name);
        requireActivity().getMenuInflater().inflate(R.menu.menu_module_manage, menu);
    }

    @Override public boolean onContextItemSelectedImpl(@NonNull MenuItem item) {
        Context context = requireContext();
        switch (item.getItemId()) {
            case R.id.module_action_launch:
                if (!Intents.openModuleSettings(context, mCurrentSelectedModule.packageName)) {
                    toast(R.string.alert_module_cannot_open);
                }
                return true;
            case R.id.module_action_mas:
                if (Dreamland.isActive()) {
                    Intent intent = new Intent(requireContext(), MaseActivity.class);
                    intent.putExtra(AppConstants.KEY_MODULE_NAME, mCurrentSelectedModule.name);
                    intent.putExtra(AppConstants.KEY_MODULE_PACKAGE, mCurrentSelectedModule.packageName);
                    Set<String> defaultScope = mCurrentSelectedModule.getDefaultScopeSet();
                    if (defaultScope != null)
                        intent.putExtra(AppConstants.KEY_MODULE_DEFAULT_SCOPE, defaultScope.toArray(new String[defaultScope.size()]));
                    startActivity(intent);
                } else {
                    toast(R.string.framework_not_active);
                }
                return true;
            case R.id.module_action_info:
                Intents.openAppDetailsSettings(context, mCurrentSelectedModule.packageName);
                return true;
            case R.id.module_action_uninstall:
                Intents.uninstallApp(context, mCurrentSelectedModule.packageName);
                return true;
        }
        return super.onContextItemSelected(item);
    }

    @Override public void onModuleStateChanged() {
        Activity activity = getActivity();
        if (activity != null) {
            Dialogs.alertForConfig(activity, R.string.module_state_changed_alert_content,
                    AppConstants.SP_KEY_SHOW_DIALOG_WHEN_MODULE_STATE_CHANGED);
        }
    }
}
