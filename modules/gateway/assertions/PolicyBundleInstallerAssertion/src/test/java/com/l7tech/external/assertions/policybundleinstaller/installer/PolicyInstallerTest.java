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
import com.l7tech.server.policy.bundle.BundleResolver;
import com.l7tech.server.policy.bundle.BundleUtils;
import com.l7tech.server.policy.bundle.GatewayManagementDocumentUtilities;
import com.l7tech.server.policy.bundle.PolicyBundleInstallerContext;
import com.l7tech.util.DomUtils;
import com.l7tech.util.Pair;
import com.l7tech.xml.DomElementCursor;
import com.l7tech.xml.ElementCursor;
import com.l7tech.xml.xpath.XpathExpression;
import com.l7tech.xml.xpath.XpathResult;
import com.l7tech.xml.xpath.XpathResultIterator;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.junit.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import java.io.IOException;
import java.text.MessageFormat;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static com.l7tech.server.policy.bundle.BundleResolver.BundleItem.POLICY;
import static com.l7tech.server.policy.bundle.GatewayManagementDocumentUtilities.MGMT_VERSION_NAMESPACE;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class PolicyInstallerTest extends PolicyBundleInstallerTestBase {
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

    protected static final String CANNED_SET_VERSION_COMMENT_RESPONSE = "<env:Envelope xmlns:env=\"http://www.w3.org/2003/05/soap-envelope\" xmlns:l7=\"http://ns.l7tech.com/2010/04/gateway-management\" xmlns:mdo=\"http://schemas.wiseman.dev.java.net/metadata/messagetypes\" xmlns:mex=\"http://schemas.xmlsoap.org/ws/2004/09/mex\" xmlns:wsa=\"http://schemas.xmlsoap.org/ws/2004/08/addressing\" xmlns:wse=\"http://schemas.xmlsoap.org/ws/2004/08/eventing\" xmlns:wsen=\"http://schemas.xmlsoap.org/ws/2004/09/enumeration\" xmlns:wsman=\"http://schemas.dmtf.org/wbem/wsman/1/wsman.xsd\" xmlns:wsmeta=\"http://schemas.dmtf.org/wbem/wsman/1/wsman/version1.0.0.a/default-addressing-model.xsd\" xmlns:wxf=\"http://schemas.xmlsoap.org/ws/2004/09/transfer\" xmlns:xs=\"http://www.w3.org/2001/XMLSchema\">\n" +
            "   <env:Header>\n" +
            "      <wsa:Action env:mustUnderstand=\"true\">http://ns.l7tech.com/2010/04/gateway-management/services/SetVersionCommentResponse</wsa:Action>\n" +
            "      <wsa:MessageID env:mustUnderstand=\"true\">uuid:bf1dbf0e-b644-4240-8cca-4a1d149665b4</wsa:MessageID>\n" +
            "      <wsa:RelatesTo>uuid:a711f948-7d39-1d39-8002-481688002100</wsa:RelatesTo>\n" +
            "      <wsa:To env:mustUnderstand=\"true\">http://schemas.xmlsoap.org/ws/2004/08/addressing/role/anonymous</wsa:To>\n" +
            "   </env:Header>\n" +
            "   <env:Body/>\n" +
            "</env:Envelope>";

    private static final String POLICIES_SET_VERSION_COMMENT_ACTION = "http://ns.l7tech.com/2010/04/gateway-management/policies/SetVersionComment";

    @Test
    public void testAllPolicyBundlesInstall() throws Exception {
        final List<Pair<BundleInfo, String>> bundleInfos = BundleUtils.getBundleInfos(getClass(), TEST_BUNDLE_BASE_NAME);
        for (Pair<BundleInfo, String> bundleInfo : bundleInfos) {
            if (OAUTH_TEST_BUNDLE_BASE_NAME.equals(bundleInfo.right)) {
                installPoliciesTest(bundleInfo.left, null);
            }
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
    private void installPoliciesTest(final @NotNull BundleInfo bundleInfo,
                                    final @Nullable String installationPrefix) throws Exception {
        final Map<String, String> nameToPreviousGuid = new HashMap<>();

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
                            assert policyEntityEl != null;
                            final Element policyDetailElm = GatewayManagementDocumentUtilities.getPolicyDetailElement(policyEntityEl);
                            final String policyName = GatewayManagementDocumentUtilities.getEntityName(policyDetailElm);
                            System.out.println("Policy name: " + policyName);
                            assertTrue("Policy name was not prefixed", policyName.startsWith(installationPrefix));
                        }

                        final Pair<AssertionStatus, Document> documentPair = cannedIdResponse(enumPolicyDocument);
                        setResponse(context, documentPair.right);
                        return AssertionStatus.NONE;
                    } else if (requestXml.contains(POLICIES_SET_VERSION_COMMENT_ACTION)) {
                        setResponse(context, CANNED_SET_VERSION_COMMENT_RESPONSE);
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
        final Map<String, String> policyNameToGuid = new HashMap<>();
        while (iterator.hasNext()) {
            final ElementCursor elementCursor = iterator.nextElementAsCursor();
            final Element policyDetailElm = elementCursor.asDomElement();
            final String guid = policyDetailElm.getAttribute("guid");
            final String name = DomUtils.getTextValue(
                    XmlUtil.findExactlyOneChildElementByName(policyDetailElm, BundleUtils.L7_NS_GW_MGMT, "Name"), true);

            policyNameToGuid.put(name, guid);
        }

        final Map<String, String> oldToNewPolicyIds = new HashMap<>();
        final Map<String, String> oldGuidToNewGuidMap = bundleInstaller.getPolicyInstaller().install(getFolderIds(), oldToNewPolicyIds);

        // verify that each known policy name was installed
        for (Map.Entry<String, String> bundlePolicy : policyNameToGuid.entrySet()) {
            // confirm it was installed
            assertTrue("Policy " + bundlePolicy.getKey() + " and guid " + bundlePolicy.getValue() + " was not installed.",
                    oldGuidToNewGuidMap.containsKey(bundlePolicy.getValue()));
        }

    }

    /**
     * That that an installation prefix is appended to each saved policy.
     * @throws Exception
     */
    @Test
    public void testPolicyNamePrefixedInstallation() throws Exception {
        final List<Pair<BundleInfo, String>> bundleInfos = BundleUtils.getBundleInfos(getClass(), TEST_BUNDLE_BASE_NAME);
        for (Pair<BundleInfo, String> bundleInfo : bundleInfos) {
            if (OAUTH_TEST_BUNDLE_BASE_NAME.equals(bundleInfo.right)) {
                installPoliciesTest(bundleInfo.left, "Version 1 - ");
            }
        }

    }
}
