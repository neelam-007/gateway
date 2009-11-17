package com.l7tech.util;

import java.io.Serializable;
import java.util.Map;

/**
 * A utility class that holds a two-item tuple.
 * <p/>
 * For convenience, this also provides a read-only implementation of {@link Map.Entry} with no
 * backing Map; the left object corresponds to the Key and the right object corresponds to the Value.
 * The {@link #setValue} method will thorw {@link UnsupportedOperationException}.
 */
public class Pair<L,R> implements Serializable, Map.Entry<L, R> {
    public final L left;
    public final R right;

    public Pair(L left, R right) {
        this.left = left;
        this.right = right;
    }

    @Override
    public L getKey() {
        return left;
    }

    @Override
    public R getValue() {
        return right;
    }

    @Override
    public R setValue(R value) {
        throw new UnsupportedOperationException();
    }

    @Override
    @SuppressWarnings({"RedundantIfStatement"})
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Pair pair = (Pair) o;

        if (left != null ? !left.equals(pair.left) : pair.left != null) return false;
        if (right != null ? !right.equals(pair.right) : pair.right != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result;
        result = (left != null ? left.hashCode() : 0);
        result = 31 * result + (right != null ? right.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return String.format("(%s,%s)", left, right);
    }
}
