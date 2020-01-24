package com.canyie.dreamland.manager.ui.activities;

import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AlertDialog;

import com.canyie.dreamland.manager.BuildConfig;
import com.canyie.dreamland.manager.R;
import com.canyie.dreamland.manager.utils.Intents;

import mehdi.sakout.aboutpage.AboutPage;
import mehdi.sakout.aboutpage.Element;

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
                .addGitHub("canyie")
                .addWebsite("https://canyie.github.io/", getString(R.string.about_my_blog));
        return aboutPage.create();
    }

    @Override protected void initLayout(Bundle savedInstanceState) {
        ActionBar actionBar = getSupportActionBar();
        assert actionBar != null;
        actionBar.setTitle(R.string.about);
        actionBar.setDisplayHomeAsUpEnabled(true);

        /*ImageView aboutIcon = requireView(mehdi.sakout.aboutpage.R.id.image);
        aboutIcon.setOnLongClickListener(this);*/
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
        final long qqGroup = 949888394;
        element.setOnClickListener(v -> {
            boolean success = Intents.joinQQGroup(this, qqGroup);
            if (!success) {
                toast(R.string.about_alert_qq_is_unavailable);
            }
        });
        return element;
    }

    @Override public boolean onLongClick(View v) {
        // Hum... do nothing now. What can I do?
        return false;
    }

    @Override public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
