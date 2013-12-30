package com.l7tech.server.policy.module;

import com.l7tech.util.Background;
import org.jetbrains.annotations.NotNull;

import java.util.TimerTask;

/**
 * Extends ModulesScanner class and provide scheduled modules scanning.
 */
public abstract class ScheduledModuleScanner<T extends BaseAssertionModule> extends ModulesScanner<T> {

    // rescan timer
    private TimerTask scanTimerTask = null;

    /**
     * Initialize the timer.
     *
     * @param rescanMillis    rescan time in milliseconds
     */
    public void startTimer(long rescanMillis, @NotNull final Runnable callBack) {
        // create the scanning timer
        scanTimerTask = new TimerTask() {
            public void run() {
                callBack.run();
            }
        };
        Background.scheduleRepeated(scanTimerTask, rescanMillis, rescanMillis);
    }

    /**
     * Call this method during SSG shutdown, to clean the timer resources.
     */
    @Override
    public synchronized void destroy() {
        // stop the scan timer
        if (scanTimerTask != null) {
            Background.cancel(scanTimerTask);
            scanTimerTask = null;
        }

        super.destroy();
    }

}
