/**
 * Copyright (C) 2008, Layer 7 Technologies Inc.
 * @author darmstrong
 */
package com.l7tech.server.policy;

import com.l7tech.policy.PolicyType;
import com.l7tech.server.MockClusterPropertyManager;
import com.l7tech.server.TestLicenseManager;
import com.l7tech.server.security.rbac.RbacServicesStub;
import com.l7tech.server.service.ServiceManagerStub;
import com.l7tech.test.BugNumber;
import org.junit.Assert;
import org.junit.Test;

public class PolicyManagerImplTest {

    @Test
    @BugNumber(10057)
    public void testDefaultAuditMessageFilterPolicyXml_Licensed(){

        final TestLicenseManager licenseManager = new TestLicenseManager();

        PolicyManagerImpl policyManager = new PolicyManagerImpl(null, null, null, licenseManager);

        final String amfDefaultXml = policyManager.getDefaultPolicyXml(PolicyType.INTERNAL, PolicyType.TAG_AUDIT_MESSAGE_FILTER);
        Assert.assertEquals("Default XML should have been returned", PolicyManagerImpl.DEFAULT_AUDIT_MESSAGE_FILTER_POLICY_XML, amfDefaultXml);

        final String avDefaultXml = policyManager.getDefaultPolicyXml(PolicyType.INTERNAL, PolicyType.TAG_AUDIT_VIEWER);
        Assert.assertEquals("Default XML should have been returned", PolicyManagerImpl.DEFAULT_AUDIT_VIEWER_POLICY_XML, avDefaultXml);
    }

    @Test
    @BugNumber(10057)
    public void testDefaultAuditMessageFilterPolicyXml_NotLicensed(){

        final TestLicenseManager licenseManager = new TestLicenseManager() {
            @Override
            public boolean isFeatureEnabled(String feature) {
                if (feature.endsWith("Unknown")) return false;
                return super.isFeatureEnabled(feature);
            }
        };

        PolicyManagerImpl policyManager = new PolicyManagerImpl(null, null, null, licenseManager);

        final String amfDefaultXml = policyManager.getDefaultPolicyXml(PolicyType.INTERNAL, PolicyType.TAG_AUDIT_MESSAGE_FILTER);
        Assert.assertEquals("Default XML should have been returned", PolicyManagerImpl.BACKUP_AUDIT_MESSAGE_FILTER_POLICY_XML, amfDefaultXml);

        final String avDefaultXml = policyManager.getDefaultPolicyXml(PolicyType.INTERNAL, PolicyType.TAG_AUDIT_VIEWER);
        Assert.assertEquals("Default XML should have been returned", PolicyManagerImpl.BACKUP_AUDIT_VIEWER_POLICY_XML, avDefaultXml);

    }
}
