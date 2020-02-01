package com.canyie.dreamland.manager.ui.fragments;

import android.Manifest;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;

import com.canyie.dreamland.manager.AppConstants;
import com.canyie.dreamland.manager.AppGlobals;
import com.canyie.dreamland.manager.BuildConfig;
import com.canyie.dreamland.manager.R;
import com.canyie.dreamland.manager.core.Dreamland;
import com.canyie.dreamland.manager.core.installation.FrameworkZipProperties;
import com.canyie.dreamland.manager.ui.activities.InstallationActivity;
import com.canyie.dreamland.manager.ui.activities.MainActivity;
import com.canyie.dreamland.manager.utils.DLog;
import com.canyie.dreamland.manager.utils.DeviceUtils;
import com.canyie.dreamland.manager.utils.Dialogs;
import com.canyie.dreamland.manager.utils.FileUtils;
import com.canyie.dreamland.manager.utils.Intents;
import com.canyie.dreamland.manager.utils.SELinuxHelper;
import com.canyie.dreamland.manager.utils.Threads;

import java.io.File;
import java.io.IOException;

/**
 * @author canyie
 */
public class StatusFragment extends PageFragment implements View.OnClickListener {
    private static final String TAG = "StatusFragment";
    /**
     * Framework state: active
     */
    private static final int FRAMEWORK_STATE_ACTIVE = 0;
    /**
     * Framework state: disabled
     */
    private static final int FRAMEWORK_STATE_DISABLED = 1;
    /**
     * Framework state: complete installed but not active
     */
    private static final int FRAMEWORK_STATE_COMPLETE_INSTALLED = 2;
    /**
     * Framework state: broken
     */
    private static final int FRAMEWORK_STATE_BROKEN = 3;
    /**
     * Framework state: not installed
     */
    private static final int FRAMEWORK_STATE_NOT_INSTALLED = 4;

    private static final int PERMISSION_REQUEST_FOR_INSTALL = 1;
    private static final int PERMISSION_REQUEST_FOR_UNINSTALL = 2;
    private View statusCardBackground;
    private TextView statusText;
    private ImageView statusImage;
    private TextView verifiedBootStateText;
    private TextView seLinuxModeText;
    private CardView installCard, uninstallCard;
    private TextView installIssueText;

    public StatusFragment() {
        super(R.string.status);
    }

    @Override protected int getLayoutResId() {
        return R.layout.fragment_status;
    }

    @Override protected void initView(@NonNull View view) {
        statusCardBackground = requireView(R.id.status_icon_background);
        statusImage = requireView(R.id.status_icon);
        statusText = requireView(R.id.status_text);
        statusText.setText(R.string.loading);

        TextView textAndroidVersion = requireView(R.id.device_info_android_version);
        textAndroidVersion.setText(String.format(getString(R.string.text_android_version), Build.VERSION.RELEASE, Build.VERSION.SDK_INT));

        TextView textDevice = requireView(R.id.device_info_device);
        textDevice.setText(DeviceUtils.getUIFramework());

        TextView textCPUArch = requireView(R.id.device_info_cpu);
        textCPUArch.setText(getString(R.string.text_cpu_info, DeviceUtils.CPU_ABI, DeviceUtils.CPU_ARCH));

        verifiedBootStateText = requireView(R.id.device_info_verity_boot);
        seLinuxModeText = requireView(R.id.device_info_selinux_mode);
        installCard = requireView(R.id.install_card);
        installCard.setOnClickListener(this);
        uninstallCard = requireView(R.id.uninstall_card);
        uninstallCard.setOnClickListener(this);
        installIssueText = requireView(R.id.install_know_issue);
    }

