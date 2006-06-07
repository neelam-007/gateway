/*
 * Copyright (C) 2004 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.common.util;

import java.util.Timer;
import java.util.TimerTask;

/**
 * Single thread handling low-priority maintenance tasks.  There is only one thread, so some tasks may be delayed
 * while other maintenance tasks complete.
 */
public final class Background {
    private static final Timer timer = new Timer(true);

    private Background() {
    }

    /**
     * Schedule the task for repeated execution after the specified delay, every period.
     * @see Timer#schedule(java.util.TimerTask, long, long) 
     */
    public static void schedule(TimerTask timerTask, long delay, long period) {
        timer.schedule(timerTask, delay, period);
    }

    /**
     * Schedule the task for one-time execution after the specified delay.
     * @see Timer#schedule(java.util.TimerTask, long)
     */
    public static void schedule(TimerTask task, long delay) {
        timer.schedule(task, delay);
    }
}
