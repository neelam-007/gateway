package com.l7tech.external.assertions.oauthinstaller;

import com.l7tech.common.io.XmlUtil;
import com.l7tech.gateway.common.AsyncAdminMethods;
import com.l7tech.policy.bundle.BundleInfo;
import com.l7tech.policy.bundle.BundleMapping;
import com.l7tech.policy.bundle.PolicyBundleDryRunResult;
import com.l7tech.server.ApplicationContexts;
import com.l7tech.server.event.wsman.DryRunInstallPolicyBundleEvent;
import com.l7tech.server.event.wsman.InstallPolicyBundleEvent;
import com.l7tech.server.policy.bundle.*;
import com.l7tech.test.BugNumber;
import com.l7tech.util.Pair;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationEventPublisher;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import java.io.ByteArrayOutputStream;
import java.util.*;

import static org.junit.Assert.*;

public class OAuthInstallerAdminImplTest {

    private final String baseName = "/com/l7tech/external/assertions/oauthinstaller/bundles/";

    @Ignore
    @Test
    public void testInstallOfAllBundleFolders() throws Exception {
//        for (String bundleName : ALL_BUNDLE_NAMES) {
//            testInstallFolders_NoneExist(bundleName);
//        }
        // OAuth_2_0
        //todo fix
//        testInstallFolders_NoneExist("ba525763-6e55-4748-9376-76055247c8b1");
    }

    /**
     * Validates that the correct number and type of spring events are published for a dry run.
     */
    @Test
    public void testValidateSpringRequestsForDryRun() throws Exception {
        final Map<String, Boolean> foundBundles = new HashMap<String, Boolean>();
        final Set<String> foundAuditEvents = new HashSet<String>();

        final OAuthInstallerAdminImpl admin = new OAuthInstallerAdminImpl(baseName, new ApplicationEventPublisher() {
            @Override
            public void publishEvent(ApplicationEvent applicationEvent) {
                // note this code will run on a separate thread to the actual test so failures here will not
                // be reported in junit

                if (applicationEvent instanceof DryRunInstallPolicyBundleEvent) {
                    DryRunInstallPolicyBundleEvent dryRunEvent = (DryRunInstallPolicyBundleEvent) applicationEvent;

                    foundBundles.put(dryRunEvent.getContext().getBundleInfo().getId(), true);
                    dryRunEvent.setProcessed(true);
                } else if (applicationEvent instanceof OAuthInstallerAdminImpl.OtkInstallationAuditEvent) {
                    final OAuthInstallerAdminImpl.OtkInstallationAuditEvent auditEvent = (OAuthInstallerAdminImpl.OtkInstallationAuditEvent) applicationEvent;
                    foundAuditEvents.add(auditEvent.getNote() + auditEvent.getAuditDetails().toString());
                }
            }
        });

        final AsyncAdminMethods.JobId<PolicyBundleDryRunResult> jobId = admin.dryRunOtkInstall(Arrays.asList("1c2a2874-df8d-4e1d-b8b0-099b576407e1",
                "ba525763-6e55-4748-9376-76055247c8b1"), new HashMap<String, BundleMapping>(), null);

        while (!admin.getJobStatus(jobId).startsWith("inactive")) {
            Thread.sleep(10L);
        }

        final AsyncAdminMethods.JobResult<PolicyBundleDryRunResult> dryRunResultJobResult = admin.getJobResult(jobId);

        final PolicyBundleDryRunResult dryRunResult = dryRunResultJobResult.result;

        assertNotNull(dryRunResult);

        assertEquals("Two bundles were configured so two events should have been published", 2, foundBundles.size());

        assertTrue(foundBundles.containsKey("1c2a2874-df8d-4e1d-b8b0-099b576407e1"));
        assertTrue(foundBundles.containsKey("ba525763-6e55-4748-9376-76055247c8b1"));

        // validate audits
        assertFalse("OTK Installation audits should have been generated.", foundAuditEvents.isEmpty());
        for (String foundAuditEvent : foundAuditEvents) {
            System.out.println(foundAuditEvent);
        }
        assertTrue(foundAuditEvents.contains("Pre installation check of the OAuth Toolkit started[]"));
        assertTrue(foundAuditEvents.contains("Pre installation check of the OAuth Toolkit completed[]"));

    }

