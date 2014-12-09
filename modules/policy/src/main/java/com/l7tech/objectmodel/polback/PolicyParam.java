package com.l7tech.objectmodel.polback;

import java.lang.annotation.*;

/**
 * Describes the context variable name and Layer 7 data type of a parameter accepted by or returned by a policy-backed
 * service invocation.
 */
@Documented
@Retention( RetentionPolicy.RUNTIME )
@Target( { ElementType.METHOD, ElementType.PARAMETER } )
public @interface PolicyParam {
    /**
     * @return the name of the parameter.
     */
    String value();

    /**
     * @return the Layer 7 data type name for the parameter (eg string, message, cert, dateTime, etc), or an empty string
     * to force the type to be guessed from the runtime type.
     */
    String dataTypeName() default "";
}
