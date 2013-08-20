package com.l7tech.server.ems.monitoring;

import com.l7tech.gateway.common.Component;
import com.l7tech.gateway.common.cluster.ClusterProperty;
import com.l7tech.objectmodel.Goid;
import com.l7tech.server.ems.EsmConfigParams;
import com.l7tech.server.ems.enterprise.JSONConstants;
import com.l7tech.server.ems.enterprise.MockSsgClusterManager;
import com.l7tech.server.ems.enterprise.SsgCluster;
import com.l7tech.server.ems.enterprise.SsgNode;
import com.l7tech.server.ems.gateway.GatewayException;
import com.l7tech.server.ems.gateway.MockGatewayContextFactory;
import com.l7tech.server.ems.gateway.MockProcessControllerContext;
import com.l7tech.server.ems.gateway.ProcessControllerContext;
import com.l7tech.server.event.EntityChangeSet;
import com.l7tech.server.event.admin.AdminEvent;
import com.l7tech.server.event.admin.Created;
import com.l7tech.server.event.admin.Updated;
import com.l7tech.server.event.system.Started;
import com.l7tech.server.management.api.monitoring.BuiltinMonitorables;
import com.l7tech.server.management.api.monitoring.MonitoringApi;
import com.l7tech.server.management.api.monitoring.MonitoringApiStub;
import com.l7tech.server.management.config.monitoring.MonitoringConfiguration;
import com.l7tech.server.management.config.monitoring.NotificationRule;
import com.l7tech.server.management.config.monitoring.Trigger;
import com.l7tech.test.BugNumber;
import com.l7tech.util.MockTimer;
import com.l7tech.util.Pair;
import com.l7tech.util.TestTimeSource;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.context.ApplicationEvent;

import javax.xml.ws.ProtocolException;
import java.io.IOException;
import java.net.ConnectException;
import java.util.*;

import static com.l7tech.server.ems.monitoring.MonitoringConfigurationSynchronizer.INITIAL_RETRY_DELAY;
import static com.l7tech.server.ems.monitoring.MonitoringConfigurationSynchronizer.MAX_RETRY_DELAY;
import static org.junit.Assert.*;

/**
 *
 */
public class MonitoringConfigurationSynchronizerTest {
    private MockTimer timer;
    private TestCluster clusterA;
    private TestCluster clusterB;
    private TestCluster[] testClusters;
    private MockSsgClusterManager ssgClusterManager;
    private TestTimeSource timeSource = new TestTimeSource();

    @Before
    public void setUp() throws Exception {
        timer = new MockTimer();
        timeSource.sync();
        clusterA = new TestCluster("A", 'A', 2);
        clusterB = new TestCluster("B", 'B', 3);
        testClusters = new TestCluster[] { clusterA, clusterB };
        ssgClusterManager = new MockSsgClusterManager(testClusters);
    }

    @After
    public void tearDown() {
        timer.cancel();
        timer.purge();
        timer = null;
    }

    @Test
    public void testPushdownAllOnStartup() {
        assertDoesCompletePushdownAfterEvent(makeMcs(), makeStartedEvent());
    }

    @Test
    public void testNoPushdownAfterIrrelevantEvent() {
        assertDoesNoPushdownAfterEvent(makeAndStartMcs(), makeIrrelevantEvent());
    }

    @Test
    public void testCompletePushdownAfterUpdatedClusterProperty() {
        assertDoesCompletePushdownAfterEvent(makeAndStartMcs(), makeClusterPropertyEvent());
    }

    @Test
    public void testCompletePushdownAfterUpdatedSystemMonitoringNotificationRule() {
        assertDoesCompletePushdownAfterEvent(makeAndStartMcs(), new Updated<SystemMonitoringNotificationRule>(new SystemMonitoringNotificationRule(null, null, null), null));
    }

    @Test
    public void testClusterPushdownAfterUpdatedEntityMonitoringPropertySetup() {
        assertDoesClusterPushdownAfterEvent(makeAndStartMcs(), new Updated<EntityMonitoringPropertySetup>(clusterA.node(1).monitorNodeCpu, null), clusterA);
    }

    @Test
    @BugNumber(6750)
    public void testClusterPushdownAfterUpdatedSsgNode() {
        assertDoesClusterPushdownAfterEvent(makeAndStartMcs(), makeUpdatedNodeEvent(clusterA.node(1), "ipAddress", "processControllerPort"), clusterA);
    }
    
    @Test
    @BugNumber(6798)
    public void testIgnoreNodeUpdateThatOnlyChangesAuditTime() {
        assertDoesClusterPushdownAfterEvent(makeAndStartMcs(), makeUpdatedNodeEvent(clusterA.node(1), "notificationAuditTime"));
    }

