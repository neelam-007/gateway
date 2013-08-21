/**
 * Copyright (C) 2008-2009 Layer 7 Technologies Inc.
 */
package com.l7tech.server.management.config.monitoring;

import com.l7tech.objectmodel.Goid;
import com.l7tech.objectmodel.imp.NamedEntityImp;
import com.l7tech.server.management.api.monitoring.Monitorable;

import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.ManyToMany;
import javax.xml.bind.annotation.*;
import java.util.ArrayList;
import java.util.List;

/**
 * A Trigger describes an event or property that the monitoring system should monitor.  If the trigger "fires," its
 * associated list of {@link #notificationRules} will be used to send notifications.
 * @author alex
 */
@Entity
@XmlRootElement
@XmlSeeAlso({PropertyTrigger.class, EventTrigger.class})
public abstract class Trigger<MT extends Monitorable> extends NamedEntityImp {
    /** The type of the subject component */
    protected ComponentType componentType;

    /** The unique ID of the subject component (usually a URI or GUID) */
    private String componentId;

    /** The name of the monitored property/event */
    protected String monitorableId;

    protected volatile MT monitorable;

    /** The notification rules that should be invoked when this trigger fires */
    private List<NotificationRule> notificationRules = new ArrayList<NotificationRule>();

    @Deprecated
    public Trigger() {
    }

    protected Trigger(ComponentType type, String componentId, String monitorableId) {
        this.componentId = componentId;
        this.componentType = type;
        this.monitorableId = monitorableId;
    }

    public MT getMonitorable() {
        final MT my = monitorable;
        if (my != null) return my;

        return monitorable = buildMonitorable();
    }

    protected abstract MT buildMonitorable();

    public ComponentType getComponentType() {
        return componentType;
    }

    public void setComponentType(ComponentType componentType) {
        this.componentType = componentType;
        this.monitorable = null;
    }

    public String getComponentId() {
        return componentId;
    }

    public void setComponentId(String componentId) {
        this.componentId = componentId;
    }

    @ManyToMany(cascade={CascadeType.MERGE, CascadeType.PERSIST, CascadeType.REFRESH})
    @XmlElementWrapper(name="notificationRules")
    @XmlElement(name="ruleId")
    @XmlIDREF()
    public List<NotificationRule> getNotificationRules() {
        return notificationRules;
    }

    public void setNotificationRules(List<NotificationRule> notificationRules) {
        this.notificationRules = notificationRules;
    }

    public String getMonitorableId() {
        return monitorableId;
    }

    public void setMonitorableId(String monitorableId) {
        this.monitorableId = monitorableId;
        this.monitorable = null;
    }

    /**
     * Determines whether another trigger is a suitable replacement for this one.  If any of the following properties
     * of the other trigger are different from those of this, they are <em>incompatible</em>:
     * <ul>
     * <li>the class or {@link #getGoid() GOID} </li>
     * <li>the {@link #monitorableId}</li>
     * <li>the {@link #componentType type} or {@link #componentId ID} of the subject component</li>
     * </ul>
     *
     * Note that this is intentionally much weaker than {@link #equals}--we want successively updated versions of the same
     * entity to be considered "equal enough" that one can replace another without necessatiating the invalidation of
     * historical values.
     */
    public boolean isIncompatibleWith(Trigger that) {
        return that.getClass() != this.getClass() ||
               !Goid.equals(that.getGoid(), this.getGoid()) ||
               that.getComponentType() != this.getComponentType() ||
               !that.getComponentId().equals(this.getComponentId()) ||
               !that.getMonitorableId().equals(this.getMonitorableId());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;

        Trigger trigger = (Trigger) o;

        if (componentId != null ? !componentId.equals(trigger.componentId) : trigger.componentId != null) return false;
        if (componentType != trigger.componentType) return false;
        if (monitorableId != null ? !monitorableId.equals(trigger.monitorableId) : trigger.monitorableId != null)
            return false;
        if (notificationRules != null ? !notificationRules.equals(trigger.notificationRules) : trigger.notificationRules != null)
            return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + (componentType != null ? componentType.hashCode() : 0);
        result = 31 * result + (componentId != null ? componentId.hashCode() : 0);
        result = 31 * result + (monitorableId != null ? monitorableId.hashCode() : 0);
        result = 31 * result + (notificationRules != null ? notificationRules.hashCode() : 0);
        return result;
    }
}
