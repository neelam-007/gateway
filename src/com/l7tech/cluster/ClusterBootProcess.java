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
import org.jgroups.*;

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

    /**
     * Ensures that the specified multicast address is available (i.e. that no other
     * cluster is running using that address).
     *
     * Remembers the channel created, because doing so is expensive.
     * @param address a multicast address (224.0.7.x) to use in establishing the channel
     * @throws AddressAlreadyInUseException if there appears to be another cluster already using the address
     * @throws ChannelException if the channel cannot be created or connected for whatever reason.
     */
    public synchronized void ensureAddressAvailable(String address) throws AddressAlreadyInUseException, ChannelException {
        JChannel channel = connectToChannel( address );
        Address me = channel.getLocalAddress();
        View view = channel.getView();
        for ( Iterator i = view.getMembers().iterator(); i.hasNext(); ) {
            Address member = (Address)i.next();
            if ( !(member.equals(me) ) ) {
                channel.disconnect();
                channel.close();
                throw new AddressAlreadyInUseException(member.toString());
            }
        }
        this.channel = channel;
    }

    private JChannel connectToChannel( String address ) throws ChannelException {
        String props = PROPERTIES_PREFIX + address + PROPERTIES_SUFFIX;
        JChannel channel = new JChannel(props);
        channel.connect(CHANNEL_NAME);
        return channel;
    }

    public void init( ComponentConfig config ) throws LifecycleException {
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
                    String addr = null;
                    while (true) {
                        addr = ClusterInfoManager.generateMulticastAddress();
                        logger.info("Generated multicast address " + addr);
                        try {
                            ensureAddressAvailable(addr);
                            break;
                        } catch ( AddressAlreadyInUseException e ) {
                            logger.log( Level.WARNING, "Cluster communication address " + addr + " appears to be in use by " + e.getAddress() +".  Will try a new address", e );
                            continue;
                        } catch ( ChannelException e ) {
                            logger.log( Level.SEVERE, "Cluster communications could not be established", e );
                            break;
                        }
                    }
                    myInfo.setMulticastAddress(addr);
                    clusterInfoManager.updateSelfStatus(myInfo);
                }
            }

            try {
                if ( channel == null ) channel = connectToChannel(multicastAddress);
            } catch ( ChannelException e ) {
                final String msg = "Unable to connect to configured cluster communications channel on address "  + multicastAddress;
                logger.log( Level.SEVERE, msg, e );
                throw new LifecycleException(msg, e);
            }

            channel.setChannelListener(new ChannelListener() {
                public void channelConnected( Channel channel ) {
                    logger.log(Level.INFO, "Connected to cluster communications channel" );
                }

                public void channelDisconnected( Channel channel ) {
                    logger.log(Level.INFO, "Disconnected from cluster communications channel" );
                }

                public void channelClosed( Channel channel ) {
                    logger.log(Level.INFO, "Cluster communications channel closed" );
                }

                public void channelShunned() {
                    logger.log(Level.INFO, "Shunned from cluster communications channel" );
                }

                public void channelReconnected( Address address ) {
                    logger.log(Level.INFO, "Reconnected to cluster communications channel" );
                }
            } );

            StatusUpdater.initialize();
        } catch (UpdateException e) {
            final String msg = "error updating boot time of node.";
            logger.log(Level.WARNING, msg, e);
            throw new LifecycleException(msg, e);
        } catch ( FindException e ) {
            logger.log(Level.WARNING, e.getMessage(), e );
            throw new LifecycleException(e.getMessage(), e);
        }
    }

    public void stop() throws LifecycleException {
        channel.disconnect();
    }

    public void close() throws LifecycleException {
        // if we were updating cluster status, stop doing it
        StatusUpdater.stopUpdater();
        channel.close();
    }

    public synchronized Channel getChannel() {
        return this.channel;
    }


    public static final String CHANNEL_NAME = "com.l7tech.cluster.jgroupsChannel";
    public static final String PROPERTIES_PREFIX = "UDP(mcast_addr=";
    public static final String PROPERTIES_SUFFIX =
            ";mcast_port=8777;ip_ttl=2):" +
            "PING(timeout=500;num_initial_members=2):" +
            "FD(timeout=500):" +
            "VERIFY_SUSPECT(timeout=500):" +
            "pbcast.NAKACK(gc_lag=10;retransmit_timeout=1000):" +
            "FC:" +
            "UNICAST(timeout=600,1200,2400,4800):" +
            "pbcast.STABLE(desired_avg_gossip=10000):" +
            "FRAG(frag_size=8096;down_thread=false;up_thread=false):" +
            "pbcast.GMS(join_timeout=500;join_retry_timeout=250;" +
            "shun=false;print_local_addr=true)";

    private final Logger logger = Logger.getLogger(getClass().getName());

    private JChannel channel;
    private ClusterInfoManager clusterInfoManager;
    private String multicastAddress;
}
