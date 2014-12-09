package com.l7tech.objectmodel.polback;

import java.lang.annotation.*;

/**
 * Specify information about a policy-backed method, such as its name and description,
 * and how the result returned by a policy-backed service invocation should be mapped to the return
 * value of the Java interface method describing it.
 * <p/>
 * If the reflected runtime return type of a method is void then no result will be produced and this annotation
 * will be ignored if present.
 */
@Documented
@Retention( RetentionPolicy.RUNTIME )
@Target( ElementType.METHOD )
public @interface PolicyBackedMethod {

    /**
     * The UI name of the policy backed method, or empty to default to the method's simple name.
     *
     * @return method name to show in UI, or empty.
     */
    String name() default "";

    /**
     * The long description of the policy backed method.
     *
     * @return method description to show in UI, or empty.
     */
    String description() default "";

    /**
     * The name and datatype of a single result, for methods that return a value other than Map.
     * <p/>
     * If the result data type name is UNKNOWN, the actual type will be guessed based on the reflected class
     * of the return value.  For example, for a method that returns String the result type will be set to "string";
     * for a method that returns java.util.Date the result type will be "dateTime"; for a method that
     * returns java.security.cert.X509Certificate the result type will be "cert"; etc.
     * <p/>
     * Only runtime types that exactly match one recognized by com.l7tech.policy.variable.DataTypeUtils#getDataTypeForClass(java.lang.Class<?>)
     * will be successfully inferred.
     * <p/>
     * If the method's runtime return type is java.util.Map then this value is ignored and the {@link #mapResults()} value
     * will be examined instead.
     *
     * @return the name and data type of a single result.
     */
    PolicyParam singleResult() default @PolicyParam( value = "result" );

    /**
     * The result names and data types for a multiple result, for methods that return Map.
     * <p/>
     * If the method's runtime return type is not Map (or void) then this value is ignored and the {@link #singleResult()} value
     * will be examined instead.
     *
     * @return the names and data types of the result values.
     */
    PolicyParam[] mapResults() default {};
}
