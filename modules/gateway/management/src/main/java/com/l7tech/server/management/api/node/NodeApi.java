/**
 * Copyright (C) 2008 Layer 7 Technologies Inc.
 */
package com.l7tech.server.management.api.node;

import com.l7tech.objectmodel.DeleteException;
import com.l7tech.objectmodel.FindException;
import com.l7tech.objectmodel.SaveException;
import com.l7tech.objectmodel.UpdateException;
import com.l7tech.server.management.api.monitoring.NodeStatus;
import com.l7tech.server.management.config.monitoring.MonitoringScheme;

import javax.jws.WebParam;
import javax.jws.WebResult;
import javax.jws.WebService;
import java.text.MessageFormat;
import java.util.Set;

/**
 * The API published by a Node for use by the Process Controller
 * 
 * @author alex
 */
@WebService(name="NodeApi", targetNamespace = "http://ns.l7tech.com/secureSpan/5.0/component/clusterNode/nodeApi")
public interface NodeApi {
    String NODE_NOT_CONFIGURED_FOR_PC = "This node has not been configured for use by the Process Controller";

    /** Tells the node to shut itself down */
    void shutdown();

    /** Called periodically by the PC to ensure that the SN is still alive */
    void ping();

    /**
     * Gets the SN's current health status
     *
     * @return the SN's current health status; never null but might be some form of "unknown"
     */
    @WebResult(name="status")
    NodeStatus getNodeStatus();

    /**
     * Pushes a new monitoring scheme for the SN to write to its database.
     *
     * @param scheme a monitoring scheme to save in the SN's database; must not be null.
     * @throws SaveException if the monitoring scheme cannot be updated
     */
    void pushMonitoringScheme(@WebParam(name="scheme")MonitoringScheme scheme) throws UpdateException;

    /**
     * Gets the current MonitoringScheme from the SN's database.
     *
     * @return the MonitoringScheme from the SN's database; may be the default scheme but never null.
     * @throws FindException if the current monitoring scheme cannot be found
     */
    @WebResult(name="scheme")
    MonitoringScheme getMonitoringScheme() throws FindException;

    /**
     * Asks the SN to register subscriptions for events with the provided IDs
     *
     * @param eventIds the IDs of the events to subscribe to; must not be null or empty.
     * @return a set of any successful subscriptions; never null
     * @throws UnsupportedEventException if any of the event IDs is unrecognized by the SN
     * @throws SaveException if any of the subscriptions cannot be saved for some reason 
     */
    @WebResult(name="subscriptions")
    Set<EventSubscription> subscribeEvents(@WebParam(name="eventIds") Set<String> eventIds) throws UnsupportedEventException,
        SaveException;

    /**
     * Asks the SN to renew the subscriptions with the specified IDs
     *
     * @param subscriptionIds the IDs of the subscriptions (not events!) that the PC would like to renew
     * @return the set of subscriptions with the provided IDs; some or all may have a new expiry time
     * @throws UpdateException if the subscriptions cannot be renewed for some reason
     */
    @WebResult(name="subscriptions")
    Set<EventSubscription> renewEventSubscriptions(@WebParam(name="subscriptionIds") Set<String> subscriptionIds) throws UpdateException;

    /**
     * Asks the Node to release the subscriptions with the specified IDs.  Unrecognized IDs will be silently ignored.
     * @param subscriptionIds the {@link EventSubscription#eventId ids} of the subscriptions to release; must not be null.
     * @throws DeleteException if the subscriptions cannot be released
     */
    void releaseEventSubscriptions(@WebParam(name="subscriptionIds") Set<String> subscriptionIds) throws DeleteException;

    /**
     * Gets the value of the Node property with the specified ID.
     *
     * @param propertyId the ID of the property to retrieve
     * @return the value of the property with the specified ID
     * @throws UnsupportedPropertyException if the specified property ID isn't supported by the Node
     * @throws FindException if the property cannot be retrieved for some reason
     */
    @WebResult(name="propertyValue")
    Object getProperty(@WebParam(name="propertyId")String propertyId) throws UnsupportedPropertyException, FindException;

    public class UnsupportedEventException extends Exception {
        private final String eventId;

        public UnsupportedEventException(final String eventId) {
            super(MessageFormat.format("Unsuported Event: {0}", eventId));
            this.eventId = eventId;
        }

        public String getEventId() {
            return eventId;
        }
    }

    public class UnsupportedPropertyException extends Exception {
        private final String propertyId;

        public UnsupportedPropertyException(final String propertyId) {
            super(MessageFormat.format("Unsuported Property: {0}", propertyId));
            this.propertyId = propertyId;
        }

        public String getPropertyId() {
            return propertyId;
        }
    }
}
