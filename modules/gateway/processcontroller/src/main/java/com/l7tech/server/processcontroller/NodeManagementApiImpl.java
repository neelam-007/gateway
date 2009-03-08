/*
 * Copyright (C) 2008-2009 Layer 7 Technologies Inc.
 */
package com.l7tech.server.processcontroller;

import com.l7tech.gateway.config.manager.NodeConfigurationManager;
import com.l7tech.objectmodel.DeleteException;
import com.l7tech.objectmodel.FindException;
import com.l7tech.objectmodel.SaveException;
import com.l7tech.objectmodel.UpdateException;
import com.l7tech.server.management.NodeStateType;
import com.l7tech.server.management.SoftwareVersion;
import com.l7tech.server.management.api.node.NodeManagementApi;
import com.l7tech.server.management.api.monitoring.NodeStatus;
import com.l7tech.server.management.config.node.DatabaseConfig;
import com.l7tech.server.management.config.node.DatabaseType;
import com.l7tech.server.management.config.node.NodeConfig;
import com.l7tech.server.management.config.node.PCNodeConfig;
import com.l7tech.util.ExceptionUtils;
import org.apache.cxf.interceptor.InInterceptors;
import org.apache.cxf.interceptor.OutFaultInterceptors;

import javax.activation.DataHandler;
import javax.annotation.Resource;
import javax.jws.WebService;
import javax.xml.ws.WebServiceContext;
import javax.xml.ws.handler.MessageContext;
import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.net.InetAddress;
import java.net.UnknownHostException;

@WebService(name="NodeManagementAPI",
            targetNamespace="http://ns.l7tech.com/secureSpan/5.0/component/processController/nodeManagementApi",
            endpointInterface="com.l7tech.server.management.api.node.NodeManagementApi")
@InInterceptors(interceptors = "org.apache.cxf.interceptor.LoggingInInterceptor")
@OutFaultInterceptors(interceptors = "org.apache.cxf.interceptor.LoggingOutInterceptor")
public class NodeManagementApiImpl implements NodeManagementApi {
    private static final Logger logger = Logger.getLogger(NodeManagementApiImpl.class.getName());

    private static final String AUTH_FAILURE = "Authentication Required";

    @Resource
    private ConfigService configService;

    @Resource
    private ProcessController processController;

    @Resource
    private WebServiceContext webServiceContext;

    private void checkLocalRequest() {
        final HttpServletRequest req = (HttpServletRequest)webServiceContext.getMessageContext().get(MessageContext.SERVLET_REQUEST);
        if (req == null) throw new IllegalStateException("Couldn't get HttpServletRequest");

        try {
            final InetAddress addr = InetAddress.getByName(req.getRemoteAddr());
            if (addr.isLoopbackAddress()) {
                logger.fine("Allowing connection from localhost with no client certificate");
            } else {
                throw new IllegalArgumentException(AUTH_FAILURE);
            }
        } catch (UnknownHostException e) {
            throw new IllegalStateException("Couldn't get client address", e);
        }
    }


    @Override
    public NodeConfig createNode(NodeConfig nodeConfig)
            throws SaveException {
        String newNodeName = nodeConfig.getName();
        boolean enabled = nodeConfig.isEnabled();
        String clusterPassphrase = nodeConfig.getClusterPassphrase();
        String clusterHostname = nodeConfig.getClusterHostname();

        final Map<String,NodeConfig> nodes = configService.getHost().getNodes();
        PCNodeConfig temp = (PCNodeConfig)nodes.get(newNodeName);
        if (temp != null) throw new IllegalArgumentException(newNodeName + " already exists");

        final List<SoftwareVersion> versions = processController.getAvailableNodeVersions();
        SoftwareVersion nodeVersion = versions.get(0);

        DatabaseConfig[] configs = getDatabaseConfigurations( nodeConfig );
        DatabaseConfig databaseConfig = configs[0];
        DatabaseConfig failoverDatabaseConfig = configs[1];

        if ( databaseConfig == null ) {
            throw new SaveException( "Database configuration is required." );
        }

        final PCNodeConfig node = new PCNodeConfig();
        node.setEnabled(enabled);
        node.setName(newNodeName);
        node.setSoftwareVersion(nodeVersion);
        node.setGuid(UUID.randomUUID().toString().replace("-",""));
        node.setHost(configService.getHost());
        node.setClusterHostname(clusterHostname);
        node.getDatabases().add(databaseConfig);
        node.getDatabases().add(failoverDatabaseConfig);

        try {
            NodeConfigurationManager.configureGatewayNode( newNodeName, node.getGuid(), enabled, clusterPassphrase, databaseConfig, failoverDatabaseConfig );
        } catch (NodeConfigurationManager.NodeConfigurationException nce) {
            logger.log(Level.WARNING, "Error during node configuration '"+ExceptionUtils.getMessage(nce)+"'.", ExceptionUtils.getDebugException(nce) );
            throw new SaveException( "Error during node configuration '"+ExceptionUtils.getMessage(nce)+"'");
        } catch ( IOException ioe ) {
            logger.log(Level.WARNING, "Error during node configuration.", ioe );
            throw new SaveException( "Error during node configuration '"+ExceptionUtils.getMessage(ioe)+"'");
        }

        try {
            configService.addServiceNode(node);
        } catch ( IOException ioe ) {
            logger.log(Level.WARNING, "Error during node configuration.", ioe );
            throw new SaveException( "Error during node configuration '"+ExceptionUtils.getMessage(ioe)+"'");
        }

        return configService.getHost().getNodes().get(newNodeName);
    }

