/*
 * Copyright (C) 2004 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.cluster;

import com.l7tech.objectmodel.FindException;
import com.l7tech.objectmodel.UpdateException;
import com.l7tech.server.LifecycleException;
import com.l7tech.server.ServerComponentLifecycle;
import com.l7tech.server.ServerConfig;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

import java.util.Collection;
import java.util.Iterator;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author alex
 * @version $Revision$
 */
public class ClusterBootProcess implements ServerComponentLifecycle, ApplicationContextAware {

    public static class AddressAlreadyInUseException extends Exception {
        public AddressAlreadyInUseException(String address) {
            this.address = address;
        }

        public String getAddress() {
            return address;
        }

        private String address;
    }

    public void setApplicationContext(ApplicationContext applicationContext) {
        if(this.applicationContext!=null) throw new IllegalStateException("applicationContext already initialized!");
        this.applicationContext = applicationContext;
    }

    public void setServerConfig( ServerConfig config ) throws LifecycleException {
        clusterInfoManager = (ClusterInfoManager)applicationContext.getBean("clusterInfoManager");
        distributedMessageIdManager = (DistributedMessageIdManager)applicationContext.getBean("distributedMessageIdManager");
        multicastAddress = config.getPropertyCached(ServerConfig.PARAM_MULTICAST_ADDRESS);
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
                    multicastAddress = ClusterInfoManagerImpl.generateMulticastAddress();
                    myInfo.setMulticastAddress(multicastAddress);
                    clusterInfoManager.updateSelfStatus(myInfo);
                }
            }

            logger.info("Initializing DistributedMessageIdManager");
            distributedMessageIdManager.initialize(multicastAddress, PORT, myInfo.getAddress());
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
            distributedMessageIdManager.close();
        } catch ( Exception e ) {
            throw new LifecycleException("DistributedMessageIdManager couldn't shut down properly", e);
        }
    }

    public void close() throws LifecycleException {
    }

    public String toString() {
        return "Cluster Boot Process";
    }

    public static final String CHANNEL_NAME = "com.l7tech.cluster.jgroupsChannel";

    private final Logger logger = Logger.getLogger(getClass().getName());

    private ApplicationContext applicationContext;
    private ClusterInfoManager clusterInfoManager;
    private DistributedMessageIdManager distributedMessageIdManager;
    private String multicastAddress;
    private static final int PORT = 8777;
}
