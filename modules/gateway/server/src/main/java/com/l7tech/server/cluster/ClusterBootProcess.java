/*
 * Copyright (C) 2004 Layer 7 Technologies Inc.
 */
package com.l7tech.server.cluster;

import com.l7tech.objectmodel.FindException;
import com.l7tech.objectmodel.UpdateException;
import com.l7tech.server.LifecycleException;
import com.l7tech.server.ServerComponentLifecycle;
import com.l7tech.server.ServerConfig;
import com.l7tech.gateway.common.cluster.ClusterNodeInfo;

import java.util.Collection;
import java.util.Random;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.security.SecureRandom;

/**
 * @author alex
 */
public class ClusterBootProcess implements ServerComponentLifecycle {

    //- PUBLIC

    public ClusterBootProcess( final ClusterInfoManager clusterInfoManager,
                               final DistributedMessageIdManager distributedMessageIdManager,
                               final ServerConfig serverConfig ) {
        this.clusterInfoManager = clusterInfoManager;
        this.distributedMessageIdManager = distributedMessageIdManager;

        multicastAddress = serverConfig.getPropertyCached(ServerConfig.PARAM_MULTICAST_ADDRESS);
        if (multicastAddress != null && multicastAddress.length() == 0) multicastAddress = null;
    }

    public static class AddressAlreadyInUseException extends Exception {
        public AddressAlreadyInUseException(String address) {
            this.address = address;
        }

        public String getAddress() {
            return address;
        }

        private String address;
    }

    public void start() throws LifecycleException {
        try {
            ClusterNodeInfo myInfo = clusterInfoManager.getSelfNodeInf();
            clusterInfoManager.updateSelfUptime();
            Collection allNodes = clusterInfoManager.retrieveClusterStatus();

            if ( multicastAddress == null || multicastAddress.length() == 0 ) {
                for (Object allNode : allNodes) {
                    ClusterNodeInfo nodeInfo = (ClusterNodeInfo) allNode;
                    String nodeAddress = nodeInfo.getMulticastAddress();
                    if (nodeAddress != null) {
                        if (multicastAddress == null) {
                            logger.info("Found an existing cluster node with multicast address " + nodeAddress);
                            multicastAddress = nodeAddress;
                        } else if (!multicastAddress.equals(nodeAddress)) {
                            throw new LifecycleException("At least two nodes in database have different multicast addresses");
                        }
                    }
                }

                if (multicastAddress == null) {
                    multicastAddress = generateMulticastAddress();
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

    //- PACKAGE

    static String generateMulticastAddress() {
        StringBuffer addr = new StringBuffer();

        if (Boolean.getBoolean(PROP_OLD_MULTICAST_GEN)) {
            // old method ... not so random
            addr.append("224.0.7.");
            addr.append(Math.abs(random.nextInt() % 256));
        } else {
            // randomize an address from the 224.0.2.0 - 224.0.255.255 range
            addr.append("224.0.");
            int randomVal = 0;
            while (randomVal < 2) {
                randomVal = Math.abs(random.nextInt() % 256);
            }
            addr.append(randomVal);
            addr.append(".");
            addr.append(Math.abs(random.nextInt() % 256));
        }

        return addr.toString();
    }

    //- PRIVATE

    private static final String PROP_OLD_MULTICAST_GEN = "com.l7tech.cluster.macAddressOldGen"; // true for old
    private static final Random random = new SecureRandom();

    private final Logger logger = Logger.getLogger(getClass().getName());

    private final ClusterInfoManager clusterInfoManager;
    private final DistributedMessageIdManager distributedMessageIdManager;
    private String multicastAddress;
    private static final int PORT = 8777;

}
