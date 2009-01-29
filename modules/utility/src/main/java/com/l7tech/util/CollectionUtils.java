/**
 * Copyright (C) 2009 Layer 7 Technologies Inc.
 */
package com.l7tech.util;

import java.util.Set;
import java.util.HashSet;

public final class CollectionUtils {
    private CollectionUtils() { }

    public static <E> Set<E> union(Set<E> left, Set<E> right) {
        final Set<E> newSet = new HashSet<E>();
        newSet.addAll(left);
        newSet.addAll(right);
        return newSet;
    }

    public static <E> Set<E> intersection(Set<E> left, Set<E> right) {
        final Set<E> newSet = new HashSet<E>();
        newSet.addAll(left);
        newSet.retainAll(right);
        return newSet;
    }

    public static <E> Set<E> difference(Set<E> left, Set<E> right) {
        final Set<E> newSet = new HashSet<E>();
        newSet.addAll(left);
        newSet.removeAll(right);
        return newSet;
    }
}
