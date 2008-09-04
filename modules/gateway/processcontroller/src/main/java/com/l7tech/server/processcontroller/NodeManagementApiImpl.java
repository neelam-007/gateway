package com.l7tech.server.processcontroller;

import com.l7tech.objectmodel.DeleteException;
import com.l7tech.objectmodel.FindException;
import com.l7tech.objectmodel.SaveException;
import com.l7tech.objectmodel.UpdateException;
import com.l7tech.server.management.NodeStateType;
import com.l7tech.server.management.SoftwareVersion;
import com.l7tech.server.management.api.node.NodeManagementApi;
import com.l7tech.server.management.config.node.NodeConfig;
import com.l7tech.server.management.config.node.PCNodeConfig;
import com.l7tech.server.management.config.node.DatabaseType;
import com.l7tech.server.management.config.node.DatabaseConfig;
import com.l7tech.gateway.config.manager.NodeConfigurationManager;
import com.l7tech.util.ExceptionUtils;

import javax.activation.DataHandler;
import javax.annotation.Resource;
import javax.jws.WebService;
import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.Map;
import java.util.UUID;

@WebService(name="NodeManagementAPI",
            targetNamespace="http://ns.l7tech.com/secureSpan/5.0/component/processController/nodeManagementApi",
            endpointInterface="com.l7tech.server.management.api.node.NodeManagementApi")
public class NodeManagementApiImpl implements NodeManagementApi {
    @Resource
    private ConfigService configService;

    @Resource
    private ProcessController processController;

    public NodeConfig createNode(String newNodeName, String desiredVersion, Set<DatabaseConfigRow> databaseConfigs)
            throws SaveException {

        final Map<String,NodeConfig> nodes = configService.getHost().getNodes();
        PCNodeConfig temp = (PCNodeConfig)nodes.get(newNodeName);
        if (temp != null) throw new IllegalArgumentException(newNodeName + " already exists");

        SoftwareVersion nodeVersion = null;
        final List<SoftwareVersion> versions = processController.getAvailableNodeVersions();
        if (desiredVersion == null) {
            nodeVersion = versions.get(0);
        } else for (SoftwareVersion aversion : versions) {
            if (aversion.toString().equals(desiredVersion)) {
                nodeVersion = aversion;
                break;
            }
        }

        if (nodeVersion == null) throw new IllegalArgumentException("Node version " + desiredVersion + " is not available on this host");
        final PCNodeConfig node = new PCNodeConfig();
        node.setEnabled(true);
        node.setName(newNodeName);
        node.setSoftwareVersion(nodeVersion);
        node.setGuid(UUID.randomUUID().toString().replace("-",""));
        node.setHost(configService.getHost());

        DatabaseConfig databaseConfig = null;
        for ( DatabaseConfigRow config : databaseConfigs ) {
            if ( config.getType() == DatabaseType.NODE_ALL ) {
                databaseConfig = config.getConfig();
                break;
            }
        }

        if ( databaseConfig == null ) {
            throw new SaveException( "Database configuration is required." );
        }

        try {
            NodeConfigurationManager.configureGatewayNode( newNodeName, node.getGuid(), null, "surprise!", databaseConfig );
        } catch ( IOException ioe ) {
            throw new SaveException( "Error during node configuration.", ioe);
        }

        configService.addServiceNode(node);
        return node;
    }

    public NodeConfig getNode(String nodeName) throws FindException {
        return configService.getHost().getNodes().get(nodeName);
    }

    public Set<NodeHeader> listNodes() throws FindException {
        final Set<NodeHeader> nodes = new HashSet<NodeHeader>();
        for (NodeConfig config : configService.getHost().getNodes().values()) {
            final PCNodeConfig pcNodeConfig = (PCNodeConfig)config;
            final NodeStateType state = processController.getNodeState(pcNodeConfig.getName());
            nodes.add(new NodeHeader(pcNodeConfig.getId(), pcNodeConfig.getName(), pcNodeConfig.getSoftwareVersion(), pcNodeConfig.isEnabled(), state));
        }
        return nodes;
    }

    public void updateNode(NodeConfig node) throws UpdateException, RestartRequiredException {
        throw new UnsupportedOperationException("Not yet implemented");
    }

    public void deleteNode(String nodeName, int shutdownTimeout) throws DeleteException, ForcedShutdownException {
        throw new UnsupportedOperationException("Not yet implemented");
    }

    public String uploadNodeSoftware(DataHandler softwareData) throws IOException, UpdateException {
        throw new UnsupportedOperationException("Not yet implemented");
    }

    public void upgradeNode(String nodeName, String targetVersion) throws UpdateException, RestartRequiredException {
        throw new UnsupportedOperationException("Not yet implemented");
    }

    public NodeStateType startNode(String nodeName) throws FindException, StartupException {
        NodeStateType tempState = processController.getNodeState(nodeName);
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
                    processController.startNode(node);
                    return NodeStateType.STARTING;
                } catch (IOException e) {
                    throw new StartupException(nodeName, "Couldn't be started: " + ExceptionUtils.getMessage(e));
                }
        }
    }

    public void stopNode(String nodeName, int timeout) throws FindException, ForcedShutdownException {
        processController.stopNode(nodeName, timeout);
    }
}
