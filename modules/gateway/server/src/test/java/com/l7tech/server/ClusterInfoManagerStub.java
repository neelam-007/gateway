package com.l7tech.server;

import com.l7tech.objectmodel.UpdateException;
import com.l7tech.objectmodel.DeleteException;
import com.l7tech.objectmodel.FindException;
import com.l7tech.server.cluster.ClusterInfoManager;
import com.l7tech.gateway.common.cluster.ClusterNodeInfo;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.Collections;

/**
 *
 */
public class ClusterInfoManagerStub implements ClusterInfoManager {

    final ClusterNodeInfo cnf = new ClusterNodeInfo();
    {
        cnf.setNodeIdentifier("TestNode-main");
        cnf.setMac("00:0c:11:f0:43:01");
        cnf.setName("SSG1");
        cnf.setAddress("192.128.1.100");
        cnf.setAvgLoad(1.5);
        cnf.setBootTime(System.currentTimeMillis());
    }

    public String thisNodeId() {
        return "TestNode";
    }

    public void updateSelfStatus(double avgLoad) throws UpdateException {
    }

    public void updateSelfStatus(ClusterNodeInfo selfCI) throws UpdateException {
    }

    public void deleteNode(String nodeid) throws DeleteException {
    }

    public void renameNode(String nodeid, String newnodename) throws UpdateException {
    }

    public void updateSelfUptime() throws UpdateException {
    }

    @Transactional(readOnly = true)
    public Collection<ClusterNodeInfo> retrieveClusterStatus() throws FindException {
        return Collections.emptyList();
    }

    public ClusterNodeInfo getSelfNodeInf() {
        return cnf;
    }
}
