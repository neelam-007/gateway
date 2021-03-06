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

import javax.jws.WebParam;
import javax.jws.WebResult;
import javax.jws.WebService;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlRootElement;
import java.text.MessageFormat;
import java.util.Collection;
import java.util.Date;

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
     *
     * @return the newly created NodeConfig
     * @throws SaveException if the new node cannot be created
     */
    NodeConfig createNode(NodeConfig nodeConfig) throws SaveException;

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
    Collection<NodeHeader> listNodes() throws FindException;

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
        private Date stateStartTime;
        private Date sinceWhen;

        public NodeHeader(String id, String name, SoftwareVersion softwareVersion, boolean enabled, NodeStateType state, Date stateStartTime, Date sinceWhen) {
            this.id = id;
            this.name = name;
            this.state = state;
            this.sinceWhen = sinceWhen;
            this.version = softwareVersion == null ? null : softwareVersion.toString();
            this.enabled = enabled;
            this.stateStartTime = stateStartTime;
        }

        public NodeHeader() { }

        @XmlAttribute
        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }

        @XmlAttribute
        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        @XmlAttribute
        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        @XmlAttribute
        public String getVersion() {
            return version;
        }

        public void setVersion(String version) {
            this.version = version;
        }

        @XmlAttribute
        public Date getSinceWhen() {
            return sinceWhen;
        }

        public void setSinceWhen( final Date sinceWhen ) {
            this.sinceWhen = sinceWhen;
        }

        @XmlAttribute
        public NodeStateType getState() {
            return state;
        }

        public void setState(NodeStateType state) {
            this.state = state;
        }

        /**
         * @return the time when this state was first detected
         */
        @XmlAttribute
        public Date getStateStartTime() {
            return stateStartTime;
        }

        public void setStateStartTime(Date stateStartTime) {
            this.stateStartTime = stateStartTime;
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
     * <p/>
     *
     * @param nodeName the name of the Node to delete.
     * @param shutdownTimeout the period, in milliseconds, to wait for a clean shutdown to complete before killing the node process. Values <= 0 indicate that the PC should wait indefinitely.
     * @throws DeleteException if the node cannot be deleted
     */
    void deleteNode(@WebParam(name="nodeName") String nodeName,
                    @WebParam(name="shutdownTimeout") int shutdownTimeout)
        throws DeleteException;

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
     * node is unable to shut down cleanly within the timeout period, its process will be killed.
     *
     * @param nodeName the name of the node to shutdown
     * @param timeout the period, in milliseconds, to wait for a clean shutdown to complete before killing the node process. Values <= 0 indicate that the PC should wait indefinitely.
     */
    void stopNode(String nodeName, int timeout) throws FindException;

    /**
     * Attempt to create the database described by the provided configuration, including a few initial settings
     * 
     * @param nodeName the name of the node for which the database is being created
     * @param dbconfig the database configuration
     * @param dbHosts the list of database hosts
     * @param adminLogin the login for the initial administrator account
     * @param adminPassword the password for the initial administrator account
     * @param clusterHostname the cluster hostname to set as a cluster property
     */
    void createDatabase(String nodeName, DatabaseConfig dbconfig, Collection<String> dbHosts, String adminLogin, String adminPassword, String clusterHostname) throws DatabaseCreationException;

    /**
     * Test the given database confguration.
     *
     * <p>For security reasons this method cannot be used from a remote host.</p>
     *
     * @param dbconfig The db configuration, administration username/password is required.
     */
    boolean testDatabaseConfig(@WebParam(name="databaseConfiguration") DatabaseConfig dbconfig );

    /**
     * Deletes the specified database.
     *
     * <p>For security reasons this method cannot be used from a remote host.</p>
     *
     * @param dbconfig The db configuration, administration username/password is required.
     * @param dbHosts The extra hosts for which database grants should be revoked.
     * @throws DatabaseDeletionException if the database cannot be deleted
     */
    void deleteDatabase(@WebParam(name="databaseConfiguration") DatabaseConfig dbconfig,
                        @WebParam(name="databaseHosts") Collection<String> dbHosts ) throws DatabaseDeletionException;
    
    public class DatabaseCreationException extends Exception {
        public DatabaseCreationException(String message, Throwable cause) {
            super(message, cause);
        }

        public DatabaseCreationException(String message) {
            super(message);
        }
    }

    public class DatabaseDeletionException extends Exception {
        public DatabaseDeletionException(String message, Throwable cause) {
            super(message, cause);
        }

        public DatabaseDeletionException(String message) {
            super(message);
        }
    }

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
        public StartupException(String message) {
            super(message);
        }

        public StartupException(String node, String reason) {
            this(MessageFormat.format("{0} could not be started: {1}", node, reason));
        }
    }
}