    /**
     * Validates that the correct number and type of spring events are published for a dry run.
     */
    @Test
    public void testValidateSpringRequestsForInstall() throws Exception {

        final Set<String> foundAuditEvents = new HashSet<String>();
        final OAuthInstallerAdminImpl admin = new OAuthInstallerAdminImpl(baseName, new ApplicationEventPublisher() {
            @Override
            public void publishEvent(ApplicationEvent applicationEvent) {
                // note this code will run on a separate thread to the actual test so failures here will not
                // be reported in junit

                if (applicationEvent instanceof InstallPolicyBundleEvent) {
                    InstallPolicyBundleEvent installEvent = (InstallPolicyBundleEvent) applicationEvent;
                    installEvent.setProcessed(true);
                } else if (applicationEvent instanceof OAuthInstallerAdminImpl.OtkInstallationAuditEvent) {
                    final OAuthInstallerAdminImpl.OtkInstallationAuditEvent auditEvent = (OAuthInstallerAdminImpl.OtkInstallationAuditEvent) applicationEvent;
                    foundAuditEvents.add(auditEvent.getNote() + auditEvent.getAuditDetails().toString());
                }
            }
        });

        final AsyncAdminMethods.JobId<ArrayList> jobId = admin.installOAuthToolkit(Arrays.asList("1c2a2874-df8d-4e1d-b8b0-099b576407e1",
                "ba525763-6e55-4748-9376-76055247c8b1"), -5002, new HashMap<String, BundleMapping>(), null);

        while (!admin.getJobStatus(jobId).startsWith("inactive")) {
            Thread.sleep(10L);
        }

        final AsyncAdminMethods.JobResult<ArrayList> installResult = admin.getJobResult(jobId);
        final List<String> results = installResult.result;

        assertNotNull(results);
        assertEquals("Two bundles were configured so two events should have been published", 2, results.size());

        assertTrue(results.contains("1c2a2874-df8d-4e1d-b8b0-099b576407e1"));
        assertTrue(results.contains("ba525763-6e55-4748-9376-76055247c8b1"));

        // validate audits
        assertFalse("OTK Installation audits should have been generated.", foundAuditEvents.isEmpty());
        for (String foundAuditEvent : foundAuditEvents) {
            System.out.println(foundAuditEvent);
        }
        assertTrue(foundAuditEvents.contains("Installation of the OAuth Toolkit completed []"));
        assertTrue(foundAuditEvents.contains("Installation of the OAuth Toolkit started []"));

    }

    @Test
    public void testListAllBundles() throws Exception {
        final OAuthInstallerAdminImpl admin = new OAuthInstallerAdminImpl(baseName, ApplicationContexts.getTestApplicationContext());

        final List<BundleInfo> allBundles = admin.getAllOtkComponents();
        assertNotNull(allBundles);

        for (BundleInfo aBundle : allBundles) {
            System.out.println("Bundle: " + aBundle);
        }

        BundleInfo expected;

        expected = new BundleInfo("1c2a2874-df8d-4e1d-b8b0-099b576407e1", "1.0", "OAuth 1.0", "Core Services and Test Client");
        assertTrue(allBundles.contains(expected));

        expected = new BundleInfo("ba525763-6e55-4748-9376-76055247c8b1", "1.0", "OAuth 2.0", "Auth Server and Test Clients");
        assertTrue(allBundles.contains(expected));

        expected = new BundleInfo("f69c7d15-4999-4761-ab26-d29d58c0dd57", "1.0", "Secure Zone OVP", "OVP - OAuth Validation Point");
        assertTrue(allBundles.contains(expected));

        expected = new BundleInfo("b082274b-f00e-4fbf-bbb7-395a95ca2a35", "1.0", "Secure Zone Storage", "Token and Client Store");
        expected.addJdbcReference("OAuth");
        assertTrue(allBundles.contains(expected));

        expected = new BundleInfo("a07924c0-0265-42ea-90f1-2428e31ae5ae", "1.0", "OAuth Manager", "Manager utility for Client and Token store for OAuth 1.0 and 2.0");
        assertTrue(allBundles.contains(expected));
    }

