package com.l7tech.external.assertions.gatewaymanagement.server.rest.resource;


import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Use this to limit the number of choices for a rest parameter. If an invalid choice is received then a 403 exception
 * will be returned. This can only be applied to String parameters
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.PARAMETER)
public @interface ChoiceParam {
    String[] value();

    boolean caseSensitive() default true;
}