package com.l7tech.server.processcontroller;

import com.l7tech.objectmodel.DeleteException;
import com.l7tech.objectmodel.FindException;
import com.l7tech.objectmodel.SaveException;
import com.l7tech.objectmodel.UpdateException;
import com.l7tech.server.management.NodeStateType;
import com.l7tech.server.management.api.node.NodeManagementApi;
import com.l7tech.server.management.config.node.NodeConfig;
import com.l7tech.server.management.config.node.PCNodeConfig;
import com.l7tech.util.ExceptionUtils;

import javax.activation.DataHandler;
import javax.annotation.Resource;
import javax.jws.WebService;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

@WebService(name="NodeManagementAPI",
            targetNamespace="http://ns.l7tech.com/secureSpan/5.0/component/processController/nodeManagementApi",
            endpointInterface="com.l7tech.server.management.api.node.NodeManagementApi")
public class NodeManagementApiImpl implements NodeManagementApi {
    @Resource
    private ConfigService configService;

    @Resource
    private ProcessController processController;

    public NodeConfig createNode(String newNodeName, String version, Set<DatabaseConfigRow> databaseConfigs)
            throws SaveException {
        throw new UnsupportedOperationException("Not yet implemented");
    }

    public NodeConfig getNode(String nodeName) throws FindException {
        final Set<NodeConfig> nodes = configService.getGateway().getNodes();
        for (NodeConfig node : nodes) {
            if (node.getName().equals(nodeName)) return node;
        }
        return null;
    }

    public Set<NodeHeader> listNodes() throws FindException {
        final Set<NodeHeader> nodes = new HashSet<NodeHeader>();
        for (NodeConfig config : configService.getGateway().getNodes()) {
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
                final Set<NodeConfig> nodeConfigs;
                try {
                    nodeConfigs = configService.getGateway().getNodes();
                } catch (Exception e) {
                    throw new FindException("Couldn't get Gateway or Nodes");
                }

                for (NodeConfig nodeConfig : nodeConfigs) {
                    if (nodeName.equals(nodeConfig.getName())) {
                        try {
                            if (!nodeConfig.isEnabled()) throw new StartupException(nodeName, "Node is disabled");
                            processController.startNode((PCNodeConfig)nodeConfig);
                            return NodeStateType.STARTING;
                        } catch (IOException e) {
                            throw new StartupException(nodeName, "Couldn't be started: " + ExceptionUtils.getMessage(e));
                        }
                    }
                }

                throw new StartupException(nodeName, "No such node");
        }
    }

    public void stopNode(String nodeName, int timeout) throws FindException, ForcedShutdownException {
        processController.stopNode(nodeName, timeout);
    }
}
