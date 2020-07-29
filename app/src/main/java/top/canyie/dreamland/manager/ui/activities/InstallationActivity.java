package top.canyie.dreamland.manager.ui.activities;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.Spanned;
import android.text.TextWatcher;
import android.text.style.ForegroundColorSpan;
import android.util.Log;
import android.view.MenuItem;
import android.widget.TextView;

import androidx.annotation.IntDef;
import androidx.annotation.NonNull;
import androidx.annotation.StringRes;
import androidx.appcompat.app.ActionBar;

import top.canyie.dreamland.manager.AppConstants;
import top.canyie.dreamland.manager.AppGlobals;
import top.canyie.dreamland.manager.R;
import top.canyie.dreamland.manager.core.Dreamland;
import top.canyie.dreamland.manager.core.installation.InstallListener;
import top.canyie.dreamland.manager.core.installation.InstallationException;
import top.canyie.dreamland.manager.core.installation.Installer;
import top.canyie.dreamland.manager.utils.Threads;

import java.io.File;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * @author canyie
 */
@Deprecated public class InstallationActivity extends BaseActivity {
    public static final int REQUEST_CODE = 1;

    private static DataHolderAndReceiver sDataHolder;
    private TextView mConsole;

    @Override protected int getLayoutResId() {
        return R.layout.activity_installation;
    }

    @Override protected void initLayout(Bundle savedInstanceState) {
        Intent sourceIntent = getIntent();
        boolean isInstall = sourceIntent.getBooleanExtra(AppConstants.KEY_IS_INSTALL, false);
        ActionBar actionBar = getSupportActionBar();
        assert actionBar != null;
        actionBar.setTitle(isInstall ? R.string.install_or_update : R.string.uninstall);
        //actionBar.setSubtitle("Version 233");
        actionBar.setDisplayHomeAsUpEnabled(true);

        mConsole = requireView(R.id.console);

        if (sDataHolder == null) {
            Installer installer = Dreamland.getInstaller();
            if (installer.hasRunningJob()) {
                toast(R.string.previous_job_not_completed);
                finish();
            }
            sDataHolder = new DataHolderAndReceiver();
            sDataHolder.setActivity(this);
            sDataHolder.setTextTo(mConsole);
            // noinspection ConstantConditions
            File frameworkZipFile = new File(sourceIntent.getStringExtra(AppConstants.KEY_FRAMEWORK_ZIP_FILE));
            if (isInstall)
                installer.startInstall(frameworkZipFile, sDataHolder);
            else
                installer.startUninstall(frameworkZipFile, sDataHolder);
        } else {
            sDataHolder.setActivity(this);
            sDataHolder.setTextTo(mConsole);
        }
    }

    @Override protected void onDestroy() {
        if (sDataHolder.isRunning()) {
            sDataHolder.saveText(mConsole);
            sDataHolder.setActivity(null);
        } else {
            sDataHolder = null;
        }
        super.onDestroy();
    }

    @Override public void onBackPressed() {
        if (sDataHolder.isRunning()) {
            toast(R.string.alert_wait_install_complete);
            return;
        }
        super.onBackPressed();
    }

    @Override public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    Editable getConsoleEditableText() {
        return mConsole.getEditableText();
    }

    private static final class DataHolderAndReceiver implements InstallListener {
        private static final int TYPE_NONE = 0;
        private static final int TYPE_ERROR = -1;
        private static final int TYPE_OK = 1;

        @Retention(RetentionPolicy.SOURCE)
        @IntDef({TYPE_NONE, TYPE_OK, TYPE_ERROR})
        private @interface TextType {
        }

        private Editable mText;
        private InstallationActivity mActivity;
        private boolean running = true;

        DataHolderAndReceiver() {
        }

        void setActivity(InstallationActivity activity) {
            this.mActivity = activity;
        }

        void saveText(TextView console) {
            mText = console.getEditableText();
            // Remove all TextWatchers that may come from TextView as they may
            // reference the TextView and cause a memory leak.
            TextWatcher[] textWatchers = mText.getSpans(0, mText.length(), TextWatcher.class);
            for (TextWatcher textWatcher : textWatchers) {
                mText.removeSpan(textWatcher);
            }
        }

        void setTextTo(TextView console) {
            console.setText(mText, TextView.BufferType.EDITABLE);
        }

        boolean isRunning() {
            return running;
        }

