/**
 * Copyright (C) 2008 Layer 7 Technologies Inc.
 */
package com.l7tech.server.management.api.node;

import com.l7tech.objectmodel.DeleteException;
import com.l7tech.objectmodel.FindException;
import com.l7tech.objectmodel.SaveException;
import com.l7tech.objectmodel.UpdateException;
import com.l7tech.server.management.NodeStateType;
import com.l7tech.server.management.SoftwareVersion;
import com.l7tech.server.management.config.node.DatabaseConfig;
import com.l7tech.server.management.config.node.DatabaseType;
import com.l7tech.server.management.config.node.NodeConfig;

import javax.activation.DataHandler;
import javax.jws.WebResult;
import javax.jws.WebService;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlRootElement;
import java.io.IOException;
import java.text.MessageFormat;
import java.util.Set;

/**
 * Part of the API invoked on the Process Controller by Enterprise Manager Servers.
 * @author alex
 */
@WebService
public interface NodeManagementApi {
    /**
     * Creates a new {@link com.l7tech.server.management.config.node.NodeConfig} with reasonable default parameters for the current host.
     *
     * TODO should this method save the node, or just return a transient instance?  I guess it depends on whether the defaults are sufficient to specify a fully functional node.
     *
     * TODO consider adding enough parameters to this method such that the resulting node is minimally specified
     *
     * @param nodeConfig The name of the new node must be set; must be unique within this host
     *                   The the software version that the Node should be initialized for, or
     *                   null to use the most recent version available.
     *                   The database configuration to use for the new node.  If null is passed, the PC will
     *                   attempt to create new, standalone database(s) with the default {@link DatabaseType type(s)}
     *                   and {@link DatabaseConfig.Vendor vendor(s)}
     *                   on the default database server(s) with the default username(s) and randomly generated
     *                   password(s).  If the PC is unable to create the databases (e.g. because it doesn't have
     *                   sufficient credentials to do so) a {@link SaveException} will be thrown.
     * @param adminLogin The admin account to create for cluster administration.
     * @param adminPassphrase The admin account passphrase
     *
     * @return the newly created NodeConfig
     * @throws SaveException if the new node cannot be created
     */
    NodeConfig createNode(NodeConfig nodeConfig, String adminLogin, String adminPassphrase) throws SaveException;

    /**
     * Retrieves the {@link com.l7tech.server.management.config.node.NodeConfig} with the provided name, or null if no such node exists on this host.
     *
     * @param nodeName the name of the node to retrieve
     * @return the NodeConfig for the node with the specified name
     * @throws FindException if for some reason the PC is unable to determine the (non-)existence of a node with the
     *                       specified name (e.g. the PC's configuration is corrupted or temporarily unavailable)
     */
    NodeConfig getNode(String nodeName) throws FindException;

    /**
     * Lists the names and versions of the Nodes on this host.  The resulting Set may be empty, indicating
     * that no nodes have been configured yet.
     *
     * @return a Set of Triples, each consisting of the name, version and enablement status of a Node on this
     *         host.
     * @throws FindException if the list of Nodes cannot be retrieved
     */
    @WebResult(name="node")
    Set<NodeHeader> listNodes() throws FindException;

    /**
     * This class only exists because JAXB can't handle a Set&lt;Triple&lt;String, String, Boolean&gt;&gt; in any reasonable way
     */
    @XmlRootElement(name="node")
    public static class NodeHeader {
        private String id;
        private String name;
        private String version;
        private boolean enabled;
        private NodeStateType state;

        public NodeHeader(String id, String name, SoftwareVersion softwareVersion, boolean enabled, NodeStateType state) {
            this.id = id;
            this.name = name;
            this.state = state;
            this.version = softwareVersion == null ? null : softwareVersion.toString();
            this.enabled = enabled;
        }

        public NodeHeader() { }

        @XmlAttribute
        public String getId() {
            return id;
        }

        @XmlAttribute
        public String getName() {
            return name;
        }

        @XmlAttribute
        public boolean isEnabled() {
            return enabled;
        }

        @XmlAttribute
        public String getVersion() {
            return version;
        }

        public void setId(String id) {
            this.id = id;
        }

