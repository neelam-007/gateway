/**
 * Copyright (C) 2008, Layer 7 Technologies Inc.
 * @author darmstrong
 */
package com.l7tech.util;

import java.util.concurrent.*;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Generic convenience class to manage a ThreadPoolExecutor. Simplifies the configuration of an executor and provides
 * reasonable defaults based on how executors are used in the gateway (jms, email) via several constructors.
 *
 * Main constructor allows almost everything to be configured. Several get methods are supplied and this class can be
 * extended as needed.
 *
 * @param <T> type of tasks the pool will execute
 */
public class ThreadPool<T extends Runnable> {

    /**
     * Construct a ThreadPool with this class specific defaults.
     * <p/>
     * Delegates to {@link #ThreadPool(String, int)}
     *
     * @param poolName name of the pool
     */
    public ThreadPool(String poolName) {
        this(poolName, 25);
    }

    /**
     * Create a ThreadPool with the specified name and max thread pool size.
     *
     * The ThreadPool will use create a ThreadPoolExecutor with the following characteristics:-
     * <p>
     * <ul>
     * <li>Core size is 5 and the threads are allowed to terminate if idle past keep alive time.
     * <li>Keep alive time is 30 seconds
     * <li>A bounded queue with a max of 25 is used to store tasks for execution.
     * <li>The maximum amount of threads is set to 25.
     * </ul>
     *
     * The behaviour of this pool is that core threads are created as needed up to 5. When a job is submitted it is
     * queued and waits for a core thread to become available. If the queue is full then a thread will be created up
     * until max threads are created (25). If a task is submitted when the queue is full and max threads have been
     * created, then the task is rejected. Any thread created over core size will terminate if idle for longer than
     * the keep alive time.
     *
     * @param poolName name of the pool
     * @param maxPoolSize max thread in the pool. Must be bigger than core pool size of 5.
     */
    public ThreadPool(final String poolName,
                      final int maxPoolSize){
        this(poolName, 5, maxPoolSize, 25, 30000l, TIME_UNIT, true, null, null, null);
    }

    /**
     * Create a ThreadPool with specific configuration. The ThreadPoolExecutor created can be made boundless by setting
     * the maxPoolSize to Integer.MAX_VALUE.
     * 
     * @param poolName name of the pool
     * @param corePoolSize core size of the thread pool. Always <= of the maximum pool size.
     * @param maxPoolSize maximum size the pool may reach.
     * @param maxQueuedTasks maximum number of tasks which can be queued before a new thread is created. A new thread
     * is created only if maxPoolSize has not been reached. If it has the behavior is dependent on the rejected
     * execution handler.
     * @param keepAliveTime how long threads above the core size can idle before being terminating.
     * @param timeUnit time unit which applies to keep alive times
     * @param allowCoreThreadTimeOuts if true, then core threads are allowed to terminate when idle.
     * @param rejectedExecutionHandler if null the default ThreadPoolExecutor.AbortPolicy is used, which will throw when
     * maximum threads have been created and the bound queue is full.
     * @param beforeExecute hook to run before a task is executed, can be null.
     * @param afterExecute hook to run after a task is executed, can be null.
     */
    public ThreadPool(final String poolName,
                      final int corePoolSize,
                      final int maxPoolSize,
                      final int maxQueuedTasks,
                      final long keepAliveTime,
                      final TimeUnit timeUnit,
                      final boolean allowCoreThreadTimeOuts,
                      final RejectedExecutionHandler rejectedExecutionHandler,
                      final Functions.BinaryVoid<Thread, Runnable> beforeExecute,
                      final Functions.BinaryVoid<Runnable, Throwable> afterExecute) {
        if(corePoolSize <= 1) throw new IllegalArgumentException("Core pool size must be >= 0");
        if(maxPoolSize < corePoolSize) throw new IllegalArgumentException("maxPoolSize must be >= corePoolSize");
        if(keepAliveTime < 1) throw new IllegalArgumentException("keepAliveTime must be >= 0");
        
        this.poolName = poolName;
        this.corePoolSize = corePoolSize;
        this.maxPoolSize = maxPoolSize;

        workerPool = new ThreadPoolExecutor(
                corePoolSize,
                maxPoolSize,
                keepAliveTime,
                timeUnit,
                new LinkedBlockingQueue<Runnable>(maxQueuedTasks),
                (rejectedExecutionHandler == null) ? new ThreadPoolExecutor.AbortPolicy() : rejectedExecutionHandler
        ) {
            @Override
            protected void beforeExecute(Thread t, Runnable r) {
                if (beforeExecute != null) {
                    beforeExecute.call(t, r);
                }
                super.beforeExecute(t, r);
            }

            @Override
            protected void afterExecute(Runnable r, Throwable t) {
                if (afterExecute != null) {
                    afterExecute.call(r, t);
                }
                super.afterExecute(r, t);
            }

            @Override
            protected void terminated() {
                logger.log(Level.INFO, "Thread pool '" + poolName+"' has terminated.");
                super.terminated();
            }
        };

        //should the core thread pool size drop when inactive?
        if(allowCoreThreadTimeOuts){
            workerPool.allowCoreThreadTimeOut(true);
        }
    }

