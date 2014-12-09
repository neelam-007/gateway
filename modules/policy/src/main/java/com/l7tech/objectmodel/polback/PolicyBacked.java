package com.l7tech.objectmodel.polback;

import java.lang.annotation.*;

/**
 * Annotation used to mark up an interface that will be implemented by a policy-backed service.
 * <P/>
 * Interface with this annotation may not include more than one method with the same name, regardless of argument types (ie, no overloaded methods).
 * Parameter and return values must be those mapped by the {@link com.l7tech.policy.variable.DataType} class, with the exception that a method may return
 * a value of type Map provided it is annotated with a {@link PolicyBackedMethod} annotation that describes the result names and types.
 * Parameters must be annotated with the {@link PolicyParam} annotation in order to specify the parameter name (since parameter names cannot be
 * accessed via reflection).
 */
@Documented
@Retention( RetentionPolicy.RUNTIME )
@Target( ElementType.TYPE )
public @interface PolicyBacked {
    /**
     * @return the human-readable policy tag to display in the database for this interface.  If not specified, a
     *         tag will be generated based on the interface class name.
     */
    // TODO do we really want to have this, or is it OK to just use the class name in the UI?
    //      if we do it like this, do we want it in policy tag format eg "data-key-value-store", "data-key-value-store-searchable"
    //      or should we make it arbitrarily human readable (English only?) like "Key Value Store", "Searchable Key Value Store"
    // String policyTag() default "";
}