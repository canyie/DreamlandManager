package top.canyie.dreamland.manager.troubleshoot;

import static top.canyie.dreamland.manager.troubleshoot.Alert.*;

/**
 * @author canyie
 */
public enum AlertList {
    MAGISK_TMP_UNREADABLE(MAGISK_TMP_UNREADABLE_0, MAGISK_TMP_UNREADABLE_1, MAGISK_TMP_UNREADABLE_2),
    NO_RIRU(NO_RIRU_0, NO_RIRU_1),
    NO_DREAMLAND(NO_DREAMLAND_MODULE_0, NO_DREAMLAND_MODULE_1),
    CORE_JAR_MISSING(CORE_JAR_MISSING_0, CORE_JAR_MISSING_1, CORE_JAR_MISSING_2),
    CONFIG_DIR_BROKEN(CONFIG_DIR_BROKEN_0, CONFIG_DIR_BROKEN_1),
    DISABLED(DISABLED_0, DISABLED_1),
    SEPOLICY_NOT_LOADED(SEPOLICY_NOT_LOADED_0, SEPOLICY_NOT_LOADED_1, SEPOLICY_NOT_LOADED_2, SEPOLICY_NOT_LOADED_3, SEPOLICY_NOT_LOADED_4),
    WRONG_RIRU_SECONTEXT(WRONG_RIRU_SECONTEXT_0, WRONG_RIRU_SECONTEXT_1, WRONG_RIRU_SECONTEXT_2, WRONG_RIRU_SECONTEXT_3, WRONG_RIRU_SECONTEXT_4);

    private Alert[] alerts;

    AlertList(Alert... alerts) {
        this.alerts = alerts;
    }

    public void show() {
        Alert.POUNDS.show();
        for (Alert alert : alerts)
            alert.show();
    }

    public void showAsError() {
        Alert.POUNDS.showAsError();
        for (Alert alert : alerts)
            alert.showAsError();
    }
}
