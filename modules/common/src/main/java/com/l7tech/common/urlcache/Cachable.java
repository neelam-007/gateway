package com.l7tech.common.urlcache;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * Annotate whether the reponse from a method can be cached.
 *
 * @author Steve Jones
 */
@Documented
@Retention(value = RUNTIME)
@Target(METHOD)
public @interface Cachable {

    /**
     * The key for the cached object.
     */
    int relevantArg() default -1;

    /**
     * The maximum age for the cached object (milliseconds).
     */
    int maxAge() default 1000;
}