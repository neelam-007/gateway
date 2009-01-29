/**
 * Copyright (C) 2009 Layer 7 Technologies Inc.
 */
package com.l7tech.server.processcontroller.monitoring;

import com.l7tech.server.management.config.monitoring.NotificationRule;
import com.l7tech.server.management.config.monitoring.Trigger;
import com.l7tech.server.management.api.monitoring.NotificationAttempt;

import java.util.NavigableMap;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.TimeUnit;

/**
 * Tracks the notification state for a given tuple of Trigger and NotificationRule.
 */
public class NotificationState {
    private final Trigger trigger;
    private final NotificationRule rule;
    private final int maxNotificationRateCount;
    private final TimeUnit maxNotificationRatePeriod;
    private final NavigableMap<Long, NotificationAttempt> attempts = new ConcurrentSkipListMap<Long, NotificationAttempt>();

    public NotificationState(Trigger trigger, NotificationRule rule, int maxNotificationRateCount, TimeUnit maxNotificationRatePeriod) {
        this.rule = rule;
        this.trigger = trigger;
        this.maxNotificationRateCount = maxNotificationRateCount;
        this.maxNotificationRatePeriod = maxNotificationRatePeriod;
    }

    public NotificationState(NotificationState oldState) {
        this.rule = oldState.rule;
        this.trigger = oldState.trigger;
        this.maxNotificationRateCount = oldState.maxNotificationRateCount;
        this.maxNotificationRatePeriod = oldState.maxNotificationRatePeriod;
        this.attempts.putAll(oldState.attempts);
    }

    void notified(Long when, NotificationAttempt attempt) {
        attempts.put(when, attempt);
        // TODO GC?
    }

    public NotificationRule getRule() {
        return rule;
    }

    public Trigger getTrigger() {
        return trigger;
    }

    public int getMaxNotificationRateCount() {
        return maxNotificationRateCount;
    }

    public TimeUnit getMaxNotificationRatePeriod() {
        return maxNotificationRatePeriod;
    }
}
