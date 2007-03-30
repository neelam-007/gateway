package com.l7tech.policy.assertion.annotation;

import java.lang.annotation.*;

/**
 * This annotation is declared on an Assetion class to represent the concept that the Assertion
 * will fail at runtime unless the message being processed is XML.
 * <p/>
 * <p/>
 * <br/><br/>
 * LAYER 7 TECHNOLOGIES, INC<br/>
 * User: flascell<br/>
 * Date: Mar 29, 2007<br/>
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Inherited
@Target({ElementType.TYPE, ElementType.ANNOTATION_TYPE})
public @interface RequiresXML {
}
