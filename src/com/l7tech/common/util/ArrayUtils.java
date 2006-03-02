/*
 * Copyright (C) 2005 Layer 7 Technologies Inc.
 *
 */

package com.l7tech.common.util;

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
}
