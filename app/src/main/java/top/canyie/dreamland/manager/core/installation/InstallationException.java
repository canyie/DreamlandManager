package top.canyie.dreamland.manager.core.installation;

import java.io.IOException;

/**
 * @author canyie
 */
@Deprecated public class InstallationException extends Exception {
    public int error;
    InstallationException(int error, String message) {
        super(message);
        this.error = error;
    }

    InstallationException(int error, String message, Throwable cause) {
        super(message, cause);
        this.error = error;
    }

    InstallationException(String message, IOException cause) {
        super(message, cause);
        this.error = Installer.ERROR_UNKNOWN_IO_ERROR;
    }

    InstallationException(IOException cause) {
        super(cause);
        this.error = Installer.ERROR_UNKNOWN_IO_ERROR;
    }

}
