package com.l7tech.util;

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
    @Override
    public Object invoke(final Object proxy,
                         final Method method,
                         final Object[] args) throws Throwable {
        Object result;

        if ( method.getDeclaringClass().equals(mappedClass) ) {
            String methodName = method.getName();
            if ( methodName.startsWith( "get" )) {
                result = values.get(methodName.substring(3,4).toLowerCase() + methodName.substring(4));
            } else if ( methodName.startsWith("is") ) {
                result = values.get(methodName.substring(2,3).toLowerCase() + methodName.substring(3));
            } else {
                result = values.get(methodName);
            }
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
