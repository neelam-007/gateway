package com.l7tech.util;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

import static java.lang.reflect.Array.*;

/**
 * Utilities for manipulating arrays.
 */
public class ArrayUtils {
    /** An empty Object array, to use in places where one is required and where it needn't be a unique object. */
    public static final Object[] EMPTY_ARRAY = new Object[0];

    /** An empty String array, to use in places where one is required and where it needn't be a unique object. */
    public static final String[] EMPTY_STRING_ARRAY = new String[0];

    /** @return a copy of the array, shifted left one position.  May be empty but never null. */
    public static String[] shift(String[] in) {
        if (in == null || in.length < 2) return new String[0];
        String[] ret = new String[in.length - 1];
        System.arraycopy(in, 1, ret, 0, ret.length);
        return ret;
    }

    /** @return a copy of the array, shifted left one position.  May be empty but never null. */
    public static Object[] shift(Object[] in) {
        if (in == null || in.length < 2) return new Object[0];
        Object[] ret = new Object[in.length - 1];
        System.arraycopy(in, 1, ret, 0, ret.length);
        return ret;
    }

    /** @return a copy of the array, shifted right one position, with str taking the place of the leftmost item. Never null or empty. */
    public static String[] unshift(String[] in, @Nullable String str) {
        if (in == null) return new String[] { str };
        String[] ret = new String[in.length + 1];
        System.arraycopy(in, 0, ret, 1, in.length);
        ret[0] = str;
        return ret;
    }

    /** @return a copy of the array, shifted right one position, with obj taking the place of the leftmost item. Never null or empty. */
    public static Object[] unshift(Object[] in, Object obj) {
        if (in == null) return new Object[] { obj };
        Object[] ret = new Object[in.length + 1];
        System.arraycopy(in, 0, ret, 1, in.length);
        ret[0] = obj;
        return ret;
    }

    /**
     * Convert a long array to a Long array
     *
     * @param data The array to box
     * @return The boxed array (never null)
     */
    public static Long[] box(final long[] data) {
        return (Long[]) bconvert( data, Long.class );
    }

    /**
     * Convert a long array to a Long array
     *
     * @param data The array to box
     * @return The boxed array (never null)
     */
    public static Character[] box(final char[] data) {
        return (Character[]) bconvert( data, Character.class );
    }

    /**
     * Convert a Long array to a long array
     *
     * @param data The array to unbox
     * @return The unboxed array (never null)
     */
    public static long[] unbox(final Long[] data) {
        return (long[]) bconvert( data, Long.TYPE );
    }

    /**
     * Convert a Character array to a char array
     *
     * @param data The array to unbox
     * @return The unboxed array (never null)
     */
    public static char[] unbox(final Character[] data) {
        return (char[]) bconvert( data, Character.TYPE );
    }

    /**
     * Box/Unbox conversions for arrays
     */
    private static Object bconvert( final Object data, final Class<?> resultType ) {
        Object bData;

        if ( data != null ) {
            final int length = getLength( data );
            bData = newInstance( resultType, length );
            for ( int i=0; i < length; i++ ) {
                set(bData, i, get( data, i ) );
            }
        } else {
            bData = newInstance( resultType, 0 );
        }

        return bData;
    }

    /** @return true if the target Object is contained in the list. */
    public static boolean contains(Object[] list, Object target) {
        for (Object s : list) {
            if (s == null) {
                if (target == null)
                    return true;
            } else {
                if (s.equals(target))
                    return true;
            }
        }
        return false;
    }

    /** @return true if the target value is contained in the list. */
    public static boolean contains(long[] list, long target) {
        for ( long value : list ) {
            if ( value == target) {
                return true;
            }
        }
        return false;
    }

    /** @return true if any of the target Objects are contained in the list. */
    public static boolean containsAny(Object[] list, Object[] targets) {
        if (list != null && targets != null) {
            for (Object t : targets) {
                if (contains(list, t))
                    return true;
            }
        }
        return false;
    }

    /** @return true if target string is contained in the list, disregarding case during comparisons. */
    public static boolean containsIgnoreCase(String[] list, String target) {
        for (String s : list) {
            if (s == null) {
                if (target == null)
                    return true;
            } else {
                if (s.equalsIgnoreCase(target))
                    return true;
            }
        }
        return false;
    }

