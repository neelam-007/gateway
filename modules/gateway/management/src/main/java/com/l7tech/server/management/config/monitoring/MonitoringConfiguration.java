/**
 * Copyright (C) 2008-2009 Layer 7 Technologies Inc.
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
 * <p/>
 * TODO should the List&lt;Pair&lt;Trigger, NotificationRule&gt;&gt; be a separate property, rather than each Trigger having its own List&lt;NotificationRule&gt;?
 *  
 * @author alex
 */
@Entity
public final class MonitoringConfiguration extends NamedEntityImp {
    private Set<Trigger> triggers = new HashSet<Trigger>();
    private Set<NotificationRule> notificationRules = new HashSet<NotificationRule>();

    @OneToMany(mappedBy="monitoringConfiguration")
    public Set<Trigger> getTriggers() {
        return triggers;
    }

    public void setTriggers(Set<Trigger> triggers) {
        this.triggers = triggers;
    }

    @OneToMany(mappedBy="monitoringConfiguration")
    public Set<NotificationRule> getNotificationRules() {
        return notificationRules;
    }

    public void setNotificationRules(Set<NotificationRule> notificationRules) {
        this.notificationRules = notificationRules;
    }

    @SuppressWarnings({"RedundantIfStatement"})
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;

        MonitoringConfiguration that = (MonitoringConfiguration) o;

        if (notificationRules != null ? !notificationRules.equals(that.notificationRules) : that.notificationRules != null)
            return false;
        if (triggers != null ? !triggers.equals(that.triggers) : that.triggers != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + (triggers != null ? triggers.hashCode() : 0);
        result = 31 * result + (notificationRules != null ? notificationRules.hashCode() : 0);
        return result;
    }
}