    @Override protected I loadDataImpl() {
        I info = new I();
        info.frameworkVersion = Dreamland.getVersion();
        if (Dreamland.isActive()) {
            info.frameworkState = FRAMEWORK_STATE_ACTIVE;
        } else if (Dreamland.isDisabled()) {
            info.frameworkState = FRAMEWORK_STATE_DISABLED;
        } else if (Dreamland.isInstalled()) {
            info.frameworkState = Dreamland.isCompleteInstalled() ? FRAMEWORK_STATE_COMPLETE_INSTALLED : FRAMEWORK_STATE_BROKEN;
        } else {
            info.frameworkState = FRAMEWORK_STATE_NOT_INSTALLED;
        }

        try {
            if (DeviceUtils.detectVerifiedBoot()) {
                info.detectVerifiedBoot = true;
                info.isVerifiedBootActive = DeviceUtils.isVerifiedBootActive();
            }
        } catch (Exception e) {
            DLog.e("DeviceUtils", "Could not detect Verified Boot state", e);
            info.checkVerifiedBootFailed = true;
        }

        if (SELinuxHelper.isEnabled()) {
            info.isSELinuxEnabled = true;
            info.isSELinuxEnforced = SELinuxHelper.isEnforcing();
        }
        return info;
    }

    @Override protected void updateUIForData(Object data) {
        I info = (I) data;
        String text;
        int cardBackgroundColorRes;
        int cardImageRes;
        boolean supported;
        switch (info.frameworkState) {
            case FRAMEWORK_STATE_ACTIVE:
                text = getString(R.string.framework_state_active, info.frameworkVersion);
                cardBackgroundColorRes = R.color.color_active;
                cardImageRes = R.drawable.ic_check_circle;
                supported = true;
                break;
            case FRAMEWORK_STATE_DISABLED:
                text = getString(R.string.framework_state_disabled);
                cardBackgroundColorRes = R.color.color_warning;
                cardImageRes = R.drawable.ic_warning;
                supported = true;
                break;
            case FRAMEWORK_STATE_COMPLETE_INSTALLED:
                text = getString(R.string.framework_state_installed_but_not_active);
                cardBackgroundColorRes = R.color.color_warning;
                cardImageRes = R.drawable.ic_warning;
                supported = true;
                break;
            case FRAMEWORK_STATE_BROKEN:
                text = getString(R.string.framework_state_broken);
                cardBackgroundColorRes = R.color.color_error;
                cardImageRes = R.drawable.ic_error;
                supported = true;
                break;
            case FRAMEWORK_STATE_NOT_INSTALLED:
                text = getString(R.string.framework_state_not_installed);
                cardBackgroundColorRes = R.color.color_error;
                cardImageRes = R.drawable.ic_error;
                supported = Dreamland.isSupported();
                break;
            default:
                throw new UnsupportedOperationException("Unexpected framework state: " + info.frameworkState);
        }

        statusText.setText(text);
        statusCardBackground.setBackgroundColor(statusCardBackground.getContext().getColor(cardBackgroundColorRes));
        statusImage.setImageResource(cardImageRes);

        if (info.checkVerifiedBootFailed) {
            verifiedBootStateText.setText(R.string.verified_boot_state_unknown);
            verifiedBootStateText.setTextColor(requireContext().getColor(R.color.color_error));
        } else if (info.isVerifiedBootActive) {
            verifiedBootStateText.setText(R.string.verified_boot_state_active);
            verifiedBootStateText.setTextColor(requireContext().getColor(R.color.color_error));
        } else if (info.detectVerifiedBoot) {
            verifiedBootStateText.setText(R.string.verified_boot_state_deactivated);
        } else {
            verifiedBootStateText.setText(R.string.verified_boot_state_not_detected);
        }

        if (info.isSELinuxEnabled) {
            if (info.isSELinuxEnforced) {
                seLinuxModeText.setText(R.string.selinux_mode_enforcing);
            } else {
                seLinuxModeText.setText(R.string.selinux_mode_permissive);
            }
        } else {
            seLinuxModeText.setText(R.string.selinux_mode_disabled);
        }

        if (supported) {
            installCard.setVisibility(View.VISIBLE);
            uninstallCard.setVisibility(View.VISIBLE);
            installIssueText.setVisibility(View.GONE);
        } else {
            installCard.setVisibility(View.GONE);
            uninstallCard.setVisibility(View.GONE);
            installIssueText.setVisibility(View.VISIBLE);
        }
        super.updateUIForData(data);
    }

