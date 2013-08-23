/**
 * Copyright (C) 2008, Layer 7 Technologies Inc.
 * @author darmstrong
 */
package com.l7tech.server.policy;

import com.l7tech.objectmodel.FindException;
import com.l7tech.objectmodel.Goid;
import com.l7tech.policy.Policy;
import com.l7tech.policy.PolicyAlias;
import com.l7tech.policy.PolicyType;
import com.l7tech.policy.PolicyVersion;
import com.l7tech.server.ApplicationContexts;
import com.l7tech.server.TestLicenseManager;
import com.l7tech.test.BugId;
import com.l7tech.test.BugNumber;
import com.l7tech.util.CollectionUtils;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.mockito.Mockito.*;


@RunWith(MockitoJUnitRunner.class)
public class PolicyAdminImplTest {
    private static final Goid GOID = new Goid(0,1234L);
    private PolicyAdminImpl policyAdmin;
    @Mock
    private PolicyAliasManager policyAliasManager;
    @Mock
    private PolicyManager policyManager;
    @Mock
    private PolicyVersionManager policyVersionManager;
    @Mock
    private EncapsulatedAssertionConfigManager encassConfigManager;
    @Mock
    private PolicyAssertionRbacChecker policyChecker;

    @Before
    public void setup() {
        policyAdmin = new PolicyAdminImpl(policyManager, policyAliasManager, null, policyVersionManager, null, null, null, null, null, null);
        ApplicationContexts.inject(policyAdmin, CollectionUtils.<String, Object>mapBuilder()
                .put("policyChecker", policyChecker)
                .put("encapsulatedAssertionConfigManager", encassConfigManager).unmodifiableMap(),
                false);
    }

    @Test
    @BugNumber(10057)
    public void testDefaultAuditMessageFilterPolicyXml_Licensed() {

        final TestLicenseManager licenseManager = new TestLicenseManager();

        PolicyAdminImpl policyManager = new PolicyAdminImpl(null, null, null, null, null, null, null, null, licenseManager, null);

        final String amfDefaultXml = policyManager.getDefaultPolicyXml(PolicyType.INTERNAL, PolicyType.TAG_AUDIT_MESSAGE_FILTER);
        Assert.assertEquals("Default XML should have been returned", PolicyAdminImpl.getDefaultAuditMessageFilterPolicyXml(null), amfDefaultXml);

        final String avDefaultXml = policyManager.getDefaultPolicyXml(PolicyType.INTERNAL, PolicyType.TAG_AUDIT_VIEWER);
        Assert.assertEquals("Default XML should have been returned", PolicyAdminImpl.DEFAULT_AUDIT_VIEWER_POLICY_XML, avDefaultXml);
    }

    @Test
    @BugNumber(10057)
    public void testDefaultAuditMessageFilterPolicyXml_NotLicensed() {
        PolicyAdminImpl policyManager = new PolicyAdminImpl(null, null, null, null, null, null, null, null, NO_UNKNOWN_LICENSE_MANAGER, null);

        final String amfDefaultXml = policyManager.getDefaultPolicyXml(PolicyType.INTERNAL, PolicyType.TAG_AUDIT_MESSAGE_FILTER);
        Assert.assertEquals("Default XML should have been returned", PolicyAdminImpl.FALLBACK_AUDIT_MESSAGE_FILTER_POLICY_XML, amfDefaultXml);

        final String avDefaultXml = policyManager.getDefaultPolicyXml(PolicyType.INTERNAL, PolicyType.TAG_AUDIT_VIEWER);
        Assert.assertEquals("Default XML should have been returned", PolicyAdminImpl.FALLBACK_AUDIT_VIEWER_POLICY_XML, avDefaultXml);
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
        PolicyAdminImpl policyManager = new PolicyAdminImpl(null, null, null, null, null, null, null, null, NO_UNKNOWN_LICENSE_MANAGER, null);

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
    public void testAllLicensedApartFromUnknown() throws Exception {
        PolicyAdminImpl policyManager = new PolicyAdminImpl(null, null, null, null, null, null, null, null, NO_UNKNOWN_LICENSE_MANAGER, null);

        final String fallbackXml = "fallback";
        final String defaultXml = policyManager.getDefaultXmlBasedOnLicense(
                nestedUnknownAssertion, fallbackXml, PolicyType.TAG_AUDIT_MESSAGE_FILTER);

        Assert.assertEquals("Fallback should be returned", fallbackXml, defaultXml);
    }

    @Test
    public void findByAlias() throws FindException {
        final Policy policy = new Policy(PolicyType.INCLUDE_FRAGMENT, "test", "test", false);
        policy.setGoid(new Goid(0,1L));
        final PolicyAlias alias = new PolicyAlias(policy, null);
        when(policyAliasManager.findByPrimaryKey(GOID)).thenReturn(alias);
        when(policyManager.findByPrimaryKey(new Goid(0,1L))).thenReturn(policy);
        assertEquals(policy, policyAdmin.findByAlias(new Goid(0,1234L)));
    }

    @Test
    public void findByAliasDoesNotExist() throws FindException {
        when(policyAliasManager.findByPrimaryKey(any(Goid.class))).thenReturn(null);
        assertNull(policyAdmin.findByAlias(new Goid(0,1234L)));
        verify(policyManager, never()).findByPrimaryKey(any(Goid.class));
    }

    @Test
    public void savePolicyChecksPolicyXmlIfChanged() throws Exception {
        final Goid goid = new Goid(0,1234L);
        final String guid = "abc1234";
        final Policy existing = new Policy(PolicyType.INCLUDE_FRAGMENT, "test", "existing", false);
        existing.setGoid(goid);
        existing.setGuid(guid);
        final Policy toUpdate = new Policy(PolicyType.INCLUDE_FRAGMENT, "test", "updated", false);
        toUpdate.setGoid(goid);
        toUpdate.setGuid(guid);
        when(policyManager.findByPrimaryKey(goid)).thenReturn(existing);
        when(policyVersionManager.checkpointPolicy(toUpdate, true, false)).thenReturn(new PolicyVersion());

        policyAdmin.savePolicy(toUpdate, true);
        verify(policyChecker).checkPolicy(toUpdate);
        verify(policyManager).update(toUpdate);
    }

    @BugId("SSG-7150")
    @Test
    public void savePolicyDoesNotCheckPolicyXmlIfNotChanged() throws Exception {
        final Goid goid = new Goid(0,1234L);
        final String guid = "abc1234";
        final Policy toUpdate = new Policy(PolicyType.INCLUDE_FRAGMENT, "test", "updated", false);
        toUpdate.setGoid(goid);
        toUpdate.setGuid(guid);
        // policy xml hasn't changed
        when(policyManager.findByPrimaryKey(goid)).thenReturn(toUpdate);
        when(policyVersionManager.checkpointPolicy(toUpdate, true, false)).thenReturn(new PolicyVersion());

        policyAdmin.savePolicy(toUpdate, true);
        verify(policyChecker, never()).checkPolicy(toUpdate);
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
            return !feature.endsWith("Unknown") && super.isFeatureEnabled(feature);
        }
    };

}
