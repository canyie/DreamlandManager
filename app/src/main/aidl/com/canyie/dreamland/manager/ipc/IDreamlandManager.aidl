// IDreamlandManager.aidl
package com.canyie.dreamland.manager.ipc;
import com.canyie.dreamland.manager.ipc.RemoteModuleInfo;
// Declare any non-default types here with import statements

interface IDreamlandManager {
    boolean isActive();
    int getVersion();
    void testAccess();

    /**
     * Returns a list of modules that the application needs to load.
     * Called by target application.
     */
    List<RemoteModuleInfo> getEnabledModulesForCurrentApp();

    /**
     * Returns whether the module is enabled. Called by module.
     */
    boolean isSelfEnabled();

    List<String> getEnabledAppsForCurrentModule();

    void applyEnabledModules(in List<RemoteModuleInfo> modules);
    void applyEnabledApps(in List<String> apps);
}
