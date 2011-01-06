package com.l7tech.util;

import java.util.concurrent.Callable;

/**
 * A Callable that always returns a preset value when invoked.
 */
public class FixedCallable<T> implements Callable<T> {
    private final T value;

    public FixedCallable(T value) {
        this.value = value;
    }

    @Override
    public T call() throws Exception {
        return value;
    }
}
