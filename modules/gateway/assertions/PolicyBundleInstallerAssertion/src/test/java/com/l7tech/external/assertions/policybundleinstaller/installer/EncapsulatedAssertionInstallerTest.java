package com.l7tech.external.assertions.policybundleinstaller.installer;

import com.l7tech.common.io.XmlUtil;
import com.l7tech.external.assertions.policybundleinstaller.GatewayManagementInvoker;
import com.l7tech.external.assertions.policybundleinstaller.PolicyBundleInstaller;
import com.l7tech.external.assertions.policybundleinstaller.PolicyBundleInstallerTestBase;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.PolicyAssertionException;
import com.l7tech.policy.bundle.BundleInfo;
import com.l7tech.policy.bundle.BundleMapping;
import com.l7tech.server.event.wsman.InstallPolicyBundleEvent;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.server.policy.bundle.*;
import com.l7tech.util.DomUtils;
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
import java.util.*;

import static com.l7tech.external.assertions.policybundleinstaller.installer.EncapsulatedAssertionInstaller.getPrefixedGuid;
import static com.l7tech.external.assertions.policybundleinstaller.installer.EncapsulatedAssertionInstaller.getPrefixedName;
import static com.l7tech.server.policy.bundle.BundleResolver.BundleItem.SERVICE;
import static com.l7tech.server.policy.bundle.GatewayManagementDocumentUtilities.*;
import static org.junit.Assert.assertEquals;

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
        }, context, serviceManager, getCancelledCallback(installEvent));

        bundleInstaller.getEncapsulatedAssertionInstaller().install( getPolicyGuids());
    }

    @Test
    // based on ServiceInstallerTest.testInstallServices()
    public void testPrerequisiteFolderInstall() throws Exception {
        final BundleResolver bundleResolver = getBundleResolver(SIMPLE_TEST_BUNDLE_BASE_NAME);
        final PolicyBundleInstallerContext context = new PolicyBundleInstallerContext(getBundleInfo(SIMPLE_TEST_BUNDLE_BASE_NAME), new BundleMapping(), null, bundleResolver, true);
        final InstallPolicyBundleEvent installEvent = new InstallPolicyBundleEvent(this, context, null);
        final Map<String, String> idToName = new HashMap<>();
        final Map<String, String> idToGuid = new HashMap<>();
        final PolicyBundleInstaller bundleInstaller = new PolicyBundleInstaller(stubGatewayManagementInvoker(idToName, idToGuid), context, serviceManager, getCancelledCallback(installEvent));

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
        final String prefix = "version1a";
        final BundleResolver bundleResolver = getBundleResolver(SIMPLE_TEST_BUNDLE_BASE_NAME);
        final PolicyBundleInstallerContext context = new PolicyBundleInstallerContext(getBundleInfo(SIMPLE_TEST_BUNDLE_BASE_NAME), new BundleMapping(), prefix, bundleResolver, true);
        final InstallPolicyBundleEvent installEvent = new InstallPolicyBundleEvent(this, context, null);
        final Map<String, String> idToName = new HashMap<>();
        final Map<String, String> idToGuid = new HashMap<>();
        final PolicyBundleInstaller bundleInstaller = new PolicyBundleInstaller(stubGatewayManagementInvoker(idToName, idToGuid), context, serviceManager, getCancelledCallback(installEvent));

        bundleInstaller.getEncapsulatedAssertionInstaller().install(getPolicyGuids());

        // validate all Encapsulated Assertions were found and all name and GUID were prefixed correctly
        assertEquals("Incorrect number of Encapsulated Assertion created", 1, idToName.size());
        assertEquals("Incorrect number of Encapsulated Assertion created", 1, idToGuid.size());

        // hardcoded test resources - validate each Encapsulated Assertion found and correct name and GUID was published
        assertEquals(getPrefixedName(prefix, SIMPLE_TEST_BUNDLE_COMP_ENCASS_NAME), idToName.get(SIMPLE_TEST_BUNDLE_COMP_ENCASS_ID));
        assertEquals(getPrefixedGuid(prefix, SIMPLE_TEST_BUNDLE_COMP_ENCASS_GUID), idToGuid.get(SIMPLE_TEST_BUNDLE_COMP_ENCASS_ID));
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
                        // validate the prefix supplied
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
        final String prefix = "v8.1.0";
        final PolicyBundleInstallerContext context = new PolicyBundleInstallerContext(bundleInfo, new BundleMapping(), prefix, bundleResolver, true);
        final Document serviceBundle = context.getBundleResolver().getBundleItem(context.getBundleInfo().getId(), SERVICE, true);

        // get the first service which should contain a resource-set that references an encapsulated assertion
        assert serviceBundle != null;
        final List<Element> serviceElms = GatewayManagementDocumentUtilities.getEntityElements(serviceBundle.getDocumentElement(), "Service");
        final Element policyResourceElmWritable = getPolicyResourceElement(serviceElms.get(0), "Service", ServiceInstallerTest.SIMPLE_TEST_BUNDLE_SERVICE_ID);
        assert policyResourceElmWritable != null;
        final Document policyDocumentFromResource = getPolicyDocumentFromResource(policyResourceElmWritable, "Service", ServiceInstallerTest.SIMPLE_TEST_BUNDLE_SERVICE_ID);

        // update to references
        EncapsulatedAssertionInstaller.updatePolicyDoc(policyResourceElmWritable, policyDocumentFromResource, prefix);

        // check its been updated as expected
        List<Element> encapsulatedAssertions = XpathUtil.findElements(policyDocumentFromResource.getDocumentElement(), "//L7p:Encapsulated/L7p:EncapsulatedAssertionConfigGuid", getNamespaceMap());
        assertEquals(getPrefixedGuid(prefix, SIMPLE_TEST_BUNDLE_ENCASS_GUID), encapsulatedAssertions.get(0).getAttribute("stringValue"));
        encapsulatedAssertions = XpathUtil.findElements(policyDocumentFromResource.getDocumentElement(), "//L7p:Encapsulated/L7p:EncapsulatedAssertionConfigName", getNamespaceMap());
        assertEquals(getPrefixedName(prefix, SIMPLE_TEST_BUNDLE_ENCASS_NAME), encapsulatedAssertions.get(0).getAttribute("stringValue"));
    }

    @Test
    public void testPrefixedName() {
        final String name = "Simple Encapsulated Assertion";
        assertEquals(name, getPrefixedName(null, name));
        assertEquals(name, getPrefixedName("", name));
        assertEquals("Prefixed Simple Encapsulated Assertion", getPrefixedName("Prefixed", name));
    }

    @Test
    public void testPrefixedGuid() {
        final String guid = "abc589b0-eba5-4b3f-81b5-be7809817623";
        assertEquals(guid, getPrefixedGuid(null, guid));
        assertEquals(guid, getPrefixedGuid("", guid));
        assertEquals("123abc589b0-eba5-4b3f-81b5-be7809817", getPrefixedGuid("123", guid));
        assertEquals("12345678901234567890123456789012345a", getPrefixedGuid("12345678901234567890123456789012345", guid));
        assertEquals("123456789012345678901234567890123456", getPrefixedGuid("123456789012345678901234567890123456", guid));
        assertEquals("123456789012345678901234567890123456", getPrefixedGuid("1234567890123456789012345678901234567", guid));
        assertEquals("123456789012345678901234567890123456", getPrefixedGuid("12345678901234567890123456789012345678901234567890", guid));
    }
}
