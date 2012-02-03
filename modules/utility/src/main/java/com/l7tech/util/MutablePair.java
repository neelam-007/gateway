package com.l7tech.util;

import static com.l7tech.util.Pair.pair;

import java.io.Serializable;
import java.util.Map;

/**
 * @author ghuang
 */
public class MutablePair<L, R> implements Map.Entry<L, R>, Serializable {

    public L left;
    public R right;

    public MutablePair() {
    }

    public MutablePair(L left, R right) {
        this.left = left;
        this.right = right;
    }

    @Override
    public L getKey() {
        return left;
    }

    public L setKey(L key) {
        left = key;
        return left;
    }

    @Override
    public R getValue() {
        return right;
    }

    @Override
    public R setValue(R value) {
        right = value;
        return right;
    }

    public Pair<L,R> asPair() {
        return pair( left, right );
    }
}
