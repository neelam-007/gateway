/**
 * Copyright (C) 2008-2009 Layer 7 Technologies Inc.
 */
package com.l7tech.server.management.api.monitoring;

import com.l7tech.server.management.config.monitoring.MonitoringConfiguration;

import javax.jws.WebResult;
import javax.jws.WebService;
import javax.xml.bind.annotation.XmlSeeAlso;
import java.io.IOException;
import java.util.List;

/**
 * Published by the PC for use (primarily) by the EM
 *
 * @author alex
 */
@WebService
@XmlSeeAlso({MonitoredPropertyStatus.class, NotificationAttempt.class})
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
     * Uploads a new or updated Monitoring Configuration to the PC.
     *
     * @param config the new or updated Monitoring Scheme
     * @throws IOException if Monitoring Scheme could not be saved to disk
     */
    void pushMonitoringConfiguration(MonitoringConfiguration config) throws IOException;

    /**
     * Gets the status of all the properties being monitored by this PC at the present time.
     *
     * @return the list of MonitedPropertyStatus objects
     */
    @WebResult(name = "propertyStatuses")
    List<MonitoredPropertyStatus> getCurrentPropertyStatuses();

    /**
     * Gets a record of attempted notifications since the specified time.
     *
     * @param sinceWhen the time after which to return notifications.
     * @return the list of notification attempts
     */
    @WebResult(name = "notificationAttempts")
    List<NotificationAttempt> getRecentNotificationAttempts(long sinceWhen);
}
