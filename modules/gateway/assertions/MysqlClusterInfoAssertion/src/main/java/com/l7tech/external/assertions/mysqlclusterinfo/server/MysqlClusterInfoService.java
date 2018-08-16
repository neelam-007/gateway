package com.l7tech.external.assertions.mysqlclusterinfo.server;

import com.ca.apim.gateway.extension.sharedstate.cluster.ClusterInfoService;
import com.ca.apim.gateway.extension.sharedstate.cluster.ClusterNodeSharedInfo;
import com.l7tech.gateway.common.cluster.ClusterNodeInfo;
import com.l7tech.objectmodel.FindException;
import com.l7tech.server.cluster.ClusterInfoManager;
import com.l7tech.util.Config;
import com.l7tech.util.TimeUnit;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.Level;
import java.util.logging.Logger;

public class MysqlClusterInfoService implements ClusterInfoService {
    public static final String KEY = "ssgdb";
    public static final String PARAM_CLUSTER_POLL_INTERVAL = "ratelimitClusterPollInterval";
    public static final String PARAM_CLUSTER_STATUS_INTERVAL = "ratelimitClusterStatusInterval";
    private static final long DEFAULT_CLUSTER_POLL_INTERVAL = TimeUnit.SECONDS.toMillis(43L); // Check every 43 seconds to see if cluster size has changed
    private static final long DEFAULT_CLUSTER_STATUS_INTERVAL = TimeUnit.SECONDS.toMillis(8L); // Nodes are considered "up" if they are the current node or if they have updated their status row within the last 8 seconds

    private static final Logger LOGGER = Logger.getLogger(MysqlClusterInfoService.class.getName());
    private long clusterStatusInterval;
    private long clusterPollInterval;
    private long lastPollTimeStamp;

    private ClusterInfoManager clusterInfoManager;
    private Config serverConfig;
    private Collection<ClusterNodeSharedInfo> activeNodes;
    private ReentrantLock lock;

    public MysqlClusterInfoService(ClusterInfoManager infoManager, Config serverConfig) {
        this.clusterInfoManager = infoManager;
        this.serverConfig = serverConfig;
        lock = new ReentrantLock();
    }

    @Override
    public Collection<ClusterNodeSharedInfo> getActiveNodes() {
        long now = Instant.now().toEpochMilli();
        clusterStatusInterval = this.serverConfig.getLongProperty(PARAM_CLUSTER_STATUS_INTERVAL, DEFAULT_CLUSTER_STATUS_INTERVAL);
        clusterPollInterval = this.serverConfig.getLongProperty(PARAM_CLUSTER_POLL_INTERVAL, DEFAULT_CLUSTER_POLL_INTERVAL);
        LOGGER.log(Level.FINE, "Cluster Poll Interval: " + clusterPollInterval + ", Cluster Status Interval: " + clusterStatusInterval);

        if (this.activeNodes == null) {
            lock.lock();
            fetchNodesFromDB(now);
            lock.unlock();
        } else if (isActiveNodesCacheOutdated(now) && lock.tryLock()) {
            fetchNodesFromDB(now);
            lock.unlock();
        }
        return activeNodes;
    }

    private void fetchNodesFromDB(long now) {
        if (!isActiveNodesCacheOutdated(now)) {
            return;
        }

        Collection<ClusterNodeInfo> nodes = Collections.emptyList();
        try {
            nodes = clusterInfoManager.retrieveClusterStatus();
            lastPollTimeStamp = now;
        } catch (FindException e) {
            activeNodes = null;
            lastPollTimeStamp = 0;
            LOGGER.log(Level.WARNING, "Cannot get nodes information", e);
        }

        Iterator<ClusterNodeInfo> it = nodes.iterator();
        activeNodes = new ArrayList<>();
        while (it.hasNext()) {
            ClusterNodeInfo info = it.next();
            if (isNodeActive(now, info)) {
                activeNodes.add(new ClusterNodeInfoWrapper(info));
            }
        }
    }

    private boolean isNodeActive(long now, ClusterNodeInfo info) {
        return now - info.getLastUpdateTimeStamp() <= clusterStatusInterval;
    }

    private boolean isActiveNodesCacheOutdated(long now) {
        long diff = now - lastPollTimeStamp;
        LOGGER.log(Level.FINE, "Cluster cache last time checked was " + diff + " ms ago.");
        return diff >= clusterPollInterval;
    }
}
