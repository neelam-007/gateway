package com.ca.apim.gateway.extension.sharedstate;

import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/**
 * A SharedScheduledExecutorService allows for scheduling tasks to be run on all members of the cluster in additional
 * to any one member.
 */
public interface SharedScheduledExecutorService extends ScheduledExecutorService {

    /**
     * @see ScheduledExecutorService#schedule(Runnable, long, TimeUnit) but for all members of the cluster
     */
    ScheduledFuture<?> scheduleForAllMembers(Runnable task, long delay, TimeUnit unit);

    /**
     * @see ScheduledExecutorService#scheduleAtFixedRate(Runnable, long, long, TimeUnit) but for all members of the cluster
     */
    ScheduledFuture<?> scheduleAtFixedRateForAllMembers(Runnable task, long initialDelay, long period, TimeUnit timeUnit);

    /**
     * @see ScheduledExecutorService#scheduleWithFixedDelay(Runnable, long, long, TimeUnit)  but for all members of the cluster
     */
    ScheduledFuture<?> scheduleWithFixedDelayForAllMembers(Runnable command, long initialDelay, long delay, TimeUnit unit);
}
