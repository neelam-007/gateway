package com.l7tech.util;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 *
 */
public class ClassUtilsTest {
    @Test
    public void testGetJavaTypeName() throws Exception {
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
    }
}
