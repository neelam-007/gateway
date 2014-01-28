package com.l7tech.external.assertions.oauthinstaller;

import com.l7tech.common.io.XmlUtil;
import com.l7tech.gateway.common.AsyncAdminMethods;
import com.l7tech.gateway.common.audit.AuditDetail;
import com.l7tech.objectmodel.Goid;
import com.l7tech.policy.bundle.BundleInfo;
import com.l7tech.policy.bundle.BundleMapping;
import com.l7tech.policy.bundle.PolicyBundleDryRunResult;
import com.l7tech.policy.variable.Syntax;
import com.l7tech.server.ApplicationContexts;
import com.l7tech.server.event.admin.DetailedAdminEvent;
import com.l7tech.server.event.wsman.DryRunInstallPolicyBundleEvent;
import com.l7tech.server.event.wsman.InstallPolicyBundleEvent;
import com.l7tech.server.policy.bundle.*;
import com.l7tech.test.BugId;
import com.l7tech.test.BugNumber;
import com.l7tech.util.*;
import com.l7tech.xml.xpath.XpathUtil;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationEventPublisher;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import java.io.ByteArrayOutputStream;
import java.util.*;

import static com.l7tech.server.policy.bundle.GatewayManagementDocumentUtilities.*;
import static com.l7tech.util.Functions.toMap;
import static org.junit.Assert.*;

/**
 * Test tests are concerned with logic relating to either the contents of the OTK bundles or logic which is controlled
 * by this module e.g. updating of policy xml before being published.
 */
public class OAuthInstallerAdminImplTest {

    private final String baseName = "/com/l7tech/external/assertions/oauthinstaller/bundles/";
    private final String infoFileName = "OAuthToolkitBundleInfo.xml";
    private final String installerVersionNamespace = "http://ns.l7tech.com/2012/11/oauth-toolkit-bundle";


    // todo test - validate that each service in an enumeration contains a unique id.
    // todo test coverage for reacahibility of folders
    // todo test that all policies with the same name have the same guid.
    // todo check that logic for finding the new guid for a policy is based on the name and not the guid.