    @Test
    @BugNumber(6798)
    public void testDontIgnoreNodeUpdateThatChangesOtherPropertiesToo() {
        assertDoesClusterPushdownAfterEvent(makeAndStartMcs(), makeUpdatedNodeEvent(clusterA.node(1), "ipAddress", "notificationAuditTime", "processControllerPort"), clusterA);
    }

    @Test
    @BugNumber(6798)
    public void testDontIgnoreNodeUpdateThatMentionsNoProperties() {
        assertDoesClusterPushdownAfterEvent(makeAndStartMcs(), makeUpdatedNodeEvent(clusterA.node(1)), clusterA);
    }

    @Test
    @BugNumber(6750)
    public void testClusterPushdownAfterUpdatedSsgCluster() {
        assertDoesClusterPushdownAfterEvent(makeAndStartMcs(), new Updated<SsgCluster>(clusterB, null), clusterB);
    }

    @Test
    public void testClusterPushdownAfterUpdatedSsgClusterNotificationSetup() {
        assertDoesClusterPushdownAfterEvent(makeAndStartMcs(), new Updated<SsgClusterNotificationSetup>(clusterB.ssgClusterNotificationSetup, null), clusterB);
    }

    @Test
    @BugNumber(6750)
    public void testNewClustersTriggerPushdown() throws Exception {
        ssgClusterManager = new MockSsgClusterManager(clusterA);
        MonitoringConfigurationSynchronizer mcs = makeAndStartMcs();
        ssgClusterManager.save(clusterB);
        assertDoesClusterPushdownAfterEvent(mcs, new Created<SsgCluster>(clusterB), clusterB);
    }

    @Test
    @BugNumber(6750)
    public void testNewNodesTriggerPushdown() {
        MonitoringConfigurationSynchronizer mcs = makeAndStartMcs();
        SsgNode newNode = clusterA.addNode();
        assertDoesClusterPushdownAfterEvent(mcs, new Created<SsgNode>(newNode, null), clusterA);
    }

    @Test
    @BugNumber(6751)
    public void testRetryIfNodeDown() {
        MonitoringConfigurationSynchronizer mcs = makeAndStartMcs();
        clusterB.node(2).up = false;
        assertDoesCompletePushdownAfterEvent(mcs, makeClusterPropertyEvent());
        timeSource.advanceByMillis(MAX_RETRY_DELAY);
        assertDoesClusterPushdownAfterEvent(mcs, null, clusterB); // Should retry cluster B and only cluster B since it has a downed node
        clusterB.node(2).up = true;
        timeSource.advanceByMillis(MAX_RETRY_DELAY);
        assertDoesClusterPushdownAfterEvent(mcs, null, clusterB); // Should retry cluster B and only cluster B
        assertDoesClusterPushdownAfterEvent(mcs, null); // Should not do any pushdown now that clean config is everywhere
    }

    @Test
    @BugNumber(6680)
    public void testSetsComponentIdToClusterSslHostname() {
        MonitoringConfigurationSynchronizer mcs = makeAndStartMcs();
        EntityMonitoringPropertySetup setup = new EntityMonitoringPropertySetup(clusterA, BuiltinMonitorables.CPU_TEMPERATURE.getName());
        setup.setMonitoringEnabled(true);
        Trigger trigger = mcs.convertTrigger(Collections.<String, Object>emptyMap(), Collections.<Goid, NotificationRule>emptyMap(), setup, clusterA.getSslHostName());
        assertNotNull(trigger);
        assertEquals(clusterA.getSslHostName(), trigger.getComponentId());
    }
    
