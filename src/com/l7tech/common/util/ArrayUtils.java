/*
 * Copyright (C) 2005 Layer 7 Technologies Inc.
 *
 */

package com.l7tech.common.util;

import java.util.Map;
import java.util.LinkedHashMap;
import java.util.Collections;
import java.lang.reflect.Array;

/**
 * Utilities for manipulating arrays.
 */
public class ArrayUtils {
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
    public static String[] unshift(String[] in, String str) {
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

    /** @return true if the target string is contained in the list. */
    public static boolean contains(String[] list, String target) {
        for (int i = 0; i < list.length; i++) {
            String s = list[i];
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

    /** @return true if target string is contained in the list, disregarding case during comparisons. */
    public static boolean containsIgnoreCase(String[] list, String target) {
        for (int i = 0; i < list.length; i++) {
            String s = list[i];
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
     * Create an array that is a copy of the given array.
     *
     * @param data the array to copy
     * @return The copy or null if data is null
     */
    public static Object[] copy(final Object[] data) {
        Object[] copy = null;

        if (data != null) {
            copy = (Object[]) Array.newInstance(data.getClass().getComponentType(), data.length);
            System.arraycopy(data, 0, copy, 0, data.length);
        }

        return copy;
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
}
