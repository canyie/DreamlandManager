package top.canyie.dreamland.manager.ui.activities;

import android.content.pm.ApplicationInfo;
import android.os.Bundle;
import android.text.Editable;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.style.ForegroundColorSpan;
import android.util.Log;
import android.view.MenuItem;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.StringRes;
import androidx.appcompat.app.ActionBar;
import androidx.core.content.ContextCompat;

import java.io.IOException;

import top.canyie.dreamland.manager.R;
import top.canyie.dreamland.manager.troubleshoot.Troubleshooter;
import top.canyie.dreamland.manager.utils.RootUtils;
import top.canyie.dreamland.manager.utils.Shell;
import top.canyie.dreamland.manager.utils.Threads;
import top.canyie.dreamland.manager.utils.callbacks.ExceptionCallback;
import top.canyie.dreamland.manager.utils.callbacks.ResultCallback;

/**
 * @author canyie
 */
public class TroubleShootActivity extends BaseActivity implements ResultCallback<Shell.Result>, ExceptionCallback<IOException> {
    private TextView mConsole;
    private Shell.Result mRunningShell;
    @Override protected int getLayoutResId() {
        return R.layout.activity_troubleshoot;
    }

    @Override protected void initLayout(Bundle savedInstanceState) {
        ActionBar actionBar = getSupportActionBar();
        assert actionBar != null;
        actionBar.setTitle(R.string.troubleshoot);
        actionBar.setDisplayHomeAsUpEnabled(true);

        mConsole = requireView(R.id.console);
        mConsole.setText(new SpannableStringBuilder(), TextView.BufferType.EDITABLE);
        final ApplicationInfo appInfo = getApplicationInfo();
        final String packagePath = appInfo.publicSourceDir != null ? appInfo.publicSourceDir : appInfo.sourceDir;
        RootUtils.execDex(packagePath, Troubleshooter.class.getName(), this, this);
    }

    @Override public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override public void onException(IOException e) {
        appendText(Log.getStackTraceString(e), true);
        if (mRunningShell != null) mRunningShell.killProcess();
    }

    @Override public void onDone(Shell.Result result) {
        mRunningShell = result;
        mRunningShell.readAllLinesAsync(line -> {
            appendText(line, false);
            return false;
        }, this);
        mRunningShell.readAllErrorLinesAsync(line -> {
            appendText(line, true);
            return false;
        }, this);
        try {
            int exitCode = result.waitFor();
            appendText(getString(R.string.script_exited_with_code, exitCode), false);
        } catch (InterruptedException e) {
            result.killProcess();
        } finally {
            mRunningShell = null;
        }
    }

    private void appendText(@StringRes int resId, boolean error) {
        appendText(getString(resId), error);
    }

    private void appendText(CharSequence text, boolean error) {
        final ForegroundColorSpan colorSpan = error ? new ForegroundColorSpan(ContextCompat.getColor(this, R.color.color_error)) : null;
        Threads.execOnMainThread(() -> {
            Editable editable = mConsole.getEditableText();
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
}
