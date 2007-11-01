package com.l7tech.common.util;

/**
 * A utility class that holds a two-item tuple.
 */
public class Pair<L,R> {
    public final L left;
    public final R right;

    public Pair(L left, R right) {
        this.left = left;
        this.right = right;
    }
}
