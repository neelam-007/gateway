package com.l7tech.server;

import com.ca.apim.gateway.extension.sharedstate.cluster.ClusterInfoService;
import com.ca.apim.gateway.extension.sharedstate.cluster.ClusterNodeSharedInfo;
import com.l7tech.gateway.common.cluster.ClusterNodeInfo;
import com.l7tech.objectmodel.DeleteException;
import com.l7tech.objectmodel.FindException;
import com.l7tech.objectmodel.UpdateException;
import com.l7tech.server.cluster.ClusterInfoManager;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;

/**
 *
 */
public class ClusterInfoManagerStub implements ClusterInfoManager, ClusterInfoService {

    public static final int CLUSTER_STATUS_INTERVAL = 8000;
    final ClusterNodeInfo cnf;

    private Collection<ClusterNodeInfo> clusterStatus;

    public ClusterInfoManagerStub() {
        cnf = new ClusterNodeInfo();
        cnf.setNodeIdentifier("TestNode-main");
        cnf.setMac("00:0c:11:f0:43:01");
        cnf.setName("SSG1");
        cnf.setAddress("192.128.1.100");
        cnf.setAvgLoad(1.5);
        cnf.setBootTime(System.currentTimeMillis());
        cnf.setLastUpdateTimeStamp(System.currentTimeMillis());

        clusterStatus = Arrays.asList(cnf);
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
    public void removeStaleNodes(long statusTimeStamp) {
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
    public ClusterNodeInfo getSelfNodeInf(final boolean refresh) {
        return cnf;
    }

    @Override
    public Collection<ClusterNodeSharedInfo> getActiveNodes() {
        long now = System.currentTimeMillis();
        cnf.setLastUpdateTimeStamp(now);
        Iterator<ClusterNodeInfo> it = clusterStatus.iterator();
        Collection<ClusterNodeSharedInfo> nodes = new ArrayList<>();
        while (it.hasNext()) {
            ClusterNodeInfo info = it.next();
            if (now - info.getLastUpdateTimeStamp() <= CLUSTER_STATUS_INTERVAL) {
                nodes.add(new ClusterNodeSharedInfo() {
                    @Override
                    public String getNodeIdentifier() {
                        return info.getNodeIdentifier();
                    }

                    @Override
                    public String getName() {
                        return info.getName();
                    }

                    @Override
                    public long getLastUpdateTimeStamp() {
                        return info.getLastUpdateTimeStamp();
                    }
                });
            }
        }

        return nodes;
    }
}
