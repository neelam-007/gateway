package com.l7tech.gateway.common.audit;

import com.l7tech.policy.AssertionRegistry;
import com.l7tech.policy.Policy;
import com.l7tech.policy.PolicyType;
import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.wsp.WspConstants;
import com.l7tech.policy.wsp.WspReader;
import org.junit.Test;

import java.io.IOException;

import static com.l7tech.policy.wsp.WspReader.INCLUDE_DISABLED;

/**
 * And audit implementation for use in tests
 */
public class ExternalAuditPoliciesTest {
    private final String connectionName = "connection";
    private final String connectionDriverClass = "mysql";
    private final String auditRecordTable = "audit_main";
    private final String auditDetailTable = "audit_detail";
    private final WspReader wspReader;
    {
        final AssertionRegistry tmf = new AssertionRegistry();
        tmf.setApplicationContext(null);
        WspConstants.setTypeMappingFinder(tmf);
        wspReader = new WspReader(tmf);
    }

    @Test
    public void testCreateAuditSinkPolicy() throws IOException {
        Policy policy = new Policy(PolicyType.INTERNAL, "[Internal Audit Sink Policy]", null, false);
        String theXml = ExternalAuditsCommonUtils.makeDefaultAuditSinkPolicyXml(connectionName,auditRecordTable,auditDetailTable);
        policy.setXml(theXml);
        Assertion assertionPolicy = wspReader.parsePermissively( theXml, INCLUDE_DISABLED);
    }

    @Test
    public void testCreateAuditLookupPolicy() throws IOException {
        Policy policy = new Policy(PolicyType.INTERNAL, "[Internal Audit Lookup Policy]", null, false);
        String theXml = ExternalAuditsCommonUtils.makeDefaultAuditLookupPolicyXml(connectionName,connectionDriverClass,auditRecordTable,auditDetailTable);
        policy.setXml(theXml);
        Assertion assertionPolicy = wspReader.parsePermissively( theXml, INCLUDE_DISABLED);
    }


}
