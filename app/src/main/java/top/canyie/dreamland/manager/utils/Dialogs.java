package top.canyie.dreamland.manager.utils;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.DialogInterface;
import android.view.LayoutInflater;
import android.widget.CheckBox;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ProgressBar;

import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.appcompat.app.AlertDialog;

import top.canyie.dreamland.manager.R;

/**
 * @author canyie
 * @date 2020/1/4.
 */
public final class Dialogs {
    private Dialogs() {
    }

    public static Builder create(Activity activity) {
        return new Builder(activity);
    }

    public static final class Builder {
        private Activity activity;
        private AlertDialog.Builder base;
        private ButtonInfo positiveButtonInfo, negativeButtonInfo, neutralButtonInfo;
        private CheckBox checkbox;
        private ProgressBar progressbar;
        Builder(Activity activity) {
            this.activity = activity;
            this.base = new AlertDialog.Builder(activity);
        }

        public Builder title(@StringRes int resId) {
            base.setTitle(resId);
            return this;
        }

        public Builder icon(@DrawableRes int resId) {
            base.setIcon(resId);
            return this;
        }

        public Builder message(@StringRes int resId) {
            base.setMessage(resId);
            return this;
        }

        public Builder checkbox(@StringRes int resId) {
            @SuppressLint("InflateParams")
            FrameLayout layout = (FrameLayout) LayoutInflater.from(activity).inflate(R.layout.dialog_checkbox, null, false);
            CheckBox checkbox = layout.findViewById(R.id.checkbox);
            checkbox.setText(resId);
            base.setView(layout);
            this.checkbox = checkbox;
            return this;
        }

        public Builder progressbar() {
            @SuppressLint("InflateParams")
            FrameLayout layout = (FrameLayout) LayoutInflater.from(activity).inflate(R.layout.dialog_progressbar, null, false);
            progressbar = layout.findViewById(R.id.progressbar);
            base.setView(layout);
            return this;
        }

        public Builder cancelable(boolean cancelable) {
            base.setCancelable(cancelable);
            return this;
        }

        public Builder positiveButton(@StringRes int resId, OnClickCallback callback) {
            positiveButtonInfo = new ButtonInfo(resId, callback);
            return this;
        }

        public Builder negativeButton(@StringRes int resId, OnClickCallback callback) {
            negativeButtonInfo = new ButtonInfo(resId, callback);
            return this;
        }

        public Builder neutralButton(@StringRes int resId, OnClickCallback callback) {
            neutralButtonInfo = new ButtonInfo(resId, callback);
            return this;
        }

        @NonNull public DialogInfo create() {
            DialogInfo dialogInfo = new DialogInfo();
            dialogInfo.checkbox = checkbox;
            dialogInfo.progressbar = progressbar;
            if (positiveButtonInfo != null) {
                base.setPositiveButton(positiveButtonInfo.textResId, asListener(dialogInfo, positiveButtonInfo));
            }
            if (negativeButtonInfo != null) {
                base.setNegativeButton(negativeButtonInfo.textResId, asListener(dialogInfo, negativeButtonInfo));
            }
            if (neutralButtonInfo != null) {
                base.setNeutralButton(neutralButtonInfo.textResId, asListener(dialogInfo, neutralButtonInfo));
            }
            dialogInfo.dialog = base.create();
            return dialogInfo;
        }

        public DialogInfo show() {
            DialogInfo dialogInfo = create();
            dialogInfo.dialog.show();
            return dialogInfo;
        }

        @Nullable public DialogInfo showIfActivityActivated() {
            if (activity.isFinishing() || activity.isDestroyed()) {
                return null;
            }
            return show();
        }

        private static DialogInterface.OnClickListener asListener(DialogInfo dialogInfo, ButtonInfo buttonInfo) {
            OnClickCallback callback = buttonInfo.callback;
            if (callback == null) return null;
            return (dialog, which) -> callback.onClick(dialogInfo);
        }

        private static final class ButtonInfo {
            @StringRes int textResId;
            OnClickCallback callback;

            ButtonInfo(@StringRes int textResId, OnClickCallback callback) {
                this.textResId = textResId;
                this.callback = callback;
            }
        }
    }

    public interface OnClickCallback {
        void onClick(DialogInfo info);
    }

    public static final class DialogInfo {
        public AlertDialog dialog;
        @Nullable public CheckBox checkbox;
        @Nullable public ProgressBar progressbar;
    }
}
