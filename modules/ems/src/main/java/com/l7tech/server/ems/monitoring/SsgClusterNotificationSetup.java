package com.l7tech.server.ems.monitoring;

import com.l7tech.objectmodel.imp.PersistentEntityImp;

import javax.persistence.*;
import javax.persistence.Entity;
import javax.persistence.Table;
import java.util.HashSet;
import java.util.Set;

import org.hibernate.annotations.Proxy;

/**
 * This class stores the notification setup for each SSG Cluster.
 *
 * @Copyright: Layer 7 Tech. Inc.
 * @Author: ghuang
 * @Date: Feb 6, 2009
 * @since Enterprise Manager 1.0
 */
@Entity
@Proxy(lazy=false)
@Table(name="ssgcluster_notification_setup")
public class SsgClusterNotificationSetup extends PersistentEntityImp {

    private String ssgClusterGuid;
    private Set<SystemMonitoringNotificationRule> systemNotificationRules = new HashSet<SystemMonitoringNotificationRule>();

    @Deprecated // For serialization and persistence only
    public SsgClusterNotificationSetup() {
    }

    public SsgClusterNotificationSetup(String ssgClusterGuid) {
        this.ssgClusterGuid = ssgClusterGuid;
    }

    public SsgClusterNotificationSetup(String ssgClusterGuid, Set<SystemMonitoringNotificationRule> systemNotificationRules) {
        this.ssgClusterGuid = ssgClusterGuid;
        this.systemNotificationRules = systemNotificationRules;
    }

    @Column(name="ssgcluster_guid", length=36, unique=true, nullable=false)
    public String getSsgClusterGuid() {
        return ssgClusterGuid;
    }

    public void setSsgClusterGuid(String ssgClusterGuid) {
        this.ssgClusterGuid = ssgClusterGuid;
    }

    @ManyToMany(cascade=CascadeType.ALL, fetch=FetchType.EAGER)
    public Set<SystemMonitoringNotificationRule> getSystemNotificationRules() {
        return systemNotificationRules;
    }

    public void setSystemNotificationRules(Set<SystemMonitoringNotificationRule> systemNotificationRules) {
        this.systemNotificationRules = systemNotificationRules;
    }
}
