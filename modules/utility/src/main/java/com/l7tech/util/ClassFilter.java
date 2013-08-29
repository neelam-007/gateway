package com.l7tech.util;

import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;

/**
 * Filter used by {@link SafeXMLDecoder} to check if a class, constuctor or method should be permitted.
 */
public interface ClassFilter {

    /**
     * Check if the specified class should be permitted by the SafeXMLDecoder while XML decoding.
     * If false, the class will not even be loaded.
     *
     * @param classname the fully qualified binary name of the class.  Required.
     * @return true if this class is allowed to appear in XML being decoded.
     */
    boolean permitClass(@NotNull String classname);

    /**
     * Check if the specified constructor should be permitted by the SafeXMLDecoder while decoding XML.
     *
     * @param constructor a constructor that the encoded XML wishes to invoke.  Required.
     * @return true if this constructor is safe to allow encoded XML to invoke; false if
     *         use of this constructor should not be allowed.
     */
    boolean permitConstructor(@NotNull Constructor<?> constructor);

    /**
     * Check if the specified method should be permitted by the SafeXMLDecoder while decoding XML.
     *
     * @param method a method that the encoded XML wishes to invoke.  Required.
     * @return true if this method is safe to allow encoded XML to invoke;
     *         false if use of this constructor should not be allowed.
     */
    boolean permitMethod(@NotNull Method method);
}
