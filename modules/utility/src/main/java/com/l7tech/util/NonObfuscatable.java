package com.l7tech.util;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;
import static java.lang.annotation.ElementType.*;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * Annotation for classes, method and fields that should not be obfuscated.
 */
@Documented
@Retention(value=RUNTIME)
@Target({TYPE, FIELD, METHOD, CONSTRUCTOR})
public @interface NonObfuscatable {

}
