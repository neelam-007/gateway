/**
 * Copyright (C) 2006-2008 Layer 7 Technologies Inc.
 */
package com.l7tech.gateway.common.security.rbac;

import com.l7tech.objectmodel.EntityType;

import java.lang.annotation.Documented;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * Documents the permission that the current subject must hold in order to invoke the annotated method,
 * or any method on the annotated class.
 * <p/>
 * If {@link #types} is specified on a class annotation, it is inherited if unspecified in that class's
 * method annotations.  Conversely, a {@link #types} specified in a method annotation overrides any specified
 * in a class annotation.
 */
@Documented
@Retention(value = RUNTIME)
@Inherited
@Target({TYPE, METHOD})
public @interface Secured {
    /**
     * The type of Entity that the annotated method or class operates on.
     */
    EntityType[] types() default EntityType.ANY;

    /**
     * The known stereotype that the annotated persistence-related admin API method conforms to.
     * Admin APIs that do not conform to a listed stereotype requires a customInterceptor classname to
     * be specified.  Otherwise, the RBAC enforcment layer will throw a RuntimeException if such methods are encountered.
     *
     * @return the method stereotype to use, or MethodStereotype.NONE if not defined.  If no stereotype is defined
     * a valid {@link #customInterceptor()} must be specified or else all invocations of the annotated method
     * will be rejected.
     */
    MethodStereotype stereotype() default MethodStereotype.NONE;

    /**
     * The index of the argument that is expected to contain the relevant Entity, EntityHeader, or entity identifier
     * (depending on the method stereotype), where an index of 0 means the first argument.
     *
     * @return the argument index of the relevant entity argument, or -1 if not defined.
     */
    int relevantArg() default -1;

    /**
     * The type of operation that must be matched by any {@link OperationType#OTHER} permission in order for that
     * permission to apply to requests to the annotated method.
     *
     * @return the special other operation type name, or empty string if not defined.
     */
    String otherOperation() default "";

    /**
     * Fully qualified class name of a custom interceptor class implementing CustomRbacInterceptor, or empty string.
     * <p/>
     * If specified, the named class must exist and be available in the JVM on which the RBAC interceptor runs,
     * in the same classloader as the annotated interface, and must have a public nullary constructor.
     * <p/>
     * <b>The custom interceptor becomes responsbile for <em>ALL</em> RBAC enforcement for the annotated method.</b>
     * <p/>
     * For every invocation of the annotated method the RBAC interceptor will obtain an instance of this class,
     * call its setters, and then call its
     * invoke() method to do before checks, invoke the action, and perform any after checks.
     * <p/>
     * The custom interceptor is NOT guaranteed to be a fresh instance for each invocation, but nor is it guaranteed
     * to be the same instance every time.  Pooling of interceptor instances may or may not be used and neither its use
     * nor disuse should be assumed by custom interceptors.  It is guaranteed that interceptor instances will only
     * ever be used with one thread at a time.
     * <p/>
     * <b>NOTE:</b> If a custom interceptor is used, the RBAC interceptor will <b>NOT</b> perform any of its
     * regular before-invocation or after-invocation checks or processing, including any exception and return value
     * filtering that would otherwise have been performed.
     *
     * @return name of custom interceptor class, or empty string if no custom class is to be used.
     */
    String customInterceptor() default "";
}
