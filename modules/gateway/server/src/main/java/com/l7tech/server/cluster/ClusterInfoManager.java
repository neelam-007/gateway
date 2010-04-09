/**
 * Copyright (C) 2006 Layer 7 Technologies Inc.
 */
package com.l7tech.server.cluster;

import com.l7tech.objectmodel.UpdateException;
import com.l7tech.objectmodel.DeleteException;
import com.l7tech.objectmodel.FindException;
import com.l7tech.gateway.common.cluster.ClusterNodeInfo;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.annotation.Propagation;

import java.util.Collection;

/**
 * @author alex
 */
@Transactional(propagation=Propagation.REQUIRED, rollbackFor=Throwable.class)
public interface ClusterInfoManager {
    String thisNodeId();

    void updateSelfStatus(double avgLoad) throws UpdateException;

    void updateSelfStatus( ClusterNodeInfo selfCI ) throws UpdateException;

    /**
     * Delete a node.
     *
     * @param nodeid The identifier of the node to delete.
     * @return The name of the deleted node.
     * @throws DeleteException If an error occurs.
     */
    String deleteNode(String nodeid) throws DeleteException;

    /**
     * Rename a node.
     *
     * @param nodeid The identifier of the node to rename.
     * @param newName The new name for the node.
     * @return The old name for the node.
     * @throws UpdateException If an error occurs.
     */
    String renameNode(String nodeid, String newName) throws UpdateException;

    void updateSelfUptime() throws UpdateException;

    /**
     * @return a collection containing ClusterNodeInfo objects. if the collection is empty, it means that
     * the SSG operated by itself outsides a cluster.
     * @throws com.l7tech.objectmodel.FindException if there is an error reading from the database
     */
    @Transactional(readOnly=true)
    Collection<ClusterNodeInfo> retrieveClusterStatus() throws FindException;

    /**
     * determines this node's nodeid value
     * @return the node info for the current node.
     */
    ClusterNodeInfo getSelfNodeInf();
}
