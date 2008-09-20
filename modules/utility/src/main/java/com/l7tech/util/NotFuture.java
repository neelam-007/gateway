package com.l7tech.util;

import java.util.concurrent.*;

/**
 * A Future that is already known.
 */
public final class NotFuture<V> implements Future<V> {

    //- PUBLIC

    /**
     * Create a future with the given result.
     *
     * @param target The future payload
     */
    public NotFuture( final V target ) {
        this.target = target;
        this.exception = null;
    }

    /**
     * Create a future with the given exception.
     *
     * <p>The exception is thrown when the payload is accessed.</p>
     *
     * @param e The future exception
     */
    public NotFuture( final Exception e ) {
        this.target = null;
        this.exception = e;
    }

    public boolean cancel(boolean mayInterruptIfRunning) {
        return false;
    }
    
    public boolean isCancelled() {
        return false;
    }

    public boolean isDone() {
        return true;
    }

    public V get() throws ExecutionException {
        if ( exception != null ) throw new ExecutionException(exception);
        return target;
    }

    public V get(long timeout, java.util.concurrent.TimeUnit unit) throws ExecutionException {
        if ( exception != null ) throw new ExecutionException(exception);
        return target;
    }

    //- PRIVATE

    private final V target;    
    private final Exception exception;    
}