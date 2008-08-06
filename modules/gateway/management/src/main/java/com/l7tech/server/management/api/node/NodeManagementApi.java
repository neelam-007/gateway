/**
 * Copyright (C) 2008 Layer 7 Technologies Inc.
 */
package com.l7tech.server.management.api.node;

import com.l7tech.objectmodel.DeleteException;
import com.l7tech.objectmodel.FindException;
import com.l7tech.objectmodel.SaveException;
import com.l7tech.objectmodel.UpdateException;
import com.l7tech.server.management.config.node.DatabaseConfig;
import com.l7tech.server.management.config.node.DatabaseType;
import com.l7tech.server.management.config.node.ServiceNodeConfig;
import com.l7tech.util.Triple;

import javax.activation.DataHandler;
import java.io.IOException;
import java.text.MessageFormat;
import java.util.Map;
import java.util.Set;

/**
 * Part of the API invoked on the Process Controller by Enterprise Manager Servers.
 * @author alex
 */
public interface NodeManagementApi {
    /**
     * Creates a new {@link ServiceNodeConfig} with reasonable default parameters for the current gateway.
     *
     * TODO should this method save the node, or just return a transient instance?  I guess it depends on whether the defaults are sufficient to specify a fully functional node.
     *
     * TODO consider adding enough parameters to this method such that the resulting node is minimally specified
     *
     * @param newNodeName the name of the new node; must be unique within this gateway
     * @param version the software version that the ServiceNode should be initialized for
     * @param databaseConfigMap the database configuration to use for the new node.  If null is passed, the PC will
     *                          attempt to create new, standalone database(s) with the default {@link DatabaseType type(s)}
     *                          and {@link DatabaseConfig.Vendor vendor(s)}
     *                          on the default database server(s) with the default username(s) and randomly generated
     *                          password(s).  If the PC is unable to create the databases (e.g. because it doesn't have
     *                          sufficient credentials to do so) a {@link SaveException} will be thrown.
     *
     * @return the newly created ServiceNodeConfig
     * @throws SaveException if the new node cannot be created
     */
    ServiceNodeConfig createNode(String newNodeName, String version, Map<DatabaseType, DatabaseConfig> databaseConfigMap) throws SaveException;

    /**
     * Retrieves the {@link ServiceNodeConfig} with the provided name, or null if no such node exists on this gateway.
     *
     * @param nodeName the name of the node to retrieve
     * @return the ServiceNodeConfig for the node with the specified name
     * @throws FindException if for some reason the PC is unable to determine the (non-)existence of a node with the
     *                       specified name (e.g. the PC's configuration is corrupted or temporarily unavailable)
     */
    ServiceNodeConfig getNode(String nodeName) throws FindException;

    /**
     * Lists the names and versions of the Service Nodes on this gateway.  The resulting Set may be empty, indicating
     * that no nodes have been configured yet.
     *
     * @return a Set of Triples, each consisting of the name, version and enablement status of a Service Node on this
     *         gateway.
     * @throws FindException if the list of Service Nodes cannot be retrieved
     */
    Set<Triple<String, String, Boolean>> listNodes() throws FindException;

    /**
     * Updates a node's configuration.  If the node is already running when this method is invoked, a
     * {@link RestartRequiredException} is thrown if the new configuration cannot be applied without restarting the
     * node.  In such cases, the node will need to be stopped using {@link #stopNode} before the update can be applied.
     *
     * @param node the new configuration
     * @throws UpdateException if the new node configuration is incorrect or incompatible with the node's current state
     * @throws RestartRequiredException if the node is running, but the new configuration requires a restart
     */
    void updateNode(ServiceNodeConfig node) throws UpdateException, RestartRequiredException;

    /**
     * Deletes the Service Node with the specified name.  If the specified node is currently running, the PC will signal
     * the node to shutdown and wait up to <code>shutdownTimeout</code> milliseconds for the shutdown to conclude.  If,
     * after the timeout has elapsed, the node has still not stopped, the PC will forcibly kill the node's process and
     * continue with the deletion.
     *
     * @param nodeName the name of the Service Node to delete.
     * @param shutdownTimeout the period, in milliseconds, to wait for a clean shutdown to complete before killing the node process. Values <= 0 indicate that the PC should wait indefinitely.
     * @throws DeleteException if the node cannot be deleted
     * @throws ForcedShutdownException if the node's process could not be shutdown cleanly and needed to be killed
     */
    void deleteNode(String nodeName, int shutdownTimeout) throws DeleteException, ForcedShutdownException;

    /**
     * Uploads a new ServiceNode software bundle.  Note that uploading software bundles does not affect any existing
     * Service Nodes on the Gateway; {@link #upgradeNode} must be called separately as needed to upgrade each node to
     * use the new software as desired.
     * 
     * @param softwareData a MIME blob containing the software bundle
     * @return the version of the Service Node software
     * @throws IOException if the software bundle could not be received or saved
     * @throws UpdateException if the software bundle is incompatible with this PC or Gateway
     */
    String uploadServiceNodeSoftware(DataHandler softwareData) throws IOException, UpdateException;

    /**
     * Attempts to upgrade the node with the specified name to the specified version.  A Service Node software bundle
     * corresponding to the specified version must already have been uploaded to the gateway via
     * {@link #uploadServiceNodeSoftware}.  If the specified node is still running, a {@link RestartRequiredException}
     * is thrown, indicating that the node must first be stopped using {@link #stopNode} before the upgrade can be
     * retried.  In the event of an upgrade failure, the PC will make a best-effort attempt to restore the service
     * node's operational capability, but no guarantee is offered.
     *
     * @param nodeName the name of the node to be upgraded
     * @param targetVersion the version to upgrade the node to
     * @throws RestartRequiredException if the node is still running when this method is called
     * @throws UpdateException if the upgrade fails.
     */
    void upgradeNode(String nodeName, String targetVersion) throws UpdateException, RestartRequiredException;

    /**
     * Attempts to start the node with the specified name.  If the specified node is already started, no action is taken.
     * @param nodeName the name of the node to start.
     * @throws FindException if the named node cannot be located
     * @throws StartupException if the node cannot be started
     */
    void startNode(String nodeName) throws FindException, StartupException;

    /**
     * Attempts to stop the named node within the provided timeout period (in milliseconds).  If the
     * node is unable to shut down cleanly within the timeout period, its process will be killed and a
     * {@link ForcedShutdownException} will be thrown.
     *
     * @param nodeName the name of the node to shutdown
     * @param timeout the period, in milliseconds, to wait for a clean shutdown to complete before killing the node process. Values <= 0 indicate that the PC should wait indefinitely.
     */
    void stopNode(String nodeName, int timeout) throws FindException, ForcedShutdownException;

    /**
     * Thrown if a desired service node action cannot be taken while the node is running
     */
    public class RestartRequiredException extends Exception {
        public RestartRequiredException(String nodeName) {
            super(MessageFormat.format("The operation may not proceed without first shutting down the {0} node", nodeName));
        }
    }

    /**
     * Thrown if a service node is unable to start
     */
    public class StartupException extends Exception {
        public StartupException(String node, String reason, Throwable cause) {
            super(MessageFormat.format("{0} could not be started: {1}", node, reason), cause);
        }
    }

    /**
     * Thrown if a service node was unable to shutdown cleanly within the provided timeout period
     */
    public class ForcedShutdownException extends Exception {
        public ForcedShutdownException(String nodeName, int timeout) {
            super(MessageFormat.format("{0} did not shutdown cleanly within {1}ms and has been killed", nodeName, timeout));
        }
    }
}
