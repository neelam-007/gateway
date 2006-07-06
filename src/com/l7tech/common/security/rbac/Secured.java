/**
 * Copyright (C) 2006 Layer 7 Technologies Inc.
 */
package com.l7tech.common.security.rbac;

import java.lang.annotation.Documented;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.TYPE;
import java.lang.annotation.Retention;
import static java.lang.annotation.RetentionPolicy.RUNTIME;
import java.lang.annotation.Target;

/**
 * Documents the permission that the current subject must hold in order to invoke the annotated method,
 * or any method on the annotated class.
 *
 * 
 */
@Documented
@Retention(value = RUNTIME)
@Target({TYPE, METHOD})
public @interface Secured {
    /**
     * The type of Entity that the operation operates on.
     */
    EntityType type();

    /**
     * The set of operations that the decorated method may attempt to perform
     */
    OperationType[] operations();

    /**
     *
     */
    String[] otherOperationNames() default {};
}
