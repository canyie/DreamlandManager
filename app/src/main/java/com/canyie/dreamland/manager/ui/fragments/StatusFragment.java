package com.canyie.dreamland.manager.ui.fragments;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Build;
import android.system.ErrnoException;
import android.system.OsConstants;
import android.util.SparseArray;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;

import com.canyie.dreamland.manager.AppGlobals;
import com.canyie.dreamland.manager.R;
import com.canyie.dreamland.manager.utils.DLog;
import com.canyie.dreamland.manager.utils.DeviceUtils;
import com.canyie.dreamland.manager.utils.Dialogs;
import com.canyie.dreamland.manager.utils.FileUtils;
import com.canyie.dreamland.manager.utils.HashUtils;
import com.canyie.dreamland.manager.utils.IOUtils;
import com.canyie.dreamland.manager.utils.Intents;
import com.canyie.dreamland.manager.utils.OkHttp;
import com.canyie.dreamland.manager.utils.SELinuxHelper;
import com.canyie.dreamland.manager.utils.Shell;
import com.canyie.dreamland.manager.utils.collections.IntHashMap;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;
import java.util.Random;

/**
 * @author canyie
 * @date 2019/12/10.
 */
public class StatusFragment extends PageFragment implements View.OnClickListener {
    private static final String TAG = "StatusFragment";
    private static final int PERMISSION_REQUEST_FOR_INSTALL = 1;
    private TextView textVerifiedBootState;
    private TextView textSELinuxMode;
    public StatusFragment() {
        super(R.string.status);
    }

    @Override protected int getLayoutResId() {
        return R.layout.fragment_status;
    }

    @Override protected void initView(@NonNull View view) {
        TextView tv = requireView(R.id.text);
        tv.setText(R.string.status);

        TextView textAndroidVersion = requireView(R.id.device_info_android_version);
        textAndroidVersion.setText(String.format(getString(R.string.text_android_version), Build.VERSION.RELEASE, Build.VERSION.SDK_INT));

        TextView textDevice = requireView(R.id.device_info_device);
        textDevice.setText(DeviceUtils.getUIFramework());

        TextView textCPUArch = requireView(R.id.device_info_cpu);
        textCPUArch.setText(String.format(getString(R.string.text_cpu_info), DeviceUtils.CPU_ABI, DeviceUtils.CPU_ARCH));

        textVerifiedBootState = requireView(R.id.device_info_verity_boot);
        textSELinuxMode = requireView(R.id.device_info_selinux_mode);
        requireView(R.id.install_card).setOnClickListener(this);

        File file = new File(AppGlobals.getApp().getExternalFilesDir("download"), "iApp_200113.apk");
        try {
            File dir = new File(file.getParent(), "iApp_200113");
            //FileUtils.unzip(file, dir);
            FileUtils.delete(dir);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        /*OkHttp.download()
                .url("https://ip2.oss-cn-shanghai.aliyuncs.com/app/iApp.Yuv5.2020.1.13.apk")
                .dest())
                .expectMD5("58c169867a8af5bddea8085b9fc2c449")
                .startAsync(new OkHttp.DownloadListener() {
                    @Override public void onProgress(OkHttp.DownloadInfo downloadInfo) {
                        System.out.println("Progress " + downloadInfo.progress);
                    }

                    @Override public void onSucceed(OkHttp.DownloadInfo downloadInfo) {
                        System.out.println("succeed");
                    }

                    @Override
                    public void onFailed(OkHttp.DownloadInfo downloadInfo, IOException e) {
                        e.printStackTrace();
                    }
                });*/

    }

    @Override protected void beforeLoadData() {
        boolean detectVerifiedBoot = false, isVerifiedBootActive = false, checkVerifiedBootFailed = false;
        boolean isSELinuxEnabled = false, isSELinuxEnforced = false;
        try {
            if (DeviceUtils.detectVerifiedBoot()) {
                detectVerifiedBoot = true;
                isVerifiedBootActive = DeviceUtils.isVerifiedBootActive();
            }
        } catch (Exception e) {
            DLog.e("DeviceUtils", "Could not detect Verified Boot state", e);
            checkVerifiedBootFailed = true;
        }

        if (SELinuxHelper.isEnabled()) {
            isSELinuxEnabled = true;
            isSELinuxEnforced = SELinuxHelper.isEnforcing();
        }

        if (checkVerifiedBootFailed) {
            textVerifiedBootState.setText(R.string.verified_boot_state_unknown);
            textVerifiedBootState.setTextColor(requireContext().getColor(R.color.color_error));
        } else if (isVerifiedBootActive) {
            textVerifiedBootState.setText(R.string.verified_boot_state_active);
            textVerifiedBootState.setTextColor(requireContext().getColor(R.color.color_error));
        } else if (detectVerifiedBoot) {
            textVerifiedBootState.setText(R.string.verified_boot_state_deactivated);
        } else {
            textVerifiedBootState.setText(R.string.verified_boot_state_not_detected);
        }

        if (isSELinuxEnabled) {
            if (isSELinuxEnforced) {
                textSELinuxMode.setText(R.string.selinux_mode_enforcing);
            } else {
                textSELinuxMode.setText(R.string.selinux_mode_permissive);
            }
        } else {
            textSELinuxMode.setText(R.string.selinux_mode_disabled);
        }
    }


    @Override public void onClick(View v) {
        switch (v.getId()) {
            case R.id.install_card:
                Dialogs.create(requireActivity())
                        .title(R.string.install_warning_title)
                        .message(R.string.install_warning_content)
                        .negativeButton(R.string.cancel, null)
                        .positiveButton(R.string.str_continue, dialogInfo -> {
                            startInstall();
                        })
                        .cancelable(false)
                        .showIfActivityActivated();
        }
    }

    private void startInstall() {
        if (!checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
            requestPermissionsForInstall();
            return;
        }
        toast("Test Install");
    }

    private void requestPermissionsForInstall() {
        requestPermissions(new String[]{Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE}, PERMISSION_REQUEST_FOR_INSTALL);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == PERMISSION_REQUEST_FOR_INSTALL) {
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
                startInstall();
                return;
            }
            boolean finalShouldShowRequestPermissionRationale = shouldShowRequestPermissionRationale;
            Dialogs.create(requireActivity())
                    .title(R.string.permission_request_rationale_title_for_install)
                    .message(R.string.permission_request_rationale_message_for_install)
                    .negativeButton(R.string.no, null)
                    .positiveButton(R.string.ok, dialogInfo -> {
                        if (finalShouldShowRequestPermissionRationale) {
                            requestPermissionsForInstall();
                        } else {
                            Intents.openPermissionSettings(requireContext());
                        }
                    })
                    .showIfActivityActivated();
        } else {
            DLog.e(TAG, "Unknown permission request code %d", requestCode);
            return;
        }

    }
}
