package com.l7tech.policy.assertion.annotation;

import java.lang.annotation.*;

/**
 * Annotations used by assertions to indicate that a property value is represented in base64. Annotation must be
 * placed on the getter.
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface Base64Value {

    /**
     * Name of instance method, if any, which can be used to obtain the decoded base64 properties value.
     *
     * @return method name, must be public and take no arguments. The actual method itself may return null.
     */
    String decodeMethodName() default "";
}
