package com.l7tech.server.audit;

import com.l7tech.gateway.common.cluster.ClusterProperty;
import com.l7tech.server.ApplicationContexts;
import com.l7tech.server.ServerConfig;
import com.l7tech.server.ServerConfigStub;
import org.junit.Before;
import org.junit.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertTrue;
import static org.junit.Assert.assertFalse;

/**
 * @author ghuang
 */
public class AuditSinkPropertiesCheckerTest {
    private static final String TEST_POLICY_GUID = "testpolicyguid";
    private ServerConfigStub serverConfig = new ServerConfigStub();
    private AuditClusterPropertiesChecker propChecker;
    private Map<String, Boolean> propStatusMap;
    
    @Before
    public void setUp() {
        serverConfig = new ServerConfigStub();
        propChecker = new AuditClusterPropertiesChecker(serverConfig, null);
        propChecker.setApplicationContext(ApplicationContexts.getTestApplicationContext());
        propStatusMap = new HashMap<String, Boolean>();
    }

    @Test
    public void testEvaluateAuditSinkPropertiesStatus() {
        // Case 1:  Internal Audit System: enabled --- Audit Sink Policy: disabled
        // Set "audit.sink.policy.guid" as null (i.e., D.N.E --- does not exist), so the audit sink policy prop will be evaluated as disabled.
        // Since the audit sink policy prop is D.N.E, no matter what status the internal audit system is, the internal audit system will be evaluated to be enabled.

        serverConfig.putProperty(ServerConfig.PARAM_AUDIT_SINK_POLICY_GUID, null);
        assertFalse("Audit Sink Policy is disabled", propChecker.isAuditSinkPolicyEnabled());

        serverConfig.putProperty(ServerConfig.PARAM_AUDIT_SINK_ALWAYS_FALLBACK, "true");
        assertTrue(" Internal Audit System is enabled", propChecker.isInternalAuditSystemEnabled(true));

        serverConfig.putProperty(ServerConfig.PARAM_AUDIT_SINK_ALWAYS_FALLBACK, "false");
        assertTrue(" Internal Audit System is enabled", propChecker.isInternalAuditSystemEnabled(true));

        serverConfig.putProperty(ServerConfig.PARAM_AUDIT_SINK_ALWAYS_FALLBACK, null);
        assertTrue(" Internal Audit System is enabled", propChecker.isInternalAuditSystemEnabled(true));

        // Case 2:  Internal Audit System: disabled--- Audit Sink Policy: enabled
        serverConfig.putProperty(ServerConfig.PARAM_AUDIT_SINK_POLICY_GUID, TEST_POLICY_GUID);
        assertTrue("Audit Sink Policy is enabled", propChecker.isAuditSinkPolicyEnabled());

        serverConfig.putProperty(ServerConfig.PARAM_AUDIT_SINK_ALWAYS_FALLBACK, null);
        assertFalse("Internal Audit System is disabled", propChecker.isInternalAuditSystemEnabled(false));

        serverConfig.putProperty(ServerConfig.PARAM_AUDIT_SINK_ALWAYS_FALLBACK, "false");
        assertFalse("Internal Audit System is disabled", propChecker.isInternalAuditSystemEnabled(false));

        // Case 3:  Internal Audit System: enabled --- Audit Sink Policy: enabled
        serverConfig.putProperty(ServerConfig.PARAM_AUDIT_SINK_POLICY_GUID, TEST_POLICY_GUID);
        assertTrue("Audit Sink Policy is enabled", propChecker.isAuditSinkPolicyEnabled());

        serverConfig.putProperty(ServerConfig.PARAM_AUDIT_SINK_ALWAYS_FALLBACK, "true");
        assertTrue("Internal Audit System is enabled", propChecker.isInternalAuditSystemEnabled(false));
        assertTrue("Internal Audit System is enabled", propChecker.isInternalAuditSystemEnabled(true));
    }

