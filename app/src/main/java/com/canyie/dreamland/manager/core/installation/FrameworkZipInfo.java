package com.canyie.dreamland.manager.core.installation;

import androidx.annotation.Keep;
import androidx.annotation.Nullable;
import androidx.annotation.Size;

/**
 * @author canyie
 */
@Keep public final class FrameworkZipInfo {
    public int version;
    public String url;
    @Nullable @Size(32) public String md5;
}
