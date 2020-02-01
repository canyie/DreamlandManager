package com.canyie.dreamland.manager.core.installation;

import androidx.annotation.Keep;

/**
 * @author canyie
 */
@Keep @SuppressWarnings("WeakerAccess") public final class FrameworkZipProperties {
    public int minManagerVersionCode;
    public String minManagerVersionName;
    public int minAndroidVersion;
    public String[] supportedAbis;
    public int versionCode;
}
