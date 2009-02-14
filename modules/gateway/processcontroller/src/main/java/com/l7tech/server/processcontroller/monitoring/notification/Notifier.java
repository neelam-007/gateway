/**
 * Copyright (C) 2009 Layer 7 Technologies Inc.
 */
package com.l7tech.server.processcontroller.monitoring.notification;

import com.l7tech.server.management.api.monitoring.NotificationAttempt;
import com.l7tech.server.management.config.monitoring.NotificationRule;
import com.l7tech.server.management.config.monitoring.PropertyTrigger;
import com.l7tech.server.management.config.monitoring.Trigger;
import com.l7tech.util.Closeable;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * Note that implementations must have a public, one-argument constructor that takes an {@link #<RT>}.
 * @param <RT> the type of NotificationRule that this Notifier is configured with 
 */
public abstract class Notifier<RT extends NotificationRule> implements Closeable {
    protected final RT rule;

    protected Notifier(RT rule) {
        this.rule = rule;
    }

    /**
     * Do the notification.
     *
     * @param timestamp the time when the trigger fired (usually quite recent)
     * @param value the value that was observed
     * @param trigger the trigger that fired
     * @throws IOException if the notification could be done
     */
    public abstract NotificationAttempt.StatusType doNotification(Long timestamp, Object value, Trigger trigger) throws IOException;

    @Override
    public void close() { }

    @Override
    protected void finalize() throws Throwable {
        try {
            super.finalize();
        } finally {
            close();
        }
    }

    private static String sn(Object in) {
        if (in == null) return "";
        String s = String.valueOf(in);
        return s == null ? "" : s;
    }

    /**
     * Return a new HashMap populated with interpolatable variables describing the specified Trigger.
     *
     * @param trigger the trigger to examine.  Required.
     * @param value the value of the property
     * @return a Map full of context variables for this trigger.  Never null or empty.
     */
    protected Map<String, String> getMonitoringVariables(Trigger trigger, Object value) {
        Map<String, String> variables = new HashMap<String, String>();
        variables.put("monitoring.context.entitytype", sn(trigger.getComponentType()));
        variables.put("monitoring.context.entitypathname", sn(trigger.getComponentId()));

        if (trigger instanceof PropertyTrigger) {
            PropertyTrigger ptrig = (PropertyTrigger) trigger;
            variables.put("monitoring.context.propertytype", sn(ptrig.getMonitorableId()));
            variables.put("monitoring.context.propertystate", "alert");
            variables.put("monitoring.context.propertyvalue", sn(value));
            variables.put("monitoring.context.propertyunit", sn(ptrig.getMonitorable().getValueUnit()));
            variables.put("monitoring.context.triggervalue", sn(ptrig.getTriggerValue()));
        }
        return variables;
    }
}
