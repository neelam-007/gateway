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

    /**
     * Return a new HashMap populated with interpolatable variables describing the specified Trigger.
     *
     * @param trigger the trigger to examine.  Required.
     * @return a Map full of context variables for this trigger.  Never null or empty.
     */
    protected Map<String, String> getMonitoringVariables(Trigger trigger) {
        Map<String, String> variables = new HashMap<String, String>();
        variables.put("monitoring.context.entityType", trigger.getComponentType().toString());
        variables.put("monitoring.context.entityPathName", trigger.getComponentId());

        if (trigger instanceof PropertyTrigger) {
            PropertyTrigger ptrig = (PropertyTrigger) trigger;
            variables.put("monitoring.context.propertyType", ptrig.getMonitorableId());
            variables.put("monitoring.context.propertyState", "alert");
            variables.put("monitoring.context.propertyValue", ""); // TODO
            variables.put("monitoring.context.propertyUnit", "");  // TODO
            variables.put("monitoring.context.triggerValue", "");  // TODO
        }
        return variables;
    }
}
