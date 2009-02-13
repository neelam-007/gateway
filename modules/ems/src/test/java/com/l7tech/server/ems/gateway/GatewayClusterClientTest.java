package com.l7tech.server.ems.gateway;

import com.l7tech.objectmodel.Entity;
import com.l7tech.objectmodel.EntityHeader;
import com.l7tech.objectmodel.EntityType;
import com.l7tech.objectmodel.ExternalEntityHeader;
import com.l7tech.server.ems.enterprise.SsgCluster;
import com.l7tech.server.ems.enterprise.SsgNode;
import com.l7tech.server.management.api.node.GatewayApi;
import com.l7tech.server.management.api.node.MigrationApi;
import com.l7tech.server.management.migration.bundle.MigratedItem;
import com.l7tech.server.management.migration.bundle.MigrationBundle;
import com.l7tech.server.management.migration.bundle.MigrationMetadata;
import com.l7tech.test.BugNumber;
import static org.junit.Assert.*;
import org.junit.*;

import javax.xml.ws.ProtocolException;
import java.net.ConnectException;
import java.util.*;

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

            public MigrationApi getMigrationApi() {
                return new MigrationApi() {
                    public Collection<ExternalEntityHeader> listEntities(Class<? extends Entity> clazz) throws MigrationException {
                        return Arrays.asList(new ExternalEntityHeader("myexternalid", new EntityHeader()));
                    }

                    public Collection<ExternalEntityHeader> checkHeaders(Collection<ExternalEntityHeader> headers) {
                        return null;
                    }

                    public MigrationMetadata findDependencies(Collection<ExternalEntityHeader> headers) throws MigrationException {
                        return null;
                    }

                    public MigrationBundle exportBundle(Collection<ExternalEntityHeader> headers) throws MigrationException {
                        return null;
                    }

                    public Collection<MappingCandidate> retrieveMappingCandidates(Collection<ExternalEntityHeader> mappables, ExternalEntityHeader scope, String filter) throws MigrationException {
                        return null;
                    }

                    public Collection<MigratedItem> importBundle(MigrationBundle bundle, ExternalEntityHeader targetFolder, boolean flattenFolders, boolean overwriteExisting, boolean enableServices, boolean dryRun) throws MigrationException {
                        return null;
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
        blahfolder = new GatewayApi.EntityInfo(EntityType.FOLDER, UUID.randomUUID().toString(), UUID.randomUUID().toString(), null, "blahfolder", null, 1);
        blahfolderInfo = Collections.unmodifiableList(Arrays.asList(blahfolder));

        foofolder = new GatewayApi.EntityInfo(EntityType.FOLDER, UUID.randomUUID().toString(), UUID.randomUUID().toString(), null, "foofolder", null, 1);
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

        GatewayApi uncachedApi = cc.getUncachedGatewayApi();

        final GatewayApi.EntityInfo firstInfo = cc.getEntityInfo(Arrays.asList(EntityType.FOLDER)).iterator().next();
        assertTrue("folder returned", blahfolder == firstInfo);

        final GatewayApi.EntityInfo firstInfoUc = uncachedApi.getEntityInfo(Arrays.asList(EntityType.FOLDER)).iterator().next();
        assertTrue("folder returned", blahfolder == firstInfoUc);

        // Change gacking data on gateway
        entityInfoToReturn = foofolderInfo;

        final GatewayApi.EntityInfo firstInfo2 = cc.getEntityInfo(Arrays.asList(EntityType.FOLDER)).iterator().next();
        assertTrue("folder returned from cache, ingoring backing change", blahfolder == firstInfo2);

        final GatewayApi.EntityInfo firstInfoUc2 = uncachedApi.getEntityInfo(Arrays.asList(EntityType.FOLDER)).iterator().next();
        assertTrue("folder returned bypassing cache, seeing the backing change", foofolder == firstInfoUc2);

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
    public void testMigration() throws Exception {
        GatewayClusterClientImpl cc = new GatewayClusterClientImpl(fooCluster, nodeContexts);

        Collection<ExternalEntityHeader> got = cc.getUncachedMigrationApi().listEntities(null);
        assertEquals(got.iterator().next().getExternalId(), "myexternalid");
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
    
    @Test
    @BugNumber(6312)
    public void testBug6312ApiReturningNull() throws Exception {
        GatewayClusterClientImpl cc = new GatewayClusterClientImpl(fooCluster, nodeContexts);
        entityInfoToReturn = null;
        Collection<GatewayApi.EntityInfo> got = cc.getEntityInfo(Arrays.asList(EntityType.FOLDER));
        assertNotNull("getEntityInfo absorbs nulls returned by WS client", got);
        assertTrue("getEntityInfo translates nulls back into an empty collection", got.isEmpty());
    }
}
