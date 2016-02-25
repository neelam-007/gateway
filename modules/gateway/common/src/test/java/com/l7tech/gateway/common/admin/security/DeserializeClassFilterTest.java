package com.l7tech.gateway.common.admin.security;

import com.l7tech.test.util.TestUtils;
import com.l7tech.util.ClassFilterObjectInputStream;
import com.l7tech.util.ClassNotPermittedException;
import com.l7tech.util.DeserializeSafe;
import com.l7tech.util.SyspropUtil;
import org.hamcrest.BaseMatcher;
import org.hamcrest.Description;
import org.hamcrest.Matchers;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.io.*;
import java.util.Arrays;

@SuppressWarnings("serial")
public class DeserializeClassFilterTest {

    /** system prop {@link DeserializeClassFilter#PROP_WHITELIST_CLASSES} for dynamically whitelist classes */
    private static final String PROP_WHITELIST_CLASSES = TestUtils.getFieldValue(DeserializeClassFilter.class, "PROP_WHITELIST_CLASSES", String.class);

    @Before
    public void setUp() throws Exception {
        SyspropUtil.clearProperties(PROP_WHITELIST_CLASSES);
    }

    @After
    public void tearDown() throws Exception {
    }

    @Test
    public void testPrimitives() throws Exception {
        // primitives
        ObjectInputStream ois = createObjectInputStream(true);
        Assert.assertThat(ois.readObject(), Matchers.allOf(Matchers.notNullValue(), Matchers.instanceOf(Boolean.class), Matchers.equalTo((Object)true)));

        ois = createObjectInputStream((byte)1);
        Assert.assertThat(ois.readObject(), Matchers.allOf(Matchers.notNullValue(), Matchers.instanceOf(Byte.class), Matchers.equalTo((Object) (byte) 1)));

        ois = createObjectInputStream('c');
        Assert.assertThat(ois.readObject(), Matchers.allOf(Matchers.notNullValue(), Matchers.instanceOf(Character.class), Matchers.equalTo((Object)'c')));

        ois = createObjectInputStream((short) 2);
        Assert.assertThat(ois.readObject(), Matchers.allOf(Matchers.notNullValue(), Matchers.instanceOf(Short.class), Matchers.equalTo((Object)(short)2)));

        ois = createObjectInputStream(3);
        Assert.assertThat(ois.readObject(), Matchers.allOf(Matchers.notNullValue(), Matchers.instanceOf(Integer.class), Matchers.equalTo((Object)3)));

        ois = createObjectInputStream(4L);
        Assert.assertThat(ois.readObject(), Matchers.allOf(Matchers.notNullValue(), Matchers.instanceOf(Long.class), Matchers.equalTo((Object)4L)));

        ois = createObjectInputStream(5D);
        Assert.assertThat(ois.readObject(), Matchers.allOf(Matchers.notNullValue(), Matchers.instanceOf(Double.class), Matchers.equalTo((Object)5D)));

        ois = createObjectInputStream(6F);
        Assert.assertThat(ois.readObject(), Matchers.allOf(Matchers.notNullValue(), Matchers.instanceOf(Float.class), Matchers.equalTo((Object) 6F)));
    }

    /**
     * Custom Matcher to match an array of objects bypassing {@code Matcher} type-safety
     */
    private static final class ArrayMatcher<T> extends BaseMatcher {
        private final T[] expected;

        private ArrayMatcher(final T[] expected) {
            Assert.assertNotNull(expected);
            this.expected = expected;
        }

        @Override
        public void describeTo(final Description description) {
            description.appendValue(expected);
        }

        @Override
        public boolean matches(final Object obj) {
            Assert.assertNotNull(obj);
            //noinspection unchecked
            return Arrays.deepEquals((T[]) obj, expected);
        }

        public static <T> ArrayMatcher<T> arrayEquals(final T[] expected) {
            return new ArrayMatcher<>(expected);
        }
    }

