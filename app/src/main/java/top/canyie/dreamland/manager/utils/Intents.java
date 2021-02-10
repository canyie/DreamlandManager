package top.canyie.dreamland.manager.utils;

import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;

import java.util.List;

import top.canyie.dreamland.manager.R;

/**
 * @author canyie
 */
public final class Intents {
    private Intents() {
    }

    @SuppressWarnings("SpellCheckingInspection") public static void openPermissionSettings(Context context) {
        String packageName = context.getPackageName();
        Intent intent = new Intent();
        if (OSUtils.isMIUI()) {
            String miuiVersion = OSUtils.getMIUIVersion();
            if ("V9".equalsIgnoreCase(miuiVersion) || "V8".equalsIgnoreCase(miuiVersion)) {
                intent.setAction("miui.intent.action.APP_PERM_EDITOR");
                intent.setClassName("com.miui.securitycenter", "com.miui.permcenter.permissions.PermissionsEditorActivity");
                intent.putExtra("extra_pkgname", packageName);
            } else if ("V7".equalsIgnoreCase(miuiVersion) || "V6".equalsIgnoreCase(miuiVersion)) {
                intent.setAction("miui.intent.action.APP_PERM_EDITOR");
                intent.setClassName("com.miui.securitycenter", "com.miui.permcenter.permissions.AppPermissionsEditorActivity");
                intent.putExtra("extra_pkgname", packageName);
            } else {
                toAppDetailsSettings(packageName, intent);
            }
        } else if (OSUtils.isEMUI()) {
            intent.putExtra("packageName", packageName);
            intent.setClassName("com.huawei.systemmanager", "com.huawei.permissionmanager.ui.MainActivity");
        } else if (OSUtils.isVIVO()) {
            if (((Build.MODEL.contains("Y85")) && (!Build.MODEL.contains("Y85A"))) || (Build.MODEL.contains("vivo Y53L"))) {
                intent.setClassName("com.vivo.permissionmanager", "com.vivo.permissionmanager.activity.PurviewTabActivity");
                intent.putExtra("tabId", "1");
            } else {
                intent.setClassName("com.vivo.permissionmanager", "com.vivo.permissionmanager.activity.SoftPermissionDetailActivity");
                intent.setAction("secure.intent.action.softPermissionDetail");
            }
            intent.putExtra("packagename", packageName);
        } if (OSUtils.isOPPO()) {
            intent.setClassName("com.color.safecenter", "com.color.safecenter.permission.PermissionManagerActivity");
            intent.putExtra("packageName", packageName);
        } else if (OSUtils.isFlyme()) {
            intent.setAction("com.meizu.safe.security.SHOW_APPSEC");
            intent.addCategory(Intent.CATEGORY_DEFAULT);
            intent.putExtra("packageName", packageName);
        } else if (DeviceUtils.isSony()) {
            intent.setClassName("com.sonymobile.cta", "com.sonymobile.cta.SomcCTAMainActivity");
            intent.putExtra("packageName", packageName);
        } else {
            toAppDetailsSettings(packageName, intent);
        }

        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        try {
            context.startActivity(intent);
        } catch (ActivityNotFoundException e) {
            DLog.e("Intents", "Failed to start activity with intent: " + intent, e);
            openAppDetailsSettings(context, packageName);
        }
    }

    public static void openAppDetailsSettings(Context context, String packageName) {
        Intent intent = new Intent();
        toAppDetailsSettings(packageName, intent);
        context.startActivity(intent);
    }

    public static boolean openModuleSettings(Context context, String packageName) {
        Intent intentToResolve = new Intent(Intent.ACTION_MAIN);
        intentToResolve.addCategory("de.robv.android.xposed.category.MODULE_SETTINGS");
        intentToResolve.setPackage(packageName);
        List<ResolveInfo> ris = context.getPackageManager().queryIntentActivities(intentToResolve, 0);
        if (ris.isEmpty()) {
            return openAppUserInterface(context, packageName);
        }
        ActivityInfo activityInfo = ris.get(0).activityInfo;
        Intent intent = new Intent(intentToResolve);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.setClassName(activityInfo.packageName, activityInfo.name);
        context.startActivity(intent);
        return true;
    }

    public static boolean openAppUserInterface(Context context, String packageName) {
        Intent launchIntent = context.getPackageManager().getLaunchIntentForPackage(packageName);
        if (launchIntent == null) return false;
        launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(launchIntent);
        return true;
    }

    public static void uninstallApp(Context context, String packageName) {
        Intent intent = new Intent(Intent.ACTION_DELETE, package2Uri(packageName));
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(intent);
    }

    public static void openUrl(Context context, String url) {
        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(Intent.createChooser(intent, context.getString(R.string.choose_browser)));
    }

    private static void toAppDetailsSettings(String packageName, Intent intent) {
        intent.setAction(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
        intent.setData(package2Uri(packageName));
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        Bundle extras = intent.getExtras();
        if (extras != null) extras.clear();
    }

    private static Uri package2Uri(String packageName) {
        return Uri.parse("package:" + packageName);
    }
}