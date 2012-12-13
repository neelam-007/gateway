package com.l7tech.external.assertions.oauthinstaller;

import com.l7tech.common.io.XmlUtil;
import com.l7tech.gateway.common.AsyncAdminMethods;
import com.l7tech.gateway.common.audit.AuditDetail;
import com.l7tech.policy.bundle.BundleInfo;
import com.l7tech.policy.bundle.BundleMapping;
import com.l7tech.policy.bundle.PolicyBundleDryRunResult;
import com.l7tech.policy.variable.Syntax;
import com.l7tech.server.ApplicationContexts;
import com.l7tech.server.event.wsman.DryRunInstallPolicyBundleEvent;
import com.l7tech.server.event.wsman.InstallPolicyBundleEvent;
import com.l7tech.server.policy.bundle.*;
import com.l7tech.test.BugNumber;
import com.l7tech.util.Charsets;
import com.l7tech.util.HexUtils;
import com.l7tech.util.Pair;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationEventPublisher;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import java.io.ByteArrayOutputStream;
import java.util.*;

import static com.l7tech.server.policy.bundle.GatewayManagementDocumentUtilities.*;
import static org.junit.Assert.*;
import static org.junit.Assert.assertEquals;

/**
 * Test tests are concerned with logic relating to either the contents of the OTK bundles or logic which is controlled
 * by this module e.g. updating of policy xml before being published.
 */
public class OAuthInstallerAdminImplTest {

    private final String baseName = "/com/l7tech/external/assertions/oauthinstaller/bundles/";

    //todo test - validate that each service in an enumeration contains a unique id.
    // todo test coverage for reacahibility of folders

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
     * Validates that the correct number and type of spring events are published for an installation.
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

