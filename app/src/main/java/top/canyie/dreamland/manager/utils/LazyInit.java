package top.canyie.dreamland.manager.utils;

/**
 * Lazy initialization helper. This class is not thread-safe.
 * @author canyie
 */
public class LazyInit<T, A, E extends Throwable> {
    private T value;
    private final Initializer<T, A, E> initializer;

    public LazyInit(Initializer<T, A, E> initializer) {
        this.initializer = initializer;
    }

    public T get(A arg) throws E {
        if (value == null) {
            value = initializer.create(arg);
        }
        return value;
    }

    @FunctionalInterface public interface Initializer<T, A, E extends Throwable> {
        T create(A arg) throws E;
    }
}
