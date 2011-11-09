package com.l7tech.test.util;

import static org.junit.Assert.fail;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

/**
 * Utility methods for use in tests
 */
public class TestUtils {

    /**
     * Enforce that a subclass overrides all methods of its superclasses.
     */
    public static <T> void assertOverridesAllMethods( final Class<T> delegateClass,
                                                      final Class<? extends T> implementationClass ) {
        try {
            for ( final Method method : delegateClass.getMethods() ) {
                if ( Modifier.isStatic( method.getModifiers() ) ||
                     Modifier.isFinal( method.getModifiers() )   ) {
                    continue;
                }
                if ( Object.class.equals( method.getDeclaringClass() ) ) {
                    continue;
                }

                implementationClass.getDeclaredMethod( method.getName(), method.getParameterTypes() );
            }
        } catch ( NoSuchMethodException e ) {
            fail( "Method not found: " + e.getMessage() );
        }
    }
}