    /**
     * Test that all expected bundles are found and contain the correct values.
     * This will need to be updated as version numbers change and / or bundles get added..
     *
     * @throws Exception
     */
    @Test
    public void testListAllBundles() throws Exception {
        final OAuthInstallerAdminImpl admin = new OAuthInstallerAdminImpl(baseName, ApplicationContexts.getTestApplicationContext());

        final List<BundleInfo> allBundles = admin.getAllOtkComponents();
        assertNotNull(allBundles);

        for (BundleInfo aBundle : allBundles) {
            System.out.println("Bundle: " + aBundle);
        }

        assertEquals("Incorrect number of bundles found.", 5, allBundles.size());
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
     * There are no dependencies across bundles. Any policy referenced from a service or policy must exist in the bundle.
     */
    @Test
    public void testAllIncludesAreWithinTheBundle() throws Exception {
        final List<Pair<BundleInfo, String>> bundleInfos = BundleUtils.getBundleInfos(getClass(), baseName);
        final OAuthToolkitBundleResolver resolver = new OAuthToolkitBundleResolver(bundleInfos);
        final List<BundleInfo> allBundles = resolver.getResultList();

        for (BundleInfo aBundle : allBundles) {
            final Document policyDocument = resolver.getBundleItem(aBundle.getId(), BundleResolver.BundleItem.POLICY, false);
            final List<Element> enumPolicyElms = getEntityElements(policyDocument.getDocumentElement(), "Policy");

            // Record all policies defined in this bundle.
            final Map<String, String> guidToPolicyNameMatch = new HashMap<String, String>();
            // Record all includes for a policy
            final Map<String, Set<String>> policyGuidToPolicyRefMap = new HashMap<String, Set<String>>();
            // Record all included policies
            final Map<String, String> policyIncludeToReferantPolicyMap = new HashMap<String, String>();
            for (Element policyElm : enumPolicyElms) {
                final String policyGuid = policyElm.getAttribute("guid");
                final Element policyDetailElm = GatewayManagementDocumentUtilities.getPolicyDetailElement(policyElm);
                final String policyDetailGuid = policyDetailElm.getAttribute("guid");
                assertEquals("Invalid Gateway Mgmt Policy XML. Guids must be the same.", policyGuid, policyDetailGuid);
                final String policyName = getEntityName(policyDetailElm);
                guidToPolicyNameMatch.put(policyGuid, policyName);

                final Element policyResourceElement = getPolicyResourceElement(policyElm, "Policy", "Not used");
                final Document layer7Policy = getPolicyDocumentFromResource(policyResourceElement, "Policy", "Not used");
                final List<Element> policyIncludes = PolicyUtils.getPolicyIncludes(layer7Policy);
                Set<String> allIncludes = new HashSet<String>();
                for (Element policyInclude : policyIncludes) {
                    final String includeGuid = policyInclude.getAttribute("stringValue");
                    allIncludes.add(includeGuid);
                    policyIncludeToReferantPolicyMap.put(includeGuid, policyGuid);
                }
                policyGuidToPolicyRefMap.put(policyGuid, allIncludes);
            }

            // check for policy referenced but not defined
            for (Map.Entry<String, String> includeToPolicyEntry : policyIncludeToReferantPolicyMap.entrySet()) {
                assertTrue("Included policy #{" + includeToPolicyEntry.getKey() + "} from policy #{" + includeToPolicyEntry.getValue() + "}" +
                        " is not defined in bundle " + aBundle.getName(), policyGuidToPolicyRefMap.containsKey(includeToPolicyEntry.getKey()));
            }

            // validate all services policy references.
            final Document serviceDocument = resolver.getBundleItem(aBundle.getId(), BundleResolver.BundleItem.SERVICE, false);
            final List<Element> enumServiceElms = getEntityElements(serviceDocument.getDocumentElement(), "Service");
            // all that matters is that the policy exists if referenced, does not matter if we don't record all the services that may reference a policy
            final Map<String, String> policyGuidToServiceRefMap = new HashMap<String, String>();

            for (Element enumServiceElm : enumServiceElms) {
                final Element serviceDetail = getServiceDetailElement(enumServiceElm);
                final String serviceName = getEntityName(serviceDetail);

                final Element policyResourceElement = getPolicyResourceElement(enumServiceElm, "Service", "Not used");
                final Document layer7Policy = getPolicyDocumentFromResource(policyResourceElement, "Policy", "Not used");
                final List<Element> policyIncludes = PolicyUtils.getPolicyIncludes(layer7Policy);
                for (Element policyInclude : policyIncludes) {
                    final String policyGuid = policyInclude.getAttribute("stringValue");
                    policyGuidToServiceRefMap.put(policyGuid, serviceName);
                }
            }

            // check for missing references.
            for (Map.Entry<String, String> guidToServiceEntry : policyGuidToServiceRefMap.entrySet()) {
                assertTrue("Policy with guid #{" + guidToServiceEntry.getKey() + "} referenced from serivce '" +
                        guidToServiceEntry.getValue() + "' does not exist in bundle " + aBundle.getName(),
                        guidToPolicyNameMatch.containsKey(guidToServiceEntry.getKey()));
            }

            // check for any unused policy includes e.g. policy defined but not referenced
            for (Map.Entry<String, String> entry : guidToPolicyNameMatch.entrySet()) {
                if (!policyGuidToServiceRefMap.containsKey(entry.getKey())) {
                    // check if a policy includes it (note: circular includes are not allowed on the gateway)
                    boolean found = false;
                    for (Map.Entry<String, Set<String>> policyIncludeEntry : policyGuidToPolicyRefMap.entrySet()) {
                        if (policyIncludeEntry.getValue().contains(entry.getKey())) {
                            found = true;
                        }
                    }
                    assertTrue("Policy with guid #{" + entry.getKey() + "} '" + entry.getValue() + "' is not referenced" +
                            "from any service or policy in bundle " + aBundle.getName(), found);
                }
            }
        }
    }

    /**
     * The same policy is contained in more than one bundle. This test validates that all policies with the same name
     * are logically equivalent. Policy names are unique on the Gateway.
     * If not then two policies / service could refer to the same policy with the same name but receive an unexpected
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
            final List<Element> enumPolicyElms = getEntityElements(policyDocument.getDocumentElement(), "Policy");
            for (Element policyElm : enumPolicyElms) {
                final Element policyDetailElm = XmlUtil.findOnlyOneChildElementByName(policyElm, BundleUtils.L7_NS_GW_MGMT, "PolicyDetail");
                final String policyName = getEntityName(policyDetailElm);
                guidToElementMap.put(policyName, policyElm);
            }

            bundleToAllPolicies.put(aBundle.getId(), guidToElementMap);
        }

        // Organise all policies with the same guid
        Map<String, List<Element>> policyNameToPolicyElms = new HashMap<String, List<Element>>();

        for (Map.Entry<String, Map<String, Element>> entry : bundleToAllPolicies.entrySet()) {
            System.out.println("Bundle: " + entry.getKey());
            final Map<String, Element> value = entry.getValue();
            for (Map.Entry<String, Element> elementEntry : value.entrySet()) {
                final String policyName = elementEntry.getKey();
                if (!policyNameToPolicyElms.containsKey(policyName)) {
                    policyNameToPolicyElms.put(policyName, new ArrayList<Element>());
                }
                policyNameToPolicyElms.get(policyName).add(elementEntry.getValue());
                System.out.println("\t" + policyName);
            }
        }

        // Validate all policies are identical
        for (Map.Entry<String, List<Element>> entry : policyNameToPolicyElms.entrySet()) {
            final String policyName = entry.getKey();
            final List<Element> policyElms = entry.getValue();

            System.out.println("Policy: " + policyName + " has " + policyElms.size() + " occurrences.");

            String canonicalXml = null;
            // Validate all policies for this policyName are identical
            for (Element policyElm : policyElms) {
                final ByteArrayOutputStream byteOutExpected = new ByteArrayOutputStream();
                XmlUtil.canonicalize(policyElm, byteOutExpected);
                final String policyCanonical = new String(byteOutExpected.toByteArray());
                if (canonicalXml == null) {
                    canonicalXml = policyCanonical;
                }

                Assert.assertEquals("Inconsistent policy XML found for policy '" + policyName + "'", canonicalXml, policyCanonical);
            }
        }
    }

    @Test
    public void testResponse_PermissionDenied() throws Exception {
        final Set<String> foundAuditEvents = new HashSet<String>();
        final Set<AuditDetail> foundDetails = new HashSet<AuditDetail>();
        final OAuthInstallerAdminImpl admin = new OAuthInstallerAdminImpl(baseName, new ApplicationEventPublisher() {
            @Override
            public void publishEvent(ApplicationEvent applicationEvent) {
                // note this code will run on a separate thread to the actual test so failures here will not
                // be reported in junit

                if (applicationEvent instanceof InstallPolicyBundleEvent) {
                    InstallPolicyBundleEvent installEvent = (InstallPolicyBundleEvent) applicationEvent;
                    // contents of the access denied request xml not important
                    final String deniedRequestXml = "<DeniedRequest />";
                    installEvent.setProcessingException(new GatewayManagementDocumentUtilities.AccessDeniedManagementResponse("Access Denied", deniedRequestXml));
                    installEvent.setProcessed(true);
                } else if (applicationEvent instanceof OAuthInstallerAdminImpl.OtkInstallationAuditEvent) {
                    final OAuthInstallerAdminImpl.OtkInstallationAuditEvent auditEvent = (OAuthInstallerAdminImpl.OtkInstallationAuditEvent) applicationEvent;
                    foundDetails.addAll(auditEvent.getAuditDetails());
                    foundAuditEvents.add(auditEvent.getNote());
                }
            }
        });

        final AsyncAdminMethods.JobId<ArrayList> jobId = admin.installOAuthToolkit(Arrays.asList("1c2a2874-df8d-4e1d-b8b0-099b576407e1")
                , -5002, new HashMap<String, BundleMapping>(), null);

        while (!admin.getJobStatus(jobId).startsWith("inactive")) {
            Thread.sleep(10L);
        }

        final AsyncAdminMethods.JobResult<ArrayList> installResult = admin.getJobResult(jobId);
        final List<String> results = installResult.result;
        assertNull(results);

        assertEquals("Problem installing OAuth 1.0: Access Denied", installResult.throwableMessage);

        for (String audit : foundAuditEvents) {
            System.out.println(audit);
        }

        //audits
        assertTrue("Required audit is missing", foundAuditEvents.contains("Problem during installation of the OAuth Toolkit"));

        //verify audit details
        boolean found = false;
        for (AuditDetail foundDetail : foundDetails) {
            final String[] params = foundDetail.getParams();
            if (params != null && params.length > 0) {
                final String actualParam = params[0];
                if (actualParam.equals("Problem installing OAuth 1.0: Access Denied")) {
                    found = true;
                }

            }
        }

        assertTrue("Expected audit detail not found", found);
    }

    /**
     * Tests that all folders referenced by policies and services in a bundle exist in the folder enumeration.
     */
    @Test
    public void testAllFolderIdsAreTheSame() throws Exception {
        final List<Pair<BundleInfo, String>> bundleInfos = BundleUtils.getBundleInfos(getClass(), baseName);
        final OAuthToolkitBundleResolver resolver = new OAuthToolkitBundleResolver(bundleInfos);
        final List<BundleInfo> allBundles = resolver.getResultList();

        // Collect all ids for a folder name
        for (BundleInfo aBundle : allBundles) {
            System.out.println("Bundle " + aBundle.getName());
            final Set<String> allFoldersInBundle = new HashSet<String>();

            final Document folderDocument = resolver.getBundleItem(aBundle.getId(), BundleResolver.BundleItem.FOLDER, false);
            final List<Element> folderElms = getEntityElements(folderDocument.getDocumentElement(), "Folder");
            for (Element folderElm : folderElms) {
                final String idAttr = folderElm.getAttribute("id");
                allFoldersInBundle.add(idAttr);
            }

            final Set<String> foundFolderIds = new HashSet<String>();
            // Get all folder ids referenced from services
            final Document serviceDocument = resolver.getBundleItem(aBundle.getId(), BundleResolver.BundleItem.SERVICE, false);
            final List<Element> serviceElements = getEntityElements(serviceDocument.getDocumentElement(), "Service");

            for (Element serviceElm : serviceElements) {
                final Element serviceDetailElm = XmlUtil.findOnlyOneChildElementByName(serviceElm, BundleUtils.L7_NS_GW_MGMT, "ServiceDetail");
                final String folderId = serviceDetailElm.getAttribute("folderId");
                foundFolderIds.add(folderId);
            }

            // Get all folder ids referenced from policies
            final Document policyDocument = resolver.getBundleItem(aBundle.getId(), BundleResolver.BundleItem.POLICY, false);
            final List<Element> policyElements = getEntityElements(policyDocument.getDocumentElement(), "Policy");

            for (Element policyElm: policyElements) {
                final Element policyDetailElm = XmlUtil.findOnlyOneChildElementByName(policyElm, BundleUtils.L7_NS_GW_MGMT, "PolicyDetail");
                final String folderId = policyDetailElm.getAttribute("folderId");
                foundFolderIds.add(folderId);
            }

            System.out.println("All folders in bundle: " + allFoldersInBundle);
            System.out.println("All found folders    : " + foundFolderIds);

            // validate all folder ids referenced exist
            assertTrue(allFoldersInBundle.containsAll(foundFolderIds));
        }

    }

    /**
     * Validates the definition and usage (where possible) of any ${host_ variable.
     *
     * This is important as the OTK installer modifies , when requested, usages of these variable to inject the 'prefix'
     * value to allow for side by side installs of the OTK. If the idiom is not followed correctly in the canned OTK
     * policies, then this mechanism will not work.
     *
     * Rules are:
     * Variables defined with a name of 'host_' must not end with a trailing slash.
     * Usages of host_ variables, whether in another set variable assertion or in a HTTP routing assertion must follow
     * the variable with a slash '/' character. This rule cannot be enforced when the usage is followed by another
     * variable reference.
     *
     */
    @Test
    public void testHostnamesDoNotContainTraililngSlash() throws Exception {
        final List<Pair<BundleInfo, String>> bundleInfos = BundleUtils.getBundleInfos(getClass(), baseName);
        final OAuthToolkitBundleResolver resolver = new OAuthToolkitBundleResolver(bundleInfos);
        final List<BundleInfo> allBundles = resolver.getResultList();

        for (BundleInfo aBundle : allBundles) {
            final Document policyDocument = resolver.getBundleItem(aBundle.getId(), BundleResolver.BundleItem.POLICY, false);
            validateEnumerationDocForHostNames(policyDocument.getDocumentElement(), "Policy");

            final Document serviceDocument = resolver.getBundleItem(aBundle.getId(), BundleResolver.BundleItem.SERVICE, false);
            validateEnumerationDocForHostNames(serviceDocument.getDocumentElement(), "Service");
        }
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
                            final Document policyEnum = installEvent.getContext().getBundleResolver().getBundleItem(bundleInfo.getId(), BundleResolver.BundleItem.POLICY, false);
                            final List<Element> gatewayMgmtPolicyElments = getEntityElements(policyEnum.getDocumentElement(), "Policy");
                            for (Element policyElement : gatewayMgmtPolicyElments) {

                                final Element policyResourceElmWritable = getPolicyResourceElement(policyElement, "Policy", "not used");
                                final Document policyResource = getPolicyDocumentFromResource(policyResourceElmWritable, "Policy", "not used");


                                savePolicyCallback.prePublishCallback(bundleInfo, "not important", policyResource);
                                verifyCommentAdded(policyResource);
                                numPolicyCommentsFound[0]++;
                            }
                        }

                        {
                            final Document serviceEnum = installEvent.getContext().getBundleResolver().getBundleItem(bundleInfo.getId(), BundleResolver.BundleItem.SERVICE, false);
                            final List<Element> gatewayMgmtPolicyElments = getEntityElements(serviceEnum.getDocumentElement(), "Service");
                            for (Element policyElement : gatewayMgmtPolicyElments) {

                                final Element policyResourceElmWritable = getPolicyResourceElement(policyElement, "Service", "not used");
                                final Document policyResource = getPolicyDocumentFromResource(policyResourceElmWritable, "Service", "not used");


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
//    private final static List<Pair<BundleInfo, String>> ALL_BUNDLE_NAMES =
//            Collections.unmodifiableList(
//                    Arrays.asList(
//                            new Pair<BundleInfo, String>(new BundleInfo("1c2a2874-df8d-4e1d-b8b0-099b576407e1", "1.0", "OAuth_1_0", "Desc"), "OAuth_1_0"),
//                            new Pair<BundleInfo, String>(new BundleInfo("ba525763-6e55-4748-9376-76055247c8b1", "1.0", "OAuth_2_0", "Desc"), "OAuth_2_0"),
//                            new Pair<BundleInfo, String>(new BundleInfo("f69c7d15-4999-4761-ab26-d29d58c0dd57", "1.0", "SecureZone_OVP", "Desc"), "SecureZone_OVP"),
//                            new Pair<BundleInfo, String>(new BundleInfo("b082274b-f00e-4fbf-bbb7-395a95ca2a35", "1.0", "SecureZone_Storage", "Desc"), "SecureZone_Storage"),
//                            new Pair<BundleInfo, String>(new BundleInfo("a07924c0-0265-42ea-90f1-2428e31ae5ae", "1.0", "StorageManager", "Desc"), "StorageManager")
//                    ));

    private void validateEnumerationDocForHostNames(Element enumElement, String type) throws Exception {

        final List<Element> enumPolicyElms = getEntityElements(enumElement, type);
        for (Element policyElm : enumPolicyElms) {
            final Element policyResourceElement = getPolicyResourceElement(policyElm, "policy", "Not needed");
            final Document layer7Policy = getPolicyDocumentFromResource(policyResourceElement, "Policy", "Not Needed");
            final List<Element> contextVariables = PolicyUtils.findContextVariables(layer7Policy.getDocumentElement());
            validateSetVariableForHostVarUsage(contextVariables);
            final List<Element> protectedUrls = PolicyUtils.findProtectedUrls(layer7Policy.getDocumentElement());
            for (Element protectedUrl : protectedUrls) {
                System.out.println(XmlUtil.nodeToFormattedString(protectedUrl));
                final String urlValue = protectedUrl.getAttribute("stringValue");
                validateHostVariableUsage(urlValue, "ProtectedServiceUrl");
            }
        }
    }

    private void validateSetVariableForHostVarUsage(List<Element> contextVariables) {
        for (Element contextVariable : contextVariables) {
            final Element variableToSet = XmlUtil.findFirstChildElementByName(contextVariable, "http://www.layer7tech.com/ws/policy", "VariableToSet");
            final String varName = variableToSet.getAttribute("stringValue");
            final String varReference = Syntax.getVariableExpression(varName);
            final Element expression = XmlUtil.findFirstChildElementByName(contextVariable, "http://www.layer7tech.com/ws/policy", "Base64Expression");
            final String base64Value = expression.getAttribute("stringValue");
            final byte[] decodedValue = HexUtils.decodeBase64(base64Value);
            final String varValue = new String(decodedValue, Charsets.UTF8);
            if (varName.startsWith("host_")) {
                assertFalse("Host variable '" + varName + "'value should not contain a trailing slash: " + varValue, varValue.endsWith("/"));
            } else if (varValue.contains("${host_")) {
                validateHostVariableUsage(varValue, varName);
            }
        }
    }

    /**
     * Validate the usage of a ${host_ variable in a policy. The same rules apply whether it is referneced from within
     * a context variable or a protected service URL in a routing assertion. The ${host_ variable must be followed by
     * a slash. If it is followed by a context variable then we cannot validate that particular usage.
     *
     * @param usageValue The value of the variable or protected service URL which references the ${host_ variable
     * @param description A description of the usage used when test fails to identify the usage issue.
     */
    private void validateHostVariableUsage(String usageValue, String description) {
        int index = usageValue.indexOf("${host_");
        while (index != -1) {
            int closeIndex = usageValue.indexOf("}", index + 1);
            char nextChar = usageValue.charAt(closeIndex + 1);
            if (nextChar == '$') {
                System.out.println("Cannot verify '" + description + "' with value '" + usageValue + "' as host variable usage as it is followed by a context variable");
            } else {
                assertEquals("Invalid ${host_ var reference for '" + description + "' with value '" + usageValue + "'. " +
                        "Usage must be followed by a trailing slash.", "/", String.valueOf(nextChar));
            }
            index = usageValue.indexOf("${host_", closeIndex + 1);
        }

    }
}
