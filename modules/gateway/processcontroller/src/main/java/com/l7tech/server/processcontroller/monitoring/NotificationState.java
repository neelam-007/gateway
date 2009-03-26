/**
 * Copyright (C) 2009 Layer 7 Technologies Inc.
 */
package com.l7tech.server.processcontroller.monitoring;

import com.l7tech.server.management.api.monitoring.NotificationAttempt;
import com.l7tech.server.management.config.monitoring.NotificationRule;
import com.l7tech.server.management.config.monitoring.Trigger;
import com.l7tech.util.CollectionUtils;
import com.l7tech.util.Functions;

import java.util.*;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Tracks the notification state for a given tuple of Trigger and NotificationRule.
 */
public class NotificationState {
    private static final Logger logger = Logger.getLogger(NotificationState.class.getName());

    private final Trigger trigger;
    private final NotificationRule rule;
    private final int maxNotificationRateCount;
    private final TimeUnit maxNotificationRatePeriod;
    private final NavigableMap<Long, NotificationAttempt> attempts = new ConcurrentSkipListMap<Long, NotificationAttempt>();
    private final EnumSet<NotificationAttempt.StatusType> okStatuses = EnumSet.of(NotificationAttempt.StatusType.SENT, NotificationAttempt.StatusType.ACKNOWLEDGED, NotificationAttempt.StatusType.IN_PROGRESS);

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

    void notified(NotificationAttempt attempt) {
        attempts.put(attempt.getTimestamp(), attempt);
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

    public Map<Long, NotificationAttempt> getNotificationAttempts(long sinceWhen) {
        logger.fine("Looking for notification attempts since " + new Date(sinceWhen).toString());
        final SortedMap<Long, NotificationAttempt> map = attempts.tailMap(sinceWhen);
        if (logger.isLoggable(Level.FINE)) {
            logger.log(Level.FINE, "Returning notification attempts: " + CollectionUtils.mkString(map.keySet(), "[", ",", "]", new Functions.Unary<String, Long>() {
                @Override
                public String call(Long aLong) {
                    return new Date(aLong).toString();
                }
            } ));
        }
        return Collections.unmodifiableMap(map);
    }

    public void condition(Long when) {
        // TODO rate-limiting
    }

    public boolean isOKToFire() {
        return true; // TODO rate-limiting
    }

    void expireHistory(Long retainNewerThan) {
        if (attempts.size() == 0) {
            logger.log(Level.FINE, "Notification history is empty");
        } else {
            final Map.Entry<Long,NotificationAttempt> entry = attempts.lastEntry(); // Save the newest info just in case
            attempts.headMap(retainNewerThan).clear();
            attempts.put(entry.getKey(), entry.getValue());

            Long date = attempts.firstKey();
            logger.log(Level.FINE, "Oldest notification attempt is now " + new Date(date).toString());
        }
    }

    public NotificationAttempt getLastAttempt() {
        final Map.Entry<Long, NotificationAttempt> entry = attempts.lastEntry();
        if (entry == null) return null;
        return entry.getValue();
    }

    public void failed(NotificationAttempt notificationAttempt) {
        attempts.put(notificationAttempt.getTimestamp(), notificationAttempt);
    }

    public boolean isNotified() {
        // Scala: attempts.values.exists(okStatuses.contains(_.getStatus))
        return Functions.exists(attempts.values(), new Functions.Unary<Boolean, NotificationAttempt>() {
            @Override
            public Boolean call(NotificationAttempt notificationAttempt) {
                return okStatuses.contains(notificationAttempt.getStatus());
            }
        });
    }
}
