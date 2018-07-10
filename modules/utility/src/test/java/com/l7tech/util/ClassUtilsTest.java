package com.l7tech.util;

import org.junit.Test;

import java.lang.reflect.Array;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 *
 */
public class ClassUtilsTest {
    @Test
    public void testGetJavaTypeName() throws Exception {
        TestInner anon = new TestInner() {
            @Override
            public Blah b() {
                return new Blah() {
                    class Blarg implements Blah {
                        @Override
                        public Object h() {
                            return this;
                        }
                    }

                    @Override
                    public Object h() {
                        return new Blarg();
                    }
                };
            }
        };

        Object inner1 = anon.b();
        Object inner2 = anon.b().h();

        assertEquals("java.lang.String", ClassUtils.getJavaTypeName(String.class));
        assertEquals("boolean", ClassUtils.getJavaTypeName(boolean.class));
        assertEquals("java.lang.Boolean", ClassUtils.getJavaTypeName(Boolean.class));
        assertEquals("void", ClassUtils.getJavaTypeName(void.class));
        assertEquals("java.lang.Void", ClassUtils.getJavaTypeName(Void.class));
        assertEquals("int", ClassUtils.getJavaTypeName(int.class));
        assertEquals("java.lang.Integer", ClassUtils.getJavaTypeName(Integer.class));
        assertEquals("int[]", ClassUtils.getJavaTypeName(int[].class));
        assertEquals("java.lang.Double[][]", ClassUtils.getJavaTypeName(Double[][].class));
        assertEquals("int[][][]", ClassUtils.getJavaTypeName(int[][][].class));
        assertEquals("java.lang.String[][][][]", ClassUtils.getJavaTypeName(String[][][][].class));
        assertEquals("java.lang.String[]", ClassUtils.getJavaTypeName(String[].class));
        assertEquals("com.l7tech.util.ClassUtilsTest$TestInner", ClassUtils.getJavaTypeName(TestInner.class));
        assertEquals("com.l7tech.util.ClassUtilsTest$TestInner[][]", ClassUtils.getJavaTypeName(TestInner[][].class));
        assertEquals("com.l7tech.util.ClassUtilsTest$TestInner[]", ClassUtils.getJavaTypeName(Array.newInstance(TestInner.class, 10).getClass()));
        assertEquals("com.l7tech.util.ClassUtilsTest$TestInner[][][]", ClassUtils.getJavaTypeName(Array.newInstance(TestInner.class, 10, 20, 44).getClass()));
        assertEquals("com.l7tech.util.ClassUtilsTest$1$1", ClassUtils.getJavaTypeName(inner1.getClass()));
        assertEquals("com.l7tech.util.ClassUtilsTest$1$1$Blarg", ClassUtils.getJavaTypeName(inner2.getClass()));
        assertEquals("com.l7tech.util.ClassUtilsTest$1$1$Blarg[][]", ClassUtils.getJavaTypeName(Array.newInstance(inner2.getClass(), 4, 4).getClass()));
    }

    @Test
    public void isPrimitiveWrapper() throws Exception {
        assertTrue(ClassUtils.isPrimitiveWrapper(Boolean.class));
        assertTrue(ClassUtils.isPrimitiveWrapper(Byte.class));
        assertTrue(ClassUtils.isPrimitiveWrapper(Character.class));
        assertTrue(ClassUtils.isPrimitiveWrapper(Short.class));
        assertTrue(ClassUtils.isPrimitiveWrapper(Integer.class));
        assertTrue(ClassUtils.isPrimitiveWrapper(Long.class));
        assertTrue(ClassUtils.isPrimitiveWrapper(Double.class));
        assertTrue(ClassUtils.isPrimitiveWrapper(Float.class));

        assertFalse(ClassUtils.isPrimitiveWrapper(String.class));
    }

    @Test
    public void wrapperArrayToPrimitiveArray() throws Exception {
        assertEquals(boolean[].class, ClassUtils.wrapperArrayToPrimitiveArray(Boolean[].class));
        assertEquals(byte[].class, ClassUtils.wrapperArrayToPrimitiveArray(Byte[].class));
        assertEquals(char[].class, ClassUtils.wrapperArrayToPrimitiveArray(Character[].class));
        assertEquals(short[].class, ClassUtils.wrapperArrayToPrimitiveArray(Short[].class));
        assertEquals(int[].class, ClassUtils.wrapperArrayToPrimitiveArray(Integer[].class));
        assertEquals(long[].class, ClassUtils.wrapperArrayToPrimitiveArray(Long[].class));
        assertEquals(double[].class, ClassUtils.wrapperArrayToPrimitiveArray(Double[].class));
        assertEquals(float[].class, ClassUtils.wrapperArrayToPrimitiveArray(Float[].class));
    }

    @Test
    public void primitiveArrayToWrapperArray() throws Exception {
        assertEquals(Boolean[].class, ClassUtils.primitiveArrayToWrapperArray(boolean[].class));
        assertEquals(Byte[].class, ClassUtils.primitiveArrayToWrapperArray(byte[].class));
        assertEquals(Character[].class, ClassUtils.primitiveArrayToWrapperArray(char[].class));
        assertEquals(Short[].class, ClassUtils.primitiveArrayToWrapperArray(short[].class));
        assertEquals(Integer[].class, ClassUtils.primitiveArrayToWrapperArray(int[].class));
        assertEquals(Long[].class, ClassUtils.primitiveArrayToWrapperArray(long[].class));
        assertEquals(Double[].class, ClassUtils.primitiveArrayToWrapperArray(double[].class));
        assertEquals(Float[].class, ClassUtils.primitiveArrayToWrapperArray(float[].class));
    }

    public static class TestInner {
        interface Blah {
            Object h();
        }

        public Blah b() {
            return null;
        }
    }


}
