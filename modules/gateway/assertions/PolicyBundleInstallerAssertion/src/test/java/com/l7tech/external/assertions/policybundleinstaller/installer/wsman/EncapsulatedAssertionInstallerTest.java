package com.l7tech.external.assertions.policybundleinstaller.installer.wsman;

import com.l7tech.common.io.XmlUtil;
import com.l7tech.external.assertions.policybundleinstaller.PolicyBundleInstaller;
import com.l7tech.external.assertions.policybundleinstaller.PolicyBundleInstallerTestBase;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.PolicyAssertionException;
import com.l7tech.policy.bundle.BundleInfo;
import com.l7tech.policy.bundle.BundleMapping;
import com.l7tech.server.event.bundle.InstallPolicyBundleEvent;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.server.policy.bundle.BundleResolver;
import com.l7tech.server.policy.bundle.GatewayManagementDocumentUtilities;
import com.l7tech.server.policy.bundle.PolicyBundleInstallerContext;
import com.l7tech.server.policy.bundle.ssgman.GatewayManagementInvoker;
import com.l7tech.util.DomUtils;
import com.l7tech.util.HexUtils;
import com.l7tech.util.Pair;
import com.l7tech.xml.DomElementCursor;
import com.l7tech.xml.ElementCursor;
import com.l7tech.xml.InvalidXpathException;
import com.l7tech.xml.xpath.XpathExpression;
import com.l7tech.xml.xpath.XpathResult;
import com.l7tech.xml.xpath.XpathUtil;
import org.junit.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;

