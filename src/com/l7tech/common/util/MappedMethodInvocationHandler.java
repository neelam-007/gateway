package com.l7tech.common.util;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.InvocationTargetException;
import java.util.Map;
import java.io.Serializable;

/**
 * InvocationHandler for exposing a "bean" interface for a Map.
 *
 * <p>Get methods such as "getName" would return the value from the Map with
 * the key "name".</p>
 * 
 * @author Steve Jones
 */
public class MappedMethodInvocationHandler implements InvocationHandler, Serializable {

    //- PUBLIC

    /**
     * Create a new InvocationHandler for the given Class (interface) and values.
     *
     * @param mappedClass The interface class
     * @param values The map of "bean" properties 
     */
    public MappedMethodInvocationHandler(final Class mappedClass,
                                         final Map<String,Object> values) {
        this.mappedClass = mappedClass;
        this.values = values;
    }

    /**
     * 
     */
    public Object invoke(final Object proxy,
                         final Method method,
                         final Object[] args) throws Throwable {
        Object result = null;

        if ( method.getDeclaringClass().equals(mappedClass) ) {
            String methodName = method.getName();
            result = values.get(methodName.substring(3,4).toLowerCase() + methodName.substring(4));
        } else {
            try {
                result = method.invoke(this, args);
            } catch (InvocationTargetException ite) {
                throw ite.getCause();
            }
        }

        return result;
    }

    //- PRIVATE

    private final Class mappedClass;
    private final Map<String,Object> values;
}
