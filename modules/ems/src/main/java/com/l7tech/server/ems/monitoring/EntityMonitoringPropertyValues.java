package com.l7tech.server.ems.monitoring;

import org.mortbay.util.ajax.JSON;

import java.util.Map;
import java.util.HashMap;

import com.l7tech.server.ems.enterprise.JSONConstants;

/**
 * @Copyright: Layer 7 Tech. Inc.
 * @Author: ghuang
 * @Date: Jan 28, 2009
 * @since Enterprise Manager 1.0
 */
public class EntityMonitoringPropertyValues implements JSON.Convertible {

    public static enum EntityType {
        SSG_CLUSTER("ssgCluster"), SSG_NODE("ssgNode");

        private final String name;

        private EntityType(String name) {
            this.name = name;
        }

        public String getName() {
            return name;
        }
    }

    private String entityGuid;
    private EntityType entityType;
    /**
     * The structure of propsMap:
     * Key   -  String: the property name, e.g., auditSize, logSize, ntpStatus, etc.
     * Value -  Object: the corresponding PropertyValues object containing four attributes, monitored, value, unit, and alert.
     */
    private Map<String, Object> propsMap = new HashMap<String, Object>();

    @Deprecated // For serialization and persistence only
    public EntityMonitoringPropertyValues() {
    }

    public EntityMonitoringPropertyValues(String entityGuid, Map<String, Object> propsMap) {
        this.entityGuid = entityGuid;
        this.propsMap = propsMap;
    }

    public String getEntityGuid() {
        return entityGuid;
    }

    public void setEntityGuid(String entityGuid) {
        this.entityGuid = entityGuid;
    }

    public EntityType getEntityType() {
        return entityType;
    }

    public void setEntityType(EntityType entityType) {
        this.entityType = entityType;
    }

    public Map<String, Object> getPropsMap() {
        return propsMap;
    }

    public void setPropsMap(Map<String, Object> propsMap) {
        this.propsMap = propsMap;
    }

    public Object getPropertyValues(String name) {
        return propsMap.get(name);
    }

    protected void setPropertyValues(String name, Object propertyValues) {
        propsMap.put(name, propertyValues);
    }

    @Override
    public void toJSON(JSON.Output output) {
        output.add(JSONConstants.ID, entityGuid);
        output.add(JSONConstants.MONITORING_PROPERTIES, getPropsMap());
    }

    @Override
    public void fromJSON(Map map) {
        throw new UnsupportedOperationException("Mapping from JSON not supported.");
    }

    public final static class PropertyValues implements JSON.Convertible {
        private boolean monitored;
        private String value;
        private String unit;
        private boolean alert;

        public PropertyValues() {
        }

        public PropertyValues(boolean monitored, String value, String unit, boolean alert) {
            this.monitored = monitored;
            this.value = value;
            this.unit = unit;
            this.alert = alert;
        }

        public boolean isAlert() {
            return alert;
        }

        public void setAlert(boolean alert) {
            this.alert = alert;
        }

        public boolean isMonitored() {
            return monitored;
        }

        public void setMonitored(boolean monitored) {
            this.monitored = monitored;
        }

        public String getUnit() {
            return unit;
        }

        public void setUnit(String unit) {
            this.unit = unit;
        }

        public String getValue() {
            return value;
        }

        public void setValue(String value) {
            this.value = value;
        }

        @Override
        public void toJSON(JSON.Output output) {
            output.add(JSONConstants.MONITORED, monitored);

            if (monitored && value != null) {
                output.add(JSONConstants.VALUE, value);
                if (unit != null) {
                    output.add(JSONConstants.UNIT, unit);
                }
                output.add(JSONConstants.ALERT, alert);
            }
        }

        @Override
        public void fromJSON(Map map) {
            throw new UnsupportedOperationException("Mapping from JSON not supported.");
        }
    }
}