    private static class UnsafeClass implements Serializable {
    }

    private static final class UnsafeClassWithSafeField implements Serializable {
        @SuppressWarnings("UnusedDeclaration")
        private final SafeClass ignore = new SafeClass();
    }

    @DeserializeSafe
    private static class SafeClass implements Serializable {
    }

    @DeserializeSafe
    private static class SafeClassWithSafeField implements Serializable {
        @SuppressWarnings("UnusedDeclaration")
        private final SafeClass ignore = new SafeClass();
    }

    @DeserializeSafe
    private static class SafeClassWithUnsafeField implements Serializable {
        @SuppressWarnings("UnusedDeclaration")
        private final UnsafeClass ignore = new UnsafeClass();
    }

    @DeserializeSafe
    public static class SafeClassInheritUnsafeClass extends UnsafeClass {
    }

    @DeserializeSafe
    public static class SafeClassWithSafeFieldInheritUnsafeClass implements Serializable {
        @SuppressWarnings("UnusedDeclaration")
        private final SafeClassInheritUnsafeClass ignore = new SafeClassInheritUnsafeClass();
    }

    @Test
    public void testArraysAndCollections() throws Exception {
        // test primitive arrays
        ObjectInputStream ois = createObjectInputStream(new boolean[] {true, false, true});
        Object obj = ois.readObject();
        Assert.assertThat(obj, Matchers.allOf(Matchers.notNullValue(), Matchers.instanceOf(boolean[].class)));
        Assert.assertTrue(Arrays.equals((boolean[])obj, new boolean[] {true, false, true}));

        ois = createObjectInputStream(new boolean[][][] {{{true, false}, {true, true, true, false}}, {{false}}, {{false, false}, {true, false}, {true, false, true}}});
        obj = ois.readObject();
        Assert.assertThat(obj, Matchers.allOf(Matchers.notNullValue(), Matchers.instanceOf(boolean[][][].class)));
        Assert.assertTrue(Arrays.deepEquals((boolean[][][])obj, new boolean[][][] { {{true, false}, {true, true, true, false}}, {{false}}, {{false, false}, {true, false}, {true, false, true}}}));

        ois = createObjectInputStream(new byte[] {1, 2, 3, 4});
        obj = ois.readObject();
        Assert.assertThat(obj, Matchers.allOf(Matchers.notNullValue(), Matchers.instanceOf(byte[].class)));
        Assert.assertTrue(Arrays.equals((byte[])obj, new byte[] {1, 2, 3, 4}));

        ois = createObjectInputStream(new byte[][][] {{{}, {2}, {3, 3}}, {{1, 2, 3, 4}, {3, 0}, {}, {5, 6, 7, 8, 9}}, {{3, 3}}, {{4, 4}, {5, 5, 5, 5, 5}, {1}}});
        obj = ois.readObject();
        Assert.assertThat(obj, Matchers.allOf(Matchers.notNullValue(), Matchers.instanceOf(byte[][][].class)));
        Assert.assertTrue(Arrays.deepEquals((byte[][][]) obj, new byte[][][]{{{}, {2}, {3, 3}}, {{1, 2, 3, 4}, {3, 0}, {}, {5, 6, 7, 8, 9}}, {{3, 3}}, {{4, 4}, {5, 5, 5, 5, 5}, {1}}}));

        ois = createObjectInputStream(new char[] {'a', 'b', 'c', 'd'});
        obj = ois.readObject();
        Assert.assertThat(obj, Matchers.allOf(Matchers.notNullValue(), Matchers.instanceOf(char[].class)));
        Assert.assertTrue(Arrays.equals((char[])obj, new char[] {'a', 'b', 'c', 'd'}));

        ois = createObjectInputStream(new char[][][] {{{'a'}, {'b', 'b'}}, {{}, {'c'}, {'d'}}, {{'c', 'd', 'e'}}, {{'d'}, {'d'}, {}, {'d'}}});
        obj = ois.readObject();
        Assert.assertThat(obj, Matchers.allOf(Matchers.notNullValue(), Matchers.instanceOf(char[][][].class)));
        Assert.assertTrue(Arrays.deepEquals((char[][][])obj, new char[][][] {{{'a'}, {'b', 'b'}}, {{}, {'c'}, {'d'}}, {{'c', 'd', 'e'}}, {{'d'}, {'d'}, {}, {'d'}}}));

        ois = createObjectInputStream(new short[] {1, 2, 3, 4});
        obj = ois.readObject();
        Assert.assertThat(obj, Matchers.allOf(Matchers.notNullValue(), Matchers.instanceOf(short[].class)));
        Assert.assertTrue(Arrays.equals((short[])obj, new short[] {1, 2, 3, 4}));

        ois = createObjectInputStream(new short[][][] {{{}, {2}, {3, 3}}, {{1, 2, 3, 4}, {3, 0}, {}, {5, 6, 7, 8, 9}}, {{3, 3}}, {{4, 4}, {5, 5, 5, 5, 5}, {1}}});
        obj = ois.readObject();
        Assert.assertThat(obj, Matchers.allOf(Matchers.notNullValue(), Matchers.instanceOf(short[][][].class)));
        Assert.assertTrue(Arrays.deepEquals((short[][][]) obj, new short[][][]{{{}, {2}, {3, 3}}, {{1, 2, 3, 4}, {3, 0}, {}, {5, 6, 7, 8, 9}}, {{3, 3}}, {{4, 4}, {5, 5, 5, 5, 5}, {1}}}));

        ois = createObjectInputStream(new int[] {1, 2, 3, 4});
        obj = ois.readObject();
        Assert.assertThat(obj, Matchers.allOf(Matchers.notNullValue(), Matchers.instanceOf(int[].class)));
        Assert.assertTrue(Arrays.equals((int[])obj, new int[] {1, 2, 3, 4}));

        ois = createObjectInputStream(new int[][][] {{{}, {2}, {3, 3}}, {{1, 2, 3, 4}, {3, 0}, {}, {5, 6, 7, 8, 9}}, {{3, 3}}, {{4, 4}, {5, 5, 5, 5, 5}, {1}}});
        obj = ois.readObject();
        Assert.assertThat(obj, Matchers.allOf(Matchers.notNullValue(), Matchers.instanceOf(int[][][].class)));
        Assert.assertTrue(Arrays.deepEquals((int[][][]) obj, new int[][][]{{{}, {2}, {3, 3}}, {{1, 2, 3, 4}, {3, 0}, {}, {5, 6, 7, 8, 9}}, {{3, 3}}, {{4, 4}, {5, 5, 5, 5, 5}, {1}}}));

        ois = createObjectInputStream(new long[] {1, 2, 3, 4});
        obj = ois.readObject();
        Assert.assertThat(obj, Matchers.allOf(Matchers.notNullValue(), Matchers.instanceOf(long[].class)));
        Assert.assertTrue(Arrays.equals((long[])obj, new long[] {1, 2, 3, 4}));

        ois = createObjectInputStream(new long [][][] {{{}, {2}, {3, 3}}, {{1, 2, 3, 4}, {3, 0}, {}, {5, 6, 7, 8, 9}}, {{3, 3}}, {{4, 4}, {5, 5, 5, 5, 5}, {1}}});
        obj = ois.readObject();
        Assert.assertThat(obj, Matchers.allOf(Matchers.notNullValue(), Matchers.instanceOf(long[][][].class)));
        Assert.assertTrue(Arrays.deepEquals((long[][][]) obj, new long[][][]{{{}, {2}, {3, 3}}, {{1, 2, 3, 4}, {3, 0}, {}, {5, 6, 7, 8, 9}}, {{3, 3}}, {{4, 4}, {5, 5, 5, 5, 5}, {1}}}));

        ois = createObjectInputStream(new double[] {1, 2, 3, 4});
        obj = ois.readObject();
        Assert.assertThat(obj, Matchers.allOf(Matchers.notNullValue(), Matchers.instanceOf(double[].class)));
        Assert.assertTrue(Arrays.equals((double[])obj, new double[] {1, 2, 3, 4}));

        ois = createObjectInputStream(new double [][][] {{{}, {2}, {3, 3}}, {{1, 2, 3, 4}, {3, 0}, {}, {5, 6, 7, 8, 9}}, {{3, 3}}, {{4, 4}, {5, 5, 5, 5, 5}, {1}}});
        obj = ois.readObject();
        Assert.assertThat(obj, Matchers.allOf(Matchers.notNullValue(), Matchers.instanceOf(double[][][].class)));
        Assert.assertTrue(Arrays.deepEquals((double[][][]) obj, new double[][][]{{{}, {2}, {3, 3}}, {{1, 2, 3, 4}, {3, 0}, {}, {5, 6, 7, 8, 9}}, {{3, 3}}, {{4, 4}, {5, 5, 5, 5, 5}, {1}}}));

        ois = createObjectInputStream(new float[] {1, 2, 3, 4});
        obj = ois.readObject();
        Assert.assertThat(obj, Matchers.allOf(Matchers.notNullValue(), Matchers.instanceOf(float[].class)));
        Assert.assertTrue(Arrays.equals((float[])obj, new float[] {1, 2, 3, 4}));

        ois = createObjectInputStream(new float [][][] {{{}, {2}, {3, 3}}, {{1, 2, 3, 4}, {3, 0}, {}, {5, 6, 7, 8, 9}}, {{3, 3}}, {{4, 4}, {5, 5, 5, 5, 5}, {1}}});
        obj = ois.readObject();
        Assert.assertThat(obj, Matchers.allOf(Matchers.notNullValue(), Matchers.instanceOf(float[][][].class)));
        Assert.assertTrue(Arrays.deepEquals((float[][][]) obj, new float[][][]{{{}, {2}, {3, 3}}, {{1, 2, 3, 4}, {3, 0}, {}, {5, 6, 7, 8, 9}}, {{3, 3}}, {{4, 4}, {5, 5, 5, 5, 5}, {1}}}));

        // object  arrays
        ois = createObjectInputStream(new Boolean[] {true, false, true});
        Assert.assertThat(ois.readObject(), Matchers.allOf(Matchers.notNullValue(), Matchers.instanceOf(Boolean[].class), ArrayMatcher.arrayEquals(new Boolean[]{true, false, true})));

        ois = createObjectInputStream(new Boolean[][][] {{{true, false}, {true, true, true, false}}, {{false}}, {{false, false}, {true, false}, {true, false, true}}});
        Assert.assertThat(ois.readObject(), Matchers.allOf(Matchers.notNullValue(), Matchers.instanceOf(Boolean[][][].class), ArrayMatcher.arrayEquals(new Boolean[][][] {{{true, false}, {true, true, true, false}}, {{false}}, {{false, false}, {true, false}, {true, false, true}}})));

        ois = createObjectInputStream(new Byte[]{1, 2, 3, 4});
        Assert.assertThat(ois.readObject(), Matchers.allOf(Matchers.notNullValue(), Matchers.instanceOf(Byte[].class), ArrayMatcher.arrayEquals(new Byte[] {1, 2, 3, 4})));

        ois = createObjectInputStream(new Byte[][][] {{{}, {2}, {3, 3}}, {{1, 2, 3, 4}, {3, 0}, {}, {5, 6, 7, 8, 9}}, {{3, 3}}, {{4, 4}, {5, 5, 5, 5, 5}, {1}}});
        Assert.assertThat(ois.readObject(), Matchers.allOf(Matchers.notNullValue(), Matchers.instanceOf(Byte[][][].class), ArrayMatcher.arrayEquals(new Byte[][][] {{{}, {2}, {3, 3}}, {{1, 2, 3, 4}, {3, 0}, {}, {5, 6, 7, 8, 9}}, {{3, 3}}, {{4, 4}, {5, 5, 5, 5, 5}, {1}}})));

        ois = createObjectInputStream(new Character[]{'a', 'b', 'c', 'd'});
        Assert.assertThat(ois.readObject(), Matchers.allOf(Matchers.notNullValue(), Matchers.instanceOf(Character[].class), ArrayMatcher.arrayEquals(new Character[]{'a', 'b', 'c', 'd'})));

        ois = createObjectInputStream(new Character[][][] {{{'a'}, {'b', 'b'}}, {{}, {'c'}, {'d'}}, {{'c', 'd', 'e'}}, {{'d'}, {'d'}, {}, {'d'}}});
        Assert.assertThat(ois.readObject(), Matchers.allOf(Matchers.notNullValue(), Matchers.instanceOf(Character[][][].class), ArrayMatcher.arrayEquals(new Character[][][] {{{'a'}, {'b', 'b'}}, {{}, {'c'}, {'d'}}, {{'c', 'd', 'e'}}, {{'d'}, {'d'}, {}, {'d'}}})));

        ois = createObjectInputStream(new Short[]{1, 2, 3, 4});
        Assert.assertThat(ois.readObject(), Matchers.allOf(Matchers.notNullValue(), Matchers.instanceOf(Short[].class), ArrayMatcher.arrayEquals(new Short[]{1, 2, 3, 4})));

        ois = createObjectInputStream(new Short[][][] {{{}, {2}, {3, 3}}, {{1, 2, 3, 4}, {3, 0}, {}, {5, 6, 7, 8, 9}}, {{3, 3}}, {{4, 4}, {5, 5, 5, 5, 5}, {1}}});
        Assert.assertThat(ois.readObject(), Matchers.allOf(Matchers.notNullValue(), Matchers.instanceOf(Short[][][].class), ArrayMatcher.arrayEquals(new Short[][][] {{{}, {2}, {3, 3}}, {{1, 2, 3, 4}, {3, 0}, {}, {5, 6, 7, 8, 9}}, {{3, 3}}, {{4, 4}, {5, 5, 5, 5, 5}, {1}}})));

        ois = createObjectInputStream(new Integer[]{1, 2, 3, 4});
        Assert.assertThat(ois.readObject(), Matchers.allOf(Matchers.notNullValue(), Matchers.instanceOf(Integer[].class), ArrayMatcher.arrayEquals(new Integer[]{1, 2, 3, 4})));

        ois = createObjectInputStream(new Integer[][][] {{{}, {2}, {3, 3}}, {{1, 2, 3, 4}, {3, 0}, {}, {5, 6, 7, 8, 9}}, {{3, 3}}, {{4, 4}, {5, 5, 5, 5, 5}, {1}}});
        Assert.assertThat(ois.readObject(), Matchers.allOf(Matchers.notNullValue(), Matchers.instanceOf(Integer[][][].class), ArrayMatcher.arrayEquals(new Integer[][][] {{{}, {2}, {3, 3}}, {{1, 2, 3, 4}, {3, 0}, {}, {5, 6, 7, 8, 9}}, {{3, 3}}, {{4, 4}, {5, 5, 5, 5, 5}, {1}}})));

        ois = createObjectInputStream(new Long[]{1L, 2L, 3L, 4L});
        Assert.assertThat(ois.readObject(), Matchers.allOf(Matchers.notNullValue(), Matchers.instanceOf(Long[].class), ArrayMatcher.arrayEquals(new Long[]{1L, 2L, 3L, 4L})));

        ois = createObjectInputStream(new Long[][][] {{{}, {2L}, {3L, 3L}}, {{1L, 2L, 3L, 4L}, {3L, 0L}, {}, {5L, 6L, 7L, 8L, 9L}}, {{3L, 3L}}, {{4L, 4L}, {5L, 5L, 5L, 5L, 5L}, {1L}}});
        Assert.assertThat(ois.readObject(), Matchers.allOf(Matchers.notNullValue(), Matchers.instanceOf(Long[][][].class), ArrayMatcher.arrayEquals(new Long[][][] {{{}, {2L}, {3L, 3L}}, {{1L, 2L, 3L, 4L}, {3L, 0L}, {}, {5L, 6L, 7L, 8L, 9L}}, {{3L, 3L}}, {{4L, 4L}, {5L, 5L, 5L, 5L, 5L}, {1L}}})));

        ois = createObjectInputStream(new Double[] {1D, 2D, 3D, 4D});
        Assert.assertThat(ois.readObject(), Matchers.allOf(Matchers.notNullValue(), Matchers.instanceOf(Double[].class), ArrayMatcher.arrayEquals(new Double[]{1D, 2D, 3D, 4D})));

        ois = createObjectInputStream(new Double[][][] {{{}, {2D}, {3D, 3D}}, {{1D, 2D, 3D, 4D}, {3D, 0D}, {}, {5D, 6D, 7D, 8D, 9D}}, {{3D, 3D}}, {{4D, 4D}, {5D, 5D, 5D, 5D, 5D}, {1D}}});
        Assert.assertThat(ois.readObject(), Matchers.allOf(Matchers.notNullValue(), Matchers.instanceOf(Double[][][].class), ArrayMatcher.arrayEquals(new Double[][][] {{{}, {2D}, {3D, 3D}}, {{1D, 2D, 3D, 4D}, {3D, 0D}, {}, {5D, 6D, 7D, 8D, 9D}}, {{3D, 3D}}, {{4D, 4D}, {5D, 5D, 5D, 5D, 5D}, {1D}}})));

        ois = createObjectInputStream(new Float[] {1F, 2F, 3F, 4F});
        Assert.assertThat(ois.readObject(), Matchers.allOf(Matchers.notNullValue(), Matchers.instanceOf(Float[].class), ArrayMatcher.arrayEquals(new Float[]{1F, 2F, 3F, 4F})));

        ois = createObjectInputStream(new Float[][][] {{{}, {2F}, {3F, 3F}}, {{1F, 2F, 3F, 4F}, {3F, 0F}, {}, {5F, 6F, 7F, 8F, 9F}}, {{3F, 3F}}, {{4F, 4F}, {5F, 5F, 5F, 5F, 5F}, {1F}}});
        Assert.assertThat(ois.readObject(), Matchers.allOf(Matchers.notNullValue(), Matchers.instanceOf(Float[][][].class), ArrayMatcher.arrayEquals(new Float[][][] {{{}, {2F}, {3F, 3F}}, {{1F, 2F, 3F, 4F}, {3F, 0F}, {}, {5F, 6F, 7F, 8F, 9F}}, {{3F, 3F}}, {{4F, 4F}, {5F, 5F, 5F, 5F, 5F}, {1F}}})));

        ois = createObjectInputStream(new SafeClass[] {new SafeClass(), new SafeClass()});
        Assert.assertThat(ois.readObject(), Matchers.allOf(Matchers.notNullValue(), Matchers.instanceOf(SafeClass[].class)));

        ois = createObjectInputStream(new SafeClass[][][] {{{new SafeClass()}}, {{new SafeClass()}}});
        Assert.assertThat(ois.readObject(), Matchers.allOf(Matchers.notNullValue(), Matchers.instanceOf(SafeClass[][][].class)));

        ois = createObjectInputStream(new SafeClassWithSafeField[] {new SafeClassWithSafeField(), new SafeClassWithSafeField()});
        Assert.assertThat(ois.readObject(), Matchers.allOf(Matchers.notNullValue(), Matchers.instanceOf(SafeClassWithSafeField[].class)));

        ois = createObjectInputStream(new SafeClassWithSafeField[][][] {{{new SafeClassWithSafeField()}}, {{new SafeClassWithSafeField()}}});
        Assert.assertThat(ois.readObject(), Matchers.allOf(Matchers.notNullValue(), Matchers.instanceOf(SafeClassWithSafeField[][][].class)));

        try {
            ois = createObjectInputStream(new SafeClassWithUnsafeField[]{new SafeClassWithUnsafeField(), new SafeClassWithUnsafeField()});
            ois.readObject();
            Assert.fail("SafeClassWithUnsafeField[] shouldn't have been deserialized!!!!!");
        } catch (final ClassNotPermittedException e) {
            Assert.assertThat(e.getMessage(), Matchers.containsString(UnsafeClass.class.getName()));
        }

        try {
            ois = createObjectInputStream(new SafeClassWithUnsafeField[][][]{{{new SafeClassWithUnsafeField()}}, {{new SafeClassWithUnsafeField()}}});
            ois.readObject();
            Assert.fail("SafeClassWithUnsafeField[][][] shouldn't have been deserialized!!!!!");
        } catch (final ClassNotPermittedException e) {
            Assert.assertThat(e.getMessage(), Matchers.containsString(UnsafeClass.class.getName()));
        }

        ois = createObjectInputStream(new String[]{"a", "b", "c", "d"});
        Assert.assertThat(ois.readObject(), Matchers.allOf(Matchers.notNullValue(), Matchers.instanceOf(String[].class), ArrayMatcher.arrayEquals(new String[]{"a", "b", "c", "d"})));

        ois = createObjectInputStream(new String[][][] {{{"a"}, {"b", "b"}}, {{}, {"c"}, {"d"}}, {{"c", "d", "e"}}, {{"d"}, {"d"}, {}, {"d"}}});
        Assert.assertThat(ois.readObject(), Matchers.allOf(Matchers.notNullValue(), Matchers.instanceOf(String[][][].class), ArrayMatcher.arrayEquals(new String[][][] {{{"a"}, {"b", "b"}}, {{}, {"c"}, {"d"}}, {{"c", "d", "e"}}, {{"d"}, {"d"}, {}, {"d"}}})));

        // array of different objects
        ois = createObjectInputStream(new Object[] {true, (byte)1, 'c', (short)2, 3, 4L, 5D, 6F, "string", new SafeClass(), new SafeClassWithSafeField()});
        Assert.assertThat(ois.readObject(), Matchers.allOf(Matchers.notNullValue(), Matchers.instanceOf(Object[].class)));

        try {
            ois = createObjectInputStream(new Object[]{true, (byte) 1, 'c', (short) 2, 3, 4L, 5D, 6F, new UnsafeClass(), "string", new SafeClass(), new SafeClassWithSafeField()});
            ois.readObject();
            Assert.fail("Array shouldn't have been deserialized, as it contains UnsafeClass!!!!!");
        } catch (final ClassNotPermittedException e) {
            Assert.assertThat(e.getMessage(), Matchers.containsString(UnsafeClass.class.getName()));
        }
    }