    /**
     * The same policy is contained in more than one bundle. This test validates that all policies with the same guid
     * are logically equivalent.
     * If not then two policies / service could refer to the same policy with the same guid but receive an unexpected
     * implementation of that policy.
     */
    @Test
    public void testValidateTheSamePoliciesAreIdentical() throws Exception {

        final List<Pair<BundleInfo, String>> bundleInfos = BundleUtils.getBundleInfos(getClass(), baseName);
        final OAuthToolkitBundleResolver resolver = new OAuthToolkitBundleResolver(bundleInfos);
        final List<BundleInfo> allBundles = resolver.getResultList();

        // Collect all policies from each bundle
        Map<String, Map<String, Element>> bundleToAllPolicies = new HashMap<String, Map<String, Element>>();
        for (BundleInfo aBundle : allBundles) {
            final Map<String, Element> guidToElementMap = new HashMap<String, Element>();
            final Document policyDocument = resolver.getBundleItem(aBundle.getId(), BundleResolver.BundleItem.POLICY, false);
            final List<Element> enumPolicyElms = GatewayManagementDocumentUtilities.getEntityElements(policyDocument.getDocumentElement(), "Policy");
            for (Element policyElm : enumPolicyElms) {
                final String guid = policyElm.getAttribute("guid");
                guidToElementMap.put(guid, policyElm);
            }

            bundleToAllPolicies.put(aBundle.getId(), guidToElementMap);
        }

        // Organise all policies with the same guid
        Map<String, List<Element>> guidToPolicyElms = new HashMap<String, List<Element>>();

        for (Map.Entry<String, Map<String, Element>> entry : bundleToAllPolicies.entrySet()) {
            System.out.println("Bundle: " + entry.getKey());
            final Map<String, Element> value = entry.getValue();
            for (Map.Entry<String, Element> elementEntry : value.entrySet()) {
                final String guid = elementEntry.getKey();
                if (!guidToPolicyElms.containsKey(guid)) {
                    guidToPolicyElms.put(guid, new ArrayList<Element>());
                }
                guidToPolicyElms.get(guid).add(elementEntry.getValue());
                System.out.println("\t" + guid);
            }
        }

        // Validate all policies are identical
        for (Map.Entry<String, List<Element>> entry : guidToPolicyElms.entrySet()) {
            final String guid = entry.getKey();
            final List<Element> policyElms = entry.getValue();

            System.out.println("Guid: " + guid + " has " + policyElms.size() + " occurrences.");

            String canonicalXml = null;
            // Validate all policies for this guid are identical
            for (Element policyElm : policyElms) {
                final ByteArrayOutputStream byteOutExpected = new ByteArrayOutputStream();
                XmlUtil.canonicalize(policyElm, byteOutExpected);
                final String policyCanonical = new String(byteOutExpected.toByteArray());
                if (canonicalXml == null) {
                    canonicalXml = policyCanonical;
                }

                Assert.assertEquals("Inconsistent policy XML found for policy '" + guid + "'", canonicalXml, policyCanonical);
            }
        }
    }

    @Test
    @Ignore
    public void testGetId() throws Exception {

    }

    @Test
    @Ignore
    public void testResponse_PermissionDenied() throws Exception {
        //todo
        fail("Implement");
    }

    @Test
    @Ignore
    public void testAllFolderIdsAreTheSame() {
        fail("Test to ensure that all bundle names contain the same folder ids");
    }

    @Test
    @Ignore
    public void testHostnamesDoNotContainTraililngSlash() throws Exception {
        fail("Test to ensure no hostnames contain a trailing slash");
    }

    @Test
    public void testGetUpdatedHostValue() throws Exception {
        String test = "https://${host_target}${request.url.path}";
        String actual = OAuthInstallerAdminImpl.getUpdatedHostValue("version1", test);
        System.out.println(actual);
        assertEquals("https://${host_target}/version1${request.url.path}", actual);

        actual = OAuthInstallerAdminImpl.getUpdatedHostValue("version1", "https://${host_target}/auth/oauth/v1/token");
        System.out.println(actual);
        assertEquals("https://${host_target}/version1/auth/oauth/v1/token", actual);

        actual = OAuthInstallerAdminImpl.getUpdatedHostValue("version1", "${host_target}");
        assertNull(actual);

        actual = OAuthInstallerAdminImpl.getUpdatedHostValue("version1", "https://${host_target}");
        assertEquals("https://${host_target}/version1", actual);
    }

