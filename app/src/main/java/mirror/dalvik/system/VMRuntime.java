package mirror.dalvik.system;

import com.canyie.dreamland.manager.utils.reflect.Reflection;

/**
 * @author canyie
 */
@SuppressWarnings("WeakerAccess") public final class VMRuntime {
    public static final String NAME = "dalvik.system.VMRuntime";
    public static final Reflection<?> REF = Reflection.on(NAME);
    public static final Reflection.MethodWrapper getRuntime = REF.method("getRuntime");
    public static final Reflection.MethodWrapper requestHeapTrim = REF.method("requestHeapTrim");

    private VMRuntime() {
        throw new InstantiationError("Mirror class mirror.dalvik.system.VMRuntime");
    }
}
