package com.l7tech.external.assertions.policybundleinstaller;

import com.l7tech.common.io.XmlUtil;
import com.l7tech.external.assertions.policybundleinstaller.installer.JdbcConnectionInstaller;
import com.l7tech.external.assertions.policybundleinstaller.installer.PolicyInstaller;
import com.l7tech.gateway.common.service.PublishedService;
import com.l7tech.objectmodel.Goid;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.PolicyAssertionException;
import com.l7tech.policy.bundle.BundleInfo;
import com.l7tech.server.event.wsman.DryRunInstallPolicyBundleEvent;
import com.l7tech.server.event.wsman.InstallPolicyBundleEvent;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.server.policy.bundle.BundleResolver;
import com.l7tech.server.policy.bundle.GatewayManagementDocumentUtilities;
import com.l7tech.server.policy.bundle.PolicyBundleInstallerContext;
import com.l7tech.test.BugNumber;
import com.l7tech.xml.DomElementCursor;
import com.l7tech.xml.ElementCursor;
import com.l7tech.xml.xpath.XpathExpression;
import com.l7tech.xml.xpath.XpathResult;
import com.l7tech.xml.xpath.XpathResultIterator;
import org.junit.Test;
import org.w3c.dom.Document;

import java.io.IOException;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import static com.l7tech.server.policy.bundle.BundleResolver.BundleItem.SERVICE;
import static org.junit.Assert.*;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.when;

public class PolicyBundleInstallerTest extends PolicyBundleInstallerTestBase {

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


    private final static String GET_SELECTOR_POLICY_RESPONSE = "<env:Envelope xmlns:env=\"http://www.w3.org/2003/05/soap-envelope\" xmlns:l7=\"http://ns.l7tech.com/2010/04/gateway-management\" xmlns:mdo=\"http://schemas.wiseman.dev.java.net/metadata/messagetypes\" xmlns:mex=\"http://schemas.xmlsoap.org/ws/2004/09/mex\" xmlns:wsa=\"http://schemas.xmlsoap.org/ws/2004/08/addressing\" xmlns:wse=\"http://schemas.xmlsoap.org/ws/2004/08/eventing\" xmlns:wsen=\"http://schemas.xmlsoap.org/ws/2004/09/enumeration\" xmlns:wsman=\"http://schemas.dmtf.org/wbem/wsman/1/wsman.xsd\" xmlns:wsmeta=\"http://schemas.dmtf.org/wbem/wsman/1/wsman/version1.0.0.a/default-addressing-model.xsd\" xmlns:wxf=\"http://schemas.xmlsoap.org/ws/2004/09/transfer\" xmlns:xs=\"http://www.w3.org/2001/XMLSchema\">\n"+
            "   <env:Header>\n"+
            "      <wsa:Action env:mustUnderstand=\"true\">http://schemas.xmlsoap.org/ws/2004/09/transfer/GetResponse</wsa:Action>\n"+
            "      <wsa:MessageID env:mustUnderstand=\"true\">uuid:c05a7ef0-b655-4451-bc0a-3e460671dd0d</wsa:MessageID>\n"+
            "      <wsa:RelatesTo>uuid:4ED2993C-4339-4E99-81FC-C2FD3812781A</wsa:RelatesTo>\n"+
            "      <wsa:To env:mustUnderstand=\"true\">http://schemas.xmlsoap.org/ws/2004/08/addressing/role/anonymous</wsa:To>\n"+
            "   </env:Header>\n"+
            "   <env:Body>\n"+
            "      <l7:Policy guid=\"fcd28e47-9fd9-47a7-9a32-0fdd7b2e4271\" id=\"a9bd6e276ab81f4f28693fd33245cc2c\" version=\"0\">\n"+
            "         <l7:PolicyDetail folderId=\"a9bd6e276ab81f4f28693fd33245ca6c\" guid=\"fcd28e47-9fd9-47a7-9a32-0fdd7b2e4271\" id=\"a9bd6e276ab81f4f28693fd33245cc2c\" version=\"0\">\n"+
            "            <l7:Name>OAuth 1.0 Context Variables</l7:Name>\n"+
            "            <l7:PolicyType>Include</l7:PolicyType>\n"+
            "            <l7:Properties>\n"+
            "               <l7:Property key=\"revision\">\n"+
            "                  <l7:LongValue>0</l7:LongValue>\n"+
            "               </l7:Property>\n"+
            "               <l7:Property key=\"soap\">\n"+
            "                  <l7:BooleanValue>false</l7:BooleanValue>\n"+
            "               </l7:Property>\n"+
            "            </l7:Properties>\n"+
            "         </l7:PolicyDetail>\n"+
            "         <l7:Resources>\n"+
            "            <l7:ResourceSet tag=\"policy\">\n"+
            "               <l7:Resource type=\"policy\"><![CDATA[<wsp:Policy xmlns:L7p=\"http://www.layer7tech.com/ws/policy\" xmlns:wsp=\"http://schemas.xmlsoap.org/ws/2002/12/policy\">\n"+
            "                    <wsp:All wsp:Usage=\"Required\">\n"+
            "                    <L7p:CommentAssertion><L7p:Comment stringValue=\"Component version 2.1.1 installed by OAuth installer version otk2.1.1\"/></L7p:CommentAssertion><L7p:SetVariable>\n"+
            "                    <L7p:Base64Expression stringValue=\"aHR0cHM6Ly9sb2NhbGhvc3Q6ODQ0Mw==\"/>\n"+
            "                    <L7p:VariableToSet stringValue=\"host_oauth_ovp_server\"/>\n"+
            "                    </L7p:SetVariable>\n"+
            "                    <L7p:SetVariable>\n"+
            "                    <L7p:Base64Expression stringValue=\"L2F1dGgvb2F1dGgvdjEvYXV0aG9yaXplL3dlYnNpdGU=\"/>\n"+
            "                    <L7p:VariableToSet stringValue=\"oauth_v1_website_path\"/>\n"+
            "                    </L7p:SetVariable>\n"+
            "                    <L7p:SetVariable>\n"+
            "                    <L7p:Base64Expression stringValue=\"aHR0cHM6Ly9sb2NhbGhvc3Q6ODQ0Mw==\"/>\n"+
            "                    <L7p:VariableToSet stringValue=\"host_oauth_v1_server_website_baseuri\"/>\n"+
            "                    </L7p:SetVariable>\n"+
            "                    <L7p:SetVariable>\n"+
            "                    <L7p:Base64Expression stringValue=\"MS4wIChSRkMgNTg0OSk=\"/>\n"+
            "                    <L7p:VariableToSet stringValue=\"oauth1_version\"/>\n"+
            "                    </L7p:SetVariable>\n"+
            "                    <L7p:ExportVariables>\n"+
            "                    <L7p:ExportedVars stringArrayValue=\"included\">\n"+
            "                    <L7p:item stringValue=\"host_oauth_ovp_server\"/>\n"+
            "                    <L7p:item stringValue=\"oauth_v1_website_path\"/>\n"+
            "                    <L7p:item stringValue=\"host_oauth_v1_server_website_baseuri\"/>\n"+
            "                    </L7p:ExportedVars>\n"+
            "                    </L7p:ExportVariables>\n"+
            "                    </wsp:All>\n"+
            "                    </wsp:Policy>]]></l7:Resource>\n"+
            "            </l7:ResourceSet>\n"+
            "         </l7:Resources>\n"+
            "      </l7:Policy>\n"+
            "   </env:Body>\n"+
            "</env:Envelope>";

