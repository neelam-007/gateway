package com.l7tech.external.assertions.oauthinstaller;

import com.l7tech.common.io.XmlUtil;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.util.DomUtils;
import com.l7tech.util.IOUtils;
import com.l7tech.util.Pair;
import com.l7tech.xml.DomElementCursor;
import com.l7tech.xml.ElementCursor;
import com.l7tech.xml.xpath.XpathExpression;
import com.l7tech.xml.xpath.XpathResult;
import com.l7tech.xml.xpath.XpathResultIterator;
import org.jetbrains.annotations.NotNull;
import org.junit.Ignore;
import org.junit.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import java.io.ByteArrayInputStream;
import java.net.URL;
import java.text.MessageFormat;
import java.util.*;

import static org.junit.Assert.*;

public class OAuthInstallerAdminImplTest {

    @Test
    public void testResponse_GetCreatedId() throws Exception {
        final Document doc = XmlUtil.parse(createdResponse);
        final Long createdId = GatewayManagementDocumentUtilities.getCreatedId(doc);
        assertNotNull(createdId);
        assertEquals(134807552L, createdId.longValue());

    }

    @Test
    public void testErrorResponse_GetCreatedId() throws Exception {
        final Document doc = XmlUtil.parse(errorResponse);
        final OAuthInstallerAdminImpl oAuthInstallerAdmin = new OAuthInstallerAdminImpl();
        final Long createdId = GatewayManagementDocumentUtilities.getCreatedId(doc);
        assertNull(createdId);
    }

    @Test
    public void testResponse_ResourceAlreadyExists() throws Exception {
        final Document doc = XmlUtil.parse(alreadyExistsResponse);
        final OAuthInstallerAdminImpl oAuthInstallerAdmin = new OAuthInstallerAdminImpl();
        assertTrue(GatewayManagementDocumentUtilities.resourceAlreadyExists(doc));
    }

    @Test
    public void testResponse_ErrorValues() throws Exception {
        final Document doc = XmlUtil.parse(alreadyExistsResponse);
        final OAuthInstallerAdminImpl oAuthInstallerAdmin = new OAuthInstallerAdminImpl();
        final List<String> errorDetails = GatewayManagementDocumentUtilities.getErrorDetails(doc);
        assertTrue(errorDetails.contains("env:Sender"));
        assertTrue(errorDetails.contains("wsman:AlreadyExists"));
    }

    private int nextOid = 1000000;

    @Test
    public void testInstallOfAllBundleFolders() throws Exception {
//        for (String bundleName : ALL_BUNDLE_NAMES) {
//            testInstallFolders_NoneExist(bundleName);
//        }
        // OAuth_2_0
        testInstallFolders_NoneExist("ba525763-6e55-4748-9376-76055247c8b1");
    }

    //todo fix test coverage for resources loaded from an aar
    @Ignore
    @Test
    public void testListAllBundles() throws Exception {
                //todo test unique ids
        final OAuthInstallerAdminImpl admin = new OAuthInstallerAdminImpl();
        admin.loadBundles();

        final List<BundleInfo> allBundles = admin.getAllOtkComponents();
        assertNotNull(allBundles);

        BundleInfo expected;

        expected = new BundleInfo("1c2a2874-df8d-4e1d-b8b0-099b576407e1", "1.0", "OAuth 1.0", "Core Services and Test Client");
        assertTrue(expected.toString(), allBundles.contains(expected));

        expected = new BundleInfo("ba525763-6e55-4748-9376-76055247c8b1", "1.0", "OAuth 2.0", "Auth Server and Test Clients");
        assertTrue(expected.toString(), allBundles.contains(expected));

        expected = new BundleInfo("f69c7d15-4999-4761-ab26-d29d58c0dd57", "1.0", "Secure Zone OVP", "OVP - OAuth Validation Point");
        assertTrue(expected.toString(), allBundles.contains(expected));

        expected = new BundleInfo("b082274b-f00e-4fbf-bbb7-395a95ca2a35", "1.0", "SecureZone Storage", "Token and Client Store");
        assertTrue(expected.toString(), allBundles.contains(expected));

        expected = new BundleInfo("a07924c0-0265-42ea-90f1-2428e31ae5ae", "1.0", "OAuth Manager", "Manager utility for Client and Token store for OAuth 1.0 and 2.0");
        assertTrue(expected.toString(), allBundles.contains(expected));
    }

