package com.l7tech.external.assertions.hazelcastembeddedprovider.server;

import com.hazelcast.core.Cluster;
import com.hazelcast.core.MemberAttributeEvent;
import com.hazelcast.core.MembershipEvent;
import com.hazelcast.core.MembershipListener;

import java.util.logging.Logger;

import static java.text.MessageFormat.format;
import static java.util.logging.Level.INFO;
import static org.apache.commons.collections.CollectionUtils.*;

/**
 * Implementation of {@link MembershipListener} to add logging when a gateway join, leave or modify its configuration within a cluster.
 */
public class ClusterMembershipListener implements MembershipListener {
    private static final Logger LOGGER = Logger.getLogger(ClusterMembershipListener.class.getName());

    @Override
    public void memberAdded(MembershipEvent membershipEvent) {
        logTaggedMessage(membershipEvent.getMember().localMember(), "Gateway is adding Hazelcast Member: {0}", membershipEvent.getMember().getAddress());
        printCurrentClusterState(membershipEvent);
    }

    @Override
    public void memberRemoved(MembershipEvent membershipEvent) {
        logTaggedMessage(membershipEvent.getMember().localMember(), "Gateway is removing Hazelcast Member: {0}", membershipEvent.getMember().getAddress());
        printCurrentClusterState(membershipEvent);
    }

    @Override
    public void memberAttributeChanged(MemberAttributeEvent memberAttributeEvent) {
        logTaggedMessage(memberAttributeEvent.getMember().localMember(), "Gateway is changing attribute of HazelcastMember: {0}", memberAttributeEvent.getMember().getAddress());
        printCurrentClusterState(memberAttributeEvent);
    }

    /**
     * Log a message, adding an extra suffix if the member related is the local one.
     */
    private static void logTaggedMessage(boolean localMember, String message, final Object ... params) {
        if (localMember){
            message = message + " (This node)";
        }
        final String finalMessage = message;
        LOGGER.log(INFO, () -> format(finalMessage, params));
    }

    private static void printCurrentClusterState(MembershipEvent membershipEvent) {
        Cluster cluster = membershipEvent.getCluster();
        if (cluster != null && isNotEmpty(cluster.getMembers())) {
            LOGGER.log(INFO, () -> format("Members in Cluster: {0}", cluster));
            cluster.getMembers().forEach(member -> logTaggedMessage(member.localMember(), "{0}", member.getAddress()));
        } else {
            LOGGER.log(INFO, () -> format("No more members registered in this cluster: {0}", cluster));
        }
    }
}