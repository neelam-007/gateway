package com.l7tech.external.assertions.policybundleinstaller;

import com.l7tech.common.io.XmlUtil;
import com.l7tech.message.Message;
import com.l7tech.policy.bundle.BundleInfo;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.PolicyAssertionException;
import com.l7tech.policy.bundle.BundleMapping;
import com.l7tech.server.event.wsman.DryRunInstallPolicyBundleEvent;
import com.l7tech.server.event.wsman.InstallPolicyBundleEvent;
import com.l7tech.server.event.wsman.PolicyBundleEvent;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.server.policy.bundle.*;
import com.l7tech.test.BugNumber;
import com.l7tech.util.DomUtils;
import com.l7tech.util.Functions;
import com.l7tech.util.IOUtils;
import com.l7tech.util.Pair;
import com.l7tech.xml.DomElementCursor;
import com.l7tech.xml.ElementCursor;
import com.l7tech.xml.InvalidXpathException;
import com.l7tech.xml.xpath.XpathExpression;
import com.l7tech.xml.xpath.XpathResult;
import com.l7tech.xml.xpath.XpathResultIterator;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.junit.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;

import javax.xml.xpath.XPathExpressionException;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.URL;
import java.text.MessageFormat;
import java.util.*;

import static com.l7tech.server.policy.bundle.BundleResolver.BundleItem.*;
import static com.l7tech.server.policy.bundle.GatewayManagementDocumentUtilities.MGMT_VERSION_NAMESPACE;
import static com.l7tech.server.policy.bundle.GatewayManagementDocumentUtilities.getNamespaceMap;
import static org.junit.Assert.*;
import static org.junit.Assert.assertEquals;

public class PolicyBundleInstallerTest {

    private int nextOid = 1000000;

    /**
     * Test the success case when no folders already exist.
     */
    @Test
    public void testInstallFolders_NoneExist() throws Exception {

        final BundleResolver bundleResolver = getBundleResolver();
        final List<BundleInfo> resultList = bundleResolver.getResultList();
        final BundleInfo bundleInfo = resultList.get(0);

        final PolicyBundleInstallerContext context = new PolicyBundleInstallerContext(bundleInfo, new BundleMapping(), null, bundleResolver, true);
        final InstallPolicyBundleEvent installEvent = new InstallPolicyBundleEvent(this, context, null);
        final Functions.Nullary<Boolean> cancelledCallback = getCancelledCallback(installEvent);
        final PolicyBundleInstaller bundleInstaller = new PolicyBundleInstaller(new GatewayManagementInvoker() {
            @Override
            public AssertionStatus checkRequest(PolicyEnforcementContext context) throws PolicyAssertionException, IOException {
                try {
                    final Pair<AssertionStatus, Document> documentPair = cannedIdResponse(context.getRequest().getXmlKnob().getDocumentReadOnly());
                    setResponse(context, documentPair.right);
                    return documentPair.left;
                } catch (SAXException e) {
                    throw new RuntimeException(e);
                }
            }
        }, context, cancelledCallback);

        final Document folderDoc = bundleResolver.getBundleItem(bundleInfo.getId(), FOLDER, false);
        final Map<Long, Long> oldToNewMap = bundleInstaller.installFolders(-5002, folderDoc);
        assertNotNull(oldToNewMap);
        assertFalse(oldToNewMap.isEmpty());

        for (Map.Entry<Long, Long> entry : oldToNewMap.entrySet()) {
            System.out.println(entry.getKey() + ": " + entry.getValue());
        }
    }

    private Functions.Nullary<Boolean> getCancelledCallback(final PolicyBundleEvent bundleEvent) {
        return new Functions.Nullary<Boolean>() {
                @Override
                public Boolean call() {
                    return bundleEvent.isCancelled();
                }
            };
    }

