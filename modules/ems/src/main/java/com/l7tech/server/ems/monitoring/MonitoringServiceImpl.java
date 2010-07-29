/*
 * Copyright (C) 2009 Layer 7 Technologies Inc.
 */
package com.l7tech.server.ems.monitoring;

import com.l7tech.objectmodel.FindException;
import com.l7tech.server.ems.enterprise.JSONConstants;
import com.l7tech.server.ems.enterprise.SsgNode;
import com.l7tech.server.ems.gateway.GatewayContextFactory;
import com.l7tech.server.ems.gateway.GatewayException;
import com.l7tech.server.ems.gateway.ProcessControllerContext;
import com.l7tech.server.management.api.monitoring.BuiltinMonitorables;
import com.l7tech.server.management.api.monitoring.MonitoredPropertyStatus;
import com.l7tech.server.management.api.monitoring.MonitoredStatus;
import com.l7tech.server.management.api.monitoring.MonitoringApi;
import com.l7tech.server.management.config.monitoring.ComponentType;
import com.l7tech.util.ExceptionUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
import java.util.logging.Level;
import java.io.IOException;

/**
 * The implementation of Monitoring Service Interface.
 */
public class MonitoringServiceImpl implements MonitoringService {
    private static final Logger logger = Logger.getLogger(MonitoringServiceImpl.class.getName());

    private GatewayContextFactory gatewayContextFactory;
    private EntityMonitoringPropertySetupManager entityMonitoringPropertySetupManager;

    // Keep tracking if the warning message for properties statuses unavailable has been logged or not.
    private boolean statusesUnavailableWarningMsgLogged;
    // The next three variables used to keep tracking of SSG cluster property status
    private long latestSsgClusterMonitoringTimestamp;
    private Object latestSsgClusterMonitoredValue;
    private boolean latestSsgClusterMonitoringAlert;

    public MonitoringServiceImpl(GatewayContextFactory gatewayContextFactory, EntityMonitoringPropertySetupManager entityMonitoringPropertySetupManager) {
        this.gatewayContextFactory = gatewayContextFactory;
        this.entityMonitoringPropertySetupManager = entityMonitoringPropertySetupManager;
    }

    @Override
    public EntityMonitoringPropertyValues getCurrentSsgClusterPropertyStatus(String ssgClusterGuid) {
        // Get the property setup of the SSG cluster from the database.
        EntityMonitoringPropertySetup ssgClusterPropertySetup;
        try {
            ssgClusterPropertySetup = entityMonitoringPropertySetupManager.findByEntityGuidAndPropertyType(ssgClusterGuid, JSONConstants.SsgClusterMonitoringProperty.AUDIT_SIZE);
        } catch (FindException e) {
            logger.warning("Cannot find the monitoring property setup of the Gateway cluster (GUID = '" + ssgClusterGuid + "').");
            return null;
        }
        if (ssgClusterPropertySetup == null) {
            return null;
        }
        // Get the current property status of the SSG cluster from the monitoring api.
        EntityMonitoringPropertyValues.PropertyValues currentPropertyValues =
            new EntityMonitoringPropertyValues.PropertyValues(
                ssgClusterPropertySetup.isMonitoringEnabled(),
                (latestSsgClusterMonitoredValue == null)? null : latestSsgClusterMonitoredValue.toString(),
                ssgClusterPropertySetup.getUnit(),
                latestSsgClusterMonitoringAlert
            );
        // Clean these lastest values
        latestSsgClusterMonitoringTimestamp = 0;
        latestSsgClusterMonitoredValue = null;
        latestSsgClusterMonitoringAlert = false;

        // Create an EntityMonitoringPropertyValues object in json-content format and return it to Monitor.
        Map<String, Object> ssgClusterPropertyValuesMap = new HashMap<String, Object>();
        ssgClusterPropertyValuesMap.put(JSONConstants.SsgClusterMonitoringProperty.AUDIT_SIZE, currentPropertyValues);
        return new EntityMonitoringPropertyValues(ssgClusterGuid, ssgClusterPropertyValuesMap);
    }

