package com.l7tech.external.assertions.hazelcastembeddedprovider.server;

import com.hazelcast.config.Config;
import com.l7tech.gateway.common.cluster.ClusterNodeInfo;
import com.l7tech.objectmodel.FindException;
import com.l7tech.server.cluster.ClusterInfoManager;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.when;

/**
 * Test the HazelcastConfigBuilder.
 */
@RunWith(MockitoJUnitRunner.class)
public class HazelcastConfigBuilderTest {

    @Mock
    private ClusterInfoManager clusterInfoManager;

    @InjectMocks
    private HazelcastConfigBuilder sut;

    @Test
    public void testHazelcastAttemptsToJoinClusterInfoMembers() throws FindException {
        ClusterNodeInfo node1 = new ClusterNodeInfo();
        ClusterNodeInfo node2 = new ClusterNodeInfo();
        ClusterMembershipListener clusterMembershipListener = new ClusterMembershipListener();
        String ip1 = "1.1.1.1"; // NOSONAR
        String ip2 = "2.2.2.2"; // NOSONAR
        node1.setAddress(ip1);
        node2.setAddress(ip2);
        when(clusterInfoManager.retrieveClusterStatus()).thenReturn(Arrays.asList(node1, node2));

        Config config = sut
                .withPort(1)
                .withConnectionTimeout(1)
                .withGroupPassword("abc")
                .withProtocol(HazelcastConfigParams.Protocol.TCPIP)
                .withTcpIpMembers(Arrays.asList(ip1, ip2))
                .withMembershipListener(clusterMembershipListener)
                .build();
        List<String> membersAdded = config.getNetworkConfig().getJoin().getTcpIpConfig().getMembers();

        assertEquals(
                Arrays.asList(ip1, ip2),
                membersAdded
        );
    }

}
