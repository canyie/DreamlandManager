package com.canyie.dreamland.manager.core.installation;

import androidx.annotation.WorkerThread;

/**
 * @author canyie
 */
@WorkerThread public interface InstallListener {
    void onPhase(@Installer.Phase int phase);
    void onLine(String line);
    void onErrorLine(String line);
    void onDone();
    void onError(InstallationException e);
}