    @Test
    @BugNumber(6809)
    public void testPerClusterQuadraticBackoff() {
        long needDelay = INITIAL_RETRY_DELAY;

        MonitoringConfigurationSynchronizer mcs = makeAndStartMcs();
        clusterB.node(2).up = false;
        assertDoesCompletePushdownAfterEvent(mcs, makeClusterPropertyEvent());
        assertDoesClusterPushdownAfterEvent(mcs, null); // Should not retry cluster B yet, until initial delay has elapsed
        timeSource.advanceByMillis(1);
        assertDoesClusterPushdownAfterEvent(mcs, null); // Should not retry cluster B yet, until initial delay has elapsed
        timeSource.advanceByMillis(needDelay);
        assertDoesClusterPushdownAfterEvent(mcs, null, clusterB); // Should retry cluster B now that delay has elapsed
        needDelay *= 2;
        timeSource.advanceByMillis(needDelay);
        assertDoesClusterPushdownAfterEvent(mcs, null, clusterB); // Should retry cluster B now that the backoff timer has expired
        needDelay *= 2;
        timeSource.advanceByMillis(needDelay/2);
        assertDoesClusterPushdownAfterEvent(mcs, null); // Should NOT retry cluster yet
        timeSource.advanceByMillis(needDelay/2);
        assertDoesClusterPushdownAfterEvent(mcs, null, clusterB); // Now it should retry cluster B
        needDelay *= 2;
        clusterB.node(2).up = true;
        timeSource.advanceByMillis(needDelay);
        assertDoesClusterPushdownAfterEvent(mcs, null, clusterB); // Should retry cluster B and only cluster B
        needDelay *= 2;
        assertDoesClusterPushdownAfterEvent(mcs, null); // Should not do any pushdown now that clean config is everywhere
    }
    
    @Test
    @BugNumber(6809)
    public void testSuspendBackoffDelayForAdminAction() {
        MonitoringConfigurationSynchronizer mcs = makeAndStartMcs();
        clusterB.node(2).up = false;
        assertDoesCompletePushdownAfterEvent(mcs, makeClusterPropertyEvent());
        assertDoesClusterPushdownAfterEvent(mcs, null); // Should not retry cluster B yet, until initial delay has elapsed
        timeSource.advanceByMillis(1);
        assertDoesClusterPushdownAfterEvent(mcs, null); // Should not retry cluster B yet, until initial delay has elapsed
        timeSource.advanceByMillis(1);
        assertDoesClusterPushdownAfterEvent(mcs, makeUpdatedNodeEvent(clusterA.node(1)), clusterA, clusterB); // Should suspend retry timers for next attempt, due to user changing a node
        timeSource.advanceByMillis(1);
        assertDoesClusterPushdownAfterEvent(mcs, null); // Should continue to not retry cluster B yet -- backoff timer should be reinstated
        timeSource.advanceByMillis(INITIAL_RETRY_DELAY*2); // (2 attempts made)
        assertDoesClusterPushdownAfterEvent(mcs, null, clusterB); // Now should retry clusterB
    }

    /* Asserts tha the specified mcs does a pushdown to all known cluster nodes after the specified event. */
    private void assertDoesCompletePushdownAfterEvent(MonitoringConfigurationSynchronizer mcs, ApplicationEvent event) {
        assertDoesClusterPushdownAfterEvent(mcs, event, testClusters);
    }

    /* Asserts that the specfied mcs does no config pushdowns after the specified event. */
    private void assertDoesNoPushdownAfterEvent(MonitoringConfigurationSynchronizer mcs, ApplicationEvent event) {
        assertDoesClusterPushdownAfterEvent(mcs, event); // ie, no clusters expected to be reconfigured
    }

    /* Asserts that the specified mcs does config pushdown to all the specified clusters (and ONLY the specified clusters) after receiving the specified event, or just after the next timer tick if event is null. */
    private void assertDoesClusterPushdownAfterEvent(MonitoringConfigurationSynchronizer mcs, ApplicationEvent event, TestCluster... clusters) {
        Set<TestCluster> expectedReconfiguredClusters = new HashSet<TestCluster>(Arrays.asList(clusters));
        for (TestCluster cluster : testClusters) {
            for (TestNode node : cluster.nodes()) {
                node.pushdownAttemptSeen = false;
            }
        }

        if (event != null)
            mcs.onApplicationEvent(event);
        timer.runNext();

        for (TestCluster cluster : clusters) {
            for (TestNode node : cluster.nodes()) {
                assertTrue("Cluster " + cluster + " node " + node + " should have been reconfigured", node.pushdownAttemptSeen);
            }
        }

        for (TestCluster cluster : testClusters) {
            if (expectedReconfiguredClusters.contains(cluster))
                continue;
            for (TestNode node : cluster.nodes()) {
                assertFalse("Cluster " + cluster + " node " + node + " should not have been reconfigured", node.pushdownAttemptSeen);
            }
        }
    }

    /** @return a synchronizer configured with our test data that is started and has done the initial config pushdown. */
    private MonitoringConfigurationSynchronizer makeAndStartMcs() {
        MonitoringConfigurationSynchronizer mcs = makeMcs();
        mcs.onApplicationEvent(makeStartedEvent());
        timer.runNext(); // Do initial pushdowns
        return mcs;
    }

