/**
 * Copyright (C) 2008-2009 Layer 7 Technologies Inc.
 */
package com.l7tech.server.management.config.monitoring;

import com.l7tech.objectmodel.imp.NamedEntityImp;

import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.ManyToOne;

/**
 * A configuration for a type of notification that can be sent by the monitoring system.
 *  
 * @author alex
 */
@Entity
public abstract class NotificationRule extends NamedEntityImp {
    protected MonitoringConfiguration monitoringConfiguration;
    private final Type type;

    protected NotificationRule(MonitoringConfiguration configuration, Type type) {
        this.monitoringConfiguration = configuration;
        this.type = type;
    }

    protected NotificationRule(Type type) {
        this.type = type;
    }

    @ManyToOne(cascade=CascadeType.ALL)
    public MonitoringConfiguration getMonitoringScheme() {
        return monitoringConfiguration;
    }

    public void setMonitoringScheme(MonitoringConfiguration monitoringConfiguration) {
        this.monitoringConfiguration = monitoringConfiguration;
    }

    public Type getType() {
        return type;
    }

    public boolean isIncompatibleWith(NotificationRule that) {
        return !this.monitoringConfiguration.equals(that.monitoringConfiguration) ||
                this.type != that.type || 
                this._oid != that._oid;
    }

    public enum Type {
        EMAIL("E-Mail"),
        SNMP("SNMP Trap"),
        HTTP("HTTP Request");

        private final String name;

        private Type(String name) {
            this.name = name;
        }

        public String getName() {
            return name;
        }
    }

    @SuppressWarnings({"RedundantIfStatement"})
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;

        NotificationRule that = (NotificationRule) o;

        if (type != that.type) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + (type != null ? type.hashCode() : 0);
        return result;
    }
}