        public void setName(String name) {
            this.name = name;
        }

        public void setVersion(String version) {
            this.version = version;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        @XmlAttribute
        public NodeStateType getState() {
            return state;
        }

        public void setState(NodeStateType state) {
            this.state = state;
        }
    }

    /**
     * Updates a node's configuration.  If the node is already running when this method is invoked, a
     * {@link RestartRequiredException} is thrown if the new configuration cannot be applied without restarting the
     * node.  In such cases, the node will need to be stopped using {@link #stopNode} before the update can be applied.
     *
     * @param node the new configuration
     * @throws UpdateException if the new node configuration is incorrect or incompatible with the node's current state
     * @throws RestartRequiredException if the node is running, but the new configuration requires a restart
     */
    void updateNode(NodeConfig node) throws UpdateException, RestartRequiredException;

    /**
     * Deletes the Node with the specified name.  If the specified node is currently running, the PC will signal
     * the node to shutdown and wait up to <code>shutdownTimeout</code> milliseconds for the shutdown to conclude.  If,
     * after the timeout has elapsed, the node has still not stopped, the PC will forcibly kill the node's process and
     * continue with the deletion.
     *
     * @param nodeName the name of the Node to delete.
     * @param shutdownTimeout the period, in milliseconds, to wait for a clean shutdown to complete before killing the node process. Values <= 0 indicate that the PC should wait indefinitely.
     * @throws DeleteException if the node cannot be deleted
     * @throws ForcedShutdownException if the node's process could not be shutdown cleanly and needed to be killed
     */
    void deleteNode(String nodeName, int shutdownTimeout) throws DeleteException, ForcedShutdownException;

    /**
     * Uploads a new Node software bundle.  Note that uploading software bundles does not affect any existing
     * Nodes on the host; {@link #upgradeNode} must be called separately as needed to upgrade each node to
     * use the new software as desired.
     * 
     * @param softwareData a MIME blob containing the software bundle
     * @return the version of the Node software
     * @throws IOException if the software bundle could not be received or saved
     * @throws UpdateException if the software bundle is incompatible with this PC or Host
     */
    String uploadNodeSoftware(DataHandler softwareData) throws IOException, UpdateException;

    /**
     * Attempts to upgrade the node with the specified name to the specified version.  A Node software bundle
     * corresponding to the specified version must already have been uploaded to the host via
     * {@link #uploadNodeSoftware}.  If the specified node is still running, a {@link RestartRequiredException}
     * is thrown, indicating that the node must first be stopped using {@link #stopNode} before the upgrade can be
     * retried.  In the event of an upgrade failure, the PC will make a best-effort attempt to restore the
     * node's operational capability, but no guarantee is offered.
     *
     * @param nodeName the name of the node to be upgraded
     * @param targetVersion the version to upgrade the node to
     * @throws RestartRequiredException if the node is still running when this method is called
     * @throws UpdateException if the upgrade fails.
     */
    void upgradeNode(String nodeName, String targetVersion) throws UpdateException, RestartRequiredException;

    /**
     * Attempts to start the node with the specified name.  If the specified node is already starting or running, no
     * action is taken.
     * 
     * @param nodeName the name of the node to start.
     * @throws FindException if the named node cannot be located
     * @throws StartupException if the node cannot be started
     */
    NodeStateType startNode(String nodeName) throws FindException, StartupException;

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
     * Thrown if a desired node action cannot be taken while the node is running
     */
    public class RestartRequiredException extends Exception {
        public RestartRequiredException(String nodeName) {
            super(MessageFormat.format("The operation may not proceed without first shutting down the {0} node", nodeName));
        }
    }

    /**
     * Thrown if a node is unable to start
     */
    public class StartupException extends Exception {
        public StartupException(String node, String reason) {
            super(MessageFormat.format("{0} could not be started: {1}", node, reason));
        }
    }

    /**
     * Thrown if a node was unable to shutdown cleanly within the provided timeout period
     */
    public class ForcedShutdownException extends Exception {
        public ForcedShutdownException(String nodeName, int timeout) {
            super(MessageFormat.format("{0} did not shutdown cleanly within {1}ms and has been killed", nodeName, timeout));
        }
    }
}
