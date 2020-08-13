package top.canyie.dreamland.manager.ui.fragments;

import android.os.Bundle;

import androidx.preference.Preference;
import androidx.preference.PreferenceDataStore;
import androidx.preference.PreferenceFragmentCompat;

import top.canyie.dreamland.manager.R;
import top.canyie.dreamland.manager.core.Dreamland;
import top.canyie.dreamland.manager.utils.CacheablePreferenceDataStore;
import top.canyie.dreamland.manager.utils.Processes;

/**
 * @author canyie
 */
public class SettingsFragment extends PreferenceFragmentCompat {
    private static final String TAG = "SettingsFragment";

    @Override public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        getPreferenceManager().setPreferenceDataStore(new CacheablePreferenceDataStore(SettingsDataStore.INSTANCE));
        setPreferencesFromResource(R.xml.settings, rootKey);
        findPreference("restart_manager").setOnPreferenceClickListener(preference -> {
            System.exit(0);
            return true;
        });
    }

    private static final class SettingsDataStore extends PreferenceDataStore {
        private static final String KEY_SAFEMODE = "safemode";
        private static final String KEY_GLOBAL_MODE = "global_mode";
        private static final String KEY_RESOURCES_HOOK = "resources_hook";
        static final SettingsDataStore INSTANCE = new SettingsDataStore();

        @Override public boolean getBoolean(String key, boolean defValue) {
            switch (key) {
                case KEY_SAFEMODE:
                    return Dreamland.isSafeMode();
                case KEY_GLOBAL_MODE:
                    return Dreamland.isGlobalMode();
                case KEY_RESOURCES_HOOK:
                    return Dreamland.isResourcesHookEnabled();
                default:
                    throw new UnsupportedOperationException("Unimplemented for key " + key);
            }
        }

        @Override public void putBoolean(String key, boolean value) {
            switch (key) {
                case KEY_SAFEMODE:
                    Dreamland.setSafeMode(value);
                    break;
                case KEY_GLOBAL_MODE:
                    Dreamland.setGlobalModeEnabled(value);
                    break;
                case KEY_RESOURCES_HOOK:
                    Dreamland.setResourcesHookEnabled(value);
                    break;
                default:
                    throw new UnsupportedOperationException("Unimplemented for key " + key);
            }
        }
    }
}
