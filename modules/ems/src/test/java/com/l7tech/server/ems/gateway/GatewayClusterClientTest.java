package com.l7tech.server.ems.gateway;

import com.l7tech.objectmodel.EntityType;
import com.l7tech.server.ems.enterprise.SsgCluster;
import com.l7tech.server.ems.enterprise.SsgNode;
import com.l7tech.server.management.api.node.GatewayApi;
import static org.junit.Assert.*;
import org.junit.*;

import javax.xml.ws.ProtocolException;
import java.util.*;
import java.net.ConnectException;

/**
 *
 */
public class GatewayClusterClientTest {
    private SsgCluster fooCluster;
    private List<GatewayContext> nodeContexts;
    private GatewayApi.EntityInfo blahfolder;
    private GatewayApi.EntityInfo foofolder;
    private Collection<GatewayApi.EntityInfo> blahfolderInfo;
    private Collection<GatewayApi.EntityInfo> foofolderInfo;
    private Collection<GatewayApi.EntityInfo> entityInfoToReturn;
    private GatewayApi.ClusterInfo clusterInfoToReturn;
    private Collection<GatewayApi.GatewayInfo> gatewayInfoToReturn;
    private int lastApiIdxContacted = -1; // each time this changes after the first time, we consider it a failover
    private int failoverAttemptsDetected = -1; // the first idx change doesn't count as a failover
    private Deque<RuntimeException> runtimeExceptionsToThrow = null;
    private Deque<GatewayApi.GatewayException> gatewayExceptionsToThrow = null;

    private SsgNode makeSsgNode(int idx) {
        SsgNode sn = new SsgNode();
        sn.setGuid(UUID.randomUUID().toString());
        sn.setName("Test node #" + idx);
        return sn;
    }

    private GatewayContext makeNodeContext(final int idx, String host) throws Exception {
        return new MockGatewayContext(UUID.randomUUID().toString(), host, "bob") {
            public GatewayApi getApi() {
                return new GatewayApi() {
                    private void onCall() {
                        if (lastApiIdxContacted != idx)
                            failoverAttemptsDetected++;
                        lastApiIdxContacted = idx;
                        if (runtimeExceptionsToThrow != null && !runtimeExceptionsToThrow.isEmpty())
                            throw runtimeExceptionsToThrow.pop();
                    }

                    public Collection<EntityInfo> getEntityInfo(Collection<EntityType> entityTypes) throws GatewayException {
                        onCall();
                        if (gatewayExceptionsToThrow != null && !gatewayExceptionsToThrow.isEmpty())
                            throw gatewayExceptionsToThrow.pop();
                        return entityInfoToReturn;
                    }

                    public ClusterInfo getClusterInfo() {
                        onCall();
                        return clusterInfoToReturn;
                    }

                    public Collection<GatewayInfo> getGatewayInfo() {
                        onCall();
                        return gatewayInfoToReturn;
                    }
                };
            }
        };
    }

    private GatewayApi.ClusterInfo makeClusterInfo() {
        final GatewayApi.ClusterInfo ci = new GatewayApi.ClusterInfo();
        ci.setClusterHostname("test-foo.nowhere.example.com");
        ci.setClusterHttpPort(8080);
        ci.setClusterHttpsPort(8443);
        return ci;
    }

    private Collection<GatewayApi.GatewayInfo> makeGatewayInfo() {
        ArrayList<GatewayApi.GatewayInfo> ret = new ArrayList<GatewayApi.GatewayInfo>();
        final GatewayApi.GatewayInfo gi = new GatewayApi.GatewayInfo();
        gi.setId(UUID.randomUUID().toString());
        gi.setName("test gateway node");
        ret.add(gi);
        return ret;
    }

    public GatewayClusterClientTest() throws Exception {
        blahfolder = new GatewayApi.EntityInfo(EntityType.FOLDER, UUID.randomUUID().toString(), "blahfolder", null, 1);
        blahfolderInfo = Collections.unmodifiableList(Arrays.asList(blahfolder));

        foofolder = new GatewayApi.EntityInfo(EntityType.FOLDER, UUID.randomUUID().toString(), "foofolder", null, 1);
        foofolderInfo = Collections.unmodifiableList(Arrays.asList(foofolder));

        fooCluster = new SsgCluster("foo", "test-foo.nowhere.example.com", 8443, null);
        Set<SsgNode> nodes = new LinkedHashSet<SsgNode>();
        nodes.add(makeSsgNode(1));
        nodes.add(makeSsgNode(2));
        nodes.add(makeSsgNode(3));
        fooCluster.setNodes(Collections.unmodifiableSet(nodes));

        List<GatewayContext> nodeContexts = new ArrayList<GatewayContext>();
        nodeContexts.add(makeNodeContext(1, "test-foo1.nowhere.example.com"));
        nodeContexts.add(makeNodeContext(2, "test-foo2.nowhere.example.com"));
        nodeContexts.add(makeNodeContext(3, "test-foo3.nowhere.example.com"));
        this.nodeContexts = Collections.unmodifiableList(nodeContexts);
    }

