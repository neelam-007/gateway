package com.l7tech.gateway.common.security.rbac;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Inherited;
import java.lang.annotation.Target;
import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * Annotation added to a class to defined the permissions required for use.
 *
 * <p>If the type is "ALL" then an empty permission set means that the class
 * is accessible without any permission.</p>
 *
 * <p>If the type is "ANY" then at least one permission must be granted to
 * access the class. 
 */
@Documented
@Retention(value = RUNTIME)
@Inherited
@Target(TYPE)
public @interface RequiredPermissionSet {

    /**
     * The composition type of the permission set.
     */
    enum Type {
        /**
         * The user must have been granted any one of the permissions in the set (but does not need all of them).
         */
        ANY,

        /**
         * the user have been granted all of the permissions in the set.
         */
        ALL
    }

    /**
     * Get the compisition type for the set.
     *
     * @return The composition type.
     */
    Type type() default Type.ALL;

    /**
     * Get the set of required permission.
     *
     * @return The permission set.
     */
    RequiredPermission[] requiredPermissions() default {};
}
