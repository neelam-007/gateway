package com.l7tech.util;

import java.util.concurrent.Callable;

/**
 * Adaptor class to convert a Runnable into a Callable.
 */
public class CallableRunnable<T> implements Runnable, Callable<T> {
    private final Callable<T> delegate;

    /**
     * Create a Runnable out of the specified Callable.
     * <p/>
     * Any checked exception thrown by the Callable will be wrapped in a RuntimeException.
     *
     * @param delegate the Callable to wrap.
     */
    public CallableRunnable(Callable<T> delegate) {
        this.delegate = delegate;
    }

    public void run() {
        try {
            delegate.call();
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public T call() throws Exception {
        return delegate.call();
    }
}