    @Test
    public void testAnnotations() throws Exception {
        ObjectInputStream ois = createObjectInputStream(new SafeClass());
        Assert.assertThat(ois.readObject(), Matchers.allOf(Matchers.notNullValue(), Matchers.instanceOf(SafeClass.class)));

        ois = createObjectInputStream(new SafeClassWithSafeField());
        Assert.assertThat(ois.readObject(), Matchers.allOf(Matchers.notNullValue(), Matchers.instanceOf(SafeClassWithSafeField.class)));

        try {
            ois = createObjectInputStream(new SafeClassWithUnsafeField());
            ois.readObject();
            Assert.fail("SafeClassWithUnsafeField shouldn't have been deserialized!!!!!");
        } catch (final ClassNotPermittedException e) {
            Assert.assertThat(e.getMessage(), Matchers.containsString(UnsafeClass.class.getName()));
        }

        try {
            ois = createObjectInputStream(new SafeClassInheritUnsafeClass());
            ois.readObject();
            Assert.fail("SafeClassInheritUnsafeClass shouldn't have been deserialized!!!!!");
        } catch (final ClassNotPermittedException e) {
            Assert.assertThat(e.getMessage(), Matchers.containsString(UnsafeClass.class.getName()));
        }

        try {
            ois = createObjectInputStream(new SafeClassWithSafeFieldInheritUnsafeClass());
            ois.readObject();
            Assert.fail("SafeClassWithSafeFieldInheritUnsafeClass shouldn't have been deserialized!!!!!");
        } catch (final ClassNotPermittedException e) {
            Assert.assertThat(e.getMessage(), Matchers.containsString(UnsafeClass.class.getName()));
        }

        try {
            ois = createObjectInputStream(new UnsafeClass());
            ois.readObject();
            Assert.fail("UnsafeClass shouldn't have been deserialized!!!!!");
        } catch (final ClassNotPermittedException e) {
            Assert.assertThat(e.getMessage(), Matchers.containsString(UnsafeClass.class.getName()));
        }

        try {
            ois = createObjectInputStream(new UnsafeClassWithSafeField());
            ois.readObject();
            Assert.fail("UnsafeClassWithSafeField shouldn't have been deserialized!!!!!");
        } catch (final ClassNotPermittedException e) {
            Assert.assertThat(e.getMessage(), Matchers.containsString(UnsafeClassWithSafeField.class.getName()));
        }
    }

