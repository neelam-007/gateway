/*
 * Copyright (C) 2004 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.cluster;

import com.l7tech.objectmodel.FindException;
import com.l7tech.objectmodel.UpdateException;
import com.l7tech.server.ComponentConfig;
import com.l7tech.server.LifecycleException;
import com.l7tech.server.ServerConfig;
import com.l7tech.server.TransactionalComponent;

import java.util.Collection;
import java.util.Iterator;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author alex
 * @version $Revision$
 */
public class ClusterBootProcess implements TransactionalComponent {
    public static class AddressAlreadyInUseException extends Exception {
        public AddressAlreadyInUseException(String address) {
            this.address = address;
        }

        public String getAddress() {
            return address;
        }

        private String address;
    }

    public void setComponentConfig( ComponentConfig config ) throws LifecycleException {
        clusterInfoManager = ClusterInfoManager.getInstance();
        multicastAddress = config.getProperty(ServerConfig.PARAM_MULTICAST_ADDRESS);
        if (multicastAddress != null && multicastAddress.length() == 0) multicastAddress = null;
    }

    public void start() throws LifecycleException {
        try {
            clusterInfoManager.updateSelfUptime();

            ClusterNodeInfo myInfo = clusterInfoManager.getSelfNodeInf();
            Collection allNodes = clusterInfoManager.retrieveClusterStatus();

            if ( multicastAddress == null || multicastAddress.length() == 0 ) {
                for ( Iterator i = allNodes.iterator(); i.hasNext(); ) {
                    ClusterNodeInfo nodeInfo = (ClusterNodeInfo)i.next();
                    String nodeAddress = nodeInfo.getMulticastAddress();
                    if (nodeAddress == null) {
                        continue;
                    } else if (multicastAddress == null) {
                        logger.info("Found an existing cluster node with multicast address " + nodeAddress);
                        multicastAddress = nodeAddress;
                    } else if (!multicastAddress.equals(nodeAddress)) {
                        throw new LifecycleException("At least two nodes in database have different multicast addresses");
                    }
                }

                if (multicastAddress == null) {
                    multicastAddress = ClusterInfoManager.generateMulticastAddress();
                    myInfo.setMulticastAddress(multicastAddress);
                    clusterInfoManager.updateSelfStatus(myInfo);
                }
            }

            StatusUpdater.initialize();

            logger.info("Initializing DistributedMessageIdManager");
            DistributedMessageIdManager.initialize(multicastAddress, PORT);
            logger.info("Initialized DistributedMessageIdManager");
        } catch (UpdateException e) {
            final String msg = "error updating boot time of node.";
            logger.log(Level.WARNING, msg, e);
            throw new LifecycleException(msg, e);
        } catch ( FindException e ) {
            logger.log(Level.WARNING, e.getMessage(), e );
            throw new LifecycleException(e.getMessage(), e);
        } catch (Exception e) {
            logger.log(Level.SEVERE, e.getMessage(), e );
            throw new LifecycleException(e.getMessage(), e);
        }
    }

    public void stop() throws LifecycleException {
        try {
            DistributedMessageIdManager.getInstance().close();
        } catch ( Exception e ) {
            throw new LifecycleException("DistributedMessageIdManager couldn't shut down properly");
        }
    }

    public void close() throws LifecycleException {
        // if we were updating cluster status, stop doing it
        StatusUpdater.stopUpdater();
    }

    public static final String CHANNEL_NAME = "com.l7tech.cluster.jgroupsChannel";
    public static final String PROPERTIES_PREFIX = "UDP(mcast_addr=";
    public static final String PROPERTIES_SUFFIX =
            ";mcast_port=8777;ip_ttl=32):" +
            "PING(timeout=1000;num_initial_members=2):" +
            "FD(timeout=5000):" +
            "VERIFY_SUSPECT(timeout=5000):" +
            "pbcast.NAKACK(gc_lag=10;retransmit_timeout=2000):" +
            "FC:" +
            "UNICAST(timeout=600,1200,2400,4800):" +
            "pbcast.STABLE(desired_avg_gossip=10000):" +
            "FRAG(frag_size=8096;down_thread=false;up_thread=false):" +
            "pbcast.GMS(join_timeout=2500;join_retry_timeout=1250;" +
                "shun=false;print_local_addr=true):" +
            "pbcast.STATE_TRANSFER:";

    private final Logger logger = Logger.getLogger(getClass().getName());

    private ClusterInfoManager clusterInfoManager;
    private String multicastAddress;
    private static final int PORT = 8777;
}
