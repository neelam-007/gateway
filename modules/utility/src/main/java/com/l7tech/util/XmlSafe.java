package com.l7tech.util;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.*;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * Indicates to {@link AnnotationClassFilter} whether the annotated class, constructor, or method is safe to be invoked automatically by
 * possibly-hostile XML parsed by the {@link SafeXMLDecoder}.
 */
@Documented
@Retention(value = RUNTIME)
@Target({TYPE, CONSTRUCTOR, METHOD})
public @interface XmlSafe {
    /**
     * Check if the annotated class, constructor or method is safe for invocation during parsing of encoded XML.
     *
     * @return true if the annotated class, constructor or method is safe for invocation while parsing encoded XML.
     */
    boolean safe() default true;

    /**
     * Check if the annotated class's default (zero-argument) constructor is permitted.
     * As this is the common case for java beans this defaults to true.
     *
     * @return true if the annotated class's default constructor should be considered safe for invocation.
     */
    boolean allowDefaultConstructor() default true;

    /**
     * Check if the annotated class promises that all of its constructors are safe for invocation during
     * parsing of encoded XML (assuming that specific parameter values are also found to be safe).
     * <p/>
     * This feature should be used extremely sparingly -- it is safe to annotate every constructor individually.
     * Future code changes may introduce an unsafe constructor.
     *
     * @return true if all constructors of the annotated class should be considered safe for invocation.
     */
    boolean allowAllConstructors() default false;

    /**
     * Check if the annotated class promises that all of its methods with names starting with the substring "set"
     * are safe for invocation during parsing of encoded XML (assuming that specific parameter values are also found
     * to be safe).
     * <p/>
     * This feature should be used extremely sparingly -- it is safer to annotate every method individually.
     * Future code changes may introduce an unsafe method.
     *
     * @return true to allow all set methods of an annotated class to be invoked by encoded XML.
     */
    boolean allowAllSetters() default false;
}
