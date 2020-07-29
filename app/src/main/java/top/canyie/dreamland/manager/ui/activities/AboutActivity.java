package top.canyie.dreamland.manager.ui.activities;

import android.app.ProgressDialog;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AlertDialog;

import top.canyie.dreamland.manager.BuildConfig;
import top.canyie.dreamland.manager.R;
import top.canyie.dreamland.manager.core.Dreamland;
import top.canyie.dreamland.manager.utils.ArtDexOptimizer;
import top.canyie.dreamland.manager.utils.DLog;
import top.canyie.dreamland.manager.utils.Dialogs;
import top.canyie.dreamland.manager.utils.RootUtils;
import top.canyie.dreamland.manager.utils.Threads;

import java.io.File;
import java.io.IOException;

import mehdi.sakout.aboutpage.AboutPage;
import mehdi.sakout.aboutpage.Element;
import top.canyie.dreamland.manager.utils.ToastCompat;

/**
 * @author canyie
 */
public class AboutActivity extends BaseActivity implements View.OnLongClickListener {
    @Override protected View createContentView() {
        AboutPage aboutPage = new AboutPage(this)
                .isRTL(false)
                .setImage(R.mipmap.ic_launcher)
                .setDescription(getString(R.string.app_description))
                .addItem(createVersionElement())
                .addItem(createThanksElement())
                .addItem(createQQGroupElement())
                .addTwitter("canyie2977")
                .addGitHub("canyie", "GitHub: canyie")
                .addWebsite("https://blog.canyie.top/", getString(R.string.about_my_blog));
        return aboutPage.create();
    }

    @Override protected void initLayout(Bundle savedInstanceState) {
        ActionBar actionBar = getSupportActionBar();
        assert actionBar != null;
        actionBar.setTitle(R.string.about);
        actionBar.setDisplayHomeAsUpEnabled(true);

        View aboutIcon = requireView(mehdi.sakout.aboutpage.R.id.image);
        aboutIcon.setOnLongClickListener(this);
    }

    private Element createVersionElement() {
        Element element = new Element();
        element.setTitle(getString(R.string.about_version, BuildConfig.VERSION_NAME, BuildConfig.VERSION_CODE));
        element.setIconDrawable(R.drawable.ic_info);
        // TODO: Click to check for updates
        return element;
    }

    private Element createThanksElement() {
        Element element = new Element();
        element.setTitle(getString(R.string.about_thanks));
        element.setIconDrawable(R.drawable.ic_thanks);
        element.setOnClickListener(v -> {
            if (isFinishing() || isDestroyed()) return;
            new AlertDialog.Builder(this)
                    .setTitle(R.string.about_thanks)
                    .setMessage(R.string.about_thanks_alert_content)
                    .setPositiveButton(R.string.ok, null)
                    .show();
        });
        return element;
    }

    private Element createQQGroupElement() {
        Element element = new Element();
        element.setTitle(getString(R.string.about_join_qq_group));
        element.setIconDrawable(R.drawable.ic_qq); // FIXME: This icon appears filled with black.

        element.setOnClickListener(v -> {
            final String qqGroupKey = "eSLRhvqWfeIuxciJyvo8Lu-On3tKgL2l";
            Intent intent = new Intent();
            intent.setData(Uri.parse("mqqopensdkapi://bizAgent/qm/qr?url=http%3A%2F%2Fqm.qq.com%2Fcgi-bin%2Fqm%2Fqr%3Ffrom%3Dapp%26p%3Dandroid%26k%3D" + qqGroupKey));
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

            try {
                startActivity(intent);
                ToastCompat.showToast(this, getString(R.string.answer_to_qq_group_question));
            } catch (ActivityNotFoundException e) {
                toast(R.string.about_alert_qq_is_unavailable);
            }
        });
        return element;
    }

    @Override public boolean onLongClick(View v) {
        if (Dreamland.CORE_JAR_FILE.exists()) {
            Dialogs.create(this)
                    .title(R.string.dex2oat_warning_alert_title)
                    .message(R.string.dex2oat_warning_alert_content)
                    .positiveButton(R.string.str_continue, dialogInfo -> startDex2OatAndShowProgressDialog())
                    .negativeButton(R.string.cancel, null)
                    .showIfActivityActivated();
            return true;
        }
        return false;
    }

    void startDex2OatAndShowProgressDialog() {
        if (isFinishing() || isDestroyed()) return;
        ProgressDialog progressDialog = ProgressDialog.show(this, null, getString(R.string.dex2oat_running_alert), true, false);
        Threads.getDefaultExecutor().execute(() -> {
            String oatFilename = ArtDexOptimizer.getOatNameInDalvikCache(Dreamland.CORE_JAR_FILE.getAbsolutePath());
            File cacheOatFile = new File(getCacheDir(), oatFilename);
            String cacheOatPath = cacheOatFile.getAbsolutePath();
            boolean success;
            try {
                ArtDexOptimizer.compile(Dreamland.CORE_JAR_FILE.getAbsolutePath(), cacheOatPath);
                RootUtils.moveFile(cacheOatPath, ArtDexOptimizer.getOatPathInDalvikCache(oatFilename));
                success = true;
            } catch (IOException e) {
                DLog.e("ArtDexOptimizer", "Failed to compile core jar and move oat file to dalvik-cache", e);
                success = false;
            } finally {
                // noinspection ResultOfMethodCallIgnored
                cacheOatFile.delete();
            }
            final boolean finalSuccess = success;
            Threads.execOnMainThread(() -> {
                progressDialog.dismiss();
                toast(finalSuccess ? R.string.dex2oat_success : R.string.dex2oat_failed);
            });
        });
    }

    @Override public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
