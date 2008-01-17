package com.l7tech.policy.assertion.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Inherited;
import java.lang.annotation.Target;
import java.lang.annotation.ElementType;

/**
 * Annotation for Assertions to indicate support for hardware accelerated processing.
 *
 * @author steve
 */
@Documented
@Retention( RetentionPolicy.RUNTIME )
@Inherited
@Target({ ElementType.TYPE, ElementType.ANNOTATION_TYPE })
public @interface HardwareAccelerated {

    Type[] type();
    
    enum Type { SCHEMA, XSLT }
}