import javax.xml.xpath.XPathExpressionException;
import java.io.IOException;
import java.security.MessageDigest;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static com.l7tech.external.assertions.policybundleinstaller.installer.BaseInstaller.getPrefixedEncapsulatedAssertionName;
import static com.l7tech.external.assertions.policybundleinstaller.installer.wsman.EncapsulatedAssertionInstaller.getVersionModifiedEncapsulatedAssertionGuid;
import static com.l7tech.policy.bundle.BundleMapping.Type.ENCAPSULATE_ASSERTION_GUID;
import static com.l7tech.server.policy.bundle.BundleResolver.BundleItem.SERVICE;
import static com.l7tech.server.policy.bundle.GatewayManagementDocumentUtilities.*;
import static com.l7tech.util.Charsets.UTF8;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class EncapsulatedAssertionInstallerTest extends PolicyBundleInstallerTestBase {
    private static final String SIMPLE_TEST_BUNDLE_ENCASS_NAME = "Simple Encapsulated Assertion";
    private static final String SIMPLE_TEST_BUNDLE_ENCASS_ID = "7a5f0678f60be245a7ad76a684c63e2e";
    private static final String SIMPLE_TEST_BUNDLE_ENCASS_GUID = "506589b0-eba5-4b3f-81b5-be7809817623";
    private static final String SIMPLE_TEST_BUNDLE_COMP_ENCASS_NAME = "Simple Composite Encapsulated Assertion";
    private static final String SIMPLE_TEST_BUNDLE_COMP_ENCASS_ID = "271ebd21129d4c0d46b848e0fc4dbc5d";
    private static final String SIMPLE_TEST_BUNDLE_COMP_ENCASS_GUID = "75062052-9f23-4be2-b7fc-c3caad51620d";

    @Test
    // based on ServiceInstallerTest.testInstallServices()
    public void testInstall() throws Exception {
        final BundleResolver bundleResolver = getBundleResolver(SIMPLE_TEST_BUNDLE_BASE_NAME);
        final PolicyBundleInstallerContext context = new PolicyBundleInstallerContext(getBundleInfo(SIMPLE_TEST_BUNDLE_BASE_NAME), new BundleMapping(), null, bundleResolver, true);
        final InstallPolicyBundleEvent installEvent = new InstallPolicyBundleEvent(this, context);

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
        }, doNothingInvoker(), context, serviceManager, getCancelledCallback(installEvent));

        bundleInstaller.getEncapsulatedAssertionInstaller().install( getPolicyGuids());
    }

    @Test
    // based on ServiceInstallerTest.testInstallServices()
    public void testPrerequisiteFolderInstall() throws Exception {
        final BundleResolver bundleResolver = getBundleResolver(SIMPLE_TEST_BUNDLE_BASE_NAME);
        final PolicyBundleInstallerContext context = new PolicyBundleInstallerContext(getBundleInfo(SIMPLE_TEST_BUNDLE_BASE_NAME), new BundleMapping(), null, bundleResolver, true);
        final InstallPolicyBundleEvent installEvent = new InstallPolicyBundleEvent(this, context);
        final Map<String, String> idToName = new HashMap<>();
        final Map<String, String> idToGuid = new HashMap<>();
        final PolicyBundleInstaller bundleInstaller = new PolicyBundleInstaller(stubGatewayManagementInvoker(idToName, idToGuid), doNothingInvoker(), context, serviceManager, getCancelledCallback(installEvent));

        // install from prerequisite folders
        for (String prerequisiteFolder : context.getBundleInfo().getPrerequisiteFolders()) {
            bundleInstaller.getEncapsulatedAssertionInstaller().install(prerequisiteFolder, getPolicyGuids());
        }

        bundleInstaller.getEncapsulatedAssertionInstaller().install(getPolicyGuids());

        // validate all Encapsulated Assertions were found and all name and GUID were prefixed correctly
        assertEquals("Incorrect number of Encapsulated Assertion created", 2, idToName.size());
        assertEquals("Incorrect number of Encapsulated Assertion created", 2, idToGuid.size());

        // hardcoded test resources - validate each Encapsulated Assertion found and correct name and GUID was published
        assertEquals(SIMPLE_TEST_BUNDLE_ENCASS_NAME, idToName.get(SIMPLE_TEST_BUNDLE_ENCASS_ID));
        assertEquals(SIMPLE_TEST_BUNDLE_ENCASS_GUID, idToGuid.get(SIMPLE_TEST_BUNDLE_ENCASS_ID));
        assertEquals(SIMPLE_TEST_BUNDLE_COMP_ENCASS_NAME, idToName.get(SIMPLE_TEST_BUNDLE_COMP_ENCASS_ID));
        assertEquals(SIMPLE_TEST_BUNDLE_COMP_ENCASS_GUID, idToGuid.get(SIMPLE_TEST_BUNDLE_COMP_ENCASS_ID));
    }

    @Test
    // based on ServiceInstallerTest.testServicesUriPrefixedInstallation()
    public void testPrefixedInstallation() throws Exception {
        final String versionModifier = "version1a";
        final BundleResolver bundleResolver = getBundleResolver(SIMPLE_TEST_BUNDLE_BASE_NAME);
        final PolicyBundleInstallerContext context = new PolicyBundleInstallerContext(getBundleInfo(SIMPLE_TEST_BUNDLE_BASE_NAME), new BundleMapping(), versionModifier, bundleResolver, true);
        final InstallPolicyBundleEvent installEvent = new InstallPolicyBundleEvent(this, context);
        final Map<String, String> idToName = new HashMap<>();
        final Map<String, String> idToGuid = new HashMap<>();
        final PolicyBundleInstaller bundleInstaller = new PolicyBundleInstaller(stubGatewayManagementInvoker(idToName, idToGuid), doNothingInvoker(), context, serviceManager, getCancelledCallback(installEvent));

        bundleInstaller.getEncapsulatedAssertionInstaller().install(getPolicyGuids());

        // validate all Encapsulated Assertions were found and all name and GUID were prefixed correctly
        assertEquals("Incorrect number of Encapsulated Assertion created", 1, idToName.size());
        assertEquals("Incorrect number of Encapsulated Assertion created", 1, idToGuid.size());

        // hardcoded test resources - validate each Encapsulated Assertion found and correct name and GUID was published
        assertEquals(getPrefixedEncapsulatedAssertionName(versionModifier, SIMPLE_TEST_BUNDLE_COMP_ENCASS_NAME), idToName.get(SIMPLE_TEST_BUNDLE_COMP_ENCASS_ID));
        assertEquals(getVersionModifiedEncapsulatedAssertionGuid(versionModifier, SIMPLE_TEST_BUNDLE_COMP_ENCASS_GUID), idToGuid.get(SIMPLE_TEST_BUNDLE_COMP_ENCASS_ID));
    }

    private GatewayManagementInvoker stubGatewayManagementInvoker(final Map<String, String> idToName, final Map<String, String> idToGuid) {
        return new GatewayManagementInvoker() {
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
                        // validate the version modifier supplied
                        try {
                            XpathResult xpathResult = cursor.getXpathResult(new XpathExpression(".//l7:Name", GatewayManagementDocumentUtilities.getNamespaceMap()).compile());
                            Element element = xpathResult.getNodeSet().getIterator().nextElementAsCursor().asDomElement();
                            final String name = DomUtils.getTextValue(element);

                            xpathResult = cursor.getXpathResult(new XpathExpression(".//l7:Guid", GatewayManagementDocumentUtilities.getNamespaceMap()).compile());
                            element = xpathResult.getNodeSet().getIterator().nextElementAsCursor().asDomElement();
                            final String guid = DomUtils.getTextValue(element);

                            // get id
                            cursor.moveToDocumentElement();
                            xpathResult = cursor.getXpathResult(new XpathExpression(".//l7:EncapsulatedAssertion", GatewayManagementDocumentUtilities.getNamespaceMap()).compile());
                            final Element serviceElm = xpathResult.getNodeSet().getIterator().nextElementAsCursor().asDomElement();
                            final String id = serviceElm.getAttribute("id");
                            if (id.trim().isEmpty()) {
                                throw new RuntimeException("Encapsulated Assertion id not found");
                            }

                            idToName.put(id, name);
                            idToGuid.put(id, guid);
                            // System.out.println("Found name: " + name + ", GUID: " + guid);
                        } catch (XPathExpressionException | InvalidXpathException e) {
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
        };
    }

    @Test
    public void testUpdatePolicyDoc() throws Exception {
        // get the simple test service bundle (e.g. /simpletest/Service.xml)
        final BundleResolver bundleResolver = getBundleResolver(SIMPLE_TEST_BUNDLE_BASE_NAME);
        final BundleInfo bundleInfo = getBundleInfo(SIMPLE_TEST_BUNDLE_BASE_NAME);
        final String versionModifier = "v8.1.0";
        final BundleMapping bundleMapping = new BundleMapping();
        final PolicyBundleInstallerContext context = new PolicyBundleInstallerContext(bundleInfo, bundleMapping, versionModifier, bundleResolver, true);
        final Document serviceBundle = context.getBundleResolver().getBundleItem(context.getBundleInfo().getId(), SERVICE, true);

        // get the first service which should contain a resource-set that references an encapsulated assertion
        assert serviceBundle != null;
        final List<Element> serviceElms = GatewayManagementDocumentUtilities.getEntityElements(serviceBundle.getDocumentElement(), "Service");
        final Element policyResourceElmWritable = getPolicyResourceElement(serviceElms.get(0), "Service", ServiceInstallerTest.SIMPLE_TEST_BUNDLE_SERVICE_ID);
        assert policyResourceElmWritable != null;
        final Document policyDocumentFromResource = getPolicyDocumentFromResource(policyResourceElmWritable, "Service", ServiceInstallerTest.SIMPLE_TEST_BUNDLE_SERVICE_ID);

        // simulate how guid mappings are set before updatePolicyDoc(...)
        List<Element> encapsulatedAssertions = XpathUtil.findElements(policyDocumentFromResource.getDocumentElement(), "//L7p:Encapsulated/L7p:EncapsulatedAssertionConfigGuid", getNamespaceMap());
        for (Element encapsulatedAssertion : encapsulatedAssertions) {
            // map old guid to newly generated guid
            bundleMapping.addMapping(BundleMapping.Type.ENCAPSULATE_ASSERTION_GUID, encapsulatedAssertion.getAttribute("stringValue"), UUID.randomUUID().toString());
        }

        // update to references
        EncapsulatedAssertionInstaller.updatePolicyDoc(policyResourceElmWritable, policyDocumentFromResource, versionModifier);

        // check guid is not one of the old exiting mapping
        encapsulatedAssertions = XpathUtil.findElements(policyDocumentFromResource.getDocumentElement(), "//L7p:Encapsulated/L7p:EncapsulatedAssertionConfigGuid", getNamespaceMap());
        assertNull(bundleMapping.getMapping(ENCAPSULATE_ASSERTION_GUID, encapsulatedAssertions.get(0).getAttribute("stringValue")));

        // check name has been updated as expected
        encapsulatedAssertions = XpathUtil.findElements(policyDocumentFromResource.getDocumentElement(), "//L7p:Encapsulated/L7p:EncapsulatedAssertionConfigName", getNamespaceMap());
        assertEquals(getPrefixedEncapsulatedAssertionName(versionModifier, SIMPLE_TEST_BUNDLE_ENCASS_NAME), encapsulatedAssertions.get(0).getAttribute("stringValue"));
    }

    @Test
    public void testPrefixedName() {
        final String name = "Simple Encapsulated Assertion";
        assertEquals(name, getPrefixedEncapsulatedAssertionName(null, name));
        assertEquals(name, getPrefixedEncapsulatedAssertionName("", name));
        assertEquals("Prefixed Simple Encapsulated Assertion", getPrefixedEncapsulatedAssertionName("Prefixed", name));
    }

    @Test
    public void testVersionModifiedGuid() throws Exception {
        final String versionModifier = "v1";

        assertEquals(SIMPLE_TEST_BUNDLE_ENCASS_GUID, getVersionModifiedEncapsulatedAssertionGuid(null, SIMPLE_TEST_BUNDLE_ENCASS_GUID));
        assertEquals(SIMPLE_TEST_BUNDLE_ENCASS_GUID, getVersionModifiedEncapsulatedAssertionGuid("", SIMPLE_TEST_BUNDLE_ENCASS_GUID));

        // verify we can deterministically compute the a version modified guid: first 128 bits (16 bytes) of SHA-256( prefix + original_guid )
        final MessageDigest md = MessageDigest.getInstance("SHA-256");
        assertEquals(HexUtils.hexDump(md.digest((versionModifier + SIMPLE_TEST_BUNDLE_ENCASS_GUID).getBytes(UTF8)), 0, 16), getVersionModifiedEncapsulatedAssertionGuid(versionModifier, SIMPLE_TEST_BUNDLE_ENCASS_GUID));
    }
}
