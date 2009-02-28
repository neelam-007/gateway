/*
 * Copyright (C) 2009 Layer 7 Technologies Inc.
 */
package com.l7tech.util;

import java.util.Iterator;
import java.util.Set;

public final class CollectionUtils {
    private static final Functions.Unary<String,Object> defaultStringer = new Functions.Unary<String, Object>() {
        @Override
        public String call(Object o) {
            return o.toString();
        }
    };

    private CollectionUtils() { }

    /**
     * Makes a String representation of the provided Iterable.
     * @param iterable the iterable to traverse
     * @param prefix the prefix to prepend to the result
     * @param delimiter the delimiter to insert between elements of the Iterable
     * @param suffix the suffix to append to the result
     * @param stringer a function that renders an element of the Iterable as a String
     * @return a String representation of the provided Iterable.
     */
    public static <T> String mkString(Iterable<T> iterable, String prefix, String delimiter, String suffix, Functions.Unary<String, T> stringer) {
        StringBuilder sb = new StringBuilder(prefix == null ? "" : prefix);
        for (Iterator<T> it = iterable.iterator(); it.hasNext();) {
            T t = it.next();
            sb.append(stringer.call(t));
            if (it.hasNext()) sb.append(delimiter == null ? "" : delimiter);
        }
        sb.append(suffix == null ? "" : suffix);
        return sb.toString();
    }

    public static <T> String mkString(Iterable<T> iterable, String delimiter, Functions.Unary<String, T> stringer) {
        return mkString(iterable, null, delimiter, null, stringer);
    }

    public static <T> String mkString(Iterable<T> iterable, String delimiter) {
        return mkString(iterable, delimiter, (Functions.Unary<String,T>) defaultStringer);
    }

    public static <T> String mkString(Iterable<T> iterable, String prefix, String delimiter, String suffix) {
        return mkString(iterable, prefix, delimiter, suffix, (Functions.Unary<String,T>) defaultStringer);
    }
}
