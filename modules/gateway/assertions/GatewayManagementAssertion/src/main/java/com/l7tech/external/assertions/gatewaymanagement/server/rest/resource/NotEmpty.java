package com.l7tech.external.assertions.gatewaymanagement.server.rest.resource;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotating a rest parameter with @NotEmpty means that it can not be set as the empty string. By default Jersey
 * Integer, Long, Boolean, etc params use the default value if the value given is empty. This will make it fail
 * instead.
 * Any parameter annotation with @NotEmpty must have a static valueOf(String) method
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.PARAMETER)
public @interface NotEmpty {
}
