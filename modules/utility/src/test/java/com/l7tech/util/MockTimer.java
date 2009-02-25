package com.l7tech.util;

import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * A timer that never actually ticks; instead, it runs jobs in the foreground when commanded manually.
 * This does not actually keep time -- all scheduled tasks are added to a queue, and their run methods are
 * called in the foreground when {@link #runNext} is invoked.
 */
public class MockTimer extends Timer {
    private static class TaskHolder {
        final TimerTask task;
        final boolean repeat;

        private TaskHolder(TimerTask task, boolean repeat) {
            this.task = task;
            this.repeat = repeat;
        }
    }

    private ConcurrentLinkedQueue<TaskHolder> queue = new ConcurrentLinkedQueue<TaskHolder>();

    private void addOneshot(TimerTask task) {
        queue.offer(new TaskHolder(task, false));
    }

    private void addRepeated(TimerTask task) {
        queue.offer(new TaskHolder(task, true));
    }

    public void schedule(TimerTask task, long delay) {
        addOneshot(task);
    }

    public void schedule(TimerTask task, Date time) {
        addOneshot(task);
    }

    public void schedule(TimerTask task, long delay, long period) {
        addRepeated(task);
    }

    public void schedule(TimerTask task, Date firstTime, long period) {
        addRepeated(task);
    }

    public void scheduleAtFixedRate(TimerTask task, long delay, long period) {
        addRepeated(task);
    }

    public void scheduleAtFixedRate(TimerTask task, Date firstTime, long period) {
        addRepeated(task);
    }

    /**
     * Run the next task.
     * <p/>
     * May throw an unchecked exception if the next timer task's run method throws one.
     *
     * @return true if a task was run.  False if the queue was empty.
     */
    public boolean runNext() {
        TaskHolder got = queue.poll();
        if (got == null)
            return false;

        if (got.repeat)
            queue.offer(got);
        got.task.run();
        return true;
    }

    public int purge() {
        int had = queue.size();
        queue.clear();
        super.purge();
        return had;
    }

    public void cancel() {
        queue.clear();
        super.cancel();
    }
}
