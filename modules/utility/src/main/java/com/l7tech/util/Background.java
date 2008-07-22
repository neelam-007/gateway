/*
 * Copyright (C) 2004 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.util;

import java.util.*;
import java.util.logging.Logger;
import java.util.logging.Level;

/**
 * Single thread handling low-priority maintenance tasks.  There is only one thread, so some tasks may be delayed
 * while other maintenance tasks complete.
 */
public final class Background {
    protected static final Logger logger = Logger.getLogger(Background.class.getName());

    private static final Map<ClassLoader, Map<TimerTask, Object>> tasksByClassLoader =
            new WeakHashMap<ClassLoader, Map<TimerTask, Object>>();
    private static Timer timer = new Timer(true);
    private static boolean timerLocked = false;

    private Background() {
    }

    public interface TimerTaskWrapper {
        public TimerTask wrap(TimerTask timerTask);
        public TimerTask unwrap(TimerTask timerTask);
    }

    public static void installTimer(Timer timer) {
        if (timerLocked)
            throw new IllegalStateException("Locked timer cannot be replaced.");
        timerLocked = true;
        Background.timer = timer;
    }

    public static class SafeTimerTask extends TimerTask {
        private final TimerTask t;

        public SafeTimerTask(TimerTask t) {
            this.t = t;
        }

        public void run() {
            try {
                t.run();
            } catch (Throwable e) {
                logger.log(Level.SEVERE, "Exception in background task: " + ExceptionUtils.getMessage(e), e);
            }
        }

        public long scheduledExecutionTime() {
            return t.scheduledExecutionTime();
        }

        public boolean cancel() {
            t.cancel();
            return super.cancel();
        }
    }

    /**
     * Schedule the task for repeated execution after the specified delay, every period.
     *
     * <p>Note that a task scheduled in this way cannot {@link TimerTask#cancel() cancel()}
     * itself.</p>
     *
     * @param task       the task to schedule.  Must not be null
     * @param delay      millisecond delay before its first invocation
     * @param period     millisecond delay in between subsequent invocations
     * @see Timer#schedule(java.util.TimerTask, long, long)
     */
    public static void scheduleRepeated(TimerTask task, long delay, long period) {
        timerLocked = true;
        timer.schedule(wrapTask(task), delay, period);
    }

    /**
     * Schedule the task for one-time execution after the specified delay.
     *
     * @param task       the task to schedule.  Must not be null
     * @param delay      millisecond delay before its invocation
     * @see Timer#schedule(java.util.TimerTask, long)
     */
    public static void scheduleOneShot(TimerTask task, long delay) {
        timerLocked = true;
        timer.schedule(wrapTask(task), delay);
    }

    /**
     * Cancel the specified timer task.
     *
     * @param taskToCancel the task to cancel.
     */
    public static void cancel(TimerTask taskToCancel) {
        final Set<TimerTask> toCancel = new HashSet<TimerTask>();

        synchronized (Background.class) {
            Collection<Map<TimerTask, Object>> taskLists = tasksByClassLoader.values();
            for (Map<TimerTask, Object> taskList : taskLists) {
                Set<TimerTask> wrappedTasks = taskList.keySet();
                for (TimerTask wrappedTask : wrappedTasks) {
                    if (wrappedTask == null)
                        continue;
                    if (unwrapTask(wrappedTask) == taskToCancel)
                        toCancel.add(wrappedTask);
                }
            }
        }

        cancelTasks(toCancel);
    }

    // Wrap this task in a wrapper that absorbs exceptions, and record which class loader the task is from in case
    // we need to evict it later (if a module is unloaded)
    private static TimerTask wrapTask(TimerTask originalTask) {
        ClassLoader classLoader = originalTask.getClass().getClassLoader();
        TimerTask task = null;
        if (timer instanceof TimerTaskWrapper)
            task = ((TimerTaskWrapper)timer).wrap(originalTask);
        else
            task = new SafeTimerTask(originalTask);

        synchronized (Background.class) {
            Map<TimerTask, Object> taskSet = tasksByClassLoader.get(classLoader);
            if (taskSet == null) {
                taskSet = new WeakHashMap<TimerTask, Object>();
                tasksByClassLoader.put(classLoader, taskSet);
            }
            taskSet.put(task, null);
        }

        return task;
    }

    private static TimerTask unwrapTask(final TimerTask wrappedTask) {
        TimerTask task = null;

        if (wrappedTask instanceof SafeTimerTask)
            task = ((SafeTimerTask)wrappedTask).t;
        else if (timer instanceof TimerTaskWrapper)
            task = ((TimerTaskWrapper)timer).unwrap(wrappedTask);

        return task;
    }

    /**
     * Cancel all scheduled tasks from the specified ClassLoader and then purge the timer,
     * so Background is no longer retaining references to timer tasks originating from this class loader.
     * <p/>
     * This is used when dynamically detaching server modules to help clean out references to their
     * periodic tasks.
     *
     * @param classLoader the class loader whose tasks are to be canceled and evicted
     */
    public static void cancelAllTasksFromClassLoader(ClassLoader classLoader) {
        final Set<TimerTask> toCancel;

        synchronized (Background.class) {
            Map<TimerTask, Object> taskSet = tasksByClassLoader.remove(classLoader);
            if (taskSet == null || taskSet.isEmpty())
                return;
            toCancel = new HashSet<TimerTask>(taskSet.keySet());
        }

        cancelTasks(toCancel);
    }

    private static void cancelTasks(Set<TimerTask> toCancel) {
        for (TimerTask task : toCancel) {
            if (task != null) {
                TimerTask actualTask = unwrapTask(task);
                if (logger.isLoggable(Level.INFO))
                    logger.info("Cancelling background task '" + actualTask + "' (" + actualTask.getClass().getName() + ")");
                task.cancel();
            }
        }

        // Ensure purge happens after any active call returns
        timer.schedule(new TimerTask() {
            public void run() {
                timer.purge();
            }
        }, 0);
    }
}
