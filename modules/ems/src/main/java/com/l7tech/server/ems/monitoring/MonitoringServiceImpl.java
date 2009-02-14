/*
 * Copyright (C) 2009 Layer 7 Technologies Inc.
 */
package com.l7tech.server.ems.monitoring;

import com.l7tech.server.ems.gateway.GatewayContextFactory;
import com.l7tech.server.ems.gateway.GatewayException;
import com.l7tech.server.ems.gateway.ProcessControllerContext;
import com.l7tech.server.ems.enterprise.JSONConstants;
import com.l7tech.server.ems.enterprise.SsgNode;
import com.l7tech.server.management.api.monitoring.*;
import com.l7tech.server.management.config.monitoring.ComponentType;
import com.l7tech.objectmodel.FindException;

import java.util.*;
import java.util.logging.Logger;

/**
 * @author: ghuang
 * @date: Feb 11, 2009
 */
public class MonitoringServiceImpl implements MonitoringService {
    private static final Logger logger = Logger.getLogger(MonitoringServiceImpl.class.getName());

    private GatewayContextFactory gatewayContextFactory;
    private EntityMonitoringPropertySetupManager entityMonitoringPropertySetupManager;

    // The next three variables used to keep tracking of SSG cluster property status
    private long latestSsgClusterMonitoringTimestamp;
    private Object latestSsgClusterMonitoredValue;
    private boolean latestSsgClusterMonitoringAlert;

    public MonitoringServiceImpl(GatewayContextFactory gatewayContextFactory, EntityMonitoringPropertySetupManager entityMonitoringPropertySetupManager) {
        this.gatewayContextFactory = gatewayContextFactory;
        this.entityMonitoringPropertySetupManager = entityMonitoringPropertySetupManager;
    }

    @Override
    public EntityMonitoringPropertyValues getCurrentSsgClusterPropertyStatus(String ssgClusterGuid) throws FindException {
        // Get the property setup of the SSG cluster from the database.
        EntityMonitoringPropertySetup ssgClusterPropertySetup =
            entityMonitoringPropertySetupManager.findByEntityGuidAndPropertyType(ssgClusterGuid, JSONConstants.SsgClusterMonitoringProperty.AUDIT_SIZE);
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
    public EntityMonitoringPropertyValues getCurrentSsgNodePropertiesStatus(SsgNode ssgNode) throws GatewayException {
        String ssgNodeGuid = ssgNode.getGuid();

        // Initialize the ssgNodePropertyValuesMap in which "monitored" and "unit" have been set yet.
        Map<String, Object> ssgNodePropertyValuesMap = initSsgNodePropertyValuesMap(ssgNodeGuid);

        // Create Monitoring API
        ProcessControllerContext pcContext = gatewayContextFactory.createProcessControllerContext(ssgNode);
        MonitoringApi monitoringApi = pcContext.getMonitoringApi();

        // Call MonitoringApi to get a list of property statuses to update the ssgNodePropertyValuesMap
        for (MonitoredPropertyStatus propertyStatus: monitoringApi.getCurrentPropertyStatuses()) {
            // Get "value"
            Object value = propertyStatus.getValue();

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
            if (propertyName.equals(BuiltinMonitorables.DISK_FREE_KIB.getName()) && value != null) {
                // Convert KB to GB, since the UI displays the disk free in GB.
                value = Long.valueOf(String.valueOf(value)) / SystemMonitoringSetupSettingsManager.KB_GB_CONVERTOR;
            } else if (propertyName.equals(BuiltinMonitorables.SWAP_FREE_KIB.getName()) && value != null) {
                // Convert KB to MB, since the UI displays the swap usage in MB.
                value = Long.valueOf(String.valueOf(value)) / SystemMonitoringSetupSettingsManager.KB_MB_CONVERTOR;
            }

            EntityMonitoringPropertyValues.PropertyValues propertyValues =
                (EntityMonitoringPropertyValues.PropertyValues) ssgNodePropertyValuesMap.get(propertyName);
            propertyValues.setValue(value == null ? null : String.valueOf(value));
            propertyValues.setAlert(alert);
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
