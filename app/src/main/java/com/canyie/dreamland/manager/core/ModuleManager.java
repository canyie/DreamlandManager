package com.canyie.dreamland.manager.core;

import androidx.annotation.NonNull;

import com.canyie.dreamland.manager.ipc.RemoteModuleInfo;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author canyie
 */
public final class ModuleManager extends GsonBasedManager<ConcurrentHashMap<String, RemoteModuleInfo>> {
    ModuleManager() {
        super("modules.json");
    }

    public Map<String, RemoteModuleInfo> getEnabledModules() {
        return new HashMap<>(getRawObject());
    }

    public boolean isModuleEnabled(ModuleInfo module) {
        return isModuleEnabled(module.packageName);
    }

    public boolean isModuleEnabled(String packageName) {
        return getRawObject().containsKey(packageName);
    }

    public void setModuleEnabled(ModuleInfo module, boolean enable) {
        Map<String, RemoteModuleInfo> map = getRawObject();
        if (enable) {
            RemoteModuleInfo remoteModuleInfo = new RemoteModuleInfo(module.name, module.path);
            map.put(module.packageName, remoteModuleInfo);
        } else {
            map.remove(module.packageName);
        }
        notifyDataChanged();
    }

    @NonNull @Override protected ConcurrentHashMap<String, RemoteModuleInfo> createEmptyObject() {
        return new ConcurrentHashMap<>();
    }
}