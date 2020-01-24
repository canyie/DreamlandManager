package com.canyie.dreamland.manager.ui.activities;

import android.os.Bundle;

import com.canyie.dreamland.manager.AppGlobals;
import com.canyie.dreamland.manager.BuildConfig;
import com.canyie.dreamland.manager.R;
import com.canyie.dreamland.manager.core.Dreamland;
import com.canyie.dreamland.manager.core.installation.FrameworkZipProperties;
import com.canyie.dreamland.manager.core.installation.InstallListener;
import com.canyie.dreamland.manager.core.installation.InstallationException;
import com.canyie.dreamland.manager.ipc.RemoteDreamlandService;
import com.canyie.dreamland.manager.ui.adapters.MainPagerAdapter;
import com.canyie.dreamland.manager.ui.fragments.AppManagerFragment;
import com.canyie.dreamland.manager.ui.fragments.StatusFragment;
import com.canyie.dreamland.manager.ui.fragments.ModuleManagerFragment;
import com.canyie.dreamland.manager.ui.fragments.PageFragment;
import com.canyie.dreamland.manager.utils.DLog;
import com.canyie.dreamland.manager.utils.Dialogs;
import com.canyie.dreamland.manager.utils.OkHttp;
import com.canyie.dreamland.manager.utils.RootUtils;
import com.canyie.dreamland.manager.utils.Shell;
import com.canyie.dreamland.manager.utils.Threads;
import com.google.android.material.tabs.TabLayout;

import androidx.annotation.IdRes;
import androidx.annotation.LayoutRes;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.SearchView;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.FragmentManager;
import androidx.viewpager.widget.ViewPager;

import android.os.Process;
import android.text.Editable;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;

import java.io.File;
import java.io.IOException;

import okhttp3.Call;

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

    @Override protected void loadData(Bundle savedInstanceState) {
        /*ModuleManager m = Dreamland.getModuleManager();
        Map<String, RemoteModuleInfo> modules = m.getEnabledModules();
        DLog.e(TAG, "modules: %s", modules);
        modules.put("test-pk", new RemoteModuleInfo("name", "path"));
        m.applyEnabledModules(modules);*/

        OkHttp.get("http://vaexposed.weishu.me/update.json", this, new OkHttp.Callback<UpdateInfo>() {
            @Override public void onSucceed(Call call, UpdateInfo data) {
                toast("title:" + data.title);
            }

            @Override public void onFailed(Call call, IOException e) {
                e.printStackTrace();
                toast("获取失败");
            }

            @Override public void onCanceled(Call call) {
                toast("请求已取消");
            }
        });
        File dest = new File(getExternalFilesDir("download"), "iapp.apk");

        /*OkHttp.download("https://ip2.oss-cn-shanghai.aliyuncs.com/app/iApp.Yuv5.2019.12.30.apk", dest, new OkHttp.DownloadListener() {
            @Override public void onStart(OkHttp.DownloadInfo downloadInfo) {
                DLog.e(TAG, "下载开始: Call " + downloadInfo.call + " 下载到 " + downloadInfo.dest);
                DLog.e(TAG, "总大小 " + downloadInfo.totalSize + " 是否未知 " + downloadInfo.isUnknownSize);
            }

            @Override public void onProgress(OkHttp.DownloadInfo downloadInfo) {
                DLog.e(TAG, "onProgress: " + downloadInfo.progress);
                DLog.e(TAG, "Downloaded " + downloadInfo.downloadedSize + " B");
            }

            @Override public void onSucceed(OkHttp.DownloadInfo downloadInfo) {
                DLog.e(TAG, "Download succeed! total " + downloadInfo.totalSize + " downloaded " + downloadInfo.downloadedSize);
            }

            @Override public void onFailed(OkHttp.DownloadInfo downloadInfo, IOException e) {
                DLog.e(TAG, "Download failed");
            }

            @Override public void onCanceled(OkHttp.DownloadInfo downloadInfo) {
                DLog.e(TAG, "Download canceled.");
            }
        });*/
        Shell.setAllowExecOnMainThread(true);
        Dreamland.getInstaller().install(new File("/sdcard/framework.zip"), new InstallListener() {
            @Override public void onPhase(int phase) {
                DLog.e(TAG, "onPhase %d", phase);
            }

            @Override public void onLine(String line) {
                DLog.e(TAG, "onLine: " + line);
            }

            @Override public void onErrorLine(String line) {
                DLog.e(TAG, "onErrorLine: " + line);
            }

            @Override public void onDone() {
                DLog.e(TAG, "Exec done.");
            }

            @Override public void onError(InstallationException e) {
                DLog.e(TAG, "onError", e);
            }
        });
        /*Shell.Result r = null;
        try {
            r = Shell.su().add("echo \"Test Output\"").add("echo \"Test Error\" >&2").start();
            DLog.e(TAG, "ERROR----" + r.readAllError());
            DLog.e(TAG, "OUTPUT----" + r.readAll());
        } catch (IOException e) {
            e.printStackTrace();
        }*/
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

    static class UpdateInfo {
        public String url2;
        public String versionCode2;
        public String updateMessage2;
        public String title;
    }
}
