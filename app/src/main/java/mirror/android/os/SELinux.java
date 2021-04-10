package mirror.android.os;

import top.canyie.dreamland.manager.utils.reflect.Reflection;
/**
 * Mirror class of android.os.SELinux
 * @author canyie
 */
@SuppressWarnings({"unused", "WeakerAccess"})
public final class SELinux {
    public static final String NAME = "android.os.SELinux";
    public static final Reflection<?> REF = Reflection.on(NAME);
    public static final Reflection.MethodWrapper isSELinuxEnabled = REF.method("isSELinuxEnabled");
    public static final Reflection.MethodWrapper isSELinuxEnforced = REF.method("isSELinuxEnforced");
    public static final Reflection.MethodWrapper getContext = REF.method("getContext");
    public static final Reflection.MethodWrapper getFileContext = REF.method("getFileContext", String.class);
    public static final Reflection.MethodWrapper checkSELinuxAccess = REF.method("checkSELinuxAccess", String.class, String.class, String.class, String.class);

    private SELinux() {
        throw new InstantiationError("Mirror class mirror.android.os.SELinux");
    }
}
