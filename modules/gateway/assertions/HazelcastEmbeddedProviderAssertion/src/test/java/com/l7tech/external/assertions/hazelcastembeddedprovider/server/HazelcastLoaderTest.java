package com.l7tech.external.assertions.hazelcastembeddedprovider.server;

import com.ca.apim.gateway.extension.lifecycle.LifecycleException;
import com.hazelcast.config.Config;
import com.hazelcast.core.Hazelcast;
import com.l7tech.gateway.common.cluster.ClusterNodeInfo;
import com.l7tech.objectmodel.FindException;
import com.l7tech.server.GatewayState;
import com.l7tech.server.ServerConfig;
import com.l7tech.server.ServerConfigStub;
import com.l7tech.server.cluster.ClusterInfoManager;
import com.l7tech.server.extension.registry.lifecycle.LifecycleExtensionRegistry;
import com.l7tech.server.extension.registry.sharedstate.SharedKeyValueStoreProviderRegistry;
import com.l7tech.server.security.sharedkey.SharedKeyManager;
import com.l7tech.server.service.FirewallRulesManager;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.springframework.context.ApplicationContext;

import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.*;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.*;

/**
 * Unit tests for HazelcastLoader
 *
 */
@RunWith(MockitoJUnitRunner.class)
public class HazelcastLoaderTest {

    private ServerConfig serverConfig;

    private final String TEST_IP1 = "1.1.1.1";
    private final String TEST_IP2 = "2.2.2.2";

    private final String TEST_SYS_PROP1 = "1.1.1.1, 2.2.2.2";
    private final String TEST_SYS_PROP2 = "3.3.3.3, 4.4.4.4";
    private final String TEST_SYS_PROP3 = ", 2.2.2.2";
    private final String TEST_SYS_PROP4 = "2.2.2.2 , ";
    private final String TEST_SYS_PROP_INVALID_IP = "1.2.3.4, 3...3";
    private final List<String> TEST_IP_ARRAY_1 = Arrays.asList("1.1.1.1", "2.2.2.2");
    private final List<String> TEST_IP_ARRAY_2 = Arrays.asList("3.3.3.3", "4.4.4.4");

    @Mock
    private FirewallRulesManager firewallRulesManager;

    @Mock
    private SharedKeyManager sharedKeyManager;

    @Mock
    private ClusterInfoManager clusterInfoManager;

    @Mock
    private GatewayState gatewayState;

    @Mock
    private SharedKeyValueStoreProviderRegistry sharedKeyValueStoreProviderRegistry;

    @Mock
    private ApplicationContext context;

    private LifecycleExtensionRegistry lifecycleExtensionRegistry;

    private final String firewallRuleName = HazelcastConfigParams.DEFAULT_INSTANCE_NAME;
    private final Integer portUnderTest = 59999;

    @Before
    public void setUp() throws FindException {
        serverConfig = new ServerConfigStub();
        lifecycleExtensionRegistry = new LifecycleExtensionRegistry();
        doReturn(firewallRulesManager).when(context).getBean("ssgFirewallManager", FirewallRulesManager.class);
        doReturn(clusterInfoManager).when(context).getBean("clusterInfoManager", ClusterInfoManager.class);
        doReturn(gatewayState).when(context).getBean("gatewayState", GatewayState.class);
        doReturn(serverConfig).when(context).getBean("serverConfig", ServerConfig.class);
        doReturn(sharedKeyManager).when(context).getBean("sharedKeyManager", SharedKeyManager.class);
        doReturn(sharedKeyValueStoreProviderRegistry).when(context).getBean("sharedKeyValueStoreProviderRegistry", SharedKeyValueStoreProviderRegistry.class);
        doReturn(lifecycleExtensionRegistry).when(context).getBean("lifecycleExtensionRegistry", LifecycleExtensionRegistry.class);
        when(sharedKeyManager.getSharedKey()).thenReturn("sharedKey".getBytes());
        when(gatewayState.isReadyForMessages()).thenReturn(false);
        HazelcastLoader.onModuleLoaded(context);
    }

