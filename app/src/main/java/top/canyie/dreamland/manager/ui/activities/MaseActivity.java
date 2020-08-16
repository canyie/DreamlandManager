package top.canyie.dreamland.manager.ui.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.widget.SearchView;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.concurrent.Future;

import top.canyie.dreamland.manager.AppConstants;
import top.canyie.dreamland.manager.R;
import top.canyie.dreamland.manager.core.Dreamland;
import top.canyie.dreamland.manager.core.MasData;
import top.canyie.dreamland.manager.ui.adapters.MaseListAdapter;
import top.canyie.dreamland.manager.utils.Dialogs;
import top.canyie.dreamland.manager.utils.Threads;

/**
 * @author canyie
 */
public class MaseActivity extends BaseActivity implements MaseListAdapter.OnStateChangeListener, SearchView.OnQueryTextListener {
    private String mModulePackageName;
    private RecyclerView mRecyclerView;
    private MaseListAdapter mAdapter;
    private Future<?> mTask;
    private MasData mData;
    private boolean mLoading = true;
    private boolean mStateChanged;
    private MenuItem mSaveMenuItem;
    private SearchView mSearchView;
    private SearchView.SearchAutoComplete mSearchAutoComplete;

    @Override protected void initLayout(Bundle savedInstanceState) {
        Intent sourceIntent = getIntent();
        String moduleName = sourceIntent.getStringExtra(AppConstants.KEY_MODULE_NAME);
        mModulePackageName = sourceIntent.getStringExtra(AppConstants.KEY_MODULE_PACKAGE);
        ActionBar actionBar = getSupportActionBar();
        assert actionBar != null;
        actionBar.setTitle(R.string.module_activation_scope);
        actionBar.setSubtitle(moduleName);
        actionBar.setDisplayHomeAsUpEnabled(true);

        mRecyclerView = requireView(R.id.list);
        mRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        mRecyclerView.setAdapter(mAdapter = new MaseListAdapter(this, this));
        Dialogs.alertForConfig(this, R.string.mas_alert, AppConstants.SP_KEY_SHOW_ALERT_FOR_MAS);
    }

    @Override protected void loadData(Bundle savedInstanceState) {
        mTask = Threads.getDefaultExecutor().submit(() -> {
            try {
                MasData data = Dreamland.getMasDataFor(mModulePackageName);
                Threads.execOnMainThread(() -> onData(data));
            } catch (InterruptedException ignored) {
                // Interrupted. Ignore.
            }
        });
    }

    void onData(MasData data) {
        mLoading = false;
        mTask = null;
        if (isFinishing() || isDestroyed()) return;
        mData = data;
        mAdapter.setData(data.enabled, data.apps);
        requireView(R.id.loading_progressbar).setVisibility(View.GONE);
        mRecyclerView.setVisibility(View.VISIBLE);
    }

    @Override protected int getLayoutResId() {
        return R.layout.activity_mase;
    }

    @Override public void onChanged() {
        if (mStateChanged) return;
        mStateChanged = true;
        mSaveMenuItem.setEnabled(true);
        mSaveMenuItem.getIcon().mutate().setAlpha(255);
    }

    @Override public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_mase, menu);
        mSaveMenuItem = menu.findItem(R.id.save);
        MenuItem searchMenuItem = menu.findItem(R.id.action_search);
        mSearchView = (SearchView) searchMenuItem.getActionView();
        mSearchView.setSubmitButtonEnabled(false);
        mSearchView.setOnQueryTextListener(this);
        mSearchView.setOnSearchClickListener(v -> {
            if (mLoading) {
                toast(R.string.alert_wait_loading_complete);
                mSearchView.onActionViewCollapsed();
            }
        });
        mSearchAutoComplete = mSearchView.findViewById(R.id.search_src_text);
        return true;
    }

    @Override public void onBackPressed() {
        if (mSearchAutoComplete.isShown()) {
            mSearchView.onActionViewCollapsed();
            return;
        }
        if (mStateChanged) {
            Dialogs.create(this)
                    .title(R.string.mas_exit_alert)
                    .positiveButton(R.string.yes, info -> super.onBackPressed())
                    .negativeButton(R.string.cancel, null)
                    .neutralButton(R.string.save_and_exit, info -> saveChangesAndExit())
                    .showIfActivityActivated();
            return;
        }
        if (mTask != null)
            mTask.cancel(true);
        super.onBackPressed();
    }

    @Override public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        switch (item.getItemId()) {
            case R.id.save:
                saveChangesAndExit();
                return true;
            case android.R.id.home:
                onBackPressed();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void saveChangesAndExit() {
        mData.enabled = mAdapter.isMasEnabled();
        Dreamland.setMasDataFor(mModulePackageName, mData);
        toast(R.string.changes_saved);
        finish();
    }

    @Override public boolean onQueryTextSubmit(String query) {
        mAdapter.getFilter().filter(query);
        return true;
    }

    @Override public boolean onQueryTextChange(String newText) {
        mAdapter.getFilter().filter(newText);
        return true;
    }
}
