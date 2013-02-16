package com.l7tech.server.util;

import java.util.TimerTask;
import java.util.logging.Logger;
import java.util.logging.Level;

import com.l7tech.util.ExceptionUtils;

/**
 * ManagedTimerTask for use with a {@link ManagedTimer}
 *
 * <p>This task can be extended for TimerTasks that need to be able to use
 * {@link TimerTask#cancel()}</p>
 *
 * @author Steve Jones
 */
public abstract class ManagedTimerTask extends TimerTask {

    //- PUBLIC

    @Override
    public final void run() {
        final ManagedTimer mt = manager;
        if (mt != null)
            mt.enter(this);
        else
            logger.warning("Task is running but not managed!");

        try {
            doRun();
        }
        catch (Throwable thrown) {
            logger.log(Level.SEVERE, "Uncaught exception in background task '" + ExceptionUtils.getMessage(thrown) + "'.", thrown);    
        }
        finally {
            if (mt != null)
                mt.exit(this);
        }
    }

    //- PROTECTED

    /**
     * Managed tasks should implement this instead of {@link Runnable#run()}
     */
    protected abstract void doRun();

    //- PACKAGE

    void scheduled(final ManagedTimer managedTimer) {
        if (manager != null)
            throw new IllegalStateException("This task is already scheduled!");
        manager = managedTimer;
    }

    //- PRIVATE

    private static final Logger logger = Logger.getLogger(ManagedTimerTask.class.getName());

    private ManagedTimer manager;
}