    @Before
    public void beforeTest() throws Exception {
        entityInfoToReturn = blahfolderInfo;
        gatewayExceptionsToThrow = null;
        runtimeExceptionsToThrow = null;
        clusterInfoToReturn = makeClusterInfo();
        gatewayInfoToReturn = makeGatewayInfo();
        lastApiIdxContacted = -1;
    }

    @Test
    public void testCache() throws Exception {
        GatewayClusterClientImpl cc = new GatewayClusterClientImpl(fooCluster, nodeContexts);
        assertTrue("cluster matches", fooCluster == cc.getCluster());
        assertTrue("results filtered", cc.getEntityInfo(Arrays.asList(EntityType.AUDIT_ADMIN)).isEmpty());

        final GatewayApi.EntityInfo firstInfo = cc.getEntityInfo(Arrays.asList(EntityType.FOLDER)).iterator().next();
        assertTrue("folder returned", blahfolder == firstInfo);

        // Change gacking data on gateway
        entityInfoToReturn = foofolderInfo;

        final GatewayApi.EntityInfo firstInfo2 = cc.getEntityInfo(Arrays.asList(EntityType.FOLDER)).iterator().next();
        assertTrue("folder returned from cache, ingoring backing change", blahfolder == firstInfo2);

        cc.clearCachedData();

        final GatewayApi.EntityInfo firstInfo3 = cc.getEntityInfo(Arrays.asList(EntityType.FOLDER)).iterator().next();
        assertTrue("new data visible via cache after cache flush", foofolder == firstInfo3);

        GatewayApi.ClusterInfo clust = cc.getClusterInfo();
        assertTrue(clust == clusterInfoToReturn);

        Collection<GatewayApi.GatewayInfo> gi = cc.getGatewayInfo();
        assertTrue("cluster info returned through cache", gatewayInfoToReturn.iterator().next() == gi.iterator().next());
    }

    @Test
    public void testFailover() throws Exception {
        GatewayClusterClientImpl cc = new GatewayClusterClientImpl(fooCluster, nodeContexts);

        GatewayApi.ClusterInfo clust = cc.getClusterInfo();
        assertTrue(clust == clusterInfoToReturn);

        assertEquals("No failover attempts made", 0, failoverAttemptsDetected);

        // First two call attempts shall fail, third will succeed
        runtimeExceptionsToThrow = new LinkedList<RuntimeException>(Arrays.asList(
                new ProtocolException(new ConnectException()),
                new ProtocolException(new ConnectException())
        ));

        cc.clearCachedData();

        clust = cc.getClusterInfo();
        assertTrue(clust == clusterInfoToReturn);

        assertEquals("Failover to third node", 2, failoverAttemptsDetected);
    }

    @Test
    @SuppressWarnings({"ThrowableInstanceNeverThrown"})
    public void testNonNetworkProtocolExceptionDoesNotTriggerFailover() throws Exception {
        GatewayClusterClientImpl cc = new GatewayClusterClientImpl(fooCluster, nodeContexts);

        final ProtocolException nonnet = new ProtocolException(new RuntimeException() {});
        runtimeExceptionsToThrow = new LinkedList<RuntimeException>(Arrays.asList(
                new ProtocolException(new ConnectException()), // should cause failover
                nonnet // should NOT cause failover
        ));

        try {
            cc.getClusterInfo();
        } catch (GatewayException e) {
            assertTrue("exception passed through", nonnet == e.getCause());
        }

        assertEquals("Only first ProtocolException, with network-related cause, triggers failover", 1, failoverAttemptsDetected);                
    }

    @Test
    public void testPassThroughApplicationException() throws Exception {
        GatewayClusterClientImpl cc = new GatewayClusterClientImpl(fooCluster, nodeContexts);
        final String msg = "this test must fail with an exception";
        gatewayExceptionsToThrow = new LinkedList<GatewayApi.GatewayException>(Arrays.asList(
                new GatewayApi.GatewayException(msg)
        ));

        try {
            cc.getEntityInfo(Arrays.asList(EntityType.FOLDER)).iterator().next();
            fail("Expected exception not thrown -- should have passed through GatewayApi.GatewayException as GatewayException");
        } catch (GatewayException e) {
            assertTrue(e.getMessage().endsWith(msg));
        }

        assertEquals("Failover not attempted for application-level exception", 0, failoverAttemptsDetected);
    }
}
