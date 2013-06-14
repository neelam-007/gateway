package com.l7tech.search;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * This was created: 6/12/13 as 11:35 AM
 *
 * @author Victor Kazakov
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD})
public @interface Dependencies {
    /**
     * If a properties method returns more then one properties that are dependencies use this to supply an array of
     * dependencies.
     *
     * @return The list of dependencies that this method returns
     */
    Dependency[] value() default {};
}
