package com.l7tech.policy.assertion.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Inherited;
import java.lang.annotation.Target;
import java.lang.annotation.ElementType;

/**
 * This marker annotation is used on an Assertion class to declare that the
 * Assertion will operate on the request Message.
 *
 * <p>An Assertion that can operate on either the request or the response
 * should not use this annotation.</p>
 *
 * @author Steve Jones
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Inherited
@Target(ElementType.TYPE)
public @interface ProcessesRequest {
}
