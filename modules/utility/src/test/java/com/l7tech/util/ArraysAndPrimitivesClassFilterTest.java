package com.l7tech.util;

import org.hamcrest.Matchers;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.runners.MockitoJUnitRunner;
import org.mockito.stubbing.Answer;

import java.io.*;
import java.lang.reflect.Array;

@RunWith(MockitoJUnitRunner.class)
public class ArraysAndPrimitivesClassFilterTest {

    private ArraysAndPrimitivesClassFilter classFilter;

    @Before
    public void setUp() throws Exception {
        classFilter = new ArraysAndPrimitivesClassFilter();
    }

    @After
    public void tearDown() throws Exception {

    }

    @Test
    public void testPermitClass() throws Exception {
        Assert.assertTrue(classFilter.permitClass("B"));
        Assert.assertTrue(classFilter.permitClass("[B"));
        Assert.assertTrue(classFilter.permitClass("[[B"));
        Assert.assertTrue(classFilter.permitClass("[[[B"));
        Assert.assertTrue(classFilter.permitClass("byte"));
        Assert.assertTrue(classFilter.permitClass("byte[]"));
        Assert.assertTrue(classFilter.permitClass("byte[][]"));
        Assert.assertTrue(classFilter.permitClass("byte[][][]"));
        Assert.assertTrue(classFilter.permitClass(byte.class.getName()));
        Assert.assertTrue(classFilter.permitClass(byte[].class.getName()));
        Assert.assertTrue(classFilter.permitClass(byte[][].class.getName()));
        Assert.assertTrue(classFilter.permitClass(byte[][][].class.getName()));
        Assert.assertTrue(classFilter.permitClass(Byte.class.getName()));
        Assert.assertTrue(classFilter.permitClass(Byte[].class.getName()));
        Assert.assertTrue(classFilter.permitClass(Byte[][].class.getName()));
        Assert.assertTrue(classFilter.permitClass(Byte[][][].class.getName()));

        Assert.assertTrue(classFilter.permitClass("Z"));
        Assert.assertTrue(classFilter.permitClass("[Z"));
        Assert.assertTrue(classFilter.permitClass("[[Z"));
        Assert.assertTrue(classFilter.permitClass("[[[Z"));
        Assert.assertTrue(classFilter.permitClass("boolean"));
        Assert.assertTrue(classFilter.permitClass("boolean[]"));
        Assert.assertTrue(classFilter.permitClass("boolean[][]"));
        Assert.assertTrue(classFilter.permitClass("boolean[][][]"));
        Assert.assertTrue(classFilter.permitClass(boolean.class.getName()));
        Assert.assertTrue(classFilter.permitClass(boolean[].class.getName()));
        Assert.assertTrue(classFilter.permitClass(boolean[][].class.getName()));
        Assert.assertTrue(classFilter.permitClass(boolean[][][].class.getName()));
        Assert.assertTrue(classFilter.permitClass(Boolean.class.getName()));
        Assert.assertTrue(classFilter.permitClass(Boolean[].class.getName()));
        Assert.assertTrue(classFilter.permitClass(Boolean[][].class.getName()));
        Assert.assertTrue(classFilter.permitClass(Boolean[][][].class.getName()));

        Assert.assertTrue(classFilter.permitClass("C"));
        Assert.assertTrue(classFilter.permitClass("[C"));
        Assert.assertTrue(classFilter.permitClass("[[C"));
        Assert.assertTrue(classFilter.permitClass("[[[C"));
        Assert.assertTrue(classFilter.permitClass("char"));
        Assert.assertTrue(classFilter.permitClass("char[]"));
        Assert.assertTrue(classFilter.permitClass("char[][]"));
        Assert.assertTrue(classFilter.permitClass("char[][][]"));
        Assert.assertTrue(classFilter.permitClass(char.class.getName()));
        Assert.assertTrue(classFilter.permitClass(char[].class.getName()));
        Assert.assertTrue(classFilter.permitClass(char[][].class.getName()));
        Assert.assertTrue(classFilter.permitClass(char[][][].class.getName()));
        Assert.assertTrue(classFilter.permitClass(Character.class.getName()));
        Assert.assertTrue(classFilter.permitClass(Character[].class.getName()));
        Assert.assertTrue(classFilter.permitClass(Character[][].class.getName()));
        Assert.assertTrue(classFilter.permitClass(Character[][][].class.getName()));

        Assert.assertTrue(classFilter.permitClass("S"));
        Assert.assertTrue(classFilter.permitClass("[S"));
        Assert.assertTrue(classFilter.permitClass("[[S"));
        Assert.assertTrue(classFilter.permitClass("[[[S"));
        Assert.assertTrue(classFilter.permitClass("short"));
        Assert.assertTrue(classFilter.permitClass("short[]"));
        Assert.assertTrue(classFilter.permitClass("short[][]"));
        Assert.assertTrue(classFilter.permitClass("short[][][]"));
        Assert.assertTrue(classFilter.permitClass(short.class.getName()));
        Assert.assertTrue(classFilter.permitClass(short[].class.getName()));
        Assert.assertTrue(classFilter.permitClass(short[][].class.getName()));
        Assert.assertTrue(classFilter.permitClass(short[][][].class.getName()));
        Assert.assertTrue(classFilter.permitClass(Short.class.getName()));
        Assert.assertTrue(classFilter.permitClass(Short[].class.getName()));
        Assert.assertTrue(classFilter.permitClass(Short[][].class.getName()));
        Assert.assertTrue(classFilter.permitClass(Short[][][].class.getName()));

        Assert.assertTrue(classFilter.permitClass("I"));
        Assert.assertTrue(classFilter.permitClass("[I"));
        Assert.assertTrue(classFilter.permitClass("[[I"));
        Assert.assertTrue(classFilter.permitClass("[[[I"));
        Assert.assertTrue(classFilter.permitClass("int"));
        Assert.assertTrue(classFilter.permitClass("int[]"));
        Assert.assertTrue(classFilter.permitClass("int[][]"));
        Assert.assertTrue(classFilter.permitClass("int[][][]"));
        Assert.assertTrue(classFilter.permitClass(int.class.getName()));
        Assert.assertTrue(classFilter.permitClass(int[].class.getName()));
        Assert.assertTrue(classFilter.permitClass(int[][].class.getName()));
        Assert.assertTrue(classFilter.permitClass(int[][][].class.getName()));
        Assert.assertTrue(classFilter.permitClass(Integer.class.getName()));
        Assert.assertTrue(classFilter.permitClass(Integer[].class.getName()));
        Assert.assertTrue(classFilter.permitClass(Integer[][].class.getName()));
        Assert.assertTrue(classFilter.permitClass(Integer[][][].class.getName()));

        Assert.assertTrue(classFilter.permitClass("J"));
        Assert.assertTrue(classFilter.permitClass("[J"));
        Assert.assertTrue(classFilter.permitClass("[[J"));
        Assert.assertTrue(classFilter.permitClass("[[[J"));
        Assert.assertTrue(classFilter.permitClass("long"));
        Assert.assertTrue(classFilter.permitClass("long[]"));
        Assert.assertTrue(classFilter.permitClass("long[][]"));
        Assert.assertTrue(classFilter.permitClass("long[][][]"));
        Assert.assertTrue(classFilter.permitClass(long.class.getName()));
        Assert.assertTrue(classFilter.permitClass(long[].class.getName()));
        Assert.assertTrue(classFilter.permitClass(long[][].class.getName()));
        Assert.assertTrue(classFilter.permitClass(long[][][].class.getName()));
        Assert.assertTrue(classFilter.permitClass(Long.class.getName()));
        Assert.assertTrue(classFilter.permitClass(Long[].class.getName()));
        Assert.assertTrue(classFilter.permitClass(Long[][].class.getName()));
        Assert.assertTrue(classFilter.permitClass(Long[][][].class.getName()));

        Assert.assertTrue(classFilter.permitClass("D"));
        Assert.assertTrue(classFilter.permitClass("[D"));
        Assert.assertTrue(classFilter.permitClass("[[D"));
        Assert.assertTrue(classFilter.permitClass("[[[D"));
        Assert.assertTrue(classFilter.permitClass("double"));
        Assert.assertTrue(classFilter.permitClass("double[]"));
        Assert.assertTrue(classFilter.permitClass("double[][]"));
        Assert.assertTrue(classFilter.permitClass("double[][][]"));
        Assert.assertTrue(classFilter.permitClass(double.class.getName()));
        Assert.assertTrue(classFilter.permitClass(double[].class.getName()));
        Assert.assertTrue(classFilter.permitClass(double[][].class.getName()));
        Assert.assertTrue(classFilter.permitClass(double[][][].class.getName()));
        Assert.assertTrue(classFilter.permitClass(Double.class.getName()));
        Assert.assertTrue(classFilter.permitClass(Double[].class.getName()));
        Assert.assertTrue(classFilter.permitClass(Double[][].class.getName()));
        Assert.assertTrue(classFilter.permitClass(Double[][][].class.getName()));

        Assert.assertTrue(classFilter.permitClass("F"));
        Assert.assertTrue(classFilter.permitClass("[F"));
        Assert.assertTrue(classFilter.permitClass("[[F"));
        Assert.assertTrue(classFilter.permitClass("[[[F"));
        Assert.assertTrue(classFilter.permitClass("float"));
        Assert.assertTrue(classFilter.permitClass("float[]"));
        Assert.assertTrue(classFilter.permitClass("float[][]"));
        Assert.assertTrue(classFilter.permitClass("float[][][]"));
        Assert.assertTrue(classFilter.permitClass(float.class.getName()));
        Assert.assertTrue(classFilter.permitClass(float[].class.getName()));
        Assert.assertTrue(classFilter.permitClass(float[][].class.getName()));
        Assert.assertTrue(classFilter.permitClass(float[][][].class.getName()));
        Assert.assertTrue(classFilter.permitClass(Float.class.getName()));
        Assert.assertTrue(classFilter.permitClass(Float[].class.getName()));
        Assert.assertTrue(classFilter.permitClass(Float[][].class.getName()));
        Assert.assertTrue(classFilter.permitClass(Float[][][].class.getName()));

        /// false ones
        Assert.assertFalse(classFilter.permitClass(""));
        Assert.assertFalse(classFilter.permitClass(" "));
        Assert.assertFalse(classFilter.permitClass("  "));

        Assert.assertFalse(classFilter.permitClass(String.class.getName()));
        Assert.assertTrue(classFilter.permitClass(String[].class.getName())); // arrays are allowed
        Assert.assertTrue(classFilter.permitClass(String[].class.getName())); // arrays are allowed

        Assert.assertFalse(classFilter.permitClass(TestInner.class.getName()));
        Assert.assertTrue(classFilter.permitClass(TestInner[][].class.getName())); // arrays are allowed
        Assert.assertTrue(classFilter.permitClass(Array.newInstance(TestInner.class, 10).getClass().getName())); // arrays are allowed
        Assert.assertTrue(classFilter.permitClass(Array.newInstance(TestInner.class, 10, 20, 44).getClass().getName())); // arrays are allowed
    }