    /**
     * Called onModuleUnloaded after each test case to reset static singleton instance of
     * HazelcastLoader so it is not reused for future tests.
     */
    @After
    public void cleanUp() {
        HazelcastLoader.onModuleUnloaded();
        serverConfig.removeProperty(HazelcastConfigParams.CLUSTER_ADDRESSES_SYS_PROP);
    }

    @Test
    public void testValidHazelcastInstance() throws LifecycleException {
        lifecycleExtensionRegistry.getExtension(HazelcastLoader.class.getName()).start();
        assertNotNull(HazelcastLoader.getInstance());
        assertTrue(HazelcastLoader.getInstance().isRunning());
        assertEquals(1, Hazelcast.getAllHazelcastInstances().size());

        // verify extension is registered
        verify(sharedKeyValueStoreProviderRegistry).register(eq("embeddedhazelcast"),
                any(HazelcastEmbeddedSharedStateProvider.class));

        HazelcastLoader.onModuleUnloaded();
        verify(sharedKeyValueStoreProviderRegistry).unregister(eq("embeddedhazelcast"));
        assertNull(HazelcastLoader.getInstance());
        assertEquals(Hazelcast.getAllHazelcastInstances().size(), 0);
    }

    @Test
    public void testEnrollmentToClusterWhenClusterMemberSystemPropertyNotSet() throws FindException, LifecycleException {
        ClusterNodeInfo node1 = new ClusterNodeInfo();
        ClusterNodeInfo node2 = new ClusterNodeInfo();
        node1.setAddress(TEST_IP1);
        node2.setAddress(TEST_IP2);

        when(clusterInfoManager.retrieveClusterStatus()).thenReturn(Arrays.asList(node1, node2));
        lifecycleExtensionRegistry.getExtension(HazelcastLoader.class.getName()).start();
        Config config = HazelcastLoader.getInstance().getHazelcastConfig();
        List<String> membersAdded = config.getNetworkConfig().getJoin().getTcpIpConfig().getMembers();
        assertEquals(TEST_IP_ARRAY_1, membersAdded);
    }

    @Test
    public void testEnrollmentToClusterPropertySet() throws LifecycleException {
        serverConfig.putProperty(HazelcastConfigParams.CLUSTER_ADDRESSES_SYS_PROP, TEST_SYS_PROP1);
        lifecycleExtensionRegistry.getExtension(HazelcastLoader.class.getName()).start();
        Config config = HazelcastLoader.getInstance().getHazelcastConfig();
        List<String> membersAdded = config.getNetworkConfig().getJoin().getTcpIpConfig().getMembers();
        assertEquals(TEST_IP_ARRAY_1, membersAdded);
    }


    @Test
    public void testEnrollmentToClusterPropertySetStartingComma() {
        serverConfig.putProperty(HazelcastConfigParams.CLUSTER_ADDRESSES_SYS_PROP, TEST_SYS_PROP3);
        testErroreneousScenario();
    }

    @Test
    public void testEnrollmentToClusterPropertySetEndingComma() {
        serverConfig.putProperty(HazelcastConfigParams.CLUSTER_ADDRESSES_SYS_PROP, TEST_SYS_PROP4);
        testErroreneousScenario();
    }

    @Test
    public void testEnrollmentToClusterPropertySetInvalidIP() {
        serverConfig.putProperty(HazelcastConfigParams.CLUSTER_ADDRESSES_SYS_PROP, TEST_SYS_PROP_INVALID_IP);
        testErroreneousScenario();
    }

    @Test
    public void testEnrollmentToClusterPropertySetWithGatewayIP() throws FindException, LifecycleException {
        serverConfig.putProperty(HazelcastConfigParams.CLUSTER_ADDRESSES_SYS_PROP, TEST_SYS_PROP2);
        ClusterNodeInfo node1 = new ClusterNodeInfo();
        ClusterNodeInfo node2 = new ClusterNodeInfo();
        node1.setAddress(TEST_IP1);
        node2.setAddress(TEST_IP2);
        when(clusterInfoManager.retrieveClusterStatus()).thenReturn(Arrays.asList(node1, node2));
        lifecycleExtensionRegistry.getExtension(HazelcastLoader.class.getName()).start();
        Config config= HazelcastLoader.getInstance().getHazelcastConfig();
        List<String> membersAdded = config.getNetworkConfig().getJoin().getTcpIpConfig().getMembers();
        assertEquals(TEST_IP_ARRAY_2, membersAdded);
    }

