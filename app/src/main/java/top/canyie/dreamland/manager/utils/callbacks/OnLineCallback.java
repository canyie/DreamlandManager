package top.canyie.dreamland.manager.utils.callbacks;

/**
 * @author canyie
 */
@FunctionalInterface
public interface OnLineCallback {
    boolean onLine(String line);
}
