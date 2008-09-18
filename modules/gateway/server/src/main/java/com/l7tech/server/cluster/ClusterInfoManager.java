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

    void deleteNode(String nodeid) throws DeleteException;

    void renameNode(String nodeid, String newnodename) throws UpdateException;

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
