package com.l7tech.util;

import org.jetbrains.annotations.NotNull;

import java.io.Serializable;

/**
 * A utility class that can hold a three-item tuple.
 */
public class Triple<L,M,R> implements Serializable {
    public final L left;
    public final M middle;
    public final R right;

    public Triple(L left, M middle, R right) {
        this.left = left;
        this.middle = middle;
        this.right = right;
    }

    /**
     * Construct a triple with the given values.
     *
     * @param left The left value
     * @param middle The middle value
     * @param right The right value
     * @param <L> The left type
     * @param <M> The middle type
     * @param <R> The right type
     * @return The triple
     */
    @NotNull
    public static <L,M,R> Triple<L,M,R> triple( final L left, final M middle, final R right) {
        return new Triple<L,M,R>( left, middle, right );
    }

    @SuppressWarnings({ "RedundantIfStatement" })
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Triple triple = (Triple)o;

        if (left != null ? !left.equals(triple.left) : triple.left != null) return false;
        if (middle != null ? !middle.equals(triple.middle) : triple.middle != null) return false;
        if (right != null ? !right.equals(triple.right) : triple.right != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result;
        result = (left != null ? left.hashCode() : 0);
        result = 31 * result + (middle != null ? middle.hashCode() : 0);
        result = 31 * result + (right != null ? right.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return String.format("(%s,%s,%s)", left, middle, right);
    }
}
