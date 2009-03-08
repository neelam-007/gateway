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
    @Override
    public NodeConfig createNode(NodeConfig nodeConfig) throws SaveException {
        return null;
    }

    @Override
    public NodeConfig getNode(String nodeName) throws FindException {
        return null;
    }

    @Override
    public Collection<NodeHeader> listNodes() throws FindException {
        return null;
    }

    @Override
    public void updateNode(NodeConfig node) throws UpdateException, RestartRequiredException {
    }

    @Override
    public void deleteNode(String nodeName, int shutdownTimeout) throws DeleteException {
    }

    @Override
    public String uploadNodeSoftware(DataHandler softwareData) throws IOException, UpdateException {
        return null;
    }

    @Override
    public void upgradeNode(String nodeName, String targetVersion) throws UpdateException, RestartRequiredException {
    }

    @Override
    public NodeStateType startNode(String nodeName) throws FindException, StartupException {
        return null;
    }

    @Override
    public void stopNode(String nodeName, int timeout) throws FindException {
    }

    @Override
    public void createDatabase(String nodeName, DatabaseConfig dbconfig, Collection<String> dbHosts, String adminLogin, String adminPassword, String clusterHostname) throws DatabaseCreationException {
    }

    @Override
    public boolean testDatabaseConfig(DatabaseConfig dbconfig) {
        return false;
    }

    @Override
    public void deleteDatabase( final DatabaseConfig dbconfig,
                                final Collection<String> dbHosts ) throws DatabaseDeletionException {                
    }
}
