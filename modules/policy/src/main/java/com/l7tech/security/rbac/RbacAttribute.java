package com.l7tech.security.rbac;

import org.apache.commons.lang.StringUtils;

import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * Identifies a field (via its getter) which can be used to define an RBAC AttributePredicate.
 */
@Inherited
@Retention(value = RUNTIME)
@Target(ElementType.METHOD)
public @interface RbacAttribute {

    /**
     * @return an identifier for this RbacAttribute which can be used to look up a display name in a resource bundle. Default is an empty string.
     */
    String displayNameIdentifier() default StringUtils.EMPTY;
}
