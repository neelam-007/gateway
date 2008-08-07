/**
 * Copyright (C) 2008 Layer 7 Technologies Inc.
 */
package com.l7tech.server.management.api.monitoring;

import com.l7tech.objectmodel.SaveException;
import com.l7tech.server.management.config.monitoring.MonitoringScheme;

import java.io.IOException;

/** @author alex */
public interface MonitoringApi {
    /**
     * Gets the status of the named Node; never null.
     *
     * @param nodeName the name of the Node to retrieve the status for
     * @return the status of the specified Node
     * @throws IOException if the Node's status cannot be retrieved
     */
    NodeStatus getNodeStatus(String nodeName) throws IOException;

    /**
     * Gets the status of the Host on which the receiving PC is running; never null.
     *
     * @return the status of the current Host
     * @throws IOException if the Host's status cannot be retrieved
     */
    HostStatus getHostStatus() throws IOException;

    /**
     * Uploads a Monitoring Scheme to be stored in the database of the specified Node.
     * 
     * @param nodeName the name of the Node in whose database the Monitoring Scheme should be saved
     * @param scheme the new or updated Monitoring Scheme
     * @throws SaveException if the Node is unable or unwilling to save the Monitoring Scheme
     * @throws IOException if Monitoring Scheme could not be transferred to the specified Node
     */
    void pushMonitoringScheme(String nodeName, MonitoringScheme scheme) throws IOException, SaveException;
}
