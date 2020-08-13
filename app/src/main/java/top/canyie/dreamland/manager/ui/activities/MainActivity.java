package top.canyie.dreamland.manager.ui.activities;

import android.content.Intent;
import android.os.Bundle;

import top.canyie.dreamland.manager.R;
import top.canyie.dreamland.manager.core.Dreamland;
import top.canyie.dreamland.manager.ui.adapters.MainPagerAdapter;
import top.canyie.dreamland.manager.ui.fragments.AppManagerFragment;
import top.canyie.dreamland.manager.ui.fragments.StatusFragment;
import top.canyie.dreamland.manager.ui.fragments.ModuleManagerFragment;
import top.canyie.dreamland.manager.ui.fragments.PageFragment;
import top.canyie.dreamland.manager.utils.Dialogs;
import top.canyie.dreamland.manager.utils.RootUtils;
import com.google.android.material.tabs.TabLayout;

import androidx.annotation.IdRes;
import androidx.annotation.LayoutRes;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.SearchView;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.FragmentManager;
import androidx.viewpager.widget.ViewPager;

import android.view.Menu;
import android.view.MenuItem;

public class MainActivity extends BaseActivity implements ViewPager.OnPageChangeListener, SearchView.OnQueryTextListener {
    private static final String TAG = "MainActivity";

    private ViewPager mViewPager;
    private StatusFragment mStatusFragment;
    private AppManagerFragment mAppManagerFragment;
    private ModuleManagerFragment mModuleManagerFragment;
    private PageFragment mCurrentFragment;
    private SearchView mSearchView;
    private SearchView.SearchAutoComplete mSearchAutoComplete;
    private MenuItem mSearchMenuItem;

    @Override protected void initLayout(Bundle savedInstanceState) {
        Toolbar toolbar = requireView(R.id.toolbar);
        setSupportActionBar(toolbar);

        mViewPager = requireView(R.id.pager);
        FragmentManager fm = getSupportFragmentManager();
        if (savedInstanceState != null) {
            // Recreating activity
            final String tagPrefix = "android:switcher:" + R.id.pager + ":"; // See FragmentPagerAdapter.makeFragmentName
            mStatusFragment = (StatusFragment) fm.findFragmentByTag(tagPrefix + 0);
            mAppManagerFragment = (AppManagerFragment) fm.findFragmentByTag(tagPrefix + 1);
            mModuleManagerFragment = (ModuleManagerFragment) fm.findFragmentByTag(tagPrefix + 2);
        }
        if (mStatusFragment == null) mStatusFragment = new StatusFragment();
        if (mAppManagerFragment == null) mAppManagerFragment = new AppManagerFragment();
        if (mModuleManagerFragment == null) mModuleManagerFragment = new ModuleManagerFragment();
        MainPagerAdapter adapter = new MainPagerAdapter(fm, mStatusFragment, mAppManagerFragment, mModuleManagerFragment);
        mViewPager.setAdapter(adapter);
        TabLayout tabLayout = requireView(R.id.tab_layout);
        tabLayout.setupWithViewPager(mViewPager);
        mViewPager.addOnPageChangeListener(this);
    }

    @Override @LayoutRes protected int getLayoutResId() {
        return R.layout.activity_main;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);

        mSearchMenuItem = menu.findItem(R.id.action_search);
        mSearchView = (SearchView) mSearchMenuItem.getActionView();
        mSearchView.setSubmitButtonEnabled(false);
        mSearchView.setOnQueryTextListener(this);
        mSearchView.setOnSearchClickListener(v -> {
            if (mCurrentFragment.onSearchViewOpen(v)) {
                mSearchView.onActionViewCollapsed();
            }
        });
        mSearchAutoComplete = mSearchView.findViewById(R.id.search_src_text);
        mSearchMenuItem.setVisible(mViewPager.getCurrentItem() != 0);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        switch (id) {
            case R.id.action_settings:
                if (Dreamland.isActive())
                    startActivity(SettingsActivity.class);
                else
                    toast(R.string.framework_not_active);
                return true;
            case R.id.action_reboot:
            case R.id.action_soft_reboot:
            case R.id.action_reboot_to_recovery:
                confirmRebootAlert(id);
                return true;
            case R.id.action_about:
                startActivity(AboutActivity.class);
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void confirmRebootAlert(@IdRes int itemId) {
        Dialogs.create(this)
                .message(R.string.reboot_confirm_alert)
                .positiveButton(R.string.yes, dialogInfo -> {
                    boolean rebootSuccess;
                    switch (itemId) {
                        case R.id.action_reboot:
                            rebootSuccess = RootUtils.reboot();
                            break;
                        case R.id.action_soft_reboot:
                            rebootSuccess = RootUtils.softReboot();
                            break;
                        case R.id.action_reboot_to_recovery:
                            rebootSuccess = RootUtils.rebootToRecovery();
                            break;
                        default:
                            throw new IllegalStateException("Unexpected item id: " + itemId);
                    }
                    if (!rebootSuccess) {
                        Dialogs.create(this)
                                .title(R.string.reboot_failed_alert_title)
                                .message(R.string.reboot_failed_alert_message)
                                .positiveButton(R.string.ok, null)
                                .showIfActivityActivated();
                    }
                })
                .negativeButton(R.string.cancel, null)
                .showIfActivityActivated();
    }

    @Override public void onBackPressed() {
        if (mSearchAutoComplete.isShown()) {
            mSearchView.onActionViewCollapsed();
            return;
        }
        super.onBackPressed();
    }

    @Override protected void onDestroy() {
        super.onDestroy();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        if (requestCode == InstallationActivity.REQUEST_CODE) {
            mStatusFragment.onRefresh();
            mAppManagerFragment.onRefresh();
            mModuleManagerFragment.onRefresh();
            return;
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
    }

    @Override public void onPageSelected(int position) {
        switch (position) {
            case 0:
                mCurrentFragment = mStatusFragment;
                if (mSearchMenuItem != null) mSearchMenuItem.setVisible(false);
                break;
            case 1:
                mCurrentFragment = mAppManagerFragment;
                if (mSearchMenuItem != null) mSearchMenuItem.setVisible(true);
                break;
            case 2:
                mCurrentFragment = mModuleManagerFragment;
                if (mSearchMenuItem != null) mSearchMenuItem.setVisible(true);
                break;
            default:
                throw new IllegalStateException("Unexpected position: " + position);
        }
    }

    @Override public void onPageScrollStateChanged(int state) {
    }

    @Override public boolean onQueryTextSubmit(String query) {
        if (mCurrentFragment instanceof SearchView.OnQueryTextListener) {
            return ((SearchView.OnQueryTextListener) mCurrentFragment).onQueryTextSubmit(query);
        }
        return false;
    }

    @Override public boolean onQueryTextChange(String newText) {
        if (mCurrentFragment instanceof SearchView.OnQueryTextListener) {
            return ((SearchView.OnQueryTextListener) mCurrentFragment).onQueryTextChange(newText);
        }
        return false;
    }
}
