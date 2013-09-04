package com.l7tech.util;

import org.junit.Test;

import java.lang.reflect.Array;

import static org.junit.Assert.assertEquals;

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

    public static class TestInner {
        interface Blah {
            Object h();
        }

        public Blah b() {
            return null;
        }
    }


}
