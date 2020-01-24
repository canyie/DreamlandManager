package com.canyie.dreamland.manager.utils.callbacks;

/**
 * @author canyie
 * @date 2019/12/11.
 */
@FunctionalInterface
public interface ExceptionCallback<E extends Exception> {
    void onException(E e);
}
