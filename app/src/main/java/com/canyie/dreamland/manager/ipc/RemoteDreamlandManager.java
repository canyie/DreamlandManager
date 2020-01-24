package com.canyie.dreamland.manager.ipc;

import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Binder;
import android.os.Process;
import android.os.RemoteException;
import android.text.TextUtils;
import android.util.Log;

import androidx.annotation.NonNull;

import com.canyie.dreamland.manager.core.Dreamland;
import com.canyie.dreamland.manager.utils.DLog;
import com.canyie.dreamland.manager.utils.Preconditions;

import java.util.List;
import java.util.ServiceConfigurationError;

/**
 * Unused now...
 * @author canyie
 */
public class RemoteDreamlandManager extends IDreamlandManager.Stub {
    private static final String TAG = "RemoteDreamlandManager";

    private PackageManager pm;
    private List<String> mEnabledApps;
    private List<RemoteModuleInfo> mEnabledModules;
    private List<Integer> mEnabledModuleUids;

    public RemoteDreamlandManager(@NonNull Context context) {
        pm = context.getPackageManager();
        Preconditions.checkNotNull(pm, "context.getPackageManager() == null; the context is invalid? ");
    }

    @Override public boolean isActive() throws RemoteException {
        return Dreamland.isActive();
    }

    @Override public int getVersion() throws RemoteException {
        return Dreamland.getVersion();
    }

    @Override public void testAccess() throws RemoteException {
        String packageName = pm.getNameForUid(Binder.getCallingUid());
        DLog.e(TAG, "Package " + packageName + " calling testAccess()");
        throw new SecurityException("Package " + packageName + " calling testAccess()");
    }

    @Override
    public List<RemoteModuleInfo> getEnabledModulesForCurrentApp() throws RemoteException {
        boolean enabled = false;
        String callingPackage = pm.getNameForUid(Binder.getCallingUid());
        for (String packageName : mEnabledApps) {
            if (TextUtils.equals(callingPackage, packageName)) {
                enabled = true;
                break;
            }
        }
        if (!enabled) {
            return null;
        }
        return mEnabledModules;
    }

    @Override public boolean isSelfEnabled() throws RemoteException {
        int callingUid = Binder.getCallingUid();
        for (Integer uid : mEnabledModuleUids) {
            if (callingUid == uid) {
                return true;
            }
        }
        return false;
    }

    @Override public List<String> getEnabledAppsForCurrentModule() throws RemoteException {
        enforceCallerIsEnabledModule();
        return mEnabledApps;
    }

    @Override public void applyEnabledModules(List<RemoteModuleInfo> modules) throws RemoteException {
        enforceCallerIsManager("applyModules");
    }

    @Override public void applyEnabledApps(List<String> apps) throws RemoteException {
        enforceCallerIsManager("applyEnabledApps");
    }

    private void applyModulesImpl() {

    }

    private void enforceCallerIsManager(String api) {
        int callingUid = Binder.getCallingUid();
        if (callingUid != Process.myUid()) {
            DLog.i(TAG, "Rejecting process %d(uid %d) to call privilege API %s.", Binder.getCallingPid(), callingUid, api);
            throw new SecurityException("Only manager are allowed to call API " + api);
        }
    }

    private void enforceCallerIsEnabledModule() {
        int callingUid = Binder.getCallingUid();
        for (Integer uid : mEnabledModuleUids) {
            if (callingUid == uid) {
                return;
            }
        }
        DLog.i(TAG, "Rejecting process %d(uid %d) to call modules APIs.", Binder.getCallingPid(), callingUid);
        throw new SecurityException("Only enabled modules are allowed to call the API.");
    }
}
