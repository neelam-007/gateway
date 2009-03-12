package com.l7tech.server.ems.monitoring;

import com.l7tech.server.ems.enterprise.SsgNode;

/**
 * @author: ghuang
 * @date: Feb 11, 2009
 */
public interface MonitoringService {
    /**
     * Get the current property value for a SSG cluster by calling MonitoringApi
     * @param ssgClusterGuid: the GUID of a SSG cluster
     * @return a json content format of current SSG cluster property value.
     */
    EntityMonitoringPropertyValues getCurrentSsgClusterPropertyStatus(String ssgClusterGuid);

    /**
     * Get the current property values for a SSG node by calling MonitoringApi
     * @param ssgNode: the SSG Node entity used to get the host IP Address and the GUID.
     * @return a json content format of current SSG node property values.
     */
    EntityMonitoringPropertyValues getCurrentSsgNodePropertiesStatus(SsgNode ssgNode);
}
