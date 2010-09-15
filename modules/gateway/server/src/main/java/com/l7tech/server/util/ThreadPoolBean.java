/**
 * Copyright (C) 2008, Layer 7 Technologies Inc.
 * @author darmstrong
 */
package com.l7tech.server.util;

import com.l7tech.util.ThreadPool;

/**
 * Wraps a ThreadPool. Classes which extend this can modify the thread pool in response
 * to environmental changes e.g. cluster properties changing.
 *
 * @param <T>
 */
public abstract class ThreadPoolBean <T extends Runnable>{
    protected final ThreadPool<T> threadPool;

    public ThreadPoolBean(String poolName, int maxPoolSize) {
        this.threadPool = new ThreadPool<T>(poolName, maxPoolSize);
    }

    public void shutdown(){
        threadPool.shutdown();
    }

    public void newTask(T task) throws ThreadPool.ThreadPoolShutDownException {
        threadPool.submitTask(task);
    }
}
