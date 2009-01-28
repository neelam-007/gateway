/**
 * Copyright (C) 2008 Layer 7 Technologies Inc.
 */
package com.l7tech.server.management.config.monitoring;

import com.l7tech.objectmodel.imp.NamedEntityImp;

import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.ManyToMany;
import javax.persistence.ManyToOne;
import java.util.ArrayList;
import java.util.List;

/**
 * A Trigger describes an event or property that the monitoring system should monitor.  If the trigger "fires," its
 * associated list of {@link #notificationRules} will be used to send notifications.
 * @author alex
 */
@Entity
public abstract class Trigger extends NamedEntityImp {
    /** The parent monitoring scheme that owns this trigger */
    private MonitoringConfiguration monitoringConfiguration;

    /** The type of the subject component */
    private ComponentType componentType;

    /** The unique ID of the subject component (usually a URI or GUID) */
    private String componentId;

    /** The notification rules that should be invoked when this trigger fires */
    private List<NotificationRule> notificationRules = new ArrayList<NotificationRule>();

    @ManyToOne(cascade=CascadeType.ALL)
    public MonitoringConfiguration getMonitoringScheme() {
        return monitoringConfiguration;
    }

    public void setMonitoringScheme(MonitoringConfiguration monitoringConfiguration) {
        this.monitoringConfiguration = monitoringConfiguration;
    }

    public ComponentType getComponentType() {
        return componentType;
    }

    public void setComponentType(ComponentType componentType) {
        this.componentType = componentType;
    }

    public String getComponentId() {
        return componentId;
    }

    public void setComponentId(String componentId) {
        this.componentId = componentId;
    }

    @ManyToMany(cascade={CascadeType.MERGE, CascadeType.PERSIST, CascadeType.REFRESH})
    public List<NotificationRule> getNotificationRules() {
        return notificationRules;
    }

    public void setNotificationRules(List<NotificationRule> notificationRules) {
        this.notificationRules = notificationRules;
    }

    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;

        Trigger trigger = (Trigger)o;

        if (componentId != null ? !componentId.equals(trigger.componentId) : trigger.componentId != null) return false;
        if (componentType != trigger.componentType) return false;

        return true;
    }

    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + (componentType != null ? componentType.hashCode() : 0);
        result = 31 * result + (componentId != null ? componentId.hashCode() : 0);
        result = 31 * result + (notificationRules != null ? notificationRules.hashCode() : 0);
        return result;
    }

    /**
     * Determines whether another trigger is a suitable replacement for this one.  If any of the following properties 
     * of the other trigger are different from those of this, they are <em>incompatible</em>:
     * <ul>
     * <li>the parent {@link #monitoringConfiguration monitoring scheme}</li>
     * <li>the {@link #componentType type} or {@link #componentId ID} of the subject component</li>
     * </ul>
     */
    public boolean isIncompatibleWith(PropertyTrigger that) {
        return that.getOid() != this.getOid() ||
               that.getComponentType() != this.getComponentType() ||
               !that.getComponentId().equals(this.getComponentId()) ||
               !that.getMonitoringScheme().equals(this.getMonitoringScheme());
    }
}
