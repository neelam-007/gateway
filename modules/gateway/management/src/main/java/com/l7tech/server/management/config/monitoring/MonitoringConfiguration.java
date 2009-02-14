/**
 * Copyright (C) 2008-2009 Layer 7 Technologies Inc.
 */
package com.l7tech.server.management.config.monitoring;

import com.l7tech.objectmodel.imp.NamedEntityImp;

import javax.persistence.Entity;
import javax.xml.bind.annotation.*;
import java.util.LinkedHashSet;
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
@XmlRootElement(namespace="http://ns.l7tech.com/secureSpan/1.0/monitoring")
@XmlSeeAlso({PropertyTrigger.class, EventTrigger.class, EmailNotificationRule.class, SnmpTrapNotificationRule.class, HttpNotificationRule.class})
public final class MonitoringConfiguration extends NamedEntityImp {
    private Set<Trigger> triggers = new LinkedHashSet<Trigger>();
    private Set<NotificationRule> notificationRules = new LinkedHashSet<NotificationRule>();
    private boolean responsibleForClusterMonitoring;

    @XmlElementWrapper(name="triggers", namespace="http://ns.l7tech.com/secureSpan/1.0/monitoring")
    @XmlElementRef()
    public Set<Trigger> getTriggers() {
        return triggers;
    }

    public void setTriggers(Set<Trigger> triggers) {
        this.triggers = triggers;
    }

    @XmlElementWrapper(name="notificationRules", namespace="http://ns.l7tech.com/secureSpan/1.0/monitoring")
    @XmlElementRef
    public Set<NotificationRule> getNotificationRules() {
        return notificationRules;
    }

    public void setNotificationRules(Set<NotificationRule> notificationRules) {
        this.notificationRules = notificationRules;
    }

    /**
     * @return true if the node given this configuration is responsible for sending notifications for cluster-wide
     *              situations (audits full, etc)
     */
    @XmlAttribute(name="responsibleForClusterMonitoring")
    public boolean isResponsibleForClusterMonitoring() {
        return responsibleForClusterMonitoring;
    }

    /**
     * @param responsibleForClusterMonitoring <code>true</code> if this PC should take responsibility for monitoring
     *                                        cluster-wide properties; <code>false</code> if it's Someone Else's
     *                                        Problem.
     */
    public void setResponsibleForClusterMonitoring(boolean responsibleForClusterMonitoring) {
        this.responsibleForClusterMonitoring = responsibleForClusterMonitoring;
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
        if (responsibleForClusterMonitoring != that.responsibleForClusterMonitoring) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + (triggers != null ? triggers.hashCode() : 0);
        result = 31 * result + (notificationRules != null ? notificationRules.hashCode() : 0);
        result = 31 * result + (responsibleForClusterMonitoring ? 1 : 0);
        return result;
    }
}