    @Override
    public EntityMonitoringPropertyValues getCurrentSsgNodePropertiesStatus(SsgNode ssgNode) {
        ProcessControllerContext pcContext;
        List<MonitoredPropertyStatus> statuses;

        // Initialize the ssgNodePropertyValuesMap in which "monitored" and "unit" have been set yet.
        String ssgNodeGuid = ssgNode.getGuid();
        Map<String, Object> ssgNodePropertyValuesMap = initSsgNodePropertyValuesMap(ssgNodeGuid);

        try {
            // Create Monitoring API
            pcContext = gatewayContextFactory.createProcessControllerContext(ssgNode);
            MonitoringApi monitoringApi = pcContext.getMonitoringApi();

            // Call MonitoringApi to get a list of property statuses to update the ssgNodePropertyValuesMap
            statuses = monitoringApi.getCurrentPropertyStatuses();

            // Reset this flag as false.  If property statuses are unavailable again, then the logger will work again.
            statusesUnavailableWarningMsgLogged = false;
         } catch (Throwable t) {
            if (! statusesUnavailableWarningMsgLogged) {
                if (t instanceof IOException || t instanceof GatewayException || t instanceof javax.xml.ws.ProtocolException) {
                    logger.log(Level.WARNING, "Current entity property statuses unavailable at this moment: " + ExceptionUtils.getMessage(t));
                } else {
                    logger.log(Level.WARNING, "Failed to retrieve current entity property statuses: " + ExceptionUtils.getMessage(t), ExceptionUtils.getDebugException(t));
                }
                statusesUnavailableWarningMsgLogged = true;
            }
            return new EntityMonitoringPropertyValues(ssgNodeGuid, ssgNodePropertyValuesMap);
        }

        // Seems that Collections.emptyList() becomes null when transmitted through remote API.
        if (statuses != null) {
            for (MonitoredPropertyStatus propertyStatus: statuses) {
                // Get "value"
                Object value = propertyStatus.getValue();
                MonitoredPropertyStatus.ValueType valueType = propertyStatus.getValueType();

                // Get "alert"
                MonitoredStatus.StatusType status = propertyStatus.getStatus();
                boolean alert = false;
                if (status.equals(MonitoredStatus.StatusType.WARNING) || status.equals(MonitoredStatus.StatusType.NOTIFIED)) {
                    alert = true;
                }

                // Set "value" and "alert"
                // Note: "monitored" and "unit" have been set in the method initSsgNodePropertyValuesMap.
                ComponentType compTyep = propertyStatus.getType();

                // Case 1: SSG cluster property status
                if (compTyep.equals(ComponentType.CLUSTER)) {
                    if (latestSsgClusterMonitoringTimestamp < propertyStatus.getTimestamp()) {
                        latestSsgClusterMonitoringTimestamp = propertyStatus.getTimestamp();
                        latestSsgClusterMonitoredValue = value;
                        latestSsgClusterMonitoringAlert = alert;
                    }
                    continue;
                }

                // Case 2: SSG node property status
                String propertyName = propertyStatus.getMonitorableId();
                if (propertyName.equals(BuiltinMonitorables.LOG_SIZE.getName()) && value != null) {
                    // Convert KB to MB, since the UI displays the log size in MB.
                    value = Long.valueOf(String.valueOf(value)) / SystemMonitoringSetupSettingsManager.KB_MB_CONVERTOR;
                } else if (propertyName.equals(BuiltinMonitorables.DISK_FREE_KIB.getName()) && value != null) {
                    // Convert KB to GB, since the UI displays the disk free in GB.
                    value = Long.valueOf(String.valueOf(value)) / SystemMonitoringSetupSettingsManager.KB_GB_CONVERTOR;
                } else if (propertyName.equals(BuiltinMonitorables.SWAP_USAGE_KIB.getName()) && value != null) {
                    // Convert KB to MB, since the UI displays the swap usage in MB.
                    value = Long.valueOf(String.valueOf(value)) / SystemMonitoringSetupSettingsManager.KB_MB_CONVERTOR;
                }

                EntityMonitoringPropertyValues.PropertyValues propertyValues =
                    (EntityMonitoringPropertyValues.PropertyValues) ssgNodePropertyValuesMap.get(propertyName);
                if (value == null) {
                    if (valueType == MonitoredPropertyStatus.ValueType.NO_DATA_YET) {
                        propertyValues.setValue(JSONConstants.NA);
                    } else {
                        propertyValues.setValue(null);
                    }
                } else {
                    propertyValues.setValue(String.valueOf(value));
                }
                propertyValues.setAlert(alert);
            }
        }

        // After ssgNodePropertyValuesMap has been updated by using current property statuses, create an
        // EntityMonitoringPropertyValues object for the SSG node and return the object to Monitor.
        return new EntityMonitoringPropertyValues(ssgNodeGuid, ssgNodePropertyValuesMap);
    }

    /**
     * Get a map of properties values for a SSG node.  In this method, "monitored" and "unit" have been loaded into the map.
     * <key, value> = <String: propertyType, PropertyValues: propertyValues>
     * @param ssgNodeGuid: the GUID of a SSG node.
     * @return a map of properties values
     */
    private Map<String, Object> initSsgNodePropertyValuesMap(String ssgNodeGuid) {
        Map<String, Object> valuesMap = new HashMap<String, Object>();
        valuesMap.put(JSONConstants.SsgNodeMonitoringProperty.OPERATING_STATUS, new EntityMonitoringPropertyValues.PropertyValues());
        valuesMap.put(JSONConstants.SsgNodeMonitoringProperty.LOG_SIZE,         new EntityMonitoringPropertyValues.PropertyValues());
        valuesMap.put(JSONConstants.SsgNodeMonitoringProperty.DISK_USAGE,       new EntityMonitoringPropertyValues.PropertyValues());
        valuesMap.put(JSONConstants.SsgNodeMonitoringProperty.DISK_FREE,        new EntityMonitoringPropertyValues.PropertyValues());
        valuesMap.put(JSONConstants.SsgNodeMonitoringProperty.RAID_STATUS,      new EntityMonitoringPropertyValues.PropertyValues());
        valuesMap.put(JSONConstants.SsgNodeMonitoringProperty.CPU_TEMP,         new EntityMonitoringPropertyValues.PropertyValues());
        valuesMap.put(JSONConstants.SsgNodeMonitoringProperty.CPU_USAGE,        new EntityMonitoringPropertyValues.PropertyValues());
        valuesMap.put(JSONConstants.SsgNodeMonitoringProperty.SWAP_USAGE,       new EntityMonitoringPropertyValues.PropertyValues());
        valuesMap.put(JSONConstants.SsgNodeMonitoringProperty.NTP_STATUS,       new EntityMonitoringPropertyValues.PropertyValues());

        try {
            for (EntityMonitoringPropertySetup ssgNodePropertySetup: entityMonitoringPropertySetupManager.findByEntityGuid(ssgNodeGuid)) {
                String propertyType = ssgNodePropertySetup.getPropertyType();
                EntityMonitoringPropertyValues.PropertyValues propertyValues = (EntityMonitoringPropertyValues.PropertyValues) valuesMap.get(propertyType);
                propertyValues.setMonitored(ssgNodePropertySetup.isMonitoringEnabled());
                propertyValues.setUnit(ssgNodePropertySetup.getUnit());
            }
        } catch (FindException e) {
            logger.warning(e.getMessage());
        }

        return valuesMap;
    }
}