    /**
     * Test the success case when no folders already exist.
     */
    public void testInstallFolders_NoneExist(String bundleId) throws Exception {

        final BundleResolver bundleResolver = getBundleResolver();
        final BundleInstaller bundleInstaller = new BundleInstaller(bundleResolver){
            @NotNull
            @Override
            protected Pair<AssertionStatus, Document> callManagementAssertion(String requestXml) {
                return cannedIdResponse(requestXml);
            }
        };

        final OAuthInstallerAdminImpl oAuthInstallerAdmin = new OAuthInstallerAdminImpl();
        oAuthInstallerAdmin.setBundleInstaller(bundleInstaller);

        final Document oAuth_1_0 = bundleResolver.getBundleItem(bundleId, "Folder.xml", false);
        final Map<Long,Long> oldToNewMap = bundleInstaller.installFolders(-5002, oAuth_1_0, new HashMap<Long, Long>());
        assertNotNull(oldToNewMap);
        assertFalse(oldToNewMap.isEmpty());

        for (Map.Entry<Long, Long> entry : oldToNewMap.entrySet()) {
            System.out.println(entry.getKey() + ": " + entry.getValue());
        }
    }

    private Pair<AssertionStatus, Document> cannedIdResponse(String requestXml) {
        try {
            System.out.println(XmlUtil.nodeToFormattedString(XmlUtil.parse(requestXml)));
            final String format = MessageFormat.format(CANNED_CREATE_ID_RESPONSE_TEMPLATE, String.valueOf(nextOid++));
//                    System.out.println(format);
//                    System.out.println();
            final Document parse = XmlUtil.parse(format);
            return new Pair<AssertionStatus, Document>(
                    AssertionStatus.NONE,
                    parse);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Test the success case when all folders already exist.
     * @throws Exception
     */
    @Test
    public void testInstallFolders_AllExist() throws Exception {
        final Map<String, Integer> nameToIdMap = new HashMap<String, Integer>();

        final BundleResolver bundleResolver = getBundleResolver();
        final BundleInstaller bundleInstaller = new BundleInstaller(bundleResolver){
            @NotNull
            @Override
            protected Pair<AssertionStatus, Document> callManagementAssertion(String requestXml) {
                try {
                    if (requestXml.contains("http://schemas.xmlsoap.org/ws/2004/09/transfer/Create")) {
                        System.out.println(XmlUtil.nodeToFormattedString(XmlUtil.parse(requestXml)));
                        final Document parse = XmlUtil.parse(alreadyExistsResponse);
                        return new Pair<AssertionStatus, Document>(
                                AssertionStatus.NONE,
                                parse);
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
                        final Document parse = XmlUtil.parse(response);
                        return new Pair<AssertionStatus, Document>(AssertionStatus.NONE, parse);
                    }
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }
        };

        final OAuthInstallerAdminImpl oAuthInstallerAdmin = new OAuthInstallerAdminImpl();
        oAuthInstallerAdmin.setBundleInstaller(bundleInstaller);

        //OAuth_1_0
        final Document oAuth_1_0 = bundleResolver.getBundleItem("1c2a2874-df8d-4e1d-b8b0-099b576407e1", "Folder.xml", false);
        final Map<Long,Long> oldToNewMap = bundleInstaller.installFolders(-5002, oAuth_1_0, new HashMap<Long, Long>());
        assertNotNull(oldToNewMap);
        assertFalse(oldToNewMap.isEmpty());

        for (Map.Entry<Long, Long> entry : oldToNewMap.entrySet()) {
            System.out.println(entry.getKey() + ": " + entry.getValue());
        }

    }

    final static List<Pair<String, String>> ALL_BUNDLE_NAMES =
            Collections.unmodifiableList(
                    Arrays.asList(
                            new Pair<String, String>("1c2a2874-df8d-4e1d-b8b0-099b576407e1", "OAuth_1_0"),
                            new Pair<String, String>("ba525763-6e55-4748-9376-76055247c8b1", "OAuth_2_0"),
                            new Pair<String, String>("f69c7d15-4999-4761-ab26-d29d58c0dd57", "SecureZone_OVP"),
                            new Pair<String, String>("b082274b-f00e-4fbf-bbb7-395a95ca2a35", "SecureZone_Storage"),
                            new Pair<String, String>("a07924c0-0265-42ea-90f1-2428e31ae5ae", "StorageManager")));

    @Test
    public void testAllPolicyBundlesInstall() throws Exception {

        final Map<String, String> oldGuidsToNewGuids = new HashMap<String, String>(){
            @Override
            public String put(String key, String value) {

                if (containsKey(key)) {
                    fail("Key already recorded - map should be checked before creating any new guids");
                }

                return super.put(key, value);
            }
        };
        for (Pair<String, String> bundlePair : ALL_BUNDLE_NAMES) {
            System.out.println("Testing install of policies from bundle " + bundlePair);
            installPoliciesTest(bundlePair.left, oldGuidsToNewGuids);
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
     * Note: This also tests policy includes as almost all oath policies contain policy includes.
     *
     */
    public void installPoliciesTest(String bundleId, final Map<String, String> oldGuidsToNewGuids) throws Exception {
        final Map<String, String> nameToPreviousGuid = new HashMap<String, String>();

        final BundleResolver bundleResolver = getBundleResolver();
        final BundleInstaller bundleInstaller = new BundleInstaller(bundleResolver){
            @NotNull
            @Override
            protected Pair<AssertionStatus, Document> callManagementAssertion(String requestXml) {
                try {
                    if (requestXml.contains("http://schemas.xmlsoap.org/ws/2004/09/transfer/Create")) {

                        // validate that the request contained policy XML which does not reference any old GUIDS
                        // e.g. policy includes have been udpated correctly.
//                        System.out.println(requestXml);
                        final Element policyResourceElmWritable = BundleInstaller.getPolicyResourceElement(XmlUtil.parse(requestXml).getDocumentElement(), "Policy", "canned id");
                        final Document includedPolicyDoc = BundleInstaller.getPolicyDocumentFromResource(policyResourceElmWritable, "Policy", "canned id");
                        final List<Element> policyIncludes = GatewayManagementDocumentUtilities.getPolicyIncludes(includedPolicyDoc);

                        //validate that these guids are not canned guids shipped with the OTK policies.
                        for (Element policyInclude : policyIncludes) {
                            final String guid = policyInclude.getAttribute("stringValue");
                            assertFalse(oldGuidsToNewGuids.containsKey(guid));
                            // guid should exist as a new value
                            assertTrue(oldGuidsToNewGuids.containsValue(guid));
                        }

                        return new Pair<AssertionStatus, Document>(AssertionStatus.NONE, XmlUtil.parse(alreadyExistsResponse));
                    } else {

                        // Create a new GUID for each policy. Return the same GUID for the same include

                        ElementCursor cursor = new DomElementCursor(XmlUtil.parse(requestXml));
                        System.out.println(requestXml);
                        final XpathResult xpathResult = cursor.getXpathResult(
                                new XpathExpression(
                                        "//wsman:Selector[@Name='name']",
                                        GatewayManagementDocumentUtilities.getNamespaceMap()).compile());
                        final String name = DomUtils.getTextValue(xpathResult.getNodeSet().getIterator().nextElementAsCursor().asDomElement(), true);
                        final String guidToUse = nameToPreviousGuid.containsKey(name)
                                ? nameToPreviousGuid.get(name) : UUID.randomUUID().toString();

                        final String response = MessageFormat.format(CANNED_GET_POLICY_RESPONSE, guidToUse, name);
                        return new Pair<AssertionStatus, Document>(AssertionStatus.NONE, XmlUtil.parse(response));
                    }
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }
        };

        final OAuthInstallerAdminImpl oAuthInstallerAdmin = new OAuthInstallerAdminImpl();
        oAuthInstallerAdmin.setBundleInstaller(bundleInstaller);

        final Document policyFromBundleDoc = bundleResolver.getBundleItem(bundleId, "Policy.xml", false);

        ElementCursor cursor = new DomElementCursor(policyFromBundleDoc);
        final XpathResult xpathResult = cursor.getXpathResult(
                new XpathExpression(
                        "//l7:PolicyDetail",
                        GatewayManagementDocumentUtilities.getNamespaceMap()).compile());


        final XpathResultIterator iterator = xpathResult.getNodeSet().getIterator();
        if (!iterator.hasNext()) {
            fail("Incorrect test configuration");
        }

        // the mocked callManagementAssertion returns
        final Map<String, String> policyNameToGuid = new HashMap<String, String>();
        while (iterator.hasNext()) {
            final ElementCursor elementCursor = iterator.nextElementAsCursor();
            final Element policyDetailElm = elementCursor.asDomElement();
            final String guid = policyDetailElm.getAttribute("guid");
            final String name = DomUtils.getTextValue(
                    XmlUtil.findExactlyOneChildElementByName(policyDetailElm, BundleInstaller.L7_NS_GW_MGMT, "Name"), true);

            policyNameToGuid.put(name, guid);
        }

        bundleInstaller.installPolicies(getFolderIds(), oldGuidsToNewGuids, policyFromBundleDoc);

        // verify that each known policy name was installed
        for (Map.Entry<String, String> bundlePolicy : policyNameToGuid.entrySet()) {
            // confirm it was installed
            assertTrue("Policy " + bundlePolicy.getKey() + " and guid " + bundlePolicy.getValue() + " was not installed.",
                    oldGuidsToNewGuids.containsKey(bundlePolicy.getValue()));
        }

    }

    @Test
    public void testServiceXpathExpression() throws Exception {
        final BundleResolver resolver = getBundleResolver();
        // OAuth_1_0
        final Document oAuth_1_0 = resolver.getBundleItem("1c2a2874-df8d-4e1d-b8b0-099b576407e1", "Service.xml", false);
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
    public void testInstallServices() throws Exception {
        final BundleResolver bundleResolver = getBundleResolver();
        final BundleInstaller bundleInstaller = new BundleInstaller(bundleResolver){
            @NotNull
            @Override
            protected Pair<AssertionStatus, Document> callManagementAssertion(String requestXml) {
                return cannedIdResponse(requestXml);
            }
        };

        final OAuthInstallerAdminImpl oAuthInstallerAdmin = new OAuthInstallerAdminImpl();
        oAuthInstallerAdmin.setBundleInstaller(bundleInstaller);

        // OAuth_1_0
        final Document serviceFromBundleDoc = bundleResolver.getBundleItem("1c2a2874-df8d-4e1d-b8b0-099b576407e1", "Service.xml", false);
        bundleInstaller.installServices(getFolderIds(), new HashMap<Long, Long>(), getPolicyGuids(), serviceFromBundleDoc);

    }

    /**
     * The same policy is contained in more than one bundle. This test validates that all policies are logically
     * equivalent. If not then two policies / serivce could refer to the same policy with the same guid but recieve
     * a different policy.
     *
     */
    @Test
    @Ignore
    public void testValidateTheSamePoliciesAreIdentical() throws Exception {
        fail("Must implement to check if all bundles with the same policy guid are logically equilivant");
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

    // - PRIVATE

    @NotNull
    private BundleResolver getBundleResolver(){

        final Map<String, Map<String, Document>> bundleToItemAndDocMap = new HashMap<String, Map<String, Document>>();
        bundleToItemAndDocMap.put("1c2a2874-df8d-4e1d-b8b0-099b576407e1", getItemsToDocs("OAuth_1_0", true));
        bundleToItemAndDocMap.put("ba525763-6e55-4748-9376-76055247c8b1", getItemsToDocs("OAuth_2_0", true));
        bundleToItemAndDocMap.put("f69c7d15-4999-4761-ab26-d29d58c0dd57", getItemsToDocs("SecureZone_OVP", true));
        bundleToItemAndDocMap.put("b082274b-f00e-4fbf-bbb7-395a95ca2a35", getItemsToDocs("SecureZone_Storage", true));
        bundleToItemAndDocMap.put("a07924c0-0265-42ea-90f1-2428e31ae5ae", getItemsToDocs("StorageManager", true));

        return new BundleResolver() {
            @Override
            public Document getBundleItem(@NotNull String bundleId, @NotNull String bundleItem, boolean allowMissing) throws UnknownBundleException, BundleResolverException {
                final Map<String, Document> itemToDocMap = bundleToItemAndDocMap.get(bundleId);
                return itemToDocMap.get(bundleItem);
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

    private Map<String, Document> getItemsToDocs(String bundleFolderName, boolean hasPolicy) {
        final Map<String, Document> itemsToDocs = new HashMap<String, Document>();
        final String baseName = "/com/l7tech/external/assertions/oauthinstaller/bundles/";
        itemsToDocs.put("Folder.xml", getDocumentFromResource(baseName + bundleFolderName + "/Folder.xml"));
        itemsToDocs.put("Service.xml", getDocumentFromResource(baseName + bundleFolderName + "/Service.xml"));

        if (hasPolicy) {
            itemsToDocs.put("Policy.xml", getDocumentFromResource(baseName + bundleFolderName + "/Policy.xml"));
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

    final String createdResponse = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
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
            "                    <wsman:Selector Name=\"id\">134807552</wsman:Selector>\n" +
            "                </wsman:SelectorSet>\n" +
            "            </wsa:ReferenceParameters>\n" +
            "        </wxf:ResourceCreated>\n" +
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

    private final static String errorResponse = "<env:Envelope xmlns:env=\"http://www.w3.org/2003/05/soap-envelope\"\n" +
            "    xmlns:mdo=\"http://schemas.wiseman.dev.java.net/metadata/messagetypes\"\n" +
            "    xmlns:mex=\"http://schemas.xmlsoap.org/ws/2004/09/mex\"\n" +
            "    xmlns:wsa=\"http://schemas.xmlsoap.org/ws/2004/08/addressing\"\n" +
            "    xmlns:wse=\"http://schemas.xmlsoap.org/ws/2004/08/eventing\"\n" +
            "    xmlns:wsen=\"http://schemas.xmlsoap.org/ws/2004/09/enumeration\"\n" +
            "    xmlns:wsman=\"http://schemas.dmtf.org/wbem/wsman/1/wsman.xsd\"\n" +
            "    xmlns:wsmeta=\"http://schemas.dmtf.org/wbem/wsman/1/wsman/version1.0.0.a/default-addressing-model.xsd\"\n" +
            "    xmlns:wxf=\"http://schemas.xmlsoap.org/ws/2004/09/transfer\" xmlns:xs=\"http://www.w3.org/2001/XMLSchema\">\n" +
            "    <env:Header>\n" +
            "        <wsa:Action env:mustUnderstand=\"true\" xmlns:l7=\"http://ns.l7tech.com/2010/04/gateway-management\">http://schemas.xmlsoap.org/ws/2004/09/transfer/fault</wsa:Action>\n" +
            "        <wsa:MessageID env:mustUnderstand=\"true\" xmlns:l7=\"http://ns.l7tech.com/2010/04/gateway-management\">uuid:e17c4e90-8693-4a9f-ac8d-c00a25d06e0f</wsa:MessageID>\n" +
            "        <wsa:RelatesTo xmlns:l7=\"http://ns.l7tech.com/2010/04/gateway-management\">uuid:7f1dd1be-120e-435c-9cff-c7e2d2f93e00</wsa:RelatesTo>\n" +
            "        <wsa:To env:mustUnderstand=\"true\" xmlns:l7=\"http://ns.l7tech.com/2010/04/gateway-management\">http://schemas.xmlsoap.org/ws/2004/08/addressing/role/anonymous</wsa:To>\n" +
            "    </env:Header>\n" +
            "    <env:Body>\n" +
            "        <env:Fault xmlns:l7=\"http://ns.l7tech.com/2010/04/gateway-management\">\n" +
            "            <env:Code>\n" +
            "                <env:Value>env:Sender</env:Value>\n" +
            "                <env:Subcode>\n" +
            "                    <env:Value>wxf:InvalidRepresentation</env:Value>\n" +
            "                </env:Subcode>\n" +
            "            </env:Code>\n" +
            "            <env:Reason>\n" +
            "                <env:Text xml:lang=\"en-US\">The XML content was invalid.</env:Text>\n" +
            "            </env:Reason>\n" +
            "            <env:Detail>\n" +
            "                <env:Text xml:lang=\"en-US\">Resource validation failed due to 'INVALID_VALUES' invalid parent folder</env:Text>\n" +
            "                <wsman:FaultDetail>http://schemas.dmtf.org/wbem/wsman/1/wsman/faultDetail/InvalidValues</wsman:FaultDetail>\n" +
            "            </env:Detail>\n" +
            "        </env:Fault>\n" +
            "    </env:Body>\n" +
            "</env:Envelope>\n";

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
            "                    &lt;L7p:VariableToSet stringValue=\"oauth_ovp_server\"/&gt;\n" +
            "                    &lt;/L7p:SetVariable&gt;\n" +
            "                    &lt;L7p:SetVariable&gt;\n" +
            "                    &lt;L7p:Base64Expression\n" +
            "                    stringValue=\"aHR0cHM6Ly9sb2NhbGhvc3Q6ODQ0My9hdXRoL29hdXRoL3YxL2F1dGhvcml6ZS93ZWJzaXRl\"/&gt;\n" +
            "                    &lt;L7p:VariableToSet stringValue=\"oauth_v1_server_website\"/&gt;\n" +
            "                    &lt;/L7p:SetVariable&gt;\n" +
            "                    &lt;L7p:SetVariable&gt;\n" +
            "                    &lt;L7p:Base64Expression stringValue=\"aHR0cHM6Ly9sb2NhbGhvc3Q6ODQ0Mw==\"/&gt;\n" +
            "                    &lt;L7p:VariableToSet stringValue=\"oauth_v1_server_website_baseuri\"/&gt;\n" +
            "                    &lt;/L7p:SetVariable&gt;\n" +
            "                    &lt;L7p:ExportVariables&gt;\n" +
            "                    &lt;L7p:ExportedVars stringArrayValue=\"included\"&gt;\n" +
            "                    &lt;L7p:item stringValue=\"oauth_ovp_server\"/&gt;\n" +
            "                    &lt;L7p:item stringValue=\"oauth_v1_server_website\"/&gt;\n" +
            "                    &lt;L7p:item stringValue=\"oauth_v1_server_website_baseuri\"/&gt;\n" +
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
}
