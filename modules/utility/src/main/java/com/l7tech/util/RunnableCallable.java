package com.l7tech.util;

import java.util.concurrent.Callable;

/**
 * Adaptor class that converts a Runnable into a Callable.
 */
public class RunnableCallable<T> implements Callable<T>, Runnable {
    private final Runnable delegate;
    private final T result;

    /**
     * Create a Callable out of the specified Runnable.
     * <p/>
     * The new Callable will always return null.
     *
     * @param delegate the Runnable to wrap.  Required.
     */
    public RunnableCallable(Runnable delegate) {
        this(delegate, null);
    }

    /**
     * Create a Callable out of the specified Runnable that will always return the specified result.
     *
     * @param delegate the Runnable to wrap.  Required.
     * @param result the result to return from {@link #call()}.  May be null.
     */
    public RunnableCallable(Runnable delegate, T result) {
        this.delegate = delegate;
        this.result = result;
    }

    public T call() {
        delegate.run();
        return result;
    }

    public void run() {
        delegate.run();
    }
}
