package com.l7tech.server.identity.ldap;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Inherited;
import java.lang.annotation.Target;
import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * Annotation for use on classes that require the LDAP context classloader.
 */
@Documented
@Retention(value = RUNTIME)
@Inherited
@Target({TYPE})
public @interface LdapClassLoaderRequired {
}