    /** @return a synchronizer configured with our test data, but not yet started. */
    private MonitoringConfigurationSynchronizer makeMcs() {
        return new MonitoringConfigurationSynchronizer(timer,
                timeSource,
                ssgClusterManager,
                new MockGatewayContextFactory() {
                    @Override
                    public ProcessControllerContext createProcessControllerContext(SsgNode node) throws GatewayException {
                        return new MockProcessControllerContext(null, ((TestNode)node).monitoringApi);
                    }
                },
                new MockSsgClusterNotificationSetupManager(),
                new MockEntityMonitoringPropertySetupManager(),
                new MockSystemMonitoringSetupSettingsManager(new Pair<String, Object>(JSONConstants.SystemMonitoringSetup.DISABLE_ALL_NOTIFICATIONS, Boolean.FALSE.toString()))
        );
    }

    private Started makeStartedEvent() {
        return new Started(this, Component.ENTERPRISE_MANAGER, null);
    }

    private AdminEvent makeIrrelevantEvent() {
        return new AdminEvent(this, "irrelevant") {};
    }

    private Updated<ClusterProperty> makeClusterPropertyEvent() {
        return new Updated<ClusterProperty>(new ClusterProperty(EsmConfigParams.PARAM_SYSTEM_MONITORING_SETUP_SETTINGS, "foo"), null);
    }

    private Updated<SsgNode> makeUpdatedNodeEvent(TestNode node, String... props) {
        Object[] oldvals = new Object[props.length];
        Object[] newvals = new Object[props.length];
        if (props.length > 0)
            newvals[0] = new Object();
        EntityChangeSet changes = new EntityChangeSet(props, oldvals, newvals);
        return new Updated<SsgNode>(node, changes);
    }

    /** Represents a mock cluster that creates zero or more mock cluster nodes for testing monitoring config pushdown. */
    private class TestCluster extends SsgCluster {
        final int baseIp;
        final List<TestNode> testNodes;

        SsgClusterNotificationSetup ssgClusterNotificationSetup;
        EntityMonitoringPropertySetup monitorClusterAudit = new EntityMonitoringPropertySetup(this, JSONConstants.SsgClusterMonitoringProperty.AUDIT_SIZE);

        private TestCluster(String name, int baseIp, int numNodes) {
            super(name, name + ".example.com", 8888, null);
            setGuid("cluster_" + name);
            this.baseIp = baseIp;
            setGoid(new Goid(0,baseIp));
            testNodes = new ArrayList<TestNode>();
            ssgClusterNotificationSetup = new SsgClusterNotificationSetup(getGuid());
            Set<SsgNode> nodes = new LinkedHashSet<SsgNode>();
            for (int i = 1; i <= numNodes; ++i) {
                TestNode node = createNode(i);
                nodes.add(node);
                testNodes.add(node);
            }
            setNodes(nodes);
        }

        private TestNode createNode(int i) {
            TestNode node = new TestNode(new Goid(0,1000 * baseIp + i));
            node.setName(getName() + i);
            node.setGuid("cluster_" + getName() + "_node_" + i);
            node.setSsgCluster(this);
            node.setIpAddress("10.69." + baseIp + "." + i);
            return node;
        }

        public SsgNode addNode() {
            TestNode node = createNode(testNodes.size() + 1);
            getNodes().add(node);
            testNodes.add(node);
            return node;
        }

        public List<TestNode> nodes() {
            return testNodes;
        }

        public TestNode node(int idx) {
            return testNodes.get(idx - 1);
        }

        @Override
        public String toString() {
            return getGuid();
        }
    }

    /** Represents a mock cluster node that owns some monitoring property setups for testing monitoring config pushdown. */
    private class TestNode extends SsgNode {
        boolean up = true;

        MonitoringConfiguration lastPushedMonitoringConfig;
        boolean pushdownAttemptSeen = false;

        EntityMonitoringPropertySetup monitorNodeCpu = new EntityMonitoringPropertySetup(this, JSONConstants.SsgNodeMonitoringProperty.CPU_USAGE);

        MonitoringApi monitoringApi = new MonitoringApiStub() {
            public void pushMonitoringConfiguration(MonitoringConfiguration config) throws IOException {
                pushdownAttemptSeen = true;
                if (!up) throw new ProtocolException(new ConnectException());
                lastPushedMonitoringConfig = config;
            }
        };

        private TestNode(Goid goid) {
            setGoid(goid);
        }

        public void setSsgCluster(SsgCluster ssgCluster) {
            super.setSsgCluster(ssgCluster);
            monitorNodeCpu.setSsgClusterNotificationSetup(((TestCluster)ssgCluster).ssgClusterNotificationSetup);
        }

        @Override
        public String toString() {
            return getGuid();
        }
    }
}