    @Test
    public void testAuditStatusOfInternalAuditSystem() {
        // Case 1: "Internal Audit System started" is audited
        // 1.1 When Internal Audit System Previous Status is disabled, turn on the property now.
        propStatusMap.clear();
        propStatusMap.put(ServerConfig.PARAM_AUDIT_SINK_ALWAYS_FALLBACK, false); // The Previous Status of "Internal Audit System" Cluster Property
        propStatusMap.put(ServerConfig.PARAM_AUDIT_SINK_POLICY_GUID, true);      // The Current Status of "Audit Sink Policy" Cluster Property
        ClusterProperty prop = new ClusterProperty(ServerConfig.PARAM_AUDIT_SINK_ALWAYS_FALLBACK, "true");

        AuditClusterPropertiesChecker.AuditPropertyStatus propertyStatus = propChecker.getAndUpdatePropertyStatus(prop, propStatusMap, false);
        List<String> auditMessages = propChecker.checkAndAuditPropsStatus(propertyStatus);
        assertEquals("Internal Audit System started", AuditClusterPropertiesChecker.INTERNAL_AUDIT_SYSTEM_STARTED, auditMessages.get(0));

        // 1.2 When Internal Audit System Previous Status is disabled, deleting Audit Sink Policy property will enable Internal Audit System.
        propStatusMap.clear();
        propStatusMap.put(ServerConfig.PARAM_AUDIT_SINK_ALWAYS_FALLBACK, false); // The Previous Status of "Internal Audit System" Cluster Property
        propStatusMap.put(ServerConfig.PARAM_AUDIT_SINK_POLICY_GUID, true);       // The Previous Status of "Audit Sink Policy" Cluster Property
        prop = new ClusterProperty(ServerConfig.PARAM_AUDIT_SINK_POLICY_GUID, null);

        propertyStatus = propChecker.getAndUpdatePropertyStatus(prop, propStatusMap, true);
        auditMessages = propChecker.checkAndAuditPropsStatus(propertyStatus);

        assertEquals("Internal Audit System started", AuditClusterPropertiesChecker.INTERNAL_AUDIT_SYSTEM_STARTED, auditMessages.get(1));
        assertEquals("Audit Sink Policy disabled", AuditClusterPropertiesChecker.AUDIT_SINK_POLICY_DISABLED, auditMessages.get(0));

        // Case 2: "Internal Audit System disabled" is audited
        // 2.1 When Internal Audit System Previous Status is enabled, turn off the property now.
        propStatusMap.clear();
        propStatusMap.put(ServerConfig.PARAM_AUDIT_SINK_ALWAYS_FALLBACK, true); // The Previous Status of "Internal Audit System" Cluster Property
        propStatusMap.put(ServerConfig.PARAM_AUDIT_SINK_POLICY_GUID, true);     // The Current Status of "Audit Sink Policy" Cluster Property
        prop = new ClusterProperty(ServerConfig.PARAM_AUDIT_SINK_ALWAYS_FALLBACK, "false");

        propertyStatus = propChecker.getAndUpdatePropertyStatus(prop, propStatusMap, false);
        auditMessages = propChecker.checkAndAuditPropsStatus(propertyStatus);

        assertEquals("Internal Audit System disabled", AuditClusterPropertiesChecker.INTERNAL_AUDIT_SYSTEM_DISABLED, auditMessages.get(0));
    }

    @Test
    public void testAuditStatusOfAuditSinkPolicy() {
        // Case 1: "Audit Sink Policy started" is audited
        // 1.1 When Audit Sink Policy Previous Status is disabled, specify the property now.
        propStatusMap.clear();
        propStatusMap.put(ServerConfig.PARAM_AUDIT_SINK_ALWAYS_FALLBACK, true); // The Current Status of "Internal Audit System" Property
        propStatusMap.put(ServerConfig.PARAM_AUDIT_SINK_POLICY_GUID, false);    // The Previous Status of "Audit Sink Policy" Cluster Property
        ClusterProperty prop = new ClusterProperty(ServerConfig.PARAM_AUDIT_SINK_POLICY_GUID, TEST_POLICY_GUID);

        AuditClusterPropertiesChecker.AuditPropertyStatus propertyStatus = propChecker.getAndUpdatePropertyStatus(prop, propStatusMap, false);
        List<String> auditMessages = propChecker.checkAndAuditPropsStatus(propertyStatus);

        assertEquals("Audit Sink Policy started", AuditClusterPropertiesChecker.AUDIT_SINK_POLICY_STARTED, auditMessages.get(0));

        // Case 2: "Audit Sink Policy disabled" is audited
        // 2.1 When Audit Sink Policy Previous Status is enabled, delete the property now.
        propStatusMap.clear();
        propStatusMap.put(ServerConfig.PARAM_AUDIT_SINK_ALWAYS_FALLBACK, false); // The Previous Status of "Internal Audit System" Cluster Property
        propStatusMap.put(ServerConfig.PARAM_AUDIT_SINK_POLICY_GUID, true);      // The Previous Status of "Audit Sink Policy" Cluster Property
        prop = new ClusterProperty(ServerConfig.PARAM_AUDIT_SINK_POLICY_GUID, null);

        propertyStatus = propChecker.getAndUpdatePropertyStatus(prop, propStatusMap, true);
        auditMessages = propChecker.checkAndAuditPropsStatus(propertyStatus);

        assertEquals("Internal Audit System started", AuditClusterPropertiesChecker.INTERNAL_AUDIT_SYSTEM_STARTED, auditMessages.get(1));
        assertEquals("Audit Sink Policy disabled", AuditClusterPropertiesChecker.AUDIT_SINK_POLICY_DISABLED, auditMessages.get(0));
    }

