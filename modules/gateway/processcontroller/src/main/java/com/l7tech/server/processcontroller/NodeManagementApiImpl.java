package com.l7tech.server.processcontroller;

import com.l7tech.gateway.config.manager.NodeConfigurationManager;
import com.l7tech.objectmodel.DeleteException;
import com.l7tech.objectmodel.FindException;
import com.l7tech.objectmodel.SaveException;
import com.l7tech.objectmodel.UpdateException;
import com.l7tech.server.management.NodeStateType;
import com.l7tech.server.management.SoftwareVersion;
import com.l7tech.server.management.api.node.NodeManagementApi;
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
import javax.servlet.http.HttpServletRequest;
import javax.xml.ws.WebServiceContext;
import javax.xml.ws.handler.MessageContext;
import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.security.cert.X509Certificate;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
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

    @Resource @SuppressWarnings({ "SpringJavaAutowiringInspection" })
    private WebServiceContext webServiceContext;

    private void checkRequest() {
        final HttpServletRequest req = (HttpServletRequest)webServiceContext.getMessageContext().get(MessageContext.SERVLET_REQUEST);
        if (req == null) throw new IllegalStateException("Couldn't get HttpServletRequest");

        try {
            final InetAddress addr = InetAddress.getByName(req.getRemoteAddr()); // TODO maybe special-case "127.0.0.1" and "localhost" to avoid the DNS lookup?
            if (addr.isLoopbackAddress()) {
                logger.fine("Allowing connection from localhost with no client certificate");
                return;
            }
        } catch (UnknownHostException e) {
            throw new IllegalStateException("Couldn't get client address", e);
        }

        final Object maybeCert = req.getAttribute("javax.servlet.request.X509Certificate");
        final X509Certificate certificate;
        if (maybeCert instanceof X509Certificate) {
            certificate = (X509Certificate)maybeCert;
        } else if (maybeCert instanceof X509Certificate[]) {
            X509Certificate[] certs = (X509Certificate[])maybeCert;
            certificate = certs[0];
        } else if (maybeCert != null) {
            logger.warning( "Client certificate was a " + maybeCert.getClass().getName() + ", not an X509Certificate" );
            throw new IllegalStateException(AUTH_FAILURE);
        } else {
            logger.fine( "Client certificate missing in request." );
            throw new IllegalArgumentException(AUTH_FAILURE);
        }

        if (!configService.getTrustedRemoteNodeManagementCerts().contains(certificate)) {
            logger.info( "Client certificate was not trusted for remote management '" + certificate.getSubjectDN().toString() + "'." );
            throw new IllegalArgumentException(AUTH_FAILURE);
        }

        logger.log(Level.FINE, "Accepted client certificate {0}", certificate.getSubjectDN().getName());
    }

    @Override
    public NodeConfig createNode(NodeConfig nodeConfig)
            throws SaveException {
        checkRequest();

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

        configService.addServiceNode(node);
        return node;
    }

    @Override
    public NodeConfig getNode(String nodeName) throws FindException {
        checkRequest();
        return configService.getHost().getNodes().get(nodeName);
    }

    @Override
    public Collection<NodeHeader> listNodes() throws FindException {
        checkRequest();
        final List<NodeHeader> nodes = new ArrayList<NodeHeader>();
        for (NodeConfig config : configService.getHost().getNodes().values()) {
            final PCNodeConfig pcNodeConfig = (PCNodeConfig)config;
            final ProcessController.NodeStateSample state = processController.getNodeState(pcNodeConfig.getName());
            nodes.add(new NodeHeader(pcNodeConfig.getId(), pcNodeConfig.getName(), pcNodeConfig.getSoftwareVersion(), pcNodeConfig.isEnabled(), state.getType(), state.getStartTime(), state.getLastObservedTime()));
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
        checkRequest();

        // validate and persist configuration
        final String nodeName = node.getName();
        final Map<String,NodeConfig> nodes = configService.getHost().getNodes();
        PCNodeConfig currentNodeConfig = (PCNodeConfig)nodes.get(nodeName);
        if (currentNodeConfig == null) throw new UpdateException("Node '" + nodeName + "' not found.");

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
        currentNodeConfig.setEnabled( node.isEnabled() );
        if ( clusterPassphrase != null )  {
            currentNodeConfig.setClusterPassphrase( clusterPassphrase );
        }
        if ( databaseConfig != null ) {
            currentNodeConfig.getDatabases().clear();
            currentNodeConfig.getDatabases().add(databaseConfig);
            if ( failoverDatabaseConfig != null ) {
                failoverDatabaseConfig.setParent(databaseConfig);
                currentNodeConfig.getDatabases().add(failoverDatabaseConfig);
            }
        }

        // apply state change if required
        NodeStateType currentState = processController.getNodeState(nodeName).getType();
        if ( node.isEnabled() && NodeStateType.RUNNING != currentState ) {
            try {
                processController.startNode( currentNodeConfig, false );
            } catch (IOException ioe) {
                logger.log( Level.WARNING, "Error starting node.", ioe );
                throw new UpdateException("Error starting node '"+ExceptionUtils.getMessage(ioe)+"'.");
            }
        } else if ( !node.isEnabled() && NodeStateType.STOPPED != currentState ) {
            processController.stopNode( nodeName, ProcessController.DEFAULT_STOP_TIMEOUT );
        }

    }

    @Override
    public void deleteNode(String nodeName, int shutdownTimeout) throws DeleteException, ForcedShutdownException {
        checkRequest();
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
        checkRequest();
        throw new UnsupportedOperationException("Not yet implemented");
    }

    @Override
    public void upgradeNode(String nodeName, String targetVersion) throws UpdateException, RestartRequiredException {
        checkRequest();
        throw new UnsupportedOperationException("Not yet implemented");
    }

    @Override
    public NodeStateType startNode(String nodeName) throws FindException, StartupException {
        checkRequest();
        NodeStateType tempState = processController.getNodeState(nodeName).getType();
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
    public void stopNode(String nodeName, int timeout) throws FindException, ForcedShutdownException {
        checkRequest();
        processController.stopNode(nodeName, timeout);
    }

    @Override
    public void createDatabase(String nodeName, DatabaseConfig dbconfig, Collection<String> dbHosts, String adminLogin, String adminPassword) throws DatabaseCreationException {
        try {
            NodeConfigurationManager.createDatabase(nodeName, dbconfig, dbHosts, adminLogin, adminPassword);
        } catch (IOException e) {
            logger.log(Level.WARNING, "Error creating database for '"+nodeName+"'.", e );
            throw new DatabaseCreationException("Unable to create database '"+ExceptionUtils.getMessage(e)+"'" );
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
