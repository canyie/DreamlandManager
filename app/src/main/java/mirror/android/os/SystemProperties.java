package mirror.android.os;

import com.canyie.dreamland.manager.utils.reflect.Reflection;

/**
 * Mirror class of {@link android.os.SystemProperties}
 * @author canyie
 * @date 2019/12/13.
 */
public final class SystemProperties {
    public static final String NAME = "android.os.SystemProperties";
    public static final Reflection<?> REF = Reflection.on(NAME);
    public static final Reflection.MethodWrapper get = REF.method("get", String.class, String.class);

    private SystemProperties() {
        throw new InstantiationError("Mirror class mirror.android.os.SystemProperties");
    }
}
