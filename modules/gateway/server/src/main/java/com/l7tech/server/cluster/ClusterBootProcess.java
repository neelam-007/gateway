/*
 * Copyright (C) 2004 Layer 7 Technologies Inc.
 */
package com.l7tech.server.cluster;

import com.l7tech.common.io.InetAddressUtil;
import com.l7tech.objectmodel.FindException;
import com.l7tech.objectmodel.UpdateException;
import com.l7tech.server.LifecycleException;
import com.l7tech.server.ServerComponentLifecycle;
import com.l7tech.server.ServerConfig;
import com.l7tech.gateway.common.cluster.ClusterNodeInfo;
import com.l7tech.util.ExceptionUtils;

import java.net.*;
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
        if ( clusterInfoManager instanceof ClusterInfoManagerImpl ) throw new IllegalArgumentException("cim autoproxy failure");

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

    @Override
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
                    multicastAddress = generateMulticastAddress(myInfo);
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

    @Override
    public void stop() throws LifecycleException {
        try {
            distributedMessageIdManager.close();
        } catch ( Exception e ) {
            throw new LifecycleException("DistributedMessageIdManager couldn't shut down properly", e);
        }
    }

    @Override
    public void close() throws LifecycleException {
    }

    public String toString() {
        return "Cluster Boot Process";
    }

    //- PACKAGE

    static String generateMulticastAddress(ClusterNodeInfo selfInfo) throws LifecycleException {
        String address = selfInfo.getAddress();
        return InetAddressUtil.isValidIpv6Address(address) ? generateMulticastAddress6(address) : generateMulticastAddress4();
    }

    static String generateMulticastAddress4() {
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

    /**
     * Follow RFC3306 / RFC3307 guidelines to generate a multicast address based on the network prefix of the (unicast) IPv6 address.
     *
     * |   8    |  4 |  4 |   8    |    8   |       64       |    32    |
     * +--------+----+----+--------+--------+----------------+----------+
     * |11111111|flgs|scop|reserved|  plen  | network prefix | group ID |
     * |        |0011|    |00000000\        |                | 1[random]|
     * +--------+----+----+--------+--------+----------------+----------+
     *
     * @param address the unicast IPv6 address from which the multicast address is generated
     * @return a the IPv6 multicast address
     */
    static String generateMulticastAddress6(String address) throws LifecycleException {
        Inet6Address ipv6addr = InetAddressUtil.getIpv6Address(address);

        try {
            short prefixLength = -1;
            int scope = -1;
            NetworkInterface netIf = NetworkInterface.getByInetAddress(ipv6addr);
            for (InterfaceAddress ifAddr : netIf.getInterfaceAddresses() ) {
                if (ipv6addr.equals(ifAddr.getAddress())) {
                    prefixLength = ifAddr.getNetworkPrefixLength();
                    scope = getMulticastScope(ipv6addr);
                    break;
                }
            }

            if (prefixLength == -1 || scope == -1)
                throw new LifecycleException("Could not determine network prefix or scope for address " + address);

            if (prefixLength > 64) {
                logger.log(Level.WARNING, "Network prefix for address " + address + " is " + prefixLength + ", longer than 64bit; not a unicast address? Using 64 instead.");
                prefixLength = 64;
            }

            byte[] networkPrefix = InetAddressUtil.getNetworkPrefix(ipv6addr, prefixLength);
            if (networkPrefix.length > 8)
                throw new LifecycleException("Network prefix for address " + address + " is longer than 64bit; not a unicast address?");

            int groupId = random.nextInt() | 0x80000000;

            byte[] multicast = new byte[16];
            multicast[0] = (byte) 0xFF;
            multicast[1] = (byte) (0x30 | (0x0F & scope));
            multicast[2] =  0;
            multicast[3] = (byte) prefixLength;
            for(int i = 0; i < 8; i++) { // byte indexes 4 to 11
                multicast[4 + i] = i < networkPrefix.length ? networkPrefix[i] : 0;
            }
            multicast[12] = (byte) (groupId >>> 24);
            multicast[13] = (byte) (groupId >>> 16);
            multicast[14] = (byte) (groupId >>> 8);
            multicast[15] = (byte) groupId;

            InetAddress multicastIpv6 = Inet6Address.getByAddress(multicast);
            logger.log(Level.INFO, "Generated IPv6 multicast address: " + multicastIpv6.getHostAddress());
            return multicastIpv6.getHostAddress();

        } catch (SocketException e) {
            throw new LifecycleException("Could not determine network prefix for address: "  + address, ExceptionUtils.getDebugException(e));
        } catch (UnknownHostException e) {
            // shouldn't happen
            throw new LifecycleException("Could not generate multicast for address: "  + address, ExceptionUtils.getDebugException(e));
        }
    }

    /**
     * Returns the scope (one of global, site-local, link-local, host) to be used as the scope for a multicast address as defined by RFC3306.
     *
     * If the supplied IPv6 address is already a multicast address it's multicast scope is returned, otherwise
     * the scope is determined based on the high-order bits of the (unicast) IPv6 address per RFC4291.
     *
     * @param ipv6addr The IPv6 address to be used as the bassis for a RFC3306 multicast group ID
     * @return the multicast scope
     */
    static int getMulticastScope(Inet6Address ipv6addr) {
        return ipv6addr.isMulticastAddress() ? ipv6addr.getAddress()[1] & 0x0f :
               ipv6addr.isLoopbackAddress() ? 1 :   // host
               ipv6addr.isLinkLocalAddress() ? 2 :  // link
               ipv6addr.isSiteLocalAddress() ? 5 :  // site
               14;                                  // global
    }

    //- PRIVATE

    private static final String PROP_OLD_MULTICAST_GEN = "com.l7tech.cluster.macAddressOldGen"; // true for old
    private static final Random random = new SecureRandom();

    private static final Logger logger = Logger.getLogger(ClusterBootProcess.class.getName());

    private final ClusterInfoManager clusterInfoManager;
    private final DistributedMessageIdManager distributedMessageIdManager;
    private String multicastAddress;
    private static final int PORT = 8777;
}
