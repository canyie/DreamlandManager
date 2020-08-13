package top.canyie.dreamland.manager.ui.activities;

import android.os.Bundle;
import android.view.MenuItem;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.fragment.app.FragmentManager;

import top.canyie.dreamland.manager.R;
import top.canyie.dreamland.manager.ui.fragments.SettingsFragment;

/**
 * @author canyie
 */
public class SettingsActivity extends BaseActivity {
    public SettingsActivity() {
        setAutoSetContentView(false);
    }

    @Override protected void initLayout(Bundle savedInstanceState) {
        ActionBar actionBar = getSupportActionBar();
        assert actionBar != null;
        actionBar.setTitle(R.string.settings);
        actionBar.setDisplayHomeAsUpEnabled(true);

        FragmentManager fm = getSupportFragmentManager();
        final String tag = "settings_fragment";
        if (fm.findFragmentByTag(tag) == null) {
            fm.beginTransaction()
                    .add(android.R.id.content, new SettingsFragment(), tag)
                    .commit();
        }
    }

    @Override public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
