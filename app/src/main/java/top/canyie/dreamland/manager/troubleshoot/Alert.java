package top.canyie.dreamland.manager.troubleshoot;

import android.annotation.SuppressLint;

import java.util.Locale;

/**
 * @author canyie
 */
public enum Alert {
    POUNDS("*************************"),
    RUN_AS_ROOT("Troubleshooter should run as root", "请以 root 身份运行错误诊断"),
    MAGISK_ERROR("Magisk occurs error, please try upgrading Magisk to the latest version", "Magisk 发生错误，请尝试更新到最新版本"),
    FOUND_PROBLEM("Found problem: ", "找到问题："),

    MAGISK_TMP_PATH_IS("Magisk tmpfs path is %s", "Magisk 根路径为 %s"),

    MAGISK_TMP_UNREADABLE_0("Magisk tmpfs path not readable", "Magisk tmpfs 路径不可读"),
    MAGISK_TMP_UNREADABLE_1("Please disable Magisk Hide for Dreamland Manager", "请对梦境管理器禁用 Magisk Hide"),
    MAGISK_TMP_UNREADABLE_2("or try upgrading Magisk to the latest canary version", "或尝试更新Magisk到最新的canary版本"),

    NO_RIRU_0("Riru is not installed", "Riru 未安装"),
    NO_RIRU_1("Please install Riru from Magisk app or https://github.com/RikkaApps/Riru/releases/", "请从 Magisk 或 https://github.com/RikkaApps/Riru/releases/ 安装 Riru"),

    NO_DREAMLAND_MODULE_0("Riru-Dreamland is not installed", "Riru-Dreamland 未安装"),
    NO_DREAMLAND_MODULE_1("Please install Riru-Dreamland from Dreamland Manager", "请从梦境管理器中安装 Riru-Dreamland"),

    CORE_JAR_MISSING_0("Core jar file is missing", "核心jar文件已丢失"),
    CORE_JAR_MISSING_1("Please try re-installing Dreamland framework", "请尝试重新安装梦境框架"),
    CORE_JAR_MISSING_2("and do not enable Magisk Hide for Dreamland Manager", "并对梦境管理器关闭Magisk Hide"),

    CONFIG_DIR_BROKEN_0("Config dir is missing or broken", "配置目录已丢失或存在问题"),
    CONFIG_DIR_BROKEN_1(CORE_JAR_MISSING_1),

    DISABLED_0("Dreamland framework is disabled", "梦境框架已被手动禁用"),
    DISABLED_1("Please delete the file /data/misc/dreamland/disable and restart the device", "请删除文件 /data/misc/dreamland/disable 并重新启动此设备"),

    SEPOLICY_NOT_LOADED_0("SEPolicy patch rules not loaded properly by Magisk", "SEPolicy修补规则未被 Magisk 正确加载"),
    SEPOLICY_NOT_LOADED_1("Please try to reboot twice,", "请尝试重启至少两次，"),
    SEPOLICY_NOT_LOADED_2("try upgrading Magisk to the latest canary version,", "更新Magisk到最新的canary版本，"),
    SEPOLICY_NOT_LOADED_3("re-install Magisk, Riru and Dreamland", "重新安装Magisk，Riru和梦境框架"),
    SEPOLICY_NOT_LOADED_4("or, report this problem to Magisk", "或将此问题报告给Magisk"),

    WRONG_RIRU_SECONTEXT_0("Riru libs have wrong SELinux permission or context", "Riru 库存在错误的权限或 SELinux 上下文"),
    WRONG_RIRU_SECONTEXT_1("If re-install Riru and Dreamland cannot solve this problem, ", "如果重新安装 Riru 和梦境框架无法解决此问题，"),
    WRONG_RIRU_SECONTEXT_2("then this is most likely due to your ROM maintainer added wrong SEPolicy rules", "那么这最有可能是因为您的ROM维护者添加了错误的SELinux规则"),
    WRONG_RIRU_SECONTEXT_3("please ask your ROM maintainer not to do this or switch to other ROMs.", "请告诉该ROM维护者删除这些错误规则或选择其他ROM"),
    WRONG_RIRU_SECONTEXT_4("For more information, please visit https://github.com/RikkaApps/Riru/issues/203", "有关更多信息，请访问 https://github.com/RikkaApps/Riru/issues/203"),

    NO_PROBLEM("No problems determined", "错误诊断未能确定问题");

    @SuppressLint("ConstantLocale")
    private static final boolean CHINESE = "zh".equalsIgnoreCase(Locale.getDefault().getLanguage());

    private String english, chinese;

    Alert(String one) {
        this.english = this.chinese = one;
    }

    Alert(String eng, String zh) {
        this.english = eng;
        this.chinese = zh;
    }

    Alert(Alert src) {
        this.english = src.english;
        this.chinese = src.chinese;
    }

    public void show(Object... args) {
        System.out.println("- " + String.format(best(), args));
    }

    public void showAsError(Object... args) {
        System.out.println("! " + String.format(best(), args));
    }

    public String best() {
        return CHINESE ? chinese : english;
    }
}
