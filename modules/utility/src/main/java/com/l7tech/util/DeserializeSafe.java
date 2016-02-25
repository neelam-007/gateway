package com.l7tech.util;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * Annotate a class being safe or unsafe to be deserialized using either {@code ClassFilterObjectInputStream}
 * or Gateway spring remoting {@code SecureHttpInvokerServiceExporter}.
 */
@Documented
@Retention(value = RUNTIME)
@Target(TYPE)
public @interface DeserializeSafe {
    /**
     * Check if the annotated class is safe for invocation during deserialization of serialized bytes.
     *
     * @return true if the annotated class is safe for invocation while deserializing serialized bytes.
     */
    boolean safe() default true;
}
