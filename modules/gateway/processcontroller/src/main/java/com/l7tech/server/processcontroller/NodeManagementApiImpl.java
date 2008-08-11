package com.l7tech.server.processcontroller;

import com.l7tech.objectmodel.DeleteException;
import com.l7tech.objectmodel.FindException;
import com.l7tech.objectmodel.SaveException;
import com.l7tech.objectmodel.UpdateException;
import com.l7tech.server.management.api.node.NodeManagementApi;
import com.l7tech.server.management.config.node.DatabaseConfig;
import com.l7tech.server.management.config.node.DatabaseType;
import com.l7tech.server.management.config.node.NodeConfig;
import com.l7tech.util.Triple;

import javax.activation.DataHandler;
import javax.jws.WebService;
import java.io.IOException;
import java.util.Map;
import java.util.Set;

@WebService(name="NodeManagementAPI", targetNamespace = "http://ns.l7tech.com/secureSpan/5.0/component/processController/nodeManagementApi")
public class NodeManagementApiImpl implements NodeManagementApi {
    private final ConfigService configService;

    public NodeManagementApiImpl(ConfigService configService) {
        this.configService = configService;
    }

    public NodeConfig createNode(String newNodeName, String version, Map<DatabaseType, DatabaseConfig> databaseConfigMap) throws SaveException {
        return null;
    }

    public NodeConfig getNode(String nodeName) throws FindException {
        final Set<NodeConfig> nodes = configService.getGateway().getNodes();
        for (NodeConfig node : nodes) {
            if (node.getName().equals(nodeName)) return node;
        }
        return null;
    }

    public Set<Triple<String, String, Boolean>> listNodes() throws FindException {
        return null;
    }

    public void updateNode(NodeConfig node) throws UpdateException, RestartRequiredException {
    }

    public void deleteNode(String nodeName, int shutdownTimeout) throws DeleteException, ForcedShutdownException {
    }

    public String uploadNodeSoftware(DataHandler softwareData) throws IOException, UpdateException {
        return null;
    }

    public void upgradeNode(String nodeName, String targetVersion) throws UpdateException, RestartRequiredException {
    }

    public void startNode(String nodeName) throws FindException, StartupException {
    }

    public void stopNode(String nodeName, int timeout) throws FindException, ForcedShutdownException {
    }
}