    /**
     * Search the specified search array positions from start to (start + searchlen - 1), inclusive, for the
     * specified subarray (or subarray
     * prefix, if it occurrs at the end of the search range).  If the subarray occurs more than once in this range,
     * this will only find the leftmost occurrance.
     * <p>
     * This method returns -1 if the subarray was not matched at all in the search array.  If it returns a value
     * between start and (start + searchlen - 1 - subarray.length), inclusive, then the entire subarray was matched at
     * the returned index of the search array.  If it returns a value greater than (start + searchlen - 1 - subarray.length),
     * then the last (start + searchlen - 1 - retval) bytes of the search range matched the first (start + searchlen - 1 - retval)
     * prefix bytes of the subarray.
     * <p>
     * If search.length is greather than subarray.length then this will only find prefix matches.
     * <p>
     * If searchlen is zero, this method will always return -1.
     *
     * @param search     the array to search.  Must not be null
     * @param start      the start position in the search array.  must be nonnegative.
     *                   (start + searchlen - 1) must be less than search.length.
     * @param searchlen  the number of bytes to search in the search array.  must be nonnegative.
     *                   (start + searchlen - 1) must be less than search.length.
     * @param subarray   the subarray to search for.  Must be non-null and non-empty.  Note that the subarray length is allowed
     *                   to exceed the search array length -- in such cases this method will only look for the prefix match.
     * @param substart   the starting position in subarray of the subarray being searched for.  Must be nonnegative
     *                   and must be less than subarray.length.
     * @return -1 if the subarray was not matched at all; or,
     *         a number between zero and (start + searchlen - 1 - subarray.length), inclusive, if the entire
     *         subarray was matched at the returned index in the search array; or,
     *         a number greater than this if the (start + searchlen - 1 - retval) bytes at the end of the search
     *         array matched the corresponding bytes at the start of the subarray.
     * @throws IllegalArgumentException if start or searchlen is less than zero
     * @throws IllegalArgumentException if substart is less than one
     */
    public static int matchSubarrayOrPrefix(byte[] search, int start, int searchlen, byte[] subarray, int substart) {
        if (search == null || subarray == null)
            throw new IllegalArgumentException("search array and subarray must be specified");
        if (start < 0 || searchlen < 0 || substart < 0)
            throw new IllegalArgumentException("search positions and lengths must be nonnegative");
        final int end = (start + searchlen - 1);
        if (substart >= subarray.length || end >= search.length)
            throw new IllegalArgumentException("Search positions would go out of bounds");

        int foundpos = -1;
        int searchpos = start;
        int subarraypos = substart;
        while (searchpos <= end && subarraypos < subarray.length) {
            if (search[searchpos] == subarray[subarraypos]) {
                if (foundpos == -1)
                    foundpos = searchpos;
                subarraypos++;
                searchpos++;
            } else {
                if (foundpos >= 0) {
                    foundpos = -1;
                    subarraypos = substart;
                } else
                    searchpos++;
            }
        }
        return foundpos;
    }

    /**
     * Compare two byte arrays for an exact match.
     *
     * @param left      one of the arrays to compare
     * @param leftoff   the offset in left at which to start the comparison
     * @param right     the other array to compare
     * @param rightoff  the offset in right at which to start the comparison
     * @param len       the number of bytes to compare (for both arrays)
     * @return          true if the corresponding sections of both arrays are byte-for-byte identical; otherwise false
     */
    public static boolean compareArrays(byte[] left, int leftoff, byte[] right, int rightoff, int len) {
        if (leftoff < 0 || rightoff < 0 || len < 1)
            throw new IllegalArgumentException("Array offsets must be nonnegative and length must be positive");
        if (leftoff + len > left.length || rightoff + len > right.length)
            throw new IllegalArgumentException("offsets + length must remain within both arrays");
        for (int i = 0; i < len; ++i) {
            if (left[leftoff + i] != right[rightoff + i])
                return false;
        }
        return true;
    }

