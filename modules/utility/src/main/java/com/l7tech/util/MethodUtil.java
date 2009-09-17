package com.l7tech.util;

import java.lang.reflect.Method;

/**
 * Utility methods for working with methods.
 */
public class MethodUtil {

    public static boolean isEqualsOrHashCodeOverridden(Class clazz) {
        try {
            Method equals = clazz.getMethod("equals", Object.class);
            if (equals.getDeclaringClass() != Object.class)
                return true;

            Method hashCode = clazz.getMethod("hashCode");
            return hashCode.getDeclaringClass() != Object.class;

        } catch (NoSuchMethodException e) {
            throw new RuntimeException(e); // Can't happen
        }
    }
}