    @Test
    public void testEnrollmentToClusterPropertySetEmpty() throws LifecycleException {
        serverConfig.putProperty(HazelcastConfigParams.CLUSTER_ADDRESSES_SYS_PROP, "");
        lifecycleExtensionRegistry.getExtension(HazelcastLoader.class.getName()).start();
        Config config= HazelcastLoader.getInstance().getHazelcastConfig();
        List<String> membersAdded = config.getNetworkConfig().getJoin().getTcpIpConfig().getMembers();
        assertTrue(membersAdded.isEmpty());
    }

    @Test
    public void testEnrollmentToClusterPropertySetEmptyWithGatewayIP() throws FindException, LifecycleException {
        serverConfig.putProperty(HazelcastConfigParams.CLUSTER_ADDRESSES_SYS_PROP, "");
        ClusterNodeInfo node1 = new ClusterNodeInfo();
        ClusterNodeInfo node2 = new ClusterNodeInfo();
        node1.setAddress(TEST_IP1);
        node2.setAddress(TEST_IP2);
        when(clusterInfoManager.retrieveClusterStatus()).thenReturn(Arrays.asList(node1, node2));
        lifecycleExtensionRegistry.getExtension(HazelcastLoader.class.getName()).start();
        Config config= HazelcastLoader.getInstance().getHazelcastConfig();
        List<String> membersAdded = config.getNetworkConfig().getJoin().getTcpIpConfig().getMembers();
        assertTrue(membersAdded.isEmpty());
    }

    @Test
    public void testFirewallOpenPortRuleIsCreatedOnSuccessfulModuleLoad() throws LifecycleException {
        serverConfig.putProperty(HazelcastConfigParams.NETWORK_PORT_SYS_PROP, portUnderTest.toString());
        lifecycleExtensionRegistry.getExtension(HazelcastLoader.class.getName()).start();

        verify(firewallRulesManager, times(1)).openPort(firewallRuleName, portUnderTest);
        verify(firewallRulesManager, never()).removeRule(firewallRuleName);
    }

    @Test
    public void testFirewallOpenPortRuleIsRemovedOnSuccessfulModuleUnload() throws LifecycleException {
        serverConfig.putProperty(HazelcastConfigParams.NETWORK_PORT_SYS_PROP, portUnderTest.toString());
        lifecycleExtensionRegistry.getExtension(HazelcastLoader.class.getName()).start();

        verify(firewallRulesManager, times(1)).openPort(firewallRuleName, portUnderTest);
        verify(firewallRulesManager, never()).removeRule(firewallRuleName);
    }

    @Test
    public void testModuleLoadFailsWhenSharedKeyNotAvailable() throws FindException {
        doThrow(new FindException()).when(sharedKeyManager).getSharedKey();
        testErroreneousScenario();
    }

    @Test
    public void testModuleLoadFailsWhenClusterInfoRetrievalFails() throws FindException {
        doThrow(new FindException()).when(clusterInfoManager).retrieveClusterStatus();
        testErroreneousScenario();
    }

    @Test
    public void testLifecycleExtensionRegistersSuccessfullyOnModuleLoad() {
        HazelcastLoader.onModuleLoaded(context);
        assertNotNull(lifecycleExtensionRegistry.getExtension(HazelcastLoader.class.getName()));
    }

    @Test
    public void testLifecycleExtensionUnregistersSuccessfullyOnModuleUnload() {
        testLifecycleExtensionRegistersSuccessfullyOnModuleLoad();
        HazelcastLoader.onModuleUnloaded();
        assertNull(lifecycleExtensionRegistry.getExtension(HazelcastLoader.class.getName()));
    }

    private void testErroreneousScenario() {
        boolean exceptionCaught = false;
        try {
            lifecycleExtensionRegistry.getExtension(HazelcastLoader.class.getName()).start();
        } catch(LifecycleException e) {
            exceptionCaught = true;
        }

        assertTrue(exceptionCaught);
        assertEquals(0, Hazelcast.getAllHazelcastInstances().size());
    }
}
