package com.l7tech.test.util;

import org.hamcrest.Matchers;
import org.junit.Assert;

import static org.junit.Assert.fail;

import java.lang.reflect.Field;
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

    /**
     * Get private field value for the specified {@code obj}.
     *
     * @param obj          the {@code Object} to get field value from.  Required and cannot be {@code null}.
     * @param fieldName    the field name.  Required and cannot be empty or {@code null}.
     * @param fieldClass   the field type.  Required and cannot be {@code null}.
     * @return the value of the filed (specified with {@code fieldName} and {@code fieldClass}) from the specified {@code obj}.
     */
    public static <T> T getFieldValue(final Object obj, final String fieldName, final Class<T> fieldClass) {
        Assert.assertNotNull(obj);
        Assert.assertThat(fieldName, Matchers.not(Matchers.isEmptyOrNullString()));
        Assert.assertNotNull(fieldClass);

        final Class<?> objClass = obj.getClass();
        try {
            final Field field = objClass.getDeclaredField(fieldName);
            if (field == null) {
                Assert.fail("field '" + fieldName + "' is missing from class '" + objClass + "'");
                throw new RuntimeException("field '" + fieldName + "' is missing from class '" + objClass + "'");
            }
            field.setAccessible(true);
            final Object ret = field.get(obj);
            Assert.assertThat(ret, Matchers.allOf(Matchers.notNullValue(), Matchers.instanceOf(fieldClass)));
            return fieldClass.cast(ret);
        } catch (Exception e) {
            Assert.fail("Exception while extracting field '" + fieldName + "' is missing from class '" + objClass + "':" + System.lineSeparator() + e.getMessage());
            throw new RuntimeException(e);
        }
    }

    /**
     * Get private static field value for the specified class.
     *
     * @param aClass       the class to get the field value from.  Required and cannot be {@code null}.
     * @param fieldName     the field name.  Required and cannot be empty or {@code null}.
     * @param fieldClass    the field type.  Required and cannot be {@code null}.
     * @return the value of the filed (specified with {@code fieldName} and {@code fieldClass}) from the specified {@code class}.
     */
    public static <T> T getFieldValue(final Class<?> aClass, final String fieldName, final Class<T> fieldClass) {
        Assert.assertNotNull(aClass);
        Assert.assertThat(fieldName, Matchers.not(Matchers.isEmptyOrNullString()));
        Assert.assertNotNull(fieldClass);
        try {
            final Field field = aClass.getDeclaredField(fieldName);
            if (field == null) {
                Assert.fail("field '" + fieldName + "' is missing from class '" + aClass + "'");
                throw new RuntimeException("field '" + fieldName + "' is missing from class '" + aClass + "'");
            }
            field.setAccessible(true);
            final Object ret = field.get(null);
            Assert.assertThat(ret, Matchers.allOf(Matchers.notNullValue(), Matchers.instanceOf(fieldClass)));
            return fieldClass.cast(ret);
        } catch (Exception e) {
            Assert.fail("Exception while extracting field '" + fieldName + "' is missing from class '" + aClass + "':" + System.lineSeparator() + e.getMessage());
            throw new RuntimeException(e);
        }
    }
}
