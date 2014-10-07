package com.l7tech.external.assertions.gatewaymanagement.server.rest.resource;


import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * This is used to track API versions
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.PARAMETER, ElementType.METHOD, ElementType.TYPE})
public @interface Since {
    /**
     * The version number that this method or parameter was introduced.
     *
     * @return The version number that this method or parameter was introduced.
     */
    RestManVersion value();
}