    /**
     * Validates that the correct number and type of spring events are published for a dry run.
     */
    @Test
    public void testValidateSpringRequestsForDryRun() throws Exception {
        final Map<String, Boolean> foundBundles = new HashMap<>();
        final Set<String> foundAuditEvents = new HashSet<>();

        final OAuthInstallerAdminImpl admin = new OAuthInstallerAdminImpl(baseName, infoFileName, installerVersionNamespace, new ApplicationEventPublisher() {
            @Override
            public void publishEvent(ApplicationEvent applicationEvent) {
                // note this code will run on a separate thread to the actual test so failures here will not
                // be reported in junit

                if (applicationEvent instanceof DryRunInstallPolicyBundleEvent) {
                    DryRunInstallPolicyBundleEvent dryRunEvent = (DryRunInstallPolicyBundleEvent) applicationEvent;

                    foundBundles.put(dryRunEvent.getContext().getBundleInfo().getId(), true);
                    dryRunEvent.setProcessed(true);
                } else if (applicationEvent instanceof DetailedAdminEvent) {
                    final DetailedAdminEvent auditEvent = (DetailedAdminEvent) applicationEvent;
                    foundAuditEvents.add(auditEvent.getNote() + auditEvent.getAuditDetails().toString());
                }
            }
        });

        final AsyncAdminMethods.JobId<PolicyBundleDryRunResult> jobId = admin.dryRunInstall(Arrays.asList("1c2a2874-df8d-4e1d-b8b0-099b576407e1",
                "ba525763-6e55-4748-9376-76055247c8b1"), new HashMap<String, BundleMapping>(), null, false);

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

        final Set<String> foundAuditEvents = new HashSet<>();
        final OAuthInstallerAdminImpl admin = new OAuthInstallerAdminImpl(baseName, infoFileName, installerVersionNamespace, new ApplicationEventPublisher() {
            @Override
            public void publishEvent(ApplicationEvent applicationEvent) {
                // note this code will run on a separate thread to the actual test so failures here will not
                // be reported in junit

                if (applicationEvent instanceof InstallPolicyBundleEvent) {
                    InstallPolicyBundleEvent installEvent = (InstallPolicyBundleEvent) applicationEvent;
                    installEvent.setProcessed(true);
                } else if (applicationEvent instanceof DetailedAdminEvent) {
                    final DetailedAdminEvent auditEvent = (DetailedAdminEvent) applicationEvent;
                    foundAuditEvents.add(auditEvent.getNote() + auditEvent.getAuditDetails().toString());
                }
            }
        });

        final AsyncAdminMethods.JobId<ArrayList> jobId = admin.install(Arrays.asList("1c2a2874-df8d-4e1d-b8b0-099b576407e1",
                "ba525763-6e55-4748-9376-76055247c8b1"), new Goid(0,-5002), new HashMap<String, BundleMapping>(), null, false);

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
     * This will need to be updated as version numbers change and / or bundles get added.
     *
     * Update - also verifies that the Secure Zone Storage bundle has the correct component id (as it does for others)
     * but this cannot change due to implementation of OAuth_Toolkit_Goatfish_Enhancements#API_Portal_Integration
     *
     * @throws Exception
     */
    @Test
    public void testListAllBundles() throws Exception {
        final OAuthInstallerAdminImpl admin = new OAuthInstallerAdminImpl(baseName, infoFileName, installerVersionNamespace, ApplicationContexts.getTestApplicationContext());

        final List<BundleInfo> allBundles = admin.getAllComponents();
        assertNotNull(allBundles);

        for (BundleInfo aBundle : allBundles) {
            System.out.println("Bundle: " + aBundle);
        }

        assertEquals("Incorrect number of bundles found.", 5, allBundles.size());
        BundleInfo expected;

        expected = new BundleInfo("1c2a2874-df8d-4e1d-b8b0-099b576407e1", "2.1.1", "OAuth 1.0", "Core Services and Test Client");
        assertTrue(allBundles.contains(expected));

        expected = new BundleInfo("ba525763-6e55-4748-9376-76055247c8b1", "2.1.1", "OAuth 2.0", "Auth Server and Test Clients");
        assertTrue(allBundles.contains(expected));

        expected = new BundleInfo("f69c7d15-4999-4761-ab26-d29d58c0dd57", "2.1.1", "Secure Zone OVP", "OVP - OAuth Validation Point");
        assertTrue(allBundles.contains(expected));

        expected = new BundleInfo("b082274b-f00e-4fbf-bbb7-395a95ca2a35", "2.1.1", "Secure Zone Storage", "Token and Client Store");
        expected.addJdbcReference("OAuth");
        assertTrue(allBundles.contains(expected));

        expected = new BundleInfo("a07924c0-0265-42ea-90f1-2428e31ae5ae", "2.1.1", "OAuth Manager", "Manager utility for Client and Token store for OAuth 1.0 and 2.0");
        assertTrue(allBundles.contains(expected));
    }

    /**
     * There are no dependencies across bundles. Any policy referenced from a service or policy must exist in the bundle.
     */
    @Test
    public void testAllIncludesAreWithinTheBundle() throws Exception {
        final List<Pair<BundleInfo, String>> bundleInfos = BundleUtils.getBundleInfos(getClass(), baseName);
        final BundleResolver resolver = new BundleResolverImpl(bundleInfos, getClass()) {};
        final List<BundleInfo> allBundles = resolver.getResultList();

        for (BundleInfo aBundle : allBundles) {
            final Document policyDocument = resolver.getBundleItem(aBundle.getId(), BundleResolver.BundleItem.POLICY, false);
            final List<Element> enumPolicyElms = getEntityElements(policyDocument.getDocumentElement(), "Policy");

            // Record all policies defined in this bundle.
            final Map<String, String> guidToPolicyNameMatch = new HashMap<>();
            // Record all includes for a policy
            final Map<String, Set<String>> policyGuidToPolicyRefMap = new HashMap<>();
            // Record all included policies
            final Map<String, String> policyIncludeToReferantPolicyMap = new HashMap<>();
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
                Set<String> allIncludes = new HashSet<>();
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
            final Map<String, String> policyGuidToServiceRefMap = new HashMap<>();

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
        final BundleResolver resolver = new BundleResolverImpl(bundleInfos, getClass()) {};
        final List<BundleInfo> allBundles = resolver.getResultList();

        // Collect all policies from each bundle
        Map<String, Map<String, Element>> bundleToAllPolicies = new HashMap<>();
        for (BundleInfo aBundle : allBundles) {
            final Map<String, Element> guidToElementMap = new HashMap<>();
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
        Map<String, List<Element>> policyNameToPolicyElms = new HashMap<>();

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
        final Set<String> foundAuditEvents = new HashSet<>();
        final Set<AuditDetail> foundDetails = new HashSet<>();
        final OAuthInstallerAdminImpl admin = new OAuthInstallerAdminImpl(baseName, infoFileName, installerVersionNamespace, new ApplicationEventPublisher() {
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
                } else if (applicationEvent instanceof DetailedAdminEvent) {
                    final DetailedAdminEvent auditEvent = (DetailedAdminEvent) applicationEvent;
                    foundDetails.addAll(auditEvent.getAuditDetails());
                    foundAuditEvents.add(auditEvent.getNote());
                }
            }
        });

        final AsyncAdminMethods.JobId<ArrayList> jobId = admin.install(Arrays.asList("1c2a2874-df8d-4e1d-b8b0-099b576407e1")
                , new Goid(0,-5002), new HashMap<String, BundleMapping>(), null, false);

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
        final BundleResolver resolver = new BundleResolverImpl(bundleInfos, getClass()) {};
        final List<BundleInfo> allBundles = resolver.getResultList();

        // Collect all ids for a folder name
        for (BundleInfo aBundle : allBundles) {
            System.out.println("Bundle " + aBundle.getName());
            final Set<String> allFoldersInBundle = new HashSet<>();

            final Document folderDocument = resolver.getBundleItem(aBundle.getId(), BundleResolver.BundleItem.FOLDER, false);
            final List<Element> folderElms = getEntityElements(folderDocument.getDocumentElement(), "Folder");
            for (Element folderElm : folderElms) {
                final String idAttr = folderElm.getAttribute("id");
                allFoldersInBundle.add(idAttr);
            }

            final Set<String> foundFolderIds = new HashSet<>();
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
        final BundleResolver resolver = new BundleResolverImpl(bundleInfos, getClass()) {};
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
        final String[] otkToolkitVersion = new String[1];

        final OAuthInstallerAdminImpl admin = new OAuthInstallerAdminImpl(baseName, infoFileName, installerVersionNamespace, new ApplicationEventPublisher() {
            @Override
            public void publishEvent(ApplicationEvent applicationEvent) {

                if (applicationEvent instanceof InstallPolicyBundleEvent) {
                    InstallPolicyBundleEvent installEvent = (InstallPolicyBundleEvent) applicationEvent;
                    final PreBundleSavePolicyCallback savePolicyCallback = installEvent.getPreBundleSavePolicyCallback();
                    if (savePolicyCallback == null) {
                        fail("Policy call back should be configured.");
                    }
                    final BundleInfo bundleInfo = installEvent.getContext().getBundleInfo();

                    final String bundleVersion = bundleInfo.getVersion();
                    try {
                        {
                            final Document policyEnum = installEvent.getContext().getBundleResolver().getBundleItem(bundleInfo.getId(), BundleResolver.BundleItem.POLICY, false);
                            final List<Element> gatewayMgmtPolicyElments = getEntityElements(policyEnum.getDocumentElement(), "Policy");
                            for (Element policyElement : gatewayMgmtPolicyElments) {

                                final Element policyResourceElmWritable = getPolicyResourceElement(policyElement, "Policy", "not used");
                                final Document policyResource = getPolicyDocumentFromResource(policyResourceElmWritable, "Policy", "not used");
                                final Element policyDetailElement = getPolicyDetailElement(policyElement);

                                savePolicyCallback.prePublishCallback(bundleInfo, policyDetailElement, policyResource);

                                verifyCommentAdded(policyResource, bundleVersion, otkToolkitVersion[0]);
                                numPolicyCommentsFound[0]++;
                            }
                        }

                        {
                            final Document serviceEnum = installEvent.getContext().getBundleResolver().getBundleItem(bundleInfo.getId(), BundleResolver.BundleItem.SERVICE, false);
                            final List<Element> gatewayMgmtPolicyElments = getEntityElements(serviceEnum.getDocumentElement(), "Service");
                            for (Element serviceElement : gatewayMgmtPolicyElments) {

                                final Element policyResourceElmWritable = getPolicyResourceElement(serviceElement, "Service", "not used");
                                final Document policyResource = getPolicyDocumentFromResource(policyResourceElmWritable, "Service", "not used");
                                final Element serviceDetailElement = getServiceDetailElement(serviceElement);

                                savePolicyCallback.prePublishCallback(bundleInfo, serviceDetailElement, policyResource);
                                verifyCommentAdded(policyResource, bundleVersion, otkToolkitVersion[0]);
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

        otkToolkitVersion[0] = admin.getVersion();

        final AsyncAdminMethods.JobId<ArrayList> jobId =
                admin.install(Arrays.asList("1c2a2874-df8d-4e1d-b8b0-099b576407e1"), new Goid(0, -5002), new HashMap<String, BundleMapping>(), null, false);

        while (!admin.getJobStatus(jobId).startsWith("inactive")) {
            Thread.sleep(10L);
        }

        assertEquals("Incorrect number of policy comments found", 7, numPolicyCommentsFound[0]);
        assertEquals("Incorrect number of service comments found", 7, numServiceCommentsFound[0]);

    }

    @Test
    @BugNumber(13282)
    public void testGetDatabaseSchema() throws Exception {
        final OAuthInstallerAdminImpl admin = new OAuthInstallerAdminImpl(baseName, infoFileName, installerVersionNamespace, new ApplicationEventPublisher() {
            @Override
            public void publishEvent(ApplicationEvent applicationEvent) {

            }
        });

        final String dbSchema = admin.getOAuthDatabaseSchema();
        System.out.println(dbSchema);
        assertNotNull(dbSchema);
        assertFalse(dbSchema.trim().isEmpty());
    }

    /**
     * API Portal integration requires storing the policy with the API Portal integrated. At install time this option
     * is off by default and if not chosen the sections of policy with deal with API Portal integration must be removed.
     *
     * This test is hardcoded with a list of folder assertions and individual comments with the left comment
     * 'PORTAL_INTEGRATION'.
     *
     * If this test fails then update only when it's confirmed that the Secure Zone Storage clientstore service's policy
     * was updated for API Portal integration.
     *
     * A future version of the API Portal may require a more complicated policy. The current version can support both
     * versions 2.1 and 2.2 but future versions may not be backwards compatible, in which case the logic to remove
     * support for the API Portal may be more complicated.
     *
     */
    @Test
    public void testVerifyExpectedPortalIntegrationCommentsExist() throws Exception {
        // Set up

        final List<Pair<BundleInfo, String>> bundleInfos = BundleUtils.getBundleInfos(getClass(), baseName);
        final BundleResolver resolver = new BundleResolverImpl(bundleInfos, getClass()) {};

        final Map<String, BundleInfo> bundleMap = toMap(resolver.getResultList(), new Functions.Unary<Pair<String, BundleInfo>, BundleInfo>() {
            @Override
            public Pair<String, BundleInfo> call(BundleInfo bundleInfo) {
                return new Pair<>(bundleInfo.getId(), bundleInfo);
            }
        });

        final BundleInfo secureZoneBundle = bundleMap.get(OAuthInstallerAssertion.SECURE_ZONE_STORAGE_COMP_ID);
        assertNotNull(secureZoneBundle);

        // Find the clientstore service's policy
        final Document serviceDocMgmtEnum = resolver.getBundleItem(secureZoneBundle.getId(), BundleResolver.BundleItem.SERVICE, false);
        final List<Element> serviceElms = GatewayManagementDocumentUtilities.getEntityElements(serviceDocMgmtEnum.getDocumentElement(), "Service");
        final Map<String, Document> serviceNameToPolicyMap = getServicesAndPolicyDocuments(serviceElms);
        final Document clientStorePolicyDoc = serviceNameToPolicyMap.get("oauth/clients");
        assertNotNull(clientStorePolicyDoc);

        // Set up finished
        // validate contents of policy
        final Element policyElm = clientStorePolicyDoc.getDocumentElement();
        // System.out.println(XmlUtil.nodeToFormattedString(policyElm));
        validatePortalIntegrationComments(policyElm, 7, 1, 3, 3);
    }

    /**
     * If the API Portal integration is not required, then the SecureZone clientstore policy needs to be updated
     * to remove all assertions added for API Portal integration.
     *
     */
    @Test
    public void testApiPortalIntegrationNotRequested() throws Exception {
        final boolean[] testPass = new boolean[1];
        final OAuthInstallerAdminImpl admin = new OAuthInstallerAdminImpl(baseName, infoFileName, installerVersionNamespace, new ApplicationEventPublisher() {
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

                        final Document serviceEnum = installEvent.getContext().getBundleResolver().getBundleItem(bundleInfo.getId(), BundleResolver.BundleItem.SERVICE, false);
                        final List<Element> gatewayMgmtPolicyElments = getEntityElements(serviceEnum.getDocumentElement(), "Service");
                        for (Element serviceEnumElm : gatewayMgmtPolicyElments) {
                            final Element policyResourceElmWritable = getPolicyResourceElement(serviceEnumElm, "Service", "not used");
                            final Document policyDocWriteable = getPolicyDocumentFromResource(policyResourceElmWritable, "Service", "not used");
                            final Element serviceDetailElement = getServiceDetailElement(serviceEnumElm);
                            savePolicyCallback.prePublishCallback(bundleInfo, serviceDetailElement, policyDocWriteable);
                            // Verify elements removed
                            final String entityName = getEntityName(serviceDetailElement);
                            if ("oauth/clients".equals(entityName)) {
                                validatePortalIntegrationComments(policyDocWriteable.getDocumentElement(), 0, 0, 0, 0);
                                testPass[0] = true;
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

        // Secure Zone Storage
        // false to not integrate the API Portal
        final AsyncAdminMethods.JobId<ArrayList> jobId =
                admin.install(Arrays.asList("b082274b-f00e-4fbf-bbb7-395a95ca2a35"), new Goid(0, -5002), new HashMap<String, BundleMapping>(), null, false);

        while (!admin.getJobStatus(jobId).startsWith("inactive")) {
            Thread.sleep(10L);
        }

        assertTrue(testPass[0]);
    }

    /**
     * The 'Manage Clients' button is not available when the API Portal is integrated with the OTK.
     *
     * This test is a heuristic, it looks for the presence of the variables and template responses needed and if it finds
     * them it assumes that the policies contain the correct logic. It is possible that the policy logic is broken and
     * this test will pass. The intent of this test is to avoid someone changing the error message or removing the
     * required values entirely.
     *
     * This test validates the connection between the manager and clientstore endpoints.
     */
    @BugId("SSG-6456")
    @Test
    public void testManageClientsNotAvailableWhenPortalIntegrated() throws Exception {

        final List<Pair<BundleInfo, String>> bundleInfos = BundleUtils.getBundleInfos(getClass(), baseName);
        final BundleResolver resolver = new BundleResolverImpl(bundleInfos, getClass()) {};

        final Map<String, BundleInfo> bundleMap = Functions.toMap(resolver.getResultList(), new Functions.Unary<Pair<String, BundleInfo>, BundleInfo>() {
            @Override
            public Pair<String, BundleInfo> call(BundleInfo bundleInfo) {
                return new Pair<String, BundleInfo>(bundleInfo.getId(), bundleInfo);
            }
        });

        final String expectedValue = "API Portal Integration is configured";
        {
            final BundleInfo clientStoreBundle = bundleMap.get("b082274b-f00e-4fbf-bbb7-395a95ca2a35");
            assertNotNull(clientStoreBundle);

            final Document serviceDocument = resolver.getBundleItem(clientStoreBundle.getId(), BundleResolver.BundleItem.SERVICE, false);
            final List<Element> enumServiceElms = getEntityElements(serviceDocument.getDocumentElement(), "Service");
            // find the client store policy

            boolean foundVariable = false;
            boolean foundTemplateResponse = false;
            for (Element serviceElm : enumServiceElms) {
                final Element serviceDetailElement = getServiceDetailElement(serviceElm);
                final String entityName = getEntityName(serviceDetailElement);
                if (!"oauth/clients".equals(entityName)) {
                    continue;
                }

                final Element policyResourceElement = getPolicyResourceElement(serviceElm, "Service", "Not used");
                final Document layer7Policy = getPolicyDocumentFromResource(policyResourceElement, "Service", "Not used");
                final List<Element> contextVariables = PolicyUtils.findContextVariables(layer7Policy.getDocumentElement());
                // find customError
                for (Element contextVariable : contextVariables) {
                    final Element variableToSet = DomUtils.findExactlyOneChildElementByName(contextVariable, "http://www.layer7tech.com/ws/policy", "VariableToSet");
                    final String varName = variableToSet.getAttribute("stringValue");
                    if ("customError".equals(varName)) {
                        final Element base64Expression = DomUtils.findExactlyOneChildElementByName(contextVariable, "http://www.layer7tech.com/ws/policy", "Base64Expression");
                        final byte[] value = HexUtils.decodeBase64(base64Expression.getAttribute("stringValue"));
                        final String varValue = new String(value, Charsets.UTF8);

                        if (varValue.contains(expectedValue)) {
                            foundVariable = true;
                        }
                    }
                }

                final List<Element> templateResponses = PolicyUtils.findTemplateResponses(layer7Policy.getDocumentElement());
                for (Element templateResponse : templateResponses) {
                    final Element base64ResponseBody = DomUtils.findExactlyOneChildElementByName(templateResponse, "http://www.layer7tech.com/ws/policy", "Base64ResponseBody");
                    final byte[] value = HexUtils.decodeBase64(base64ResponseBody.getAttribute("stringValue"));
                    final String varValue = new String(value, Charsets.UTF8);
                    if (varValue.contains("${customError}")) {
                        foundTemplateResponse = true;
                    }
                }
            }

            assertTrue("Did not find customError variable with the correct value", foundVariable);
            assertTrue("Did not find a template response which references ${customError}", foundTemplateResponse);
        }

        // client store has passed, now test the manager policy

        {
            final BundleInfo managerBundle = bundleMap.get("a07924c0-0265-42ea-90f1-2428e31ae5ae");
            assertNotNull(managerBundle);

            final Document serviceDocument = resolver.getBundleItem(managerBundle.getId(), BundleResolver.BundleItem.SERVICE, false);
            final List<Element> enumServiceElms = getEntityElements(serviceDocument.getDocumentElement(), "Service");

            boolean foundCorrectComparison = false;
            for (Element serviceElm : enumServiceElms) {
                final Element serviceDetailElement = getServiceDetailElement(serviceElm);
                final String entityName = getEntityName(serviceDetailElement);
                if (!"oauth/manager".equals(entityName)) {
                    continue;
                }

                final Element policyResourceElement = getPolicyResourceElement(serviceElm, "Service", "Not used");
                final Document layer7Policy = getPolicyDocumentFromResource(policyResourceElement, "Service", "Not used");
                final List<Element> comparisonAssertions = PolicyUtils.findComparisonAssertions(layer7Policy.getDocumentElement());
                for (Element comparisonAssertion : comparisonAssertions) {
                    final Element expression2 = DomUtils.findExactlyOneChildElementByName(comparisonAssertion, "http://www.layer7tech.com/ws/policy", "Expression2");
                    final String expression2Value = expression2.getAttribute("stringValue");

                    final Element rightValueElm = DomUtils.findFirstDescendantElement(comparisonAssertion, "http://www.layer7tech.com/ws/policy", "RightValue");
                    final String rightValue = rightValueElm.getAttribute("stringValue");

                    if (expectedValue.equals(expression2Value) && expectedValue.equals(rightValue)) {
                        foundCorrectComparison = true;
                    }
                }
            }

            assertTrue("Did not find check for correct error message", foundCorrectComparison);
        }

    }

    /**
     * Comments are required on all portal integration assertions. The presence of the specific comment
     * implies the assertion, all all it's childern if it's an All, are for portal integration only.
     */
    private void validatePortalIntegrationComments(Element policyElm,
                                                   final int totalCommentsExpected,
                                                   final int setVariableFoundExpected,
                                                   final int comparisonFoundExpected,
                                                   final int allFoundExpected) {
        final List<Element> foundComments = XpathUtil.findElements(policyElm, ".//L7p:value[@stringValue='PORTAL_INTEGRATION']", getNamespaceMap());
        assertEquals("Wrong number of PORTAL_INTEGRATION comments found", totalCommentsExpected, foundComments.size());

        // verify they are all left comments
        int setVariableFound = 0;
        int comparionsFound = 0;
        int allFound = 0;
        for (Element foundComment : foundComments) {
            final Element parentNode = (Element) foundComment.getParentNode();
            final List<Element> elements = XpathUtil.findElements(parentNode, ".//L7p:key[@stringValue='LEFT.COMMENT']", getNamespaceMap());
            assertNotNull(elements);
            assertEquals(1, elements.size());
            final Node assertionNode = parentNode.getParentNode().getParentNode().getParentNode();
            final String assertionName = assertionNode.getLocalName();
            System.out.println(assertionName);
            if ("SetVariable".equals(assertionName)) {
                setVariableFound++;
            } else if ("ComparisonAssertion".equals(assertionName)) {
                comparionsFound++;
            } else if ("All".equals(assertionName)) {
                allFound++;
            }
        }

        // 3 assertions and 2 all folders
        assertEquals(setVariableFoundExpected, setVariableFound);
        assertEquals(comparisonFoundExpected, comparionsFound);
        assertEquals(allFoundExpected, allFound);
    }

    private Map<String, Document> getServicesAndPolicyDocuments(List<Element> serviceMgmtElements) {
        return toMap(serviceMgmtElements, new Functions.Unary<Pair<String, Document>, Element>() {
            @Override
            public Pair<String, Document> call(Element serviceElement) {
                try {
                    final Element serviceDetailElement = getServiceDetailElement(serviceElement);
                    final String serviceName = getEntityName(serviceDetailElement);
                    final Element resourceElement = getPolicyResourceElement(serviceElement, "Service", "Not Used");
                    assertNotNull(resourceElement);
                    final Document layer7Policy = getPolicyDocumentFromResource(resourceElement, "Policy", "Not Used");
                    return new Pair<String, Document>(serviceName, layer7Policy);
                } catch (BundleResolver.InvalidBundleException e) {
                    throw new RuntimeException(e);
                }
            }
        });
    }

    private void verifyCommentAdded(Document policyResource, String bundleVersion, String otkToolkitVersion) {
        // verify version was added
        final Element allElm = XmlUtil.findFirstChildElement(policyResource.getDocumentElement());
        final Element comment = XmlUtil.findFirstChildElement(allElm);
        assertTrue(comment.getLocalName().equals("CommentAssertion"));
        final Element commentValueElm = XmlUtil.findFirstChildElement(comment);
        final String commentValue = commentValueElm.getAttribute("stringValue");
        assertEquals("Invalid comment value found", "Component version " + bundleVersion + " installed by OAuth installer version " + otkToolkitVersion, commentValue);
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