    @Override
    public NodeConfig getNode(String nodeName) throws FindException {
        return configService.getHost().getNodes().get(nodeName);
    }

    @Override
    public Collection<NodeHeader> listNodes() throws FindException {
        final List<NodeHeader> nodes = new ArrayList<NodeHeader>();
        for (NodeConfig config : configService.getHost().getNodes().values()) {
            final PCNodeConfig pcNodeConfig = (PCNodeConfig)config;
            final NodeStatus status = processController.getNodeStatus(pcNodeConfig.getName());
            nodes.add(new NodeHeader(pcNodeConfig.getId(), pcNodeConfig.getName(), pcNodeConfig.getSoftwareVersion(), pcNodeConfig.isEnabled(), status.getType(), status.getStartTime(), status.getLastObservedTime()));
        }
        return nodes;
    }

    /**
     * Update configuration for a node.
     *
     * <p>This allows for update of the following node configuration</p>
     *
     * <ul>
     *   <li>Database Configuration : The primary DB configuration</li>
     *   <li>Failover Database Configuration : The secondary DB configuration</li>
     *   <li>Enabled : The node enabled/disabled state</li>
     *   <li>Cluster Passphrase : The node passphrase</li>
     * </ul>
     *
     * @node The updated node configuration.
     */
    @Override
    public void updateNode(NodeConfig node) throws UpdateException, RestartRequiredException {
        // validate and persist configuration
        final String nodeName = node.getName();
        final Map<String,NodeConfig> nodes = configService.getHost().getNodes();
        if (nodes.get(nodeName) == null) throw new UpdateException("Node '" + nodeName + "' not found.");

        String clusterPassphrase = node.getClusterPassphrase();
        DatabaseConfig[] configs;
        try {
            configs = getDatabaseConfigurations( node );
        } catch (SaveException se) {
            throw new UpdateException( se.getMessage() );
        }
        DatabaseConfig databaseConfig = configs[0];
        DatabaseConfig failoverDatabaseConfig = configs[1];

        try {
            NodeConfigurationManager.configureGatewayNode( nodeName, null, node.isEnabled(), clusterPassphrase, databaseConfig, failoverDatabaseConfig  );
        } catch (NodeConfigurationManager.NodeConfigurationException nce) {
            logger.log(Level.WARNING, "Error during node configuration '"+ExceptionUtils.getMessage(nce)+"'.", ExceptionUtils.getDebugException(nce) );
            throw new UpdateException( "Error during node configuration '"+ExceptionUtils.getMessage(nce)+"'");
        } catch ( IOException ioe ) {
            logger.log(Level.WARNING, "Error during node configuration.", ioe );
            throw new UpdateException( "Error during node configuration '"+ExceptionUtils.getMessage(ioe)+"'");
        }

        // update internal configuration
        try {
            configService.updateServiceNode(node);
        } catch ( IOException ioe ) {
            logger.log(Level.WARNING, "Error during node configuration.", ioe );
            throw new UpdateException( "Error during node configuration '"+ExceptionUtils.getMessage(ioe)+"'");
        }

        // apply state change if required
        NodeStateType currentState = processController.getNodeStatus(nodeName).getType();
        if ( node.isEnabled() && NodeStateType.RUNNING != currentState ) {
            try {
                processController.startNode( (PCNodeConfig)nodes.get(nodeName), false );
            } catch (IOException ioe) {
                logger.log( Level.WARNING, "Error starting node.", ioe );
                throw new UpdateException("Error starting node '"+ExceptionUtils.getMessage(ioe)+"'.");
            }
        } else if ( !node.isEnabled() && NodeStateType.STOPPED != currentState ) {
            processController.stopNode( nodeName, ProcessController.DEFAULT_STOP_TIMEOUT );
        }

    }

