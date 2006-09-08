/**
 * Copyright (C) 2006 Layer 7 Technologies Inc.
 */
package com.l7tech.cluster;

import com.l7tech.objectmodel.UpdateException;
import com.l7tech.objectmodel.DeleteException;
import com.l7tech.objectmodel.FindException;
import com.l7tech.common.security.rbac.Secured;
import com.l7tech.common.security.rbac.EntityType;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.annotation.Propagation;

import java.util.Collection;

/**
 * @author alex
 */
@Transactional(propagation=Propagation.REQUIRED, rollbackFor=Throwable.class)
@Secured(types=EntityType.CLUSTER_INFO)
public interface ClusterInfoManager {
    String thisNodeId();

    void updateSelfStatus(double avgLoad) throws UpdateException;

    void updateSelfStatus( ClusterNodeInfo selfCI ) throws UpdateException;

    void deleteNode(String nodeid) throws DeleteException;

    void renameNode(String nodeid, String newnodename) throws UpdateException;

    void updateSelfUptime() throws UpdateException;

    @Transactional(readOnly=true)
    Collection<ClusterNodeInfo> retrieveClusterStatus() throws FindException;

    ClusterNodeInfo getSelfNodeInf();
}