    @Test
    public void testFallbackToInternal() {
        // Case 1: "Fall back on internal audit system is enabled" is audited
        // 1.1 Turn the property from off to on
        propStatusMap.clear();
        propStatusMap.put(ServerConfig.PARAM_AUDIT_SINK_FALLBACK_ON_FAIL, false); // The Previous Status of "Fall back to Internal" Property
        ClusterProperty prop = new ClusterProperty(ServerConfig.PARAM_AUDIT_SINK_FALLBACK_ON_FAIL, "true");

        AuditClusterPropertiesChecker.AuditPropertyStatus propertyStatus = propChecker.getAndUpdatePropertyStatus(prop, propStatusMap, false);
        List<String> auditMessages = propChecker.checkAndAuditPropsStatus(propertyStatus);

        assertEquals("Fall back on internal audit system enabled", AuditClusterPropertiesChecker.AUDIT_SINK_FALL_BACK_ENABLED, auditMessages.get(0));

        // 1.2 Delete the cluster property
        propStatusMap.put(ServerConfig.PARAM_AUDIT_SINK_FALLBACK_ON_FAIL, false); // The Previous Status of "Fall back to Internal" Property
        prop = new ClusterProperty(ServerConfig.PARAM_AUDIT_SINK_FALLBACK_ON_FAIL, null);

        propertyStatus = propChecker.getAndUpdatePropertyStatus(prop, propStatusMap, true);
        auditMessages = propChecker.checkAndAuditPropsStatus(propertyStatus);

        assertEquals("Fall back on internal audit system enabled", AuditClusterPropertiesChecker.AUDIT_SINK_FALL_BACK_ENABLED, auditMessages.get(0));

        // Case 2: "Fall back on internal audit system is disabled" is audited
        propStatusMap.put(ServerConfig.PARAM_AUDIT_SINK_FALLBACK_ON_FAIL, true); // The Previous Status of "Fall back to Internal" Property
        prop = new ClusterProperty(ServerConfig.PARAM_AUDIT_SINK_FALLBACK_ON_FAIL, "false");

        propertyStatus = propChecker.getAndUpdatePropertyStatus(prop, propStatusMap, false);
        auditMessages = propChecker.checkAndAuditPropsStatus(propertyStatus);

        assertEquals("Fall back on internal audit system disabled", AuditClusterPropertiesChecker.AUDIT_SINK_FALL_BACK_DISABLED, auditMessages.get(0));

        // Case 3: No status change will not audit.  For example, when the previous status is enabled, delete the property now.
        propStatusMap.put(ServerConfig.PARAM_AUDIT_SINK_FALLBACK_ON_FAIL, true); // The Previous Status of "Fall back to Internal" Property
        prop = new ClusterProperty(ServerConfig.PARAM_AUDIT_SINK_FALLBACK_ON_FAIL, null);

        propertyStatus = propChecker.getAndUpdatePropertyStatus(prop, propStatusMap, true);
        auditMessages = propChecker.checkAndAuditPropsStatus(propertyStatus);

        assertEquals("No status change will not audit", 0, auditMessages.size());
    }
}