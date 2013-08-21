/**
 * Copyright (C) 2008-2009 Layer 7 Technologies Inc.
 */
package com.l7tech.server.management.config.monitoring;

import com.l7tech.objectmodel.Goid;
import com.l7tech.objectmodel.imp.NamedEntityImp;

import javax.persistence.Entity;

/**
 * A configuration for a type of notification that can be sent by the monitoring system.
 *  
 * @author alex
 */
@Entity
public abstract class NotificationRule extends NamedEntityImp {
    private Type type;

    @Deprecated
    protected NotificationRule() { }

    protected NotificationRule(Type type) {
        this.type = type;
    }

    public Type getType() {
        return type;
    }

    public boolean isIncompatibleWith(NotificationRule that) {
        return this.type != that.type ||
               !Goid.equals(this.getGoid(), that.getGoid());
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

    @Override
    public String toString() {
        return "NotificationRule{" +
                "type=" + type +
                "} " + super.toString();
    }
}
