/**
 * Copyright (C) 2008, Layer 7 Technologies Inc.
 * @author darmstrong
 */
package com.l7tech.server.policy;

import com.l7tech.policy.PolicyType;
import com.l7tech.server.TestLicenseManager;
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
        PolicyManagerImpl policyManager = new PolicyManagerImpl(null, null, null, NO_UNKNOWN_LICENSE_MANAGER);

        final String amfDefaultXml = policyManager.getDefaultPolicyXml(PolicyType.INTERNAL, PolicyType.TAG_AUDIT_MESSAGE_FILTER);
        Assert.assertEquals("Default XML should have been returned", PolicyManagerImpl.FALLBACK_AUDIT_MESSAGE_FILTER_POLICY_XML, amfDefaultXml);

        final String avDefaultXml = policyManager.getDefaultPolicyXml(PolicyType.INTERNAL, PolicyType.TAG_AUDIT_VIEWER);
        Assert.assertEquals("Default XML should have been returned", PolicyManagerImpl.FALLBACK_AUDIT_VIEWER_POLICY_XML, avDefaultXml);
    }

    /**
     * Tests with default policy xml which contains core assertions. These are parsed into real assertions and not
     * 'Unknown'.
     * 
     * @throws Exception
     */
    @Test
    @BugNumber(10057)
    public void testAllLicensed() throws Exception {
        PolicyManagerImpl policyManager = new PolicyManagerImpl(null, null, null, NO_UNKNOWN_LICENSE_MANAGER);

        final String defaultXml = policyManager.getDefaultXmlBasedOnLicense(allLicensedXml, "", PolicyType.TAG_AUDIT_MESSAGE_FILTER);
        Assert.assertEquals("Default should be returned", allLicensedXml, defaultXml);
    }

    /**
     * Tests with default policy xml which contains core assertions and unknown modular assertions..
     *
     * @throws Exception
     */
    @Test
    @BugNumber(10057)
    public void testAllLicensedApartFromUnknown() throws Exception{
        PolicyManagerImpl policyManager = new PolicyManagerImpl(null, null, null, NO_UNKNOWN_LICENSE_MANAGER);

        final String fallbackXml = "fallback";
        final String defaultXml = policyManager.getDefaultXmlBasedOnLicense(
                nestedUnknownAssertion, fallbackXml, PolicyType.TAG_AUDIT_MESSAGE_FILTER);

        Assert.assertEquals("Fallback should be returned", fallbackXml, defaultXml);
    }

    private static final String allLicensedXml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
            "<wsp:Policy xmlns:L7p=\"http://www.layer7tech.com/ws/policy\" xmlns:wsp=\"http://schemas.xmlsoap.org/ws/2002/12/policy\">\n" +
            "    <wsp:All wsp:Usage=\"Required\">\n" +
            "        <L7p:SslAssertion/>\n" +
            "    </wsp:All>\n" +
            "</wsp:Policy>";

    private static final String nestedUnknownAssertion = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
            "<wsp:Policy xmlns:L7p=\"http://www.layer7tech.com/ws/policy\" xmlns:wsp=\"http://schemas.xmlsoap.org/ws/2002/12/policy\">\n" +
            "    <wsp:All wsp:Usage=\"Required\">\n" +
            "        <L7p:SslAssertion/>\n" +
            "        <wsp:OneOrMore wsp:Usage=\"Required\">\n" +
            "            <wsp:All wsp:Usage=\"Required\"/>\n" +
            "        </wsp:OneOrMore>\n" +
            "        <wsp:All wsp:Usage=\"Required\">\n" +
            "            <wsp:All wsp:Usage=\"Required\">\n" +
            "                <L7p:SslAssertion/>\n" +
            "        <L7p:EncodeDecode>\n" +
            "            <L7p:SourceVariableName stringValue=\"request.mainpart\"/>\n" +
            "            <L7p:TargetContentType stringValue=\"text/xml; charset=utf-8\"/>\n" +
            "            <L7p:TargetDataType variableDataType=\"message\"/>\n" +
            "            <L7p:TargetVariableName stringValue=\"request\"/>\n" +
            "            <L7p:TransformType transformType=\"BASE64_ENCODE\"/>\n" +
            "        </L7p:EncodeDecode>\n" +
            "            </wsp:All>\n" +
            "        </wsp:All>\n" +
            "    </wsp:All>\n" +
            "</wsp:Policy>";

    private static final TestLicenseManager NO_UNKNOWN_LICENSE_MANAGER = new TestLicenseManager() {
        @Override
        public boolean isFeatureEnabled(String feature) {
            if (feature.endsWith("Unknown")) return false;
            return super.isFeatureEnabled(feature);
        }
    };
    
}
