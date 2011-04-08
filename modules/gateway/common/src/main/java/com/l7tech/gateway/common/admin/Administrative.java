package com.l7tech.gateway.common.admin;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Inherited;
import java.lang.annotation.Target;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * Documents the administative options for authentication and licensing.
 *
 * <p>By default all administrative methods require that the user is
 * authenticated and that the gateway is licensed for administration.</p>
 *
 * <p>This annotation is used on an administrative interface methods to
 * denote that authentication or license feature is not required.</p>
 *
 * <p>This annotation is used on implementation classes to denote that
 * the class requires administrative processing.</p>
 *
 * <p>Note that this is distinct from security checks for admin beans.</p>
 *
 * @author Steve Jones
 * @see com.l7tech.gateway.common.security.rbac.Secured
 */
@Documented
@Retention(value = RUNTIME)
@Inherited
@Target({METHOD, TYPE})
public @interface Administrative {

    /**
     * Set to false if the method does not require an authenticated user.
     *
     * @return True if the caller must be authenticated.
     */
    boolean authenticated() default true;

    /**
     * Set to false if the method does not require a valid license.
     *
     * @return True if the method can be invoked without being licensed.
     */
    boolean licensed() default true;

    /**
     * Set to true if this method is treated as "background" activity (such as audit/dashboard requests)
     *
     * @return True if the method can be invoked without user interaction.
     */
    boolean background() default false;
}
