/**
 * Copyright (C) 2008 Layer 7 Technologies Inc.
 */
package com.l7tech.server.management.config.monitoring;

import com.l7tech.objectmodel.imp.NamedEntityImp;

import javax.persistence.Entity;
import javax.persistence.OneToMany;
import java.util.HashSet;
import java.util.Set;

/**
 * A named collection of {@link Trigger}s and {@link NotificationRule}s that describes the monitoring and notification
 * behaviour for a system.
 *  
 * @author alex
 */
@Entity
public class MonitoringScheme extends NamedEntityImp {
    private Set<Trigger> triggers = new HashSet<Trigger>();
    private Set<NotificationRule> notificationRules = new HashSet<NotificationRule>();

    @OneToMany(mappedBy="monitoringScheme")
    public Set<Trigger> getTriggers() {
        return triggers;
    }

    public void setTriggers(Set<Trigger> triggers) {
        this.triggers = triggers;
    }

    @OneToMany(mappedBy="monitoringScheme")
    public Set<NotificationRule> getNotificationRules() {
        return notificationRules;
    }

    public void setNotificationRules(Set<NotificationRule> notificationRules) {
        this.notificationRules = notificationRules;
    }
}
