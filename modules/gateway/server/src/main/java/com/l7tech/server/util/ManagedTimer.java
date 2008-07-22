package com.l7tech.server.util;

import java.util.Timer;
import java.util.TimerTask;
import java.util.Date;
import java.util.Collection;
import java.util.ArrayList;
import java.util.Collections;
import java.util.logging.Logger;
import java.lang.ref.WeakReference;

import com.l7tech.util.Background;

/**
 * Extension of Timer with life cycle management.
 *
 * <p>This Timer implementation supports a small number of long-lived
 * ManagedTimer objects.</p>
 *
 * <p>WARNING! TimerTasks that are scheduled with the ManagedTimer cannot
 * be cancelled by invoking {@link TimerTask#cancel}.</p>
 *
 * <p>If you need to cancel tasks you will need to extend {@link ManagedTimerTask}</p>
 *
 * @author Steve Jones
 */
public class ManagedTimer extends Timer implements Background.TimerTaskWrapper {

    //- PUBLIC

    public ManagedTimer(final String name) {
        super(decorateName(name), false);
        this.name = decorateName(name);
        allManagedTimers.add(new WeakReference(this));
    }

    public void schedule(TimerTask task, long delay) {
        super.schedule(managementProxy(task), delay);
    }

    public void schedule(TimerTask task, long delay, long period) {
        super.schedule(managementProxy(task), delay, period);
    }

    public void schedule(TimerTask task, Date firstTime, long period) {
        super.schedule(managementProxy(task), firstTime, period);
    }

    public void schedule(TimerTask task, Date time) {
        super.schedule(managementProxy(task), time);
    }

    public void scheduleAtFixedRate(TimerTask task, long delay, long period) {
        super.scheduleAtFixedRate(managementProxy(task), delay, period);
    }

    public void scheduleAtFixedRate(TimerTask task, Date firstTime, long period) {
        super.scheduleAtFixedRate(managementProxy(task), firstTime, period);
    }

    /**
     * Cancel all timer tasks and wait until any running task is complete.
     */
    public void cancelAndWait() {
        // cancel
        cancel();

        // is a task currently active?
        TimerTask currentTask = null;
        synchronized(taskLock) {
            currentTask = runningTask;
        }

        // if so then wait for it to complete
        if (currentTask != null) {
            logger.fine("Waiting for current task to complete.");
            long waitStartTime = System.currentTimeMillis();
            try {
                long totalWait = 0;
                long waitStep = 100L;
                boolean done = false;
                while (totalWait < SHUTDOWN_MAX_WAIT && !done) {
                    synchronized (currentTask) {
                        currentTask.wait(waitStep);
                    }
                    totalWait += waitStep;
                    synchronized(taskLock) {
                        done = runningTask == null;
                    }
                }
            }
            catch(InterruptedException ie) {
                Thread.currentThread().interrupt();
            }
            logger.fine("Wait time for current task was " + (System.currentTimeMillis() - waitStartTime) + "ms.");
            synchronized(taskLock) {
                currentTask = runningTask;
            }
            if (currentTask != null) {
                logger.warning("Current task did not complete! (max wait exceeded)");
            }
        } else {
            logger.finest("No current task, timer is shutdown.");
        }
    }

    /**
     * Cancel all timer tasks on all ManagedTimers and wait until any running task is complete.
     */
    public static void cancelAndWaitAll() {
        synchronized(allManagedTimers) {
            // First cancel everything
            for (WeakReference<ManagedTimer> reference : allManagedTimers) {
                ManagedTimer managedTimer = reference.get();
                if (managedTimer != null) {
                    logger.fine("Cancelling timer '" + managedTimer.getName() + "'.");
                    managedTimer.cancel();
                }
            }

            // Then wait for everything to stop
            for (WeakReference<ManagedTimer> reference : allManagedTimers) {
                ManagedTimer managedTimer = reference.get();
                if (managedTimer != null) {
                    logger.finest("Waiting for timer tasks to complete '" + managedTimer.getName() + "'.");
                    managedTimer.cancelAndWait();
                }
            }

            logger.fine("Timer cancel complete.");
        }
    }

    public TimerTask unwrap(TimerTask timerTask) {
        TimerTask unwrapped = null;

        if (timerTask instanceof ManagedTimerTaskWrapper) {
            unwrapped = ((ManagedTimerTaskWrapper) timerTask).delegate;
        } else {
            unwrapped = timerTask;
        }

        return unwrapped;
    }

    public TimerTask wrap(TimerTask timerTask) {
        TimerTask wrapped = null;

        if (timerTask instanceof ManagedTimerTaskWrapper) {
            wrapped = (ManagedTimerTaskWrapper) timerTask;
        } else {
            wrapped = new ManagedTimerTaskWrapper(timerTask);
        }

        return wrapped;
    }

    //- PACKAGE

    void enter(final TimerTask task) {
        synchronized(taskLock) {
            runningTask = task;
        }
    }

    void exit(final TimerTask task) {
        synchronized(taskLock) {
            runningTask = null;
        }
        synchronized(this) {
            this.notifyAll();
        }
    }

    //- PRIVATE

    private static final Logger logger = Logger.getLogger(ManagedTimer.class.getName());

    private static final long SHUTDOWN_MAX_WAIT = 5000L;
    private static final String NAME_PREFIX = "ManagedTimer[";
    private static final String NAME_POSTFIX = "]";
    private static final Collection<WeakReference<ManagedTimer>> allManagedTimers = Collections.synchronizedList(new ArrayList<WeakReference<ManagedTimer>>());

    private final String name;
    private final Object taskLock = new Object();
    private TimerTask runningTask;

    /**
     * Get the name for this timer.
     *
     * @return The timer name.
     */
    public String getName() {
        return name;
    }

    /**
     * Wrap a timer task in a management proxy that tracks the running task.
     *
     * @param timerTask the TimerTask to wrap (may be null)
     * @return null if timerTask is null else the wrapped task
     */
    private TimerTask managementProxy(final TimerTask timerTask) {
        ManagedTimerTask managedTimerTask = null;

        if (timerTask instanceof ManagedTimerTask) {
            managedTimerTask = (ManagedTimerTask) timerTask;    
        } else if (timerTask != null) {
            managedTimerTask = new ManagedTimerTaskWrapper(timerTask);
        }

        managedTimerTask.scheduled(this);

        return managedTimerTask;
    }

    /**
     * Decorate the name to distinguish managed timers.
     *
     * @param name The base name
     * @return The decorated name.
     */
    private static String decorateName(final String name) {
        return NAME_PREFIX + name + NAME_POSTFIX;
    }

    /**
     * Extension of TimerTask that tracks if the underlying task is currently
     * running.
     */
    private class ManagedTimerTaskWrapper extends ManagedTimerTask {
        private TimerTask delegate;

        private ManagedTimerTaskWrapper(TimerTask delegate) {
            this.delegate = delegate;
        }

        protected void doRun() {
            delegate.run();
        }

        public boolean cancel() {
            delegate.cancel();
            return super.cancel();
        }
    }
}
