package com.canyie.dreamland.manager.core;

import android.graphics.drawable.Drawable;

/**
 * @author canyie
 * @date 2019/12/18.
 */
public final class ModuleInfo {
    public String name;
    public String packageName;
    public String description;
    public String path;
    public String version;
    public Drawable icon;
    public boolean supported;
    public boolean enabled;

    private ModuleManager mm;
    ModuleInfo(String packageName, ModuleManager mm) {
        this.packageName = packageName;
        this.mm = mm;
        this.enabled = mm.isModuleEnabled(packageName);
    }

    public void setEnabled(boolean enable) {
        if (enable == enabled) return;
        enabled = enable;
        mm.setModuleEnabled(this, enable);
    }
}
