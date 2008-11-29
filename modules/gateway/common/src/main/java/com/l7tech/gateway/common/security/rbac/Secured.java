/**
 * Copyright (C) 2006-2008 Layer 7 Technologies Inc.
 */
package com.l7tech.gateway.common.security.rbac;

import com.l7tech.objectmodel.EntityType;

import java.lang.annotation.Documented;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.TYPE;
import java.lang.annotation.Retention;
import static java.lang.annotation.RetentionPolicy.RUNTIME;
import java.lang.annotation.Target;
import java.lang.annotation.Inherited;

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

    MethodStereotype stereotype() default MethodStereotype.NONE;

    int relevantArg() default -1;
}
