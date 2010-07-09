/**
 * Copyright (C) 2008-2009 Layer 7 Technologies Inc.
 */
package com.l7tech.server.management.api.node;

import com.l7tech.objectmodel.FindException;
import com.l7tech.server.management.api.monitoring.NodeStatus;

import javax.jws.WebParam;
import javax.jws.WebResult;
import javax.jws.WebService;
import java.text.MessageFormat;

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
     * Gets the value of the Node property with the specified ID.
     *
     * @param propertyId the ID of the property to retrieve
     * @return the value of the property with the specified ID
     * @throws UnsupportedPropertyException if the specified property ID isn't supported by the Node
     * @throws FindException if the property cannot be retrieved for some reason
     */
    @WebResult(name="propertyValue")
    String getProperty(@WebParam(name="propertyId")String propertyId) throws UnsupportedPropertyException, FindException;

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
