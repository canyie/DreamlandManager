package top.canyie.dreamland.manager.ui.fragments;

import android.os.Bundle;

import androidx.preference.Preference;
import androidx.preference.PreferenceDataStore;
import androidx.preference.PreferenceFragmentCompat;

import top.canyie.dreamland.manager.AppConstants;
import top.canyie.dreamland.manager.AppGlobals;
import top.canyie.dreamland.manager.R;
import top.canyie.dreamland.manager.core.Dreamland;
import top.canyie.dreamland.manager.utils.CacheablePreferenceDataStore;

/**
 * @author canyie
 */
public class SettingsFragment extends PreferenceFragmentCompat {
    private static final String TAG = "SettingsFragment";

    @Override public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        getPreferenceManager().setPreferenceDataStore(new CacheablePreferenceDataStore(SettingsDataStore.INSTANCE));
        setPreferencesFromResource(R.xml.settings, rootKey);
    }

    @Override public boolean onPreferenceTreeClick(Preference preference) {
        switch (preference.getKey()) {
            case "invalidate_alert_settings":
                AppGlobals.getDefaultConfigSP()
                        .edit()
                        .remove(AppConstants.SP_KEY_SHOW_DIALOG_WHEN_APP_STATE_CHANGED)
                        .remove(AppConstants.SP_KEY_SHOW_DIALOG_WHEN_MODULE_STATE_CHANGED)
                        .remove(AppConstants.SP_KEY_SHOW_ALERT_FOR_MAS)
                        .apply();
                return true;
            case "restart_manager":
                System.exit(0);
                return true;
        }
        return super.onPreferenceTreeClick(preference);
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
