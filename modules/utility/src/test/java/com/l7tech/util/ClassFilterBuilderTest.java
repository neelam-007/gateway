package com.l7tech.util;

import org.junit.After;
import org.junit.Test;

import java.util.HashMap;
import java.util.Hashtable;
import java.util.LinkedHashMap;
import java.util.List;

import static org.junit.Assert.*;

/**
 * Test case for {@link ClassFilterBuilder}.
 */
public class ClassFilterBuilderTest {

    public ClassFilterBuilderTest() {}

    public static class A {
        public A() {}

        @SuppressWarnings("UnusedDeclaration")
        public A(String dummy1, int dummy2, ClassFilterBuilderTest dummy3) {}

        @SuppressWarnings("UnusedDeclaration")
        public void setDummy(String dummy1, boolean dummy2) {}
    }

    @After
    public void cleanup() {
        SyspropUtil.clearProperties(
            SafeXMLDecoder.PROP_DISABLE_FILTER,
            ClassFilterBuilder.PROP_WHITELIST_CLASSES,
            ClassFilterBuilder.PROP_WHITELIST_CONSTRUCTORS,
            ClassFilterBuilder.PROP_WHITELIST_METHODS
        );
    }

    @Test
    public void testEmpty() throws Exception {
        ClassFilter cf = new ClassFilterBuilder().
            build();
        CompositeClassFilter comp = (CompositeClassFilter) cf;
        List<ClassFilter> filters = comp.delegates;
        assertTrue(filters.isEmpty());

        assertFalse(cf.permitClass("java.util.LinkedHashMap"));
        assertFalse(cf.permitClass("java.util.Hashtable"));
        assertFalse(cf.permitClass("java.lang.ProcessBuilder"));

        assertFalse(cf.permitConstructor(LinkedHashMap.class.getConstructor()));
        assertFalse(cf.permitConstructor(Hashtable.class.getConstructor()));

        assertFalse(cf.permitMethod(HashMap.class.getMethod("put", Object.class, Object.class)));
        assertFalse(cf.permitMethod(Hashtable.class.getMethod("put", Object.class, Object.class)));

    }

    @Test
    public void testDefaults() throws Exception  {
        ClassFilter cf = new ClassFilterBuilder().
            allowDefaults().
            build();

        CompositeClassFilter comp = (CompositeClassFilter) cf;
        List<ClassFilter> filters = comp.delegates;

        assertEquals(2, filters.size());

        AnnotationClassFilter acf = (AnnotationClassFilter) filters.get(0);
        assertNotNull(acf);

        StringClassFilter scf = (StringClassFilter) filters.get(1);
        assertTrue(scf.classes.contains("java.util.LinkedHashMap"));
        assertEquals(ClassFilterBuilder.DEFAULT_CLASSES.size(), scf.classes.size());

        assertTrue(scf.constructors.contains("java.util.LinkedHashMap()"));
        assertEquals(ClassFilterBuilder.DEFAULT_CONSTRUCTORS.size(), scf.constructors.size());

        assertTrue(scf.methods.contains("java.util.HashMap.put(java.lang.Object,java.lang.Object)"));
        assertEquals(ClassFilterBuilder.DEFAULT_METHODS.size(), scf.methods.size());

        assertTrue(cf.permitClass("java.util.LinkedHashMap"));
        assertFalse(cf.permitClass("java.util.Hashtable"));
        assertFalse(cf.permitClass("java.lang.ProcessBuilder"));

        assertTrue(cf.permitConstructor(LinkedHashMap.class.getConstructor()));
        assertFalse(cf.permitConstructor(Hashtable.class.getConstructor()));

        assertTrue(cf.permitMethod(HashMap.class.getMethod("put", Object.class, Object.class)));
        assertFalse(cf.permitMethod(Hashtable.class.getMethod("put", Object.class, Object.class)));
    }

    @Test
    public void testExtraClasses() throws Exception {
        SyspropUtil.setProperty(ClassFilterBuilder.PROP_WHITELIST_CLASSES, "java.util.Hashtable;com.l7tech.util.ClassFilterBuilderTest$A");

        ClassFilter cf = new ClassFilterBuilder().
            allowDefaults().
            build();

        assertTrue(cf.permitClass("java.util.LinkedHashMap"));
        assertTrue(cf.permitClass("java.util.Hashtable"));
        assertTrue(cf.permitClass("com.l7tech.util.ClassFilterBuilderTest$A"));
        assertFalse(cf.permitClass("java.lang.ProcessBuilder"));
    }

    @Test
    public void testExtraConstructors() throws Exception {
        SyspropUtil.setProperty(ClassFilterBuilder.PROP_WHITELIST_CONSTRUCTORS, "java.util.Hashtable();com.l7tech.util.ClassFilterBuilderTest$A(java.lang.String,int,com.l7tech.util.ClassFilterBuilderTest)");

        ClassFilter cf = new ClassFilterBuilder().
            allowDefaults().
            build();

        assertTrue(cf.permitConstructor(LinkedHashMap.class.getConstructor()));
        assertTrue(cf.permitConstructor(Hashtable.class.getConstructor()));
        assertTrue(cf.permitConstructor(A.class.getConstructor(String.class, int.class, ClassFilterBuilderTest.class)));
        assertFalse(cf.permitConstructor(ProcessBuilder.class.getConstructor(String[].class)));
    }

    @Test
    public void testExtraMethods() throws Exception {
        SyspropUtil.setProperty(ClassFilterBuilder.PROP_WHITELIST_METHODS, "java.util.Hashtable.put(java.lang.Object,java.lang.Object);com.l7tech.util.ClassFilterBuilderTest$A.setDummy(java.lang.String,boolean)");

        ClassFilter cf = new ClassFilterBuilder().
            allowDefaults().
            build();

        assertTrue(cf.permitMethod(HashMap.class.getMethod("put", Object.class, Object.class)));
        assertTrue(cf.permitMethod(Hashtable.class.getMethod("put", Object.class, Object.class)));
        assertTrue(cf.permitMethod(A.class.getMethod("setDummy", String.class, boolean.class)));
        assertFalse(cf.permitMethod(ProcessBuilder.class.getMethod("start")));
    }

}
