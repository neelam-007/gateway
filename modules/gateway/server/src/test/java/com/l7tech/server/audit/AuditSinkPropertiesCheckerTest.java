package com.l7tech.server.audit;

import com.l7tech.gateway.common.audit.SystemMessages;
import com.l7tech.gateway.common.cluster.ClusterProperty;
import com.l7tech.server.ApplicationContexts;
import com.l7tech.server.ServerConfig;
import com.l7tech.server.ServerConfigStub;
import org.junit.Before;
import org.junit.Test;

import java.text.MessageFormat;
import java.util.List;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertTrue;
import static org.junit.Assert.assertFalse;

/**
 * @author ghuang
 */
public class AuditSinkPropertiesCheckerTest {
    private static final String TEST_POLICY_GUID = "testpolicyguid";
    private ServerConfigStub serverConfig = new ServerConfigStub();
    private AuditSinkPropertiesChecker propChecker;
    private boolean[] prevPropStatus = new boolean[2];

    @Before
    public void setUp() {
        serverConfig = new ServerConfigStub();
        propChecker = new AuditSinkPropertiesChecker(serverConfig);
        propChecker.setApplicationContext(ApplicationContexts.getTestApplicationContext());
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
        // 1.1 Internal Audit System Previous Status is disabled and the property is turned on now.
        prevPropStatus[0] = false; // The "Internal Audit System" Property Previous Status
        ClusterProperty prop = new ClusterProperty(ServerConfig.PARAM_AUDIT_SINK_ALWAYS_FALLBACK, "true");
        List<String> auditMessages = propChecker.checkAndAuditPropsStatus(prop, prevPropStatus, false);

        assertEquals("Internal Audit System started",
            MessageFormat.format(SystemMessages.AUDIT_SINK_START.getMessage(), "Internal Audit System"),
            auditMessages.get(0));

        // 1.2 Internal Audit System Previous Status is disabled and the Audit Sink Policy property is deleted.
        serverConfig.putProperty(ServerConfig.PARAM_AUDIT_SINK_POLICY_GUID, TEST_POLICY_GUID);
        prevPropStatus[0] = true;  // The "Audit Sink Policy" Property Previous Status
        prevPropStatus[1] = false; // The "Internal Audit System" Property Previous Status
        prop = new ClusterProperty(ServerConfig.PARAM_AUDIT_SINK_POLICY_GUID, null);
        auditMessages = propChecker.checkAndAuditPropsStatus(prop, prevPropStatus, true);

        assertEquals("Internal Audit System started",
            MessageFormat.format(SystemMessages.AUDIT_SINK_START.getMessage(), "Internal Audit System"),
            auditMessages.get(0));
        assertEquals("Audit Sink Policy disabled",
            MessageFormat.format(SystemMessages.AUDIT_SINK_DISABLED.getMessage(), "Audit Sink Policy"),
            auditMessages.get(1));

        // Case 2: "Internal Audit System disabled" is audited
        // Internal Audit System Previous Status is enabled and the property is turned off now.
        prevPropStatus[0] = true; // The "Internal Audit System" Property Previous Status
        prop = new ClusterProperty(ServerConfig.PARAM_AUDIT_SINK_ALWAYS_FALLBACK, "false");
        serverConfig.putProperty(ServerConfig.PARAM_AUDIT_SINK_POLICY_GUID, TEST_POLICY_GUID); // Since Internal Audit System will be disabled, then the Audit Sink Policy must be specified.
        auditMessages = propChecker.checkAndAuditPropsStatus(prop, prevPropStatus, false);

        assertEquals("Internal Audit System disabled",
            MessageFormat.format(SystemMessages.AUDIT_SINK_DISABLED.getMessage(), "Internal Audit System"),
            auditMessages.get(0));
    }

    @Test
    public void testAuditStatusOfAuditSinkPolicy() {
        // Case 1: "Audit Sink Policy started" is audited
        // 1.1 Audit Sink Policy Previous Status is disabled and the property is specified now.
        prevPropStatus[0] = false; // The "Audit Sink Policy" Property Previous Status
        ClusterProperty prop = new ClusterProperty(ServerConfig.PARAM_AUDIT_SINK_POLICY_GUID, TEST_POLICY_GUID);
        List<String> auditMessages = propChecker.checkAndAuditPropsStatus(prop, prevPropStatus, false);

        assertEquals("Audit Sink Policy started",
            MessageFormat.format(SystemMessages.AUDIT_SINK_START.getMessage(), "Audit Sink Policy"),
            auditMessages.get(0));

        // Case 2: "Audit Sink Policy disabled" is audited
        // Audit Sink Policy Previous Status is enabled and the Audit Sink Policy property is deleted.
        serverConfig.putProperty(ServerConfig.PARAM_AUDIT_SINK_POLICY_GUID, TEST_POLICY_GUID);
        prevPropStatus[0] = true;  // The "Audit Sink Policy" Property Previous Status
        prevPropStatus[1] = false; // The "Internal Audit System" Property Previous Status
        prop = new ClusterProperty(ServerConfig.PARAM_AUDIT_SINK_POLICY_GUID, null);
        auditMessages = propChecker.checkAndAuditPropsStatus(prop, prevPropStatus, true);

        assertEquals("Internal Audit System started",
            MessageFormat.format(SystemMessages.AUDIT_SINK_START.getMessage(), "Internal Audit System"),
            auditMessages.get(0));
        assertEquals("Audit Sink Policy disabled",
            MessageFormat.format(SystemMessages.AUDIT_SINK_DISABLED.getMessage(), "Audit Sink Policy"),
            auditMessages.get(1));
    }

    @Test
    public void testFallbackToInternal() {
        // Case 1: "Fall back on internal audit system is enabled" is audited
        // 1.1 Turn the property from off to on
        prevPropStatus[0] = false; // The "Fall back to Internal" Property Previous Status
        ClusterProperty prop = new ClusterProperty(ServerConfig.PARAM_AUDIT_SINK_FALLBACK_ON_FAIL, "true");
        List<String> auditMessages = propChecker.checkAndAuditPropsStatus(prop, prevPropStatus, false);
        assertEquals("Fall back on internal audit system enabled",
            MessageFormat.format(SystemMessages.AUDIT_SINK_FALL_BACK_STATUS.getMessage(), "enabled"),
            auditMessages.get(0));

        // 1.2 Delete the cluster property
        prevPropStatus[0] = false; // The "Fall back to Internal" Property Previous Status
        prop = new ClusterProperty(ServerConfig.PARAM_AUDIT_SINK_FALLBACK_ON_FAIL, null);
        auditMessages = propChecker.checkAndAuditPropsStatus(prop, prevPropStatus, true);
        assertEquals("Fall back on internal audit system enabled",
            MessageFormat.format(SystemMessages.AUDIT_SINK_FALL_BACK_STATUS.getMessage(), "enabled"),
            auditMessages.get(0));

        // Case 2: "Fall back on internal audit system is disabled" is audited
        prevPropStatus[0] = true;  // The "Fall back to Internal" Property Previous Status
        prop = new ClusterProperty(ServerConfig.PARAM_AUDIT_SINK_FALLBACK_ON_FAIL, "false");
        auditMessages = propChecker.checkAndAuditPropsStatus(prop, prevPropStatus, false);
        assertEquals("Fall back on internal audit system disabled",
            MessageFormat.format(SystemMessages.AUDIT_SINK_FALL_BACK_STATUS.getMessage(), "disabled"),
            auditMessages.get(0));
    }
}