    public String getPoolName() {
        return poolName;
    }

    public int getCorePoolSize() {
        return corePoolSize;
    }

    public void setCorePoolSize(int corePoolSize) {
        this.corePoolSize = corePoolSize;
        this.workerPool.setCorePoolSize(corePoolSize);
    }

    public int getMaxPoolSize() {
        return maxPoolSize;
    }

    public void setMaxPoolSize(int maxPoolSize) {
        if(maxPoolSize < corePoolSize){
            throw new IllegalArgumentException("maxPoolSize must be >= corePoolSize");
        }
        
        this.maxPoolSize = maxPoolSize;
        this.workerPool.setMaximumPoolSize(maxPoolSize);
    }

    /**
     * Submit a new task for execution. Task will be executed immediately if a thread is available or if the number
     * of threads is below the core size or if the queue is full and the number of threads is less than the max size.
     * Otherwise it will be queued until a thread becomes available.
     *
     * @param newTask Task to submit to Executor
     * @throws RejectedExecutionException thrown if the task cannot be submitted. If a rejected execution
     * handler is configured then this exception will not be thrown (unless it throws it), otherwise this will happen if the
     * internal bounded queue is full and max threads have been created. This is a signal that the ThreadPool cannot
     * manage the tasks it is executing quickly enough with the current configuration. In this case the bounded queue should be
     * extended or the max threads should be increased.
     * @throws com.l7tech.util.ThreadPool.ThreadPoolShutDownException thrown if a task is submitted after a call to
     * shutdown() has been executed.
     *
     */
    public void submitTask(T newTask) throws ThreadPoolShutDownException, RejectedExecutionException{
        if(workerPool.isShutdown() || workerPool.isTerminating()){
            throw new ThreadPoolShutDownException();
        }

        workerPool.execute(newTask);
    }

    public boolean isShutdown(){
        return workerPool.isShutdown();
    }

    /**
     * Shutdown the ThreadPoolExecutor this ThreadPool manages. Invocation has no affect after first call.
     */
    public void shutdown(){
        System.out.println("SHUTTING DOWN!");
        if(workerPool.isShutdown()) return;

        synchronized (shutdownLock){
            if(!workerPool.isShutdown()){

                // shutdown the executor
                workerPool.shutdown();

                // tasks may still be queued for execution and actively executing.
                try {
                    long start = System.currentTimeMillis();
                    long now = start;
                    while ((!workerPool.isTerminated()) && (now - start < MAX_SHUTDOWN_TIME)) {
                        //block for 2 seconds waiting for pool to shutdown
                        workerPool.awaitTermination(2000L, TIME_UNIT);
                        now = System.currentTimeMillis();
                    }

                    if(logger.isLoggable(Level.INFO)){
                        logger.info(stats());
                    }

                } catch (InterruptedException iex) {
                    logger.info(poolName + " shutdown interrupted.");
                }
            }
        }
    }

    public static class ThreadPoolShutDownException extends Exception{}

    // - PRIVATE

    private String stats() {

        final String SEP="\n";

        StringBuffer sb = new StringBuffer();
        sb.append(SEP).append("Pool Status:").append(SEP);
        final String status = (workerPool.isTerminated()) ? "terminated" : (workerPool.isTerminating()) ? "still terminating" : "active";
        sb.append("\tPool '" + poolName+"' is ").append(status).append(".").append(SEP);
        if(!workerPool.isTerminated()){
            sb.append("\tActive Threads=").append(workerPool.getActiveCount()).append(SEP);
            sb.append("\tTask count=").append(workerPool.getTaskCount()).append(SEP);
        }

        sb.append("Summary Stats:").append(SEP);
        sb.append("\tMaxRunningThreads=").append(workerPool.getLargestPoolSize()).append(SEP);
        sb.append("\tCompletedTasks=").append(workerPool.getCompletedTaskCount()).append(SEP);

        return sb.toString();
    }

    private int corePoolSize;
    private int maxPoolSize;
    private final ThreadPoolExecutor workerPool;
    private final String poolName;
    private final Object shutdownLock = new Object();

    private static final long MAX_SHUTDOWN_TIME = 8000l; // 8 sec
    private static final java.util.concurrent.TimeUnit TIME_UNIT = TimeUnit.MILLISECONDS;
    private static final Logger logger = Logger.getLogger(ThreadPool.class.getName());
}
