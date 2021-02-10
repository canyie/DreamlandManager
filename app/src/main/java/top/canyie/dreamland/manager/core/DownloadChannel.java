package top.canyie.dreamland.manager.core;

import androidx.annotation.NonNull;

/**
 * @author canyie
 */
public enum DownloadChannel {
    CANARY("https://dev.azure.com/ssz33334930121/ssz3333493/_build?definitionId=1"),
    BETA("https://github.com/canyie/Dreamland/releases");
    //STABLE;

    private final String url;

    DownloadChannel(@NonNull String url) {
        this.url = url;
    }

    public String getUrl() {
        return url;
    }

    public String getName() {
        if (this == CANARY) {
            return "canary";
        } else if (this == BETA) {
            return "beta";
        } else {
            return "stable";
        }
    }
}