        @Override public void onPhase(@Installer.Phase int phase) {
            @StringRes int textRes;
            switch (phase) {
                case Installer.PHASE_UNZIPPING_FRAMEWORK_ZIP:
                    textRes = R.string.phase_unzipping;
                    break;
                case Installer.PHASE_CHECKING_ZIP:
                    textRes = R.string.phase_checking_zip;
                    break;
                case Installer.PHASE_EXECUTING_INSTALL_SCRIPT:
                    textRes = R.string.phase_executing_script;
                    break;
                case Installer.PHASE_CLEANUP:
                    textRes = R.string.phase_cleanup;
                    break;
                default:
                    throw new IllegalStateException("Unexpected phase " + phase);
            }
            appendText(textRes, TYPE_NONE);
        }

        @Override public void onLine(String line) {
            appendText(line, TYPE_NONE);
        }

        @Override public void onErrorLine(String line) {
            appendText(line, TYPE_ERROR);
        }

        @Override public void onDone() {
            appendText(R.string.install_success, TYPE_OK);
            running = false;
            notifyStateChanged();
        }

        @Override public void onError(InstallationException e) {
            Context context = AppGlobals.getApp();
            StringBuilder sb = new StringBuilder(context.getString(R.string.install_failed)).append('\n');
            int errorTypeStringRes;
            String errorInfo = null;
            boolean maybeNoRoot = false;
            switch (e.error) {
                case Installer.ERROR_BAD_FRAMEWORK_ZIP:
                    errorTypeStringRes = R.string.install_error_bad_zip;
                    break;
                case Installer.ERROR_REQUIRED_MANAGER_VERSION_TOO_HIGH:
                    errorTypeStringRes = R.string.install_error_framework_required_manager_too_high;
                    break;
                case Installer.ERROR_ANDROID_VERSION_NOT_SUPPORTED:
                case Installer.ERROR_CPU_NOT_SUPPORTED:
                    errorTypeStringRes = R.string.install_error_device_not_supported;
                    break;
                case Installer.ERROR_UNKNOWN_IO_ERROR:
                    errorTypeStringRes = R.string.install_error_unknown_io_error;
                    errorInfo = "\n" + Log.getStackTraceString(e.getCause());
                    maybeNoRoot = true;
                    break;
                case Installer.ERROR_SCRIPT_ERROR:
                    errorTypeStringRes = R.string.install_error_unknown_script_error;
                    errorInfo = e.getMessage();
                    maybeNoRoot = true;
                    break;
                case Installer.ERROR_UNKNOWN_ERROR:
                    errorTypeStringRes = R.string.install_error_unknown_error;
                    errorInfo = "\n" + Log.getStackTraceString(e);
                    maybeNoRoot = true;
                    break;
                default:
                    throw new IllegalStateException("Unexpected error " + e.error, e);
            }
            sb.append(context.getString(R.string.error_type)).append(context.getString(errorTypeStringRes)).append('\n');
            if (errorInfo != null) {
                sb.append(context.getString(R.string.error_info)).append(errorInfo).append('\n');
            }
            if (maybeNoRoot) {
                sb.append(context.getString(R.string.maybe_no_root_alert)).append('\n');
            }
            appendText(sb, TYPE_ERROR);
            running = false;
            notifyStateChanged();
        }

        private void appendText(@StringRes int textRes, @TextType int type) {
            appendText(AppGlobals.getApp().getString(textRes), type);
        }

        private void appendText(CharSequence text, @TextType int type) {
            final ForegroundColorSpan colorSpan;
            switch (type) {
                case TYPE_ERROR:
                    colorSpan = new ForegroundColorSpan(AppGlobals.getApp().getColor(R.color.color_error));
                    break;
                case TYPE_OK:
                    colorSpan = new ForegroundColorSpan(AppGlobals.getApp().getColor(R.color.color_active));
                    break;
                case TYPE_NONE:
                default:
                    colorSpan = null;
                    break;
            }

            Threads.execOnMainThread(() -> {
                InstallationActivity activity = mActivity;
                Editable editable = activity != null ? activity.getConsoleEditableText() : mText;

                if (colorSpan != null) {
                    int start = editable.length();
                    editable.append(text);
                    int end = editable.length();
                    editable.setSpan(colorSpan, start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                } else {
                    editable.append(text);
                }
                editable.append("\n");
            });
        }

        private void notifyStateChanged() {
            InstallationActivity activity = this.mActivity;
            if (activity != null) {
                activity.setResult(RESULT_OK); // result are ignored
            }
        }
    }
}
