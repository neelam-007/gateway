package com.l7tech.util;

import java.lang.reflect.InvocationTargetException;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.Callable;
import java.util.concurrent.Executor;

/**
 * A simple utility class that executes a Runnable or Callable synchronously in the current thread with a timeout.
 */
public class TimeoutExecutor implements Executor {
    private static class TimerHolder {
        private static final Timer timeoutExecutorTimer = new Timer("TimeoutExecutor global timer", true);
    }

    private final Timer timer;
    private final long timeoutMillis;

    /**
     * Create an executor that will give tasks up to timeoutMillis ms to execute.
     * This will use a Timer shared with all other TimeoutExecutor instances.
     *
     * @param timeoutMillis milliseconds to give the task to run before starting to interrupt it.  Must be nonnegative.
     */
    public TimeoutExecutor(long timeoutMillis) {
        this(timeoutMillis, TimerHolder.timeoutExecutorTimer);
    }

    /**
     * Create an executor that will give tasks up to timeoutMillis ms to execute, using the specified Timer instance.
     *
     * @param timeoutMillis milliseconds to give the task to run before starting to interrupt it.  Must be nonnegative.
     * @param timer   the timer to use to schedule the interrupts.  Must not be null.
     */
    public TimeoutExecutor(long timeoutMillis, Timer timer) {
        if (timer == null) throw new IllegalArgumentException("timer must not be null");
        if (timeoutMillis < 0) throw new IllegalArgumentException("timeoutMillis must be nonnegative");
        this.timer = timer;
        this.timeoutMillis = timeoutMillis;
    }

    public void execute(final Runnable command) {
        try {
            runWithTimeout(timer, timeoutMillis, new Callable<Object>() {
                public Object call() throws Exception {
                    command.run();
                    return null;
                }
            });
        } catch (InvocationTargetException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Run the specified runnable with a timeout.
     * <p/>
     * If the runnable hasn't returned (or thrown an unchecked exception) after timeoutMillis milliseconds,
     * the current thread will be interrupted, and then interrupted again every (timeoutMillis / 10 + 200) milliseconds,
     * until the runnable returns or throws.
     *
     * @param timer            the timer to use to schedule (and cancel) the interrupts
     * @param timeoutMillis    milliseconds to delay before sending the first interrupt
     * @param callable         a task to run.  It should be prepared to be interrupted if it runs for a long time.
     * @return the             return value from the callable.
     * @throws InvocationTargetException  if the Callable threw an exception.
     */
    public static <T> T runWithTimeout(Timer timer, long timeoutMillis, Callable<T> callable) throws InvocationTargetException {        
        if (Thread.currentThread().isInterrupted())
            throw new InvocationTargetException(new InterruptedException("Current thread is already interrupted"));

        // Schedule an interrupt before we read, and safely cancel it afterward
        final Object lock = new Object();
        final Thread readerThread = Thread.currentThread();
        final boolean[] wantInterrupt = { true };
        final boolean[] returned = { false };
        final boolean[] sentInterrupt = { false };
        synchronized (lock) {
            wantInterrupt[0] = true;
            returned[0] = false;
        }
        TimerTask interruptMe = new TimerTask() {
            public void run() {
                synchronized (lock) {
                    if (!wantInterrupt[0]) {
                        cancel();
                        return;
                    }
                    // Start interrupting, and keep doing it until it returns
                    readerThread.interrupt();
                    sentInterrupt[0] = true;
                    if (returned[0]) {
                        wantInterrupt[0] = false;
                        cancel();
                    }
                }
            }
        };

        try {
            timer.schedule(interruptMe, timeoutMillis, timeoutMillis / 10 + 200);
            return callable.call();
        } catch (Exception e) {
            throw new InvocationTargetException(e);
        } finally {
            // Ensure the interrupt is safely canceled and cleared
            synchronized (lock) {
                wantInterrupt[0] = false;
                returned[0] = true;
                interruptMe.cancel();
                if (sentInterrupt[0]) // swallow the interrupted status if we caused it ourselves
                    Thread.interrupted();
            }
        }
    }
}