    /**
     * Test the success case when all folders already exist.
     *
     * @throws Exception
     */
    @Test
    public void testInstallFolders_AllExist() throws Exception {
        final Map<String, Integer> nameToIdMap = new HashMap<String, Integer>();

        final BundleResolver bundleResolver = getBundleResolver();
        final List<BundleInfo> resultList = bundleResolver.getResultList();
        final BundleInfo bundleInfo = resultList.get(0);
        //OAuth_1_0
        final Document oAuth_1_0 = bundleResolver.getBundleItem(bundleInfo.getId(), FOLDER, false);
        final PolicyBundleInstallerContext context = new PolicyBundleInstallerContext(bundleInfo, new BundleMapping(), null, bundleResolver, true);
        final InstallPolicyBundleEvent installEvent = new InstallPolicyBundleEvent(this, context, null);

        final PolicyBundleInstaller bundleInstaller = new PolicyBundleInstaller(new GatewayManagementInvoker() {
            @Override
            public AssertionStatus checkRequest(PolicyEnforcementContext context) throws PolicyAssertionException, IOException {
                try {
                    final String requestXml = XmlUtil.nodeToString(context.getRequest().getXmlKnob().getDocumentReadOnly());
                    if (requestXml.contains("http://schemas.xmlsoap.org/ws/2004/09/transfer/Create")) {
                        System.out.println(XmlUtil.nodeToFormattedString(XmlUtil.parse(requestXml)));
                        setResponse(context, alreadyExistsResponse);
                        return AssertionStatus.NONE;
                    } else {
                        // it's an enumerate request
                        final int beginIndex = requestXml.indexOf("folderId='") + 10;
                        final int endIndex = requestXml.indexOf("'", beginIndex + 1);
                        final String folderId = requestXml.substring(beginIndex, endIndex);

                        final int i = requestXml.indexOf("text()='") + 8;
                        final int i1 = requestXml.indexOf("'", i + 1);
                        final String name = requestXml.substring(i, i1);

                        int idToUse;
                        if (nameToIdMap.containsKey(name)) {
                            idToUse = nameToIdMap.get(name);
                        } else {
                            idToUse = 1000 + nextOid++;
                            nameToIdMap.put(name, idToUse);
                        }

                        final Long folderIdLong = Long.valueOf(folderId);
                        System.out.println("Requesting lookup for folder: " + folderIdLong + ": " + requestXml);
                        final String response = MessageFormat.format(CANNED_ENUMERATE_WITH_FILTER_AND_EPR_RESPONSE, String.valueOf(idToUse));
                        setResponse(context, response);
                        return AssertionStatus.NONE;
                    }
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }
        }, context, getCancelledCallback(installEvent));

        final Map<Long, Long> oldToNewMap = bundleInstaller.installFolders(-5002, oAuth_1_0);
        assertNotNull(oldToNewMap);
        assertFalse(oldToNewMap.isEmpty());

        for (Map.Entry<Long, Long> entry : oldToNewMap.entrySet()) {
            System.out.println(entry.getKey() + ": " + entry.getValue());
        }

    }

    @Test
    public void testServiceXpathExpression() throws Exception {
        final BundleResolver resolver = getBundleResolver();
        // OAuth_1_0
        final Document oAuth_1_0 = resolver.getBundleItem("4e321ca1-83a0-4df5-8216-c2d2bb36067d", SERVICE, false);
        ElementCursor cursor = new DomElementCursor(oAuth_1_0);

        final String urlMapping = "/auth/oauth/v1/token";
        String xpath = "//l7:Service/l7:ServiceDetail/l7:ServiceMappings/l7:HttpMapping/l7:UrlPattern[text()='"+urlMapping+"']";
        final XpathResult xpath1 = cursor.getXpathResult(new XpathExpression(xpath, GatewayManagementDocumentUtilities.getNamespaceMap()).compile());
        final XpathResultIterator iterator = xpath1.getNodeSet().getIterator();
        while (iterator.hasNext()) {
            System.out.println(XmlUtil.nodeToFormattedString(iterator.nextElementAsCursor().asDomElement()));
        }
    }

    @Test
    public void testAllPolicyBundlesInstall() throws Exception {
        final List<Pair<BundleInfo, String>> bundleInfos = BundleUtils.getBundleInfos(getClass(), "/com/l7tech/external/assertions/policybundleinstaller/bundles/");
        for (Pair<BundleInfo, String> bundleInfo : bundleInfos) {
            installPoliciesTest(bundleInfo.left, null);
        }
    }

    /**
     * Tests that all policies in a bundle can be installed.
     *
     * This test says that all policies already exist so the installer needs to then request each policy's guid.
     *
     * The test then verifies that all guids from the test bundle were created via an old to new mapping post the
     * call to installPolicies().
     *
     * Note: This also tests policy includes as almost all test policies in resources folder contain policy includes.
     *
     */
    public void installPoliciesTest(final @NotNull BundleInfo bundleInfo,
                                    final @Nullable String installationPrefix) throws Exception {
        final Map<String, String> nameToPreviousGuid = new HashMap<String, String>();

        final BundleResolver bundleResolver = getBundleResolver();
        final PolicyBundleInstallerContext context = new PolicyBundleInstallerContext(bundleInfo, new BundleMapping(), installationPrefix, bundleResolver, true);
        final InstallPolicyBundleEvent installEvent = new InstallPolicyBundleEvent(this, context, null);

        final PolicyBundleInstaller bundleInstaller = new PolicyBundleInstaller(new GatewayManagementInvoker() {
            @Override
            public AssertionStatus checkRequest(PolicyEnforcementContext context) throws PolicyAssertionException, IOException {
                try {
                    final String requestXml = XmlUtil.nodeToString(context.getRequest().getXmlKnob().getDocumentReadOnly());
                    final Document enumPolicyDocument = XmlUtil.parse(requestXml);
                    if (requestXml.contains("http://schemas.xmlsoap.org/ws/2004/09/transfer/Create")) {

                        // validate any prefix was correctly applied
                        if (installationPrefix != null) {
                            final Element policyEntityEl = XmlUtil.findFirstDescendantElement(enumPolicyDocument.getDocumentElement(), MGMT_VERSION_NAMESPACE, "Policy");
                            final Element policyDetailElm = GatewayManagementDocumentUtilities.getPolicyDetailElement(policyEntityEl);
                            final String policyName = GatewayManagementDocumentUtilities.getEntityName(policyDetailElm);
                            System.out.println("Policy name: " + policyName);
                            assertTrue("Policy name was not prefixed", policyName.startsWith(installationPrefix));
                        }

                        setResponse(context, alreadyExistsResponse);
                        return AssertionStatus.NONE;
                    } else {

                        // Create a new GUID for each policy. Return the same GUID for the same include

                        ElementCursor cursor = new DomElementCursor(enumPolicyDocument);
                        System.out.println(requestXml);
                        final XpathResult xpathResult = cursor.getXpathResult(
                                new XpathExpression(
                                        "//wsman:Selector[@Name='name']",
                                        GatewayManagementDocumentUtilities.getNamespaceMap()).compile());
                        final String name = DomUtils.getTextValue(xpathResult.getNodeSet().getIterator().nextElementAsCursor().asDomElement(), true);
                        final String guidToUse = nameToPreviousGuid.containsKey(name)
                                ? nameToPreviousGuid.get(name) : UUID.randomUUID().toString();

                        final String response = MessageFormat.format(CANNED_GET_POLICY_RESPONSE, guidToUse, name);
                        setResponse(context, response);
                        return AssertionStatus.NONE;
                    }
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }
        }, context, getCancelledCallback(installEvent));

        final Document policyFromBundleDoc = bundleResolver.getBundleItem(bundleInfo.getId(), POLICY, false);

        ElementCursor cursor = new DomElementCursor(policyFromBundleDoc);
        final XpathResult xpathResult = cursor.getXpathResult(
                new XpathExpression(
                        "//l7:PolicyDetail",
                        GatewayManagementDocumentUtilities.getNamespaceMap()).compile());


        final XpathResultIterator iterator = xpathResult.getNodeSet().getIterator();
        if (!iterator.hasNext()) {
            fail("Incorrect test configuration");
        }

        // record all policy names mapped to the canned GUID
        final Map<String, String> policyNameToGuid = new HashMap<String, String>();
        while (iterator.hasNext()) {
            final ElementCursor elementCursor = iterator.nextElementAsCursor();
            final Element policyDetailElm = elementCursor.asDomElement();
            final String guid = policyDetailElm.getAttribute("guid");
            final String name = DomUtils.getTextValue(
                    XmlUtil.findExactlyOneChildElementByName(policyDetailElm, BundleUtils.L7_NS_GW_MGMT, "Name"), true);

            policyNameToGuid.put(name, guid);
        }

        final Map<String, String> oldGuidToNewGuidMap = bundleInstaller.installPolicies(getFolderIds(), policyFromBundleDoc);

        // verify that each known policy name was installed
        for (Map.Entry<String, String> bundlePolicy : policyNameToGuid.entrySet()) {
            // confirm it was installed
            assertTrue("Policy " + bundlePolicy.getKey() + " and guid " + bundlePolicy.getValue() + " was not installed.",
                    oldGuidToNewGuidMap.containsKey(bundlePolicy.getValue()));
        }

    }

    @Test
    public void testInstallServices() throws Exception {
        final BundleResolver bundleResolver = getBundleResolver();
        final List<BundleInfo> resultList = bundleResolver.getResultList();
        final BundleInfo bundleInfo = resultList.get(0);
        //OAuth_1_0
        final PolicyBundleInstallerContext context = new PolicyBundleInstallerContext(bundleInfo, new BundleMapping(), null, bundleResolver, true);
        final InstallPolicyBundleEvent installEvent = new InstallPolicyBundleEvent(this, context, null);

        final PolicyBundleInstaller bundleInstaller = new PolicyBundleInstaller(new GatewayManagementInvoker() {
            @Override
            public AssertionStatus checkRequest(PolicyEnforcementContext context) throws PolicyAssertionException, IOException {
                try {
                    final Pair<AssertionStatus, Document> documentPair = cannedIdResponse(context.getRequest().getXmlKnob().getDocumentReadOnly());
                    setResponse(context, documentPair.right);
                    return documentPair.left;

                } catch (SAXException e) {
                    throw new RuntimeException(e);
                }
            }
        }, context, getCancelledCallback(installEvent));

        // OAuth_1_0
        final Document serviceFromBundleDoc = bundleResolver.getBundleItem(bundleInfo.getId(), SERVICE, false);
        bundleInstaller.installServices(getFolderIds(), getPolicyGuids(), serviceFromBundleDoc);

    }

    /**
     * IF a policy contains a JDBC connection and the client wants to update that reference, then the policy should
     * be updated before the call to the management service to install it happens
     * @throws Exception
     */
    @Test
    public void testJdbcReferencesUpdatedCorrectly() throws Exception {
        final BundleResolver bundleResolver = getBundleResolver();

        final boolean[] invoked = new boolean[1];
        final Map<String, Boolean> servicesFound = new HashMap<String, Boolean>();
        final Map<String, Set<String>> jdbcPerService = new HashMap<String, Set<String>>();

        final PolicyBundleInstallerContext context = new PolicyBundleInstallerContext(
                new BundleInfo("4e321ca1-83a0-4df5-8216-c2d2bb36067d", "1.0", "Bundle with JDBC references", "Desc"),
                null, null, bundleResolver, true);
        final InstallPolicyBundleEvent installEvent = new InstallPolicyBundleEvent(this, context, null);

        final PolicyBundleInstaller bundleInstaller = new PolicyBundleInstaller(new GatewayManagementInvoker() {
            @Override
            public AssertionStatus checkRequest(PolicyEnforcementContext context) throws PolicyAssertionException, IOException {
                invoked[0] = true;
                final String requestXml;
                final Document requestDoc;
                try {
                    requestDoc = context.getRequest().getXmlKnob().getDocumentReadOnly();
                    requestXml = XmlUtil.nodeToString(requestDoc);
                } catch (SAXException e) {
                    throw new RuntimeException(e);
                }

                if (requestXml.contains("Enumerate")) {
                    // reply with a does not exist
                    // just reply with any non created response
                    setResponse(context, alreadyExistsResponse);
                    return AssertionStatus.NONE;
                } else {
                    // Validate any JDBC references
                    if (requestXml.contains("http://ns.l7tech.com/2010/04/gateway-management/services")) {
                        //it's a service
                        final ElementCursor cursor;
                        try {
                            cursor = new DomElementCursor(XmlUtil.parse(requestXml));
                        } catch (SAXException e) {
                            throw new RuntimeException(e);
                        }
                        // search from the current element only
                        final XpathResult xpathResult;
                        try {
                            xpathResult = cursor.getXpathResult(
                                    new XpathExpression(".//l7:Service", getNamespaceMap()).compile());
                        } catch (XPathExpressionException e) {
                            throw new RuntimeException("Unexpected issue with internal xpath expression: " + e.getMessage(), e);
                        } catch (InvalidXpathException e) {
                            throw new RuntimeException("Unexpected issue with internal xpath expression: " + e.getMessage(), e);
                        }

                        final Element serviceElement = xpathResult.getNodeSet().getIterator().nextElementAsCursor().asDomElement();
                        final String id = serviceElement.getAttribute("id");
                        servicesFound.put(id, true);
                        System.out.println("Testing service: " + id);
                        try {
                            final Element policyResourceElement = GatewayManagementDocumentUtilities.getPolicyResourceElement(serviceElement, "Service not important", id);
//                            System.out.println(XmlUtil.nodeToFormattedString(policyResourceElement));
                            final Set<String> jdbcConnsFound = BundleUtils.searchForJdbcReferences(policyResourceElement, "Service", id);
                            jdbcPerService.put(id, jdbcConnsFound);

                        } catch (BundleResolver.InvalidBundleException e) {
                            throw new RuntimeException(e);
                        }

                    } else if (requestXml.contains(" to do policy")) {

                    }


                    // pretend it was created
                    // validate the requestXml contains updated JDBC Connections
                    final Pair<AssertionStatus, Document> documentPair = cannedIdResponse(requestDoc);
                    setResponse(context, documentPair.right);
                    return documentPair.left;
                }
            }
        }, context, getCancelledCallback(installEvent));

        bundleInstaller.installBundle();

//        assertEquals(1, jdbcConnsFound.size());
//        assertEquals("Invalid JDBC connection name found", "OAuth", jdbcConnsFound.iterator().next());

        assertTrue("Call to management service was not invoked", invoked[0]);

        assertTrue("Required service not found", servicesFound.get("123797510"));
        assertTrue("Required service not found", servicesFound.get("123797504"));
        assertTrue("Required service not found", servicesFound.get("123797505"));
        assertTrue("Required service not found", servicesFound.get("123797508"));
        assertTrue("Required service not found", servicesFound.get("123797509"));
        assertTrue("Required service not found", servicesFound.get("123797506"));
        assertTrue("Required service not found", servicesFound.get("123797507"));

        for (Map.Entry<String, Set<String>> entry : jdbcPerService.entrySet()) {
            final Set<String> values = entry.getValue();
            if (entry.getKey().equals("123797509")) {
                assertEquals(1, values.size());
                assertEquals("OAuth", values.iterator().next());
            } else {
                assertTrue(values.isEmpty());
            }
        }

    }

    /**
     * That that an installation prefix is appended to each saved policy.
     * @throws Exception
     */
    @Test
    public void testPolicyNamePrefixedInstallation() throws Exception {
        final List<Pair<BundleInfo, String>> bundleInfos = BundleUtils.getBundleInfos(getClass(), "/com/l7tech/external/assertions/policybundleinstaller/bundles/");
        for (Pair<BundleInfo, String> bundleInfo : bundleInfos) {
            installPoliciesTest(bundleInfo.left, "Version 1 - ");
        }

    }

    @Test
    public void testServicesUriPrefixedInstallation() throws Exception {
        final BundleResolver bundleResolver = getBundleResolver();
        final Map<String, String> serviceIdToUri = new HashMap<String, String>();
        final List<BundleInfo> resultList = bundleResolver.getResultList();
        //OAuth_1_0
        final BundleInfo bundleInfo = resultList.get(0);
        final String prefix = "version1a";
        final PolicyBundleInstallerContext context = new PolicyBundleInstallerContext(bundleInfo, new BundleMapping(), prefix, bundleResolver, true);
        final InstallPolicyBundleEvent installEvent = new InstallPolicyBundleEvent(this, context, null);

        final PolicyBundleInstaller bundleInstaller = new PolicyBundleInstaller(new GatewayManagementInvoker() {
            @Override
            public AssertionStatus checkRequest(PolicyEnforcementContext context) throws PolicyAssertionException, IOException {
                try {
                    final Document documentReadOnly = context.getRequest().getXmlKnob().getDocumentReadOnly();
                    final String requestXml = XmlUtil.nodeToString(documentReadOnly);
                    if (requestXml.contains("http://schemas.xmlsoap.org/ws/2004/09/enumeration/Enumerate")) {
                        final Document response = XmlUtil.parse(FILTER_NO_RESULTS);
                        setResponse(context, response);
                        return AssertionStatus.NONE;
                    } else if(requestXml.contains("http://schemas.xmlsoap.org/ws/2004/09/transfer/Create")){

                        ElementCursor cursor = new DomElementCursor(documentReadOnly);
                        // validate the prefix supplied
                        try {
                            XpathResult xpathResult = cursor.getXpathResult(
                                    new XpathExpression(".//l7:UrlPattern", GatewayManagementDocumentUtilities.getNamespaceMap()).compile());
                            final Element urlElement = xpathResult.getNodeSet().getIterator().nextElementAsCursor().asDomElement();
                            final String url = DomUtils.getTextValue(urlElement);

                            // Get service id
                            cursor.moveToDocumentElement();
                            xpathResult = cursor.getXpathResult(new XpathExpression(".//l7:Service", GatewayManagementDocumentUtilities.getNamespaceMap()).compile());
                            final Element serviceElm = xpathResult.getNodeSet().getIterator().nextElementAsCursor().asDomElement();
                            final String serviceId = serviceElm.getAttribute("id");
                            if (serviceId.trim().isEmpty()) {
                                throw new RuntimeException("Serivce id not found");
                            }

                            serviceIdToUri.put(serviceId, url);
                            System.out.println("Found url: " + url);
                        } catch (XPathExpressionException e) {
                            throw new RuntimeException(e);
                        } catch (InvalidXpathException e) {
                            throw new RuntimeException(e);
                        }

                        final Pair<AssertionStatus, Document> documentPair = cannedIdResponse(documentReadOnly);
                        setResponse(context, documentPair.right);
                        return documentPair.left;
                    }

                    throw new RuntimeException("Unexpected request");
                } catch (SAXException e) {
                    throw new RuntimeException(e);
                }
            }
        }, context, getCancelledCallback(installEvent));

        final Document serviceFromBundleDoc = bundleResolver.getBundleItem(bundleInfo.getId(), SERVICE, false);

        bundleInstaller.installServices(getFolderIds(), getPolicyGuids(), serviceFromBundleDoc);

        // validate all services were found and all URIs were prefixed correctly
        assertEquals("Incorrect number of services created", 7, serviceIdToUri.size());

        // hardcoded test resources - validate each service found and correct URI was published
        assertEquals("/" + prefix + "/auth/oauth/v1/token", serviceIdToUri.get("123797510"));
        assertEquals("/" + prefix + "/auth/oauth/v1/*", serviceIdToUri.get("123797505"));
        assertEquals("/" + prefix + "/protected/resource", serviceIdToUri.get("123797504"));
        assertEquals("/" + prefix + "/auth/oauth/v1/request", serviceIdToUri.get("123797507"));
        assertEquals("/" + prefix + "/auth/oauth/v1/authorize", serviceIdToUri.get("123797506"));
        assertEquals("/" + prefix + "/oauth/v1/client", serviceIdToUri.get("123797509"));
        assertEquals("/" + prefix + "/auth/oauth/v1/authorize/website", serviceIdToUri.get("123797508"));
    }

    @Test
    public void testDryInstallationWithConflicts() throws Exception {
        final BundleResolver bundleResolver = getBundleResolver();
        final BundleInfo bundleInfo = new BundleInfo("4e321ca1-83a0-4df5-8216-c2d2bb36067d", "1.0", "Bundle with JDBC references", "Desc");
        bundleInfo.addJdbcReference("OAuth");

        final PolicyBundleInstallerContext context = new PolicyBundleInstallerContext(
                bundleInfo,
                null, null, bundleResolver, true);

        final DryRunInstallPolicyBundleEvent dryRunEvent = new DryRunInstallPolicyBundleEvent(this, context);

        PolicyBundleInstaller installer = new PolicyBundleInstaller(new GatewayManagementInvoker() {
            @Override
            public AssertionStatus checkRequest(PolicyEnforcementContext context) throws PolicyAssertionException, IOException {

                // For policies and services, return that they already exist. For JDBC conns return that they do not exist.
                try {
                    final Document documentReadOnly = context.getRequest().getXmlKnob().getDocumentReadOnly();
                    final String requestXml = XmlUtil.nodeToFormattedString(documentReadOnly);

                    if (requestXml.contains("http://schemas.xmlsoap.org/ws/2004/09/enumeration/Enumerate")) {
                        if (requestXml.contains(InstallerUtils.JDBC_MGMT_NS)) {
                            // no results
                            setResponse(context, XmlUtil.parse(FILTER_NO_RESULTS));
                        } else if (requestXml.contains("/l7:Service/l7:ServiceDetail[@id='123345678']/l7:ServiceMappings/l7:HttpMapping/l7:UrlPattern")) {
                            // no results
                            setResponse(context, XmlUtil.parse(FILTER_NO_RESULTS));
                        } else {
                            // results
                            setResponse(context, XmlUtil.parse(MessageFormat.format(CANNED_ENUMERATE_WITH_FILTER_AND_EPR_RESPONSE, "123345678")));
                        }
                    }
                } catch (Exception e) {
                    fail("Unexpected exception: " + e.getMessage());
                }

                return AssertionStatus.NONE;
            }
        }, context, getCancelledCallback(dryRunEvent));

        installer.dryRunInstallBundle(dryRunEvent);

        final List<String> serviceConflict = dryRunEvent.getServiceConflict();
        assertFalse(serviceConflict.isEmpty());
        assertEquals(8, serviceConflict.size());

        // validate all expected services from the bundle were found
        assertTrue(serviceConflict.contains("/auth/oauth/v1/token"));
        assertTrue(serviceConflict.contains("/auth/oauth/v1/*"));
        assertTrue(serviceConflict.contains("/protected/resource"));
        assertTrue(serviceConflict.contains("/auth/oauth/v1/request"));
        assertTrue(serviceConflict.contains("/auth/oauth/v1/authorize"));
        assertTrue(serviceConflict.contains("/oauth/v1/client"));
        assertTrue(serviceConflict.contains("/auth/oauth/v1/authorize/website"));
        assertTrue(serviceConflict.contains("TestingSoapServiceWithoutResolutionUrl"));


        final List<String> policyWithNameConflict = dryRunEvent.getPolicyConflict();
        assertFalse(policyWithNameConflict.isEmpty());
        assertEquals(7, policyWithNameConflict.size());

        assertTrue(policyWithNameConflict.contains("OAuth 1.0 Context Variables"));
        assertTrue(policyWithNameConflict.contains("Require OAuth 1.0 Token"));
        assertTrue(policyWithNameConflict.contains("getClientSignature"));
        assertTrue(policyWithNameConflict.contains("Authenticate OAuth 1.0 Parameter"));
        assertTrue(policyWithNameConflict.contains("Token Lifetime Context Variables"));
        assertTrue(policyWithNameConflict.contains("GenerateOAuthToken"));
        assertTrue(policyWithNameConflict.contains("OAuth Client Token Store Context Variables"));

        final List<String> certificateConflict = dryRunEvent.getCertificateConflict();
        assertFalse(certificateConflict.isEmpty());
        assertEquals(2, certificateConflict.size());
        assertTrue(certificateConflict.contains("TestBundleCertificateInstallation1"));
        assertTrue(certificateConflict.contains("TestBundleCertificateInstallation2"));

        final List<String> missingAssertions = dryRunEvent.getMissingAssertions();
        assertFalse((missingAssertions.isEmpty()));
        assertEquals(2, missingAssertions.size());

        final List<String> jdbcConnsThatDontExist = dryRunEvent.getJdbcConnsThatDontExist();
        assertFalse(jdbcConnsThatDontExist.isEmpty());
        assertEquals(1, jdbcConnsThatDontExist.size());

        assertTrue(jdbcConnsThatDontExist.contains("OAuth"));
    }

    @Test
    public void testCheckingAssertionExistenceRequired () throws Exception {
        final BundleResolver bundleResolver = getBundleResolver();
        final BundleInfo bundleInfo = new BundleInfo("4e321ca1-83a0-4df5-8216-c2d2bb36067d", "1.0", "Bundle with JDBC references", "Desc");

        final PolicyBundleInstallerContext context = new PolicyBundleInstallerContext(
            bundleInfo,
            null, null, bundleResolver, false);

        final DryRunInstallPolicyBundleEvent dryRunEvent = new DryRunInstallPolicyBundleEvent(this, context);

        PolicyBundleInstaller installer = new PolicyBundleInstaller(new GatewayManagementInvoker() {
            @Override
            public AssertionStatus checkRequest(PolicyEnforcementContext context) throws PolicyAssertionException, IOException {

                // For policies and services, return that they already exist. For JDBC conns return that they do not exist.
                try {
                    final Document documentReadOnly = context.getRequest().getXmlKnob().getDocumentReadOnly();
                    final String requestXml = XmlUtil.nodeToFormattedString(documentReadOnly);

                    if (requestXml.contains("http://schemas.xmlsoap.org/ws/2004/09/enumeration/Enumerate")) {
                        if (requestXml.contains(InstallerUtils.JDBC_MGMT_NS)) {
                            // no results
                            setResponse(context, XmlUtil.parse(FILTER_NO_RESULTS));
                        } else if (requestXml.contains("/l7:Service/l7:ServiceDetail[@id='123345678']/l7:ServiceMappings/l7:HttpMapping/l7:UrlPattern")) {
                            // no results
                            setResponse(context, XmlUtil.parse(FILTER_NO_RESULTS));
                        } else {
                            // results
                            setResponse(context, XmlUtil.parse(MessageFormat.format(CANNED_ENUMERATE_WITH_FILTER_AND_EPR_RESPONSE, "123345678")));
                        }
                    }
                } catch (Exception e) {
                    fail("Unexpected exception: " + e.getMessage());
                }

                return AssertionStatus.NONE;
            }
        }, context, getCancelledCallback(dryRunEvent));

        installer.dryRunInstallBundle(dryRunEvent);
        final List<String> missingAssertions = dryRunEvent.getMissingAssertions();
        assertTrue((missingAssertions.isEmpty()));
    }

    @Test
    public void testDryInstallationWithNoConflicts() throws Exception {
        final BundleResolver bundleResolver = getBundleResolver();
        final BundleInfo bundleInfo = new BundleInfo("4e321ca1-83a0-4df5-8216-c2d2bb36067d", "1.0", "Bundle with JDBC references", "Desc");
        bundleInfo.addJdbcReference("OAuth");

        final PolicyBundleInstallerContext context = new PolicyBundleInstallerContext(
                bundleInfo,
                null, null, bundleResolver, true);

        final DryRunInstallPolicyBundleEvent dryRunEvent = new DryRunInstallPolicyBundleEvent(this, context);

        PolicyBundleInstaller installer = new PolicyBundleInstaller(new GatewayManagementInvoker() {
            @Override
            public AssertionStatus checkRequest(PolicyEnforcementContext context) throws PolicyAssertionException, IOException {

                // For policies and services, return that they already exist. For JDBC conns return that they do not exist.
                try {
                    final Document documentReadOnly = context.getRequest().getXmlKnob().getDocumentReadOnly();
                    final String requestXml = XmlUtil.nodeToFormattedString(documentReadOnly);
//                    System.out.println(requestXml);

                    if (requestXml.contains("http://schemas.xmlsoap.org/ws/2004/09/enumeration/Enumerate")) {
                        if (requestXml.contains(InstallerUtils.JDBC_MGMT_NS)) {
                            // results
                            setResponse(context, XmlUtil.parse(MessageFormat.format(CANNED_ENUMERATE_WITH_FILTER_AND_EPR_RESPONSE, "123345678")));
                        } else {
                            // no results
                            setResponse(context, XmlUtil.parse(FILTER_NO_RESULTS));
                        }
                    }
                } catch (Exception e) {
                    fail("Unexpected exception: " + e.getMessage());
                }

                return AssertionStatus.NONE;
            }
        }, context, getCancelledCallback(dryRunEvent));

        installer.dryRunInstallBundle(dryRunEvent);

        final List<String> urlPatternWithConflict = dryRunEvent.getServiceConflict();
        assertTrue(urlPatternWithConflict.isEmpty());

        final List<String> policyWithNameConflict = dryRunEvent.getPolicyConflict();
        assertTrue(policyWithNameConflict.isEmpty());

        final List<String> jdbcConnsThatDontExist = dryRunEvent.getJdbcConnsThatDontExist();
        assertTrue(jdbcConnsThatDontExist.isEmpty());
    }

    @Test
    @BugNumber(13586)
    public void testRequestDeniedForAdminUser() throws Exception {
        final BundleResolver bundleResolver = getBundleResolver();
        final BundleInfo bundleInfo = new BundleInfo("4e321ca1-83a0-4df5-8216-c2d2bb36067d", "1.0", "Any bundle will do", "Desc");
        bundleInfo.addJdbcReference("OAuth");

        final PolicyBundleInstallerContext context = new PolicyBundleInstallerContext(
                bundleInfo,
                null, null, bundleResolver, true);

        final InstallPolicyBundleEvent installEvent = new InstallPolicyBundleEvent(this, context, null);

        PolicyBundleInstaller installer = new PolicyBundleInstaller(new GatewayManagementInvoker() {
            @Override
            public AssertionStatus checkRequest(PolicyEnforcementContext context) throws PolicyAssertionException, IOException {

                try {
                    final Document documentReadOnly = context.getRequest().getXmlKnob().getDocumentReadOnly();
                    final String requestXml = XmlUtil.nodeToFormattedString(documentReadOnly);
                    if (requestXml.contains("http://schemas.xmlsoap.org/ws/2004/09/enumeration/Enumerate")) {
                        // say it doesn't exist
                        setResponse(context, XmlUtil.parse(FILTER_NO_RESULTS));
                    } else {
                        // access denied for first create
                        setResponse(context, XmlUtil.parse(ACCESS_DENIED));
                    }
                } catch (Exception e) {
                    fail("Unexpected exception: " + e.getMessage());
                }

                return AssertionStatus.NONE;
            }
        }, context, getCancelledCallback(installEvent));

        try {
            installer.installBundle();
            fail("Access denied exception should be thrown");
        } catch (GatewayManagementDocumentUtilities.AccessDeniedManagementResponse e) {
            // pass
            // validate that hte denied request was set and is non empty
            assertNotNull(e.getDeniedRequest());
            assertFalse(e.getDeniedRequest().trim().isEmpty());
        }
    }

    @NotNull
    private BundleResolver getBundleResolver(){

        final Map<String, Map<String, Document>> bundleToItemAndDocMap = new HashMap<String, Map<String, Document>>();
        final String cannedId = "4e321ca1-83a0-4df5-8216-c2d2bb36067d";
        bundleToItemAndDocMap.put(cannedId, getItemsToDocs(true));

        return new BundleResolver() {
            @Override
            public Document getBundleItem(@NotNull String bundleId, @NotNull BundleItem bundleItem, boolean allowMissing) throws UnknownBundleException, BundleResolverException {
                final Map<String, Document> itemToDocMap = bundleToItemAndDocMap.get(bundleId);
                return itemToDocMap.get(bundleItem.getFileName());
            }

            @NotNull
            @Override
            public List<BundleInfo> getResultList() {
                return Arrays.asList(new BundleInfo(cannedId, "1.0", "Name", "Desc"));
            }
        };
    }

    private Document getDocumentFromResource(String resource) {
        final URL resourceUrl = getClass().getResource(resource);
        final byte[] bytes;
        try {
            bytes = IOUtils.slurpUrl(resourceUrl);
            return XmlUtil.parse(new ByteArrayInputStream(bytes));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private Map<String, Document> getItemsToDocs(boolean hasPolicy) {
        final Map<String, Document> itemsToDocs = new HashMap<String, Document>();
        final String baseName = "/com/l7tech/external/assertions/policybundleinstaller/bundles/Bundle1";
        itemsToDocs.put("Folder.xml", getDocumentFromResource(baseName + "/Folder.xml"));
        itemsToDocs.put("Service.xml", getDocumentFromResource(baseName + "/Service.xml"));
        itemsToDocs.put("TrustedCertificate.xml", getDocumentFromResource(baseName + "/TrustedCertificate.xml"));
        itemsToDocs.put("Assertion.xml", getDocumentFromResource(baseName + "/Assertion.xml"));

        if (hasPolicy) {
            itemsToDocs.put("Policy.xml", getDocumentFromResource(baseName + "/Policy.xml"));
        }

        return itemsToDocs;
    }

    private Map<Long, Long> getFolderIds() {
        // fake the folder ids
        return new HashMap<Long, Long>(){
            @Override
            public Long get(Object key) {
                return Long.valueOf(key.toString());
            }

            @Override
            public boolean containsKey(Object key) {
                return true;
            }
        };
    }

    private Map<String, String> getPolicyGuids() {
        return new HashMap<String, String>(){
            @Override
            public String get(Object key) {
                return UUID.randomUUID().toString();
            }

            @Override
            public boolean containsKey(Object key) {
                return true;
            }
        };
    }


    private Pair<AssertionStatus, Document> cannedIdResponse(Document requestXml) {
        try {
            System.out.println(XmlUtil.nodeToFormattedString(requestXml));
            final String format = MessageFormat.format(CANNED_CREATE_ID_RESPONSE_TEMPLATE, String.valueOf(nextOid++));
            final Document parse = XmlUtil.parse(format);
            return new Pair<AssertionStatus, Document>(
                    AssertionStatus.NONE,
                    parse);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private void setResponse(PolicyEnforcementContext context, String response) {
        try {
            setResponse(context, XmlUtil.parse(response));
        } catch (SAXException e) {
            throw new RuntimeException(e);
        }
    }

    public static void setResponse(PolicyEnforcementContext context, Document response) {
        final Message responseMsg = context.getResponse();
        responseMsg.initialize(response);
    }

    /**
     * This is a canned response useful for faking a create ID - don't use to verify types, message ids etc
     */
    private final static String CANNED_CREATE_ID_RESPONSE_TEMPLATE = "<env:Envelope xmlns:env=\"http://www.w3.org/2003/05/soap-envelope\"\n" +
            "    xmlns:mdo=\"http://schemas.wiseman.dev.java.net/metadata/messagetypes\"\n" +
            "    xmlns:mex=\"http://schemas.xmlsoap.org/ws/2004/09/mex\"\n" +
            "    xmlns:wsa=\"http://schemas.xmlsoap.org/ws/2004/08/addressing\"\n" +
            "    xmlns:wse=\"http://schemas.xmlsoap.org/ws/2004/08/eventing\"\n" +
            "    xmlns:wsen=\"http://schemas.xmlsoap.org/ws/2004/09/enumeration\"\n" +
            "    xmlns:wsman=\"http://schemas.dmtf.org/wbem/wsman/1/wsman.xsd\"\n" +
            "    xmlns:wsmeta=\"http://schemas.dmtf.org/wbem/wsman/1/wsman/version1.0.0.a/default-addressing-model.xsd\"\n" +
            "    xmlns:wxf=\"http://schemas.xmlsoap.org/ws/2004/09/transfer\" xmlns:xs=\"http://www.w3.org/2001/XMLSchema\">\n" +
            "    <env:Header>\n" +
            "        <wsa:Action env:mustUnderstand=\"true\" xmlns:l7=\"http://ns.l7tech.com/2010/04/gateway-management\">http://schemas.xmlsoap.org/ws/2004/09/transfer/CreateResponse</wsa:Action>\n" +
            "        <wsa:MessageID env:mustUnderstand=\"true\" xmlns:l7=\"http://ns.l7tech.com/2010/04/gateway-management\">uuid:ce1f79e3-479d-4602-a93c-230bfe0f6050</wsa:MessageID>\n" +
            "        <wsa:RelatesTo xmlns:l7=\"http://ns.l7tech.com/2010/04/gateway-management\">uuid:73442d63-37ee-4908-8d18-c635e327d515</wsa:RelatesTo>\n" +
            "        <wsa:To env:mustUnderstand=\"true\" xmlns:l7=\"http://ns.l7tech.com/2010/04/gateway-management\">http://schemas.xmlsoap.org/ws/2004/08/addressing/role/anonymous</wsa:To>\n" +
            "    </env:Header>\n" +
            "    <env:Body>\n" +
            "        <wxf:ResourceCreated xmlns:l7=\"http://ns.l7tech.com/2010/04/gateway-management\">\n" +
            "            <wsa:Address env:mustUnderstand=\"true\">https://localhost:9443/wsman/</wsa:Address>\n" +
            "            <wsa:ReferenceParameters>\n" +
            "                <wsman:ResourceURI>http://ns.l7tech.com/2010/04/gateway-management/folders</wsman:ResourceURI>\n" +
            "                <wsman:SelectorSet>\n" +
            "                    <wsman:Selector Name=\"id\">{0}</wsman:Selector>\n" +
            "                </wsman:SelectorSet>\n" +
            "            </wsa:ReferenceParameters>\n" +
            "        </wxf:ResourceCreated>\n" +
            "    </env:Body>\n" +
            "</env:Envelope>";

    private final static String CANNED_ENUMERATE_WITH_FILTER_AND_EPR_RESPONSE = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"+
            "<env:Envelope xmlns:env=\"http://www.w3.org/2003/05/soap-envelope\"\n"+
            "    xmlns:l7=\"http://ns.l7tech.com/2010/04/gateway-management\"\n"+
            "    xmlns:mdo=\"http://schemas.wiseman.dev.java.net/metadata/messagetypes\"\n"+
            "    xmlns:mex=\"http://schemas.xmlsoap.org/ws/2004/09/mex\"\n"+
            "    xmlns:wsa=\"http://schemas.xmlsoap.org/ws/2004/08/addressing\"\n"+
            "    xmlns:wse=\"http://schemas.xmlsoap.org/ws/2004/08/eventing\"\n"+
            "    xmlns:wsen=\"http://schemas.xmlsoap.org/ws/2004/09/enumeration\"\n"+
            "    xmlns:wsman=\"http://schemas.dmtf.org/wbem/wsman/1/wsman.xsd\"\n"+
            "    xmlns:wsmeta=\"http://schemas.dmtf.org/wbem/wsman/1/wsman/version1.0.0.a/default-addressing-model.xsd\"\n"+
            "    xmlns:wxf=\"http://schemas.xmlsoap.org/ws/2004/09/transfer\" xmlns:xs=\"http://www.w3.org/2001/XMLSchema\">\n"+
            "    <env:Header>\n"+
            "        <wsa:Action env:mustUnderstand=\"true\" xmlns:l7=\"http://ns.l7tech.com/2010/04/gateway-management\">http://schemas.xmlsoap.org/ws/2004/09/enumeration/EnumerateResponse</wsa:Action>\n"+
            "        <wsman:TotalItemsCountEstimate>56</wsman:TotalItemsCountEstimate>\n"+
            "        <wsa:MessageID env:mustUnderstand=\"true\">uuid:580d6add-30b5-468f-be33-cbe2112ce845</wsa:MessageID>\n"+
            "        <wsa:RelatesTo>uuid:53418e52-f8a1-4c8e-94b7-a3f4fe33d8f3</wsa:RelatesTo>\n"+
            "        <wsa:To env:mustUnderstand=\"true\">http://schemas.xmlsoap.org/ws/2004/08/addressing/role/anonymous</wsa:To>\n"+
            "    </env:Header>\n"+
            "    <env:Body>\n"+
            "        <wsen:EnumerateResponse>\n"+
            "            <wsen:Expires>2147483647-12-31T23:59:59.999-14:00</wsen:Expires>\n"+
            "            <wsen:EnumerationContext>55bd9e59-9dc7-43c9-8d6a-4e2c2f940659</wsen:EnumerationContext>\n"+
            "            <wsman:Items>\n"+
            "                <wsman:Item>\n"+
            "                    <wsman:XmlFragment>\n"+
            "                        <l7:Name>OAuth</l7:Name>\n"+
            "                    </wsman:XmlFragment>\n"+
            "                    <wsa:EndpointReference>\n"+
            "                        <wsa:Address env:mustUnderstand=\"true\">http://127.0.0.1:80/wsman</wsa:Address>\n"+
            "                        <wsa:ReferenceParameters>\n"+
            "                            <wsman:ResourceURI>http://ns.l7tech.com/2010/04/gateway-management/folders</wsman:ResourceURI>\n"+
            "                            <wsman:SelectorSet>\n"+
            "                                <wsman:Selector Name=\"id\">{0}</wsman:Selector>\n"+
            "                            </wsman:SelectorSet>\n"+
            "                        </wsa:ReferenceParameters>\n"+
            "                    </wsa:EndpointReference>\n"+
            "                </wsman:Item>\n"+
            "            </wsman:Items>\n"+
            "            <wsman:EndOfSequence/>\n"+
            "        </wsen:EnumerateResponse>\n"+
            "    </env:Body>\n"+
            "</env:Envelope>";

    /**
     * The guid and name are configurable. Everything else about this xml should be ignored.
     */
    private static final String CANNED_GET_POLICY_RESPONSE = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
            "<env:Envelope xmlns:env=\"http://www.w3.org/2003/05/soap-envelope\"\n" +
            "    xmlns:l7=\"http://ns.l7tech.com/2010/04/gateway-management\"\n" +
            "    xmlns:mdo=\"http://schemas.wiseman.dev.java.net/metadata/messagetypes\"\n" +
            "    xmlns:mex=\"http://schemas.xmlsoap.org/ws/2004/09/mex\"\n" +
            "    xmlns:wsa=\"http://schemas.xmlsoap.org/ws/2004/08/addressing\"\n" +
            "    xmlns:wse=\"http://schemas.xmlsoap.org/ws/2004/08/eventing\"\n" +
            "    xmlns:wsen=\"http://schemas.xmlsoap.org/ws/2004/09/enumeration\"\n" +
            "    xmlns:wsman=\"http://schemas.dmtf.org/wbem/wsman/1/wsman.xsd\"\n" +
            "    xmlns:wsmeta=\"http://schemas.dmtf.org/wbem/wsman/1/wsman/version1.0.0.a/default-addressing-model.xsd\"\n" +
            "    xmlns:wxf=\"http://schemas.xmlsoap.org/ws/2004/09/transfer\" xmlns:xs=\"http://www.w3.org/2001/XMLSchema\">\n" +
            "    <env:Header>\n" +
            "        <wsa:Action env:mustUnderstand=\"true\" xmlns:l7=\"http://ns.l7tech.com/2010/04/gateway-management\">http://schemas.xmlsoap.org/ws/2004/09/transfer/GetResponse</wsa:Action>\n" +
            "        <wsa:MessageID env:mustUnderstand=\"true\">uuid:165d8824-4171-4753-99b6-85e68a894982</wsa:MessageID>\n" +
            "        <wsa:RelatesTo>uuid:f15c6ec8-a8f7-499d-86f8-501cf93aa9ab</wsa:RelatesTo>\n" +
            "        <wsa:To env:mustUnderstand=\"true\">http://schemas.xmlsoap.org/ws/2004/08/addressing/role/anonymous</wsa:To>\n" +
            "    </env:Header>\n" +
            "    <env:Body>\n" +
            "        <l7:Policy guid=\"{0}\"\n" +
            "            id=\"139198464\" version=\"0\">\n" +
            "            <l7:PolicyDetail folderId=\"139067402\"\n" +
            "                guid=\"42a43b15-27ad-41c4-b534-d7dbcec2aa7f\"\n" +
            "                id=\"139198464\" version=\"0\">\n" +
            "                <l7:Name>{1}</l7:Name>\n" +
            "                <l7:PolicyType>Include</l7:PolicyType>\n" +
            "                <l7:Properties>\n" +
            "                    <l7:Property key=\"revision\">\n" +
            "                        <l7:LongValue>0</l7:LongValue>\n" +
            "                    </l7:Property>\n" +
            "                    <l7:Property key=\"soap\">\n" +
            "                        <l7:BooleanValue>false</l7:BooleanValue>\n" +
            "                    </l7:Property>\n" +
            "                </l7:Properties>\n" +
            "            </l7:PolicyDetail>\n" +
            "            <l7:Resources>\n" +
            "                <l7:ResourceSet tag=\"policy\">\n" +
            "                    <l7:Resource type=\"policy\">&lt;?xml version=\"1.0\" encoding=\"UTF-8\"?&gt;\n" +
            "                    &lt;wsp:Policy xmlns:L7p=\"http://www.layer7tech.com/ws/policy\"\n" +
            "                    xmlns:wsp=\"http://schemas.xmlsoap.org/ws/2002/12/policy\"&gt;\n" +
            "                    &lt;wsp:All wsp:Usage=\"Required\"&gt;\n" +
            "                    &lt;L7p:AuditDetailAssertion&gt;\n" +
            "                    &lt;L7p:Detail stringValue=\"Policy Fragment: OAuth Context Variables\"/&gt;\n" +
            "                    &lt;/L7p:AuditDetailAssertion&gt;\n" +
            "                    &lt;L7p:SetVariable&gt;\n" +
            "                    &lt;L7p:Base64Expression stringValue=\"aHR0cHM6Ly9sb2NhbGhvc3Q6ODQ0Mw==\"/&gt;\n" +
            "                    &lt;L7p:VariableToSet stringValue=\"host_oauth_ovp_server\"/&gt;\n" +
            "                    &lt;/L7p:SetVariable&gt;\n" +
            "                    &lt;L7p:SetVariable&gt;\n" +
            "                    &lt;L7p:Base64Expression\n" +
            "                    stringValue=\"aHR0cHM6Ly9sb2NhbGhvc3Q6ODQ0My9hdXRoL29hdXRoL3YxL2F1dGhvcml6ZS93ZWJzaXRl\"/&gt;\n" +
            "                    &lt;L7p:VariableToSet stringValue=\"host_oauth_v1_server_website\"/&gt;\n" +
            "                    &lt;/L7p:SetVariable&gt;\n" +
            "                    &lt;L7p:SetVariable&gt;\n" +
            "                    &lt;L7p:Base64Expression stringValue=\"aHR0cHM6Ly9sb2NhbGhvc3Q6ODQ0Mw==\"/&gt;\n" +
            "                    &lt;L7p:VariableToSet stringValue=\"host_oauth_v1_server_website_baseuri\"/&gt;\n" +
            "                    &lt;/L7p:SetVariable&gt;\n" +
            "                    &lt;L7p:ExportVariables&gt;\n" +
            "                    &lt;L7p:ExportedVars stringArrayValue=\"included\"&gt;\n" +
            "                    &lt;L7p:item stringValue=\"host_oauth_ovp_server\"/&gt;\n" +
            "                    &lt;L7p:item stringValue=\"host_oauth_v1_server_website\"/&gt;\n" +
            "                    &lt;L7p:item stringValue=\"host_oauth_v1_server_website_baseuri\"/&gt;\n" +
            "                    &lt;/L7p:ExportedVars&gt;\n" +
            "                    &lt;/L7p:ExportVariables&gt;\n" +
            "                    &lt;/wsp:All&gt;\n" +
            "                    &lt;/wsp:Policy&gt;\n" +
            "                </l7:Resource>\n" +
            "                </l7:ResourceSet>\n" +
            "            </l7:Resources>\n" +
            "        </l7:Policy>\n" +
            "    </env:Body>\n" +
            "</env:Envelope>";

    final String alreadyExistsResponse = "<env:Envelope xmlns:env=\"http://www.w3.org/2003/05/soap-envelope\"\n" +
            "    xmlns:mdo=\"http://schemas.wiseman.dev.java.net/metadata/messagetypes\"\n" +
            "    xmlns:mex=\"http://schemas.xmlsoap.org/ws/2004/09/mex\"\n" +
            "    xmlns:wsa=\"http://schemas.xmlsoap.org/ws/2004/08/addressing\"\n" +
            "    xmlns:wse=\"http://schemas.xmlsoap.org/ws/2004/08/eventing\"\n" +
            "    xmlns:wsen=\"http://schemas.xmlsoap.org/ws/2004/09/enumeration\"\n" +
            "    xmlns:wsman=\"http://schemas.dmtf.org/wbem/wsman/1/wsman.xsd\"\n" +
            "    xmlns:wsmeta=\"http://schemas.dmtf.org/wbem/wsman/1/wsman/version1.0.0.a/default-addressing-model.xsd\"\n" +
            "    xmlns:wxf=\"http://schemas.xmlsoap.org/ws/2004/09/transfer\" xmlns:xs=\"http://www.w3.org/2001/XMLSchema\">\n" +
            "    <env:Header>\n" +
            "        <wsa:Action env:mustUnderstand=\"true\" xmlns:l7=\"http://ns.l7tech.com/2010/04/gateway-management\">http://schemas.dmtf.org/wbem/wsman/1/wsman/fault</wsa:Action>\n" +
            "        <wsa:MessageID env:mustUnderstand=\"true\" xmlns:l7=\"http://ns.l7tech.com/2010/04/gateway-management\">uuid:7486aa54-f144-4656-badf-16d9570fc37d</wsa:MessageID>\n" +
            "        <wsa:RelatesTo xmlns:l7=\"http://ns.l7tech.com/2010/04/gateway-management\">uuid:6a947b0a-415d-490d-a1d1-fcf57a2ba329</wsa:RelatesTo>\n" +
            "        <wsa:To env:mustUnderstand=\"true\" xmlns:l7=\"http://ns.l7tech.com/2010/04/gateway-management\">http://schemas.xmlsoap.org/ws/2004/08/addressing/role/anonymous</wsa:To>\n" +
            "    </env:Header>\n" +
            "    <env:Body>\n" +
            "        <env:Fault xmlns:l7=\"http://ns.l7tech.com/2010/04/gateway-management\">\n" +
            "            <env:Code>\n" +
            "                <env:Value>env:Sender</env:Value>\n" +
            "                <env:Subcode>\n" +
            "                    <env:Value>wsman:AlreadyExists</env:Value>\n" +
            "                </env:Subcode>\n" +
            "            </env:Code>\n" +
            "            <env:Reason>\n" +
            "                <env:Text xml:lang=\"en-US\">The sender attempted to create a resource which already exists.</env:Text>\n" +
            "            </env:Reason>\n" +
            "            <env:Detail>\n" +
            "                <env:Text xml:lang=\"en-US\">(folder, name)  must be unique</env:Text>\n" +
            "            </env:Detail>\n" +
            "        </env:Fault>\n" +
            "    </env:Body>\n" +
            "</env:Envelope>";

    private static final String FILTER_NO_RESULTS = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
            "<env:Envelope xmlns:env=\"http://www.w3.org/2003/05/soap-envelope\"\n" +
            "    xmlns:mdo=\"http://schemas.wiseman.dev.java.net/metadata/messagetypes\"\n" +
            "    xmlns:mex=\"http://schemas.xmlsoap.org/ws/2004/09/mex\"\n" +
            "    xmlns:wsa=\"http://schemas.xmlsoap.org/ws/2004/08/addressing\"\n" +
            "    xmlns:wse=\"http://schemas.xmlsoap.org/ws/2004/08/eventing\"\n" +
            "    xmlns:wsen=\"http://schemas.xmlsoap.org/ws/2004/09/enumeration\"\n" +
            "    xmlns:wsman=\"http://schemas.dmtf.org/wbem/wsman/1/wsman.xsd\"\n" +
            "    xmlns:wsmeta=\"http://schemas.dmtf.org/wbem/wsman/1/wsman/version1.0.0.a/default-addressing-model.xsd\"\n" +
            "    xmlns:wxf=\"http://schemas.xmlsoap.org/ws/2004/09/transfer\" xmlns:xs=\"http://www.w3.org/2001/XMLSchema\">\n" +
            "    <env:Header>\n" +
            "        <wsa:Action env:mustUnderstand=\"true\" xmlns:l7=\"http://ns.l7tech.com/2010/04/gateway-management\">http://schemas.xmlsoap.org/ws/2004/09/enumeration/EnumerateResponse</wsa:Action>\n" +
            "        <wsman:TotalItemsCountEstimate xmlns:l7=\"http://ns.l7tech.com/2010/04/gateway-management\">135</wsman:TotalItemsCountEstimate>\n" +
            "        <wsa:MessageID env:mustUnderstand=\"true\" xmlns:l7=\"http://ns.l7tech.com/2010/04/gateway-management\">uuid:19c6ab84-e9e7-4bf1-8114-b2d74ba41f6d</wsa:MessageID>\n" +
            "        <wsa:RelatesTo xmlns:l7=\"http://ns.l7tech.com/2010/04/gateway-management\">uuid:cdf8352f-eb06-4d90-a728-c7fa7560adca</wsa:RelatesTo>\n" +
            "        <wsa:To env:mustUnderstand=\"true\" xmlns:l7=\"http://ns.l7tech.com/2010/04/gateway-management\">http://schemas.xmlsoap.org/ws/2004/08/addressing/role/anonymous</wsa:To>\n" +
            "    </env:Header>\n" +
            "    <env:Body>\n" +
            "        <wsen:EnumerateResponse xmlns:l7=\"http://ns.l7tech.com/2010/04/gateway-management\">\n" +
            "            <wsen:Expires>2147483647-12-31T23:59:59.999-14:00</wsen:Expires>\n" +
            "            <wsen:EnumerationContext>85bc6f2a-3c00-45df-87fa-056f623d0dd8</wsen:EnumerationContext>\n" +
            "            <wsman:Items/>\n" +
            "            <wsman:EndOfSequence/>\n" +
            "        </wsen:EnumerateResponse>\n" +
            "    </env:Body>\n" +
            "</env:Envelope>\n";

    private static final String ACCESS_DENIED = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
            "<env:Envelope xmlns:env=\"http://www.w3.org/2003/05/soap-envelope\"\n" +
            "    xmlns:mdo=\"http://schemas.wiseman.dev.java.net/metadata/messagetypes\"\n" +
            "    xmlns:mex=\"http://schemas.xmlsoap.org/ws/2004/09/mex\"\n" +
            "    xmlns:wsa=\"http://schemas.xmlsoap.org/ws/2004/08/addressing\"\n" +
            "    xmlns:wse=\"http://schemas.xmlsoap.org/ws/2004/08/eventing\"\n" +
            "    xmlns:wsen=\"http://schemas.xmlsoap.org/ws/2004/09/enumeration\"\n" +
            "    xmlns:wsman=\"http://schemas.dmtf.org/wbem/wsman/1/wsman.xsd\"\n" +
            "    xmlns:wsmeta=\"http://schemas.dmtf.org/wbem/wsman/1/wsman/version1.0.0.a/default-addressing-model.xsd\"\n" +
            "    xmlns:wxf=\"http://schemas.xmlsoap.org/ws/2004/09/transfer\" xmlns:xs=\"http://www.w3.org/2001/XMLSchema\">\n" +
            "    <env:Header>\n" +
            "        <wsa:Action env:mustUnderstand=\"true\" xmlns:l7=\"http://ns.l7tech.com/2010/04/gateway-management\">http://schemas.dmtf.org/wbem/wsman/1/wsman/fault</wsa:Action>\n" +
            "        <wsa:MessageID env:mustUnderstand=\"true\" xmlns:l7=\"http://ns.l7tech.com/2010/04/gateway-management\">uuid:ca86cfbf-ecff-430e-8f98-6e02a50f1367</wsa:MessageID>\n" +
            "        <wsa:RelatesTo xmlns:l7=\"http://ns.l7tech.com/2010/04/gateway-management\">uuid:6a459d19-4603-4934-9a4b-8182e2fc56c1</wsa:RelatesTo>\n" +
            "        <wsa:To env:mustUnderstand=\"true\" xmlns:l7=\"http://ns.l7tech.com/2010/04/gateway-management\">http://schemas.xmlsoap.org/ws/2004/08/addressing/role/anonymous</wsa:To>\n" +
            "    </env:Header>\n" +
            "    <env:Body>\n" +
            "        <env:Fault xmlns:l7=\"http://ns.l7tech.com/2010/04/gateway-management\">\n" +
            "            <env:Code>\n" +
            "                <env:Value>env:Sender</env:Value>\n" +
            "                <env:Subcode>\n" +
            "                    <env:Value>wsman:AccessDenied</env:Value>\n" +
            "                </env:Subcode>\n" +
            "            </env:Code>\n" +
            "            <env:Reason>\n" +
            "                <env:Text xml:lang=\"en-US\">The sender was not authorized to access the resource.</env:Text>\n" +
            "            </env:Reason>\n" +
            "            <env:Detail/>\n" +
            "        </env:Fault>\n" +
            "    </env:Body>\n" +
            "</env:Envelope>\n";
}
