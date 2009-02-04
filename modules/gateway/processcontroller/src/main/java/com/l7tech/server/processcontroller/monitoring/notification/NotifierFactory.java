/**
 * Copyright (C) 2009 Layer 7 Technologies Inc.
 */
package com.l7tech.server.processcontroller.monitoring.notification;

import com.l7tech.server.management.config.monitoring.EmailNotificationRule;
import com.l7tech.server.management.config.monitoring.HttpNotificationRule;
import com.l7tech.server.management.config.monitoring.NotificationRule;
import com.l7tech.server.management.config.monitoring.SnmpTrapNotificationRule;

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

    public Notifier makeNotifier(NotificationRule rule) {
        final Class<? extends Notifier> notifierClass = registry.get(rule.getClass());
        if (notifierClass == null) throw new IllegalArgumentException("Unsupported notification rule type: " + rule.getClass().getSimpleName());
        try {
            Constructor<? extends Notifier> ctor = notifierClass.getConstructor(Notifier.class);
            return ctor.newInstance(rule);
        } catch (Exception e) {
            throw new IllegalArgumentException("Couldn't make Notifier", e);
        }
    }
}
