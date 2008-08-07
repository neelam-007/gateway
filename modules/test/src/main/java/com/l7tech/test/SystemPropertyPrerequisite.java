package com.l7tech.test;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Inherited;
import java.lang.annotation.Target;
import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 *
 */
@Documented
@Retention(value = RUNTIME)
@Inherited
@Target({METHOD})
public @interface SystemPropertyPrerequisite {
    String require() default "";
    String unless() default "";
}