    @Test
    public void testSystemProperty() throws Exception {
        ObjectInputStream ois;

        // first make sure SafeClassWithUnsafeField and UnsafeClass are not deserialized
        try {
            ois = createObjectInputStream(new SafeClassWithUnsafeField());
            ois.readObject();
            Assert.fail("SafeClassWithUnsafeField shouldn't have been deserialized!!!!!");
        } catch (final ClassNotPermittedException e) {
            Assert.assertThat(e.getMessage(), Matchers.containsString(UnsafeClass.class.getName()));
        }

        try {
            ois = createObjectInputStream(new UnsafeClass());
            ois.readObject();
            Assert.fail("UnsafeClass shouldn't have been deserialized!!!!!");
        } catch (final ClassNotPermittedException e) {
            Assert.assertThat(e.getMessage(), Matchers.containsString(UnsafeClass.class.getName()));
        }

        // whitelist UnsafeClass
        SyspropUtil.setProperty(PROP_WHITELIST_CLASSES, UnsafeClass.class.getName());

        ois = createObjectInputStream(new SafeClassWithUnsafeField());
        Assert.assertThat(ois.readObject(), Matchers.allOf(Matchers.notNullValue(), Matchers.instanceOf(SafeClassWithUnsafeField.class)));

        ois = createObjectInputStream(new UnsafeClass());
        Assert.assertThat(ois.readObject(), Matchers.allOf(Matchers.notNullValue(), Matchers.instanceOf(UnsafeClass.class)));
    }

    /**
     * Construct {@link ClassFilterObjectInputStream}, using {@link com.l7tech.gateway.common.admin.security.DeserializeClassFilter}
     * from the specified {@code object}.
     * <p/>
     * First the {@code object} is serialized int a {@code byte array} and afterwards {@code ClassFilterObjectInputStream}
     * is created from the {@code bytes}.
     */
    private ClassFilterObjectInputStream createObjectInputStream(final Object object) throws Exception {
        Assert.assertNotNull(object);

        // first serialize
        final ByteArrayOutputStream bos = new ByteArrayOutputStream();
        try (final ObjectOutputStream oos = new ObjectOutputStream(bos)) {
            oos.writeObject(object);
            oos.flush();
        }

        Assert.assertNotNull(bos.toByteArray());
        return new ClassFilterObjectInputStream(new ByteArrayInputStream(bos.toByteArray()), new DeserializeClassFilter());
    }
}