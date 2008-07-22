package com.l7tech.util;

import java.io.Serializable;

/**
 * A utility class that holds a two-item tuple.
 */
public class Pair<L,R> implements Serializable {
    public final L left;
    public final R right;

    public Pair(L left, R right) {
        this.left = left;
        this.right = right;
    }

    /** @noinspection RedundantIfStatement*/
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Pair pair = (Pair)o;

        if (left != null ? !left.equals(pair.left) : pair.left != null) return false;
        if (right != null ? !right.equals(pair.right) : pair.right != null) return false;

        return true;
    }

    public int hashCode() {
        int result;
        result = (left != null ? left.hashCode() : 0);
        result = 31 * result + (right != null ? right.hashCode() : 0);
        return result;
    }
}
