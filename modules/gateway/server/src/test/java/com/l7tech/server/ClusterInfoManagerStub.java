package com.l7tech.server;

import com.l7tech.gateway.common.cluster.ClusterNodeInfo;
import com.l7tech.objectmodel.DeleteException;
import com.l7tech.objectmodel.FindException;
import com.l7tech.objectmodel.UpdateException;
import com.l7tech.server.cluster.ClusterInfoManager;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.Collections;

/**
 *
 */
public class ClusterInfoManagerStub implements ClusterInfoManager {

    final ClusterNodeInfo cnf = new ClusterNodeInfo();
    Collection<ClusterNodeInfo> clusterStatus = Collections.emptyList();

    {
        cnf.setNodeIdentifier("TestNode-main");
        cnf.setMac("00:0c:11:f0:43:01");
        cnf.setName("SSG1");
        cnf.setAddress("192.128.1.100");
        cnf.setAvgLoad(1.5);
        cnf.setBootTime(System.currentTimeMillis());
    }

    @Override
    public String thisNodeId() {
        return "TestNode";
    }

    @Override
    public void updateSelfStatus(double avgLoad) throws UpdateException {
    }

    @Override
    public void updateSelfStatus(ClusterNodeInfo selfCI) throws UpdateException {
    }

    @Override
    public String deleteNode(String nodeid) throws DeleteException {
        return null;
    }

    @Override
    public String renameNode(String nodeid, String newName) throws UpdateException {
        return null;
    }

    @Override
    public void updateSelfUptime() throws UpdateException {
    }

    @Override
    @Transactional(readOnly = true)
    public Collection<ClusterNodeInfo> retrieveClusterStatus() throws FindException {
        return clusterStatus;
    }

    public void setClusterStatus(Collection<ClusterNodeInfo> status) {
        clusterStatus = status;
    }

    @Override
    public ClusterNodeInfo getSelfNodeInf() {
        return getSelfNodeInf(false);
    }

    @Override
    public ClusterNodeInfo getSelfNodeInf( final boolean refresh ) {
        return cnf;
    }
}