    @Test
    public void testPermitConstructor() throws Exception {

    }

    @Test
    public void testPermitMethod() throws Exception {

    }

    static class TestClass implements Serializable {
        final String aString;
        final Number aNumber;
        final int anInt;
        final long aLong;
        final int[] intArr;
        final long[] longArr;
        final String[] stringArr;
        final Number[] numberArr;

        public TestClass(
                final String aString,
                final Number aNumber,
                final int anInt,
                final long aLong,
                final int[] intArr,
                final long[] longArr,
                final String[] stringArr,
                final Number[] numberArr
        ) {
            this.aString = aString;
            this.aNumber = aNumber;
            this.anInt = anInt;
            this.aLong = aLong;
            this.intArr = intArr;
            this.longArr = longArr;
            this.stringArr = stringArr;
            this.numberArr = numberArr;
        }
    }

    @Test
    public void testArraySerialization() throws Exception {
        final TestClass[] testClasses = new TestClass[] {
                new TestClass("str1", 1,   10, 100, new int[] {1, 2, 3},                 new long[] {9L, 8L},           new String[] {"1", "2", "3"}, new Number[] {1, 2L, 3.0}),
                new TestClass("str2", 2L,  20, 200, new int[] {11, 12, 13, 14},          new long[] {99L, 98L, 97L},    new String[] {"5", "6"},      new Number[] {10, 20L, 30.0}),
                new TestClass("str3", 3.0, 30, 300, new int[] {101, 102, 103, 104, 105}, new long[] {909L, 908L, 907L}, new String[] {"9", "8", "7"}, new Number[] {100, 200L, 300.0})
        };

        final ByteArrayOutputStream bos = new ByteArrayOutputStream();
        try (final ObjectOutputStream oos = new ObjectOutputStream(bos)) {
            oos.writeObject(testClasses);
            oos.flush();
        }
        final byte[] bytes = bos.toByteArray();

        final ClassFilter theClassFilter = new ClassFilterBuilder().addClassFilter(classFilter).addClasses(false, String.class.getName(), Number.class.getName(), TestClass.class.getName()).build();
        Assert.assertTrue(theClassFilter.permitClass(String.class.getName()));
        Assert.assertTrue(theClassFilter.permitClass(Number.class.getName()));
        Assert.assertTrue(theClassFilter.permitClass(int.class.getName()));
        Assert.assertTrue(theClassFilter.permitClass(long.class.getName()));
        Assert.assertTrue(theClassFilter.permitClass(int[].class.getName()));
        Assert.assertTrue(theClassFilter.permitClass(long[].class.getName()));
        Assert.assertTrue(theClassFilter.permitClass(String[].class.getName()));
        Assert.assertTrue(theClassFilter.permitClass(Number[].class.getName()));
        Assert.assertTrue(theClassFilter.permitClass(TestClass.class.getName()));
        Assert.assertTrue(theClassFilter.permitClass(TestClass[].class.getName()));

        final boolean[] flags = {false, false};
        final ClassFilterObjectInputStream cfois = Mockito.spy(new ClassFilterObjectInputStream(new ByteArrayInputStream(bytes), theClassFilter));
        Mockito.doAnswer(new Answer() {
            @Override
            public Object answer(final InvocationOnMock invocation) throws Throwable {
                Assert.assertNotNull(invocation);
                Assert.assertNotNull(invocation.getArguments());
                Assert.assertThat(invocation.getArguments().length, Matchers.is(1));
                Assert.assertNotNull(invocation.getArguments()[0]);
                Assert.assertThat(invocation.getArguments()[0], Matchers.instanceOf(ObjectStreamClass.class));
                final String clsName = ((ObjectStreamClass)invocation.getArguments()[0]).getName();
                if (TestClass[].class.getName().equals(clsName)) {
                    flags[0] = true;
                } else if (TestClass.class.getName().equals(clsName)) {
                    flags[1] = true;
                }
                return invocation.callRealMethod();
            }
        }).when(cfois).resolveClass(Mockito.any(ObjectStreamClass.class));

        Assert.assertThat(flags[0], Matchers.is(false));
        Assert.assertThat(flags[1], Matchers.is(false));

        try (final ObjectInputStream ois = cfois) {
            final Object obj = ois.readObject();
            Assert.assertNotNull(obj);
            Assert.assertThat(obj, Matchers.instanceOf(TestClass[].class));
        }

        Assert.assertThat(flags[0], Matchers.is(true));
        Assert.assertThat(flags[1], Matchers.is(true));
    }

