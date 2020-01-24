package mirror.android.os;

import com.canyie.dreamland.manager.utils.reflect.Reflection;
/**
 * Mirror class of {@link android.os.SELinux}
 * @author canyie
 * @date 2019/12/21.
 */
@SuppressWarnings("unused")
public final class SELinux {
    public static final String NAME = "android.os.SELinux";
    public static final Reflection<?> REF = Reflection.on(NAME);
    public static final Reflection.MethodWrapper isSELinuxEnabled = REF.method("isSELinuxEnabled");
    public static final Reflection.MethodWrapper isSELinuxEnforced = REF.method("isSELinuxEnforced");
    public static final Reflection.MethodWrapper getContext = REF.method("getContext");
    public static final Reflection.MethodWrapper getFileContext = REF.method("getFileContext", String.class);

    private SELinux() {
        throw new InstantiationError("Mirror class mirror.android.os.SELinux");
    }
}