    /**
     * Test that each saved policy contains the comment for the version of the component and installer being used
     * to install the OTK.
     *
     * Note: If this test fails it may be because a new policy or service was added to the OAuth_1_0 bundle and the
     * count of policies or services found below needs to be updated.
     *
     * @throws Exception
     */
    @Test
    @BugNumber(13309)
    public void testCommentAddedToEachPolicyAndService() throws Exception {

        final int [] numPolicyCommentsFound = new int[1];
        final int [] numServiceCommentsFound = new int[1];

        final OAuthInstallerAdminImpl admin = new OAuthInstallerAdminImpl(baseName, new ApplicationEventPublisher() {
            @Override
            public void publishEvent(ApplicationEvent applicationEvent) {

                if (applicationEvent instanceof InstallPolicyBundleEvent) {
                    InstallPolicyBundleEvent installEvent = (InstallPolicyBundleEvent) applicationEvent;
                    final PreBundleSavePolicyCallback savePolicyCallback = installEvent.getPreBundleSavePolicyCallback();
                    if (savePolicyCallback == null) {
                        fail("Policy call back should be configured.");
                    }
                    final BundleInfo bundleInfo = installEvent.getContext().getBundleInfo();

                    try {
                        {
                            final Document policyEnum = installEvent.getBundleResolver().getBundleItem(bundleInfo.getId(), BundleResolver.BundleItem.POLICY, false);
                            final List<Element> gatewayMgmtPolicyElments = GatewayManagementDocumentUtilities.getEntityElements(policyEnum.getDocumentElement(), "Policy");
                            for (Element policyElement : gatewayMgmtPolicyElments) {

                                final Element policyResourceElmWritable = PolicyUtils.getPolicyResourceElement(policyElement, "Policy", "not used");
                                final Document policyResource = PolicyUtils.getPolicyDocumentFromResource(policyResourceElmWritable, "Policy", "not used");


                                savePolicyCallback.prePublishCallback(bundleInfo, "not important", policyResource);
                                verifyCommentAdded(policyResource);
                                numPolicyCommentsFound[0]++;
                            }
                        }

                        {
                            final Document serviceEnum = installEvent.getBundleResolver().getBundleItem(bundleInfo.getId(), BundleResolver.BundleItem.SERVICE, false);
                            final List<Element> gatewayMgmtPolicyElments = GatewayManagementDocumentUtilities.getEntityElements(serviceEnum.getDocumentElement(), "Service");
                            for (Element policyElement : gatewayMgmtPolicyElments) {

                                final Element policyResourceElmWritable = PolicyUtils.getPolicyResourceElement(policyElement, "Service", "not used");
                                final Document policyResource = PolicyUtils.getPolicyDocumentFromResource(policyResourceElmWritable, "Service", "not used");


                                savePolicyCallback.prePublishCallback(bundleInfo, "not important", policyResource);
                                verifyCommentAdded(policyResource);
                                numServiceCommentsFound[0]++;
                            }
                        }

                    } catch (Exception e) {
                        e.printStackTrace();
                        fail("Unexpected exception: " + e.getMessage());
                    }

                    installEvent.setProcessed(true);
                }

            }
        });

        final AsyncAdminMethods.JobId<ArrayList> jobId =
                admin.installOAuthToolkit(Arrays.asList("1c2a2874-df8d-4e1d-b8b0-099b576407e1"), -5002, new HashMap<String, BundleMapping>(), null);

        while (!admin.getJobStatus(jobId).startsWith("inactive")) {
            Thread.sleep(10L);
        }

        assertEquals("Incorrect number of policy comments found", 7, numPolicyCommentsFound[0]);
        assertEquals("Incorrect number of service comments found", 7, numServiceCommentsFound[0]);

    }

    @Test
    @BugNumber(13282)
    public void testGetDatabaseSchema() throws Exception {
        final OAuthInstallerAdminImpl admin = new OAuthInstallerAdminImpl(baseName, new ApplicationEventPublisher() {
            @Override
            public void publishEvent(ApplicationEvent applicationEvent) {

            }
        });

        final String dbSchema = admin.getOAuthDatabaseSchema();
        System.out.println(dbSchema);
        assertNotNull(dbSchema);
        assertFalse(dbSchema.trim().isEmpty());
    }

    @Ignore
    @Test
    public void testCommentReferencedFromDocs() throws Exception {
        // check for 'grant types' on all assertion in /auth/oauth/v2/token
        fail("Implement me");
    }

    private void verifyCommentAdded(Document policyResource) {
        // verify version was added
        final Element allElm = XmlUtil.findFirstChildElement(policyResource.getDocumentElement());
        final Element comment = XmlUtil.findFirstChildElement(allElm);
        assertTrue(comment.getLocalName().equals("CommentAssertion"));
        final Element commentValueElm = XmlUtil.findFirstChildElement(comment);
        final String commentValue = commentValueElm.getAttribute("stringValue");
        assertEquals("Invalid comment value found", "Component version 1.0 installed by OAuth installer version otk1.0", commentValue);
    }


    // - PRIVATE
    private final static List<Pair<BundleInfo, String>> ALL_BUNDLE_NAMES =
            Collections.unmodifiableList(
                    Arrays.asList(
                            new Pair<BundleInfo, String>(new BundleInfo("1c2a2874-df8d-4e1d-b8b0-099b576407e1", "1.0", "OAuth_1_0", "Desc"), "OAuth_1_0"),
                            new Pair<BundleInfo, String>(new BundleInfo("ba525763-6e55-4748-9376-76055247c8b1", "1.0", "OAuth_2_0", "Desc"), "OAuth_2_0"),
                            new Pair<BundleInfo, String>(new BundleInfo("f69c7d15-4999-4761-ab26-d29d58c0dd57", "1.0", "SecureZone_OVP", "Desc"), "SecureZone_OVP"),
                            new Pair<BundleInfo, String>(new BundleInfo("b082274b-f00e-4fbf-bbb7-395a95ca2a35", "1.0", "SecureZone_Storage", "Desc"), "SecureZone_Storage"),
                            new Pair<BundleInfo, String>(new BundleInfo("a07924c0-0265-42ea-90f1-2428e31ae5ae", "1.0", "StorageManager", "Desc"), "StorageManager")
                    ));

}