    @Test
    public void testUnwrapClassName() throws Exception {
        Assert.assertEquals("", classFilter.unwrapClassName(""));
        Assert.assertEquals(" ", classFilter.unwrapClassName(" "));
        Assert.assertEquals("  ", classFilter.unwrapClassName("  "));

        Assert.assertEquals("byte", classFilter.unwrapClassName("B"));
        Assert.assertEquals("byte", classFilter.unwrapClassName("[B"));
        Assert.assertEquals("byte", classFilter.unwrapClassName("[[B"));
        Assert.assertEquals("byte", classFilter.unwrapClassName("byte"));
        Assert.assertEquals("byte", classFilter.unwrapClassName("byte[]"));
        Assert.assertEquals("byte", classFilter.unwrapClassName("byte[][]"));
        Assert.assertEquals("java.lang.Boolean", classFilter.unwrapClassName("java.lang.Boolean"));
        Assert.assertEquals("java.lang.Boolean", classFilter.unwrapClassName("java.lang.Boolean[]"));
        Assert.assertEquals("java.lang.Boolean", classFilter.unwrapClassName("java.lang.Boolean[][]"));

        Assert.assertEquals("byte", classFilter.unwrapClassName(byte.class.getName()));
        Assert.assertEquals("byte", classFilter.unwrapClassName(byte[].class.getName()));
        Assert.assertEquals("byte", classFilter.unwrapClassName(byte[][].class.getName()));
        Assert.assertEquals("java.lang.Byte", classFilter.unwrapClassName(Byte.class.getName()));
        Assert.assertEquals("java.lang.Byte", classFilter.unwrapClassName(Byte[].class.getName()));
        Assert.assertEquals("java.lang.Byte", classFilter.unwrapClassName(Byte[][].class.getName()));


        Assert.assertEquals("boolean", classFilter.unwrapClassName(boolean.class.getName()));
        Assert.assertEquals("boolean", classFilter.unwrapClassName(boolean[].class.getName()));
        Assert.assertEquals("boolean", classFilter.unwrapClassName(boolean[][].class.getName()));
        Assert.assertEquals("java.lang.Boolean", classFilter.unwrapClassName(Boolean.class.getName()));
        Assert.assertEquals("java.lang.Boolean", classFilter.unwrapClassName(Boolean[].class.getName()));
        Assert.assertEquals("java.lang.Boolean", classFilter.unwrapClassName(Boolean[][].class.getName()));


        Assert.assertEquals("char", classFilter.unwrapClassName(char.class.getName()));
        Assert.assertEquals("char", classFilter.unwrapClassName(char[].class.getName()));
        Assert.assertEquals("char", classFilter.unwrapClassName(char[][].class.getName()));
        Assert.assertEquals("java.lang.Character", classFilter.unwrapClassName(Character.class.getName()));
        Assert.assertEquals("java.lang.Character", classFilter.unwrapClassName(Character[].class.getName()));
        Assert.assertEquals("java.lang.Character", classFilter.unwrapClassName(Character[][].class.getName()));


        Assert.assertEquals("short", classFilter.unwrapClassName(short.class.getName()));
        Assert.assertEquals("short", classFilter.unwrapClassName(short[].class.getName()));
        Assert.assertEquals("short", classFilter.unwrapClassName(short[][].class.getName()));
        Assert.assertEquals("java.lang.Short", classFilter.unwrapClassName(Short.class.getName()));
        Assert.assertEquals("java.lang.Short", classFilter.unwrapClassName(Short[].class.getName()));
        Assert.assertEquals("java.lang.Short", classFilter.unwrapClassName(Short[][].class.getName()));


        Assert.assertEquals("int", classFilter.unwrapClassName(int.class.getName()));
        Assert.assertEquals("int", classFilter.unwrapClassName(int[].class.getName()));
        Assert.assertEquals("int", classFilter.unwrapClassName(int[][].class.getName()));
        Assert.assertEquals("java.lang.Integer", classFilter.unwrapClassName(Integer.class.getName()));
        Assert.assertEquals("java.lang.Integer", classFilter.unwrapClassName(Integer[].class.getName()));
        Assert.assertEquals("java.lang.Integer", classFilter.unwrapClassName(Integer[][].class.getName()));


        Assert.assertEquals("long", classFilter.unwrapClassName(long.class.getName()));
        Assert.assertEquals("long", classFilter.unwrapClassName(long[].class.getName()));
        Assert.assertEquals("long", classFilter.unwrapClassName(long[][].class.getName()));
        Assert.assertEquals("java.lang.Long", classFilter.unwrapClassName(Long.class.getName()));
        Assert.assertEquals("java.lang.Long", classFilter.unwrapClassName(Long[].class.getName()));
        Assert.assertEquals("java.lang.Long", classFilter.unwrapClassName(Long[][].class.getName()));


        Assert.assertEquals("double", classFilter.unwrapClassName(double.class.getName()));
        Assert.assertEquals("double", classFilter.unwrapClassName(double[].class.getName()));
        Assert.assertEquals("double", classFilter.unwrapClassName(double[][].class.getName()));
        Assert.assertEquals("java.lang.Double", classFilter.unwrapClassName(Double.class.getName()));
        Assert.assertEquals("java.lang.Double", classFilter.unwrapClassName(Double[].class.getName()));
        Assert.assertEquals("java.lang.Double", classFilter.unwrapClassName(Double[][].class.getName()));


        Assert.assertEquals("float", classFilter.unwrapClassName(float.class.getName()));
        Assert.assertEquals("float", classFilter.unwrapClassName(float[].class.getName()));
        Assert.assertEquals("float", classFilter.unwrapClassName(float[][].class.getName()));
        Assert.assertEquals("java.lang.Float", classFilter.unwrapClassName(Float.class.getName()));
        Assert.assertEquals("java.lang.Float", classFilter.unwrapClassName(Float[].class.getName()));
        Assert.assertEquals("java.lang.Float", classFilter.unwrapClassName(Float[][].class.getName()));


        Assert.assertEquals("void", classFilter.unwrapClassName(void.class.getName()));
        Assert.assertEquals("java.lang.Void", classFilter.unwrapClassName(Void.class.getName()));
        Assert.assertEquals("java.lang.Void", classFilter.unwrapClassName(Void[].class.getName()));
        Assert.assertEquals("java.lang.Void", classFilter.unwrapClassName(Void[][].class.getName()));


        Assert.assertEquals("com.l7tech.util.ArraysAndPrimitivesClassFilterTest$TestInner", classFilter.unwrapClassName(TestInner.class.getName()));
        Assert.assertEquals("com.l7tech.util.ArraysAndPrimitivesClassFilterTest$TestInner", classFilter.unwrapClassName(TestInner[][].class.getName()));
        Assert.assertEquals("com.l7tech.util.ArraysAndPrimitivesClassFilterTest$TestInner", classFilter.unwrapClassName(Array.newInstance(TestInner.class, 10).getClass().getName()));
        Assert.assertEquals("com.l7tech.util.ArraysAndPrimitivesClassFilterTest$TestInner", classFilter.unwrapClassName(Array.newInstance(TestInner.class, 10, 20, 44).getClass().getName()));
    }

    static class TestInner {
        interface Blah {
            Object h();
        }

        public Blah b() {
            return null;
        }
    }
}