    @Test
    public void testServiceXpathExpression() throws Exception {
        final BundleResolver resolver = getBundleResolver(OAUTH_TEST_BUNDLE_BASE_NAME);
        // OAuth_1_0
        final Document oAuth_1_0 = resolver.getBundleItem("4e321ca1-83a0-4df5-8216-c2d2bb36067d", SERVICE, false);
        ElementCursor cursor = new DomElementCursor(oAuth_1_0);

        final String urlMapping = "/auth/oauth/v1/token";
        String xpath = "//l7:Service/l7:ServiceDetail/l7:ServiceMappings/l7:HttpMapping/l7:UrlPattern[text()='"+urlMapping+"']";
        final XpathResult xpath1 = cursor.getXpathResult(new XpathExpression(xpath, GatewayManagementDocumentUtilities.getNamespaceMap()).compile());
        final XpathResultIterator iterator = xpath1.getNodeSet().getIterator();
        while (iterator.hasNext()) {
            XmlUtil.nodeToFormattedString(iterator.nextElementAsCursor().asDomElement());
            // System.out.println(XmlUtil.nodeToFormattedString(iterator.nextElementAsCursor().asDomElement()));
        }
    }

    @Test
    public void testDryInstallationWithConflicts() throws Exception {
        final BundleInfo bundleInfo = getBundleInfo(OAUTH_TEST_BUNDLE_BASE_NAME);
        bundleInfo.addJdbcReference("OAuth");

        final PolicyBundleInstallerContext context = new PolicyBundleInstallerContext(
                bundleInfo, null, null, getBundleResolver(OAUTH_TEST_BUNDLE_BASE_NAME), true);

        final DryRunInstallPolicyBundleEvent dryRunEvent = new DryRunInstallPolicyBundleEvent(this, context);

        // mock service found
        final Collection<PublishedService> aServiceFound = new ArrayList<>();
        aServiceFound.add(new PublishedService());
        when(serviceManager.findByRoutingUri(anyString())).thenReturn(aServiceFound);

        PolicyBundleInstaller installer = new PolicyBundleInstaller(new GatewayManagementInvoker() {
            @Override
            public AssertionStatus checkRequest(PolicyEnforcementContext context) throws PolicyAssertionException, IOException {

                // For policies and services, return that they already exist. For JDBC conns return that they do not exist.
                try {
                    final Document documentReadOnly = context.getRequest().getXmlKnob().getDocumentReadOnly();
                    final String requestXml = XmlUtil.nodeToFormattedString(documentReadOnly);

                    if (requestXml.contains("http://schemas.xmlsoap.org/ws/2004/09/enumeration/Enumerate")) {
                        if (requestXml.contains(JdbcConnectionInstaller.JDBC_MGMT_NS)) {
                            // no results
                            setResponse(context, XmlUtil.parse(FILTER_NO_RESULTS));
                        } else if (requestXml.contains("/l7:Service/l7:ServiceDetail[@id='"+new Goid(0,123345678)+"']/l7:ServiceMappings/l7:HttpMapping/l7:UrlPattern")) {
                            // no results
                            setResponse(context, XmlUtil.parse(FILTER_NO_RESULTS));
                        } else {
                            // results
                            setResponse(context, XmlUtil.parse(MessageFormat.format(CANNED_ENUMERATE_WITH_FILTER_AND_EPR_RESPONSE, new Goid(0, 123345678))));
                        }
                    } else if (requestXml.contains("http://schemas.xmlsoap.org/ws/2004/09/transfer/Get")) {
                        if (requestXml.contains(PolicyInstaller.POLICIES_MGMT_NS)) {
                            setResponse(context, XmlUtil.parse(GET_SELECTOR_POLICY_RESPONSE));
                        } else {
                            setResponse(context, XmlUtil.parse(FAULT_RESPONSE_INVALID_SELECTOR));
                        }
                    }
                } catch (Exception e) {
                    fail("Unexpected exception: " + e.getMessage());
                }

                return AssertionStatus.NONE;
            }
        }, context, serviceManager, getCancelledCallback(dryRunEvent));

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
    public void testDryInstallationWithNoConflicts() throws Exception {
        final BundleInfo bundleInfo = getBundleInfo(OAUTH_TEST_BUNDLE_BASE_NAME);
        bundleInfo.addJdbcReference("OAuth");

        final PolicyBundleInstallerContext context = new PolicyBundleInstallerContext(
                bundleInfo, null, null, getBundleResolver(OAUTH_TEST_BUNDLE_BASE_NAME), true);

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
                        if (requestXml.contains(JdbcConnectionInstaller.JDBC_MGMT_NS)) {
                            // results
                            setResponse(context, XmlUtil.parse(MessageFormat.format(CANNED_ENUMERATE_WITH_FILTER_AND_EPR_RESPONSE, new Goid(0, 123345678))));
                        } else {
                            // no results
                            setResponse(context, XmlUtil.parse(FILTER_NO_RESULTS));
                        }
                    } else if (requestXml.contains("http://schemas.xmlsoap.org/ws/2004/09/transfer/Get")) {
                        setResponse(context, XmlUtil.parse(FAULT_RESPONSE_INVALID_SELECTOR));
                    }
                } catch (Exception e) {
                    fail("Unexpected exception: " + e.getMessage());
                }

                return AssertionStatus.NONE;
            }
        }, context, serviceManager, getCancelledCallback(dryRunEvent));

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
        final BundleInfo bundleInfo = getBundleInfo(OAUTH_TEST_BUNDLE_BASE_NAME);
        bundleInfo.addJdbcReference("OAuth");

        final PolicyBundleInstallerContext context = new PolicyBundleInstallerContext(
                bundleInfo, null, null, getBundleResolver(OAUTH_TEST_BUNDLE_BASE_NAME), true);

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
        }, context, serviceManager, getCancelledCallback(installEvent));

        try {
            installer.installBundle();
            fail("Access denied exception should be thrown");
        } catch (GatewayManagementDocumentUtilities.AccessDeniedManagementResponse e) {
            // pass
            // validate that the denied request was set and is non empty
            assertNotNull(e.getDeniedRequest());
            assertFalse(e.getDeniedRequest().trim().isEmpty());
        }
    }
}