    /**
     * Perform a lexical compare of two byte arrays, treating each byte as signed,
     * as with {@link Comparator#compare(Object, Object)}.
     *
     * @param left  the left array.  May be null.
     * @param right the right array.  May be null.
     * @return 0 if both arrays are the same length and contain the same elements.
     *         Returns a negative integer if the left array is null but the right is not; or, if the first
     *         differing byte has a value in the left array that is less than the corresponding value
     *         in the right array; or, if the arrays are different lengths and the left array is a prefix of the right array.
     *         Returns a positive integer if the right array is null but the left is not; or, if the first
     *         differing byte has a vlue in the right array that is less than the corresponding value
     *         in the left array; or, if the arrays are different lengths and the right array is a prefix of the left array.
     */
    public static int compareArrays(byte[] left, byte[] right) {
        if (left == right) return 0;
        if (left == null) return -1;
        if (right == null) return 1;
        for (int i = 0; i < left.length; i++) {
            if (i >= right.length)
                return 1;
            int comp = left[i] - right[i];
            if (comp != 0)
                return comp;
        }
        if (right.length == left.length) return 0;
        return left.length < right.length ? -1 : 1;
    }

    /**
     * Perform a lexical compare of two byte arrays, treating each byte as unsigned,
     * as with {@link Comparator#compare(Object, Object)}.
     *
     * @param left  the left array.  May be null.
     * @param right the right array.  May be null.
     * @return 0 if both arrays are the same length and contain the same elements.
     *         Returns a negative integer if the left array is null but the right is not; or, if the first
     *         differing byte has a value in the left array that is less than the corresponding value
     *         in the right array; or, if the arrays are different lengths and the left array is a prefix of the right array.
     *         Returns a positive integer if the right array is null but the left is not; or, if the first
     *         differing byte has a vlue in the right array that is less than the corresponding value
     *         in the left array; or, if the arrays are different lengths and the right array is a prefix of the left array.
     */
    public static int compareArraysUnsigned(byte[] left, byte[] right) {
        if (left == right) return 0;
        if (left == null) return -1;
        if (right == null) return 1;
        for (int i = 0; i < left.length; i++) {
            if (i >= right.length)
                return 1;
            int comp = (((int)left[i]) & 0xFF) - (((int)right[i] & 0xFF));
            if (comp != 0)
                return comp;
        }
        if (right.length == left.length) return 0;
        return left.length < right.length ? -1 : 1;
    }

    public static byte[] copy(final byte[] data) {
        return (byte[])copyInternal(data);
    }

    public static int[] copy(final int[] data) {
        return (int[])copyInternal(data);
    }

    public static char[] copy(final char[] data) {
        return (char[])copyInternal(data);
    }

    public static boolean[] copy(final boolean[] data) {
        return (boolean[])copyInternal(data);
    }

    /**
     * Create an array that is a copy of the given array.
     *
     * @param data the array to copy
     * @return The copy or null if data is null
     */
    @SuppressWarnings({ "unchecked" })
    public static <T> T[] copy(final T[] data) {
        return (T[])copyInternal( data );
    }

    @SuppressWarnings({ "SuspiciousSystemArraycopy" })
    private static Object copyInternal( final Object data ) {
        Object copy = null;

        if (data != null) {
            final int length = getLength(data);
            copy = newInstance( data.getClass().getComponentType(), length );
            System.arraycopy(data, 0, copy, 0, length);
        }

        return copy;
    }

    /**
     * Create an array that is a copy of the given arrays.
     */
    @SuppressWarnings({ "SuspiciousSystemArraycopy" })
    private static Object copyInternal( final Object data1, final Object data2 ) {
        final int data1Length = getLength( data1 );
        final int data2Length = getLength( data2 );
        final Object copy = newInstance( data1.getClass().getComponentType(), data1Length + data2Length );
        System.arraycopy(data1, 0, copy, 0, data1Length);
        System.arraycopy(data2, 0, copy, data1Length, data2Length);
        return copy;
    }

    @SuppressWarnings({ "unchecked" })
    private static <T,T1 extends T> T[] copyInternalTyped( final T[] data1, final T1[] data2 ) {
        return (T[])copyInternal( data1, data2 );
    }

    /**
     * Convert the given array to a Map.
     *
     * <p>Each item in the given entry array must be of length 2 with the
     * key being the first item and the value the second.</p>
     *
     * @param keyValueArray the array with keys value pair sub arrays
     * @param keyFirst true if the key is the first item in each sub array
     * @return The map.
     * @throws IllegalArgumentException if any keys are duplicated.
     * @throws IllegalArgumentException if any sub-arrays have invalid dimensions.
     */
    public static Map asMap(Object[][] keyValueArray, boolean keyFirst) {
        Map resultMap = new LinkedHashMap(keyValueArray.length);

        int keyIndex = 0;
        int valueIndex = 1;

        if (!keyFirst) {
            keyIndex = 1;
            valueIndex = 0;
        }

        for (int i = 0; i < keyValueArray.length; i++) {
            Object[] objects = keyValueArray[i];
            if (objects.length != 2) {
                throw new IllegalArgumentException("Invalid key/value array at position " + i);
            }

            if (resultMap.put(objects[keyIndex], objects[valueIndex]) != null) {
                throw new IllegalArgumentException("Duplicated key at position " + i);
            }
        }

        return Collections.unmodifiableMap(resultMap);
    }

