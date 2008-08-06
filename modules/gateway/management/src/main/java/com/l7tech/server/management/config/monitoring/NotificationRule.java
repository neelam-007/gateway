/**
 * Copyright (C) 2008 Layer 7 Technologies Inc.
 */
package com.l7tech.server.management.config.monitoring;

import com.l7tech.objectmodel.imp.NamedEntityImp;

import javax.persistence.Entity;
import javax.persistence.ManyToOne;
import javax.persistence.CascadeType;

/** @author alex */
@Entity
public abstract class NotificationRule extends NamedEntityImp {
    private MonitoringScheme monitoringScheme;

    @ManyToOne(cascade=CascadeType.ALL)
    public MonitoringScheme getMonitoringScheme() {
        return monitoringScheme;
    }

    public void setMonitoringScheme(MonitoringScheme monitoringScheme) {
        this.monitoringScheme = monitoringScheme;
    }
}
