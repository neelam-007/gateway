/*
 * Copyright (C) 2004 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.common.util;

import java.util.Timer;
import java.util.TimerTask;
import java.util.logging.Logger;
import java.util.logging.Level;

/**
 * Single thread handling low-priority maintenance tasks.  There is only one thread, so some tasks may be delayed
 * while other maintenance tasks complete.
 */
public final class Background {
    protected static final Logger logger = Logger.getLogger(Background.class.getName());

    private static final Timer timer = new Timer(true);

    private Background() {
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
            return t.cancel();
        }
    }

    /**
     * Schedule the task for repeated execution after the specified delay, every period.
     * @see Timer#schedule(java.util.TimerTask, long, long) 
     */
    public static void scheduleRepeated(TimerTask timerTask, long delay, long period) {
        timer.schedule(new SafeTimerTask(timerTask), delay, period);
    }

    /**
     * Schedule the task for one-time execution after the specified delay.
     * @see Timer#schedule(java.util.TimerTask, long)
     */
    public static void scheduleOneShot(TimerTask task, long delay) {
        timer.schedule(new SafeTimerTask(task), delay);
    }
}
