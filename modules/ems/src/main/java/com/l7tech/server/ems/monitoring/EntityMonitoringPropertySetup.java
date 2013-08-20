package com.l7tech.server.ems.monitoring;

import com.l7tech.objectmodel.NamedEntity;
import com.l7tech.objectmodel.imp.GoidEntityImp;
import com.l7tech.server.ems.enterprise.JSONConstants;
import com.l7tech.server.ems.enterprise.SsgCluster;
import com.l7tech.server.ems.enterprise.SsgNode;
import org.hibernate.annotations.Proxy;
import org.mortbay.util.ajax.JSON;

import javax.persistence.*;
import java.util.HashMap;
import java.util.Map;

/**
 * The class stores the property setup settings of an entity such as SSG Cluster or SSG Node.
 *
 * @Copyright: Layer 7 Tech. Inc.
 * @Author: ghuang
 * @Date: Jan 28, 2009
 * @since Enterprise Manager 1.0
 */
@Entity
@Proxy(lazy=false)
@Table(name="entity_monitoring_property_setup")
public class EntityMonitoringPropertySetup extends GoidEntityImp implements JSON.Convertible {
    private NamedEntity entity;
    private String entityGuid;
    private String propertyType;
    private boolean monitoringEnabled;
    private boolean triggerEnabled;
    private Long triggerValue;
    private String unit;
    private boolean notificationEnabled;
    private SsgClusterNotificationSetup ssgClusterNotificationSetup;

    @Deprecated // For serialization and persistence only
    public EntityMonitoringPropertySetup() {
    }

    public EntityMonitoringPropertySetup(NamedEntity entity, String propertyType) {
        this.propertyType = propertyType;
        this.entity = entity;

        if (entity instanceof SsgCluster) {
            entityGuid = ((SsgCluster)entity).getGuid();
        } else if (entity instanceof SsgNode) {
            entityGuid = ((SsgNode)entity).getGuid();
        }
    }

    public void setEntity(NamedEntity entity) {
        this.entity = entity;
    }

    @Column(name="entity_guid", length=36, nullable=false)
    public String getEntityGuid() {
        return entityGuid;
    }

    public void setEntityGuid(String entityGuid) {
        this.entityGuid = entityGuid;
    }
    
    @Column(name="property_type", length=32, nullable=false)
    public String getPropertyType() {
        return propertyType;
    }

    public void setPropertyType(String propertyType) {
        this.propertyType = propertyType;
    }

    @Column(name="monitoring_enabled")
    public boolean isMonitoringEnabled() {
        return monitoringEnabled;
    }

    public void setMonitoringEnabled(boolean monitoringEnabled) {
        this.monitoringEnabled = monitoringEnabled;
    }

    @Column(name="notification_enabled")
    public boolean isNotificationEnabled() {
        return notificationEnabled;
    }

    public void setNotificationEnabled(boolean notificationEnabled) {
        this.notificationEnabled = notificationEnabled;
    }

    @Column(name="trigger_enabled")
    public boolean isTriggerEnabled() {
        return triggerEnabled;
    }

    public void setTriggerEnabled(boolean triggerEnabled) {
        this.triggerEnabled = triggerEnabled;
    }

    @Column(name="trigger_value")
    public Long getTriggerValue() {
        return triggerValue;
    }

    public void setTriggerValue(Long triggerValue) {
        this.triggerValue = triggerValue;
    }

    @Column(name="unit")
    public String getUnit() {
        return unit;
    }

    public void setUnit(String unit) {
        this.unit = unit;
    }

    @ManyToOne(optional=false)
    @JoinColumn(name="ssgcluster_notification_setup_oid", nullable=false)
    public SsgClusterNotificationSetup getSsgClusterNotificationSetup() {
        return ssgClusterNotificationSetup;
    }

    public void setSsgClusterNotificationSetup(SsgClusterNotificationSetup ssgClusterNotificationSetup) {
        this.ssgClusterNotificationSetup = ssgClusterNotificationSetup;
    }

    @Override
    public void toJSON(JSON.Output output) {
        output.add(JSONConstants.ENTITY_PROPS_SETUP.ENTITY, getEntityProps());
        output.add(JSONConstants.ENTITY_PROPS_SETUP.PROP_TYPE, propertyType);
        output.add(JSONConstants.ENTITY_PROPS_SETUP.MONITORING_ENABLED, monitoringEnabled);
        output.add(JSONConstants.ENTITY_PROPS_SETUP.TRIGGER_ENABLED, triggerEnabled);
        output.add(JSONConstants.ENTITY_PROPS_SETUP.TRIGGER_VALUE, triggerValue);
        output.add(JSONConstants.ENTITY_PROPS_SETUP.UNIT, unit);
        output.add(JSONConstants.ENTITY_PROPS_SETUP.NOTIFICATION_ENABLED, notificationEnabled);
        output.add(JSONConstants.ENTITY_PROPS_SETUP.NOTIFICATION_RULES, ssgClusterNotificationSetup.getSystemNotificationRules());
    }

    @Transient
    public boolean isSsgNode() {
        return entity instanceof SsgNode;
    }

    @Transient
    public boolean isSsgCluster() {
        return entity instanceof SsgCluster;
    }

    @Transient
    private HashMap<String, Object> getEntityProps() {
        HashMap<String, Object> entityProps = new HashMap<String, Object>();
        if (isSsgCluster()) {
            SsgCluster ssgCluster = (SsgCluster) entity;
            entityProps.put(JSONConstants.ID, ssgCluster.getGuid());
            entityProps.put(JSONConstants.TYPE, JSONConstants.EntityType.SSG_CLUSTER);
            entityProps.put(JSONConstants.NAME, ssgCluster.getName());
            entityProps.put(JSONConstants.ANCESTORS, ssgCluster.ancestors());
        } else if (isSsgNode()) {
            SsgNode ssgNode = (SsgNode) entity;
            entityProps.put(JSONConstants.ID, ssgNode.getGuid());
            entityProps.put(JSONConstants.TYPE, JSONConstants.EntityType.SSG_NODE);
            entityProps.put(JSONConstants.NAME, ssgNode.getName());
            entityProps.put(JSONConstants.ANCESTORS, ssgNode.ancestors());
        }
        return entityProps;
    }

    @Override
    public void fromJSON(Map map) {
        throw new UnsupportedOperationException("Mapping from JSON not supported.");
    }
}
