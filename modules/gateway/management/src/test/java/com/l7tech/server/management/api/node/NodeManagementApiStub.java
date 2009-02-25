package com.l7tech.server.management.api.node;

import com.l7tech.objectmodel.DeleteException;
import com.l7tech.objectmodel.FindException;
import com.l7tech.objectmodel.SaveException;
import com.l7tech.objectmodel.UpdateException;
import com.l7tech.server.management.NodeStateType;
import com.l7tech.server.management.config.node.DatabaseConfig;
import com.l7tech.server.management.config.node.NodeConfig;

import javax.activation.DataHandler;
import java.io.IOException;
import java.util.Collection;

/**
 *
 */
public class NodeManagementApiStub implements NodeManagementApi {
    public NodeConfig createNode(NodeConfig nodeConfig) throws SaveException {
        return null;
    }

    public NodeConfig getNode(String nodeName) throws FindException {
        return null;
    }

    public Collection<NodeHeader> listNodes() throws FindException {
        return null;
    }

    public void updateNode(NodeConfig node) throws UpdateException, RestartRequiredException {
    }

    public void deleteNode(String nodeName, int shutdownTimeout) throws DeleteException {
    }

    public String uploadNodeSoftware(DataHandler softwareData) throws IOException, UpdateException {
        return null;
    }

    public void upgradeNode(String nodeName, String targetVersion) throws UpdateException, RestartRequiredException {
    }

    public NodeStateType startNode(String nodeName) throws FindException, StartupException {
        return null;
    }

    public void stopNode(String nodeName, int timeout) throws FindException {
    }

    public void createDatabase(String nodeName, DatabaseConfig dbconfig, Collection<String> dbHosts, String adminLogin, String adminPassword) throws DatabaseCreationException {
    }
}