    /**
     * Merge two arrays.
     *
     * <p>The 2nd array must be the same or a sub-type of the 1st array.</p>
     *
     * @param a  an array.  May be empty but not null.
     * @param b  an array.  May be empty but not null.
     * @param <T> The array type
     * @param <T1> The array (sub)type
     * @return a new array that contains all the elements of a followed by all the elements of b.
     * @see #copy
     */
    @SuppressWarnings({ "unchecked" })
    public static <T,T1 extends T> T[] concat(T[] a, T1[] b) {
        return copyInternalTyped( a, b );
    }

    /**
     * Merge two arrays.
     *
     * @param a  a byte array.  May be empty but not null.
     * @param b  a byte array.  May be empty but not null.
     * @return a new array that contains all the elements of a followed by all the elements of b.  Never null.
     */
    public static byte[] concat(@NotNull byte[] a, @NotNull byte[] b) {
        return (byte[])copyInternal( a, b );
    }

    /** Same as {@link Arrays#fill} except it returns the array (e.g. so you can use it in a super constructor call) */
    public static char[] fill(char[] chars, char c) {
        Arrays.fill(chars, c);
        return chars;
    }

    public static char[] zero(char[] in) {
        Arrays.fill(in, '\0');
        return in;
    }

    public static byte[] zero(byte[] in) {
        Arrays.fill(in, (byte)0);
        return in;
    }

    public static long[] unbox(List<Long> oids) {
        return unbox(oids.toArray(new Long[oids.size()]));
    }

    public static char[] unboxChars(List<Character> cs) {
        return unbox(cs.toArray(new Character[cs.size()]));
    }

    public static <L,R> Iterable<Pair<L,R>> zipI( final L[] left, final R[] right  ) {
        return new Iterable<Pair<L,R>>() {
            @Override
            public Iterator<Pair<L, R>> iterator() {
                return new Iterator<Pair<L, R>>(){
                    private int index = 0;

                    @Override
                    public boolean hasNext() {
                        return (left != null && left.length > index) ||
                               (right != null && right.length > index);
                    }

                    @Override
                    public Pair<L, R> next() {
                        if ( !hasNext() ) throw new NoSuchElementException();

                        final Pair<L,R> next = new Pair<L,R>(
                            left != null && left.length > index ? left[index] : null,
                            right != null && right.length > index ? right[index] : null
                        );

                        index++;

                        return next;
                    }

                    @Override
                    public void remove() {
                        throw new UnsupportedOperationException();
                    }
                };
            }
        };
    }

    /**
     * Compute the union of two String arrays.
     * <p/>
     * Elements of the output array will be in the same order they'd be in if the arrays were simply concatenated,
     * but with all but the first duplicate of any given element omitted.
     *
     * @param left   one of the arrays to combine.  Required.
     * @param right  another of the arrays to combine.  Required.
     * @return an new array consisting of all strings that are present in either input arrays.  May be empty but never null.
     */
    public static String[] union(String[] left, String[] right) {
        Set<String> set = new LinkedHashSet<String>(Arrays.asList(left));
        set.addAll(Arrays.asList(right));
        return set.toArray(new String[set.size()]);
    }

    /**
     * Compute the intersection of two String arrays.
     * <p/>
     * Elements of the output array will be the order of the left array, but with any elements not also
     * present in the right array omitted.
     *
     * @param left   one of the arrays to combine.  This array fully determines the output order.  Required.
     * @param right  another of the arrays to combine.  Required.
     * @return a new array consiting of all strings that are present in both input arrays.  May be empty but never null.
     */
    public static String[] intersection(String[] left, String[] right) {
        Set<String> ret = new LinkedHashSet<String>(Arrays.asList(left));
        ret.retainAll(Arrays.asList(right));
        return ret.toArray(new String[ret.size()]);
    }
}