    @Override public void onClick(View v) {
        switch (v.getId()) {
            case R.id.install_card:
                Dialogs.create(requireActivity())
                        .title(R.string.install_warning_title)
                        .message(R.string.install_warning_content)
                        .negativeButton(R.string.cancel, null)
                        .positiveButton(R.string.str_continue, dialogInfo -> startInstallOrUninstall(true))
                        .cancelable(false)
                        .showIfActivityActivated();
                break;
            case R.id.uninstall_card:
                Dialogs.create(requireActivity())
                        .title(R.string.uninstall_alert)
                        .negativeButton(R.string.cancel, null)
                        .positiveButton(R.string.str_continue, dialogInfo -> startInstallOrUninstall(false))
                        .showIfActivityActivated();
                break;
            default:
                throw new IllegalStateException("Unexpected view id: " + v.getId());
        }
    }

    private boolean checkInstallPermission(boolean isInstall) {
        if (!checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
            requestInstallPermissions(isInstall ? PERMISSION_REQUEST_FOR_INSTALL : PERMISSION_REQUEST_FOR_UNINSTALL);
            return false;
        }
        return true;
    }

    private static boolean downloadFrameworkZip(File dest) {
        try {
            // TODO: Download framework zip from internet
            FileUtils.copyFromAssets("dreamland-v1.zip", dest);
            return true;
        } catch (IOException e) {
            DLog.e(TAG, "Failed to copy framework zip", e);
            return false;
        }
    }

    private void startInstallOrUninstall(boolean isInstall) {
        if (!checkInstallPermission(isInstall)) {
            return;
        }
        MainActivity activity = (MainActivity) getActivity();
        if (activity == null || activity.isFinishing() || activity.isDestroyed()) return;
        ProgressDialog progressDialog = ProgressDialog.show(activity, null, getString(R.string.alert_wait_framework_download_complete), true, false);
        Threads.getDefaultExecutor().execute(() -> {
            File frameworkZip = new File(activity.getExternalFilesDir("downloads"), "dreamland.zip");
            boolean copied = downloadFrameworkZip(frameworkZip);

            Threads.execOnMainThread(() -> {
                progressDialog.dismiss();
                if (copied) {
                    Intent intent = new Intent(activity, InstallationActivity.class);
                    intent.putExtra(AppConstants.KEY_FRAMEWORK_ZIP_FILE, frameworkZip.getAbsolutePath());
                    intent.putExtra(AppConstants.KEY_IS_INSTALL, isInstall);
                    activity.startActivityForResult(intent, InstallationActivity.REQUEST_CODE);
                } else {
                    activity.toast(R.string.alert_framework_zip_download_failed);
                }
            });
        });
    }

    private void requestInstallPermissions(int requestCode) {
        requestPermissions(new String[] {Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE}, requestCode);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        boolean isInstall = false;
        if (requestCode == PERMISSION_REQUEST_FOR_INSTALL) {
            isInstall = true;
        } else if (requestCode != PERMISSION_REQUEST_FOR_UNINSTALL) {
            DLog.e(TAG, "Unknown permission request code %d", requestCode);
            return;
        }

        boolean allGranted = true;
        boolean shouldShowRequestPermissionRationale = false;
        for (int i = 0; i < grantResults.length; i++) {
            if (grantResults[i] != PackageManager.PERMISSION_GRANTED) {
                allGranted = false;
                String permission = permissions[i];
                DLog.i(TAG, "Permission %s not granted", permission);
                shouldShowRequestPermissionRationale = shouldShowRequestPermissionRationale(permission);
                break;
            }
        }

        if (allGranted) {
            startInstallOrUninstall(isInstall);
            return;
        }
        boolean finalShouldShowRequestPermissionRationale = shouldShowRequestPermissionRationale;
        Dialogs.create(requireActivity())
                .title(R.string.permission_request_rationale_title_for_install)
                .message(R.string.permission_request_rationale_message_for_install)
                .negativeButton(R.string.no, null)
                .positiveButton(R.string.ok, dialogInfo -> {
                    if (finalShouldShowRequestPermissionRationale) {
                        requestInstallPermissions(requestCode);
                    } else {
                        Intents.openPermissionSettings(requireContext());
                    }
                })
                .showIfActivityActivated();
    }

    private static final class I {
        int frameworkState;
        int frameworkVersion;
        boolean detectVerifiedBoot, isVerifiedBootActive, checkVerifiedBootFailed;
        boolean isSELinuxEnabled, isSELinuxEnforced;
    }
}
