package com.l7tech.policy.assertion.annotation;

import java.lang.annotation.*;

/**
 * This annotation is declared on an Assertion class to represent the concept that the Assertion
 * will fail at runtime unless the message being processed is SOAP.
 *
 * <p>The <code>wss</code> property should be true for any assertions that are WS-Security
 * related (defaults to false).</p>
 *
 * <br/><br/>
 * LAYER 7 TECHNOLOGIES, INC<br/>
 * User: flascell<br/>
 * Date: Mar 29, 2007<br/>
 */
@RequiresXML
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Inherited
@Target(ElementType.TYPE)
public @interface RequiresSOAP {
    boolean wss() default false;
}
