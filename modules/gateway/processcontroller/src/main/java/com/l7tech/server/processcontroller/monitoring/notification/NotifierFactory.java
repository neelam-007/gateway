/**
 * Copyright (C) 2009 Layer 7 Technologies Inc.
 */
package com.l7tech.server.processcontroller.monitoring.notification;

import com.l7tech.common.io.WhirlycacheFactory;
import com.l7tech.server.management.config.monitoring.EmailNotificationRule;
import com.l7tech.server.management.config.monitoring.HttpNotificationRule;
import com.l7tech.server.management.config.monitoring.NotificationRule;
import com.l7tech.server.management.config.monitoring.SnmpTrapNotificationRule;
import com.whirlycott.cache.Cache;
import com.whirlycott.cache.policy.LRUMaintenancePolicy;

import java.lang.reflect.Constructor;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public final class NotifierFactory {
    private static final Map<Class<? extends NotificationRule>, Class<? extends Notifier>> registry = Collections.unmodifiableMap(new HashMap<Class<? extends NotificationRule>, Class<? extends Notifier>>() {{
        put(SnmpTrapNotificationRule.class, SnmpNotifier.class);
        put(EmailNotificationRule.class, EmailNotifier.class);
        put(HttpNotificationRule.class, HttpNotifier.class);
    }});

    private final Cache notifierCache = WhirlycacheFactory.createCache("notifierCache", 500, 9931, new LRUMaintenancePolicy());

    public <RT extends NotificationRule> Notifier<RT> getNotifier(RT rule) {
        @SuppressWarnings({"unchecked"})
        Notifier<RT> notifier = (Notifier<RT>) notifierCache.retrieve(rule);
        if (notifier != null) return notifier;

        final Class<? extends Notifier> notifierClass = registry.get(rule.getClass());
        if (notifierClass == null) throw new IllegalArgumentException("Unsupported notification rule type: " + rule.getClass().getSimpleName());
        try {
            Constructor<? extends Notifier> ctor = notifierClass.getConstructor(Notifier.class);
            //noinspection unchecked
            notifier = ctor.newInstance(rule);
            notifierCache.store(rule, notifier);
            return notifier;
        } catch (Exception e) {
            throw new IllegalArgumentException("Couldn't make Notifier", e);
        }
    }
}
