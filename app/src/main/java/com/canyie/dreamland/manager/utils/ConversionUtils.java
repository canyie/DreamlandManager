package com.canyie.dreamland.manager.utils;

/**
 * @author canyie
 * @date 2019/12/17.
 */
public final class ConversionUtils {
    private ConversionUtils() {
    }

    public static Boolean str2BooleanStrict(String text) {
        if ("true".equalsIgnoreCase(text)) {
            return Boolean.TRUE;
        } else if ("false".equalsIgnoreCase(text)) {
            return Boolean.FALSE;
        } else {
            throw new IllegalArgumentException("invalid boolean text: " + text);
        }
    }
}