    @Override
    public void deleteNode(String nodeName, int shutdownTimeout) throws DeleteException {
        try {
            processController.deleteNode(nodeName, shutdownTimeout);
            configService.deleteNode(nodeName);
        } catch (Exception e) {
            logger.log(Level.WARNING, "Error deleting node '"+nodeName+"'.", e );
            throw new DeleteException("Couldn't delete node '"+nodeName+"', due to '"+ExceptionUtils.getMessage(e)+"'.");
        }
    }

    @Override
    public String uploadNodeSoftware(DataHandler softwareData) throws IOException, UpdateException {
        throw new UnsupportedOperationException("Not yet implemented");
    }

    @Override
    public void upgradeNode(String nodeName, String targetVersion) throws UpdateException, RestartRequiredException {
        throw new UnsupportedOperationException("Not yet implemented");
    }

    @Override
    public NodeStateType startNode(String nodeName) throws FindException, StartupException {
        NodeStateType tempState = processController.getNodeStatus(nodeName).getType();
        if (tempState == null) tempState = NodeStateType.UNKNOWN;
        switch(tempState) {
            case RUNNING:
            case STARTING:
                return tempState; // Good enough

            case WONT_START:
                throw new StartupException(nodeName, "Node couldn't be started last time it was attempted; the PC will retry soon");

            case STOPPING:
                throw new StartupException(nodeName, "Node is in the process of shutting down; try again later");

            case CRASHED:
            case STOPPED:
            case UNKNOWN:
            default:
                final PCNodeConfig node = (PCNodeConfig)configService.getHost().getNodes().get(nodeName);
                if (node == null) throw new StartupException(nodeName, "No such node");

                try {
                    if (!node.isEnabled()) throw new StartupException(nodeName, "Node is disabled");
                    processController.startNode(node, false);
                    return NodeStateType.STARTING;
                } catch (IOException e) {
                    throw new StartupException(nodeName, "Couldn't be started: " + ExceptionUtils.getMessage(e));
                }
        }
    }

    @Override
    public void stopNode(String nodeName, int timeout) throws FindException {
        processController.stopNode(nodeName, timeout);
    }

    @Override
    public void createDatabase(String nodeName, DatabaseConfig dbconfig, Collection<String> dbHosts, String adminLogin, String adminPassword, String clusterHostname) throws DatabaseCreationException {
        try {
            NodeConfigurationManager.createDatabase(nodeName, dbconfig, dbHosts, adminLogin, adminPassword, clusterHostname);
        } catch (IOException e) {
            logger.log(Level.WARNING, "Error creating database for '"+nodeName+"'.", e );
            throw new DatabaseCreationException(ExceptionUtils.getMessage(e));
        }
    }

    @Override
    public boolean testDatabaseConfig( final DatabaseConfig dbconfig ) {
        checkLocalRequest();
        return NodeConfigurationManager.testDatabase( dbconfig );
    }

    @Override
    public void deleteDatabase( final DatabaseConfig dbconfig,
                                final Collection<String> dbHosts ) throws DatabaseDeletionException {
        checkLocalRequest();
        try {
            NodeConfigurationManager.deleteDatabase( dbconfig, dbHosts, true );
        } catch (IOException e) {
            logger.log(Level.WARNING, "Error deleting database '"+dbconfig.getHost()+"' due to '"+ExceptionUtils.getMessage(e)+"'.", e.getCause()!=null ? e : ExceptionUtils.getDebugException(e) );
            throw new DatabaseDeletionException("Unable to delete database '"+ExceptionUtils.getMessage(e)+"'" );
        }
    }

    private DatabaseConfig[] getDatabaseConfigurations( final NodeConfig nodeConfig ) throws SaveException {
        DatabaseConfig databaseConfig = null;
        DatabaseConfig failoverDatabaseConfig = null;

        if ( nodeConfig.getDatabases() != null ) {
            for ( DatabaseConfig config : nodeConfig.getDatabases() ) {
                if ( config !=null && config.getType() == DatabaseType.NODE_ALL ) {
                    if ( config.getClusterType() != null ) {
                        switch ( config.getClusterType() ) {
                            case REPL_MASTER:
                            case STANDALONE:
                                if ( databaseConfig != null ) throw new SaveException("Invalid database configuration (primary db conflict).");
                                databaseConfig = config;
                                break;
                            case REPL_SLAVE:
                                if ( failoverDatabaseConfig != null ) throw new SaveException("Invalid database configuration (failover db conflict).");
                                failoverDatabaseConfig = config;
                                break;
                        }
                    }
                }
            }
        }

        return new DatabaseConfig[]{ databaseConfig, failoverDatabaseConfig };
    }
}
