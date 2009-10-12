package com.l7tech.console.util;

import java.util.Map;

/**
 * @author ghuang
 */
public class MutablePair<L, R> implements Map.Entry<L, R> {

    public L left;
    public R right;

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
}
