/**
 * Copyright (C) 2009 Layer 7 Technologies Inc.
 */
package com.l7tech.server.processcontroller.monitoring;

import com.l7tech.server.management.config.monitoring.NotificationRule;
import com.l7tech.server.management.config.monitoring.Trigger;
import com.l7tech.server.management.config.monitoring.PropertyTrigger;
import com.l7tech.util.Closeable;

import java.io.IOException;
import java.util.Map;
import java.util.HashMap;

public abstract class Notifier implements Closeable {
    protected final NotificationRule rule;

    protected Notifier(NotificationRule rule) {
        this.rule = rule;
    }

    /**
     * Do the notification.
     *
     * @param timestamp the time when the trigger fired (usually quite recent)
     * @param trigger the trigger that fired
     * @throws IOException if the notification could be done
     */
    public abstract void doNotification(Long timestamp, Trigger trigger) throws IOException;

    @Override
    public void close() { }

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
            variables.put("monitoring.context.propertyType", ptrig.getPropertyName());
            variables.put("monitoring.context.propertyState", "alert");
            variables.put("monitoring.context.propertyValue", ""); // TODO
            variables.put("monitoring.context.propertyUnit", "");  // TODO
            variables.put("monitoring.context.triggerValue", "");  // TODO
        }
        return variables;
    }
}
