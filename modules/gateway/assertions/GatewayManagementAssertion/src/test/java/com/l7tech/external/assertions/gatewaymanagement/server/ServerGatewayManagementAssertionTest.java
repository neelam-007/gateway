package com.l7tech.external.assertions.gatewaymanagement.server;

import com.l7tech.common.TestDocuments;
import com.l7tech.common.io.XmlUtil;
import com.l7tech.common.mime.ContentTypeHeader;
import com.l7tech.external.assertions.gatewaymanagement.GatewayManagementAssertion;
import com.l7tech.gateway.common.cluster.ClusterProperty;
import com.l7tech.gateway.common.cluster.ClusterStatusAdminStub;
import com.l7tech.gateway.common.jdbc.JdbcConnection;
import com.l7tech.gateway.common.resources.HttpConfiguration;
import com.l7tech.gateway.common.resources.ResourceEntry;
import com.l7tech.gateway.common.resources.ResourceType;
import com.l7tech.gateway.common.security.keystore.SsgKeyEntry;
import com.l7tech.gateway.common.security.password.SecurePassword;
import com.l7tech.gateway.common.security.rbac.OperationType;
import com.l7tech.gateway.common.security.rbac.Role;
import com.l7tech.gateway.common.security.rbac.RoleAssignment;
import com.l7tech.gateway.common.service.PublishedService;
import com.l7tech.gateway.common.service.PublishedServiceAlias;
import com.l7tech.gateway.common.service.ServiceHeader;
import com.l7tech.gateway.common.service.ServiceTemplate;
import com.l7tech.gateway.common.siteminder.SiteMinderConfiguration;
import com.l7tech.gateway.common.transport.SsgActiveConnector;
import com.l7tech.gateway.common.transport.email.EmailListener;
import com.l7tech.gateway.common.transport.email.EmailServerType;
import com.l7tech.gateway.common.transport.jms.JmsConnection;
import com.l7tech.gateway.common.transport.jms.JmsEndpoint;
import com.l7tech.gateway.common.transport.jms.JmsProviderType;
import com.l7tech.identity.*;
import com.l7tech.identity.ldap.LdapIdentityProviderConfig;
import com.l7tech.message.HttpRequestKnob;
import com.l7tech.message.HttpServletRequestKnob;
import com.l7tech.message.HttpServletResponseKnob;
import com.l7tech.message.Message;
import com.l7tech.objectmodel.*;
import com.l7tech.objectmodel.encass.EncapsulatedAssertionArgumentDescriptor;
import com.l7tech.objectmodel.encass.EncapsulatedAssertionConfig;
import com.l7tech.objectmodel.encass.EncapsulatedAssertionResultDescriptor;
import com.l7tech.objectmodel.folder.Folder;
import com.l7tech.policy.*;
import com.l7tech.policy.assertion.JmsRoutingAssertion;
import com.l7tech.policy.assertion.ext.store.KeyValueStoreServices;
import com.l7tech.policy.wsp.WspReader;
import com.l7tech.security.cert.TrustedCert;
import com.l7tech.security.token.http.HttpBasicToken;
import com.l7tech.server.*;
import com.l7tech.server.audit.AuditRecordManagerStub;
import com.l7tech.server.bundling.EntityBundleExporterStub;
import com.l7tech.server.bundling.EntityBundleImporterStub;
import com.l7tech.server.cassandra.CassandraConnectionEntityManagerStub;
import com.l7tech.server.cluster.ClusterPropertyCache;
import com.l7tech.server.cluster.ClusterPropertyManager;
import com.l7tech.server.encass.EncapsulatedAssertionConfigManagerStub;
import com.l7tech.server.entity.GenericEntityManagerStub;
import com.l7tech.server.export.PolicyExporterImporterManagerStub;
import com.l7tech.server.folder.FolderManagerStub;
import com.l7tech.server.globalresources.HttpConfigurationManagerStub;
import com.l7tech.server.globalresources.ResourceEntryManagerStub;
import com.l7tech.server.identity.*;
import com.l7tech.server.identity.cert.ClientCertManagerStub;
import com.l7tech.server.identity.cert.TestTrustedCertManager;
import com.l7tech.server.identity.internal.TestPasswordHasher;
import com.l7tech.server.jdbc.JdbcConnectionManagerStub;
import com.l7tech.server.logon.LogonInfoManagerStub;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.server.message.PolicyEnforcementContextFactory;
import com.l7tech.server.polback.PolicyBackedServiceManagerStub;
import com.l7tech.server.policy.PolicyManager;
import com.l7tech.server.policy.PolicyManagerStub;
import com.l7tech.server.policy.PolicyVersionManagerStub;
import com.l7tech.server.policy.ServerPolicyFactory;
import com.l7tech.server.search.DependencyAnalyzerImpl;
import com.l7tech.server.security.PasswordEnforcerManager;
import com.l7tech.server.security.keystore.SsgKeyFinderStub;
import com.l7tech.server.security.keystore.SsgKeyStoreManagerStub;
import com.l7tech.server.security.password.SecurePasswordManagerStub;
import com.l7tech.server.security.rbac.MockRoleManager;
import com.l7tech.server.security.rbac.RbacServicesStub;
import com.l7tech.server.security.rbac.SecurityZoneManagerStub;
import com.l7tech.server.service.*;
import com.l7tech.server.siteminder.SiteMinderConfigurationManagerStub;
import com.l7tech.server.solutionkit.SolutionKitManagerStub;
import com.l7tech.server.store.CustomKeyValueStoreManagerStub;
import com.l7tech.server.transport.SsgActiveConnectorManagerStub;
import com.l7tech.server.transport.SsgConnectorManagerStub;
import com.l7tech.server.transport.email.EmailListenerManagerStub;
import com.l7tech.server.transport.firewall.SsgFirewallRulesManagerStub;
import com.l7tech.server.transport.jms.JmsConnectionManagerStub;
import com.l7tech.server.transport.jms.JmsEndpointManagerStub;
import com.l7tech.server.uddi.ServiceWsdlUpdateChecker;
import com.l7tech.server.uddi.UDDIServiceControlManagerStub;
import com.l7tech.server.util.ApplicationContextInjector;
import com.l7tech.server.util.ApplicationEventProxy;
import com.l7tech.server.util.ResourceClassLoader;
import com.l7tech.test.BugId;
import com.l7tech.test.BugNumber;
import com.l7tech.util.*;
import com.l7tech.util.Functions.UnaryVoidThrows;
import com.l7tech.xml.soap.SoapUtil;
import junit.framework.Assert;
import org.jetbrains.annotations.Nullable;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Matchers;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockServletContext;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import javax.xml.soap.SOAPConstants;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.security.PrivateKey;
import java.security.cert.X509Certificate;
import java.text.MessageFormat;
import java.util.*;

import static org.junit.Assert.*;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.mockito.Matchers.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Test the GatewayManagementAssertion.
 */
@RunWith(MockitoJUnitRunner.class)
public class ServerGatewayManagementAssertionTest {

    //- PUBLIC

    @Test
    public void testServiceTemplate() {
        // This test is only expected to work if the WSDL has been built / packaged
        if ( GatewayManagementModuleLifecycle.class.getResource("serviceTemplate/gateway-management.wsdl") != null ) {
            final ServiceTemplate template = GatewayManagementModuleLifecycle.createServiceTemplate();
            assertNotNull("Service template null", template);
            assertNotNull("Policy xml null", template.getDefaultPolicyXml());
            assertEquals("Default URI", "/wsman", template.getDefaultUriPrefix());
        } else {
            System.out.println("Test skipped, module not packaged.");
        }
    }

    @Test
    public void testIdentify() throws Exception {
        final String message =
                "<s:Envelope xmlns:s=\"http://www.w3.org/2003/05/soap-envelope\"\n" +
                "            xmlns:wsmid=\"http://schemas.dmtf.org/wbem/wsman/identity/1/wsmanidentity.xsd\">\n" +
                "    <s:Body>\n" +
                "          <wsmid:Identify/>\n" +
                "    </s:Body>\n" +
                "</s:Envelope>";

        final Document result = processRequest( "", message );

        final Element soapBody = SoapUtil.getBodyElement(result);
        final Element identifyResponse = XmlUtil.findExactlyOneChildElementByName(soapBody, NS_WS_MANAGEMENT_IDENTITY, "IdentifyResponse");
        final Element protocol = XmlUtil.findExactlyOneChildElementByName(identifyResponse, NS_WS_MANAGEMENT_IDENTITY, "ProtocolVersion");
        final Element vendor = XmlUtil.findExactlyOneChildElementByName(identifyResponse, NS_WS_MANAGEMENT_IDENTITY, "ProductVendor");

        assertEquals("Protocol", "http://schemas.dmtf.org/wbem/wsman/1/wsman.xsd", XmlUtil.getTextValue(protocol));
        assertEquals("ProductVendor", "Layer 7 Technologies", XmlUtil.getTextValue(vendor));
    }

    /**
     * Test that WS-MetadataExchange requests get a SOAP Fault response
     */
    @Test
    public void testMetadataExchangeGet() throws Exception {
        final String message =
                "<s11:Envelope\n" +
                "    xmlns:s11='http://www.w3.org/2003/05/soap-envelope'\n" +
                "    xmlns:wsa10='http://schemas.xmlsoap.org/ws/2004/08/addressing'>\n" +
                "  <s11:Header>\n" +
                "    <wsa10:Action>\n" +
                "      http://schemas.xmlsoap.org/ws/2004/09/transfer/Get\n" +
                "    </wsa10:Action>\n" +
                "    <wsa10:To>http://127.0.0.1:8080/wsman</wsa10:To>\n" +
                "    <wsa10:ReplyTo>\n" +
                "      <wsa10:Address>http://schemas.xmlsoap.org/ws/2004/08/addressing/role/anonymous</wsa10:Address>\n" +
                "    </wsa10:ReplyTo>\n" +
                "    <wsa10:MessageID>\n" +
                "      urn:uuid:1cec121a-82fe-41da-87e1-3b23f254f128\n" +
                "    </wsa10:MessageID>\n" +
                "  </s11:Header>\n" +
                "  <s11:Body />\n" +
                "</s11:Envelope>";

        final Document result = processRequest( "http://schemas.xmlsoap.org/ws/2004/09/transfer/Get", message );

        final Element soapBody = SoapUtil.getBodyElement(result);
        XmlUtil.findExactlyOneChildElementByName(soapBody, SOAPConstants.URI_NS_SOAP_1_2_ENVELOPE, "Fault");
    }

    /**
     * Test that WS-Eventing subscription requests get a SOAP Fault response
     */
    @Test
    public void testWSEventingSubscription() throws Exception {
        final String message =
                "<s:Envelope xmlns:s=\"http://www.w3.org/2003/05/soap-envelope\" \n" +
                "            xmlns:a=\"http://schemas.xmlsoap.org/ws/2004/08/addressing\" \n" +
                "            xmlns:wse=\"http://schemas.xmlsoap.org/ws/2004/08/eventing\" \n" +
                "            xmlns:w=\"http://schemas.dmtf.org/wbem/wsman/1/wsman.xsd\">\n" +
                "  <s:Header>\n" +
                "    <a:MessageID>uuid:4ED2993C-4339-4E99-81FC-C2FD3812781A</a:MessageID> \n" +
                "    <a:To>http://127.0.0.1:8080/wsman</a:To> \n" +
                "    <a:ReplyTo> \n" +
                "      <a:Address s:mustUnderstand=\"true\">http://schemas.xmlsoap.org/ws/2004/08/addressing/role/anonymous</a:Address> \n" +
                "    </a:ReplyTo> \n" +
                "    <a:Action s:mustUnderstand=\"true\">http://schemas.xmlsoap.org/ws/2004/08/eventing/Subscribe</a:Action> \n" +
                "    <w:ResourceURI s:mustUnderstand=\"true\">http://ns.l7tech.com/2010/04/gateway-management/services</w:ResourceURI> \n" +
                "    <w:SelectorSet>\n" +
                "      <w:Selector Name=\"id\">1</w:Selector> \n" +
                "    </w:SelectorSet>\n" +
                "    <w:OperationTimeout>PT60.000S</w:OperationTimeout> \n" +
                "  </s:Header>\n" +
                "  <s:Body>" +
                "    <wse:Subscribe>\n" +
                "      <wse:Delivery>\n" +
                "        <wse:NotifyTo><a:Address>http://127.0.0.1/notification</a:Address></wse:NotifyTo>\n" +
                "        <w:ConnectionRetry Total=\"count\">PT60.000S</w:ConnectionRetry>\n" +
                "      </wse:Delivery>\n" +
                "    </wse:Subscribe>" +
                "  </s:Body> \n" +
                "</s:Envelope>";

        final Document result = processRequest( "http://schemas.xmlsoap.org/ws/2004/08/eventing/Subscribe", message );

        final Element soapBody = SoapUtil.getBodyElement(result);
        XmlUtil.findExactlyOneChildElementByName(soapBody, SOAPConstants.URI_NS_SOAP_1_2_ENVELOPE, "Fault");
    }

    @Test
    public void testGet() throws Exception {
        final String message =
                "<s:Envelope xmlns:s=\"http://www.w3.org/2003/05/soap-envelope\" \n" +
                "            xmlns:a=\"http://schemas.xmlsoap.org/ws/2004/08/addressing\" \n" +
                "            xmlns:w=\"http://schemas.dmtf.org/wbem/wsman/1/wsman.xsd\">\n" +
                "  <s:Header>\n" +
                "    <a:MessageID>uuid:4ED2993C-4339-4E99-81FC-C2FD3812781A</a:MessageID> \n" +
                "    <a:To>http://127.0.0.1:8080/wsman</a:To> \n" +
                "    <a:ReplyTo> \n" +
                "      <a:Address s:mustUnderstand=\"true\">http://schemas.xmlsoap.org/ws/2004/08/addressing/role/anonymous</a:Address> \n" +
                "    </a:ReplyTo> \n" +
                "    <a:Action s:mustUnderstand=\"true\">http://schemas.xmlsoap.org/ws/2004/09/transfer/Get</a:Action> \n" +
                "    <w:ResourceURI s:mustUnderstand=\"true\">http://ns.l7tech.com/2010/04/gateway-management/services</w:ResourceURI> \n" +
                "    <w:SelectorSet>\n" +
                "      <w:Selector Name=\"id\">"+new Goid(0,2)+"</w:Selector> \n" +
                "    </w:SelectorSet>\n" +
                "    <w:OperationTimeout>PT60.000S</w:OperationTimeout> \n" +
                "  </s:Header>\n" +
                "  <s:Body/> \n" +
                "</s:Envelope>";

        final Document result = processRequest( "http://schemas.xmlsoap.org/ws/2004/09/transfer/Get", message );

        final Element soapBody = SoapUtil.getBodyElement(result);
        final Element serviceContainer = XmlUtil.findExactlyOneChildElementByName(soapBody, NS_GATEWAY_MANAGEMENT, "Service");
        final Element service = XmlUtil.findExactlyOneChildElementByName(serviceContainer, NS_GATEWAY_MANAGEMENT, "ServiceDetail");
        final Element name = XmlUtil.findExactlyOneChildElementByName(service, NS_GATEWAY_MANAGEMENT, "Name");
        final Element properties = XmlUtil.findExactlyOneChildElementByName(service, NS_GATEWAY_MANAGEMENT, "Properties");

        assertEquals("Service name", "Test Service 2", XmlUtil.getTextValue(name));
        assertEquals("Service soapVersion", "unspecified", getPropertyValue(properties, "soapVersion"));
    }

    @Test
    public void testGetFolder() throws Exception {
        final String message =
                "<s:Envelope xmlns:s=\"http://www.w3.org/2003/05/soap-envelope\" \n" +
                        "            xmlns:a=\"http://schemas.xmlsoap.org/ws/2004/08/addressing\" \n" +
                        "            xmlns:w=\"http://schemas.dmtf.org/wbem/wsman/1/wsman.xsd\">\n" +
                        "  <s:Header>\n" +
                        "    <a:MessageID>uuid:4ED2993C-4339-4E99-81FC-C2FD3812781A</a:MessageID> \n" +
                        "    <a:To>http://127.0.0.1:8080/wsman</a:To> \n" +
                        "    <a:ReplyTo> \n" +
                        "      <a:Address s:mustUnderstand=\"true\">http://schemas.xmlsoap.org/ws/2004/08/addressing/role/anonymous</a:Address> \n" +
                        "    </a:ReplyTo> \n" +
                        "    <a:Action s:mustUnderstand=\"true\">http://schemas.xmlsoap.org/ws/2004/09/transfer/Get</a:Action> \n" +
                        "    <w:ResourceURI s:mustUnderstand=\"true\">http://ns.l7tech.com/2010/04/gateway-management/folders</w:ResourceURI> \n" +
                        "    <w:SelectorSet>\n" +
                        "      <w:Selector Name=\"id\">"+new Goid(0,1)+"</w:Selector> \n" +
                        "    </w:SelectorSet>\n" +
                        "    <w:OperationTimeout>PT60.000S</w:OperationTimeout> \n" +
                        "  </s:Header>\n" +
                        "  <s:Body/> \n" +
                        "</s:Envelope>";

        final Document result = processRequest( "http://schemas.xmlsoap.org/ws/2004/09/transfer/Get", message );

        final Element soapBody = SoapUtil.getBodyElement(result);
        final Element folder = XmlUtil.findExactlyOneChildElementByName(soapBody, NS_GATEWAY_MANAGEMENT, "Folder");
        final Element name = XmlUtil.findExactlyOneChildElementByName(folder, NS_GATEWAY_MANAGEMENT, "Name");
        String folderId = folder.getAttribute("folderId");
        String id = folder.getAttribute("id");

        assertEquals("Service name", "Test Folder", XmlUtil.getTextValue(name));
        assertEquals("id", new Goid(0,1).toHexString(), id);
        assertEquals("parent id", Folder.ROOT_FOLDER_ID.toHexString(), folderId);
    }

    @Test
    public void testGetFolderOldRootOid() throws Exception {
        final String message =
                "<s:Envelope xmlns:s=\"http://www.w3.org/2003/05/soap-envelope\" \n" +
                        "            xmlns:a=\"http://schemas.xmlsoap.org/ws/2004/08/addressing\" \n" +
                        "            xmlns:w=\"http://schemas.dmtf.org/wbem/wsman/1/wsman.xsd\">\n" +
                        "  <s:Header>\n" +
                        "    <a:MessageID>uuid:4ED2993C-4339-4E99-81FC-C2FD3812781A</a:MessageID> \n" +
                        "    <a:To>http://127.0.0.1:8080/wsman</a:To> \n" +
                        "    <a:ReplyTo> \n" +
                        "      <a:Address s:mustUnderstand=\"true\">http://schemas.xmlsoap.org/ws/2004/08/addressing/role/anonymous</a:Address> \n" +
                        "    </a:ReplyTo> \n" +
                        "    <a:Action s:mustUnderstand=\"true\">http://schemas.xmlsoap.org/ws/2004/09/transfer/Get</a:Action> \n" +
                        "    <w:ResourceURI s:mustUnderstand=\"true\">http://ns.l7tech.com/2010/04/gateway-management/folders</w:ResourceURI> \n" +
                        "    <w:SelectorSet>\n" +
                        "      <w:Selector Name=\"id\">-5002</w:Selector> \n" +
                        "    </w:SelectorSet>\n" +
                        "    <w:OperationTimeout>PT60.000S</w:OperationTimeout> \n" +
                        "  </s:Header>\n" +
                        "  <s:Body/> \n" +
                        "</s:Envelope>";

        final Document result = processRequest( "http://schemas.xmlsoap.org/ws/2004/09/transfer/Get", message );

        final Element soapBody = SoapUtil.getBodyElement(result);
        final Element folder = XmlUtil.findExactlyOneChildElementByName(soapBody, NS_GATEWAY_MANAGEMENT, "Folder");
        final Element name = XmlUtil.findExactlyOneChildElementByName(folder, NS_GATEWAY_MANAGEMENT, "Name");
        String folderId = folder.getAttribute("folderId");
        String id = folder.getAttribute("id");

        assertEquals("Service name", "Root Node", XmlUtil.getTextValue(name));
        assertEquals("id", Folder.ROOT_FOLDER_ID.toHexString(), id);
        assertEquals("parent id", "", folderId);
    }

    @BugId("SSG-5551")
    @Test
    public void testGetPolicyWithGuid() throws Exception {
        final String policyGuid = UUID.nameUUIDFromBytes(Goid.toString(new Goid(0,1)).getBytes()).toString();
        String message =
                "<env:Envelope xmlns:env=\"http://www.w3.org/2003/05/soap-envelope\"\n" +
                        "    xmlns:wsa=\"http://schemas.xmlsoap.org/ws/2004/08/addressing\"\n" +
                        "    xmlns:wse=\"http://schemas.xmlsoap.org/ws/2004/08/eventing\"\n" +
                        "    xmlns:wsen=\"http://schemas.xmlsoap.org/ws/2004/09/enumeration\"\n" +
                        "    xmlns:wsman=\"http://schemas.dmtf.org/wbem/wsman/1/wsman.xsd\"\n" +
                        "    xmlns:wxf=\"http://schemas.xmlsoap.org/ws/2004/09/transfer\" xmlns:xs=\"http://www.w3.org/2001/XMLSchema\">\n" +
                        "    <env:Header>\n" +
                        "        <wsa:Action env:mustUnderstand=\"true\">http://schemas.xmlsoap.org/ws/2004/09/transfer/Get</wsa:Action>\n" +
                        "        <wsa:ReplyTo>\n" +
                        "            <wsa:Address env:mustUnderstand=\"true\">http://schemas.xmlsoap.org/ws/2004/08/addressing/role/anonymous</wsa:Address>\n" +
                        "        </wsa:ReplyTo>\n" +
                        "        <wsa:MessageID env:mustUnderstand=\"true\">uuid:b9bba2b9-42ae-4507-92ba-d93dbbaf38b9</wsa:MessageID>\n" +
                        "        <wsa:To env:mustUnderstand=\"true\">http://localhost:8080/wsman</wsa:To>\n" +
                        "        <wsman:ResourceURI>http://ns.l7tech.com/2010/04/gateway-management/policies</wsman:ResourceURI>\n" +
                        "        <wsman:OperationTimeout>P0Y0M0DT0H5M0.000S</wsman:OperationTimeout>\n" +
                        "        <wsman:SelectorSet>\n" +
                        "            <wsman:Selector Name=\"guid\">" + policyGuid + "</wsman:Selector>\n" +
                        "        </wsman:SelectorSet>\n" +
                        "    </env:Header>\n" +
                        "    <env:Body/>\n" +
                        "</env:Envelope>\n";

        final Document result = processRequest( "http://schemas.xmlsoap.org/ws/2004/09/transfer/Get", message );

        final Element soapBody = SoapUtil.getBodyElement(result);
        final Element serviceContainer = XmlUtil.findExactlyOneChildElementByName(soapBody, NS_GATEWAY_MANAGEMENT, "Policy");
        final Element service = XmlUtil.findExactlyOneChildElementByName(serviceContainer, NS_GATEWAY_MANAGEMENT, "PolicyDetail");
        final Element name = XmlUtil.findExactlyOneChildElementByName(service, NS_GATEWAY_MANAGEMENT, "Name");
        final Element properties = XmlUtil.findExactlyOneChildElementByName(service, NS_GATEWAY_MANAGEMENT, "Properties");

        assertEquals("Policy Name", "Test Policy", XmlUtil.getTextValue(name));
        assertEquals("Policy is soap", "true", getPropertyValue(properties, "soap"));
    }

    @Test
    public void testGetInterfaceTag() throws Exception {
        final String message =
                "<s:Envelope xmlns:s=\"http://www.w3.org/2003/05/soap-envelope\" \n" +
                "            xmlns:a=\"http://schemas.xmlsoap.org/ws/2004/08/addressing\" \n" +
                "            xmlns:w=\"http://schemas.dmtf.org/wbem/wsman/1/wsman.xsd\">\n" +
                "  <s:Header>\n" +
                "    <a:MessageID>uuid:4ED2993C-4339-4E99-81FC-C2FD3812781A</a:MessageID> \n" +
                "    <a:To>http://127.0.0.1:8080/wsman</a:To> \n" +
                "    <a:ReplyTo> \n" +
                "      <a:Address s:mustUnderstand=\"true\">http://schemas.xmlsoap.org/ws/2004/08/addressing/role/anonymous</a:Address> \n" +
                "    </a:ReplyTo> \n" +
                "    <a:Action s:mustUnderstand=\"true\">http://schemas.xmlsoap.org/ws/2004/09/transfer/Get</a:Action> \n" +
                "    <w:ResourceURI s:mustUnderstand=\"true\">http://ns.l7tech.com/2010/04/gateway-management/interfaceTags</w:ResourceURI> \n" +
                "    <w:SelectorSet>\n" +
                "      <w:Selector Name=\"name\">localhost</w:Selector> \n" +
                "    </w:SelectorSet>\n" +
                "    <w:OperationTimeout>PT60.000S</w:OperationTimeout> \n" +
                "  </s:Header>\n" +
                "  <s:Body/> \n" +
                "</s:Envelope>";

        final Document result = processRequest( "http://schemas.xmlsoap.org/ws/2004/09/transfer/Get", message );

        final Element soapBody = SoapUtil.getBodyElement(result);
        final Element interfaceTag = XmlUtil.findExactlyOneChildElementByName( soapBody, NS_GATEWAY_MANAGEMENT, "InterfaceTag" );
        final Element name = XmlUtil.findExactlyOneChildElementByName(interfaceTag, NS_GATEWAY_MANAGEMENT, "Name");
        final Element addressPatterns = XmlUtil.findExactlyOneChildElementByName(interfaceTag, NS_GATEWAY_MANAGEMENT, "AddressPatterns");
        final Element stringValue = XmlUtil.findExactlyOneChildElementByName(addressPatterns, NS_GATEWAY_MANAGEMENT, "StringValue");

        // FYI the GUID below is generated via UUID.nameUUIDFromBytes which produces a constant UUID for it's input
        // in this case its generated from the above selector name value of 'localhost'.
        // due to this deterministic value behaviour there is no mocking required in this test case.
        assertEquals("Interface tag id", "421aa90e-079f-3326-b649-4f812ad13e79", interfaceTag.getAttribute("id"));
        assertEquals("Interface tag name", "localhost", XmlUtil.getTextValue( name ));
        assertEquals("Interface tag ip pattern", "127.0.0.1", XmlUtil.getTextValue(stringValue));
    }

    @BugId("SSG-5572")
    @Test
    public void testGetGenericEntity() throws Exception {
        final String message =
                "<s:Envelope xmlns:s=\"http://www.w3.org/2003/05/soap-envelope\" \n" +
                "            xmlns:a=\"http://schemas.xmlsoap.org/ws/2004/08/addressing\" \n" +
                "            xmlns:w=\"http://schemas.dmtf.org/wbem/wsman/1/wsman.xsd\">\n" +
                "  <s:Header>\n" +
                "    <a:MessageID>uuid:4ED2993C-4339-4E99-81FC-C2FD3812781A</a:MessageID> \n" +
                "    <a:To>http://127.0.0.1:8080/wsman</a:To> \n" +
                "    <a:ReplyTo> \n" +
                "      <a:Address s:mustUnderstand=\"true\">http://schemas.xmlsoap.org/ws/2004/08/addressing/role/anonymous</a:Address> \n" +
                "    </a:ReplyTo> \n" +
                "    <a:Action s:mustUnderstand=\"true\">http://schemas.xmlsoap.org/ws/2004/09/transfer/Get</a:Action> \n" +
                "    <w:ResourceURI s:mustUnderstand=\"true\">http://ns.l7tech.com/2010/04/gateway-management/genericEntities</w:ResourceURI> \n" +
                "    <w:SelectorSet>\n" +
                "      <w:Selector Name=\"id\">"+new Goid(0,1).toHexString()+"</w:Selector> \n" +
                "    </w:SelectorSet>\n" +
                "    <w:OperationTimeout>PT60.000S</w:OperationTimeout> \n" +
                "  </s:Header>\n" +
                "  <s:Body/> \n" +
                "</s:Envelope>";

        final Document result = processRequest("http://schemas.xmlsoap.org/ws/2004/09/transfer/Get", message);
        System.out.println(XmlUtil.nodeToFormattedString(result));

        final Element soapBody = SoapUtil.getBodyElement(result);
        final Element genericEntityElm = XmlUtil.findExactlyOneChildElementByName(soapBody, NS_GATEWAY_MANAGEMENT, "GenericEntity");
        System.out.println(XmlUtil.nodeToString(genericEntityElm));
        final Element nameElm = XmlUtil.findExactlyOneChildElementByName(genericEntityElm, NS_GATEWAY_MANAGEMENT, "Name");
        final Element descriptionElm = XmlUtil.findExactlyOneChildElementByName(genericEntityElm, NS_GATEWAY_MANAGEMENT, "Description");
        final Element entityClassNameElm = XmlUtil.findExactlyOneChildElementByName(genericEntityElm, NS_GATEWAY_MANAGEMENT, "EntityClassName");
        final Element enabledElm = XmlUtil.findExactlyOneChildElementByName(genericEntityElm, NS_GATEWAY_MANAGEMENT, "Enabled");
        final Element valueXmlElm = XmlUtil.findExactlyOneChildElementByName(genericEntityElm, NS_GATEWAY_MANAGEMENT, "ValueXml");

        assertEquals("Name", "My Test Entity", DomUtils.getTextValue(nameElm));
        assertEquals("Description", "My test entity description", DomUtils.getTextValue(descriptionElm));
        assertEquals("Entity class name", this.getClass().getName(), DomUtils.getTextValue(entityClassNameElm));
        assertTrue("Enabled", Boolean.valueOf(DomUtils.getTextValue(enabledElm)));
        assertEquals("ValueXml", "<xml>xml value</xml>", DomUtils.getTextValue(valueXmlElm));
    }

    @Test
    public void testGetLDAPIdentityProvider() throws Exception {
        final String message =
                "<s:Envelope xmlns:s=\"http://www.w3.org/2003/05/soap-envelope\" \n" +
                "            xmlns:a=\"http://schemas.xmlsoap.org/ws/2004/08/addressing\" \n" +
                "            xmlns:w=\"http://schemas.dmtf.org/wbem/wsman/1/wsman.xsd\">\n" +
                "  <s:Header>\n" +
                "    <a:MessageID>uuid:4ED2993C-4339-4E99-81FC-C2FD3812781A</a:MessageID> \n" +
                "    <a:To>http://127.0.0.1:8080/wsman</a:To> \n" +
                "    <a:ReplyTo> \n" +
                "      <a:Address s:mustUnderstand=\"true\">http://schemas.xmlsoap.org/ws/2004/08/addressing/role/anonymous</a:Address> \n" +
                "    </a:ReplyTo> \n" +
                "    <a:Action s:mustUnderstand=\"true\">http://schemas.xmlsoap.org/ws/2004/09/transfer/Get</a:Action> \n" +
                "    <w:ResourceURI s:mustUnderstand=\"true\">http://ns.l7tech.com/2010/04/gateway-management/identityProviders</w:ResourceURI> \n" +
                "    <w:SelectorSet>\n" +
                "      <w:Selector Name=\"name\">LDAP</w:Selector> \n" +
                "    </w:SelectorSet>\n" +
                "    <w:OperationTimeout>PT60.000S</w:OperationTimeout> \n" +
                "  </s:Header>\n" +
                "  <s:Body/> \n" +
                "</s:Envelope>";

        final Document result = processRequest( "http://schemas.xmlsoap.org/ws/2004/09/transfer/Get", message );

        final Element soapBody = SoapUtil.getBodyElement(result);
        final Element identityProvider = XmlUtil.findExactlyOneChildElementByName( soapBody, NS_GATEWAY_MANAGEMENT, "IdentityProvider" );
        final Element name = XmlUtil.findExactlyOneChildElementByName(identityProvider, NS_GATEWAY_MANAGEMENT, "Name");

        assertEquals("Identity provider name", "LDAP", XmlUtil.getTextValue( name ));
    }

    @Test
    public void testGetJmsDestination() throws Exception {
        final String id = new Goid(0,1).toString();
        final String message =
                "<s:Envelope xmlns:s=\"http://www.w3.org/2003/05/soap-envelope\" \n" +
                        "            xmlns:a=\"http://schemas.xmlsoap.org/ws/2004/08/addressing\" \n" +
                        "            xmlns:w=\"http://schemas.dmtf.org/wbem/wsman/1/wsman.xsd\">\n" +
                        "  <s:Header>\n" +
                        "    <a:MessageID>uuid:4ED2993C-4339-4E99-81FC-C2FD3812781A</a:MessageID> \n" +
                        "    <a:To>http://127.0.0.1:8080/wsman</a:To> \n" +
                        "    <a:ReplyTo> \n" +
                        "      <a:Address s:mustUnderstand=\"true\">http://schemas.xmlsoap.org/ws/2004/08/addressing/role/anonymous</a:Address> \n" +
                        "    </a:ReplyTo> \n" +
                        "    <a:Action s:mustUnderstand=\"true\">http://schemas.xmlsoap.org/ws/2004/09/transfer/Get</a:Action> \n" +
                        "    <w:ResourceURI s:mustUnderstand=\"true\">http://ns.l7tech.com/2010/04/gateway-management/jmsDestinations</w:ResourceURI> \n" +
                        "    <w:SelectorSet>\n" +
                        "      <w:Selector Name=\"id\">"+id+"</w:Selector> \n" +
                        "    </w:SelectorSet>\n" +
                        "    <w:OperationTimeout>PT60.000S</w:OperationTimeout> \n" +
                        "  </s:Header>\n" +
                        "  <s:Body/> \n" +
                        "</s:Envelope>";

        final Document result = processRequest( "http://schemas.xmlsoap.org/ws/2004/09/transfer/Get", message );

        final Element soapBody = SoapUtil.getBodyElement(result);
        final Element jmsDestination = XmlUtil.findExactlyOneChildElementByName( soapBody, NS_GATEWAY_MANAGEMENT, "JMSDestination" );

        assertEquals("Jms Destination identifier:", id, jmsDestination.getAttribute("id"));
    }

    @BugId("SSG-7449")
    @Test
    public void testGetJmsDestinationWithJNDIPassword() throws Exception {
        final Goid id = new Goid(0,1);

        final String idStr = id.toString();
        final String message =
                "<s:Envelope xmlns:s=\"http://www.w3.org/2003/05/soap-envelope\" \n" +
                        "            xmlns:a=\"http://schemas.xmlsoap.org/ws/2004/08/addressing\" \n" +
                        "            xmlns:w=\"http://schemas.dmtf.org/wbem/wsman/1/wsman.xsd\">\n" +
                        "  <s:Header>\n" +
                        "    <a:MessageID>uuid:4ED2993C-4339-4E99-81FC-C2FD3812781A</a:MessageID> \n" +
                        "    <a:To>http://127.0.0.1:8080/wsman</a:To> \n" +
                        "    <a:ReplyTo> \n" +
                        "      <a:Address s:mustUnderstand=\"true\">http://schemas.xmlsoap.org/ws/2004/08/addressing/role/anonymous</a:Address> \n" +
                        "    </a:ReplyTo> \n" +
                        "    <a:Action s:mustUnderstand=\"true\">http://schemas.xmlsoap.org/ws/2004/09/transfer/Get</a:Action> \n" +
                        "    <w:ResourceURI s:mustUnderstand=\"true\">http://ns.l7tech.com/2010/04/gateway-management/jmsDestinations</w:ResourceURI> \n" +
                        "    <w:SelectorSet>\n" +
                        "      <w:Selector Name=\"id\">"+idStr+"</w:Selector> \n" +
                        "    </w:SelectorSet>\n" +
                        "    <w:OperationTimeout>PT60.000S</w:OperationTimeout> \n" +
                        "  </s:Header>\n" +
                        "  <s:Body/> \n" +
                        "</s:Envelope>";

        final Document result = processRequest( "http://schemas.xmlsoap.org/ws/2004/09/transfer/Get", message );

        final Element soapBody = SoapUtil.getBodyElement(result);
        final Element jmsDestination = XmlUtil.findExactlyOneChildElementByName( soapBody, NS_GATEWAY_MANAGEMENT, "JMSDestination" );
        final Element jmsConnection = XmlUtil.findExactlyOneChildElementByName( jmsDestination, NS_GATEWAY_MANAGEMENT, "JMSConnection" );
        final Element contextProperties = XmlUtil.findExactlyOneChildElementByName( jmsConnection, NS_GATEWAY_MANAGEMENT, "ContextPropertiesTemplate" );

        assertEquals("Jms Destination identifier:", idStr, jmsDestination.getAttribute("id"));

        try{
            final Element jndiPassword = XmlUtil.findExactlyOneChildElementByName( contextProperties, NS_GATEWAY_MANAGEMENT, "java.naming.security.credentials" );
            fail("Jndi password element found");
        }catch(MissingRequiredElementException e){
            // expected
        }
    }

    @Test
    public void testGetActiveConnector() throws Exception {
        final String id = new Goid(0,2).toString();
        final String message =
                "<s:Envelope xmlns:s=\"http://www.w3.org/2003/05/soap-envelope\" \n" +
                        "            xmlns:a=\"http://schemas.xmlsoap.org/ws/2004/08/addressing\" \n" +
                        "            xmlns:w=\"http://schemas.dmtf.org/wbem/wsman/1/wsman.xsd\">\n" +
                        "  <s:Header>\n" +
                        "    <a:MessageID>uuid:4ED2993C-4339-4E99-81FC-C2FD3812781A</a:MessageID> \n" +
                        "    <a:To>http://127.0.0.1:8080/wsman</a:To> \n" +
                        "    <a:ReplyTo> \n" +
                        "      <a:Address s:mustUnderstand=\"true\">http://schemas.xmlsoap.org/ws/2004/08/addressing/role/anonymous</a:Address> \n" +
                        "    </a:ReplyTo> \n" +
                        "    <a:Action s:mustUnderstand=\"true\">http://schemas.xmlsoap.org/ws/2004/09/transfer/Get</a:Action> \n" +
                        "    <w:ResourceURI s:mustUnderstand=\"true\">http://ns.l7tech.com/2010/04/gateway-management/activeConnectors</w:ResourceURI> \n" +
                        "    <w:SelectorSet>\n" +
                        "      <w:Selector Name=\"id\">"+id+"</w:Selector> \n" +
                        "    </w:SelectorSet>\n" +
                        "    <w:OperationTimeout>PT60.000S</w:OperationTimeout> \n" +
                        "  </s:Header>\n" +
                        "  <s:Body/> \n" +
                        "</s:Envelope>";

        final Document result = processRequest( "http://schemas.xmlsoap.org/ws/2004/09/transfer/Get", message );

        final Element soapBody = SoapUtil.getBodyElement(result);
        final Element activeConnector = XmlUtil.findExactlyOneChildElementByName( soapBody, NS_GATEWAY_MANAGEMENT, "ActiveConnector" );
        final Element type = XmlUtil.findExactlyOneChildElementByName( activeConnector, NS_GATEWAY_MANAGEMENT, "Type" );
        final Element hardwiredId = XmlUtil.findExactlyOneChildElementByName( activeConnector, NS_GATEWAY_MANAGEMENT, "HardwiredId" );

        assertEquals("Active connector identifier:", id, activeConnector.getAttribute("id"));
        assertEquals("Active connector type:", "SFTP", XmlUtil.getTextValue(type));
        assertEquals("Active connector hardwired id:", new Goid(0,4567).toHexString(),XmlUtil.getTextValue(hardwiredId));
    }

    @Test
    public void testGetInvalidSelector() throws Exception {
        final String message =
                "<s:Envelope xmlns:s=\"http://www.w3.org/2003/05/soap-envelope\" \n" +
                "            xmlns:a=\"http://schemas.xmlsoap.org/ws/2004/08/addressing\" \n" +
                "            xmlns:w=\"http://schemas.dmtf.org/wbem/wsman/1/wsman.xsd\">\n" +
                "  <s:Header>\n" +
                "    <a:MessageID>uuid:4ED2993C-4339-4E99-81FC-C2FD3812781A</a:MessageID> \n" +
                "    <a:To>http://127.0.0.1:8080/wsman</a:To> \n" +
                "    <a:ReplyTo> \n" +
                "      <a:Address s:mustUnderstand=\"true\">http://schemas.xmlsoap.org/ws/2004/08/addressing/role/anonymous</a:Address> \n" +
                "    </a:ReplyTo> \n" +
                "    <a:Action s:mustUnderstand=\"true\">http://schemas.xmlsoap.org/ws/2004/09/transfer/Get</a:Action> \n" +
                "    <w:ResourceURI s:mustUnderstand=\"true\">http://ns.l7tech.com/2010/04/gateway-management/services</w:ResourceURI> \n" +
                "    <w:SelectorSet>\n" +
                "      <w:Selector Name=\"unknown-selector\">1</w:Selector> \n" +
                "    </w:SelectorSet>\n" +
                "    <w:OperationTimeout>PT60.000S</w:OperationTimeout> \n" +
                "  </s:Header>\n" +
                "  <s:Body/> \n" +
                "</s:Envelope>";

        final Document result = processRequest( "http://schemas.xmlsoap.org/ws/2004/09/transfer/Get", message );

        final Element soapBody = SoapUtil.getBodyElement(result);
        final Element soapFault = XmlUtil.findExactlyOneChildElementByName(soapBody, SOAPConstants.URI_NS_SOAP_1_2_ENVELOPE, "Fault");
        final Element code = XmlUtil.findExactlyOneChildElementByName(soapFault, SOAPConstants.URI_NS_SOAP_1_2_ENVELOPE, "Code");
        final Element subcode = XmlUtil.findExactlyOneChildElementByName(code, SOAPConstants.URI_NS_SOAP_1_2_ENVELOPE, "Subcode");
        final Element value = XmlUtil.findExactlyOneChildElementByName(subcode, SOAPConstants.URI_NS_SOAP_1_2_ENVELOPE, "Value");

        assertEquals("SOAP Fault value", "wsman:InvalidSelectors", XmlUtil.getTextValue(value));
    }

    @BugNumber(9585)
    @Test
    public void testGetInvalidOperationTimeout() throws Exception {
        final String message =
                "<s:Envelope xmlns:s=\"http://www.w3.org/2003/05/soap-envelope\" \n" +
                "            xmlns:a=\"http://schemas.xmlsoap.org/ws/2004/08/addressing\" \n" +
                "            xmlns:w=\"http://schemas.dmtf.org/wbem/wsman/1/wsman.xsd\">\n" +
                "  <s:Header>\n" +
                "    <a:MessageID>uuid:4ED2993C-4339-4E99-81FC-C2FD3812781A</a:MessageID> \n" +
                "    <a:To>http://127.0.0.1:8080/wsman</a:To> \n" +
                "    <a:ReplyTo> \n" +
                "      <a:Address s:mustUnderstand=\"true\">http://schemas.xmlsoap.org/ws/2004/08/addressing/role/anonymous</a:Address> \n" +
                "    </a:ReplyTo> \n" +
                "    <a:Action s:mustUnderstand=\"true\">http://schemas.xmlsoap.org/ws/2004/09/transfer/Get</a:Action> \n" +
                "    <w:ResourceURI s:mustUnderstand=\"true\">http://ns.l7tech.com/2010/04/gateway-management/services</w:ResourceURI> \n" +
                "    <w:SelectorSet>\n" +
                "      <w:Selector Name=\"id\">1</w:Selector> \n" +
                "    </w:SelectorSet>\n" +
                "    <w:OperationTimeout>10</w:OperationTimeout> \n" +
                "  </s:Header>\n" +
                "  <s:Body/> \n" +
                "</s:Envelope>";

        final Document result = processRequest( "http://schemas.xmlsoap.org/ws/2004/09/transfer/Get", message );

        final Element soapBody = SoapUtil.getBodyElement(result);
        final Element soapFault = XmlUtil.findExactlyOneChildElementByName(soapBody, SOAPConstants.URI_NS_SOAP_1_2_ENVELOPE, "Fault");
        final Element code = XmlUtil.findExactlyOneChildElementByName(soapFault, SOAPConstants.URI_NS_SOAP_1_2_ENVELOPE, "Code");
        final Element subcode = XmlUtil.findExactlyOneChildElementByName(code, SOAPConstants.URI_NS_SOAP_1_2_ENVELOPE, "Subcode");
        final Element value = XmlUtil.findExactlyOneChildElementByName(subcode, SOAPConstants.URI_NS_SOAP_1_2_ENVELOPE, "Value");

        assertEquals("SOAP Fault value", "wsman:SchemaValidationError", XmlUtil.getTextValue(value));
    }

    @Test
    public void testGetCustomKeyValueById() throws Exception {
        final String message =
            "<s:Envelope xmlns:s=\"http://www.w3.org/2003/05/soap-envelope\" \n" +
            "            xmlns:a=\"http://schemas.xmlsoap.org/ws/2004/08/addressing\" \n" +
            "            xmlns:w=\"http://schemas.dmtf.org/wbem/wsman/1/wsman.xsd\">\n" +
            "  <s:Header>\n" +
            "    <a:MessageID>uuid:4ED2993C-4339-4E99-81FC-C2FD3812781A</a:MessageID> \n" +
            "    <a:To>http://127.0.0.1:8080/wsman</a:To> \n" +
            "    <a:ReplyTo> \n" +
            "      <a:Address s:mustUnderstand=\"true\">http://schemas.xmlsoap.org/ws/2004/08/addressing/role/anonymous</a:Address> \n" +
            "    </a:ReplyTo> \n" +
            "    <a:Action s:mustUnderstand=\"true\">http://schemas.xmlsoap.org/ws/2004/09/transfer/Get</a:Action> \n" +
            "    <w:ResourceURI s:mustUnderstand=\"true\">http://ns.l7tech.com/2010/04/gateway-management/customKeyValues</w:ResourceURI> \n" +
            "    <w:SelectorSet>\n" +
            "      <w:Selector Name=\"id\">"+new Goid(0,1).toHexString()+"</w:Selector> \n" +
            "    </w:SelectorSet>\n" +
            "    <w:OperationTimeout>PT60.000S</w:OperationTimeout> \n" +
            "  </s:Header>\n" +
            "  <s:Body/> \n" +
            "</s:Envelope>";

        final Document result = processRequest("http://schemas.xmlsoap.org/ws/2004/09/transfer/Get", message);
        final Element soapBody = SoapUtil.getBodyElement(result);
        final Element customKeyValueElm = XmlUtil.findExactlyOneChildElementByName(soapBody, NS_GATEWAY_MANAGEMENT, "CustomKeyValue");
        final Element storeNameElm = XmlUtil.findExactlyOneChildElementByName(customKeyValueElm, NS_GATEWAY_MANAGEMENT, "StoreName");
        final Element keyElm = XmlUtil.findExactlyOneChildElementByName(customKeyValueElm, NS_GATEWAY_MANAGEMENT, "Key");
        final Element valueElm = XmlUtil.findExactlyOneChildElementByName(customKeyValueElm, NS_GATEWAY_MANAGEMENT, "Value");

        assertEquals(KeyValueStoreServices.INTERNAL_TRANSACTIONAL_KEY_VALUE_STORE_NAME, DomUtils.getTextValue(storeNameElm));
        assertEquals("key.prefix.key1", DomUtils.getTextValue(keyElm));
        assertEquals("PHhtbD5UZXN0IHZhbHVlPC94bWw+", DomUtils.getTextValue(valueElm));
    }

    @Test
    public void testGetCustomKeyValueByKey() throws Exception {
        final String message =
            "<s:Envelope xmlns:s=\"http://www.w3.org/2003/05/soap-envelope\" \n" +
            "            xmlns:a=\"http://schemas.xmlsoap.org/ws/2004/08/addressing\" \n" +
            "            xmlns:w=\"http://schemas.dmtf.org/wbem/wsman/1/wsman.xsd\">\n" +
            "  <s:Header>\n" +
            "    <a:MessageID>uuid:4ED2993C-4339-4E99-81FC-C2FD3812781A</a:MessageID> \n" +
            "    <a:To>http://127.0.0.1:8080/wsman</a:To> \n" +
            "    <a:ReplyTo> \n" +
            "      <a:Address s:mustUnderstand=\"true\">http://schemas.xmlsoap.org/ws/2004/08/addressing/role/anonymous</a:Address> \n" +
            "    </a:ReplyTo> \n" +
            "    <a:Action s:mustUnderstand=\"true\">http://schemas.xmlsoap.org/ws/2004/09/transfer/Get</a:Action> \n" +
            "    <w:ResourceURI s:mustUnderstand=\"true\">http://ns.l7tech.com/2010/04/gateway-management/customKeyValues</w:ResourceURI> \n" +
            "    <w:SelectorSet>\n" +
            "      <w:Selector Name=\"name\">key.prefix.key1</w:Selector> \n" +
            "    </w:SelectorSet>\n" +
            "    <w:OperationTimeout>PT60.000S</w:OperationTimeout> \n" +
            "  </s:Header>\n" +
            "  <s:Body/> \n" +
            "</s:Envelope>";

        final Document result = processRequest("http://schemas.xmlsoap.org/ws/2004/09/transfer/Get", message);
        final Element soapBody = SoapUtil.getBodyElement(result);
        final Element customKeyValueElm = XmlUtil.findExactlyOneChildElementByName(soapBody, NS_GATEWAY_MANAGEMENT, "CustomKeyValue");
        final Element storeNameElm = XmlUtil.findExactlyOneChildElementByName(customKeyValueElm, NS_GATEWAY_MANAGEMENT, "StoreName");
        final Element keyElm = XmlUtil.findExactlyOneChildElementByName(customKeyValueElm, NS_GATEWAY_MANAGEMENT, "Key");
        final Element valueElm = XmlUtil.findExactlyOneChildElementByName(customKeyValueElm, NS_GATEWAY_MANAGEMENT, "Value");

        assertEquals(KeyValueStoreServices.INTERNAL_TRANSACTIONAL_KEY_VALUE_STORE_NAME, DomUtils.getTextValue(storeNameElm));
        assertEquals("key.prefix.key1", DomUtils.getTextValue(keyElm));
        assertEquals("PHhtbD5UZXN0IHZhbHVlPC94bWw+", DomUtils.getTextValue(valueElm));
    }

    @Test
    public void testGetSiteminderByKey() throws Exception {
        final String idStr = Goid.toString(new Goid(0,1));
        final String message =
                "<s:Envelope xmlns:s=\"http://www.w3.org/2003/05/soap-envelope\" \n" +
                        "            xmlns:a=\"http://schemas.xmlsoap.org/ws/2004/08/addressing\" \n" +
                        "            xmlns:w=\"http://schemas.dmtf.org/wbem/wsman/1/wsman.xsd\">\n" +
                        "  <s:Header>\n" +
                        "    <a:MessageID>uuid:4ED2993C-4339-4E99-81FC-C2FD3812781A</a:MessageID> \n" +
                        "    <a:To>http://127.0.0.1:8080/wsman</a:To> \n" +
                        "    <a:ReplyTo> \n" +
                        "      <a:Address s:mustUnderstand=\"true\">http://schemas.xmlsoap.org/ws/2004/08/addressing/role/anonymous</a:Address> \n" +
                        "    </a:ReplyTo> \n" +
                        "    <a:Action s:mustUnderstand=\"true\">http://schemas.xmlsoap.org/ws/2004/09/transfer/Get</a:Action> \n" +
                        "    <w:ResourceURI s:mustUnderstand=\"true\">http://ns.l7tech.com/2010/04/gateway-management/siteMinderConfigurations</w:ResourceURI> \n" +
                        "    <w:SelectorSet>\n" +
                        "      <w:Selector Name=\"id\">"+idStr+"</w:Selector> \n" +
                        "    </w:SelectorSet>\n" +
                        "    <w:OperationTimeout>PT60.000S</w:OperationTimeout> \n" +
                        "  </s:Header>\n" +
                        "  <s:Body/> \n" +
                        "</s:Envelope>";

        final Document result = processRequest("http://schemas.xmlsoap.org/ws/2004/09/transfer/Get", message);
        final Element soapBody = SoapUtil.getBodyElement(result);
        final Element siteminderConfigurationElm = XmlUtil.findExactlyOneChildElementByName(soapBody, NS_GATEWAY_MANAGEMENT, "SiteMinderConfiguration");
        final Element nameElm = XmlUtil.findExactlyOneChildElementByName(siteminderConfigurationElm, NS_GATEWAY_MANAGEMENT, "Name");
        final Element addressElm = XmlUtil.findExactlyOneChildElementByName(siteminderConfigurationElm, NS_GATEWAY_MANAGEMENT, "Address");

        assertEquals("Config 1", DomUtils.getTextValue(nameElm));
    }

    @Test
    public void testGetAssertionSecurityZoneByKey() throws Exception {
        final String idStr = Goid.toString(new Goid(0,1));
        final String message =
                "<s:Envelope xmlns:s=\"http://www.w3.org/2003/05/soap-envelope\" \n" +
                        "            xmlns:a=\"http://schemas.xmlsoap.org/ws/2004/08/addressing\" \n" +
                        "            xmlns:w=\"http://schemas.dmtf.org/wbem/wsman/1/wsman.xsd\">\n" +
                        "  <s:Header>\n" +
                        "    <a:MessageID>uuid:4ED2993C-4339-4E99-81FC-C2FD3812781A</a:MessageID> \n" +
                        "    <a:To>http://127.0.0.1:8080/wsman</a:To> \n" +
                        "    <a:ReplyTo> \n" +
                        "      <a:Address s:mustUnderstand=\"true\">http://schemas.xmlsoap.org/ws/2004/08/addressing/role/anonymous</a:Address> \n" +
                        "    </a:ReplyTo> \n" +
                        "    <a:Action s:mustUnderstand=\"true\">http://schemas.xmlsoap.org/ws/2004/09/transfer/Get</a:Action> \n" +
                        "    <w:ResourceURI s:mustUnderstand=\"true\">http://ns.l7tech.com/2010/04/gateway-management/assertionSecurityZones</w:ResourceURI> \n" +
                        "    <w:SelectorSet>\n" +
                        "      <w:Selector Name=\"id\">"+idStr+"</w:Selector> \n" +
                        "    </w:SelectorSet>\n" +
                        "    <w:OperationTimeout>PT60.000S</w:OperationTimeout> \n" +
                        "  </s:Header>\n" +
                        "  <s:Body/> \n" +
                        "</s:Envelope>";

        final Document result = processRequest("http://schemas.xmlsoap.org/ws/2004/09/transfer/Get", message);
        final Element soapBody = SoapUtil.getBodyElement(result);
        final Element assSecurityZoneElm = XmlUtil.findExactlyOneChildElementByName(soapBody, NS_GATEWAY_MANAGEMENT, "AssertionSecurityZone");
        final Element nameElm = XmlUtil.findExactlyOneChildElementByName(assSecurityZoneElm, NS_GATEWAY_MANAGEMENT, "Name");

        assertEquals(JmsRoutingAssertion.class.getName(), DomUtils.getTextValue(nameElm));
        assertEquals(idStr, assSecurityZoneElm.getAttribute("securityZoneId"));
    }

    @Test
    public void testGetSecurityZoneByKey() throws Exception {
        final String idStr = Goid.toString(new Goid(0,1));
        final String message =
                "<s:Envelope xmlns:s=\"http://www.w3.org/2003/05/soap-envelope\" \n" +
                        "            xmlns:a=\"http://schemas.xmlsoap.org/ws/2004/08/addressing\" \n" +
                        "            xmlns:w=\"http://schemas.dmtf.org/wbem/wsman/1/wsman.xsd\">\n" +
                        "  <s:Header>\n" +
                        "    <a:MessageID>uuid:4ED2993C-4339-4E99-81FC-C2FD3812781A</a:MessageID> \n" +
                        "    <a:To>http://127.0.0.1:8080/wsman</a:To> \n" +
                        "    <a:ReplyTo> \n" +
                        "      <a:Address s:mustUnderstand=\"true\">http://schemas.xmlsoap.org/ws/2004/08/addressing/role/anonymous</a:Address> \n" +
                        "    </a:ReplyTo> \n" +
                        "    <a:Action s:mustUnderstand=\"true\">http://schemas.xmlsoap.org/ws/2004/09/transfer/Get</a:Action> \n" +
                        "    <w:ResourceURI s:mustUnderstand=\"true\">http://ns.l7tech.com/2010/04/gateway-management/securityZones</w:ResourceURI> \n" +
                        "    <w:SelectorSet>\n" +
                        "      <w:Selector Name=\"id\">"+idStr+"</w:Selector> \n" +
                        "    </w:SelectorSet>\n" +
                        "    <w:OperationTimeout>PT60.000S</w:OperationTimeout> \n" +
                        "  </s:Header>\n" +
                        "  <s:Body/> \n" +
                        "</s:Envelope>";

        final Document result = processRequest("http://schemas.xmlsoap.org/ws/2004/09/transfer/Get", message);
        final Element soapBody = SoapUtil.getBodyElement(result);
        final Element zoneElm = XmlUtil.findExactlyOneChildElementByName(soapBody, NS_GATEWAY_MANAGEMENT, "SecurityZone");
        final Element nameElm = XmlUtil.findExactlyOneChildElementByName(zoneElm, NS_GATEWAY_MANAGEMENT, "Name");

        assertEquals("Test Security Zone 0001", DomUtils.getTextValue(nameElm));
    }

    @Test
    public void testGetPolicyAlias() throws Exception {
        final String idStr = Goid.toString(new Goid(0,1));
        final String message =
                "<s:Envelope xmlns:s=\"http://www.w3.org/2003/05/soap-envelope\" \n" +
                        "            xmlns:a=\"http://schemas.xmlsoap.org/ws/2004/08/addressing\" \n" +
                        "            xmlns:w=\"http://schemas.dmtf.org/wbem/wsman/1/wsman.xsd\">\n" +
                        "  <s:Header>\n" +
                        "    <a:MessageID>uuid:4ED2993C-4339-4E99-81FC-C2FD3812781A</a:MessageID> \n" +
                        "    <a:To>http://127.0.0.1:8080/wsman</a:To> \n" +
                        "    <a:ReplyTo> \n" +
                        "      <a:Address s:mustUnderstand=\"true\">http://schemas.xmlsoap.org/ws/2004/08/addressing/role/anonymous</a:Address> \n" +
                        "    </a:ReplyTo> \n" +
                        "    <a:Action s:mustUnderstand=\"true\">http://schemas.xmlsoap.org/ws/2004/09/transfer/Get</a:Action> \n" +
                        "    <w:ResourceURI s:mustUnderstand=\"true\">http://ns.l7tech.com/2010/04/gateway-management/policyAliases</w:ResourceURI> \n" +
                        "    <w:SelectorSet>\n" +
                        "      <w:Selector Name=\"id\">"+idStr+"</w:Selector> \n" +
                        "    </w:SelectorSet>\n" +
                        "    <w:OperationTimeout>PT60.000S</w:OperationTimeout> \n" +
                        "  </s:Header>\n" +
                        "  <s:Body/> \n" +
                        "</s:Envelope>";

        final Document result = processRequest("http://schemas.xmlsoap.org/ws/2004/09/transfer/Get", message);
        final Element soapBody = SoapUtil.getBodyElement(result);
        final Element zoneElm = XmlUtil.findExactlyOneChildElementByName(soapBody, NS_GATEWAY_MANAGEMENT, "PolicyAlias");
        final Element policyRefElm = XmlUtil.findExactlyOneChildElementByName(zoneElm, NS_GATEWAY_MANAGEMENT, "PolicyReference");

        assertEquals("http://ns.l7tech.com/2010/04/gateway-management/policies", policyRefElm.getAttribute("resourceUri"));
    }
    @Test
    public void testGetServiceAlias() throws Exception {
        final String idStr = Goid.toString(new Goid(0,1));
        final String message =
                "<s:Envelope xmlns:s=\"http://www.w3.org/2003/05/soap-envelope\" \n" +
                        "            xmlns:a=\"http://schemas.xmlsoap.org/ws/2004/08/addressing\" \n" +
                        "            xmlns:w=\"http://schemas.dmtf.org/wbem/wsman/1/wsman.xsd\">\n" +
                        "  <s:Header>\n" +
                        "    <a:MessageID>uuid:4ED2993C-4339-4E99-81FC-C2FD3812781A</a:MessageID> \n" +
                        "    <a:To>http://127.0.0.1:8080/wsman</a:To> \n" +
                        "    <a:ReplyTo> \n" +
                        "      <a:Address s:mustUnderstand=\"true\">http://schemas.xmlsoap.org/ws/2004/08/addressing/role/anonymous</a:Address> \n" +
                        "    </a:ReplyTo> \n" +
                        "    <a:Action s:mustUnderstand=\"true\">http://schemas.xmlsoap.org/ws/2004/09/transfer/Get</a:Action> \n" +
                        "    <w:ResourceURI s:mustUnderstand=\"true\">http://ns.l7tech.com/2010/04/gateway-management/serviceAliases</w:ResourceURI> \n" +
                        "    <w:SelectorSet>\n" +
                        "      <w:Selector Name=\"id\">"+idStr+"</w:Selector> \n" +
                        "    </w:SelectorSet>\n" +
                        "    <w:OperationTimeout>PT60.000S</w:OperationTimeout> \n" +
                        "  </s:Header>\n" +
                        "  <s:Body/> \n" +
                        "</s:Envelope>";

        final Document result = processRequest("http://schemas.xmlsoap.org/ws/2004/09/transfer/Get", message);
        final Element soapBody = SoapUtil.getBodyElement(result);
        final Element zoneElm = XmlUtil.findExactlyOneChildElementByName(soapBody, NS_GATEWAY_MANAGEMENT, "ServiceAlias");
        final Element serviceRefElm = XmlUtil.findExactlyOneChildElementByName(zoneElm, NS_GATEWAY_MANAGEMENT, "ServiceReference");

        assertEquals("http://ns.l7tech.com/2010/04/gateway-management/services", serviceRefElm.getAttribute("resourceUri"));
    }

    @Test
    public void testCreateEncapsulatedAssertion() throws Exception {
        String resourceUri = "http://ns.l7tech.com/2010/04/gateway-management/encapsulatedAssertions";
        String payload =
            "<l7:EncapsulatedAssertion xmlns:l7=\"http://ns.l7tech.com/2010/04/gateway-management\">\n" +
            "    <l7:Name>Test</l7:Name>\n" +
            "    <l7:PolicyReference id=\""+new Goid(0,2)+"\" resourceUri=\"http://ns.l7tech.com/2010/04/gateway-management/policies\"/>\n" +
            "    <l7:EncapsulatedArguments>\n" +
            "        <l7:EncapsulatedAssertionArgument>\n" +
            "            <l7:Ordinal>1</l7:Ordinal>\n" +
            "            <l7:ArgumentName>input1</l7:ArgumentName>\n" +
            "            <l7:ArgumentType>decimal</l7:ArgumentType>\n" +
            "            <l7:GuiLabel>Input1 Label</l7:GuiLabel>\n" +
            "            <l7:GuiPrompt>true</l7:GuiPrompt>\n" +
            "        </l7:EncapsulatedAssertionArgument>\n" +
            "        <l7:EncapsulatedAssertionArgument>\n" +
            "            <l7:Ordinal>2</l7:Ordinal>\n" +
            "            <l7:ArgumentName>input2</l7:ArgumentName>\n" +
            "            <l7:ArgumentType>string</l7:ArgumentType>\n" +
            "            <l7:GuiLabel>Input2 Label</l7:GuiLabel>\n" +
            "            <l7:GuiPrompt>false</l7:GuiPrompt>\n" +
            "        </l7:EncapsulatedAssertionArgument>\n" +
            "    </l7:EncapsulatedArguments>\n" +
            "    <l7:EncapsulatedResults>\n" +
            "        <l7:EncapsulatedAssertionResult>\n" +
            "            <l7:ResultName>result1</l7:ResultName>\n" +
            "            <l7:ResultType>boolean</l7:ResultType>\n" +
            "        </l7:EncapsulatedAssertionResult>\n" +
            "        <l7:EncapsulatedAssertionResult>\n" +
            "            <l7:ResultName>result2</l7:ResultName>\n" +
            "            <l7:ResultType>string</l7:ResultType>\n" +
            "        </l7:EncapsulatedAssertionResult>\n" +
            "        <l7:EncapsulatedAssertionResult>\n" +
            "            <l7:ResultName>result3</l7:ResultName>\n" +
            "            <l7:ResultType>message</l7:ResultType>\n" +
            "        </l7:EncapsulatedAssertionResult>\n" +
            "    </l7:EncapsulatedResults>\n" +
            "    <l7:Properties>\n" +
            "        <l7:Property key=\"a\">\n" +
            "            <l7:StringValue>b</l7:StringValue>\n" +
            "        </l7:Property>\n" +
            "    </l7:Properties>\n" +
            "</l7:EncapsulatedAssertion>";

        String expectedId = new Goid(0,2).toString();
        doCreate( resourceUri, payload, expectedId );
    }

    @Test
    public void testCreateCertificate() throws Exception {
        String resourceUri = "http://ns.l7tech.com/2010/04/gateway-management/trustedCertificates";
        String payload =
                "<TrustedCertificate xmlns=\"http://ns.l7tech.com/2010/04/gateway-management\">\n" +
                "    <Name>OASIS Interop Test Root</Name>\n" +
                "    <CertificateData>\n" +
                "        <Encoded>\n" +
                "MIIDizCCAnOgAwIBAgIQWaCxRe3INcSU8VNJ4/HerDANBgkqhkiG9w0BAQUFADAy\n" +
                "MQ4wDAYDVQQKDAVPQVNJUzEgMB4GA1UEAwwXT0FTSVMgSW50ZXJvcCBUZXN0IFJv\n" +
                "b3QwHhcNMDUwMzE5MDAwMDAwWhcNMTkwMzE5MjM1OTU5WjAwMQ4wDAYDVQQKDAVP\n" +
                "QVNJUzEeMBwGA1UEAwwVT0FTSVMgSW50ZXJvcCBUZXN0IENBMIIBIjANBgkqhkiG\n" +
                "9w0BAQEFAAOCAQ8AMIIBCgKCAQEAmR2GR3IduCfoZfvmwYpepKNZN6iaDcm4Jmqq\n" +
                "C3nN5NiuQ4ROq2YCRhG90QW8puhsO6XaRiRO6WQQpwdtm/tgseDAAdw0bMPWrnja\n" +
                "FhgFlaEB0eK5fu9UiCPGkwurWNc8EQlk2r71uCwOx6BYGFsnSnBEfj64zoVri2ol\n" +
                "ksXc2aos6urhujP6zvixsCxfo8Jq2v1yLUZpDaiTp2GfyDMSZKROcBz4FnEIN7yK\n" +
                "ZDMYpHSx2SmcwmQnjeeAx1EH876+PpycsbJwStt3lIYchk5vWqJSZzN7PElEgzLW\n" +
                "v8QeWZ0Zb8wteQyWrG5wN2FCTcqF3W29FBeZig6u5Y3mibwDYQIDAQABo4GeMIGb\n" +
                "MBIGA1UdEwEB/wQIMAYBAf8CAQAwNQYDVR0fBC4wLDAqoiiGJmh0dHA6Ly9pbnRl\n" +
                "cm9wLmJidGVzdC5uZXQvY3JsL3Jvb3QuY3JsMA4GA1UdDwEB/wQEAwIBBjAdBgNV\n" +
                "HQ4EFgQUwJ0o/MHrNaEd1qqqoBwaTcJJDw8wHwYDVR0jBBgwFoAU3/6RlcdWSCY9\n" +
                "wNw5PcYJ90z6SOIwDQYJKoZIhvcNAQEFBQADggEBADvsOGOmhnxjwW2+2c17/W7o\n" +
                "4BolmqqlVFppFyEB4pUd+kqJ3XFiyVxweVwGdJfpUQLKP/KBzpqo4D11ttMaE2io\n" +
                "at0RUGylAl9PG/yalOH/vMgFq4XkhokoHPPD1tUbiuY8+pD+5jXR0NNj25yv7iSu\n" +
                "tZ7xA7bcMx+RQpDO9Mzhlk03SZt5FjsLrimLiEOtkTkBt8Gw1wCu253+Bt5JHboB\n" +
                "hgEa9hTmdQ3hYqO/q54Gymmd/NsNCxZDbUxVqu/XzBxZer6AQ4domv5fc9efCOk0\n" +
                "k06aMmYjKXEYI5i9OqutWu442ZXJV6lnWKZ1akFi/sA4DNnYPrz825+hzOeesBI=</Encoded>\n" +
                "    </CertificateData>\n" +
                "    <Properties>\n" +
                "        <Property key=\"trustAnchor\">\n" +
                "            <BooleanValue>true</BooleanValue>\n" +
                "        </Property>\n" +
                "    </Properties>\n" +
                "</TrustedCertificate>";
        String expectedId = new Goid(0,3).toHexString();
        doCreate( resourceUri, payload, expectedId );
    }

    @Test
    public void testCreateClusterProperty() throws Exception {
        String resourceUri = "http://ns.l7tech.com/2010/04/gateway-management/clusterProperties";
        String payload = "<n1:ClusterProperty xmlns:n1=\"http://ns.l7tech.com/2010/04/gateway-management\"><n1:Name>test</n1:Name><n1:Value>value</n1:Value></n1:ClusterProperty>";
        String expectedId = new Goid(0,6).toHexString();
        doCreate( resourceUri, payload, expectedId );
    }

    @BugId("SSG-5572")
    @Test
    public void testCreateGenericEntity() throws Exception {
        String resourceUri = "http://ns.l7tech.com/2010/04/gateway-management/genericEntities";
        String payload = "<l7:GenericEntity xmlns:l7=\"http://ns.l7tech.com/2010/04/gateway-management\"><l7:Name>My Test Entity</l7:Name><l7:Description>My test entity description</l7:Description><l7:EntityClassName>com.l7tech.external.assertions.gatewaymanagement.server.ServerGatewayManagementAssertionTest</l7:EntityClassName><l7:Enabled>true</l7:Enabled><l7:ValueXml>&lt;xml&gt;xml value&lt;/xml&gt;</l7:ValueXml></l7:GenericEntity>";
        // id will be 1 or 2 depending on when the test is ran
        doCreate( resourceUri, payload, new Goid(0,1).toHexString(), new Goid(0,2).toHexString() );
    }

    @BugId("SSG-6839")
    @Test
    public void testCreateGenericEntityUnregisteredClass() throws Exception {
        String resourceUri = "http://ns.l7tech.com/2010/04/gateway-management/genericEntities";
        String payload = "<l7:GenericEntity xmlns:l7=\"http://ns.l7tech.com/2010/04/gateway-management\"><l7:Name>My Test Entity</l7:Name><l7:Description>My test entity description</l7:Description><l7:EntityClassName>com.l7tech.external.assertions.gatewaymanagement.server.OtherClass</l7:EntityClassName><l7:Enabled>true</l7:Enabled><l7:ValueXml>&lt;xml&gt;xml value&lt;/xml&gt;</l7:ValueXml></l7:GenericEntity>";

        doCreateFail( resourceUri, payload,"wsman:InternalError");
    }

    @Test
    public void testCreateFolder() throws Exception {
        String resourceUri = "http://ns.l7tech.com/2010/04/gateway-management/folders";
        String payload = "<n1:Folder xmlns:n1=\"http://ns.l7tech.com/2010/04/gateway-management\" folderId=\""+new Goid(0,1)+"\"><n1:Name>test</n1:Name></n1:Folder>";
        doCreate( resourceUri, payload, new Goid(0,4).toHexString() );
    }

    @Test
    public void testCreateFolderInOldRoot() throws Exception {
        String resourceUri = "http://ns.l7tech.com/2010/04/gateway-management/folders";
        String payload = "<n1:Folder xmlns:n1=\"http://ns.l7tech.com/2010/04/gateway-management\" folderId=\"-5002\"><n1:Name>test</n1:Name></n1:Folder>";
        doCreate( resourceUri, payload, new Goid(0,4).toHexString() );
    }

    @Test
    public void testCreateService() throws Exception {
        String resourceUri = "http://ns.l7tech.com/2010/04/gateway-management/services";
        String payload = "<Service xmlns=\"http://ns.l7tech.com/2010/04/gateway-management\"><ServiceDetail folderId=\""+new Goid(0,1)+"\"><Name>Warehouse Service</Name><Enabled>true</Enabled><ServiceMappings><HttpMapping><UrlPattern>/waremulti</UrlPattern><Verbs><Verb>POST</Verb></Verbs></HttpMapping></ServiceMappings><Properties><Property key=\"soap\"><BooleanValue>false</BooleanValue></Property></Properties></ServiceDetail><Resources><ResourceSet tag=\"policy\"><Resource type=\"policy\">&lt;?xml version=&quot;1.0&quot; encoding=&quot;UTF-8&quot;?&gt;\n" +
                "&lt;wsp:Policy xmlns:L7p=&quot;http://www.layer7tech.com/ws/policy&quot; xmlns:wsp=&quot;http://schemas.xmlsoap.org/ws/2002/12/policy&quot;&gt;\n" +
                "    &lt;wsp:All wsp:Usage=&quot;Required&quot;&gt;\n" +
                "        &lt;L7p:EchoRoutingAssertion/&gt;\n" +
                "    &lt;/wsp:All&gt;\n" +
                "&lt;/wsp:Policy&gt;\n" +
                "</Resource></ResourceSet></Resources></Service>";
        String expectedId = new Goid(0,3).toHexString();
        doCreate( resourceUri, payload, expectedId );
    }

    @Test
    public void testCreateFederatedIdentityProvider() throws Exception {
        String resourceUri = "http://ns.l7tech.com/2010/04/gateway-management/identityProviders";
        String payload = "<IdentityProvider xmlns=\"http://ns.l7tech.com/2010/04/gateway-management\" extendedAttribute=\"extension\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\">\n" +
                "    <Name other:extendedAttribute=\"extension\" xmlns:other=\"urn:othernamespace\">QAAD4</Name>\n" +
                "    <IdentityProviderType other:extendedAttribute=\"extension\" xmlns:other=\"urn:othernamespace\">Federated</IdentityProviderType>\n" +
                "    <Properties extendedAttribute=\"extension\">\n" +
                "        <Property key=\"propertiesAreNotCurrentUsed\" extendedAttribute=\"extension\">\n" +
                "            <BooleanValue>true</BooleanValue>\n" +
                "        </Property>\n" +
                "    </Properties>\n" +
                "    <Extension other:extendedAttribute=\"extension\" xmlns:other=\"urn:othernamespace\">\n" +
                "        <FederatedIdentityProviderDetail other:extendedAttribute=\"extension\" xmlns:other=\"urn:othernamespace\">\n" +
                "            <CertificateReferences other:extendedAttribute=\"extension\" xmlns:other=\"urn:othernamespace\">\n" +
                "                <Reference resourceUri=\"http://ns.l7tech.com/2010/04/gateway-management/privateKeys\" id=\"1\" other:extendedAttribute=\"extension\" xmlns:other=\"urn:othernamespace\"/>\n" +
                "            </CertificateReferences>\n" +
                "        </FederatedIdentityProviderDetail>\n" +
                "    </Extension>\n" +
                "</IdentityProvider>";
        String[] expectedIds = new String[]{Goid.toString(new Goid(0,1)),Goid.toString(new Goid(0,2)),Goid.toString(new Goid(0,3)),Goid.toString(new Goid(0,4))};
        doCreate( resourceUri, payload, expectedIds );
    }

    @Test
    public void testCreateBindOnlyLDAPIdentityProvider() throws Exception {
        String resourceUri = "http://ns.l7tech.com/2010/04/gateway-management/identityProviders";
        String payload = "<IdentityProvider xmlns=\"http://ns.l7tech.com/2010/04/gateway-management\" extendedAttribute=\"extension\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\">\n" +
                "    <Name other:extendedAttribute=\"extension\" xmlns:other=\"urn:othernamespace\">Simple Bind LDAP</Name>\n" +
                "    <IdentityProviderType other:extendedAttribute=\"extension\" xmlns:other=\"urn:othernamespace\">Simple LDAP</IdentityProviderType>\n" +
                "    <Extension other:extendedAttribute=\"extension\" xmlns:other=\"urn:othernamespace\">\n" +
                "        <BindOnlyLdapIdentityProviderDetail other:extendedAttribute=\"extension\" xmlns:other=\"urn:othernamespace\">\n" +
                "            <ServerUrls><StringValue>ldap://10.7.2.1</StringValue></ServerUrls>\n" +
                "            <BindPatternPrefix>CN=</BindPatternPrefix>\n" +
                "            <BindPatternSuffix>,DC=test,DC=com</BindPatternSuffix>\n" +
                "        </BindOnlyLdapIdentityProviderDetail>\n" +
                "    </Extension>\n" +
                "</IdentityProvider>";
        String[] expectedIds = new String[]{Goid.toString(new Goid(0,1)),Goid.toString(new Goid(0,2)),Goid.toString(new Goid(0,3)),Goid.toString(new Goid(0,4))};
        doCreate( resourceUri, payload, expectedIds );
    }

    @Test
    public void testCreateLDAPIdentityProviderFromTemplate() throws Exception {
        String resourceUri = "http://ns.l7tech.com/2010/04/gateway-management/identityProviders";
        String payload = "<IdentityProvider xmlns=\"http://ns.l7tech.com/2010/04/gateway-management\" extendedAttribute=\"extension\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\">\n" +
                "    <Name other:extendedAttribute=\"extension\" xmlns:other=\"urn:othernamespace\">LDAP1</Name>\n" +
                "    <IdentityProviderType other:extendedAttribute=\"extension\" xmlns:other=\"urn:othernamespace\">LDAP</IdentityProviderType>\n" +
                "    <Properties extendedAttribute=\"extension\">\n" +
                "        <Property key=\"userCertificateUsage\" extendedAttribute=\"extension\">\n" +
                "            <StringValue>Index</StringValue>\n" +
                "        </Property>\n" +
                "        <Property key=\"userLookupByCertMode\" extendedAttribute=\"extension\">\n" +
                "            <StringValue>Entire Certificate</StringValue>\n" +
                "        </Property>\n" +
                "    </Properties>\n" +
                "    <Extension other:extendedAttribute=\"extension\" xmlns:other=\"urn:othernamespace\">\n" +
                "        <LdapIdentityProviderDetail other:extendedAttribute=\"extension\" xmlns:other=\"urn:othernamespace\">\n" +
                "            <SourceType>MicrosoftActiveDirectory</SourceType>\n" +
                "            <ServerUrls><StringValue>ldap://10.7.2.1</StringValue></ServerUrls>\n" +
                "            <SearchBase>cn=something</SearchBase>\n" +
                "            <BindDn>browse</BindDn>\n" +
                "            <BindPassword>password</BindPassword>\n" +
                "        </LdapIdentityProviderDetail>\n" +
                "    </Extension>\n" +
                "</IdentityProvider>";
        String[] expectedIds = new String[]{Goid.toString(new Goid(0,1)),Goid.toString(new Goid(0,2)),Goid.toString(new Goid(0,3)),Goid.toString(new Goid(0,4))};
        doCreate( resourceUri, payload, expectedIds );
    }

    @BugNumber(10920)
    @Test
    public void testCreateLdapIdentityProviderFull() throws Exception {
        String resourceUri = "http://ns.l7tech.com/2010/04/gateway-management/identityProviders";
        String payload = "<IdentityProvider xmlns=\"http://ns.l7tech.com/2010/04/gateway-management\">\n" +
                "    <Name>LDAP Example</Name>\n" +
                "    <IdentityProviderType>LDAP</IdentityProviderType>\n" +
                "    <Properties>\n" +
                "        <Property key=\"groupCacheMaximumAge\">\n" +
                "            <LongValue>60000</LongValue>\n" +
                "        </Property>\n" +
                "        <Property key=\"groupMembershipCaseInsensitive\">\n" +
                "            <BooleanValue>false</BooleanValue>\n" +
                "        </Property>\n" +
                "        <Property key=\"groupMaximumNesting\">\n" +
                "            <IntegerValue>0</IntegerValue>\n" +
                "        </Property>\n" +
                "        <Property key=\"certificateValidation\">\n" +
                "            <StringValue>Revocation Checking</StringValue>\n" +
                "        </Property>\n" +
                "        <Property key=\"groupCacheSize\">\n" +
                "            <IntegerValue>100</IntegerValue>\n" +
                "        </Property>\n" +
                "        <Property key=\"userCertificateUsage\">\n" +
                "            <StringValue>None</StringValue>\n" +
                "        </Property>\n" +
                "        <Property key=\"adminEnabled\">\n" +
                "            <BooleanValue>true</BooleanValue>\n" +
                "        </Property>\n" +
                "    </Properties>\n" +
                "    <Extension>\n" +
                "        <LdapIdentityProviderDetail>\n" +
                "            <SourceType>MicrosoftActiveDirectory</SourceType>\n" +
                "            <ServerUrls>\n" +
                "                <StringValue>ldap://example.layer7tech.com</StringValue>\n" +
                "            </ServerUrls>\n" +
                "            <UseSslClientAuthentication>false</UseSslClientAuthentication>\n" +
                "            <SearchBase>ou= Users,DC=layer7tech,dc=com</SearchBase>\n" +
                "            <BindDn>browse</BindDn>\n" +
                "            <UserMappings>\n" +
                "                <Mapping>\n" +
                "                    <ObjectClass>user</ObjectClass>\n" +
                "                    <Mappings>\n" +
                "                        <Property key=\"kerberosEnterpriseAttrName\">\n" +
                "                            <StringValue>userPrincipalName</StringValue>\n" +
                "                        </Property>\n" +
                "                        <Property key=\"userCertAttrName\">\n" +
                "                            <StringValue>userCertificate</StringValue>\n" +
                "                        </Property>\n" +
                "                        <Property key=\"loginAttrName\">\n" +
                "                            <StringValue>sAMAccountName</StringValue>\n" +
                "                        </Property>\n" +
                "                        <Property key=\"nameAttrName\">\n" +
                "                            <StringValue>cn</StringValue>\n" +
                "                        </Property>\n" +
                "                        <Property key=\"passwdAttrName\">\n" +
                "                            <StringValue>userPassword</StringValue>\n" +
                "                        </Property>\n" +
                "                        <Property key=\"kerberosAttrName\">\n" +
                "                            <StringValue>sAMAccountName</StringValue>\n" +
                "                        </Property>\n" +
                "                        <Property key=\"emailNameAttrName\">\n" +
                "                            <StringValue>mail</StringValue>\n" +
                "                        </Property>\n" +
                "                        <Property key=\"firstNameAttrName\">\n" +
                "                            <StringValue>givenName</StringValue>\n" +
                "                        </Property>\n" +
                "                        <Property key=\"objClass\">\n" +
                "                            <StringValue>user</StringValue>\n" +
                "                        </Property>\n" +
                "                        <Property key=\"lastNameAttrName\">\n" +
                "                            <StringValue>sn</StringValue>\n" +
                "                        </Property>\n" +
                "                    </Mappings>\n" +
                "                </Mapping>\n" +
                "            </UserMappings>\n" +
                "            <GroupMappings>\n" +
                "                <Mapping>\n" +
                "                    <ObjectClass>group</ObjectClass>\n" +
                "                    <Mappings>\n" +
                "                        <Property key=\"nameAttrName\">\n" +
                "                            <StringValue>cn</StringValue>\n" +
                "                        </Property>\n" +
                "                        <Property key=\"memberAttrName\">\n" +
                "                            <StringValue>member</StringValue>\n" +
                "                        </Property>\n" +
                "                        <Property key=\"objClass\">\n" +
                "                            <StringValue>group</StringValue>\n" +
                "                        </Property>\n" +
                "                    </Mappings>\n" +
                "                    <Properties>\n" +
                "                        <Property key=\"memberStrategy\">\n" +
                "                            <StringValue>Member is User DN</StringValue>\n" +
                "                        </Property>\n" +
                "                    </Properties>\n" +
                "                </Mapping>\n" +
                "            </GroupMappings>\n" +
                "            <SpecifiedAttributes/>\n" +
                "        </LdapIdentityProviderDetail>\n" +
                "    </Extension>\n" +
                "</IdentityProvider>";
        String[] expectedIds = new String[]{Goid.toString(new Goid(0,1)),Goid.toString(new Goid(0,2)),Goid.toString(new Goid(0,3)),Goid.toString(new Goid(0,4))};
        doCreate( resourceUri, payload, expectedIds );
    }

    @Test
    public void testCreateActiveConnector() throws Exception {
        String resourceUri = "http://ns.l7tech.com/2010/04/gateway-management/activeConnectors";
        String payload =
                "<l7:ActiveConnector xmlns:l7=\"http://ns.l7tech.com/2010/04/gateway-management\">\n" +
                "    <l7:Name>test</l7:Name>\n" +
                "    <l7:Enabled>true</l7:Enabled>\n" +
                "    <l7:Type>SFTP</l7:Type>\n" +
                "    <l7:HardwiredId>"+new Goid(0,163840)+"</l7:HardwiredId>\n" +
                "    <l7:Properties>\n" +
                "        <l7:Property key=\"enableResponseMessages\">\n" +
                "            <l7:StringValue>true</l7:StringValue>\n" +
                "        </l7:Property>\n" +
                "        <l7:Property key=\"SftpHost\">\n" +
                "            <l7:StringValue>centospp.l7tech.com</l7:StringValue>\n" +
                "        </l7:Property>\n" +
                "        <l7:Property key=\"SftpPort\">\n" +
                "            <l7:StringValue>22</l7:StringValue>\n" +
                "        </l7:Property>\n" +
                "        <l7:Property key=\"overrideContentType\">\n" +
                "            <l7:StringValue>text/xml; charset=utf-8</l7:StringValue>\n" +
                "        </l7:Property>\n" +
                "        <l7:Property key=\"pollingInterval\">\n" +
                "            <l7:StringValue>10</l7:StringValue>\n" +
                "        </l7:Property>\n" +
                "        <l7:Property key=\"SftpDeleteOnReceive\">\n" +
                "            <l7:StringValue>false</l7:StringValue>\n" +
                "        </l7:Property>\n" +
                "        <l7:Property key=\"SftpSecurePasswordOid\">\n" +
                "            <l7:StringValue>360448</l7:StringValue>\n" +
                "        </l7:Property>\n" +
                "        <l7:Property key=\"SftpUsername\">\n" +
                "            <l7:StringValue>fish</l7:StringValue>\n" +
                "        </l7:Property>\n" +
                "        <l7:Property key=\"SftpDirectory\">\n" +
                "            <l7:StringValue>/home/fish/messages</l7:StringValue>\n" +
                "        </l7:Property>\n" +
                "    </l7:Properties>\n" +
                "</l7:ActiveConnector>";
        doCreate( resourceUri, payload, Goid.toString(new Goid(0,4)) );
    }

    @BugId("SSG-7422")
    @Test
    public void testCreateActiveConnectorBadHardwiredId() throws Exception {
        String resourceUri = "http://ns.l7tech.com/2010/04/gateway-management/activeConnectors";
        String payload =
                "<l7:ActiveConnector xmlns:l7=\"http://ns.l7tech.com/2010/04/gateway-management\">\n" +
                        "    <l7:Name>test</l7:Name>\n" +
                        "    <l7:Enabled>true</l7:Enabled>\n" +
                        "    <l7:Type>SFTP</l7:Type>\n" +
                        "    <l7:HardwiredId>hardwiredId</l7:HardwiredId>\n" +
                        "    <l7:Properties>\n" +
                        "        <l7:Property key=\"enableResponseMessages\">\n" +
                        "            <l7:StringValue>true</l7:StringValue>\n" +
                        "        </l7:Property>\n" +
                        "        <l7:Property key=\"SftpHost\">\n" +
                        "            <l7:StringValue>centospp.l7tech.com</l7:StringValue>\n" +
                        "        </l7:Property>\n" +
                        "        <l7:Property key=\"SftpPort\">\n" +
                        "            <l7:StringValue>22</l7:StringValue>\n" +
                        "        </l7:Property>\n" +
                        "        <l7:Property key=\"overrideContentType\">\n" +
                        "            <l7:StringValue>text/xml; charset=utf-8</l7:StringValue>\n" +
                        "        </l7:Property>\n" +
                        "        <l7:Property key=\"pollingInterval\">\n" +
                        "            <l7:StringValue>10</l7:StringValue>\n" +
                        "        </l7:Property>\n" +
                        "        <l7:Property key=\"SftpDeleteOnReceive\">\n" +
                        "            <l7:StringValue>false</l7:StringValue>\n" +
                        "        </l7:Property>\n" +
                        "        <l7:Property key=\"SftpSecurePasswordOid\">\n" +
                        "            <l7:StringValue>360448</l7:StringValue>\n" +
                        "        </l7:Property>\n" +
                        "        <l7:Property key=\"SftpUsername\">\n" +
                        "            <l7:StringValue>fish</l7:StringValue>\n" +
                        "        </l7:Property>\n" +
                        "        <l7:Property key=\"SftpDirectory\">\n" +
                        "            <l7:StringValue>/home/fish/messages</l7:StringValue>\n" +
                        "        </l7:Property>\n" +
                        "    </l7:Properties>\n" +
                        "</l7:ActiveConnector>";
        doCreateFail( resourceUri, payload, "wxf:InvalidRepresentation" );
    }

    @Test
    public void testCreateActiveConnectorBadType() throws Exception {
        String resourceUri = "http://ns.l7tech.com/2010/04/gateway-management/activeConnectors";
        String payload =
                "<l7:ActiveConnector xmlns:l7=\"http://ns.l7tech.com/2010/04/gateway-management\">\n" +
                        "    <l7:ActiveConnectorType>TYPE</l7:ActiveConnectorType>\n" +
                        "    <l7:Enabled>true</l7:Enabled>\n" +
                        "    <l7:HardwiredId>163840</l7:HardwiredId>\n" +
                        "    <l7:Name>test</l7:Name>\n" +
                        "    <l7:Properties>\n" +
                        "        <l7:Property key=\"enableResponseMessages\">\n" +
                        "            <l7:StringValue>true</l7:StringValue>\n" +
                        "        </l7:Property>\n" +
                        "        <l7:Property key=\"SftpHost\">\n" +
                        "            <l7:StringValue>centospp.l7tech.com</l7:StringValue>\n" +
                        "        </l7:Property>\n" +
                        "        <l7:Property key=\"SftpPort\">\n" +
                        "            <l7:StringValue>22</l7:StringValue>\n" +
                        "        </l7:Property>\n" +
                        "        <l7:Property key=\"overrideContentType\">\n" +
                        "            <l7:StringValue>text/xml; charset=utf-8</l7:StringValue>\n" +
                        "        </l7:Property>\n" +
                        "        <l7:Property key=\"pollingInterval\">\n" +
                        "            <l7:StringValue>10</l7:StringValue>\n" +
                        "        </l7:Property>\n" +
                        "        <l7:Property key=\"SftpDeleteOnReceive\">\n" +
                        "            <l7:StringValue>false</l7:StringValue>\n" +
                        "        </l7:Property>\n" +
                        "        <l7:Property key=\"SftpSecurePasswordOid\">\n" +
                        "            <l7:StringValue>360448</l7:StringValue>\n" +
                        "        </l7:Property>\n" +
                        "        <l7:Property key=\"SftpUsername\">\n" +
                        "            <l7:StringValue>fish</l7:StringValue>\n" +
                        "        </l7:Property>\n" +
                        "        <l7:Property key=\"SftpDirectory\">\n" +
                        "            <l7:StringValue>/home/fish/messages</l7:StringValue>\n" +
                        "        </l7:Property>\n" +
                        "    </l7:Properties>\n" +
                        "</l7:ActiveConnector>";
        doCreateFail( resourceUri, payload, "wsman:SchemaValidationError" );
    }

    @Test
    public void testCreateInterfaceTag() throws Exception {
        String resourceUri = "http://ns.l7tech.com/2010/04/gateway-management/interfaceTags";
        String payload =
                "<InterfaceTag xmlns=\"http://ns.l7tech.com/2010/04/gateway-management\">\n" +
                "    <Name>test</Name>\n" +
                "    <AddressPatterns>\n" +
                "        <StringValue>192.168</StringValue>\n" +
                "        <StringValue>10.0.0.0/8</StringValue>\n" +
                "        <StringValue>127</StringValue>\n" +
                "    </AddressPatterns>\n" +
                "</InterfaceTag>";
        doCreate( resourceUri, payload, "098f6bcd-4621-3373-8ade-4e832627b4f6" );
    }

    @Test
    public void testCreateJDBCConnection() throws Exception {
        String resourceUri = "http://ns.l7tech.com/2010/04/gateway-management/jdbcConnections";
        String payload =
                "<JDBCConnection xmlns=\"http://ns.l7tech.com/2010/04/gateway-management\">\n" +
                "    <Name>Another Test Connection</Name>\n" +
                "    <Enabled>true</Enabled>\n" +
                "    <Extension>\n" +
                "        <DriverClass>com.mysql.jdbc.Driver</DriverClass>\n" +
                "        <JdbcUrl>jdbc:mysql://localhost:3306/ssg</JdbcUrl>\n" +
                "        <ConnectionProperties>\n" +
                "            <Property key=\"user\">\n" +
                "                <StringValue>username</StringValue>\n" +
                "            </Property>\n" +
                "            <Property key=\"password\">\n" +
                "                <StringValue>password</StringValue>\n" +
                "            </Property>\n" +
                "        </ConnectionProperties>\n" +
                "    </Extension>\n" +
                "</JDBCConnection>";
        doCreate( resourceUri, payload, new Goid(0,2).toString(), new Goid(0,3).toString(), new Goid(0,4).toString());
    }

    @Test
    public void testCreateJMSDestination() throws Exception {
        String resourceUri = "http://ns.l7tech.com/2010/04/gateway-management/jmsDestinations";
        String payload =
                "<JMSDestination xmlns=\"http://ns.l7tech.com/2010/04/gateway-management\">\n" +
                "    <JMSDestinationDetail version=\"0\" id=\"48037888\">\n" +
                "        <Name>QueueName</Name>\n" +
                "        <DestinationName>QueueName</DestinationName>\n" +
                "        <Inbound>false</Inbound>\n" +
                "        <Enabled>true</Enabled>\n" +
                "        <Properties>\n"+
                "            <Property key=\"type\">\n" +
                "                <StringValue>Topic</StringValue>\n" +
                "            </Property>\n" +
                "        </Properties>\n"+
                "    </JMSDestinationDetail>\n" +
                "    <JMSConnection>\n" +
                "        <Properties>\n" +
                "            <Property key=\"jndi.initialContextFactoryClassname\">\n" +
                "                <StringValue>com.sun.jndi.ldap.LdapCtxFactory</StringValue>\n" +
                "            </Property>\n" +
                "            <Property key=\"jndi.providerUrl\">\n" +
                "                <StringValue>ldap://127.0.0.1/</StringValue>\n" +
                "            </Property>\n" +
                "            <Property key=\"queue.connectionFactoryName\">\n" +
                "                <StringValue>cn=QueueConnectionFactory</StringValue>\n" +
                "            </Property>\n" +
                "        </Properties>\n" +
                "    </JMSConnection>\n" +
                "</JMSDestination>";
        doCreate( resourceUri, payload, new Goid(0,2).toString(), new Goid(0,3).toString(), new Goid(0,4).toString() );
    }

    @Test
    public void testCreateJMSDestinationNoDetailProperties() throws Exception {
        String resourceUri = "http://ns.l7tech.com/2010/04/gateway-management/jmsDestinations";
        String payload =
                "<JMSDestination xmlns=\"http://ns.l7tech.com/2010/04/gateway-management\">\n" +
                "    <JMSDestinationDetail version=\"0\" id=\"48037888\">\n" +
                "        <Name>QueueName</Name>\n" +
                "        <DestinationName>QueueName</DestinationName>\n" +
                "        <Inbound>false</Inbound>\n" +
                "        <Enabled>true</Enabled>\n" +
                "    </JMSDestinationDetail>\n" +
                "    <JMSConnection>\n" +
                "        <Properties>\n" +
                "            <Property key=\"jndi.initialContextFactoryClassname\">\n" +
                "                <StringValue>com.sun.jndi.ldap.LdapCtxFactory</StringValue>\n" +
                "            </Property>\n" +
                "            <Property key=\"jndi.providerUrl\">\n" +
                "                <StringValue>ldap://127.0.0.1/</StringValue>\n" +
                "            </Property>\n" +
                "            <Property key=\"queue.connectionFactoryName\">\n" +
                "                <StringValue>cn=QueueConnectionFactory</StringValue>\n" +
                "            </Property>\n" +
                "        </Properties>\n" +
                "    </JMSConnection>\n" +
                "</JMSDestination>";
        doCreate( resourceUri, payload, new Goid(0,2).toString(), new Goid(0,3).toString(), new Goid(0,4).toString() );
    }

    @Test
    public void testCreateTemplateJMSDestination() throws Exception {
        String resourceUri = "http://ns.l7tech.com/2010/04/gateway-management/jmsDestinations";
        String payload =
                "<JMSDestination xmlns=\"http://ns.l7tech.com/2010/04/gateway-management\">\n" +
                "    <JMSDestinationDetail version=\"0\" id=\"48037888\">\n" +
                "        <Name>QueueName</Name>\n" +
                "        <Inbound>false</Inbound>\n" +
                "        <Enabled>true</Enabled>\n" +
                "        <Template>true</Template>\n" +
                "    </JMSDestinationDetail>\n" +
                "    <JMSConnection>\n" +
                "        <ProviderType>TIBCO EMS</ProviderType>" +
                "        <Template>true</Template>\n" +
                "    </JMSConnection>\n" +
                "</JMSDestination>";
        doCreate( resourceUri, payload, new Goid(0,2).toString(), new Goid(0,3).toString(), new Goid(0,4).toString() );
    }

    @BugId("SSG-6372")
    @Test
    public void testCreateTemplateJMSWeblogicDestination() throws Exception {
        String resourceUri = "http://ns.l7tech.com/2010/04/gateway-management/jmsDestinations";
        String payload =
                "<JMSDestination xmlns=\"http://ns.l7tech.com/2010/04/gateway-management\">\n" +
                "    <JMSDestinationDetail version=\"0\" id=\"48037888\">\n" +
                "        <Name>QueueName</Name>\n" +
                "        <Inbound>false</Inbound>\n" +
                "        <Enabled>true</Enabled>\n" +
                "        <Template>true</Template>\n" +
                "    </JMSDestinationDetail>\n" +
                "    <JMSConnection>\n" +
                "        <ProviderType>WebLogic JMS</ProviderType>" +
                "        <Template>true</Template>\n" +
                "    </JMSConnection>\n" +
                "</JMSDestination>";
        doCreate( resourceUri, payload, new Goid(0,2).toString(), new Goid(0,3).toString(), new Goid(0,4).toString()  );
    }

    @Test
    public void testCreateTemplateJMSDestinationFailDestination() throws Exception {
        String resourceUri = "http://ns.l7tech.com/2010/04/gateway-management/jmsDestinations";
        String payload = // payload with missing destination name
                "<JMSDestination xmlns=\"http://ns.l7tech.com/2010/04/gateway-management\">\n" +
                "    <JMSDestinationDetail version=\"0\" id=\"48037888\">\n" +
                "        <Name>QueueName</Name>\n" +
                "        <Inbound>false</Inbound>\n" +
                "        <Enabled>true</Enabled>\n" +
                "        <Template>false</Template>\n" +
                "    </JMSDestinationDetail>\n" +
                "    <JMSConnection>\n" +
                "        <Template>true</Template>\n" +
                "    </JMSConnection>\n" +
                "</JMSDestination>";
        doCreateFail( resourceUri, payload, "wxf:InvalidRepresentation" );
    }

    @Test
    public void testCreateTemplateJMSDestinationFailConnection() throws Exception {
        String resourceUri = "http://ns.l7tech.com/2010/04/gateway-management/jmsDestinations";
        String payload = // payload with missing JMS connection properties
                "<JMSDestination xmlns=\"http://ns.l7tech.com/2010/04/gateway-management\">\n" +
                "    <JMSDestinationDetail version=\"0\" id=\"48037888\">\n" +
                "        <Name>QueueName</Name>\n" +
                "        <Inbound>false</Inbound>\n" +
                "        <Enabled>true</Enabled>\n" +
                "        <Template>true</Template>\n" +
                "    </JMSDestinationDetail>\n" +
                "    <JMSConnection>\n" +
                "    </JMSConnection>\n" +
                "</JMSDestination>";
        doCreateFail( resourceUri, payload, "wxf:InvalidRepresentation" );
    }

    @Test
    public void testCreateTemplateJMSDestinationFailConnectionProviderType() throws Exception {
        String resourceUri = "http://ns.l7tech.com/2010/04/gateway-management/jmsDestinations";
        String payload = // payload invalid JMS provider type
                "<JMSDestination xmlns=\"http://ns.l7tech.com/2010/04/gateway-management\">\n" +
                "    <JMSDestinationDetail version=\"0\" id=\"48037888\">\n" +
                "        <Name>QueueName</Name>\n" +
                "        <Inbound>false</Inbound>\n" +
                "        <Enabled>true</Enabled>\n" +
                "        <Template>true</Template>\n" +
                "    </JMSDestinationDetail>\n" +
                "    <JMSConnection>\n" +
                "        <ProviderType>bad</ProviderType>" +
                "        <Template>true</Template>\n" +
                "    </JMSConnection>\n" +
                "</JMSDestination>";
        doCreateFail( resourceUri, payload, "wsman:SchemaValidationError" );
    }

    @Test
    public void testCreatePolicy() throws Exception {
        String resourceUri = "http://ns.l7tech.com/2010/04/gateway-management/policies";
        String payload = "<Policy xmlns=\"http://ns.l7tech.com/2010/04/gateway-management\"><PolicyDetail folderId=\""+new Goid(0,1)+"\"><Name>Policy Name</Name><PolicyType>Include</PolicyType><Properties><Property key=\"soap\"><BooleanValue>true</BooleanValue></Property></Properties></PolicyDetail><Resources><ResourceSet tag=\"policy\"><Resource type=\"policy\">&lt;?xml version=&quot;1.0&quot; encoding=&quot;UTF-8&quot;?&gt;\n" +
                "&lt;wsp:Policy xmlns:L7p=&quot;http://www.layer7tech.com/ws/policy&quot; xmlns:wsp=&quot;http://schemas.xmlsoap.org/ws/2002/12/policy&quot;&gt;\n" +
                "    &lt;wsp:All wsp:Usage=&quot;Required&quot;&gt;\n" +
                "        &lt;L7p:CommentAssertion&gt;\n" +
                "            &lt;L7p:Comment stringValue=&quot;Comment&quot;/&gt;\n" +
                "        &lt;/L7p:CommentAssertion&gt;\n" +
                "    &lt;/wsp:All&gt;\n" +
                "&lt;/wsp:Policy&gt;\n" +
                "</Resource></ResourceSet></Resources></Policy>";
        String expectedId = new Goid(0,4).toHexString();
        doCreate( resourceUri, payload, expectedId );
    }

    @BugId("SSG-8575")
    @Test
    public void testCreateDebugTracePolicy() throws Exception {
        String resourceUri = "http://ns.l7tech.com/2010/04/gateway-management/policies";
        String payload = "<Policy xmlns=\"http://ns.l7tech.com/2010/04/gateway-management\"><PolicyDetail folderId=\""+new Goid(0,1)+"\"><Name>[Internal Debug Trace Policy]</Name><PolicyType>Internal</PolicyType><Properties><Property key=\"soap\"><BooleanValue>false</BooleanValue></Property><Property key=\"tag\"><StringValue>debug-trace</StringValue></Property></Properties></PolicyDetail><Resources><ResourceSet tag=\"policy\"><Resource type=\"policy\">&lt;?xml version=&quot;1.0&quot; encoding=&quot;UTF-8&quot;?&gt;\n" +
                "&lt;wsp:Policy xmlns:L7p=&quot;http://www.layer7tech.com/ws/policy&quot; xmlns:wsp=&quot;http://schemas.xmlsoap.org/ws/2002/12/policy&quot;&gt;\n" +
                "   &lt;wsp:All wsp:Usage=&quot;Required&quot;&gt;\n" +
                "        &lt;L7p:CommentAssertion&gt;\n" +
                "            &lt;L7p:Comment stringValue=&quot;A simple debug trace policy.&quot;/&gt;\n" +
                "        &lt;/L7p:CommentAssertion&gt;\n" +
                "        &lt;L7p:CommentAssertion&gt;\n" +
                "            &lt;L7p:Comment stringValue=&quot;This policy will be invoked after every assertion for any service with debug tracing enabled.&quot;/&gt;\n" +
                "        &lt;/L7p:CommentAssertion&gt;\n" +
                "        &lt;L7p:CommentAssertion&gt;\n" +
                "            &lt;L7p:Comment stringValue=&quot;For example, we can trigger auditing of trace information about the assertion that just finished.&quot;/&gt;\n" +
                "        &lt;/L7p:CommentAssertion&gt;\n" +
                "        &lt;L7p:AuditAssertion&gt;\n" +
                "            &lt;L7p:ChangeSaveRequest booleanValue=&quot;false&quot;/&gt;\n" +
                "            &lt;L7p:ChangeSaveResponse booleanValue=&quot;false&quot;/&gt;\n" +
                "        &lt;/L7p:AuditAssertion&gt;\n" +
                "        &lt;L7p:AuditDetailAssertion&gt;\n" +
                "            &lt;L7p:Detail stringValue=&quot;TRACE: service.name=${trace.service.name} policy.name=${trace.policy.name} policy.guid=${trace.policy.guid} assertion.number=${trace.assertion.numberstr} assertion.shortname=${trace.assertion.shortname} assertion.start=${trace.assertion.start.ms} assertion.latency=${trace.assertion.latency.ms} status=${trace.status}&quot;/&gt;\n" +
                "            &lt;L7p:Enabled booleanValue=&quot;false&quot;/&gt;\n" +
                "        &lt;/L7p:AuditDetailAssertion&gt;\n" +
                "        &lt;L7p:AuditDetailAssertion&gt;\n" +
                "            &lt;L7p:Detail stringValue=&quot;/${trace.service.name} | ${trace.assertion.numberstr} | ${trace.assertion.shortname} | ${trace.var.service} | ${trace.var.ea1} | ${trace.var.input1} | ${trace.var.output1}&quot;/&gt;\n" +
                "        &lt;/L7p:AuditDetailAssertion&gt;\n" +
                "    &lt;/wsp:All&gt;\n" +
                "&lt;/wsp:Policy&gt;\n" +
                "</Resource></ResourceSet></Resources></Policy>";
        String expectedId = new Goid(0,4).toHexString();
        doCreate( resourceUri, payload, expectedId );

        //verify the cluster property was created correctly
        PolicyManager policyManager = applicationContext.getBean("policyManager", PolicyManager.class);
        ClusterPropertyManager clusterPropertyManager = applicationContext.getBean("clusterPropertyManager", ClusterPropertyManager.class);
        Assert.assertEquals(policyManager.findByPrimaryKey(Goid.parseGoid(expectedId)).getGuid(),clusterPropertyManager.getProperty("trace.policy.guid"));

    }

    @Test
    public void testCreateDTD() throws Exception {
        String resourceUri = "http://ns.l7tech.com/2010/04/gateway-management/resources";
        String payload =
                "<ResourceDocument xmlns=\"http://ns.l7tech.com/2010/04/gateway-management\">\n" +
                "    <Resource sourceUrl=\"books2.dtd\" type=\"dtd\"><![CDATA[<!ELEMENT book ANY>]]></Resource>\n" +
                "    <Properties>\n" +
                "        <Property key=\"description\">\n" +
                "            <StringValue>The books2 DTD.</StringValue>\n" +
                "        </Property>\n" +
                "        <Property key=\"publicIdentifier\">\n" +
                "            <StringValue>books2</StringValue>\n" +
                "        </Property>\n" +
                "    </Properties>\n" +
                "</ResourceDocument>";
        doCreate( resourceUri, payload, new Goid(0,4).toString(), new Goid(0,5).toString() );
    }

    @Test
    public void testCreateSchema() throws Exception {
        String resourceUri = "http://ns.l7tech.com/2010/04/gateway-management/resources";
        String payload =
                "<ResourceDocument xmlns=\"http://ns.l7tech.com/2010/04/gateway-management\">\n" +
                "    <Resource sourceUrl=\"books2.xsd\" type=\"xmlschema\">&lt;xs:schema targetNamespace=\"urn:books2\" xmlns:xs=\"http://www.w3.org/2001/XMLSchema\"&gt;&lt;xs:element name=\"book\" type=\"xs:string\"/&gt;&lt;/xs:schema&gt;</Resource>\n" +
                "</ResourceDocument>";
        doCreate( resourceUri, payload, new Goid(0,4).toString(), new Goid(0,5).toString() );
    }

    @Test
    public void testCreateStoredPassword() throws Exception {
        String resourceUri = "http://ns.l7tech.com/2010/04/gateway-management/storedPasswords";
        String payload =
                "<StoredPassword xmlns=\"http://ns.l7tech.com/2010/04/gateway-management\">\n" +
                "    <Name>Stored Password</Name>\n" +
                "    <Password>password</Password>\n" +
                "</StoredPassword>";
        doCreate( resourceUri, payload, new Goid(0,2).toString(), new Goid(0,3).toString() );
    }

    @Test
    public void testCreateStoredPasswordWithType() throws Exception {
        String resourceUri = "http://ns.l7tech.com/2010/04/gateway-management/storedPasswords";
        String payload =
                "<StoredPassword xmlns=\"http://ns.l7tech.com/2010/04/gateway-management\">\n" +
                "    <Name>Stored Password</Name>\n" +
                "    <Password>password</Password>\n" +
                "    <Properties>\n" +
                "        <Property key=\"type\">\n" +
                "            <StringValue>PEM Private Key</StringValue>\n" +
                "        </Property>\n" +
                "    </Properties>\n" +
                "</StoredPassword>";
        doCreate( resourceUri, payload, new Goid(0,2).toString(), new Goid(0,3).toString() );
    }

    @Test
    public void testCreateCustomKeyValue() throws Exception {
        String resourceUri = "http://ns.l7tech.com/2010/04/gateway-management/customKeyValues";
        String payload =
            "<l7:CustomKeyValue xmlns:l7=\"http://ns.l7tech.com/2010/04/gateway-management\">\n" +
            "    <l7:StoreName>" + KeyValueStoreServices.INTERNAL_TRANSACTIONAL_KEY_VALUE_STORE_NAME + "</l7:StoreName>\n" +
            "    <l7:Key>key.prefix.key2</l7:Key>\n" +
            "    <l7:Value>PHhtbD5UZXN0IHZhbHVlPC94bWw+</l7:Value>\n" +
            "</l7:CustomKeyValue>";

        String expectedId = new Goid(0,2).toString();
        doCreate(resourceUri, payload, expectedId);
    }

    @Test
    public void testCreateSiteminderConfig() throws Exception {
        String resourceUri = "http://ns.l7tech.com/2010/04/gateway-management/siteMinderConfigurations";
        String payload =
                "<l7:SiteMinderConfiguration xmlns:l7=\"http://ns.l7tech.com/2010/04/gateway-management\">\n" +
                "   <l7:Name>New Config</l7:Name>\n" +
                "   <l7:Address>0.0.0.999</l7:Address>\n" +
                "   <l7:Hostname>localhost</l7:Hostname>\n" +
                "   <l7:HostConfiguration>55</l7:HostConfiguration>\n" +
                "   <l7:UserName>user</l7:UserName>\n" +
                "   <l7:PasswordId>00000000000000000000000000000001</l7:PasswordId>\n" +
                "   <l7:Enabled>true</l7:Enabled>\n" +
                "   <l7:FipsMode>3</l7:FipsMode>\n" +
                "   <l7:IpCheck>false</l7:IpCheck>\n" +
                "   <l7:UpdateSsoToken>false</l7:UpdateSsoToken>\n" +
                "   <l7:ClusterThreshold>50</l7:ClusterThreshold>\n" +
                "   <l7:NonClusterFailover>false</l7:NonClusterFailover>"+
                "   <l7:Secret>secret</l7:Secret>\n" +
                "   <l7:Properties/>\n" +
                "</l7:SiteMinderConfiguration>";

        String expectedId = new Goid(0,3).toString();
        doCreate(resourceUri, payload, expectedId);
    }

    @Test
    public void testCreateFailAssertionSecurityZone() throws Exception {
        String resourceUri = "http://ns.l7tech.com/2010/04/gateway-management/assertionSecurityZones";
        String payload =
                "<l7:AssertionSecurityZone xmlns:l7=\"http://ns.l7tech.com/2010/04/gateway-management\"  securityZoneId=\"00000000000000000000000000000002\">\n" +
                        "   <l7:Name>New Assertion Security Zone</l7:Name>\n" +
                        "</l7:AssertionSecurityZone>";

        doCreateFail(resourceUri, payload, "wsa:ActionNotSupported");
    }

    @Test
    public void testDeleteFailAssertionSecurityZone() throws Exception {
        String message = "<s:Envelope xmlns:s=\"http://www.w3.org/2003/05/soap-envelope\" xmlns:wsa=\"http://schemas.xmlsoap.org/ws/2004/08/addressing\" xmlns:wsman=\"http://schemas.dmtf.org/wbem/wsman/1/wsman.xsd\"><s:Header><wsa:Action s:mustUnderstand=\"true\">http://schemas.xmlsoap.org/ws/2004/09/transfer/Delete</wsa:Action><wsa:To s:mustUnderstand=\"true\">http://127.0.0.1:8080/wsman</wsa:To><wsman:ResourceURI s:mustUnderstand=\"true\">http://ns.l7tech.com/2010/04/gateway-management/assertionSecurityZones</wsman:ResourceURI><wsa:MessageID s:mustUnderstand=\"true\">uuid:b2794ffb-7d39-1d39-8002-481688002100</wsa:MessageID><wsa:ReplyTo><wsa:Address>http://schemas.xmlsoap.org/ws/2004/08/addressing/role/anonymous</wsa:Address></wsa:ReplyTo><wsman:SelectorSet><wsman:Selector Name=\"id\">"+new Goid(0,2).toHexString()+"</wsman:Selector></wsman:SelectorSet></s:Header><s:Body/></s:Envelope>";

        final Document result = processRequest( "http://schemas.xmlsoap.org/ws/2004/09/transfer/Delete", message );

        final Element soapBody = SoapUtil.getBodyElement(result);
        final Element faultElement = XmlUtil.findExactlyOneChildElementByName(soapBody, SOAPConstants.URI_NS_SOAP_1_2_ENVELOPE, "Fault");
        final Element codeElement = XmlUtil.findExactlyOneChildElementByName(faultElement, SOAPConstants.URI_NS_SOAP_1_2_ENVELOPE, "Code");
        final Element subcodeElement = XmlUtil.findExactlyOneChildElementByName(codeElement, SOAPConstants.URI_NS_SOAP_1_2_ENVELOPE, "Subcode");
        final Element valueElement = XmlUtil.findExactlyOneChildElementByName(subcodeElement, SOAPConstants.URI_NS_SOAP_1_2_ENVELOPE, "Value");

        final String code = XmlUtil.getTextValue( valueElement );

        assertEquals("SOAP Fault subcode", "wsa:ActionNotSupported", code);
    }

    @Test
    public void testCreateSecurityZone() throws Exception {
        String resourceUri = "http://ns.l7tech.com/2010/04/gateway-management/securityZones";
        String payload =
                "<l7:SecurityZone id=\"00000000000000000000000000000001\" version=\"0\" xmlns:l7=\"http://ns.l7tech.com/2010/04/gateway-management\">\n" +
                "    <l7:Name>New Security Zone</l7:Name>\n" +
                "    <l7:Description>Canned New Security Zone</l7:Description>\n" +
                "    <l7:PermittedEntityTypes>\n" +
                "        <l7:StringValue>FOLDER</l7:StringValue>\n" +
                "    </l7:PermittedEntityTypes>\n" +
                "</l7:SecurityZone>";

        String expectedId = new Goid(0,3).toString();
        doCreate(resourceUri, payload, expectedId);
    }

    @Test
    public void testCreatePolicyAlias() throws Exception {
        String resourceUri = "http://ns.l7tech.com/2010/04/gateway-management/policyAliases";
        String payload =
                "<l7:PolicyAlias folderId=\"00000000000000000000000000000003\" id=\"00000000000000000000000000000001\" version=\"0\" xmlns:l7=\"http://ns.l7tech.com/2010/04/gateway-management\">\n" +
                "    <l7:PolicyReference id=\"00000000000000000000000000000001\" resourceUri=\"http://ns.l7tech.com/2010/04/gateway-management/policies\"/>\n" +
                "</l7:PolicyAlias>";

        String expectedId = new Goid(0,2).toString();
        doCreate(resourceUri, payload, expectedId);
    }

    @Test
    public void testCreatePolicyAliasFailSameFolder() throws Exception {

        // same folder as original
        String resourceUri = "http://ns.l7tech.com/2010/04/gateway-management/policyAliases";
        String payloadFail =
        "<l7:PolicyAlias folderId=\"0000000000000000ffffffffffffec76\" id=\"00000000000000000000000000000001\" version=\"0\" xmlns:l7=\"http://ns.l7tech.com/2010/04/gateway-management\">\n" +
                        "    <l7:PolicyReference id=\"00000000000000000000000000000001\" resourceUri=\"http://ns.l7tech.com/2010/04/gateway-management/policies\"/>\n" +
                        "</l7:PolicyAlias>";

        doCreateFail(resourceUri, payloadFail, "wxf:InvalidRepresentation","Cannot create alias in the same folder as original");
    }

    @Test
    public void testCreatePolicyAliasFailSameFolderAsAlias() throws Exception {
        // same folder as another alias referencing same policy
        String resourceUri = "http://ns.l7tech.com/2010/04/gateway-management/policyAliases";
        String payloadFail =
                "<l7:PolicyAlias folderId=\"00000000000000000000000000000001\" id=\"00000000000000000000000000000001\" version=\"0\" xmlns:l7=\"http://ns.l7tech.com/2010/04/gateway-management\">\n" +
                        "    <l7:PolicyReference id=\"00000000000000000000000000000001\" resourceUri=\"http://ns.l7tech.com/2010/04/gateway-management/policies\"/>\n" +
                        "</l7:PolicyAlias>";

        doCreateFail(resourceUri, payloadFail, "wxf:InvalidRepresentation", "already exists in folder");
    }

    @Test
    public void testCreatePolicyAliasFailNoFolderPermission() throws Exception {
        when(rbacService.isPermittedForEntity(any(User.class), any(Folder.class),eq(OperationType.UPDATE), anyString())).thenReturn(false);

        String resourceUri = "http://ns.l7tech.com/2010/04/gateway-management/policyAliases";
        String payloadFail =
                "<l7:PolicyAlias folderId=\"00000000000000000000000000000001\" id=\"00000000000000000000000000000001\" version=\"0\" xmlns:l7=\"http://ns.l7tech.com/2010/04/gateway-management\">\n" +
                        "    <l7:PolicyReference id=\"00000000000000000000000000000001\" resourceUri=\"http://ns.l7tech.com/2010/04/gateway-management/policies\"/>\n" +
                        "</l7:PolicyAlias>";

        doCreateFail(resourceUri, payloadFail, "wsman:AccessDenied");
    }

    @Test
    public void testCreateServiceAlias() throws Exception {
        String resourceUri = "http://ns.l7tech.com/2010/04/gateway-management/serviceAliases";
        String payload =
                "<l7:ServiceAlias folderId=\"00000000000000000000000000000003\" id=\"00000000000000000000000000000001\" version=\"0\" xmlns:l7=\"http://ns.l7tech.com/2010/04/gateway-management\">\n" +
                        "    <l7:ServiceReference id=\"00000000000000000000000000000001\" resourceUri=\"http://ns.l7tech.com/2010/04/gateway-management/services\"/>\n" +
                        "</l7:ServiceAlias>";

        String expectedId = new Goid(0,2).toString();
        doCreate(resourceUri, payload, expectedId);
    }

    @Test
    public void testCreateServiceAliasFailSameFolder() throws Exception {

        // same folder as original
        String resourceUri = "http://ns.l7tech.com/2010/04/gateway-management/serviceAliases";
        String payloadFail =
                "<l7:ServiceAlias folderId=\"0000000000000000ffffffffffffec76\" id=\"00000000000000000000000000000001\" version=\"0\" xmlns:l7=\"http://ns.l7tech.com/2010/04/gateway-management\">\n" +
                        "    <l7:ServiceReference id=\"00000000000000000000000000000001\" resourceUri=\"http://ns.l7tech.com/2010/04/gateway-management/services\"/>\n" +
                        "</l7:ServiceAlias>";

        doCreateFail(resourceUri, payloadFail, "wxf:InvalidRepresentation");
    }


    @Test
    public void testCreateServiceAliasFailSameFolderAsAlias() throws Exception {
        // same folder as another alias referencing same service
        String resourceUri = "http://ns.l7tech.com/2010/04/gateway-management/serviceAliases";
        String payloadFail =
                "<l7:ServiceAlias folderId=\"00000000000000000000000000000001\" id=\"00000000000000000000000000000001\" version=\"0\" xmlns:l7=\"http://ns.l7tech.com/2010/04/gateway-management\">\n" +
                        "    <l7:ServiceReference id=\"00000000000000000000000000000001\" resourceUri=\"http://ns.l7tech.com/2010/04/gateway-management/services\"/>\n" +
                        "</l7:ServiceAlias>";

        doCreateFail(resourceUri, payloadFail, "wxf:InvalidRepresentation", "already exists in folder");
    }

    @Test
    public void testCreateServiceAliasFailNoFolderPermission() throws Exception {
        when(rbacService.isPermittedForEntity(any(User.class), any(Folder.class),eq(OperationType.UPDATE), anyString())).thenReturn(false);

        String resourceUri = "http://ns.l7tech.com/2010/04/gateway-management/serviceAliases";
        String payloadFail =
                "<l7:ServiceAlias folderId=\"00000000000000000000000000000001\" id=\"00000000000000000000000000000001\" version=\"0\" xmlns:l7=\"http://ns.l7tech.com/2010/04/gateway-management\">\n" +
                        "    <l7:ServiceReference id=\"00000000000000000000000000000001\" resourceUri=\"http://ns.l7tech.com/2010/04/gateway-management/services\"/>\n" +
                        "</l7:ServiceAlias>";

        doCreateFail(resourceUri, payloadFail, "wsman:AccessDenied");
    }

    @Test
    public void testPutEncapsulatedAssertion() throws Exception {

        String id = new Goid(0,1).toString();
        // Try basic successful update of existing encass config
        putAndVerify(makeEncassPutMess("id=\""+id+"\"", "version=\"0\"", "<l7:Guid>ABCD-0001</l7:Guid>", "id=\""+new Goid(0,1)+"\"", 1), verifyEncassUpdate(1, null), false );

        // Try omitting OID and ver (can't just pass "true" for removeVersionAndId arg because it kills the policy OID in the crossfire and we need it)
        putAndVerify(makeEncassPutMess("", "", "<l7:Guid>ABCD-0001</l7:Guid>", "id=\""+new Goid(0,1)+"\"", 2), verifyEncassUpdate(2, null), false );

        // Try leaving out the GUID (should fail)
        putAndVerify(makeEncassPutMess("id=\""+id+"\"", "version=\"2\"", "", "id=\""+new Goid(0,1)+"\"", 3), verifyEncassUpdate(3, "unable to change GUID of existing encapsulated assertion config"), false );

        // Try changing the encass GUID (should fail)
        putAndVerify(makeEncassPutMess("id=\""+id+"\"", "version=\"2\"", "<l7:Guid>ABCD-EXTRA-0001</l7:Guid>", "id=\""+new Goid(0,1)+"\"", 4), verifyEncassUpdate(4, "unable to change GUID of existing encapsulated assertion config"), false );

        // Try leaving out the backing policy OID (should work OK)
        putAndVerify(makeEncassPutMess("id=\""+id+"\"", "version=\"2\"", "<l7:Guid>ABCD-0001</l7:Guid>", "id=\""+new Goid(0,1)+"\"", 5), verifyEncassUpdate(5, null), false );

        // Try changing the backing policy OID (should fail)
        putAndVerify(makeEncassPutMess("id=\""+id+"\"", "version=\"2\"", "<l7:Guid>ABCD-0001</l7:Guid>", "id=\""+new Goid(0,2)+"\"", 6), verifyEncassUpdate(6, "unable to change backing policy of an existing encapsulated assertion config"), false );
    }

    private UnaryVoidThrows<Document, Exception> verifyEncassUpdate(final int updateNum, @Nullable final String expectedFaultSubstring) {
        return new UnaryVoidThrows<Document,Exception>(){
                @Override
                public void call( final Document result ) throws Exception {

                    if (expectedFaultSubstring == null) {
                        // Verify successful update (check updated encass config name)
                        final Element soapBody = SoapUtil.getBodyElement(result);
                        final Element encapsulatedAssertion = XmlUtil.findExactlyOneChildElementByName(soapBody, NS_GATEWAY_MANAGEMENT, "EncapsulatedAssertion");
                        final Element name = XmlUtil.findExactlyOneChildElementByName(encapsulatedAssertion, NS_GATEWAY_MANAGEMENT, "Name");
                        assertEquals("Encapsulated assertion name", "Test Encass Config 1 (updated #" + updateNum + ")", XmlUtil.getTextValue(name));
                    } else {
                        // Verify fault is as expected
                        String str = XmlUtil.nodeToString(result);

                        assertTrue("Should be a fault", str.contains("FaultDetail>http://schemas.dmtf.org/wbem/wsman/1/wsman/faultDetail"));
                        assertTrue("Should contain expected fault substring", str.contains(expectedFaultSubstring));
                    }

                }
            };
    }

    private static String makeEncassPutMess(String oidStr, String versionStr, String guidStr, String policyOidStr, int updateNum) {
        return "<s:Envelope xmlns:s=\"http://www.w3.org/2003/05/soap-envelope\" xmlns:wsa=\"http://schemas.xmlsoap.org/ws/2004/08/addressing\" xmlns:wsman=\"http://schemas.dmtf.org/wbem/wsman/1/wsman.xsd\" xmlns:n1=\"http://ns.l7tech.com/2010/04/gateway-management\"><s:Header><wsa:Action s:mustUnderstand=\"true\">http://schemas.xmlsoap.org/ws/2004/09/transfer/Put</wsa:Action><wsa:To s:mustUnderstand=\"true\">http://127.0.0.1:8080/wsman</wsa:To>\n" +
            "<wsman:ResourceURI s:mustUnderstand=\"true\">http://ns.l7tech.com/2010/04/gateway-management/encapsulatedAssertions</wsman:ResourceURI><wsa:MessageID s:mustUnderstand=\"true\">uuid:afad2993-7d39-1d39-8002-481688002100</wsa:MessageID><wsa:ReplyTo><wsa:Address>http://schemas.xmlsoap.org/ws/2004/08/addressing/role/anonymous</wsa:Address></wsa:ReplyTo><wsman:SelectorSet><wsman:Selector Name=\"id\">00000000000000000000000000000001</wsman:Selector></wsman:SelectorSet><wsman:RequestEPR/></s:Header><s:Body>\n" +
            "<l7:EncapsulatedAssertion xmlns:l7=\"http://ns.l7tech.com/2010/04/gateway-management\" " + oidStr + " " + versionStr + ">\n" +
            "    <l7:Name>Test Encass Config 1 (updated #" + updateNum + ")</l7:Name>\n" +
            "    " + guidStr + "\n" +
            "    <l7:PolicyReference " + policyOidStr + " resourceUri=\"http://ns.l7tech.com/2010/04/gateway-management/policies\"/>\n" +
            "    <l7:EncapsulatedArguments>\n" +
            "        <l7:EncapsulatedAssertionArgument>\n" +
            "            <l7:Ordinal>1</l7:Ordinal>\n" +
            "            <l7:ArgumentName>input1</l7:ArgumentName>\n" +
            "            <l7:ArgumentType>decimal</l7:ArgumentType>\n" +
            "            <l7:GuiLabel>Input1 Label</l7:GuiLabel>\n" +
            "            <l7:GuiPrompt>true</l7:GuiPrompt>\n" +
            "        </l7:EncapsulatedAssertionArgument>\n" +
            "        <l7:EncapsulatedAssertionArgument>\n" +
            "            <l7:Ordinal>2</l7:Ordinal>\n" +
            "            <l7:ArgumentName>input2</l7:ArgumentName>\n" +
            "            <l7:ArgumentType>string</l7:ArgumentType>\n" +
            "            <l7:GuiLabel>Input2 Label</l7:GuiLabel>\n" +
            "            <l7:GuiPrompt>false</l7:GuiPrompt>\n" +
            "        </l7:EncapsulatedAssertionArgument>\n" +
            "    </l7:EncapsulatedArguments>\n" +
            "    <l7:EncapsulatedResults>\n" +
            "        <l7:EncapsulatedAssertionResult>\n" +
            "            <l7:ResultName>result1</l7:ResultName>\n" +
            "            <l7:ResultType>boolean</l7:ResultType>\n" +
            "        </l7:EncapsulatedAssertionResult>\n" +
            "        <l7:EncapsulatedAssertionResult>\n" +
            "            <l7:ResultName>result2</l7:ResultName>\n" +
            "            <l7:ResultType>string</l7:ResultType>\n" +
            "        </l7:EncapsulatedAssertionResult>\n" +
            "    </l7:EncapsulatedResults>\n" +
            "    <l7:Properties>\n" +
            "        <l7:Property key=\"c\">\n" +
            "            <l7:StringValue>d</l7:StringValue>\n" +
            "        </l7:Property>\n" +
            "        <l7:Property key=\"e\">\n" +
            "            <l7:StringValue>f</l7:StringValue>\n" +
            "        </l7:Property>\n" +
            "    </l7:Properties>\n" +
            "</l7:EncapsulatedAssertion>\n" +
    "</s:Body></s:Envelope>";
    }

    @Test
    public void testPutCertificate() throws Exception {
        final String message =
                "<s:Envelope xmlns:s=\"http://www.w3.org/2003/05/soap-envelope\" xmlns:wsa=\"http://schemas.xmlsoap.org/ws/2004/08/addressing\" xmlns:wsman=\"http://schemas.dmtf.org/wbem/wsman/1/wsman.xsd\" xmlns:n1=\"http://ns.l7tech.com/2010/04/gateway-management\"><s:Header><wsa:Action s:mustUnderstand=\"true\">http://schemas.xmlsoap.org/ws/2004/09/transfer/Put</wsa:Action><wsa:To s:mustUnderstand=\"true\">http://127.0.0.1:8080/wsman</wsa:To><wsman:ResourceURI s:mustUnderstand=\"true\">http://ns.l7tech.com/2010/04/gateway-management/trustedCertificates</wsman:ResourceURI><wsa:MessageID s:mustUnderstand=\"true\">uuid:afad2993-7d39-1d39-8002-481688002100</wsa:MessageID><wsa:ReplyTo><wsa:Address>http://schemas.xmlsoap.org/ws/2004/08/addressing/role/anonymous</wsa:Address></wsa:ReplyTo><wsman:SelectorSet><wsman:Selector Name=\"id\">" + new Goid(0,2).toHexString() + "</wsman:Selector></wsman:SelectorSet><wsman:RequestEPR/></s:Header><s:Body><TrustedCertificate xmlns=\"http://ns.l7tech.com/2010/04/gateway-management\" id=\"" + new Goid(0,2).toHexString() + "\" version=\"0\">\n" +
                "    <Name>Bob (updated)</Name>\n" +
                "    <CertificateData>\n" +
                "        <Encoded>MIIDCjCCAfKgAwIBAgIQYDju2/6sm77InYfTq65x+DANBgkqhkiG9w0BAQUFADAwMQ4wDAYDVQQKDAVPQVNJUzEeMBwGA1UEAwwVT0FTSVMgSW50ZXJvcCBUZXN0IENBMB4XDTA1MDMxOTAwMDAwMFoXDTE4MDMxOTIzNTk1OVowQDEOMAwGA1UECgwFT0FTSVMxIDAeBgNVBAsMF09BU0lTIEludGVyb3AgVGVzdCBDZXJ0MQwwCgYDVQQDDANCb2IwgZ8wDQYJKoZIhvcNAQEBBQADgY0AMIGJAoGBAMCquMva4lFDrv3fXQnKK8CkSU7HvVZ0USyJtlL/yhmHH/FQXHyYY+fTcSyWYItWJYiTZ99PAbD+6EKBGbdfuJNUJCGaTWc5ZDUISqM/SGtacYe/PD/4+g3swNPzTUQAIBLRY1pkr2cm3s5Ch/f+mYVNBR41HnBeIxybw25kkoM7AgMBAAGjgZMwgZAwCQYDVR0TBAIwADAzBgNVHR8ELDAqMCiiJoYkaHR0cDovL2ludGVyb3AuYmJ0ZXN0Lm5ldC9jcmwvY2EuY3JsMA4GA1UdDwEB/wQEAwIEsDAdBgNVHQ4EFgQUXeg55vRyK3ZhAEhEf+YT0z986L0wHwYDVR0jBBgwFoAUwJ0o/MHrNaEd1qqqoBwaTcJJDw8wDQYJKoZIhvcNAQEFBQADggEBAIiVGv2lGLhRvmMAHSlY7rKLVkv+zEUtSyg08FBT8z/RepUbtUQShcIqwWsemDU8JVtsucQLc+g6GCQXgkCkMiC8qhcLAt3BXzFmLxuCEAQeeFe8IATr4wACmEQE37TEqAuWEIanPYIplbxYgwP0OBWBSjcRpKRAxjEzuwObYjbll6vKdFHYIweWhhWPrefquFp7TefTkF4D3rcctTfWJ76I5NrEVld+7PBnnJNpdDEuGsoaiJrwTW3Ixm40RXvG3fYS4hIAPeTCUk3RkYfUkqlaaLQnUrF2hZSgiBNLPe8gGkYORccRIlZCGQDEpcWl1Uf9OHw6fC+3hkqolFd5CVI=</Encoded>\n" +
                "    </CertificateData>\n" +
                "</TrustedCertificate></s:Body></s:Envelope>";

        final UnaryVoidThrows<Document,Exception> verifier = new UnaryVoidThrows<Document,Exception>(){
            @Override
            public void call( final Document result ) throws Exception {
                final Element soapBody = SoapUtil.getBodyElement(result);
                final Element trustedCertificate = XmlUtil.findExactlyOneChildElementByName(soapBody, NS_GATEWAY_MANAGEMENT, "TrustedCertificate");
                final Element name = XmlUtil.findExactlyOneChildElementByName(trustedCertificate, NS_GATEWAY_MANAGEMENT, "Name");

                assertEquals("Trusted certificate name", "Bob (updated)", XmlUtil.getTextValue(name));
            }
        };

        putAndVerify( message, verifier, false );
        putAndVerify( message, verifier, true );
    }

    @Test
    public void testPutClusterProperty() throws Exception {
        final String message = "<s:Envelope xmlns:s=\"http://www.w3.org/2003/05/soap-envelope\" xmlns:wsa=\"http://schemas.xmlsoap.org/ws/2004/08/addressing\" xmlns:wsman=\"http://schemas.dmtf.org/wbem/wsman/1/wsman.xsd\" xmlns:n1=\"http://ns.l7tech.com/2010/04/gateway-management\"><s:Header><wsa:Action s:mustUnderstand=\"true\">http://schemas.xmlsoap.org/ws/2004/09/transfer/Put</wsa:Action><wsa:To s:mustUnderstand=\"true\">http://127.0.0.1:8080/wsman</wsa:To><wsman:ResourceURI s:mustUnderstand=\"true\">http://ns.l7tech.com/2010/04/gateway-management/clusterProperties</wsman:ResourceURI><wsa:MessageID s:mustUnderstand=\"true\">uuid:afad2993-7d39-1d39-8002-481688002100</wsa:MessageID><wsa:ReplyTo><wsa:Address>http://schemas.xmlsoap.org/ws/2004/08/addressing/role/anonymous</wsa:Address></wsa:ReplyTo><wsman:SelectorSet><wsman:Selector Name=\"id\">"+new Goid(0,1).toHexString()+"</wsman:Selector></wsman:SelectorSet><wsman:RequestEPR/></s:Header><s:Body> <n1:ClusterProperty id=\""+new Goid(0,1).toHexString()+"\" version=\"0\"><n1:Name>test</n1:Name><n1:Value>value2</n1:Value></n1:ClusterProperty>  </s:Body></s:Envelope>";

        final UnaryVoidThrows<Document,Exception> verifier = new UnaryVoidThrows<Document,Exception>(){
            @Override
            public void call( final Document result ) throws Exception {
                final Element soapBody = SoapUtil.getBodyElement(result);
                final Element clusterProperty = XmlUtil.findExactlyOneChildElementByName(soapBody, NS_GATEWAY_MANAGEMENT, "ClusterProperty");
                final Element value = XmlUtil.findExactlyOneChildElementByName(clusterProperty, NS_GATEWAY_MANAGEMENT, "Value");

                assertEquals("Property value", "value2", XmlUtil.getTextValue(value));
            }
        };

        putAndVerify( message, verifier, false );
        putAndVerify( message, verifier, true );
    }


    @Test
    public void testPutClusterPropertyUpdateRoleOnly() throws Exception {

        when(rbacService.isPermittedForEntity(any(User.class), any(ClusterProperty.class),eq(OperationType.READ), anyString())).thenReturn(false);
        when(rbacService.isPermittedForEntity(any(User.class), any(ClusterProperty.class),eq(OperationType.CREATE), anyString())).thenReturn(false);
        when(rbacService.isPermittedForEntity(any(User.class), any(ClusterProperty.class),eq(OperationType.DELETE), anyString())).thenReturn(false);

        final String message = "<s:Envelope xmlns:s=\"http://www.w3.org/2003/05/soap-envelope\" xmlns:wsa=\"http://schemas.xmlsoap.org/ws/2004/08/addressing\" xmlns:wsman=\"http://schemas.dmtf.org/wbem/wsman/1/wsman.xsd\" xmlns:n1=\"http://ns.l7tech.com/2010/04/gateway-management\"><s:Header><wsa:Action s:mustUnderstand=\"true\">http://schemas.xmlsoap.org/ws/2004/09/transfer/Put</wsa:Action><wsa:To s:mustUnderstand=\"true\">http://127.0.0.1:8080/wsman</wsa:To><wsman:ResourceURI s:mustUnderstand=\"true\">http://ns.l7tech.com/2010/04/gateway-management/clusterProperties</wsman:ResourceURI><wsa:MessageID s:mustUnderstand=\"true\">uuid:afad2993-7d39-1d39-8002-481688002100</wsa:MessageID><wsa:ReplyTo><wsa:Address>http://schemas.xmlsoap.org/ws/2004/08/addressing/role/anonymous</wsa:Address></wsa:ReplyTo><wsman:SelectorSet><wsman:Selector Name=\"id\">"+new Goid(0,1).toHexString()+"</wsman:Selector></wsman:SelectorSet><wsman:RequestEPR/></s:Header><s:Body> <n1:ClusterProperty id=\""+new Goid(0,1).toHexString()+"\" version=\"0\"><n1:Name>test</n1:Name><n1:Value>value2</n1:Value></n1:ClusterProperty>  </s:Body></s:Envelope>";

        final UnaryVoidThrows<Document,Exception> verifier = new UnaryVoidThrows<Document,Exception>(){
            @Override
            public void call( final Document result ) throws Exception {
                final Element soapBody = SoapUtil.getBodyElement(result);
                assertEquals("Soap body is not empty", 0 , soapBody.getChildNodes().getLength());
            }
        };

        putAndVerify( message, verifier, false );
        putAndVerify( message, verifier, true );
    }

    @BugId("SSG-5572")
    @Test
    public void testPutGenericEntity() throws Exception {
        final String message = "<s:Envelope xmlns:s=\"http://www.w3.org/2003/05/soap-envelope\" xmlns:wsa=\"http://schemas.xmlsoap.org/ws/2004/08/addressing\" xmlns:wsman=\"http://schemas.dmtf.org/wbem/wsman/1/wsman.xsd\" xmlns:n1=\"http://ns.l7tech.com/2010/04/gateway-management\"><s:Header><wsa:Action s:mustUnderstand=\"true\">http://schemas.xmlsoap.org/ws/2004/09/transfer/Put</wsa:Action><wsa:To s:mustUnderstand=\"true\">http://127.0.0.1:8080/wsman</wsa:To><wsman:ResourceURI s:mustUnderstand=\"true\">http://ns.l7tech.com/2010/04/gateway-management/genericEntities</wsman:ResourceURI><wsa:MessageID s:mustUnderstand=\"true\">uuid:afad2993-7d39-1d39-8002-481688002100</wsa:MessageID><wsa:ReplyTo><wsa:Address>http://schemas.xmlsoap.org/ws/2004/08/addressing/role/anonymous</wsa:Address></wsa:ReplyTo><wsman:SelectorSet><wsman:Selector Name=\"id\">"+new Goid(0,1).toHexString()+"</wsman:Selector></wsman:SelectorSet><wsman:RequestEPR/></s:Header><s:Body> <n1:GenericEntity ><n1:Name>My Test Entity UPDATED</n1:Name><n1:Description>My test entity description UPDATED</n1:Description><n1:EntityClassName>com.l7tech.external.assertions.gatewaymanagement.server.ServerGatewayManagementAssertionTestUPDATED</n1:EntityClassName><n1:Enabled>false</n1:Enabled><n1:ValueXml>&lt;xml&gt;xml valueUPDATED&lt;/xml&gt;</n1:ValueXml></n1:GenericEntity>  </s:Body></s:Envelope>";

        final UnaryVoidThrows<Document,Exception> verifier = new UnaryVoidThrows<Document,Exception>(){
            @Override
            public void call( final Document result ) throws Exception {
                final Element soapBody = SoapUtil.getBodyElement(result);
                final Element genericEntityElm = XmlUtil.findExactlyOneChildElementByName(soapBody, NS_GATEWAY_MANAGEMENT, "GenericEntity");

                final Element nameElm = XmlUtil.findExactlyOneChildElementByName(genericEntityElm, NS_GATEWAY_MANAGEMENT, "Name");
                final Element entityClassNameElm = XmlUtil.findExactlyOneChildElementByName(genericEntityElm, NS_GATEWAY_MANAGEMENT, "EntityClassName");
                final Element descriptionElm = XmlUtil.findExactlyOneChildElementByName(genericEntityElm, NS_GATEWAY_MANAGEMENT, "Description");
                final Element enabledElm = XmlUtil.findExactlyOneChildElementByName(genericEntityElm, NS_GATEWAY_MANAGEMENT, "Enabled");
                final Element valueXmlElm = XmlUtil.findExactlyOneChildElementByName(genericEntityElm, NS_GATEWAY_MANAGEMENT, "ValueXml");

                // Verify attempted changes entity class were ignored
                assertEquals("EntityClassName", ServerGatewayManagementAssertionTest.this.getClass().getName(), XmlUtil.getTextValue(entityClassNameElm));

                // Verify other changes were persisted
                assertEquals("Name", "My Test Entity UPDATED", XmlUtil.getTextValue(nameElm));
                assertEquals("Description", "My test entity description UPDATED", XmlUtil.getTextValue(descriptionElm));
                assertFalse("Enabled", Boolean.parseBoolean(XmlUtil.getTextValue(enabledElm)));
                assertEquals("ValueXml", "<xml>xml valueUPDATED</xml>", XmlUtil.getTextValue(valueXmlElm));

            }
        };

        putAndVerify( message, verifier, false );
        putAndVerify( message, verifier, true );
    }

    @Test
    public void testPutFolder() throws Exception {
        final String message = "<s:Envelope xmlns:s=\"http://www.w3.org/2003/05/soap-envelope\" xmlns:wsa=\"http://schemas.xmlsoap.org/ws/2004/08/addressing\" xmlns:wsman=\"http://schemas.dmtf.org/wbem/wsman/1/wsman.xsd\" xmlns:n1=\"http://ns.l7tech.com/2010/04/gateway-management\"><s:Header><wsa:Action s:mustUnderstand=\"true\">http://schemas.xmlsoap.org/ws/2004/09/transfer/Put</wsa:Action><wsa:To s:mustUnderstand=\"true\">http://127.0.0.1:8080/wsman</wsa:To><wsman:ResourceURI s:mustUnderstand=\"true\">http://ns.l7tech.com/2010/04/gateway-management/folders</wsman:ResourceURI><wsa:MessageID s:mustUnderstand=\"true\">uuid:afad2993-7d39-1d39-8002-481688002100</wsa:MessageID><wsa:ReplyTo><wsa:Address>http://schemas.xmlsoap.org/ws/2004/08/addressing/role/anonymous</wsa:Address></wsa:ReplyTo><wsman:SelectorSet><wsman:Selector Name=\"id\">"+new Goid(0,2)+"</wsman:Selector></wsman:SelectorSet><wsman:RequestEPR/></s:Header><s:Body> <n1:Folder xmlns:n1=\"http://ns.l7tech.com/2010/04/gateway-management\" folderId=\""+new Goid(0,1)+"\" id=\""+new Goid(0,2)+"\" version=\"0\"><n1:Name>Test Folder (updated)</n1:Name></n1:Folder> </s:Body></s:Envelope>";

        final UnaryVoidThrows<Document,Exception> verifier = new UnaryVoidThrows<Document,Exception>(){
            @Override
            public void call( final Document result ) throws Exception {
                final Element soapBody = SoapUtil.getBodyElement(result);
                final Element folder = XmlUtil.findExactlyOneChildElementByName(soapBody, NS_GATEWAY_MANAGEMENT, "Folder");
                final Element name = XmlUtil.findExactlyOneChildElementByName(folder, NS_GATEWAY_MANAGEMENT, "Name");
                final String folderId = folder.getAttribute("folderId");

                assertEquals("Name", "Test Folder (updated)", XmlUtil.getTextValue(name));
                assertEquals("FolderId", new Goid(0,1).toHexString(), folderId);
            }
        };

        putAndVerify( message, verifier, false );
        putAndVerify( message, verifier, true );
    }

    @Test
    public void testPutFolderInRoot() throws Exception {
        final String message = "<s:Envelope xmlns:s=\"http://www.w3.org/2003/05/soap-envelope\" xmlns:wsa=\"http://schemas.xmlsoap.org/ws/2004/08/addressing\" xmlns:wsman=\"http://schemas.dmtf.org/wbem/wsman/1/wsman.xsd\" xmlns:n1=\"http://ns.l7tech.com/2010/04/gateway-management\"><s:Header><wsa:Action s:mustUnderstand=\"true\">http://schemas.xmlsoap.org/ws/2004/09/transfer/Put</wsa:Action><wsa:To s:mustUnderstand=\"true\">http://127.0.0.1:8080/wsman</wsa:To><wsman:ResourceURI s:mustUnderstand=\"true\">http://ns.l7tech.com/2010/04/gateway-management/folders</wsman:ResourceURI><wsa:MessageID s:mustUnderstand=\"true\">uuid:afad2993-7d39-1d39-8002-481688002100</wsa:MessageID><wsa:ReplyTo><wsa:Address>http://schemas.xmlsoap.org/ws/2004/08/addressing/role/anonymous</wsa:Address></wsa:ReplyTo><wsman:SelectorSet><wsman:Selector Name=\"id\">"+new Goid(0,1)+"</wsman:Selector></wsman:SelectorSet><wsman:RequestEPR/></s:Header><s:Body> <n1:Folder xmlns:n1=\"http://ns.l7tech.com/2010/04/gateway-management\" folderId=\""+new Goid(0,-5002)+"\" id=\""+new Goid(0,1)+"\" version=\"0\"><n1:Name>Test Folder (updated)</n1:Name></n1:Folder> </s:Body></s:Envelope>";

        final UnaryVoidThrows<Document,Exception> verifier = new UnaryVoidThrows<Document,Exception>(){
            @Override
            public void call( final Document result ) throws Exception {
                final Element soapBody = SoapUtil.getBodyElement(result);
                final Element folder = XmlUtil.findExactlyOneChildElementByName(soapBody, NS_GATEWAY_MANAGEMENT, "Folder");
                final Element name = XmlUtil.findExactlyOneChildElementByName(folder, NS_GATEWAY_MANAGEMENT, "Name");
                final String folderId = folder.getAttribute("folderId");

                assertEquals("Name", "Test Folder (updated)", XmlUtil.getTextValue(name));
                assertEquals("FolderId", Folder.ROOT_FOLDER_ID.toHexString(), folderId);
            }
        };

        putAndVerify( message, verifier, false );
        putAndVerify( message, verifier, true );
    }

    @Test
    public void testPutFolderInOldRoot() throws Exception {
        final String message = "<s:Envelope xmlns:s=\"http://www.w3.org/2003/05/soap-envelope\" xmlns:wsa=\"http://schemas.xmlsoap.org/ws/2004/08/addressing\" xmlns:wsman=\"http://schemas.dmtf.org/wbem/wsman/1/wsman.xsd\" xmlns:n1=\"http://ns.l7tech.com/2010/04/gateway-management\"><s:Header><wsa:Action s:mustUnderstand=\"true\">http://schemas.xmlsoap.org/ws/2004/09/transfer/Put</wsa:Action><wsa:To s:mustUnderstand=\"true\">http://127.0.0.1:8080/wsman</wsa:To><wsman:ResourceURI s:mustUnderstand=\"true\">http://ns.l7tech.com/2010/04/gateway-management/folders</wsman:ResourceURI><wsa:MessageID s:mustUnderstand=\"true\">uuid:afad2993-7d39-1d39-8002-481688002100</wsa:MessageID><wsa:ReplyTo><wsa:Address>http://schemas.xmlsoap.org/ws/2004/08/addressing/role/anonymous</wsa:Address></wsa:ReplyTo><wsman:SelectorSet><wsman:Selector Name=\"id\">"+new Goid(0,1)+"</wsman:Selector></wsman:SelectorSet><wsman:RequestEPR/></s:Header><s:Body> <n1:Folder xmlns:n1=\"http://ns.l7tech.com/2010/04/gateway-management\" folderId=\"-5002\" id=\""+new Goid(0,1)+"\" version=\"0\"><n1:Name>Test Folder (updated)</n1:Name></n1:Folder> </s:Body></s:Envelope>";

        final UnaryVoidThrows<Document,Exception> verifier = new UnaryVoidThrows<Document,Exception>(){
            @Override
            public void call( final Document result ) throws Exception {
                final Element soapBody = SoapUtil.getBodyElement(result);
                final Element folder = XmlUtil.findExactlyOneChildElementByName(soapBody, NS_GATEWAY_MANAGEMENT, "Folder");
                final Element name = XmlUtil.findExactlyOneChildElementByName(folder, NS_GATEWAY_MANAGEMENT, "Name");
                final String folderId = folder.getAttribute("folderId");
                assertEquals("Name", "Test Folder (updated)", XmlUtil.getTextValue(name));
                assertEquals("FolderId", Folder.ROOT_FOLDER_ID.toHexString(), folderId);
            }
        };

        putAndVerify( message, verifier, false );
        putAndVerify( message, verifier, true );
    }

    @Test
    public void testPutRootFolder() throws Exception {
        final String message = "<s:Envelope xmlns:s=\"http://www.w3.org/2003/05/soap-envelope\" xmlns:wsa=\"http://schemas.xmlsoap.org/ws/2004/08/addressing\" xmlns:wsman=\"http://schemas.dmtf.org/wbem/wsman/1/wsman.xsd\" xmlns:n1=\"http://ns.l7tech.com/2010/04/gateway-management\"><s:Header><wsa:Action s:mustUnderstand=\"true\">http://schemas.xmlsoap.org/ws/2004/09/transfer/Put</wsa:Action><wsa:To s:mustUnderstand=\"true\">http://127.0.0.1:8080/wsman</wsa:To><wsman:ResourceURI s:mustUnderstand=\"true\">http://ns.l7tech.com/2010/04/gateway-management/folders</wsman:ResourceURI><wsa:MessageID s:mustUnderstand=\"true\">uuid:afad2993-7d39-1d39-8002-481688002100</wsa:MessageID><wsa:ReplyTo><wsa:Address>http://schemas.xmlsoap.org/ws/2004/08/addressing/role/anonymous</wsa:Address></wsa:ReplyTo><wsman:SelectorSet><wsman:Selector Name=\"id\">"+new Goid(0,-5002)+"</wsman:Selector></wsman:SelectorSet><wsman:RequestEPR/></s:Header><s:Body> <n1:Folder xmlns:n1=\"http://ns.l7tech.com/2010/04/gateway-management\" id=\""+new Goid(0,-5002)+"\" version=\"0\"><n1:Name>Test Folder (updated)</n1:Name></n1:Folder> </s:Body></s:Envelope>";

        final UnaryVoidThrows<Document,Exception> verifier = new UnaryVoidThrows<Document,Exception>(){
            @Override
            public void call( final Document result ) throws Exception {
                final Element soapBody = SoapUtil.getBodyElement(result);
                final Element fault = XmlUtil.findExactlyOneChildElementByName(soapBody, NS_SOAP_ENVELOPE, "Fault");
                final Element detail = XmlUtil.findExactlyOneChildElementByName(fault, NS_SOAP_ENVELOPE, "Detail");
                final Element text = XmlUtil.findExactlyOneChildElementByName(detail, NS_SOAP_ENVELOPE, "Text");
                final String message = XmlUtil.getTextValue(text);
                assertEquals("Error message", "Cannot update root folder", message);
            }
        };

        putAndVerify( message, verifier, false );
        putAndVerify( message, verifier, true );
    }

    @Test
    public void testPutRootFolderOldOid() throws Exception {
        final String message = "<s:Envelope xmlns:s=\"http://www.w3.org/2003/05/soap-envelope\" xmlns:wsa=\"http://schemas.xmlsoap.org/ws/2004/08/addressing\" xmlns:wsman=\"http://schemas.dmtf.org/wbem/wsman/1/wsman.xsd\" xmlns:n1=\"http://ns.l7tech.com/2010/04/gateway-management\"><s:Header><wsa:Action s:mustUnderstand=\"true\">http://schemas.xmlsoap.org/ws/2004/09/transfer/Put</wsa:Action><wsa:To s:mustUnderstand=\"true\">http://127.0.0.1:8080/wsman</wsa:To><wsman:ResourceURI s:mustUnderstand=\"true\">http://ns.l7tech.com/2010/04/gateway-management/folders</wsman:ResourceURI><wsa:MessageID s:mustUnderstand=\"true\">uuid:afad2993-7d39-1d39-8002-481688002100</wsa:MessageID><wsa:ReplyTo><wsa:Address>http://schemas.xmlsoap.org/ws/2004/08/addressing/role/anonymous</wsa:Address></wsa:ReplyTo><wsman:SelectorSet><wsman:Selector Name=\"id\">-5002</wsman:Selector></wsman:SelectorSet><wsman:RequestEPR/></s:Header><s:Body> <n1:Folder xmlns:n1=\"http://ns.l7tech.com/2010/04/gateway-management\" id=\"-5002\" version=\"0\"><n1:Name>Test Folder (updated)</n1:Name></n1:Folder> </s:Body></s:Envelope>";

        final UnaryVoidThrows<Document,Exception> verifier = new UnaryVoidThrows<Document,Exception>(){
            @Override
            public void call( final Document result ) throws Exception {
                final Element soapBody = SoapUtil.getBodyElement(result);
                final Element fault = XmlUtil.findExactlyOneChildElementByName(soapBody, NS_SOAP_ENVELOPE, "Fault");
                final Element detail = XmlUtil.findExactlyOneChildElementByName(fault, NS_SOAP_ENVELOPE, "Detail");
                final Element text = XmlUtil.findExactlyOneChildElementByName(detail, NS_SOAP_ENVELOPE, "Text");
                final String message = XmlUtil.getTextValue(text);
                assertEquals("Error message", "Cannot update root folder", message);
            }
        };

        putAndVerify( message, verifier, false );
        putAndVerify( message, verifier, true );
    }

    @BugNumber(10947)
    @Test
    public void testPutIdentityProvider() throws Exception {
        final String message = "<s:Envelope xmlns:s=\"http://www.w3.org/2003/05/soap-envelope\" xmlns:wsa=\"http://schemas.xmlsoap.org/ws/2004/08/addressing\" xmlns:wsman=\"http://schemas.dmtf.org/wbem/wsman/1/wsman.xsd\" xmlns:l7=\"http://ns.l7tech.com/2010/04/gateway-management\"><s:Header><wsa:Action s:mustUnderstand=\"true\">http://schemas.xmlsoap.org/ws/2004/09/transfer/Put</wsa:Action><wsa:To s:mustUnderstand=\"true\">http://127.0.0.1:8080/wsman</wsa:To><wsman:ResourceURI s:mustUnderstand=\"true\">http://ns.l7tech.com/2010/04/gateway-management/identityProviders</wsman:ResourceURI><wsa:MessageID s:mustUnderstand=\"true\">uuid:afad2993-7d39-1d39-8002-481688002100</wsa:MessageID><wsa:ReplyTo><wsa:Address>http://schemas.xmlsoap.org/ws/2004/08/addressing/role/anonymous</wsa:Address></wsa:ReplyTo><wsman:SelectorSet><wsman:Selector Name=\"id\">0000000000000000fffffffffffffffd</wsman:Selector></wsman:SelectorSet><wsman:RequestEPR/></s:Header><s:Body> " +
                    "<l7:IdentityProvider id=\"0000000000000000fffffffffffffffd\" version=\"0\">\n" +
                    "    <l7:Name>LDAP (updated)</l7:Name>\n" +
                    "    <l7:IdentityProviderType>LDAP</l7:IdentityProviderType>\n" +
                    "    <l7:Properties>\n" +
                    "        <l7:Property key=\"groupMembershipCaseInsensitive\">\n" +
                    "            <l7:BooleanValue>false</l7:BooleanValue>\n" +
                    "        </l7:Property>\n" +
                    "        <l7:Property key=\"userLookupByCertMode\">\n" +
                    "            <l7:StringValue>Entire Certificate</l7:StringValue>\n" +
                    "        </l7:Property>\n" +
                    "        <l7:Property key=\"userCertificateUsage\">\n" +
                    "            <l7:StringValue>None</l7:StringValue>\n" +
                    "        </l7:Property>\n" +
                    "        <l7:Property key=\"adminEnabled\">\n" +
                    "            <l7:BooleanValue>false</l7:BooleanValue>\n" +
                    "        </l7:Property>\n" +
                    "    </l7:Properties>\n" +
                    "    <l7:Extension>\n" +
                    "        <l7:LdapIdentityProviderDetail>\n" +
                    "            <l7:SourceType>MicrosoftActiveDirectory</l7:SourceType>\n" +
                    "            <l7:ServerUrls><l7:StringValue>ldap://10.7.2.1</l7:StringValue></l7:ServerUrls>\n" +
                    "            <l7:UseSslClientAuthentication>true</l7:UseSslClientAuthentication>\n" +
                    "            <l7:SearchBase>cn=something</l7:SearchBase>\n" +
                    "            <l7:BindDn>browse</l7:BindDn>\n" +
                    "            <l7:UserMappings/>\n" +
                    "            <l7:GroupMappings/>\n" +
                    "        </l7:LdapIdentityProviderDetail>\n" +
                    "    </l7:Extension>\n" +
                    "</l7:IdentityProvider>\n" +
                    "</s:Body></s:Envelope>";

        final UnaryVoidThrows<Document,Exception> verifier = new UnaryVoidThrows<Document,Exception>(){
            @Override
            public void call( final Document result ) throws Exception {
                final Element soapBody = SoapUtil.getBodyElement(result);
                final Element folder = XmlUtil.findExactlyOneChildElementByName(soapBody, NS_GATEWAY_MANAGEMENT, "IdentityProvider");
                final Element name = XmlUtil.findExactlyOneChildElementByName(folder, NS_GATEWAY_MANAGEMENT, "Name");

                assertEquals("Name", "LDAP (updated)", XmlUtil.getTextValue(name));
            }
        };

        putAndVerify( message, verifier, false );
        putAndVerify( message, verifier, true );
    }

    @Test
    public void testPutInterfaceTag() throws Exception {
        final String message = "<s:Envelope xmlns:s=\"http://www.w3.org/2003/05/soap-envelope\" xmlns:wsa=\"http://schemas.xmlsoap.org/ws/2004/08/addressing\" xmlns:wsman=\"http://schemas.dmtf.org/wbem/wsman/1/wsman.xsd\" xmlns:n1=\"http://ns.l7tech.com/2010/04/gateway-management\"><s:Header><wsa:Action s:mustUnderstand=\"true\">http://schemas.xmlsoap.org/ws/2004/09/transfer/Put</wsa:Action><wsa:To s:mustUnderstand=\"true\">http://127.0.0.1:8080/wsman</wsa:To><wsman:ResourceURI s:mustUnderstand=\"true\">http://ns.l7tech.com/2010/04/gateway-management/interfaceTags</wsman:ResourceURI><wsa:MessageID s:mustUnderstand=\"true\">uuid:afad2993-7d39-1d39-8002-481688002100</wsa:MessageID><wsa:ReplyTo><wsa:Address>http://schemas.xmlsoap.org/ws/2004/08/addressing/role/anonymous</wsa:Address></wsa:ReplyTo><wsman:SelectorSet><wsman:Selector Name=\"name\">localhost</wsman:Selector></wsman:SelectorSet><wsman:RequestEPR/></s:Header><s:Body> <l7:InterfaceTag xmlns:l7=\"http://ns.l7tech.com/2010/04/gateway-management\" id=\"421aa90e-079f-3326-b649-4f812ad13e79\"><l7:Name>localhost</l7:Name><l7:AddressPatterns><l7:StringValue>127.0.0.1</l7:StringValue><l7:StringValue>127.0.0.2</l7:StringValue></l7:AddressPatterns></l7:InterfaceTag> </s:Body></s:Envelope>";

        final UnaryVoidThrows<Document,Exception> verifier = new UnaryVoidThrows<Document,Exception>(){
            @Override
            public void call( final Document result ) throws Exception {
                final Element soapBody = SoapUtil.getBodyElement(result);
                final Element interfaceTag = XmlUtil.findExactlyOneChildElementByName(soapBody, NS_GATEWAY_MANAGEMENT, "InterfaceTag");
                final Element name = XmlUtil.findExactlyOneChildElementByName(interfaceTag, NS_GATEWAY_MANAGEMENT, "Name");
                final Element addressPatterns = XmlUtil.findExactlyOneChildElementByName(interfaceTag, NS_GATEWAY_MANAGEMENT, "AddressPatterns");
                final List<Element> stringValues = XmlUtil.findChildElementsByName( addressPatterns, NS_GATEWAY_MANAGEMENT, "StringValue" );

                assertEquals("Interface tag id", "421aa90e-079f-3326-b649-4f812ad13e79", interfaceTag.getAttribute( "id" ));
                assertEquals("Interface tag name", "localhost", XmlUtil.getTextValue(name));
                assertEquals("Interface tag two patterns", 2L, (long)stringValues.size() );
                assertEquals("Interface tag ip pattern 1", "127.0.0.1", XmlUtil.getTextValue(stringValues.get( 0 )));
                assertEquals("Interface tag ip pattern 2", "127.0.0.2", XmlUtil.getTextValue(stringValues.get( 1 )));
            }
        };

        putAndVerify( message, verifier, false );
        putAndVerify( message, verifier, true );
    }

    @Test
    public void testPutJdbcConnection() throws Exception {
        final Goid goid = new Goid(0, 1);
        final String message = "<s:Envelope xmlns:s=\"http://www.w3.org/2003/05/soap-envelope\" xmlns:wsa=\"http://schemas.xmlsoap.org/ws/2004/08/addressing\" xmlns:wsman=\"http://schemas.dmtf.org/wbem/wsman/1/wsman.xsd\" xmlns:l7=\"http://ns.l7tech.com/2010/04/gateway-management\"><s:Header><wsa:Action s:mustUnderstand=\"true\">http://schemas.xmlsoap.org/ws/2004/09/transfer/Put</wsa:Action><wsa:To s:mustUnderstand=\"true\">http://127.0.0.1:8080/wsman</wsa:To><wsman:ResourceURI s:mustUnderstand=\"true\">http://ns.l7tech.com/2010/04/gateway-management/jdbcConnections</wsman:ResourceURI><wsa:MessageID s:mustUnderstand=\"true\">uuid:afad2993-7d39-1d39-8002-481688002100</wsa:MessageID><wsa:ReplyTo><wsa:Address>http://schemas.xmlsoap.org/ws/2004/08/addressing/role/anonymous</wsa:Address></wsa:ReplyTo><wsman:SelectorSet><wsman:Selector Name=\"id\">"+ goid.toString()+"</wsman:Selector></wsman:SelectorSet><wsman:RequestEPR/></s:Header><s:Body>" +
              "<l7:JDBCConnection id=\""+ goid.toString()+"\" version=\"0\">\n" +
              "    <l7:Name>A Test Connection (updated)</l7:Name>\n" +
              "    <l7:Enabled>false</l7:Enabled>\n" +
              "    <l7:Properties>\n" +
              "        <l7:Property key=\"maximumPoolSize\">\n" +
              "            <l7:IntegerValue>16</l7:IntegerValue>\n" +
              "        </l7:Property>\n" +
              "        <l7:Property key=\"minimumPoolSize\">\n" +
              "            <l7:IntegerValue>4</l7:IntegerValue>\n" +
              "        </l7:Property>\n" +
              "    </l7:Properties>\n" +
              "    <l7:Extension>\n" +
              "        <l7:DriverClass>com.mysql.jdbc.Driver</l7:DriverClass>\n" +
              "        <l7:JdbcUrl>jdbc:mysql://localhost:3306/ssg</l7:JdbcUrl>\n" +
              "        <l7:ConnectionProperties>\n" +
              "            <l7:Property key=\"user\">\n" +
              "                <l7:StringValue>username</l7:StringValue>\n" +
              "            </l7:Property>\n" +
              "            <l7:Property key=\"password\">\n" +
              "                <l7:StringValue>password</l7:StringValue>\n" +
              "            </l7:Property>\n" +
              "        </l7:ConnectionProperties>\n" +
              "    </l7:Extension>\n" +
              "</l7:JDBCConnection>" +
              "</s:Body></s:Envelope>";

        final UnaryVoidThrows<Document,Exception> verifier = new UnaryVoidThrows<Document,Exception>(){
            private int expectedVersion = 1;

            @Override
            public void call( final Document result ) throws Exception {
                final Element soapBody = SoapUtil.getBodyElement(result);
                final Element jdbcConnection = XmlUtil.findExactlyOneChildElementByName(soapBody, NS_GATEWAY_MANAGEMENT, "JDBCConnection");
                final Element jdbcConnectionName = XmlUtil.findExactlyOneChildElementByName(jdbcConnection, NS_GATEWAY_MANAGEMENT, "Name");
                final Element jdbcConnectionEnabled = XmlUtil.findExactlyOneChildElementByName(jdbcConnection, NS_GATEWAY_MANAGEMENT, "Enabled");
                final Element jdbcConnectionExtension = XmlUtil.findExactlyOneChildElementByName(jdbcConnection, NS_GATEWAY_MANAGEMENT, "Extension");
                final Element jdbcConnectionExtensionDriverClass = XmlUtil.findExactlyOneChildElementByName(jdbcConnectionExtension, NS_GATEWAY_MANAGEMENT, "DriverClass");
                final Element jdbcConnectionExtensionJdbcUrl = XmlUtil.findExactlyOneChildElementByName(jdbcConnectionExtension, NS_GATEWAY_MANAGEMENT, "JdbcUrl");

                assertEquals("JDBC connection id", goid.toString(), jdbcConnection.getAttribute( "id" ));
                assertEquals("JDBC connection version", Integer.toString( expectedVersion++ ), jdbcConnection.getAttribute( "version" ));
                assertEquals("JDBC connection name", "A Test Connection (updated)", XmlUtil.getTextValue(jdbcConnectionName));
                assertEquals("JDBC connection enabled", "false", XmlUtil.getTextValue(jdbcConnectionEnabled));
                assertEquals("JDBC connection extension driver class", "com.mysql.jdbc.Driver", XmlUtil.getTextValue(jdbcConnectionExtensionDriverClass));
                assertEquals("JDBC connection extension jdbc url", "jdbc:mysql://localhost:3306/ssg", XmlUtil.getTextValue(jdbcConnectionExtensionJdbcUrl));
            }
        };

        putAndVerify( message, verifier, false );
        putAndVerify( message, verifier, true );
    }

    @Test
    public void testPutJmsDestination() throws Exception {
        final Goid id = new Goid(0,1);
        final String idStr = id.toString();
        final String message = "<s:Envelope xmlns:s=\"http://www.w3.org/2003/05/soap-envelope\" xmlns:wsa=\"http://schemas.xmlsoap.org/ws/2004/08/addressing\" xmlns:wsman=\"http://schemas.dmtf.org/wbem/wsman/1/wsman.xsd\" xmlns:l7=\"http://ns.l7tech.com/2010/04/gateway-management\"><s:Header><wsa:Action s:mustUnderstand=\"true\">http://schemas.xmlsoap.org/ws/2004/09/transfer/Put</wsa:Action><wsa:To s:mustUnderstand=\"true\">http://127.0.0.1:8080/wsman</wsa:To><wsman:ResourceURI s:mustUnderstand=\"true\">http://ns.l7tech.com/2010/04/gateway-management/jmsDestinations</wsman:ResourceURI><wsa:MessageID s:mustUnderstand=\"true\">uuid:afad2993-7d39-1d39-8002-481688002100</wsa:MessageID><wsa:ReplyTo><wsa:Address>http://schemas.xmlsoap.org/ws/2004/08/addressing/role/anonymous</wsa:Address></wsa:ReplyTo><wsman:SelectorSet><wsman:Selector Name=\"id\">"+id+"</wsman:Selector></wsman:SelectorSet><wsman:RequestEPR/></s:Header><s:Body>" +
              "    <l7:JMSDestination id=\""+idStr+"\" version=\"0\">\n" +
              "        <l7:JMSDestinationDetail id=\""+idStr+"\" version=\"0\">\n" +
              "            <l7:Name>Test Endpoint 1</l7:Name>\n" +
              "            <l7:DestinationName>Test Endpoint</l7:DestinationName>\n" +
              "            <l7:Inbound>false</l7:Inbound>\n" +
              "            <l7:Enabled>true</l7:Enabled>\n" +
              "            <l7:Template>false</l7:Template>\n" +
              "            <l7:Properties>\n" +
              "                <l7:Property key=\"replyType\">\n" +
              "                    <l7:StringValue>AUTOMATIC</l7:StringValue>\n" +
              "                </l7:Property>\n" +
              "                <l7:Property key=\"outbound.MessageType\">\n" +
              "                    <l7:StringValue>AUTOMATIC</l7:StringValue>\n" +
              "                </l7:Property>\n" +
              "                <l7:Property key=\"useRequestCorrelationId\">\n" +
              "                    <l7:BooleanValue>false</l7:BooleanValue>\n" +
              "                </l7:Property>\n" +
              "            </l7:Properties>\n" +
              "        </l7:JMSDestinationDetail>\n" +
              "        <l7:JMSConnection id=\""+idStr+"\" version=\"0\">\n" +
              "            <l7:Template>false</l7:Template>\n" +
              "            <l7:Properties>\n" +
              "                <l7:Property key=\"jndi.initialContextFactoryClassname\">\n" +
              "                    <l7:StringValue>com.context.Classname</l7:StringValue>\n" +
              "                </l7:Property>\n" +
              "                <l7:Property key=\"jndi.providerUrl\">\n" +
              "                    <l7:StringValue>ldap://jndi</l7:StringValue>\n" +
              "                </l7:Property>\n" +
              "                <l7:Property key=\"queue.connectionFactoryName\">\n" +
              "                    <l7:StringValue>qcf</l7:StringValue>\n" +
              "                </l7:Property>\n" +
              "            </l7:Properties>\n" +
              "            <l7:ContextPropertiesTemplate/>\n" +
              "        </l7:JMSConnection>\n" +
              "    </l7:JMSDestination>\n" +
                "</s:Body></s:Envelope>";

        final UnaryVoidThrows<Document,Exception> verifier = new UnaryVoidThrows<Document,Exception>(){
            private int expectedVersion = 1;

            @Override
            public void call( final Document result ) throws Exception {
                final Element soapBody = SoapUtil.getBodyElement(result);
                final Element jmsDestination = XmlUtil.findExactlyOneChildElementByName(soapBody, NS_GATEWAY_MANAGEMENT, "JMSDestination");
                final Element jmsDestinationDetail = XmlUtil.findExactlyOneChildElementByName(jmsDestination, NS_GATEWAY_MANAGEMENT, "JMSDestinationDetail");
                final Element jmsDestinationDetailName = XmlUtil.findExactlyOneChildElementByName(jmsDestinationDetail, NS_GATEWAY_MANAGEMENT, "Name");

                final int version = expectedVersion++;

                assertEquals("JMS destination id", idStr, jmsDestination.getAttribute( "id" ));
                assertEquals("JMS destination version", Integer.toString( version ), jmsDestination.getAttribute( "version" ));
                assertEquals("JMS destination detail id", idStr , jmsDestinationDetail.getAttribute( "id" ));
                assertEquals("JMS destination detail version", Integer.toString( version ), jmsDestinationDetail.getAttribute( "version" ));
                assertEquals("JMS destination detail name", "Test Endpoint 1", XmlUtil.getTextValue(jmsDestinationDetailName));
            }
        };

        putAndVerify( message, verifier, false );
        putAndVerify( message, verifier, true );

        // Added for SSG-5693
        JmsEndpointManagerStub jmsManager = applicationContext.getBean( "jmsEndpointManager",  JmsEndpointManagerStub.class);
        JmsEndpoint endpoint = jmsManager.findByPrimaryKey(id);
        assertEquals("Password field should be ignored", "password", endpoint.getPassword());
    }
    @Test
    public void testPutJmsDestinationWithJndiPassword() throws Exception {
        final Goid id = new Goid(0,1);
        final String idStr = id.toString();
        final String message = "<s:Envelope xmlns:s=\"http://www.w3.org/2003/05/soap-envelope\" xmlns:wsa=\"http://schemas.xmlsoap.org/ws/2004/08/addressing\" xmlns:wsman=\"http://schemas.dmtf.org/wbem/wsman/1/wsman.xsd\" xmlns:l7=\"http://ns.l7tech.com/2010/04/gateway-management\"><s:Header><wsa:Action s:mustUnderstand=\"true\">http://schemas.xmlsoap.org/ws/2004/09/transfer/Put</wsa:Action><wsa:To s:mustUnderstand=\"true\">http://127.0.0.1:8080/wsman</wsa:To><wsman:ResourceURI s:mustUnderstand=\"true\">http://ns.l7tech.com/2010/04/gateway-management/jmsDestinations</wsman:ResourceURI><wsa:MessageID s:mustUnderstand=\"true\">uuid:afad2993-7d39-1d39-8002-481688002100</wsa:MessageID><wsa:ReplyTo><wsa:Address>http://schemas.xmlsoap.org/ws/2004/08/addressing/role/anonymous</wsa:Address></wsa:ReplyTo><wsman:SelectorSet><wsman:Selector Name=\"id\">"+id+"</wsman:Selector></wsman:SelectorSet><wsman:RequestEPR/></s:Header><s:Body>" +
                "    <l7:JMSDestination id=\""+idStr+"\" version=\"0\">\n" +
                "        <l7:JMSDestinationDetail id=\""+idStr+"\" version=\"0\">\n" +
                "            <l7:Name>Test Endpoint 1</l7:Name>\n" +
                "            <l7:DestinationName>Test Endpoint</l7:DestinationName>\n" +
                "            <l7:Inbound>false</l7:Inbound>\n" +
                "            <l7:Enabled>true</l7:Enabled>\n" +
                "            <l7:Template>false</l7:Template>\n" +
                "            <l7:Properties>\n" +
                "                <l7:Property key=\"replyType\">\n" +
                "                    <l7:StringValue>AUTOMATIC</l7:StringValue>\n" +
                "                </l7:Property>\n" +
                "                <l7:Property key=\"outbound.MessageType\">\n" +
                "                    <l7:StringValue>AUTOMATIC</l7:StringValue>\n" +
                "                </l7:Property>\n" +
                "                <l7:Property key=\"useRequestCorrelationId\">\n" +
                "                    <l7:BooleanValue>false</l7:BooleanValue>\n" +
                "                </l7:Property>\n" +
                "            </l7:Properties>\n" +
                "        </l7:JMSDestinationDetail>\n" +
                "        <l7:JMSConnection id=\""+idStr+"\" version=\"0\">\n" +
                "            <l7:Template>false</l7:Template>\n" +
                "            <l7:Properties>\n" +
                "                <l7:Property key=\"jndi.initialContextFactoryClassname\">\n" +
                "                    <l7:StringValue>com.context.Classname</l7:StringValue>\n" +
                "                </l7:Property>\n" +
                "                <l7:Property key=\"jndi.providerUrl\">\n" +
                "                    <l7:StringValue>ldap://jndi</l7:StringValue>\n" +
                "                </l7:Property>\n" +
                "                <l7:Property key=\"queue.connectionFactoryName\">\n" +
                "                    <l7:StringValue>qcf</l7:StringValue>\n" +
                "                </l7:Property>\n" +
                "            </l7:Properties>\n" +
                "            <l7:ContextPropertiesTemplate/>\n" +
                "        </l7:JMSConnection>\n" +
                "    </l7:JMSDestination>\n" +
                "</s:Body></s:Envelope>";

        final UnaryVoidThrows<Document,Exception> verifier = new UnaryVoidThrows<Document,Exception>(){
            private int expectedVersion = 1;

            @Override
            public void call( final Document result ) throws Exception {
                final Element soapBody = SoapUtil.getBodyElement(result);
                final Element jmsDestination = XmlUtil.findExactlyOneChildElementByName(soapBody, NS_GATEWAY_MANAGEMENT, "JMSDestination");
                final Element jmsDestinationDetail = XmlUtil.findExactlyOneChildElementByName(jmsDestination, NS_GATEWAY_MANAGEMENT, "JMSDestinationDetail");
                final Element jmsDestinationDetailName = XmlUtil.findExactlyOneChildElementByName(jmsDestinationDetail, NS_GATEWAY_MANAGEMENT, "Name");

                final int version = expectedVersion++;

                assertEquals("JMS destination id", idStr, jmsDestination.getAttribute( "id" ));
                assertEquals("JMS destination version", Integer.toString( version ), jmsDestination.getAttribute( "version" ));
                assertEquals("JMS destination detail id", idStr , jmsDestinationDetail.getAttribute( "id" ));
                assertEquals("JMS destination detail version", Integer.toString( version ), jmsDestinationDetail.getAttribute( "version" ));
                assertEquals("JMS destination detail name", "Test Endpoint 1", XmlUtil.getTextValue(jmsDestinationDetailName));
            }
        };


        putAndVerify( message, verifier, false );
        putAndVerify( message, verifier, true );

        // Added for SSG-5693
        JmsEndpointManagerStub jmsManager = applicationContext.getBean( "jmsEndpointManager",  JmsEndpointManagerStub.class);
        JmsEndpoint endpoint = jmsManager.findByPrimaryKey(id);
        assertEquals("Password field should be ignored", "password", endpoint.getPassword());

        // Added for SSG-7449
        JmsConnectionManagerStub jmsConnectionManager = applicationContext.getBean( "jmsConnectionManager",  JmsConnectionManagerStub.class);
        JmsConnection connection = jmsConnectionManager.findByPrimaryKey(id);
        assertEquals("JNDI Password field should be ignored", "jndi-password", connection.properties().getProperty("java.naming.security.credentials"));
    }

    @Test
    public void testPutActiveConnector() throws Exception {
        final String id = new Goid(0,2).toString();
        final String message = "<s:Envelope xmlns:s=\"http://www.w3.org/2003/05/soap-envelope\" xmlns:wsa=\"http://schemas.xmlsoap.org/ws/2004/08/addressing\" xmlns:wsman=\"http://schemas.dmtf.org/wbem/wsman/1/wsman.xsd\" xmlns:l7=\"http://ns.l7tech.com/2010/04/gateway-management\"><s:Header><wsa:Action s:mustUnderstand=\"true\">http://schemas.xmlsoap.org/ws/2004/09/transfer/Put</wsa:Action><wsa:To s:mustUnderstand=\"true\">http://127.0.0.1:8080/wsman</wsa:To><wsman:ResourceURI s:mustUnderstand=\"true\">http://ns.l7tech.com/2010/04/gateway-management/activeConnectors</wsman:ResourceURI><wsa:MessageID s:mustUnderstand=\"true\">uuid:afad2993-7d39-1d39-8002-481688002100</wsa:MessageID><wsa:ReplyTo><wsa:Address>http://schemas.xmlsoap.org/ws/2004/08/addressing/role/anonymous</wsa:Address></wsa:ReplyTo><wsman:SelectorSet><wsman:Selector Name=\"id\">"+id+"</wsman:Selector></wsman:SelectorSet><wsman:RequestEPR/></s:Header><s:Body>" +
                "    <l7:ActiveConnector id=\""+id+"\" version=\"0\">\n" +
                "        <l7:Name>Test SFTP 1</l7:Name>\n" +
                "        <l7:Enabled>false</l7:Enabled>\n" +
                "        <l7:Type>SFTP</l7:Type>\n" +
                "        <l7:Properties>\n" +
                "            <l7:Property key=\"SftpHost\">\n" +
                "                <l7:StringValue>host</l7:StringValue>\n" +
                "            </l7:Property>\n" +
                "            <l7:Property key=\"SftpPort\">\n" +
                "                <l7:StringValue>1234</l7:StringValue>\n" +
                "            </l7:Property>\n" +
                "            <l7:Property key=\"SftpSecurePasswordOid\">\n" +
                "                <l7:StringValue>1234</l7:StringValue>\n" +
                "            </l7:Property>\n" +
                "            <l7:Property key=\"SftpUsername\">\n" +
                "                <l7:StringValue>user</l7:StringValue>\n" +
                "            </l7:Property>\n" +
                "            <l7:Property key=\"SftpDirectory\">\n" +
                "                <l7:StringValue>dir</l7:StringValue>\n" +
                "            </l7:Property>\n" +
                "        </l7:Properties>\n" +
                "    </l7:ActiveConnector>" +
                "</s:Body></s:Envelope>";

        final UnaryVoidThrows<Document,Exception> verifier = new UnaryVoidThrows<Document,Exception>(){
            private int expectedVersion = 1;

            @Override
            public void call( final Document result ) throws Exception {
                final Element soapBody = SoapUtil.getBodyElement(result);
                final Element connector = XmlUtil.findExactlyOneChildElementByName(soapBody, NS_GATEWAY_MANAGEMENT, "ActiveConnector");
                final Element connectorName = XmlUtil.findExactlyOneChildElementByName(connector, NS_GATEWAY_MANAGEMENT, "Name");

                final int version = expectedVersion++;

                assertEquals("Active connector id", id, connector.getAttribute( "id" ));
                assertEquals("Active connector version", Integer.toString( version ), connector.getAttribute( "version" ));
                assertEquals("Active connector name", "Test SFTP 1", XmlUtil.getTextValue(connectorName));
            }
        };

        putAndVerify( message, verifier, false );
        putAndVerify( message, verifier, true );
    }

    @BugId("SSG-7426")
    @Test
    public void testPutActiveConnectorNoProperties() throws Exception {
        final String id = new Goid(0,2).toString();
        final String message = "<s:Envelope xmlns:s=\"http://www.w3.org/2003/05/soap-envelope\" xmlns:wsa=\"http://schemas.xmlsoap.org/ws/2004/08/addressing\" xmlns:wsman=\"http://schemas.dmtf.org/wbem/wsman/1/wsman.xsd\" xmlns:l7=\"http://ns.l7tech.com/2010/04/gateway-management\"><s:Header><wsa:Action s:mustUnderstand=\"true\">http://schemas.xmlsoap.org/ws/2004/09/transfer/Put</wsa:Action><wsa:To s:mustUnderstand=\"true\">http://127.0.0.1:8080/wsman</wsa:To><wsman:ResourceURI s:mustUnderstand=\"true\">http://ns.l7tech.com/2010/04/gateway-management/activeConnectors</wsman:ResourceURI><wsa:MessageID s:mustUnderstand=\"true\">uuid:afad2993-7d39-1d39-8002-481688002100</wsa:MessageID><wsa:ReplyTo><wsa:Address>http://schemas.xmlsoap.org/ws/2004/08/addressing/role/anonymous</wsa:Address></wsa:ReplyTo><wsman:SelectorSet><wsman:Selector Name=\"id\">"+id+"</wsman:Selector></wsman:SelectorSet><wsman:RequestEPR/></s:Header><s:Body>" +
                "    <l7:ActiveConnector id=\""+id+"\" version=\"0\">\n" +
                "        <l7:Name>Test SFTP 1111</l7:Name>\n" +
                "        <l7:Enabled>false</l7:Enabled>\n" +
                "        <l7:Type>SFTP</l7:Type>\n" +
                "    </l7:ActiveConnector>" +
                "</s:Body></s:Envelope>";

        final UnaryVoidThrows<Document,Exception> verifier = new UnaryVoidThrows<Document,Exception>(){
            private int expectedVersion = 1;

            @Override
            public void call( final Document result ) throws Exception {
                final Element soapBody = SoapUtil.getBodyElement(result);
                final Element connector = XmlUtil.findExactlyOneChildElementByName(soapBody, NS_GATEWAY_MANAGEMENT, "ActiveConnector");
                final Element connectorName = XmlUtil.findExactlyOneChildElementByName(connector, NS_GATEWAY_MANAGEMENT, "Name");

                final int version = expectedVersion++;

                assertEquals("Active connector id", id, connector.getAttribute( "id" ));
                assertEquals("Active connector version", Integer.toString( version ), connector.getAttribute( "version" ));
                assertEquals("Active connector name", "Test SFTP 1111", XmlUtil.getTextValue(connectorName));

            }
        };

        putAndVerify( message, verifier, false );
        putAndVerify( message, verifier, true );
    }

    @Test
    public void testPutPolicy() throws Exception {
        final String message = "<s:Envelope xmlns:s=\"http://www.w3.org/2003/05/soap-envelope\" xmlns:wsa=\"http://schemas.xmlsoap.org/ws/2004/08/addressing\" xmlns:wsman=\"http://schemas.dmtf.org/wbem/wsman/1/wsman.xsd\" xmlns:l7=\"http://ns.l7tech.com/2010/04/gateway-management\"><s:Header><wsa:Action s:mustUnderstand=\"true\">http://schemas.xmlsoap.org/ws/2004/09/transfer/Put</wsa:Action><wsa:To s:mustUnderstand=\"true\">http://127.0.0.1:8080/wsman</wsa:To><wsman:ResourceURI s:mustUnderstand=\"true\">http://ns.l7tech.com/2010/04/gateway-management/policies</wsman:ResourceURI><wsa:MessageID s:mustUnderstand=\"true\">uuid:afad2993-7d39-1d39-8002-481688002100</wsa:MessageID><wsa:ReplyTo><wsa:Address>http://schemas.xmlsoap.org/ws/2004/08/addressing/role/anonymous</wsa:Address></wsa:ReplyTo><wsman:SelectorSet><wsman:Selector Name=\"id\">"+new Goid(0,1)+"</wsman:Selector></wsman:SelectorSet><wsman:RequestEPR/></s:Header><s:Body>" +
                "<l7:Policy id=\""+new Goid(0,1)+"\" guid=\"c4ca4238-a0b9-3382-8dcc-509a6f75849b\" version=\"0\">\n" +
                "    <l7:PolicyDetail id=\""+new Goid(0,1)+"\" guid=\"c4ca4238-a0b9-3382-8dcc-509a6f75849b\" version=\"0\">\n" +
                "        <l7:Name>Test Policy 1</l7:Name>\n" +
                "        <l7:PolicyType>Include</l7:PolicyType>\n" +
                "        <l7:Properties>\n" +
                "            <l7:Property key=\"revision\">\n" +
                "                <l7:LongValue>0</l7:LongValue>\n" +
                "            </l7:Property>\n" +
                "            <l7:Property key=\"soap\">\n" +
                "                <l7:BooleanValue>true</l7:BooleanValue>\n" +
                "            </l7:Property>\n" +
                "        </l7:Properties>\n" +
                "    </l7:PolicyDetail>\n" +
                "    <l7:Resources>\n" +
                "        <l7:ResourceSet tag=\"policy\">\n" +
                "            <l7:Resource type=\"policy\">&lt;wsp:Policy xmlns:L7p=\"http://www.layer7tech.com/ws/policy\" xmlns:wsp=\"http://schemas.xmlsoap.org/ws/2002/12/policy\"&gt;&lt;wsp:All wsp:Usage=\"Required\"&gt;&lt;L7p:AuditAssertion/&gt;&lt;/wsp:All&gt;&lt;/wsp:Policy&gt;</l7:Resource>\n" +
                "        </l7:ResourceSet>\n" +
                "    </l7:Resources>\n" +
                "</l7:Policy>\n" +
                "</s:Body></s:Envelope>";

        final UnaryVoidThrows<Document,Exception> verifier = new UnaryVoidThrows<Document,Exception>(){
            private int expectedVersion = 1;

            @Override
            public void call( final Document result ) throws Exception {
                final Element soapBody = SoapUtil.getBodyElement(result);
                final Element policy = XmlUtil.findExactlyOneChildElementByName(soapBody, NS_GATEWAY_MANAGEMENT, "Policy");
                final Element policyDetail = XmlUtil.findExactlyOneChildElementByName(policy, NS_GATEWAY_MANAGEMENT, "PolicyDetail");
                final Element policyDetailName = XmlUtil.findExactlyOneChildElementByName(policyDetail, NS_GATEWAY_MANAGEMENT, "Name");

                final int version = expectedVersion++;

                assertEquals("Policy id", new Goid(0,1).toHexString(), policy.getAttribute( "id" ));
                assertEquals("Policy version", Integer.toString( version ), policy.getAttribute( "version" ));
                assertEquals("Policy detail id", new Goid(0,1).toHexString(), policyDetail.getAttribute( "id" ));
                assertEquals("Policy detail version", Integer.toString( version ), policyDetail.getAttribute( "version" ));
                assertEquals("Policy detail name", "Test Policy 1", XmlUtil.getTextValue(policyDetailName));
            }
        };

        putAndVerify( message, verifier, false );
        putAndVerify( message, verifier, true );
    }

    @Test
    public void testPutPrivateKey() throws Exception {
        final String message =
                "<s:Envelope xmlns:s=\"http://www.w3.org/2003/05/soap-envelope\" xmlns:wsa=\"http://schemas.xmlsoap.org/ws/2004/08/addressing\" xmlns:wsman=\"http://schemas.dmtf.org/wbem/wsman/1/wsman.xsd\" xmlns:l7=\"http://ns.l7tech.com/2010/04/gateway-management\"><s:Header><wsa:Action s:mustUnderstand=\"true\">http://schemas.xmlsoap.org/ws/2004/09/transfer/Put</wsa:Action><wsa:To s:mustUnderstand=\"true\">http://127.0.0.1:8080/wsman</wsa:To><wsman:ResourceURI s:mustUnderstand=\"true\">http://ns.l7tech.com/2010/04/gateway-management/privateKeys</wsman:ResourceURI><wsa:MessageID s:mustUnderstand=\"true\">uuid:afad2993-7d39-1d39-8002-481688002100</wsa:MessageID><wsa:ReplyTo><wsa:Address>http://schemas.xmlsoap.org/ws/2004/08/addressing/role/anonymous</wsa:Address></wsa:ReplyTo><wsman:SelectorSet><wsman:Selector Name=\"id\">-1:bob</wsman:Selector></wsman:SelectorSet><wsman:RequestEPR/></s:Header><s:Body>\n" +
                "<PrivateKey xmlns=\"http://ns.l7tech.com/2010/04/gateway-management\" keystoreId=\"-1\" alias=\"bob\" id=\"-1:bob\">\n" +
                        "    <CertificateChain>\n" +
                        "        <CertificateData>\n" +
                        "            <Encoded>MIIDDDCCAfSgAwIBAgIQM6YEf7FVYx/tZyEXgVComTANBgkqhkiG9w0BAQUFADAwMQ4wDAYDVQQKDAVPQVNJUzEeMBwGA1UEAwwVT0FTSVMgSW50ZXJvcCBUZXN0IENBMB4XDTA1MDMxOTAwMDAwMFoXDTE4MDMxOTIzNTk1OVowQjEOMAwGA1UECgwFT0FTSVMxIDAeBgNVBAsMF09BU0lTIEludGVyb3AgVGVzdCBDZXJ0MQ4wDAYDVQQDDAVBbGljZTCBnzANBgkqhkiG9w0BAQEFAAOBjQAwgYkCgYEAoqi99By1VYo0aHrkKCNT4DkIgPL/SgahbeKdGhrbu3K2XG7arfD9tqIBIKMfrX4Gp90NJa85AV1yiNsEyvq+mUnMpNcKnLXLOjkTmMCqDYbbkehJlXPnaWLzve+mW0pJdPxtf3rbD4PS/cBQIvtpjmrDAU8VsZKT8DN5Kyz+EZsCAwEAAaOBkzCBkDAJBgNVHRMEAjAAMDMGA1UdHwQsMCowKKImhiRodHRwOi8vaW50ZXJvcC5iYnRlc3QubmV0L2NybC9jYS5jcmwwDgYDVR0PAQH/BAQDAgSwMB0GA1UdDgQWBBQK4l0TUHZ1QV3V2QtlLNDm+PoxiDAfBgNVHSMEGDAWgBTAnSj8wes1oR3WqqqgHBpNwkkPDzANBgkqhkiG9w0BAQUFAAOCAQEABTqpOpvW+6yrLXyUlP2xJbEkohXHI5OWwKWleOb9hlkhWntUalfcFOJAgUyH30TTpHldzx1+vK2LPzhoUFKYHE1IyQvokBN2JjFO64BQukCKnZhldLRPxGhfkTdxQgdf5rCK/wh3xVsZCNTfuMNmlAM6lOAg8QduDah3WFZpEA0s2nwQaCNQTNMjJC8tav1CBr6+E5FAmwPXP7pJxn9Fw9OXRyqbRA4v2y7YpbGkG2GI9UvOHw6SGvf4FRSthMMO35YbpikGsLix3vAsXWWi4rwfVOYzQK0OFPNi9RMCUdSH06m9uLWckiCxjos0FQODZE9l4ATGy9s9hNVwryOJTw==</Encoded>\n" +
                        "        </CertificateData>\n" +
                        "        <CertificateData>\n" +
                        "            <Encoded>MIIDizCCAnOgAwIBAgIQWaCxRe3INcSU8VNJ4/HerDANBgkqhkiG9w0BAQUFADAyMQ4wDAYDVQQKDAVPQVNJUzEgMB4GA1UEAwwXT0FTSVMgSW50ZXJvcCBUZXN0IFJvb3QwHhcNMDUwMzE5MDAwMDAwWhcNMTkwMzE5MjM1OTU5WjAwMQ4wDAYDVQQKDAVPQVNJUzEeMBwGA1UEAwwVT0FTSVMgSW50ZXJvcCBUZXN0IENBMIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAmR2GR3IduCfoZfvmwYpepKNZN6iaDcm4JmqqC3nN5NiuQ4ROq2YCRhG90QW8puhsO6XaRiRO6WQQpwdtm/tgseDAAdw0bMPWrnjaFhgFlaEB0eK5fu9UiCPGkwurWNc8EQlk2r71uCwOx6BYGFsnSnBEfj64zoVri2olksXc2aos6urhujP6zvixsCxfo8Jq2v1yLUZpDaiTp2GfyDMSZKROcBz4FnEIN7yKZDMYpHSx2SmcwmQnjeeAx1EH876+PpycsbJwStt3lIYchk5vWqJSZzN7PElEgzLWv8QeWZ0Zb8wteQyWrG5wN2FCTcqF3W29FBeZig6u5Y3mibwDYQIDAQABo4GeMIGbMBIGA1UdEwEB/wQIMAYBAf8CAQAwNQYDVR0fBC4wLDAqoiiGJmh0dHA6Ly9pbnRlcm9wLmJidGVzdC5uZXQvY3JsL3Jvb3QuY3JsMA4GA1UdDwEB/wQEAwIBBjAdBgNVHQ4EFgQUwJ0o/MHrNaEd1qqqoBwaTcJJDw8wHwYDVR0jBBgwFoAU3/6RlcdWSCY9wNw5PcYJ90z6SOIwDQYJKoZIhvcNAQEFBQADggEBADvsOGOmhnxjwW2+2c17/W7o4BolmqqlVFppFyEB4pUd+kqJ3XFiyVxweVwGdJfpUQLKP/KBzpqo4D11ttMaE2ioat0RUGylAl9PG/yalOH/vMgFq4XkhokoHPPD1tUbiuY8+pD+5jXR0NNj25yv7iSutZ7xA7bcMx+RQpDO9Mzhlk03SZt5FjsLrimLiEOtkTkBt8Gw1wCu253+Bt5JHboBhgEa9hTmdQ3hYqO/q54Gymmd/NsNCxZDbUxVqu/XzBxZer6AQ4domv5fc9efCOk0k06aMmYjKXEYI5i9OqutWu442ZXJV6lnWKZ1akFi/sA4DNnYPrz825+hzOeesBI=</Encoded>\n" +
                        "        </CertificateData>\n" +
                        "    </CertificateChain>\n" +
                        "</PrivateKey>\n" +
                "</s:Body></s:Envelope>";

        final UnaryVoidThrows<Document,Exception> verifier = new UnaryVoidThrows<Document,Exception>(){
            @Override
            public void call( final Document result ) throws Exception {
                final Element soapBody = SoapUtil.getBodyElement(result);
                final Element privateKey = XmlUtil.findExactlyOneChildElementByName(soapBody, NS_GATEWAY_MANAGEMENT, "PrivateKey");
                final Element certificateChain = XmlUtil.findExactlyOneChildElementByName(privateKey, NS_GATEWAY_MANAGEMENT, "CertificateChain");
                final Element certificateData = XmlUtil.findFirstChildElementByName(certificateChain, NS_GATEWAY_MANAGEMENT, "CertificateData");
                final Element subjectName = XmlUtil.findFirstChildElementByName(certificateData, NS_GATEWAY_MANAGEMENT, "SubjectName");

                assertEquals("PrivateKey id", "00000000000000000000000000000000:bob", privateKey.getAttribute( "id" ));
                assertEquals("PrivateKey cert chain [0] subject", URLEncoder.encode("CN=Alice,OU=OASIS Interop Test Cert,O=OASIS", "UTF-8"), XmlUtil.getTextValue( subjectName ));
            }
        };

        putAndVerify( message, verifier, false );
        putAndVerify( message, verifier, true );
    }

    @Test
    public void testPutResourceDocument() throws Exception {
        final String message = "<s:Envelope xmlns:s=\"http://www.w3.org/2003/05/soap-envelope\" xmlns:wsa=\"http://schemas.xmlsoap.org/ws/2004/08/addressing\" xmlns:wsman=\"http://schemas.dmtf.org/wbem/wsman/1/wsman.xsd\" xmlns:l7=\"http://ns.l7tech.com/2010/04/gateway-management\"><s:Header><wsa:Action s:mustUnderstand=\"true\">http://schemas.xmlsoap.org/ws/2004/09/transfer/Put</wsa:Action><wsa:To s:mustUnderstand=\"true\">http://127.0.0.1:8080/wsman</wsa:To><wsman:ResourceURI s:mustUnderstand=\"true\">http://ns.l7tech.com/2010/04/gateway-management/resources</wsman:ResourceURI><wsa:MessageID s:mustUnderstand=\"true\">uuid:afad2993-7d39-1d39-8002-481688002100</wsa:MessageID><wsa:ReplyTo><wsa:Address>http://schemas.xmlsoap.org/ws/2004/08/addressing/role/anonymous</wsa:Address></wsa:ReplyTo><wsman:SelectorSet><wsman:Selector Name=\"id\">"+new Goid(0,1)+"</wsman:Selector></wsman:SelectorSet><wsman:RequestEPR/></s:Header><s:Body>" +
                "    <l7:ResourceDocument id=\""+new Goid(0,1)+"\" version=\"0\">\n" +
                "        <l7:Resource sourceUrl=\"books2.xsd\" type=\"xmlschema\">&lt;xs:schema xmlns:xs=\"http://www.w3.org/2001/XMLSchema\"&gt;&lt;xs:element name=\"book\" type=\"xs:string\"/&gt;&lt;/xs:schema&gt;</l7:Resource>\n" +
                "    </l7:ResourceDocument>\n" +
                "</s:Body></s:Envelope>";

        final UnaryVoidThrows<Document,Exception> verifier = new UnaryVoidThrows<Document,Exception>(){
            private int expectedVersion = 1;

            @Override
            public void call( final Document result ) throws Exception {
                final Element soapBody = SoapUtil.getBodyElement(result);
                final Element resourceDocument = XmlUtil.findExactlyOneChildElementByName(soapBody, NS_GATEWAY_MANAGEMENT, "ResourceDocument");
                final Element resource = XmlUtil.findExactlyOneChildElementByName(resourceDocument, NS_GATEWAY_MANAGEMENT, "Resource");

                assertEquals("Resource document id", new Goid(0,1).toString(), resourceDocument.getAttribute( "id" ));
                assertEquals("Resource document version", Integer.toString( expectedVersion++ ), resourceDocument.getAttribute( "version" ));
                assertEquals("Resource document resource sourceUrl", "books2.xsd", resource.getAttribute( "sourceUrl" ));
            }
        };

        putAndVerify( message, verifier, false );
        putAndVerify( message, verifier, true );
    }

    @Test
    public void testPutService() throws Exception {
        final String message = "<s:Envelope xmlns:s=\"http://www.w3.org/2003/05/soap-envelope\" xmlns:wsa=\"http://schemas.xmlsoap.org/ws/2004/08/addressing\" xmlns:wsman=\"http://schemas.dmtf.org/wbem/wsman/1/wsman.xsd\" xmlns:l7=\"http://ns.l7tech.com/2010/04/gateway-management\"><s:Header><wsa:Action s:mustUnderstand=\"true\">http://schemas.xmlsoap.org/ws/2004/09/transfer/Put</wsa:Action><wsa:To s:mustUnderstand=\"true\">http://127.0.0.1:8080/wsman</wsa:To><wsman:ResourceURI s:mustUnderstand=\"true\">http://ns.l7tech.com/2010/04/gateway-management/services</wsman:ResourceURI><wsa:MessageID s:mustUnderstand=\"true\">uuid:afad2993-7d39-1d39-8002-481688002100</wsa:MessageID><wsa:ReplyTo><wsa:Address>http://schemas.xmlsoap.org/ws/2004/08/addressing/role/anonymous</wsa:Address></wsa:ReplyTo><wsman:SelectorSet><wsman:Selector Name=\"id\">"+new Goid(0,2)+"</wsman:Selector></wsman:SelectorSet><wsman:RequestEPR/></s:Header><s:Body>" +
                "<l7:Service id=\""+new Goid(0,2)+"\" version=\"1\">\n" +
                "            <l7:ServiceDetail id=\""+new Goid(0,2)+"\" version=\"1\">\n" +
                "                <l7:Name>Test Service 2</l7:Name>\n" +
                "                <l7:Enabled>true</l7:Enabled>\n" +
                "                <l7:ServiceMappings>\n" +
                "                    <l7:HttpMapping>\n" +
                "                        <l7:Verbs>\n" +
                "                            <l7:Verb>POST</l7:Verb>\n" +
                "                        </l7:Verbs>\n" +
                "                    </l7:HttpMapping>\n" +
                "                    <l7:SoapMapping>\n" +
                "                        <l7:Lax>false</l7:Lax>\n" +
                "                    </l7:SoapMapping>\n" +
                "                </l7:ServiceMappings>\n" +
                "                <l7:Properties>\n" +
                "                    <l7:Property key=\"policyRevision\">\n" +
                "                        <l7:LongValue>0</l7:LongValue>\n" +
                "                    </l7:Property>\n" +
                "                    <l7:Property key=\"wssProcessingEnabled\">\n" +
                "                        <l7:BooleanValue>true</l7:BooleanValue>\n" +
                "                    </l7:Property>\n" +
                "                    <l7:Property key=\"soap\">\n" +
                "                        <l7:BooleanValue>true</l7:BooleanValue>\n" +
                "                    </l7:Property>\n" +
                "                    <l7:Property key=\"soapVersion\">\n" +
                "                        <l7:StringValue>1.2</l7:StringValue>\n" +
                "                    </l7:Property>\n" +
                "                    <l7:Property key=\"internal\">\n" +
                "                        <l7:BooleanValue>false</l7:BooleanValue>\n" +
                "                    </l7:Property>\n" +
                "                </l7:Properties>\n" +
                "            </l7:ServiceDetail>\n" +
                "            <l7:Resources>\n" +
                "                <l7:ResourceSet tag=\"policy\">\n" +
                "                    <l7:Resource type=\"policy\">&lt;wsp:Policy xmlns:L7p=\"http://www.layer7tech.com/ws/policy\" xmlns:wsp=\"http://schemas.xmlsoap.org/ws/2002/12/policy\"&gt;\n" +
                "    &lt;wsp:All wsp:Usage=\"Required\"&gt;\n" +
                "        &lt;L7p:EchoRoutingAssertion/&gt;\n" +
                "    &lt;/wsp:All&gt;\n" +
                "&lt;/wsp:Policy&gt;</l7:Resource>\n" +
                "                </l7:ResourceSet>\n" +
                "                <l7:ResourceSet\n" +
                "                    rootUrl=\"http://localhost:8080/test.wsdl\" tag=\"wsdl\">\n" +
                "                    <l7:Resource\n" +
                "                        sourceUrl=\"http://localhost:8080/test.wsdl\" type=\"wsdl\">&lt;wsdl:definitions xmlns:wsdl=\"http://schemas.xmlsoap.org/wsdl/\" targetNamespace=\"http://warehouse.acme.com/ws\"/&gt;</l7:Resource>\n" +
                "                </l7:ResourceSet>\n" +
                "            </l7:Resources>\n" +
                "        </l7:Service>" +
                "</s:Body></s:Envelope>";

        final UnaryVoidThrows<Document,Exception> verifier = new UnaryVoidThrows<Document,Exception>(){
            private int expectedVersion = 2;

            @Override
            public void call( final Document result ) throws Exception {
                final Element soapBody = SoapUtil.getBodyElement(result);
                final Element service = XmlUtil.findExactlyOneChildElementByName(soapBody, NS_GATEWAY_MANAGEMENT, "Service");
                final Element serviceDetail = XmlUtil.findExactlyOneChildElementByName(service, NS_GATEWAY_MANAGEMENT, "ServiceDetail");
                final Element serviceDetailName = XmlUtil.findExactlyOneChildElementByName(serviceDetail, NS_GATEWAY_MANAGEMENT, "Name");
                final Element properties = XmlUtil.findExactlyOneChildElementByName(serviceDetail, NS_GATEWAY_MANAGEMENT, "Properties");

                final int version = expectedVersion++;

                assertEquals("Service id", new Goid(0,2).toHexString(), service.getAttribute( "id" ));
                assertEquals("Service version", Integer.toString( version ), service.getAttribute( "version" ));
                assertEquals("Service detail id", new Goid(0,2).toHexString(), serviceDetail.getAttribute( "id" ));
                assertEquals("Service detail version", Integer.toString( version ), serviceDetail.getAttribute( "version" ));
                assertEquals("Service detail name", "Test Service 2", XmlUtil.getTextValue(serviceDetailName));
                assertEquals("Service soapVersion", "1.2", getPropertyValue(properties, "soapVersion"));
            }
        };

        putAndVerify( message, verifier, false );
        putAndVerify( message, verifier, true );
    }

    @BugId("APIM-330")
    @Test
    public void testPutServiceWithProperties() throws Exception {
        final String message = "<s:Envelope xmlns:s=\"http://www.w3.org/2003/05/soap-envelope\" xmlns:wsa=\"http://schemas.xmlsoap.org/ws/2004/08/addressing\" xmlns:wsman=\"http://schemas.dmtf.org/wbem/wsman/1/wsman.xsd\" xmlns:l7=\"http://ns.l7tech.com/2010/04/gateway-management\"><s:Header><wsa:Action s:mustUnderstand=\"true\">http://schemas.xmlsoap.org/ws/2004/09/transfer/Put</wsa:Action><wsa:To s:mustUnderstand=\"true\">http://127.0.0.1:8080/wsman</wsa:To><wsman:ResourceURI s:mustUnderstand=\"true\">http://ns.l7tech.com/2010/04/gateway-management/services</wsman:ResourceURI><wsa:MessageID s:mustUnderstand=\"true\">uuid:afad2993-7d39-1d39-8002-481688002100</wsa:MessageID><wsa:ReplyTo><wsa:Address>http://schemas.xmlsoap.org/ws/2004/08/addressing/role/anonymous</wsa:Address></wsa:ReplyTo><wsman:SelectorSet><wsman:Selector Name=\"id\">"+new Goid(0,2)+"</wsman:Selector></wsman:SelectorSet><wsman:RequestEPR/></s:Header><s:Body>" +
                "<l7:Service id=\""+new Goid(0,2)+"\" version=\"1\">\n" +
                "            <l7:ServiceDetail id=\""+new Goid(0,2)+"\" version=\"1\">\n" +
                "                <l7:Name>Test Service 2</l7:Name>\n" +
                "                <l7:Enabled>true</l7:Enabled>\n" +
                "                <l7:ServiceMappings>\n" +
                "                    <l7:HttpMapping>\n" +
                "                        <l7:Verbs>\n" +
                "                            <l7:Verb>POST</l7:Verb>\n" +
                "                        </l7:Verbs>\n" +
                "                    </l7:HttpMapping>\n" +
                "                    <l7:SoapMapping>\n" +
                "                        <l7:Lax>false</l7:Lax>\n" +
                "                    </l7:SoapMapping>\n" +
                "                </l7:ServiceMappings>\n" +
                "                <l7:Properties>\n" +
                "                    <l7:Property key=\"policyRevision\">\n" +
                "                        <l7:LongValue>0</l7:LongValue>\n" +
                "                    </l7:Property>\n" +
                "                    <l7:Property key=\"wssProcessingEnabled\">\n" +
                "                        <l7:BooleanValue>true</l7:BooleanValue>\n" +
                "                    </l7:Property>\n" +
                "                    <l7:Property key=\"soap\">\n" +
                "                        <l7:BooleanValue>true</l7:BooleanValue>\n" +
                "                    </l7:Property>\n" +
                "                    <l7:Property key=\"soapVersion\">\n" +
                "                        <l7:StringValue>1.2</l7:StringValue>\n" +
                "                    </l7:Property>\n" +
                "                    <l7:Property key=\"internal\">\n" +
                "                        <l7:BooleanValue>false</l7:BooleanValue>\n" +
                "                    </l7:Property>\n" +
                "                    <l7:Property key=\"property.newProperty\">\n" +
                "                        <l7:StringValue>propValue</l7:StringValue>\n" +
                "                    </l7:Property>\n" +
                "                </l7:Properties>\n" +
                "            </l7:ServiceDetail>\n" +
                "            <l7:Resources>\n" +
                "                <l7:ResourceSet tag=\"policy\">\n" +
                "                    <l7:Resource type=\"policy\">&lt;wsp:Policy xmlns:L7p=\"http://www.layer7tech.com/ws/policy\" xmlns:wsp=\"http://schemas.xmlsoap.org/ws/2002/12/policy\"&gt;\n" +
                "    &lt;wsp:All wsp:Usage=\"Required\"&gt;\n" +
                "        &lt;L7p:EchoRoutingAssertion/&gt;\n" +
                "    &lt;/wsp:All&gt;\n" +
                "&lt;/wsp:Policy&gt;</l7:Resource>\n" +
                "                </l7:ResourceSet>\n" +
                "                <l7:ResourceSet\n" +
                "                    rootUrl=\"http://localhost:8080/test.wsdl\" tag=\"wsdl\">\n" +
                "                    <l7:Resource\n" +
                "                        sourceUrl=\"http://localhost:8080/test.wsdl\" type=\"wsdl\">&lt;wsdl:definitions xmlns:wsdl=\"http://schemas.xmlsoap.org/wsdl/\" targetNamespace=\"http://warehouse.acme.com/ws\"/&gt;</l7:Resource>\n" +
                "                </l7:ResourceSet>\n" +
                "            </l7:Resources>\n" +
                "        </l7:Service>" +
                "</s:Body></s:Envelope>";

        final UnaryVoidThrows<Document,Exception> verifier = new UnaryVoidThrows<Document,Exception>(){
            private int expectedVersion = 2;

            @Override
            public void call( final Document result ) throws Exception {
                final Element soapBody = SoapUtil.getBodyElement(result);
                final Element service = XmlUtil.findExactlyOneChildElementByName(soapBody, NS_GATEWAY_MANAGEMENT, "Service");
                final Element serviceDetail = XmlUtil.findExactlyOneChildElementByName(service, NS_GATEWAY_MANAGEMENT, "ServiceDetail");
                final Element serviceDetailName = XmlUtil.findExactlyOneChildElementByName(serviceDetail, NS_GATEWAY_MANAGEMENT, "Name");
                final Element properties = XmlUtil.findExactlyOneChildElementByName(serviceDetail, NS_GATEWAY_MANAGEMENT, "Properties");

                final int version = expectedVersion++;

                assertEquals("Service id", new Goid(0,2).toHexString(), service.getAttribute( "id" ));
                assertEquals("Service version", Integer.toString( version ), service.getAttribute( "version" ));
                assertEquals("Service detail id", new Goid(0,2).toHexString(), serviceDetail.getAttribute( "id" ));
                assertEquals("Service detail version", Integer.toString( version ), serviceDetail.getAttribute( "version" ));
                assertEquals("Service detail name", "Test Service 2", XmlUtil.getTextValue(serviceDetailName));
                assertEquals("Service property", "propValue", getPropertyValue(properties, "property.newProperty"));
            }
        };

        putAndVerify( message, verifier, false );
        putAndVerify( message, verifier, true );
    }

    @Test
    public void testPutStoredPassword() throws Exception {
        final String message = "<s:Envelope xmlns:s=\"http://www.w3.org/2003/05/soap-envelope\" xmlns:wsa=\"http://schemas.xmlsoap.org/ws/2004/08/addressing\" xmlns:wsman=\"http://schemas.dmtf.org/wbem/wsman/1/wsman.xsd\" xmlns=\"http://ns.l7tech.com/2010/04/gateway-management\"><s:Header><wsa:Action s:mustUnderstand=\"true\">http://schemas.xmlsoap.org/ws/2004/09/transfer/Put</wsa:Action><wsa:To s:mustUnderstand=\"true\">http://127.0.0.1:8080/wsman</wsa:To><wsman:ResourceURI s:mustUnderstand=\"true\">http://ns.l7tech.com/2010/04/gateway-management/storedPasswords</wsman:ResourceURI><wsa:MessageID s:mustUnderstand=\"true\">uuid:afad2993-7d39-1d39-8002-481688002101</wsa:MessageID><wsa:ReplyTo><wsa:Address>http://schemas.xmlsoap.org/ws/2004/08/addressing/role/anonymous</wsa:Address></wsa:ReplyTo><wsman:SelectorSet><wsman:Selector Name=\"id\">"+new Goid(0,1)+"</wsman:Selector></wsman:SelectorSet><wsman:RequestEPR/></s:Header><s:Body>" +
                "<StoredPassword version=\"0\" id=\""+new Goid(0,1)+"\">\n" +
                "    <Name>test updated</Name>\n" +
                "    <Properties>\n" +
                "        <Property key=\"usageFromVariable\">\n" +
                "            <BooleanValue>true</BooleanValue>\n" +
                "        </Property>\n" +
                "        <Property key=\"description\">\n" +
                "            <StringValue>description updated</StringValue>\n" +
                "        </Property>\n" +
                "    </Properties>\n" +
                "</StoredPassword>" +
                "</s:Body></s:Envelope>";

        final UnaryVoidThrows<Document,Exception> verifier = new UnaryVoidThrows<Document,Exception>(){
            private int expectedVersion = 1;

            @Override
            public void call( final Document result ) throws Exception {
                final Element soapBody = SoapUtil.getBodyElement(result);
                final Element storedPassword = XmlUtil.findExactlyOneChildElementByName(soapBody, NS_GATEWAY_MANAGEMENT, "StoredPassword");
                final Element name = XmlUtil.findExactlyOneChildElementByName(storedPassword, NS_GATEWAY_MANAGEMENT, "Name");
                final Element properties = XmlUtil.findExactlyOneChildElementByName(storedPassword, NS_GATEWAY_MANAGEMENT, "Properties");

                assertEquals("Stored password id", new Goid(0,1).toString(), storedPassword.getAttribute( "id" ));
                assertEquals("Stored password version", Integer.toString( expectedVersion++ ), storedPassword.getAttribute( "version" ));
                assertEquals("Stored password name", "test updated", XmlUtil.getTextValue(name));
                assertEquals("Stored password description", "description updated", getPropertyValue(properties, "description"));
                assertEquals("Stored password usageFromVariable", "true", getPropertyValue(properties, "usageFromVariable"));
            }
        };

        putAndVerify( message, verifier, false );
        putAndVerify( message, verifier, true );
    }

    @Test
    public void testPutCustomKeyValue() throws Exception {
        final String message =
            "<s:Envelope xmlns:s=\"http://www.w3.org/2003/05/soap-envelope\" \n" +
            "            xmlns:a=\"http://schemas.xmlsoap.org/ws/2004/08/addressing\" \n" +
            "            xmlns:w=\"http://schemas.dmtf.org/wbem/wsman/1/wsman.xsd\">\n" +
            "  <s:Header>\n" +
            "    <a:MessageID>uuid:4ED2993C-4339-4E99-81FC-C2FD3812781A</a:MessageID> \n" +
            "    <a:To>http://127.0.0.1:8080/wsman</a:To> \n" +
            "    <a:ReplyTo> \n" +
            "      <a:Address s:mustUnderstand=\"true\">http://schemas.xmlsoap.org/ws/2004/08/addressing/role/anonymous</a:Address> \n" +
            "    </a:ReplyTo> \n" +
            "    <a:Action s:mustUnderstand=\"true\">http://schemas.xmlsoap.org/ws/2004/09/transfer/Put</a:Action> \n" +
            "    <w:ResourceURI s:mustUnderstand=\"true\">http://ns.l7tech.com/2010/04/gateway-management/customKeyValues</w:ResourceURI> \n" +
            "    <w:SelectorSet>\n" +
            "      <w:Selector Name=\"id\">"+new Goid(0,1).toHexString()+"</w:Selector> \n" +
            "    </w:SelectorSet>\n" +
            "    <w:OperationTimeout>PT60.000S</w:OperationTimeout>\n" +
            "  </s:Header>\n" +
            "  <s:Body> \n" +
            "    <l7:CustomKeyValue xmlns:l7=\"http://ns.l7tech.com/2010/04/gateway-management\">\n" +
            "      <l7:StoreName>" + KeyValueStoreServices.INTERNAL_TRANSACTIONAL_KEY_VALUE_STORE_NAME + "</l7:StoreName>\n" +
            "      <l7:Key>key.prefix.key1</l7:Key>\n" +
            "      <l7:Value>PHhtbD5UZXN0IHZhbHVlIC0gVXBkYXRlZDwveG1sPg==</l7:Value>\n" +
            "    </l7:CustomKeyValue>\n" +
            "  </s:Body>\n" +
            "</s:Envelope>";

        final UnaryVoidThrows<Document,Exception> verifier = new UnaryVoidThrows<Document,Exception>(){
            @Override
            public void call( final Document result ) throws Exception {
                final Element soapBody = SoapUtil.getBodyElement(result);
                final Element customKeyValueElm = XmlUtil.findExactlyOneChildElementByName(soapBody, NS_GATEWAY_MANAGEMENT, "CustomKeyValue");
                final Element storeNameElm = XmlUtil.findExactlyOneChildElementByName(customKeyValueElm, NS_GATEWAY_MANAGEMENT, "StoreName");
                final Element keyElm = XmlUtil.findExactlyOneChildElementByName(customKeyValueElm, NS_GATEWAY_MANAGEMENT, "Key");
                final Element valueElm = XmlUtil.findExactlyOneChildElementByName(customKeyValueElm, NS_GATEWAY_MANAGEMENT, "Value");

                assertEquals(KeyValueStoreServices.INTERNAL_TRANSACTIONAL_KEY_VALUE_STORE_NAME, DomUtils.getTextValue(storeNameElm));
                assertEquals("key.prefix.key1", DomUtils.getTextValue(keyElm));
                assertEquals("PHhtbD5UZXN0IHZhbHVlIC0gVXBkYXRlZDwveG1sPg==", DomUtils.getTextValue(valueElm));
            }
        };

        putAndVerify( message, verifier, false );
        putAndVerify( message, verifier, true );
    }

    @Test
    public void testPutSiteminderConfig() throws Exception {
        final String message =
                "<s:Envelope xmlns:s=\"http://www.w3.org/2003/05/soap-envelope\" \n" +
                        "            xmlns:a=\"http://schemas.xmlsoap.org/ws/2004/08/addressing\" \n" +
                        "            xmlns:w=\"http://schemas.dmtf.org/wbem/wsman/1/wsman.xsd\">\n" +
                        "  <s:Header>\n" +
                        "    <a:MessageID>uuid:4ED2993C-4339-4E99-81FC-C2FD3812781A</a:MessageID> \n" +
                        "    <a:To>http://127.0.0.1:8080/wsman</a:To> \n" +
                        "    <a:ReplyTo> \n" +
                        "      <a:Address s:mustUnderstand=\"true\">http://schemas.xmlsoap.org/ws/2004/08/addressing/role/anonymous</a:Address> \n" +
                        "    </a:ReplyTo> \n" +
                        "    <a:Action s:mustUnderstand=\"true\">http://schemas.xmlsoap.org/ws/2004/09/transfer/Put</a:Action> \n" +
                        "    <w:ResourceURI s:mustUnderstand=\"true\">http://ns.l7tech.com/2010/04/gateway-management/siteMinderConfigurations</w:ResourceURI> \n" +
                        "    <w:SelectorSet>\n" +
                        "      <w:Selector Name=\"id\">"+new Goid(0,1).toHexString()+"</w:Selector> \n" +
                        "    </w:SelectorSet>\n" +
                        "    <w:OperationTimeout>PT60.000S</w:OperationTimeout>\n" +
                        "  </s:Header>\n" +
                        "  <s:Body> \n" +
                        "     <l7:SiteMinderConfiguration xmlns:l7=\"http://ns.l7tech.com/2010/04/gateway-management\">\n" +
                        "       <l7:Name>New Config 1</l7:Name>\n" +
                        "       <l7:Address>0.0.0.999</l7:Address>\n" +
                        "       <l7:Hostname>localhost</l7:Hostname>\n" +
                        "       <l7:HostConfiguration>55</l7:HostConfiguration>\n" +
                        "       <l7:Enabled>true</l7:Enabled>\n" +
                        "       <l7:FipsMode>3</l7:FipsMode>\n" +
                        "       <l7:IpCheck>false</l7:IpCheck>\n" +
                        "       <l7:UpdateSsoToken>false</l7:UpdateSsoToken>\n" +
                        "       <l7:ClusterThreshold>50</l7:ClusterThreshold>\n" +
                        "       <l7:NonClusterFailover>false</l7:NonClusterFailover>"+
                        "       <l7:Secret>secret</l7:Secret>\n" +
                        "       <l7:Properties/>\n" +
                        "    </l7:SiteMinderConfiguration>\n" +
                        "  </s:Body>\n" +
                        "</s:Envelope>";

        final UnaryVoidThrows<Document,Exception> verifier = new UnaryVoidThrows<Document,Exception>(){
            @Override
            public void call( final Document result ) throws Exception {
                final Element soapBody = SoapUtil.getBodyElement(result);
                final Element siteminderConfigElm = XmlUtil.findExactlyOneChildElementByName(soapBody, NS_GATEWAY_MANAGEMENT, "SiteMinderConfiguration");
                final Element nameElm = XmlUtil.findExactlyOneChildElementByName(siteminderConfigElm, NS_GATEWAY_MANAGEMENT, "Name");

                assertEquals("New Config 1", DomUtils.getTextValue(nameElm));
            }
        };

        putAndVerify( message, verifier, false );
        putAndVerify( message, verifier, true );
    }

    @Test
    public void testPutAssertionSecurityZone() throws Exception {
        final String message =
                "<s:Envelope xmlns:s=\"http://www.w3.org/2003/05/soap-envelope\" \n" +
                        "            xmlns:a=\"http://schemas.xmlsoap.org/ws/2004/08/addressing\" \n" +
                        "            xmlns:w=\"http://schemas.dmtf.org/wbem/wsman/1/wsman.xsd\">\n" +
                        "  <s:Header>\n" +
                        "    <a:MessageID>uuid:4ED2993C-4339-4E99-81FC-C2FD3812781A</a:MessageID> \n" +
                        "    <a:To>http://127.0.0.1:8080/wsman</a:To> \n" +
                        "    <a:ReplyTo> \n" +
                        "      <a:Address s:mustUnderstand=\"true\">http://schemas.xmlsoap.org/ws/2004/08/addressing/role/anonymous</a:Address> \n" +
                        "    </a:ReplyTo> \n" +
                        "    <a:Action s:mustUnderstand=\"true\">http://schemas.xmlsoap.org/ws/2004/09/transfer/Put</a:Action> \n" +
                        "    <w:ResourceURI s:mustUnderstand=\"true\">http://ns.l7tech.com/2010/04/gateway-management/assertionSecurityZones</w:ResourceURI> \n" +
                        "    <w:SelectorSet>\n" +
                        "      <w:Selector Name=\"id\">"+new Goid(0,1).toHexString()+"</w:Selector> \n" +
                        "    </w:SelectorSet>\n" +
                        "    <w:OperationTimeout>PT60.000S</w:OperationTimeout>\n" +
                        "  </s:Header>\n" +
                        "  <s:Body> \n" +
                        "     <l7:AssertionSecurityZone xmlns:l7=\"http://ns.l7tech.com/2010/04/gateway-management\" id=\"00000000000000000000000000000001\">\n" +
                        "       <l7:Name>"+JmsRoutingAssertion.class.getName()+"</l7:Name>\n" +
                        "    </l7:AssertionSecurityZone>\n" +
                        "  </s:Body>\n" +
                        "</s:Envelope>";

        final UnaryVoidThrows<Document,Exception> verifier = new UnaryVoidThrows<Document,Exception>(){
            @Override
            public void call( final Document result ) throws Exception {
                final Element soapBody = SoapUtil.getBodyElement(result);
                final Element zoneElm = XmlUtil.findExactlyOneChildElementByName(soapBody, NS_GATEWAY_MANAGEMENT, "AssertionSecurityZone");
                final Element nameElm = XmlUtil.findExactlyOneChildElementByName(zoneElm, NS_GATEWAY_MANAGEMENT, "Name");

                assertEquals(JmsRoutingAssertion.class.getName(), DomUtils.getTextValue(nameElm));
                assertFalse(zoneElm.hasAttribute("securityZoneId"));
            }
        };

        putAndVerify( message, verifier, false );
        putAndVerify( message, verifier, true );
    }

    @Test
    public void testPutSecurityZone() throws Exception {
        final String message =
                "<s:Envelope xmlns:s=\"http://www.w3.org/2003/05/soap-envelope\" \n" +
                "            xmlns:a=\"http://schemas.xmlsoap.org/ws/2004/08/addressing\" \n" +
                "            xmlns:w=\"http://schemas.dmtf.org/wbem/wsman/1/wsman.xsd\">\n" +
                "  <s:Header>\n" +
                "    <a:MessageID>uuid:4ED2993C-4339-4E99-81FC-C2FD3812781A</a:MessageID> \n" +
                "    <a:To>http://127.0.0.1:8080/wsman</a:To> \n" +
                "    <a:ReplyTo> \n" +
                "      <a:Address s:mustUnderstand=\"true\">http://schemas.xmlsoap.org/ws/2004/08/addressing/role/anonymous</a:Address> \n" +
                "    </a:ReplyTo> \n" +
                "    <a:Action s:mustUnderstand=\"true\">http://schemas.xmlsoap.org/ws/2004/09/transfer/Put</a:Action> \n" +
                "    <w:ResourceURI s:mustUnderstand=\"true\">http://ns.l7tech.com/2010/04/gateway-management/securityZones</w:ResourceURI> \n" +
                "    <w:SelectorSet>\n" +
                "      <w:Selector Name=\"id\">"+new Goid(0,1).toHexString()+"</w:Selector> \n" +
                "    </w:SelectorSet>\n" +
                "    <w:OperationTimeout>PT60.000S</w:OperationTimeout>\n" +
                "  </s:Header>\n" +
                "  <s:Body> \n" +
                "     <l7:SecurityZone id=\"00000000000000000000000000000001\" version=\"0\" xmlns:l7=\"http://ns.l7tech.com/2010/04/gateway-management\">\n" +
                "            <l7:Name>New Name Security Zone 0001</l7:Name>\n" +
                "            <l7:Description>Canned New Name Security Zone 0001</l7:Description>\n" +
                "            <l7:PermittedEntityTypes>\n" +
                "                <l7:StringValue>SERVICE</l7:StringValue>\n" +
                "                <l7:StringValue>SERVICE_ALIAS</l7:StringValue>\n" +
                "                <l7:StringValue>JDBC_CONNECTION</l7:StringValue>\n" +
                "                <l7:StringValue>ASSERTION_ACCESS</l7:StringValue>\n" +
                "            </l7:PermittedEntityTypes>\n" +
                "        </l7:SecurityZone>\n" +
                "  </s:Body>\n" +
                "</s:Envelope>";

        final UnaryVoidThrows<Document,Exception> verifier = new UnaryVoidThrows<Document,Exception>(){
            @Override
            public void call( final Document result ) throws Exception {
                final Element soapBody = SoapUtil.getBodyElement(result);
                final Element zoneElm = XmlUtil.findExactlyOneChildElementByName(soapBody, NS_GATEWAY_MANAGEMENT, "SecurityZone");
                final Element nameElm = XmlUtil.findExactlyOneChildElementByName(zoneElm, NS_GATEWAY_MANAGEMENT, "Name");

                assertEquals("New Name Security Zone 0001", DomUtils.getTextValue(nameElm));
            }
        };

        putAndVerify( message, verifier, false );
        putAndVerify( message, verifier, true );
    }

    @Test
    public void testPutPolicyAlias() throws Exception {
        final String idStr = Goid.toString(new Goid(0,1));

        final String messageEmptyFolder =
                "<s:Envelope xmlns:s=\"http://www.w3.org/2003/05/soap-envelope\" \n" +
                        "            xmlns:a=\"http://schemas.xmlsoap.org/ws/2004/08/addressing\" \n" +
                        "            xmlns:w=\"http://schemas.dmtf.org/wbem/wsman/1/wsman.xsd\">\n" +
                        "  <s:Header>\n" +
                        "    <a:MessageID>uuid:4ED2993C-4339-4E99-81FC-C2FD3812781A</a:MessageID> \n" +
                        "    <a:To>http://127.0.0.1:8080/wsman</a:To> \n" +
                        "    <a:ReplyTo> \n" +
                        "      <a:Address s:mustUnderstand=\"true\">http://schemas.xmlsoap.org/ws/2004/08/addressing/role/anonymous</a:Address> \n" +
                        "    </a:ReplyTo> \n" +
                        "    <a:Action s:mustUnderstand=\"true\">http://schemas.xmlsoap.org/ws/2004/09/transfer/Put</a:Action> \n" +
                        "    <w:ResourceURI s:mustUnderstand=\"true\">http://ns.l7tech.com/2010/04/gateway-management/policyAliases</w:ResourceURI> \n" +
                        "    <w:SelectorSet>\n" +
                        "      <w:Selector Name=\"id\">"+idStr+"</w:Selector> \n" +
                        "    </w:SelectorSet>\n" +
                        "    <w:OperationTimeout>PT60.000S</w:OperationTimeout>\n" +
                        "  </s:Header>\n" +
                        "  <s:Body> \n" +
                        "     <l7:PolicyAlias folderId=\"00000000000000000000000000000003\" id=\"00000000000000000000000000000001\" version=\"0\" xmlns:l7=\"http://ns.l7tech.com/2010/04/gateway-management\">\n" +
                        "         <l7:PolicyReference resourceUri=\"http://ns.l7tech.com/2010/04/gateway-management/policies\" id=\"00000000000000000000000000000001\"/>\n" +
                        "     </l7:PolicyAlias>\n" +
                        "  </s:Body>\n" +
                        "</s:Envelope>";

        final String messageNoIdVersionTestFolder =
                "<s:Envelope xmlns:s=\"http://www.w3.org/2003/05/soap-envelope\" \n" +
                        "            xmlns:a=\"http://schemas.xmlsoap.org/ws/2004/08/addressing\" \n" +
                        "            xmlns:w=\"http://schemas.dmtf.org/wbem/wsman/1/wsman.xsd\">\n" +
                        "  <s:Header>\n" +
                        "    <a:MessageID>uuid:4ED2993C-4339-4E99-81FC-C2FD3812781A</a:MessageID> \n" +
                        "    <a:To>http://127.0.0.1:8080/wsman</a:To> \n" +
                        "    <a:ReplyTo> \n" +
                        "      <a:Address s:mustUnderstand=\"true\">http://schemas.xmlsoap.org/ws/2004/08/addressing/role/anonymous</a:Address> \n" +
                        "    </a:ReplyTo> \n" +
                        "    <a:Action s:mustUnderstand=\"true\">http://schemas.xmlsoap.org/ws/2004/09/transfer/Put</a:Action> \n" +
                        "    <w:ResourceURI s:mustUnderstand=\"true\">http://ns.l7tech.com/2010/04/gateway-management/policyAliases</w:ResourceURI> \n" +
                        "    <w:SelectorSet>\n" +
                        "      <w:Selector Name=\"id\">"+idStr+"</w:Selector> \n" +
                        "    </w:SelectorSet>\n" +
                        "    <w:OperationTimeout>PT60.000S</w:OperationTimeout>\n" +
                        "  </s:Header>\n" +
                        "  <s:Body> \n" +
                        "     <l7:PolicyAlias folderId=\"00000000000000000000000000000001\" xmlns:l7=\"http://ns.l7tech.com/2010/04/gateway-management\">\n" +
                        "         <l7:PolicyReference resourceUri=\"http://ns.l7tech.com/2010/04/gateway-management/policies\" id=\"00000000000000000000000000000001\"/>\n" +
                        "     </l7:PolicyAlias>\n" +
                        "  </s:Body>\n" +
                        "</s:Envelope>";

        final UnaryVoidThrows<Document,Exception> verifier = new UnaryVoidThrows<Document,Exception>(){
            @Override
            public void call( final Document result ) throws Exception {
                final Element soapBody = SoapUtil.getBodyElement(result);
                final Element aliasElm = XmlUtil.findExactlyOneChildElementByName(soapBody, NS_GATEWAY_MANAGEMENT, "PolicyAlias");
                final Element policyRefElm = XmlUtil.findExactlyOneChildElementByName(aliasElm, NS_GATEWAY_MANAGEMENT, "PolicyReference");

                assertEquals(new Goid(0,1).toString(), policyRefElm.getAttribute("id"));
            }
        };

        putAndVerify( messageEmptyFolder, verifier, false );
        putAndVerify( messageNoIdVersionTestFolder, verifier, false );
    }


    @Test
    public void testPutPolicyAliasSameFolderAsOriginal() throws Exception {
        final String idStr = Goid.toString(new Goid(0,1));

        final String messageEmptyFolder =
                "<s:Envelope xmlns:s=\"http://www.w3.org/2003/05/soap-envelope\" \n" +
                        "            xmlns:a=\"http://schemas.xmlsoap.org/ws/2004/08/addressing\" \n" +
                        "            xmlns:w=\"http://schemas.dmtf.org/wbem/wsman/1/wsman.xsd\">\n" +
                        "  <s:Header>\n" +
                        "    <a:MessageID>uuid:4ED2993C-4339-4E99-81FC-C2FD3812781A</a:MessageID> \n" +
                        "    <a:To>http://127.0.0.1:8080/wsman</a:To> \n" +
                        "    <a:ReplyTo> \n" +
                        "      <a:Address s:mustUnderstand=\"true\">http://schemas.xmlsoap.org/ws/2004/08/addressing/role/anonymous</a:Address> \n" +
                        "    </a:ReplyTo> \n" +
                        "    <a:Action s:mustUnderstand=\"true\">http://schemas.xmlsoap.org/ws/2004/09/transfer/Put</a:Action> \n" +
                        "    <w:ResourceURI s:mustUnderstand=\"true\">http://ns.l7tech.com/2010/04/gateway-management/policyAliases</w:ResourceURI> \n" +
                        "    <w:SelectorSet>\n" +
                        "      <w:Selector Name=\"id\">"+idStr+"</w:Selector> \n" +
                        "    </w:SelectorSet>\n" +
                        "    <w:OperationTimeout>PT60.000S</w:OperationTimeout>\n" +
                        "  </s:Header>\n" +
                        "  <s:Body> \n" +
                        "     <l7:PolicyAlias folderId=\"0000000000000000ffffffffffffec76\" id=\"00000000000000000000000000000001\" version=\"0\" xmlns:l7=\"http://ns.l7tech.com/2010/04/gateway-management\">\n" +
                        "         <l7:PolicyReference resourceUri=\"http://ns.l7tech.com/2010/04/gateway-management/policies\" id=\"00000000000000000000000000000001\"/>\n" +
                        "     </l7:PolicyAlias>\n" +
                        "  </s:Body>\n" +
                        "</s:Envelope>";


        final UnaryVoidThrows<Document,Exception> verifier = new UnaryVoidThrows<Document,Exception>(){
            @Override
            public void call( final Document result ) throws Exception {
                final Element soapBody = SoapUtil.getBodyElement(result);
                final Element faultElm = XmlUtil.findExactlyOneChildElementByName(soapBody, NS_SOAP_ENVELOPE, "Fault");
                final Element codeElm = XmlUtil.findExactlyOneChildElementByName(faultElm, NS_SOAP_ENVELOPE, "Code");
                final Element subcodeElm = XmlUtil.findExactlyOneChildElementByName(codeElm, NS_SOAP_ENVELOPE, "Subcode");
                final Element valueElm = XmlUtil.findExactlyOneChildElementByName(subcodeElm, NS_SOAP_ENVELOPE, "Value");
                final Element detailElm = XmlUtil.findExactlyOneChildElementByName(faultElm, NS_SOAP_ENVELOPE, "Detail");
                final Element detailTextElm = XmlUtil.findExactlyOneChildElementByName(detailElm, NS_SOAP_ENVELOPE, "Text");

                assertEquals("wxf:InvalidRepresentation", valueElm.getTextContent());
                assertTrue("Detail contains: ", detailTextElm.getTextContent().contains("same folder"));
            }
        };

        putAndVerify( messageEmptyFolder, verifier, false );
    }


    @Test
    public void testPutPolicyAliasNoFolderPermission() throws Exception {
        when(rbacService.isPermittedForEntity(any(User.class), any(Folder.class),eq(OperationType.UPDATE), anyString())).thenReturn(false);

        final String idStr = Goid.toString(new Goid(0,1));

        final String messageEmptyFolder =
                "<s:Envelope xmlns:s=\"http://www.w3.org/2003/05/soap-envelope\" \n" +
                        "            xmlns:a=\"http://schemas.xmlsoap.org/ws/2004/08/addressing\" \n" +
                        "            xmlns:w=\"http://schemas.dmtf.org/wbem/wsman/1/wsman.xsd\">\n" +
                        "  <s:Header>\n" +
                        "    <a:MessageID>uuid:4ED2993C-4339-4E99-81FC-C2FD3812781A</a:MessageID> \n" +
                        "    <a:To>http://127.0.0.1:8080/wsman</a:To> \n" +
                        "    <a:ReplyTo> \n" +
                        "      <a:Address s:mustUnderstand=\"true\">http://schemas.xmlsoap.org/ws/2004/08/addressing/role/anonymous</a:Address> \n" +
                        "    </a:ReplyTo> \n" +
                        "    <a:Action s:mustUnderstand=\"true\">http://schemas.xmlsoap.org/ws/2004/09/transfer/Put</a:Action> \n" +
                        "    <w:ResourceURI s:mustUnderstand=\"true\">http://ns.l7tech.com/2010/04/gateway-management/policyAliases</w:ResourceURI> \n" +
                        "    <w:SelectorSet>\n" +
                        "      <w:Selector Name=\"id\">"+idStr+"</w:Selector> \n" +
                        "    </w:SelectorSet>\n" +
                        "    <w:OperationTimeout>PT60.000S</w:OperationTimeout>\n" +
                        "  </s:Header>\n" +
                        "  <s:Body> \n" +
                        "     <l7:PolicyAlias folderId=\"00000000000000000000000000000003\" id=\"00000000000000000000000000000001\" version=\"0\" xmlns:l7=\"http://ns.l7tech.com/2010/04/gateway-management\">\n" +
                        "         <l7:PolicyReference resourceUri=\"http://ns.l7tech.com/2010/04/gateway-management/policies\" id=\"00000000000000000000000000000001\"/>\n" +
                        "     </l7:PolicyAlias>\n" +
                        "  </s:Body>\n" +
                        "</s:Envelope>";


        final UnaryVoidThrows<Document,Exception> verifier = new UnaryVoidThrows<Document,Exception>(){
            @Override
            public void call( final Document result ) throws Exception {
                final Element soapBody = SoapUtil.getBodyElement(result);
                final Element faultElm = XmlUtil.findExactlyOneChildElementByName(soapBody, NS_SOAP_ENVELOPE, "Fault");
                final Element codeElm = XmlUtil.findExactlyOneChildElementByName(faultElm, NS_SOAP_ENVELOPE, "Code");
                final Element subcodeElm = XmlUtil.findExactlyOneChildElementByName(codeElm, NS_SOAP_ENVELOPE, "Subcode");
                final Element valueElm = XmlUtil.findExactlyOneChildElementByName(subcodeElm, NS_SOAP_ENVELOPE, "Value");

                assertEquals("wsman:AccessDenied", valueElm.getTextContent());
            }
        };

        putAndVerify( messageEmptyFolder, verifier, false );
    }

    @Test
    public void testPutPolicyAliasNoSourceFolderPermission() throws Exception {
        when(rbacService.isPermittedForEntity(any(User.class), same(testFolder),eq(OperationType.UPDATE), anyString())).thenReturn(false);

        final String idStr = Goid.toString(new Goid(0,1));

        final String messageEmptyFolder =
                "<s:Envelope xmlns:s=\"http://www.w3.org/2003/05/soap-envelope\" \n" +
                        "            xmlns:a=\"http://schemas.xmlsoap.org/ws/2004/08/addressing\" \n" +
                        "            xmlns:w=\"http://schemas.dmtf.org/wbem/wsman/1/wsman.xsd\">\n" +
                        "  <s:Header>\n" +
                        "    <a:MessageID>uuid:4ED2993C-4339-4E99-81FC-C2FD3812781A</a:MessageID> \n" +
                        "    <a:To>http://127.0.0.1:8080/wsman</a:To> \n" +
                        "    <a:ReplyTo> \n" +
                        "      <a:Address s:mustUnderstand=\"true\">http://schemas.xmlsoap.org/ws/2004/08/addressing/role/anonymous</a:Address> \n" +
                        "    </a:ReplyTo> \n" +
                        "    <a:Action s:mustUnderstand=\"true\">http://schemas.xmlsoap.org/ws/2004/09/transfer/Put</a:Action> \n" +
                        "    <w:ResourceURI s:mustUnderstand=\"true\">http://ns.l7tech.com/2010/04/gateway-management/policyAliases</w:ResourceURI> \n" +
                        "    <w:SelectorSet>\n" +
                        "      <w:Selector Name=\"id\">"+idStr+"</w:Selector> \n" +
                        "    </w:SelectorSet>\n" +
                        "    <w:OperationTimeout>PT60.000S</w:OperationTimeout>\n" +
                        "  </s:Header>\n" +
                        "  <s:Body> \n" +
                        "     <l7:PolicyAlias folderId=\"00000000000000000000000000000003\" id=\"00000000000000000000000000000001\" version=\"0\" xmlns:l7=\"http://ns.l7tech.com/2010/04/gateway-management\">\n" +
                        "         <l7:PolicyReference resourceUri=\"http://ns.l7tech.com/2010/04/gateway-management/policies\" id=\"00000000000000000000000000000001\"/>\n" +
                        "     </l7:PolicyAlias>\n" +
                        "  </s:Body>\n" +
                        "</s:Envelope>";


        final UnaryVoidThrows<Document,Exception> verifier = new UnaryVoidThrows<Document,Exception>(){
            @Override
            public void call( final Document result ) throws Exception {
                final Element soapBody = SoapUtil.getBodyElement(result);
                final Element faultElm = XmlUtil.findExactlyOneChildElementByName(soapBody, NS_SOAP_ENVELOPE, "Fault");
                final Element codeElm = XmlUtil.findExactlyOneChildElementByName(faultElm, NS_SOAP_ENVELOPE, "Code");
                final Element subcodeElm = XmlUtil.findExactlyOneChildElementByName(codeElm, NS_SOAP_ENVELOPE, "Subcode");
                final Element valueElm = XmlUtil.findExactlyOneChildElementByName(subcodeElm, NS_SOAP_ENVELOPE, "Value");

                assertEquals("wsman:AccessDenied", valueElm.getTextContent());
            }
        };

        putAndVerify( messageEmptyFolder, verifier, false );
    }

    @Test
    public void testPutServiceAlias() throws Exception {
        final String messageEmptyFolder =
                "<s:Envelope xmlns:s=\"http://www.w3.org/2003/05/soap-envelope\" \n" +
                        "            xmlns:a=\"http://schemas.xmlsoap.org/ws/2004/08/addressing\" \n" +
                        "            xmlns:w=\"http://schemas.dmtf.org/wbem/wsman/1/wsman.xsd\">\n" +
                        "  <s:Header>\n" +
                        "    <a:MessageID>uuid:4ED2993C-4339-4E99-81FC-C2FD3812781A</a:MessageID> \n" +
                        "    <a:To>http://127.0.0.1:8080/wsman</a:To> \n" +
                        "    <a:ReplyTo> \n" +
                        "      <a:Address s:mustUnderstand=\"true\">http://schemas.xmlsoap.org/ws/2004/08/addressing/role/anonymous</a:Address> \n" +
                        "    </a:ReplyTo> \n" +
                        "    <a:Action s:mustUnderstand=\"true\">http://schemas.xmlsoap.org/ws/2004/09/transfer/Put</a:Action> \n" +
                        "    <w:ResourceURI s:mustUnderstand=\"true\">http://ns.l7tech.com/2010/04/gateway-management/serviceAliases</w:ResourceURI> \n" +
                        "    <w:SelectorSet>\n" +
                        "      <w:Selector Name=\"id\">"+new Goid(0,1).toHexString()+"</w:Selector> \n" +
                        "    </w:SelectorSet>\n" +
                        "    <w:OperationTimeout>PT60.000S</w:OperationTimeout>\n" +
                        "  </s:Header>\n" +
                        "  <s:Body> \n" +
                        "     <l7:ServiceAlias folderId=\"00000000000000000000000000000003\" id=\"00000000000000000000000000000001\" version=\"0\" xmlns:l7=\"http://ns.l7tech.com/2010/04/gateway-management\">\n" +
                        "         <l7:ServiceReference resourceUri=\"http://ns.l7tech.com/2010/04/gateway-management/services\" id=\"00000000000000000000000000000001\"/>\n" +
                        "     </l7:ServiceAlias>\n" +
                        "  </s:Body>\n" +
                        "</s:Envelope>";

        final String messageNoIdVersionTestFolder =
                "<s:Envelope xmlns:s=\"http://www.w3.org/2003/05/soap-envelope\" \n" +
                        "            xmlns:a=\"http://schemas.xmlsoap.org/ws/2004/08/addressing\" \n" +
                        "            xmlns:w=\"http://schemas.dmtf.org/wbem/wsman/1/wsman.xsd\">\n" +
                        "  <s:Header>\n" +
                        "    <a:MessageID>uuid:4ED2993C-4339-4E99-81FC-C2FD3812781A</a:MessageID> \n" +
                        "    <a:To>http://127.0.0.1:8080/wsman</a:To> \n" +
                        "    <a:ReplyTo> \n" +
                        "      <a:Address s:mustUnderstand=\"true\">http://schemas.xmlsoap.org/ws/2004/08/addressing/role/anonymous</a:Address> \n" +
                        "    </a:ReplyTo> \n" +
                        "    <a:Action s:mustUnderstand=\"true\">http://schemas.xmlsoap.org/ws/2004/09/transfer/Put</a:Action> \n" +
                        "    <w:ResourceURI s:mustUnderstand=\"true\">http://ns.l7tech.com/2010/04/gateway-management/serviceAliases</w:ResourceURI> \n" +
                        "    <w:SelectorSet>\n" +
                        "      <w:Selector Name=\"id\">"+new Goid(0,1).toHexString()+"</w:Selector> \n" +
                        "    </w:SelectorSet>\n" +
                        "    <w:OperationTimeout>PT60.000S</w:OperationTimeout>\n" +
                        "  </s:Header>\n" +
                        "  <s:Body> \n" +
                        "     <l7:ServiceAlias folderId=\"00000000000000000000000000000001\" xmlns:l7=\"http://ns.l7tech.com/2010/04/gateway-management\">\n" +
                        "         <l7:ServiceReference resourceUri=\"http://ns.l7tech.com/2010/04/gateway-management/services\" id=\"00000000000000000000000000000001\"/>\n" +
                        "     </l7:ServiceAlias>\n" +
                        "  </s:Body>\n" +
                        "</s:Envelope>";

        final UnaryVoidThrows<Document,Exception> verifier = new UnaryVoidThrows<Document,Exception>(){
            @Override
            public void call( final Document result ) throws Exception {
                final Element soapBody = SoapUtil.getBodyElement(result);
                final Element aliasElm = XmlUtil.findExactlyOneChildElementByName(soapBody, NS_GATEWAY_MANAGEMENT, "ServiceAlias");
                final Element policyRefElm = XmlUtil.findExactlyOneChildElementByName(aliasElm, NS_GATEWAY_MANAGEMENT, "ServiceReference");

                assertEquals(new Goid(0,1).toString(), policyRefElm.getAttribute("id"));
            }
        };

        putAndVerify( messageEmptyFolder, verifier, false );
        putAndVerify( messageNoIdVersionTestFolder, verifier, false );
    }

    @Test
    public void testPutServiceAliasSameFolderAsOriginal() throws Exception {
        final String idStr = Goid.toString(new Goid(0,1));

        final String messageEmptyFolder =
                "<s:Envelope xmlns:s=\"http://www.w3.org/2003/05/soap-envelope\" \n" +
                        "            xmlns:a=\"http://schemas.xmlsoap.org/ws/2004/08/addressing\" \n" +
                        "            xmlns:w=\"http://schemas.dmtf.org/wbem/wsman/1/wsman.xsd\">\n" +
                        "  <s:Header>\n" +
                        "    <a:MessageID>uuid:4ED2993C-4339-4E99-81FC-C2FD3812781A</a:MessageID> \n" +
                        "    <a:To>http://127.0.0.1:8080/wsman</a:To> \n" +
                        "    <a:ReplyTo> \n" +
                        "      <a:Address s:mustUnderstand=\"true\">http://schemas.xmlsoap.org/ws/2004/08/addressing/role/anonymous</a:Address> \n" +
                        "    </a:ReplyTo> \n" +
                        "    <a:Action s:mustUnderstand=\"true\">http://schemas.xmlsoap.org/ws/2004/09/transfer/Put</a:Action> \n" +
                        "    <w:ResourceURI s:mustUnderstand=\"true\">http://ns.l7tech.com/2010/04/gateway-management/serviceAliases</w:ResourceURI> \n" +
                        "    <w:SelectorSet>\n" +
                        "      <w:Selector Name=\"id\">"+idStr+"</w:Selector> \n" +
                        "    </w:SelectorSet>\n" +
                        "    <w:OperationTimeout>PT60.000S</w:OperationTimeout>\n" +
                        "  </s:Header>\n" +
                        "  <s:Body> \n" +
                        "     <l7:ServiceAlias folderId=\"0000000000000000ffffffffffffec76\" id=\"00000000000000000000000000000001\" version=\"0\" xmlns:l7=\"http://ns.l7tech.com/2010/04/gateway-management\">\n" +
                        "         <l7:ServiceReference resourceUri=\"http://ns.l7tech.com/2010/04/gateway-management/service\" id=\"00000000000000000000000000000001\"/>\n" +
                        "     </l7:ServiceAlias>\n" +
                        "  </s:Body>\n" +
                        "</s:Envelope>";


        final UnaryVoidThrows<Document,Exception> verifier = new UnaryVoidThrows<Document,Exception>(){
            @Override
            public void call( final Document result ) throws Exception {
                final Element soapBody = SoapUtil.getBodyElement(result);
                final Element faultElm = XmlUtil.findExactlyOneChildElementByName(soapBody, NS_SOAP_ENVELOPE, "Fault");
                final Element codeElm = XmlUtil.findExactlyOneChildElementByName(faultElm, NS_SOAP_ENVELOPE, "Code");
                final Element subcodeElm = XmlUtil.findExactlyOneChildElementByName(codeElm, NS_SOAP_ENVELOPE, "Subcode");
                final Element valueElm = XmlUtil.findExactlyOneChildElementByName(subcodeElm, NS_SOAP_ENVELOPE, "Value");
                final Element detailElm = XmlUtil.findExactlyOneChildElementByName(faultElm, NS_SOAP_ENVELOPE, "Detail");
                final Element detailTextElm = XmlUtil.findExactlyOneChildElementByName(detailElm, NS_SOAP_ENVELOPE, "Text");

                assertEquals("wxf:InvalidRepresentation", valueElm.getTextContent());
                assertTrue("Detail contains: ", detailTextElm.getTextContent().contains("same folder"));
            }
        };

        putAndVerify( messageEmptyFolder, verifier, false );
    }


    @Test
    public void testPutServiceAliasNoFolderPermission() throws Exception {
        when(rbacService.isPermittedForEntity(any(User.class), any(Folder.class),eq(OperationType.UPDATE), anyString())).thenReturn(false);

        final String idStr = Goid.toString(new Goid(0,1));

        final String messageEmptyFolder =
                "<s:Envelope xmlns:s=\"http://www.w3.org/2003/05/soap-envelope\" \n" +
                        "            xmlns:a=\"http://schemas.xmlsoap.org/ws/2004/08/addressing\" \n" +
                        "            xmlns:w=\"http://schemas.dmtf.org/wbem/wsman/1/wsman.xsd\">\n" +
                        "  <s:Header>\n" +
                        "    <a:MessageID>uuid:4ED2993C-4339-4E99-81FC-C2FD3812781A</a:MessageID> \n" +
                        "    <a:To>http://127.0.0.1:8080/wsman</a:To> \n" +
                        "    <a:ReplyTo> \n" +
                        "      <a:Address s:mustUnderstand=\"true\">http://schemas.xmlsoap.org/ws/2004/08/addressing/role/anonymous</a:Address> \n" +
                        "    </a:ReplyTo> \n" +
                        "    <a:Action s:mustUnderstand=\"true\">http://schemas.xmlsoap.org/ws/2004/09/transfer/Put</a:Action> \n" +
                        "    <w:ResourceURI s:mustUnderstand=\"true\">http://ns.l7tech.com/2010/04/gateway-management/serviceAliases</w:ResourceURI> \n" +
                        "    <w:SelectorSet>\n" +
                        "      <w:Selector Name=\"id\">"+idStr+"</w:Selector> \n" +
                        "    </w:SelectorSet>\n" +
                        "    <w:OperationTimeout>PT60.000S</w:OperationTimeout>\n" +
                        "  </s:Header>\n" +
                        "  <s:Body> \n" +
                        "     <l7:ServiceAlias folderId=\"00000000000000000000000000000003\" id=\"00000000000000000000000000000001\" version=\"0\" xmlns:l7=\"http://ns.l7tech.com/2010/04/gateway-management\">\n" +
                        "         <l7:ServiceReference resourceUri=\"http://ns.l7tech.com/2010/04/gateway-management/service\" id=\"00000000000000000000000000000001\"/>\n" +
                        "     </l7:ServiceAlias>\n" +
                        "  </s:Body>\n" +
                        "</s:Envelope>";


        final UnaryVoidThrows<Document,Exception> verifier = new UnaryVoidThrows<Document,Exception>(){
            @Override
            public void call( final Document result ) throws Exception {
                final Element soapBody = SoapUtil.getBodyElement(result);
                final Element faultElm = XmlUtil.findExactlyOneChildElementByName(soapBody, NS_SOAP_ENVELOPE, "Fault");
                final Element codeElm = XmlUtil.findExactlyOneChildElementByName(faultElm, NS_SOAP_ENVELOPE, "Code");
                final Element subcodeElm = XmlUtil.findExactlyOneChildElementByName(codeElm, NS_SOAP_ENVELOPE, "Subcode");
                final Element valueElm = XmlUtil.findExactlyOneChildElementByName(subcodeElm, NS_SOAP_ENVELOPE, "Value");

                assertEquals("wsman:AccessDenied", valueElm.getTextContent());
            }
        };

        putAndVerify( messageEmptyFolder, verifier, false );
    }

    @Test
    public void testDelete() throws Exception {
        String message = "<s:Envelope xmlns:s=\"http://www.w3.org/2003/05/soap-envelope\" xmlns:wsa=\"http://schemas.xmlsoap.org/ws/2004/08/addressing\" xmlns:wsman=\"http://schemas.dmtf.org/wbem/wsman/1/wsman.xsd\"><s:Header><wsa:Action s:mustUnderstand=\"true\">http://schemas.xmlsoap.org/ws/2004/09/transfer/Delete</wsa:Action><wsa:To s:mustUnderstand=\"true\">http://127.0.0.1:8080/wsman</wsa:To><wsman:ResourceURI s:mustUnderstand=\"true\">http://ns.l7tech.com/2010/04/gateway-management/clusterProperties</wsman:ResourceURI><wsa:MessageID s:mustUnderstand=\"true\">uuid:b2794ffb-7d39-1d39-8002-481688002100</wsa:MessageID><wsa:ReplyTo><wsa:Address>http://schemas.xmlsoap.org/ws/2004/08/addressing/role/anonymous</wsa:Address></wsa:ReplyTo><wsman:SelectorSet><wsman:Selector Name=\"id\">"+new Goid(0,2).toHexString()+"</wsman:Selector></wsman:SelectorSet></s:Header><s:Body/></s:Envelope>";

        final Document result = processRequest( "http://schemas.xmlsoap.org/ws/2004/09/transfer/Delete", message );

        final Element soapBody = SoapUtil.getBodyElement(result);
        assertNotNull("SOAP Body", soapBody);
        assertNull("No body content", soapBody.getFirstChild());
    }

    @BugId("SSG-5572")
    @Test
    public void testDeleteGenericEntity() throws Exception {
        String message = "<s:Envelope xmlns:s=\"http://www.w3.org/2003/05/soap-envelope\" xmlns:wsa=\"http://schemas.xmlsoap.org/ws/2004/08/addressing\" xmlns:wsman=\"http://schemas.dmtf.org/wbem/wsman/1/wsman.xsd\"><s:Header><wsa:Action s:mustUnderstand=\"true\">http://schemas.xmlsoap.org/ws/2004/09/transfer/Delete</wsa:Action><wsa:To s:mustUnderstand=\"true\">http://127.0.0.1:8080/wsman</wsa:To><wsman:ResourceURI s:mustUnderstand=\"true\">http://ns.l7tech.com/2010/04/gateway-management/genericEntities</wsman:ResourceURI><wsa:MessageID s:mustUnderstand=\"true\">uuid:b2794ffb-7d39-1d39-8002-481688002100</wsa:MessageID><wsa:ReplyTo><wsa:Address>http://schemas.xmlsoap.org/ws/2004/08/addressing/role/anonymous</wsa:Address></wsa:ReplyTo>" +
                "<wsman:SelectorSet><wsman:Selector Name=\"id\">"+new Goid(0,1).toHexString()+"</wsman:Selector></wsman:SelectorSet></s:Header><s:Body/></s:Envelope>";

        final Document result = processRequest( "http://schemas.xmlsoap.org/ws/2004/09/transfer/Delete", message );

        final Element soapBody = SoapUtil.getBodyElement(result);
        assertNotNull("SOAP Body", soapBody);
        assertNull("No body content", soapBody.getFirstChild());
    }

    @Test
    public void testDeleteCustomKeyValue() throws Exception {
        final String message =
            "<s:Envelope xmlns:s=\"http://www.w3.org/2003/05/soap-envelope\" \n" +
            "            xmlns:a=\"http://schemas.xmlsoap.org/ws/2004/08/addressing\" \n" +
            "            xmlns:w=\"http://schemas.dmtf.org/wbem/wsman/1/wsman.xsd\">\n" +
            "  <s:Header>\n" +
            "    <a:MessageID>uuid:4ED2993C-4339-4E99-81FC-C2FD3812781A</a:MessageID> \n" +
            "    <a:To>http://127.0.0.1:8080/wsman</a:To> \n" +
            "    <a:ReplyTo> \n" +
            "      <a:Address s:mustUnderstand=\"true\">http://schemas.xmlsoap.org/ws/2004/08/addressing/role/anonymous</a:Address> \n" +
            "    </a:ReplyTo> \n" +
            "    <a:Action s:mustUnderstand=\"true\">http://schemas.xmlsoap.org/ws/2004/09/transfer/Delete</a:Action> \n" +
            "    <w:ResourceURI s:mustUnderstand=\"true\">http://ns.l7tech.com/2010/04/gateway-management/customKeyValues</w:ResourceURI> \n" +
            "    <w:SelectorSet>\n" +
            "      <w:Selector Name=\"id\">"+new Goid(0,1).toHexString()+"</w:Selector> \n" +
            "    </w:SelectorSet>\n" +
            "    <w:OperationTimeout>PT60.000S</w:OperationTimeout> \n" +
            "  </s:Header>\n" +
            "  <s:Body/> \n" +
            "</s:Envelope>";

        final Document result = processRequest( "http://schemas.xmlsoap.org/ws/2004/09/transfer/Delete", message );

        final Element soapBody = SoapUtil.getBodyElement(result);

        assertNotNull("SOAP Body", soapBody);
        assertNull("No body content", soapBody.getFirstChild());
    }

    @Test
    public void testFragmentGet() throws Exception {
        final String message =
                "<s:Envelope xmlns:s=\"http://www.w3.org/2003/05/soap-envelope\" \n" +
                "            xmlns:a=\"http://schemas.xmlsoap.org/ws/2004/08/addressing\" \n" +
                "            xmlns:l7=\"http://ns.l7tech.com/2010/04/gateway-management\" \n" +
                "            xmlns:w=\"http://schemas.dmtf.org/wbem/wsman/1/wsman.xsd\">\n" +
                "  <s:Header>\n" +
                "    <a:MessageID>uuid:4ED2993C-4339-4E99-81FC-C2FD3812781A</a:MessageID> \n" +
                "    <a:To>http://127.0.0.1:8080/wsman</a:To> \n" +
                "    <a:ReplyTo> \n" +
                "      <a:Address s:mustUnderstand=\"true\">http://schemas.xmlsoap.org/ws/2004/08/addressing/role/anonymous</a:Address> \n" +
                "    </a:ReplyTo> \n" +
                "    <a:Action s:mustUnderstand=\"true\">http://schemas.xmlsoap.org/ws/2004/09/transfer/Get</a:Action> \n" +
                "    <w:ResourceURI s:mustUnderstand=\"true\">http://ns.l7tech.com/2010/04/gateway-management/services</w:ResourceURI> \n" +
                "    <w:SelectorSet>\n" +
                "      <w:Selector Name=\"id\">"+new Goid(0,1)+"</w:Selector> \n" +
                "    </w:SelectorSet>\n" +
                "    <w:OperationTimeout>PT60.000S</w:OperationTimeout> \n" +
                "    <w:FragmentTransfer s:mustUnderstand=\"true\">l7:ServiceDetail/l7:Enabled</w:FragmentTransfer> \n" +
                "  </s:Header>\n" +
                "  <s:Body/> \n" +
                "</s:Envelope>";

        final Document result = processRequest( "http://schemas.xmlsoap.org/ws/2004/09/transfer/Get", message );

        final Element soapBody = SoapUtil.getBodyElement(result);
        final Element fragment = XmlUtil.findExactlyOneChildElementByName(soapBody, NS_WS_MANAGEMENT, "XmlFragment");
        final Element enabled = XmlUtil.findExactlyOneChildElementByName(fragment, NS_GATEWAY_MANAGEMENT, "Enabled");

        assertEquals("Service enabled", "true", XmlUtil.getTextValue(enabled));
    }

    /**
     * Test fragment put with an element
     */
    @Test
    public void testFragmentPut() throws Exception {
        String message = "<s:Envelope xmlns:s=\"http://www.w3.org/2003/05/soap-envelope\" xmlns:wsa=\"http://schemas.xmlsoap.org/ws/2004/08/addressing\" xmlns:wsman=\"http://schemas.dmtf.org/wbem/wsman/1/wsman.xsd\" xmlns:n1=\"http://ns.l7tech.com/2010/04/gateway-management\"><s:Header><wsa:Action s:mustUnderstand=\"true\">http://schemas.xmlsoap.org/ws/2004/09/transfer/Put</wsa:Action><wsa:To s:mustUnderstand=\"true\">http://127.0.0.1:8080/wsman</wsa:To><wsman:ResourceURI s:mustUnderstand=\"true\">http://ns.l7tech.com/2010/04/gateway-management/clusterProperties</wsman:ResourceURI><wsa:MessageID s:mustUnderstand=\"true\">uuid:afad2993-7d39-1d39-8002-481688002100</wsa:MessageID><wsa:ReplyTo><wsa:Address>http://schemas.xmlsoap.org/ws/2004/08/addressing/role/anonymous</wsa:Address></wsa:ReplyTo><wsman:SelectorSet><wsman:Selector Name=\"id\">"+new Goid(0,1).toHexString()+"</wsman:Selector></wsman:SelectorSet><wsman:FragmentTransfer s:mustUnderstand=\"true\">n1:Value[1]</wsman:FragmentTransfer></s:Header><s:Body><wsman:XmlFragment><n1:Value>value3</n1:Value></wsman:XmlFragment></s:Body></s:Envelope>";

        final Document result = processRequest( "http://schemas.xmlsoap.org/ws/2004/09/transfer/Put", message );

        final Element soapBody = SoapUtil.getBodyElement(result);
        final Element fragment = XmlUtil.findExactlyOneChildElementByName(soapBody, NS_WS_MANAGEMENT, "XmlFragment");
        final Element value = XmlUtil.findExactlyOneChildElementByName(fragment, NS_GATEWAY_MANAGEMENT, "Value");

        assertEquals("Property value", "value3", XmlUtil.getTextValue(value));
    }

    /**
     * Test fragment put with text only
     */
    @Test
    public void testFragmentPut2() throws Exception {
        String message = "<s:Envelope xmlns:s=\"http://www.w3.org/2003/05/soap-envelope\" xmlns:wsa=\"http://schemas.xmlsoap.org/ws/2004/08/addressing\" xmlns:wsman=\"http://schemas.dmtf.org/wbem/wsman/1/wsman.xsd\" xmlns:n1=\"http://ns.l7tech.com/2010/04/gateway-management\"><s:Header><wsa:Action s:mustUnderstand=\"true\">http://schemas.xmlsoap.org/ws/2004/09/transfer/Put</wsa:Action><wsa:To s:mustUnderstand=\"true\">http://127.0.0.1:8080/wsman</wsa:To><wsman:ResourceURI s:mustUnderstand=\"true\">http://ns.l7tech.com/2010/04/gateway-management/clusterProperties</wsman:ResourceURI><wsa:MessageID s:mustUnderstand=\"true\">uuid:afad2993-7d39-1d39-8002-481688002100</wsa:MessageID><wsa:ReplyTo><wsa:Address>http://schemas.xmlsoap.org/ws/2004/08/addressing/role/anonymous</wsa:Address></wsa:ReplyTo><wsman:SelectorSet><wsman:Selector Name=\"id\">"+new Goid(0,1).toHexString()+"</wsman:Selector></wsman:SelectorSet><wsman:FragmentTransfer s:mustUnderstand=\"true\">n1:Value/text()</wsman:FragmentTransfer></s:Header><s:Body><wsman:XmlFragment>value3text</wsman:XmlFragment></s:Body></s:Envelope>";

        final Document result = processRequest( "http://schemas.xmlsoap.org/ws/2004/09/transfer/Put", message );

        final Element soapBody = SoapUtil.getBodyElement(result);
        final Element fragment = XmlUtil.findExactlyOneChildElementByName(soapBody, NS_WS_MANAGEMENT, "XmlFragment");

        assertEquals("Property value", "value3text", XmlUtil.getTextValue(fragment));
    }

    @Test
    public void testEnumerate() throws Exception {
        final String message =
                "<s:Envelope xmlns:s=\"http://www.w3.org/2003/05/soap-envelope\" \n" +
                "            xmlns:a=\"http://schemas.xmlsoap.org/ws/2004/08/addressing\" \n" +
                "            xmlns:wsen=\"http://schemas.xmlsoap.org/ws/2004/09/enumeration\" \n" +
                "            xmlns:w=\"http://schemas.dmtf.org/wbem/wsman/1/wsman.xsd\">\n" +
                "  <s:Header>\n" +
                "    <a:MessageID>uuid:4ED2993C-4339-4E99-81FC-C2FD3812781A</a:MessageID> \n" +
                "    <a:To>http://127.0.0.1:8080/wsman</a:To> \n" +
                "    <a:ReplyTo> \n" +
                "      <a:Address s:mustUnderstand=\"true\">http://schemas.xmlsoap.org/ws/2004/08/addressing/role/anonymous</a:Address> \n" +
                "    </a:ReplyTo> \n" +
                "    <a:Action s:mustUnderstand=\"true\">http://schemas.xmlsoap.org/ws/2004/09/enumeration/Enumerate</a:Action> \n" +
                "    <w:ResourceURI s:mustUnderstand=\"true\">http://ns.l7tech.com/2010/04/gateway-management/services</w:ResourceURI> \n" +
                "    <w:OperationTimeout>PT60.000S</w:OperationTimeout> \n" +
                "  </s:Header>\n" +
                "  <s:Body>\n" +
                "    <wsen:Enumerate/>" +
                "  </s:Body> \n" +
                "</s:Envelope>";

        final Document result = processRequest( "http://schemas.xmlsoap.org/ws/2004/09/enumeration/Enumerate", message );

        final Element soapBody = SoapUtil.getBodyElement(result);
        final Element enumerateResponse = XmlUtil.findExactlyOneChildElementByName(soapBody, NS_WS_ENUMERATION, "EnumerateResponse");
        final Element enumerationContext = XmlUtil.findExactlyOneChildElementByName(enumerateResponse, NS_WS_ENUMERATION, "EnumerationContext");

        final String context = XmlUtil.getTextValue(enumerationContext);

        assertNotNull("Valid enumeration context", context);
        assertFalse("Valid enumeration context", context.trim().isEmpty());

        final String pullMessage =
                "<s:Envelope xmlns:s=\"http://www.w3.org/2003/05/soap-envelope\" \n" +
                "            xmlns:a=\"http://schemas.xmlsoap.org/ws/2004/08/addressing\" \n" +
                "            xmlns:wsen=\"http://schemas.xmlsoap.org/ws/2004/09/enumeration\" \n" +
                "            xmlns:w=\"http://schemas.dmtf.org/wbem/wsman/1/wsman.xsd\">\n" +
                "  <s:Header>\n" +
                "    <a:MessageID>uuid:4ED2993C-4339-4E99-81FC-C2FD3812781A</a:MessageID> \n" +
                "    <a:To>http://127.0.0.1:8080/wsman</a:To> \n" +
                "    <a:ReplyTo> \n" +
                "      <a:Address s:mustUnderstand=\"true\">http://schemas.xmlsoap.org/ws/2004/08/addressing/role/anonymous</a:Address> \n" +
                "    </a:ReplyTo> \n" +
                "    <a:Action s:mustUnderstand=\"true\">http://schemas.xmlsoap.org/ws/2004/09/enumeration/Pull</a:Action> \n" +
                "    <w:ResourceURI s:mustUnderstand=\"true\">http://ns.l7tech.com/2010/04/gateway-management/services</w:ResourceURI> \n" +
                "    <w:OperationTimeout>PT60.000S</w:OperationTimeout> \n" +
                "  </s:Header>\n" +
                "  <s:Body>\n" +
                "    <wsen:Pull>\n" +
                "      <wsen:EnumerationContext>{0}</wsen:EnumerationContext>\n" +
                "      <wsen:MaxElements>1000</wsen:MaxElements>\n" +
                "    </wsen:Pull>\n" +
                "  </s:Body> \n" +
                "</s:Envelope>";

        final Document pullResult = processRequest( "http://schemas.xmlsoap.org/ws/2004/09/enumeration/Pull", MessageFormat.format(pullMessage, context));

        final Element soapBody2 = SoapUtil.getBodyElement(pullResult);
        final Element pullResponse = XmlUtil.findExactlyOneChildElementByName(soapBody2, NS_WS_ENUMERATION, "PullResponse");
        XmlUtil.findExactlyOneChildElementByName(pullResponse, NS_WS_ENUMERATION, "Items");
        XmlUtil.findExactlyOneChildElementByName(pullResponse, NS_WS_ENUMERATION, "EndOfSequence");
    }

    @Test
    public void testEnumerateAll() throws Exception {
        for ( String resourceUri : RESOURCE_URIS ) {
            final String message =
                    "<s:Envelope xmlns:s=\"http://www.w3.org/2003/05/soap-envelope\" \n" +
                    "            xmlns:a=\"http://schemas.xmlsoap.org/ws/2004/08/addressing\" \n" +
                    "            xmlns:wsen=\"http://schemas.xmlsoap.org/ws/2004/09/enumeration\" \n" +
                    "            xmlns:w=\"http://schemas.dmtf.org/wbem/wsman/1/wsman.xsd\">\n" +
                    "  <s:Header>\n" +
                    "    <a:MessageID>uuid:4ED2993C-4339-4E99-81FC-C2FD3812781A</a:MessageID> \n" +
                    "    <a:To>http://127.0.0.1:8080/wsman</a:To> \n" +
                    "    <a:ReplyTo> \n" +
                    "      <a:Address s:mustUnderstand=\"true\">http://schemas.xmlsoap.org/ws/2004/08/addressing/role/anonymous</a:Address> \n" +
                    "    </a:ReplyTo> \n" +
                    "    <a:Action s:mustUnderstand=\"true\">http://schemas.xmlsoap.org/ws/2004/09/enumeration/Enumerate</a:Action> \n" +
                    "    <w:ResourceURI s:mustUnderstand=\"true\">{0}</w:ResourceURI> \n" +
                    "    <w:OperationTimeout>PT60.000S</w:OperationTimeout> \n" +
                    "  </s:Header>\n" +
                    "  <s:Body>\n" +
                    "    <wsen:Enumerate/>" +
                    "  </s:Body> \n" +
                    "</s:Envelope>";

            final Document result = processRequest( "http://schemas.xmlsoap.org/ws/2004/09/enumeration/Enumerate", MessageFormat.format(message, resourceUri) );

            final Element soapBody = SoapUtil.getBodyElement(result);
            final Element enumerateResponse = XmlUtil.findExactlyOneChildElementByName(soapBody, NS_WS_ENUMERATION, "EnumerateResponse");
            final Element enumerationContext = XmlUtil.findExactlyOneChildElementByName(enumerateResponse, NS_WS_ENUMERATION, "EnumerationContext");

            final String context = XmlUtil.getTextValue(enumerationContext);

            assertNotNull("Valid enumeration context " + resourceUri, context);
            assertFalse("Valid enumeration context " + resourceUri, context.trim().isEmpty());

            final String pullMessage =
                    "<s:Envelope xmlns:s=\"http://www.w3.org/2003/05/soap-envelope\" \n" +
                    "            xmlns:a=\"http://schemas.xmlsoap.org/ws/2004/08/addressing\" \n" +
                    "            xmlns:wsen=\"http://schemas.xmlsoap.org/ws/2004/09/enumeration\" \n" +
                    "            xmlns:w=\"http://schemas.dmtf.org/wbem/wsman/1/wsman.xsd\">\n" +
                    "  <s:Header>\n" +
                    "    <a:MessageID>uuid:4ED2993C-4339-4E99-81FC-C2FD3812781A</a:MessageID> \n" +
                    "    <a:To>http://127.0.0.1:8080/wsman</a:To> \n" +
                    "    <a:ReplyTo> \n" +
                    "      <a:Address s:mustUnderstand=\"true\">http://schemas.xmlsoap.org/ws/2004/08/addressing/role/anonymous</a:Address> \n" +
                    "    </a:ReplyTo> \n" +
                    "    <a:Action s:mustUnderstand=\"true\">http://schemas.xmlsoap.org/ws/2004/09/enumeration/Pull</a:Action> \n" +
                    "    <w:ResourceURI s:mustUnderstand=\"true\">{0}</w:ResourceURI> \n" +
                    "    <w:OperationTimeout>PT60.000S</w:OperationTimeout> \n" +
                    "  </s:Header>\n" +
                    "  <s:Body>\n" +
                    "    <wsen:Pull>\n" +
                    "      <wsen:EnumerationContext>{1}</wsen:EnumerationContext>\n" +
                    "      <wsen:MaxElements>1000</wsen:MaxElements>\n" +
                    "    </wsen:Pull>\n" +
                    "  </s:Body> \n" +
                    "</s:Envelope>";

            final Document pullResult = processRequest( "http://schemas.xmlsoap.org/ws/2004/09/enumeration/Pull", MessageFormat.format(pullMessage, resourceUri, context));

            final Element soapBody2 = SoapUtil.getBodyElement(pullResult);
            final Element pullResponse = XmlUtil.findExactlyOneChildElementByName(soapBody2, NS_WS_ENUMERATION, "PullResponse");
            final Element items = XmlUtil.findExactlyOneChildElementByName(pullResponse, NS_WS_ENUMERATION, "Items");
            assertTrue( "Enumeration not empty " + resourceUri, items.hasChildNodes() );
            XmlUtil.findExactlyOneChildElementByName(pullResponse, NS_WS_ENUMERATION, "EndOfSequence");
        }
    }

    @Test
    public void testPolicyExport() throws Exception {
        final String message =
                "<env:Envelope xmlns:env=\"http://www.w3.org/2003/05/soap-envelope\" xmlns:wsa=\"http://schemas.xmlsoap.org/ws/2004/08/addressing\" xmlns:wsman=\"http://schemas.dmtf.org/wbem/wsman/1/wsman.xsd\" xmlns:wxf=\"http://schemas.xmlsoap.org/ws/2004/09/transfer\" xmlns:xs=\"http://www.w3.org/2001/XMLSchema\"><env:Header><wsa:Action env:mustUnderstand=\"true\">http://ns.l7tech.com/2010/04/gateway-management/policies/ExportPolicy</wsa:Action><wsa:ReplyTo><wsa:Address env:mustUnderstand=\"true\">http://schemas.xmlsoap.org/ws/2004/08/addressing/role/anonymous</wsa:Address></wsa:ReplyTo><wsa:MessageID env:mustUnderstand=\"true\">uuid:d521f4b4-ef2a-4381-b11c-fa72eb760a99</wsa:MessageID><wsa:To env:mustUnderstand=\"true\">http://localhost:8080/wsman</wsa:To><wsman:ResourceURI>http://ns.l7tech.com/2010/04/gateway-management/policies</wsman:ResourceURI><wsman:OperationTimeout>P0Y0M0DT0H5M0.000S</wsman:OperationTimeout><wsman:SelectorSet><wsman:Selector Name=\"id\">"+new Goid(0,1)+"</wsman:Selector></wsman:SelectorSet></env:Header><env:Body/></env:Envelope>";

        final Document result = processRequest( "http://ns.l7tech.com/2010/04/gateway-management/policies/ExportPolicy", message );

        final Element soapBody = SoapUtil.getBodyElement(result);
        final Element policyExportResult = XmlUtil.findExactlyOneChildElementByName(soapBody, NS_GATEWAY_MANAGEMENT, "PolicyExportResult");
        final Element resource = XmlUtil.findExactlyOneChildElementByName(policyExportResult, NS_GATEWAY_MANAGEMENT, "Resource");

        assertEquals( "resource type", "policyexport", resource.getAttribute("type"));
        assertTrue( "resource content is export", XmlUtil.getTextValue(resource).contains( "<exp:Export " ));
    }

    @Test
    public void testPolicySetVersionComment() throws Exception {
        final String message =
                "<env:Envelope xmlns:env=\"http://www.w3.org/2003/05/soap-envelope\" xmlns:wsa=\"http://schemas.xmlsoap.org/ws/2004/08/addressing\" xmlns:wsman=\"http://schemas.dmtf.org/wbem/wsman/1/wsman.xsd\" xmlns:wxf=\"http://schemas.xmlsoap.org/ws/2004/09/transfer\" xmlns:xs=\"http://www.w3.org/2001/XMLSchema\"><env:Header><wsa:Action env:mustUnderstand=\"true\">http://ns.l7tech.com/2010/04/gateway-management/policies/SetVersionComment</wsa:Action><wsa:ReplyTo><wsa:Address env:mustUnderstand=\"true\">http://schemas.xmlsoap.org/ws/2004/08/addressing/role/anonymous</wsa:Address></wsa:ReplyTo><wsa:MessageID env:mustUnderstand=\"true\">uuid:d0f59849-9eaa-4027-8b2e-f5ec2dfc1f9d</wsa:MessageID><wsa:To env:mustUnderstand=\"true\">http://localhost:8080/wsman</wsa:To><wsman:ResourceURI>http://ns.l7tech.com/2010/04/gateway-management/policies</wsman:ResourceURI><wsman:OperationTimeout>P0Y0M0DT0H5M0.000S</wsman:OperationTimeout><wsman:SelectorSet><wsman:Selector Name=\"id\">"+new Goid(0,1)+"</wsman:Selector></wsman:SelectorSet></env:Header>" +
                        "<env:Body><VersionComment xmlns=\"http://ns.l7tech.com/2010/04/gateway-management\">" +
                        "<Comment>New Comment</Comment>" +
                        "</VersionComment></env:Body></env:Envelope>";

        final Document result = processRequest( "http://ns.l7tech.com/2010/04/gateway-management/policies/ImportPolicy", message );

        final Element soapBody = SoapUtil.getBodyElement(result);
        assertNull("No body content", soapBody.getFirstChild());
    }

    @Test
    public void testPolicySetVersion1Comment() throws Exception {
        final String message =
                "<env:Envelope xmlns:env=\"http://www.w3.org/2003/05/soap-envelope\" xmlns:wsa=\"http://schemas.xmlsoap.org/ws/2004/08/addressing\" xmlns:wsman=\"http://schemas.dmtf.org/wbem/wsman/1/wsman.xsd\" xmlns:wxf=\"http://schemas.xmlsoap.org/ws/2004/09/transfer\" xmlns:xs=\"http://www.w3.org/2001/XMLSchema\"><env:Header><wsa:Action env:mustUnderstand=\"true\">http://ns.l7tech.com/2010/04/gateway-management/policies/SetVersionComment</wsa:Action><wsa:ReplyTo><wsa:Address env:mustUnderstand=\"true\">http://schemas.xmlsoap.org/ws/2004/08/addressing/role/anonymous</wsa:Address></wsa:ReplyTo><wsa:MessageID env:mustUnderstand=\"true\">uuid:d0f59849-9eaa-4027-8b2e-f5ec2dfc1f9d</wsa:MessageID><wsa:To env:mustUnderstand=\"true\">http://localhost:8080/wsman</wsa:To><wsman:ResourceURI>http://ns.l7tech.com/2010/04/gateway-management/policies</wsman:ResourceURI><wsman:OperationTimeout>P0Y0M0DT0H5M0.000S</wsman:OperationTimeout><wsman:SelectorSet><wsman:Selector Name=\"id\">"+new Goid(0,1)+"</wsman:Selector></wsman:SelectorSet></env:Header>" +
                        "<env:Body><VersionComment xmlns=\"http://ns.l7tech.com/2010/04/gateway-management\">" +
                        "<Comment>New Comment</Comment><VersionNumber>1</VersionNumber>" +
                        "</VersionComment></env:Body></env:Envelope>";

        final Document result = processRequest( "http://ns.l7tech.com/2010/04/gateway-management/policies/ImportPolicy", message );

        final Element soapBody = SoapUtil.getBodyElement(result);
        assertNull("No body content", soapBody.getFirstChild());
    }

    @Test
    public void testPolicySetVersion2CommentFails() throws Exception {
        final String message =
                "<env:Envelope xmlns:env=\"http://www.w3.org/2003/05/soap-envelope\" xmlns:wsa=\"http://schemas.xmlsoap.org/ws/2004/08/addressing\" xmlns:wsman=\"http://schemas.dmtf.org/wbem/wsman/1/wsman.xsd\" xmlns:wxf=\"http://schemas.xmlsoap.org/ws/2004/09/transfer\" xmlns:xs=\"http://www.w3.org/2001/XMLSchema\"><env:Header><wsa:Action env:mustUnderstand=\"true\">http://ns.l7tech.com/2010/04/gateway-management/policies/SetVersionComment</wsa:Action><wsa:ReplyTo><wsa:Address env:mustUnderstand=\"true\">http://schemas.xmlsoap.org/ws/2004/08/addressing/role/anonymous</wsa:Address></wsa:ReplyTo><wsa:MessageID env:mustUnderstand=\"true\">uuid:d0f59849-9eaa-4027-8b2e-f5ec2dfc1f9d</wsa:MessageID><wsa:To env:mustUnderstand=\"true\">http://localhost:8080/wsman</wsa:To><wsman:ResourceURI>http://ns.l7tech.com/2010/04/gateway-management/policies</wsman:ResourceURI><wsman:OperationTimeout>P0Y0M0DT0H5M0.000S</wsman:OperationTimeout><wsman:SelectorSet><wsman:Selector Name=\"id\">"+new Goid(0,1)+"</wsman:Selector></wsman:SelectorSet></env:Header>" +
                        "<env:Body><VersionComment xmlns=\"http://ns.l7tech.com/2010/04/gateway-management\">" +
                        "<Comment>New Comment</Comment><VersionNumber>2</VersionNumber>" +
                        "</VersionComment></env:Body></env:Envelope>";

        final Document result = processRequest( "http://ns.l7tech.com/2010/04/gateway-management/policies/ImportPolicy", message );

        final Element soapBody = SoapUtil.getBodyElement(result);
        final Element soapFault = XmlUtil.findExactlyOneChildElementByName(soapBody, SOAPConstants.URI_NS_SOAP_1_2_ENVELOPE, "Fault");
        final Element code = XmlUtil.findExactlyOneChildElementByName(soapFault, SOAPConstants.URI_NS_SOAP_1_2_ENVELOPE, "Code");
        final Element subcode = XmlUtil.findExactlyOneChildElementByName(code, SOAPConstants.URI_NS_SOAP_1_2_ENVELOPE, "Subcode");
        final Element value = XmlUtil.findExactlyOneChildElementByName(subcode, SOAPConstants.URI_NS_SOAP_1_2_ENVELOPE, "Value");
        assertEquals("SOAP Fault value", "wxf:InvalidRepresentation", XmlUtil.getTextValue(value));
    }


    @BugId("SSG-8371")
    @Test
    public void testPolicySetVersionLongCommentFails() throws Exception {
        // set comment longer than 255 characters
        final String message =
                "<env:Envelope xmlns:env=\"http://www.w3.org/2003/05/soap-envelope\" xmlns:wsa=\"http://schemas.xmlsoap.org/ws/2004/08/addressing\" xmlns:wsman=\"http://schemas.dmtf.org/wbem/wsman/1/wsman.xsd\" xmlns:wxf=\"http://schemas.xmlsoap.org/ws/2004/09/transfer\" xmlns:xs=\"http://www.w3.org/2001/XMLSchema\"><env:Header><wsa:Action env:mustUnderstand=\"true\">http://ns.l7tech.com/2010/04/gateway-management/policies/SetVersionComment</wsa:Action><wsa:ReplyTo><wsa:Address env:mustUnderstand=\"true\">http://schemas.xmlsoap.org/ws/2004/08/addressing/role/anonymous</wsa:Address></wsa:ReplyTo><wsa:MessageID env:mustUnderstand=\"true\">uuid:d0f59849-9eaa-4027-8b2e-f5ec2dfc1f9d</wsa:MessageID><wsa:To env:mustUnderstand=\"true\">http://localhost:8080/wsman</wsa:To><wsman:ResourceURI>http://ns.l7tech.com/2010/04/gateway-management/policies</wsman:ResourceURI><wsman:OperationTimeout>P0Y0M0DT0H5M0.000S</wsman:OperationTimeout><wsman:SelectorSet><wsman:Selector Name=\"id\">"+new Goid(0,1)+"</wsman:Selector></wsman:SelectorSet></env:Header>" +
                        "<env:Body><VersionComment xmlns=\"http://ns.l7tech.com/2010/04/gateway-management\">" +
                        "<Comment>"+TextUtils.pad("Long comment",300)+"end comment"+"</Comment>" +
                        "</VersionComment></env:Body></env:Envelope>";

        final Document result = processRequest( "http://ns.l7tech.com/2010/04/gateway-management/policies/ImportPolicy", message );

        final Element soapBody = SoapUtil.getBodyElement(result);
        final Element soapFault = XmlUtil.findExactlyOneChildElementByName(soapBody, SOAPConstants.URI_NS_SOAP_1_2_ENVELOPE, "Fault");
        final Element code = XmlUtil.findExactlyOneChildElementByName(soapFault, SOAPConstants.URI_NS_SOAP_1_2_ENVELOPE, "Code");
        final Element subcode = XmlUtil.findExactlyOneChildElementByName(code, SOAPConstants.URI_NS_SOAP_1_2_ENVELOPE, "Subcode");
        final Element value = XmlUtil.findExactlyOneChildElementByName(subcode, SOAPConstants.URI_NS_SOAP_1_2_ENVELOPE, "Value");
        assertEquals("SOAP Fault value", "wxf:InvalidRepresentation", XmlUtil.getTextValue(value));
    }

    @Test
    public void testServiceSetVersionComment() throws Exception {
        final String message =
                "<env:Envelope xmlns:env=\"http://www.w3.org/2003/05/soap-envelope\" xmlns:wsa=\"http://schemas.xmlsoap.org/ws/2004/08/addressing\" xmlns:wsman=\"http://schemas.dmtf.org/wbem/wsman/1/wsman.xsd\" xmlns:wxf=\"http://schemas.xmlsoap.org/ws/2004/09/transfer\" xmlns:xs=\"http://www.w3.org/2001/XMLSchema\"><env:Header><wsa:Action env:mustUnderstand=\"true\">http://ns.l7tech.com/2010/04/gateway-management/services/SetVersionComment</wsa:Action><wsa:ReplyTo><wsa:Address env:mustUnderstand=\"true\">http://schemas.xmlsoap.org/ws/2004/08/addressing/role/anonymous</wsa:Address></wsa:ReplyTo><wsa:MessageID env:mustUnderstand=\"true\">uuid:d0f59849-9eaa-4027-8b2e-f5ec2dfc1f9d</wsa:MessageID><wsa:To env:mustUnderstand=\"true\">http://localhost:8080/wsman</wsa:To><wsman:ResourceURI>http://ns.l7tech.com/2010/04/gateway-management/services</wsman:ResourceURI><wsman:OperationTimeout>P0Y0M0DT0H5M0.000S</wsman:OperationTimeout><wsman:SelectorSet><wsman:Selector Name=\"id\">"+new Goid(0,1)+"</wsman:Selector></wsman:SelectorSet></env:Header>" +
                        "<env:Body><VersionComment xmlns=\"http://ns.l7tech.com/2010/04/gateway-management\">" +
                        "<Comment>New Comment</Comment>" +
                        "</VersionComment></env:Body></env:Envelope>";

        final Document result = processRequest( "http://ns.l7tech.com/2010/04/gateway-management/services/SetVersionComment", message );

        final Element soapBody = SoapUtil.getBodyElement(result);
        assertNull("No body content", soapBody.getFirstChild());
    }

    @Test
    public void testServiceSetVersion1Comment() throws Exception {
        final String message =
                "<env:Envelope xmlns:env=\"http://www.w3.org/2003/05/soap-envelope\" xmlns:wsa=\"http://schemas.xmlsoap.org/ws/2004/08/addressing\" xmlns:wsman=\"http://schemas.dmtf.org/wbem/wsman/1/wsman.xsd\" xmlns:wxf=\"http://schemas.xmlsoap.org/ws/2004/09/transfer\" xmlns:xs=\"http://www.w3.org/2001/XMLSchema\"><env:Header><wsa:Action env:mustUnderstand=\"true\">http://ns.l7tech.com/2010/04/gateway-management/services/SetVersionComment</wsa:Action><wsa:ReplyTo><wsa:Address env:mustUnderstand=\"true\">http://schemas.xmlsoap.org/ws/2004/08/addressing/role/anonymous</wsa:Address></wsa:ReplyTo><wsa:MessageID env:mustUnderstand=\"true\">uuid:d0f59849-9eaa-4027-8b2e-f5ec2dfc1f9d</wsa:MessageID><wsa:To env:mustUnderstand=\"true\">http://localhost:8080/wsman</wsa:To><wsman:ResourceURI>http://ns.l7tech.com/2010/04/gateway-management/services</wsman:ResourceURI><wsman:OperationTimeout>P0Y0M0DT0H5M0.000S</wsman:OperationTimeout><wsman:SelectorSet><wsman:Selector Name=\"id\">"+new Goid(0,1)+"</wsman:Selector></wsman:SelectorSet></env:Header>" +
                        "<env:Body><VersionComment xmlns=\"http://ns.l7tech.com/2010/04/gateway-management\">" +
                        "<Comment>New Comment</Comment><VersionNumber>1</VersionNumber>" +
                        "</VersionComment></env:Body></env:Envelope>";

        final Document result = processRequest( "http://ns.l7tech.com/2010/04/gateway-management/services/SetVersionComment", message );

        final Element soapBody = SoapUtil.getBodyElement(result);
        assertNull("No body content", soapBody.getFirstChild());
    }

    @Test
    public void testServiceSetVersion2CommentFails() throws Exception {
        final String message =
                "<env:Envelope xmlns:env=\"http://www.w3.org/2003/05/soap-envelope\" xmlns:wsa=\"http://schemas.xmlsoap.org/ws/2004/08/addressing\" xmlns:wsman=\"http://schemas.dmtf.org/wbem/wsman/1/wsman.xsd\" xmlns:wxf=\"http://schemas.xmlsoap.org/ws/2004/09/transfer\" xmlns:xs=\"http://www.w3.org/2001/XMLSchema\"><env:Header><wsa:Action env:mustUnderstand=\"true\">http://ns.l7tech.com/2010/04/gateway-management/services/SetVersionComment</wsa:Action><wsa:ReplyTo><wsa:Address env:mustUnderstand=\"true\">http://schemas.xmlsoap.org/ws/2004/08/addressing/role/anonymous</wsa:Address></wsa:ReplyTo><wsa:MessageID env:mustUnderstand=\"true\">uuid:d0f59849-9eaa-4027-8b2e-f5ec2dfc1f9d</wsa:MessageID><wsa:To env:mustUnderstand=\"true\">http://localhost:8080/wsman</wsa:To><wsman:ResourceURI>http://ns.l7tech.com/2010/04/gateway-management/services</wsman:ResourceURI><wsman:OperationTimeout>P0Y0M0DT0H5M0.000S</wsman:OperationTimeout><wsman:SelectorSet><wsman:Selector Name=\"id\">"+new Goid(0,1)+"</wsman:Selector></wsman:SelectorSet></env:Header>" +
                        "<env:Body><VersionComment xmlns=\"http://ns.l7tech.com/2010/04/gateway-management\">" +
                        "<Comment>New Comment</Comment><VersionNumber>2</VersionNumber>" +
                        "</VersionComment></env:Body></env:Envelope>";

        final Document result = processRequest( "http://ns.l7tech.com/2010/04/gateway-management/services/SetVersionComment", message );

        final Element soapBody = SoapUtil.getBodyElement(result);
        final Element soapFault = XmlUtil.findExactlyOneChildElementByName(soapBody, SOAPConstants.URI_NS_SOAP_1_2_ENVELOPE, "Fault");
        final Element code = XmlUtil.findExactlyOneChildElementByName(soapFault, SOAPConstants.URI_NS_SOAP_1_2_ENVELOPE, "Code");
        final Element subcode = XmlUtil.findExactlyOneChildElementByName(code, SOAPConstants.URI_NS_SOAP_1_2_ENVELOPE, "Subcode");
        final Element value = XmlUtil.findExactlyOneChildElementByName(subcode, SOAPConstants.URI_NS_SOAP_1_2_ENVELOPE, "Value");
        assertEquals("SOAP Fault value", "wxf:InvalidRepresentation", XmlUtil.getTextValue(value));
    }

    @BugId("SSG-8371")
    @Test
    public void testServiceSetVersionLongCommentFails() throws Exception {
        // set comment longer than 255 characters
        final String message =
                "<env:Envelope xmlns:env=\"http://www.w3.org/2003/05/soap-envelope\" xmlns:wsa=\"http://schemas.xmlsoap.org/ws/2004/08/addressing\" xmlns:wsman=\"http://schemas.dmtf.org/wbem/wsman/1/wsman.xsd\" xmlns:wxf=\"http://schemas.xmlsoap.org/ws/2004/09/transfer\" xmlns:xs=\"http://www.w3.org/2001/XMLSchema\"><env:Header><wsa:Action env:mustUnderstand=\"true\">http://ns.l7tech.com/2010/04/gateway-management/services/SetVersionComment</wsa:Action><wsa:ReplyTo><wsa:Address env:mustUnderstand=\"true\">http://schemas.xmlsoap.org/ws/2004/08/addressing/role/anonymous</wsa:Address></wsa:ReplyTo><wsa:MessageID env:mustUnderstand=\"true\">uuid:d0f59849-9eaa-4027-8b2e-f5ec2dfc1f9d</wsa:MessageID><wsa:To env:mustUnderstand=\"true\">http://localhost:8080/wsman</wsa:To><wsman:ResourceURI>http://ns.l7tech.com/2010/04/gateway-management/services</wsman:ResourceURI><wsman:OperationTimeout>P0Y0M0DT0H5M0.000S</wsman:OperationTimeout><wsman:SelectorSet><wsman:Selector Name=\"id\">"+new Goid(0,1)+"</wsman:Selector></wsman:SelectorSet></env:Header>" +
                        "<env:Body><VersionComment xmlns=\"http://ns.l7tech.com/2010/04/gateway-management\">" +
                        "<Comment>"+TextUtils.pad("Long comment",300)+"</Comment>" +
                        "</VersionComment></env:Body></env:Envelope>";

        final Document result = processRequest( "http://ns.l7tech.com/2010/04/gateway-management/policies/ImportPolicy", message );

        final Element soapBody = SoapUtil.getBodyElement(result);
        final Element soapFault = XmlUtil.findExactlyOneChildElementByName(soapBody, SOAPConstants.URI_NS_SOAP_1_2_ENVELOPE, "Fault");
        final Element code = XmlUtil.findExactlyOneChildElementByName(soapFault, SOAPConstants.URI_NS_SOAP_1_2_ENVELOPE, "Code");
        final Element subcode = XmlUtil.findExactlyOneChildElementByName(code, SOAPConstants.URI_NS_SOAP_1_2_ENVELOPE, "Subcode");
        final Element value = XmlUtil.findExactlyOneChildElementByName(subcode, SOAPConstants.URI_NS_SOAP_1_2_ENVELOPE, "Value");
        assertEquals("SOAP Fault value", "wxf:InvalidRepresentation", XmlUtil.getTextValue(value));
    }

    @Test
    public void testPolicyImport() throws Exception {
        final String message =
                "<env:Envelope xmlns:env=\"http://www.w3.org/2003/05/soap-envelope\" xmlns:wsa=\"http://schemas.xmlsoap.org/ws/2004/08/addressing\" xmlns:wsman=\"http://schemas.dmtf.org/wbem/wsman/1/wsman.xsd\" xmlns:wxf=\"http://schemas.xmlsoap.org/ws/2004/09/transfer\" xmlns:xs=\"http://www.w3.org/2001/XMLSchema\"><env:Header><wsa:Action env:mustUnderstand=\"true\">http://ns.l7tech.com/2010/04/gateway-management/policies/ImportPolicy</wsa:Action><wsa:ReplyTo><wsa:Address env:mustUnderstand=\"true\">http://schemas.xmlsoap.org/ws/2004/08/addressing/role/anonymous</wsa:Address></wsa:ReplyTo><wsa:MessageID env:mustUnderstand=\"true\">uuid:d0f59849-9eaa-4027-8b2e-f5ec2dfc1f9d</wsa:MessageID><wsa:To env:mustUnderstand=\"true\">http://localhost:8080/wsman</wsa:To><wsman:ResourceURI>http://ns.l7tech.com/2010/04/gateway-management/policies</wsman:ResourceURI><wsman:OperationTimeout>P0Y0M0DT0H5M0.000S</wsman:OperationTimeout><wsman:SelectorSet><wsman:Selector Name=\"id\">"+new Goid(0,1)+"</wsman:Selector></wsman:SelectorSet></env:Header><env:Body><PolicyImportContext xmlns=\"http://ns.l7tech.com/2010/04/gateway-management\"><Properties/><Resource type=\"policyexport\">&lt;?xml version=\"1.0\" encoding=\"UTF-8\"?&gt;\n" +
                        "&lt;exp:Export Version=\"3.0\"\n" +
                        "    xmlns:L7p=\"http://www.layer7tech.com/ws/policy\"\n" +
                        "    xmlns:exp=\"http://www.layer7tech.com/ws/policy/export\" xmlns:wsp=\"http://schemas.xmlsoap.org/ws/2002/12/policy\"&gt;\n" +
                        "    &lt;exp:References/&gt;\n" +
                        "    &lt;wsp:Policy xmlns:L7p=\"http://www.layer7tech.com/ws/policy\" xmlns:wsp=\"http://schemas.xmlsoap.org/ws/2002/12/policy\"&gt;\n" +
                        "        &lt;wsp:All wsp:Usage=\"Required\"&gt;\n" +
                        "            &lt;L7p:AuditAssertion/&gt;\n" +
                        "        &lt;/wsp:All&gt;\n" +
                        "    &lt;/wsp:Policy&gt;\n" +
                        "&lt;/exp:Export&gt;\n" +
                        "</Resource></PolicyImportContext></env:Body></env:Envelope>";

        final Document result = processRequest( "http://ns.l7tech.com/2010/04/gateway-management/policies/ImportPolicy", message );

        final Element soapBody = SoapUtil.getBodyElement(result);
        XmlUtil.findExactlyOneChildElementByName(soapBody, NS_GATEWAY_MANAGEMENT, "PolicyImportResult");
    }

    @Test
    public void testPolicyImportWithReferences() throws Exception {
        final String message =
                "<env:Envelope xmlns:env=\"http://www.w3.org/2003/05/soap-envelope\" xmlns:wsa=\"http://schemas.xmlsoap.org/ws/2004/08/addressing\" xmlns:wsman=\"http://schemas.dmtf.org/wbem/wsman/1/wsman.xsd\" xmlns:wxf=\"http://schemas.xmlsoap.org/ws/2004/09/transfer\" xmlns:xs=\"http://www.w3.org/2001/XMLSchema\"><env:Header><wsa:Action env:mustUnderstand=\"true\">http://ns.l7tech.com/2010/04/gateway-management/policies/ImportPolicy</wsa:Action><wsa:ReplyTo><wsa:Address env:mustUnderstand=\"true\">http://schemas.xmlsoap.org/ws/2004/08/addressing/role/anonymous</wsa:Address></wsa:ReplyTo><wsa:MessageID env:mustUnderstand=\"true\">uuid:d0f59849-9eaa-4027-8b2e-f5ec2dfc1f9d</wsa:MessageID><wsa:To env:mustUnderstand=\"true\">http://localhost:8080/wsman</wsa:To><wsman:ResourceURI>http://ns.l7tech.com/2010/04/gateway-management/policies</wsman:ResourceURI><wsman:OperationTimeout>P0Y0M0DT0H5M0.000S</wsman:OperationTimeout><wsman:SelectorSet><wsman:Selector Name=\"id\">"+new Goid(0,1)+"</wsman:Selector></wsman:SelectorSet></env:Header><env:Body><PolicyImportContext xmlns=\"http://ns.l7tech.com/2010/04/gateway-management\"><Properties/><Resource type=\"policyexport\">&lt;?xml version=\"1.0\" encoding=\"UTF-8\"?&gt;\n" +
                        "&lt;exp:Export Version=\"3.0\"\n" +
                        "    xmlns:L7p=\"http://www.layer7tech.com/ws/policy\"\n" +
                        "    xmlns:exp=\"http://www.layer7tech.com/ws/policy/export\" xmlns:wsp=\"http://schemas.xmlsoap.org/ws/2002/12/policy\"&gt;\n" +
                        "    &lt;exp:References>\n" +
                        "        &lt;IDProviderReference RefType=\"com.l7tech.console.policy.exporter.IdProviderReference\">\n" +
                        "            &lt;OID&gt;-2&lt;/OID&gt;\n" +
                        "            &lt;Name&gt;Internal Identity Provider&lt;/Name&gt;\n" +
                        "            &lt;TypeVal&gt;2&lt;/TypeVal&gt;\n" +
                        "        &lt;/IDProviderReference&gt;\n" +
                        "        &lt;JdbcConnectionReference RefType=\"com.l7tech.console.policy.exporter.JdbcConnectionReference\"&gt;\n" +
                        "            &lt;ConnectionName&gt;Test Connection&lt;/ConnectionName&gt;\n" +
                        "            &lt;DriverClass&gt;d&lt;/DriverClass&gt;\n" +
                        "            &lt;JdbcUrl&gt;j&lt;/JdbcUrl&gt;\n" +
                        "            &lt;UserName&gt;u&lt;/UserName&gt;\n" +
                        "        &lt;/JdbcConnectionReference&gt;\n" +
                        "        &lt;ExternalSchema\n" +
                        "            RefType=\"com.l7tech.console.policy.exporter.ExternalSchemaReference\" schemaLocation=\"books_refd.xsd\"/&gt;\n" +
                        "        &lt;IncludedPolicyReference\n" +
                        "            RefType=\"com.l7tech.console.policy.exporter.IncludedPolicyReference\"\n" +
                        "            guid=\"886ece03-a64c-4c17-93cf-ce49e7265daa\" included=\"true\"\n" +
                        "            name=\"Imported Policy Include Fragment\" soap=\"false\" type=\"INCLUDE_FRAGMENT\"&gt;\n" +
                        "            &lt;wsp:Policy xmlns:L7p=\"http://www.layer7tech.com/ws/policy\" xmlns:wsp=\"http://schemas.xmlsoap.org/ws/2002/12/policy\"&gt;\n" +
                        "                &lt;wsp:All wsp:Usage=\"Required\"&gt;\n" +
                        "                    &lt;L7p:AuditDetailAssertion&gt;\n" +
                        "                        &lt;L7p:Detail stringValue=\"Policy Fragment: Imported Policy Include Fragment\"/&gt;\n" +
                        "                    &lt;/L7p:AuditDetailAssertion&gt;\n" +
                        "                &lt;/wsp:All&gt;\n" +
                        "            &lt;/wsp:Policy&gt;\n" +
                        "        &lt;/IncludedPolicyReference&gt;\n" +
                        "        &lt;TrustedCertificateReference RefType=\"com.l7tech.console.policy.exporter.TrustedCertReference\"&gt;\n" +
                        "            &lt;GOID&gt;" + new Goid(0,2).toHexString() + "&lt;/GOID&gt;\n" +
                        "            &lt;CertificateName&gt;Bob&lt;/CertificateName&gt;\n" +
                        "            &lt;CertificateIssuerDn&gt;CN=OASIS Interop Test CA, O=OASIS&lt;/CertificateIssuerDn&gt;\n" +
                        "            &lt;CertificateSerialNum&gt;127901500862700997089151460209364726264&lt;/CertificateSerialNum&gt;\n" +
                        "        &lt;/TrustedCertificateReference&gt;\n" +
                        "        &lt;JMSConnectionReference RefType=\"com.l7tech.console.policy.exporter.JMSEndpointReference\"&gt;\n" +
                        "            &lt;GOID&gt;000000000000007b00000000000001c8&lt;/GOID&gt;\n" +
                        "            &lt;InitialContextFactoryClassname&gt;com.context.Classname&lt;/InitialContextFactoryClassname&gt;\n" +
                        "            &lt;JndiUrl&gt;ldap://jndi&lt;/JndiUrl&gt;\n" +
                        "            &lt;QueueFactoryUrl&gt;qcf&lt;/QueueFactoryUrl&gt;\n" +
                        "            &lt;TopicFactoryUrl/&gt;\n" +
                        "            &lt;DestinationFactoryUrl/&gt;\n" +
                        "            &lt;Name&gt;Test Endpoint&lt;/Name&gt;\n" +
                        "            &lt;DestinationName&gt;Test Endpoint&lt;/DestinationName&gt;\n" +
                        "        &lt;/JMSConnectionReference&gt;\n" +
                        "        &lt;PrivateKeyReference RefType=\"com.l7tech.console.policy.exporter.PrivateKeyReference\"&gt;\n" +
                        "            &lt;IsDefaultKey&gt;false&lt;/IsDefaultKey&gt;\n" +
                        "            &lt;KeystoreOID&gt;2&lt;/KeystoreOID&gt;\n" +
                        "            &lt;KeyAlias&gt;alice&lt;/KeyAlias&gt;\n" +
                        "        &lt;/PrivateKeyReference&gt;\n" +
                        "    &lt;/exp:References&gt;\n" +
                        "    &lt;wsp:Policy xmlns:L7p=\"http://www.layer7tech.com/ws/policy\" xmlns:wsp=\"http://schemas.xmlsoap.org/ws/2002/12/policy\"&gt;\n" +
                        "        &lt;wsp:All wsp:Usage=\"Required\"&gt;\n" +
                        "            &lt;L7p:AuditAssertion/&gt;\n" +
                        "            &lt;L7p:Authentication&gt;\n" +
                        "                &lt;L7p:IdentityProviderOid longValue=\"-2\"/&gt;\n" +
                        "            &lt;/L7p:Authentication&gt;\n" +
                        "            &lt;L7p:JdbcQuery&gt;\n" +
                        "                &lt;L7p:ConnectionName stringValue=\"Test Connection\"/&gt;\n" +
                        "                &lt;L7p:SqlQuery stringValue=\"SELECT 1\"/&gt;\n" +
                        "            &lt;/L7p:JdbcQuery&gt;\n" +
                        "            &lt;L7p:SchemaValidation&gt;\n" +
                        "                &lt;L7p:ResourceInfo globalResourceInfo=\"included\"&gt;\n" +
                        "                    &lt;L7p:Id stringValue=\"books_refd.xsd\"/&gt;\n" +
                        "                &lt;/L7p:ResourceInfo&gt;\n" +
                        "                &lt;L7p:Target target=\"REQUEST\"/&gt;\n" +
                        "            &lt;/L7p:SchemaValidation&gt;\n" +
                        "            &lt;L7p:Include&gt;\n" +
                        "                &lt;L7p:PolicyGuid stringValue=\"886ece03-a64c-4c17-93cf-ce49e7265daa\"/&gt;\n" +
                        "            &lt;/L7p:Include&gt;\n" +
                        "            &lt;L7p:WsSecurity&gt;\n" +
                        "                &lt;L7p:RecipientTrustedCertificateOid longValue=\"2\"/&gt;\n" +
                        "            &lt;/L7p:WsSecurity&gt;\n" +
                        "            &lt;L7p:JmsRoutingAssertion&gt;\n" +
                        "                &lt;L7p:EndpointName stringValue=\"Test Endpoint\"/&gt;\n" +
                        "                &lt;L7p:EndpointOid goidValue=\"000000000000007b00000000000001c8\"/&gt;\n" +
                        "                &lt;L7p:RequestJmsMessagePropertyRuleSet jmsMessagePropertyRuleSet=\"included\"&gt;\n" +
                        "                    &lt;L7p:Rules jmsMessagePropertyRuleArray=\"included\"/&gt;\n" +
                        "                &lt;/L7p:RequestJmsMessagePropertyRuleSet&gt;\n" +
                        "                &lt;L7p:ResponseJmsMessagePropertyRuleSet jmsMessagePropertyRuleSet=\"included\"&gt;\n" +
                        "                    &lt;L7p:Rules jmsMessagePropertyRuleArray=\"included\"/&gt;\n" +
                        "                &lt;/L7p:ResponseJmsMessagePropertyRuleSet&gt;\n" +
                        "            &lt;/L7p:JmsRoutingAssertion&gt;\n" +
                        "            &lt;L7p:WssSignElement&gt;\n" +
                        "                &lt;L7p:KeyAlias stringValue=\"alice\"/&gt;\n" +
                        "                &lt;L7p:NonDefaultKeystoreId longValue=\"2\"/&gt;\n" +
                        "                &lt;L7p:UsesDefaultKeyStore booleanValue=\"false\"/&gt;\n" +
                        "            &lt;/L7p:WssSignElement&gt;\n" +
                        "        &lt;/wsp:All&gt;\n" +
                        "    &lt;/wsp:Policy&gt;\n" +
                        "&lt;/exp:Export&gt;\n" +
                        "</Resource></PolicyImportContext></env:Body></env:Envelope>";

        final Document result = processRequest( "http://ns.l7tech.com/2010/04/gateway-management/policies/ImportPolicy", message );

        final Element soapBody = SoapUtil.getBodyElement(result);
        final Element importResult = XmlUtil.findExactlyOneChildElementByName(soapBody, NS_GATEWAY_MANAGEMENT, "PolicyImportResult");
        final Element importedPolicyRefs = XmlUtil.findExactlyOneChildElementByName(importResult, NS_GATEWAY_MANAGEMENT, "ImportedPolicyReferences");
        final List<Element> importedPolicyRef = XmlUtil.findChildElementsByName(importedPolicyRefs, NS_GATEWAY_MANAGEMENT, "ImportedPolicyReference");

        for(Element ele: importedPolicyRef){
            if(ele.getAttribute("referenceType").contains("JMSEndpointReference")){
                assertEquals("JMS reference mapped", "Mapped", ele.getAttribute( "type" ));
                // do nothing
            }else if(ele.getAttribute("referenceType").contains("IncludedPolicyReference")){
                assertEquals("Imported policy ref GUID", "886ece03-a64c-4c17-93cf-ce49e7265daa", ele.getAttribute( "guid" ));
                assertEquals("Imported policy ref type", "Created", ele.getAttribute( "type" ));
            }else{
                assertFalse("Unexpeceted reference type: "+ ele.getAttribute("referenceType"),true);
            }
        }
    }

    @Test
    public void testPolicyImportWithInstructions() throws Exception {
        final String message =
                "<env:Envelope xmlns:env=\"http://www.w3.org/2003/05/soap-envelope\" xmlns:wsa=\"http://schemas.xmlsoap.org/ws/2004/08/addressing\" xmlns:wsman=\"http://schemas.dmtf.org/wbem/wsman/1/wsman.xsd\" xmlns:wxf=\"http://schemas.xmlsoap.org/ws/2004/09/transfer\" xmlns:xs=\"http://www.w3.org/2001/XMLSchema\"><env:Header><wsa:Action env:mustUnderstand=\"true\">http://ns.l7tech.com/2010/04/gateway-management/policies/ImportPolicy</wsa:Action><wsa:ReplyTo><wsa:Address env:mustUnderstand=\"true\">http://schemas.xmlsoap.org/ws/2004/08/addressing/role/anonymous</wsa:Address></wsa:ReplyTo><wsa:MessageID env:mustUnderstand=\"true\">uuid:d0f59849-9eaa-4027-8b2e-f5ec2dfc1f9d</wsa:MessageID><wsa:To env:mustUnderstand=\"true\">http://localhost:8080/wsman</wsa:To><wsman:ResourceURI>http://ns.l7tech.com/2010/04/gateway-management/policies</wsman:ResourceURI><wsman:OperationTimeout>P0Y0M0DT0H5M0.000S</wsman:OperationTimeout><wsman:SelectorSet><wsman:Selector Name=\"id\">"+new Goid(0,1)+"</wsman:Selector></wsman:SelectorSet></env:Header><env:Body><PolicyImportContext xmlns=\"http://ns.l7tech.com/2010/04/gateway-management\"><Properties/><Resource type=\"policyexport\">&lt;?xml version=\"1.0\" encoding=\"UTF-8\"?&gt;\n" +
                        "&lt;exp:Export Version=\"3.0\"\n" +
                        "    xmlns:L7p=\"http://www.layer7tech.com/ws/policy\"\n" +
                        "    xmlns:exp=\"http://www.layer7tech.com/ws/policy/export\" xmlns:wsp=\"http://schemas.xmlsoap.org/ws/2002/12/policy\"&gt;\n" +
                        "    &lt;exp:References>\n" +
                        "        &lt;IDProviderReference RefType=\"com.l7tech.console.policy.exporter.IdProviderReference\">\n" +
                        "            &lt;GOID&gt;000000000000000300000000000000c8&lt;/GOID&gt;\n" +
                        "            &lt;Name&gt;Internal Identity Provider&lt;/Name&gt;\n" +
                        "            &lt;TypeVal&gt;2&lt;/TypeVal&gt;\n" +
                        "        &lt;/IDProviderReference&gt;\n" +
                        "        &lt;JdbcConnectionReference RefType=\"com.l7tech.console.policy.exporter.JdbcConnectionReference\"&gt;\n" +
                        "            &lt;ConnectionName&gt;Invalid Connection&lt;/ConnectionName&gt;\n" +
                        "            &lt;DriverClass&gt;invalid&lt;/DriverClass&gt;\n" +
                        "            &lt;JdbcUrl&gt;invalid&lt;/JdbcUrl&gt;\n" +
                        "            &lt;UserName&gt;invalid&lt;/UserName&gt;\n" +
                        "        &lt;/JdbcConnectionReference&gt;\n" +
                        "        &lt;ExternalSchema\n" +
                        "            RefType=\"com.l7tech.console.policy.exporter.ExternalSchemaReference\" schemaLocation=\"invalid.xsd\" targetNamespace=\"urn:invalid\"/&gt;\n" +
                        "        &lt;IncludedPolicyReference\n" +
                        "            RefType=\"com.l7tech.console.policy.exporter.IncludedPolicyReference\"\n" +
                        "            guid=\"006ece03-a64c-4c17-93cf-ce49e7265daa\" included=\"true\"\n" +
                        "            name=\"Imported Policy Include Fragment\" soap=\"false\" type=\"INCLUDE_FRAGMENT\"&gt;\n" +
                        "            &lt;wsp:Policy xmlns:L7p=\"http://www.layer7tech.com/ws/policy\" xmlns:wsp=\"http://schemas.xmlsoap.org/ws/2002/12/policy\"&gt;\n" +
                        "                &lt;wsp:All wsp:Usage=\"Required\"&gt;\n" +
                        "                    &lt;L7p:AuditDetailAssertion&gt;\n" +
                        "                        &lt;L7p:Detail stringValue=\"Policy Fragment: Imported Policy Include Fragment\"/&gt;\n" +
                        "                        &lt;L7p:Detail stringValue=\"This extra assertion makes the policy conflict\"/&gt;\n" +
                        "                    &lt;/L7p:AuditDetailAssertion&gt;\n" +
                        "                &lt;/wsp:All&gt;\n" +
                        "            &lt;/wsp:Policy&gt;\n" +
                        "        &lt;/IncludedPolicyReference&gt;\n" +
                        "        &lt;TrustedCertificateReference RefType=\"com.l7tech.console.policy.exporter.TrustedCertReference\"&gt;\n" +
                        "            &lt;GOID&gt;00000000000000030000000007ba0000&lt;/GOID&gt;\n" +
                        "            &lt;CertificateName&gt;Invalid&lt;/CertificateName&gt;\n" +
                        "            &lt;CertificateIssuerDn&gt;CN=Invalid&lt;/CertificateIssuerDn&gt;\n" +
                        "            &lt;CertificateSerialNum&gt;997089151460209364726264&lt;/CertificateSerialNum&gt;\n" +
                        "        &lt;/TrustedCertificateReference&gt;\n" +
                        "        &lt;JMSConnectionReference RefType=\"com.l7tech.console.policy.exporter.JMSEndpointReference\"&gt;\n" +
                        "            &lt;GOID&gt;00260000000000a30000000007ba0033&lt;/GOID&gt;\n" +
                        "            &lt;InitialContextFactoryClassname&gt;com.context.OtherClassname&lt;/InitialContextFactoryClassname&gt;\n" +
                        "            &lt;JndiUrl&gt;ldap://host/&lt;/JndiUrl&gt;\n" +
                        "            &lt;QueueFactoryUrl&gt;qcf2&lt;/QueueFactoryUrl&gt;\n" +
                        "            &lt;TopicFactoryUrl/&gt;\n" +
                        "            &lt;DestinationFactoryUrl/&gt;\n" +
                        "            &lt;Name&gt;Invalid Test Endpoint&lt;/Name&gt;\n" +
                        "            &lt;DestinationName&gt;Invalid Test Endpoint&lt;/DestinationName&gt;\n" +
                        "        &lt;/JMSConnectionReference&gt;\n" +
                        "        &lt;PrivateKeyReference RefType=\"com.l7tech.console.policy.exporter.PrivateKeyReference\"&gt;\n" +
                        "            &lt;IsDefaultKey&gt;false&lt;/IsDefaultKey&gt;\n" +
                        "            &lt;KeystoreOID&gt;2&lt;/KeystoreOID&gt;\n" +
                        "            &lt;KeyAlias&gt;invalid&lt;/KeyAlias&gt;\n" +
                        "        &lt;/PrivateKeyReference&gt;\n" +
                        "    &lt;/exp:References&gt;\n" +
                        "    &lt;wsp:Policy xmlns:L7p=\"http://www.layer7tech.com/ws/policy\" xmlns:wsp=\"http://schemas.xmlsoap.org/ws/2002/12/policy\"&gt;\n" +
                        "        &lt;wsp:All wsp:Usage=\"Required\"&gt;\n" +
                        "            &lt;L7p:AuditAssertion/&gt;\n" +
                        "            &lt;L7p:Authentication&gt;\n" +
                        "                &lt;L7p:IdentityProviderOid goidValue=\"000000000000000300000000000000c8\"/&gt;\n" +
                        "            &lt;/L7p:Authentication&gt;\n" +
                        "            &lt;L7p:JdbcQuery&gt;\n" +
                        "                &lt;L7p:ConnectionName stringValue=\"Invalid Connection\"/&gt;\n" +
                        "                &lt;L7p:SqlQuery stringValue=\"SELECT 1\"/&gt;\n" +
                        "            &lt;/L7p:JdbcQuery&gt;\n" +
                        "            &lt;L7p:SchemaValidation&gt;\n" +
                        "                &lt;L7p:ResourceInfo globalResourceInfo=\"included\"&gt;\n" +
                        "                    &lt;L7p:Id stringValue=\"invalid.xsd\"/&gt;\n" +
                        "                &lt;/L7p:ResourceInfo&gt;\n" +
                        "                &lt;L7p:Target target=\"REQUEST\"/&gt;\n" +
                        "            &lt;/L7p:SchemaValidation&gt;\n" +
                        "            &lt;L7p:Include&gt;\n" +
                        "                &lt;L7p:PolicyGuid stringValue=\"006ece03-a64c-4c17-93cf-ce49e7265daa\"/&gt;\n" +
                        "            &lt;/L7p:Include&gt;\n" +
                        "            &lt;L7p:WsSecurity&gt;\n" +
                        "                &lt;L7p:RecipientTrustedCertificateGoid goidValue=\"00000000000000030000000007ba0000\"/&gt;\n" +
                        "            &lt;/L7p:WsSecurity&gt;\n" +
                        "            &lt;L7p:JmsRoutingAssertion&gt;\n" +
                        "                &lt;L7p:EndpointName stringValue=\"Invalid Test Endpoint\"/&gt;\n" +
                        "                &lt;L7p:EndpointOid goidValue=\"00260000000000a30000000007ba0033\"/&gt;\n" +
                        "                &lt;L7p:RequestJmsMessagePropertyRuleSet jmsMessagePropertyRuleSet=\"included\"&gt;\n" +
                        "                    &lt;L7p:Rules jmsMessagePropertyRuleArray=\"included\"/&gt;\n" +
                        "                &lt;/L7p:RequestJmsMessagePropertyRuleSet&gt;\n" +
                        "                &lt;L7p:ResponseJmsMessagePropertyRuleSet jmsMessagePropertyRuleSet=\"included\"&gt;\n" +
                        "                    &lt;L7p:Rules jmsMessagePropertyRuleArray=\"included\"/&gt;\n" +
                        "                &lt;/L7p:ResponseJmsMessagePropertyRuleSet&gt;\n" +
                        "            &lt;/L7p:JmsRoutingAssertion&gt;\n" +
                        "            &lt;L7p:WssSignElement&gt;\n" +
                        "                &lt;L7p:KeyAlias stringValue=\"invalid\"/&gt;\n" +
                        "                &lt;L7p:NonDefaultKeystoreId longValue=\"2\"/&gt;\n" +
                        "                &lt;L7p:UsesDefaultKeyStore booleanValue=\"false\"/&gt;\n" +
                        "            &lt;/L7p:WssSignElement&gt;\n" +
                        "        &lt;/wsp:All&gt;\n" +
                        "    &lt;/wsp:Policy&gt;\n" +
                        "&lt;/exp:Export&gt;\n" +
                        "</Resource>\n" +
                        "<PolicyReferenceInstructions>\n" +
                        "    <PolicyReferenceInstruction type=\"Map\"    referenceType=\"com.l7tech.console.policy.exporter.IdProviderReference\"     referenceId=\"000000000000000300000000000000c8\" mappedReferenceId=\"0000000000000000fffffffffffffffe\"/>\n" +
                        "    <PolicyReferenceInstruction type=\"Ignore\" referenceType=\"com.l7tech.console.policy.exporter.JdbcConnectionReference\" referenceId=\"syn:70ab8caf-35e4-3c3f-a3ae-3685e4b296e0\" />\n" +
                        "    <PolicyReferenceInstruction type=\"Delete\" referenceType=\"com.l7tech.console.policy.exporter.ExternalSchemaReference\" referenceId=\"syn:e69a8c36-66c6-3f0b-ba67-74749c1c62b5\" />\n" +
                        "    <PolicyReferenceInstruction type=\"Rename\" referenceType=\"com.l7tech.console.policy.exporter.IncludedPolicyReference\" referenceId=\"006ece03-a64c-4c17-93cf-ce49e7265daa\" mappedName=\"Renamed Imported Policy Include Fragment\"/>\n" +
                        "    <PolicyReferenceInstruction type=\"Ignore\" referenceType=\"com.l7tech.console.policy.exporter.TrustedCertReference\"    referenceId=\"00000000000000030000000007ba0000\" />\n" +
                        "    <PolicyReferenceInstruction type=\"Ignore\" referenceType=\"com.l7tech.console.policy.exporter.JMSEndpointReference\"    referenceId=\"00260000000000a30000000007ba0033\" />\n" +
                        "    <PolicyReferenceInstruction type=\"Ignore\" referenceType=\"com.l7tech.console.policy.exporter.PrivateKeyReference\"     referenceId=\"2:invalid\" />\n" +
                        "</PolicyReferenceInstructions>\n" +
                        "</PolicyImportContext></env:Body></env:Envelope>";

        final Document result = processRequest( "http://ns.l7tech.com/2010/04/gateway-management/policies/ImportPolicy", message );

        final Element soapBody = SoapUtil.getBodyElement(result);
        final Element importResult = XmlUtil.findExactlyOneChildElementByName(soapBody, NS_GATEWAY_MANAGEMENT, "PolicyImportResult");
        final Element importedPolicyRefs = XmlUtil.findExactlyOneChildElementByName(importResult, NS_GATEWAY_MANAGEMENT, "ImportedPolicyReferences");
        final Element importedPolicyRef = XmlUtil.findExactlyOneChildElementByName(importedPolicyRefs, NS_GATEWAY_MANAGEMENT, "ImportedPolicyReference");

        assertEquals("Imported policy ref GUID", "006ece03-a64c-4c17-93cf-ce49e7265daa", importedPolicyRef.getAttribute( "guid" ));
        assertEquals("Imported policy ref type", "Created", importedPolicyRef.getAttribute( "type" ));
    }

    @Test
    @BugId("SSG-7627")
    public void testPolicyImportWithInstructionsForSSG7627() throws Exception {
        final String message =
                "<env:Envelope xmlns:env=\"http://www.w3.org/2003/05/soap-envelope\"\n" +
                        "    xmlns:wsa=\"http://schemas.xmlsoap.org/ws/2004/08/addressing\"\n" +
                        "    xmlns:wse=\"http://schemas.xmlsoap.org/ws/2004/08/eventing\"\n" +
                        "    xmlns:wsen=\"http://schemas.xmlsoap.org/ws/2004/09/enumeration\"\n" +
                        "    xmlns:wsman=\"http://schemas.dmtf.org/wbem/wsman/1/wsman.xsd\"\n" +
                        "    xmlns:wxf=\"http://schemas.xmlsoap.org/ws/2004/09/transfer\" xmlns:xs=\"http://www.w3.org/2001/XMLSchema\">\n" +
                        "    <env:Header>\n" +
                        "        <wsa:Action env:mustUnderstand=\"true\">http://ns.l7tech.com/2010/04/gateway-management/services/ImportPolicy</wsa:Action>\n" +
                        "        <wsa:ReplyTo>\n" +
                        "            <wsa:Address env:mustUnderstand=\"true\">http://schemas.xmlsoap.org/ws/2004/08/addressing/role/anonymous</wsa:Address>\n" +
                        "        </wsa:ReplyTo>\n" +
                        "        <wsa:MessageID env:mustUnderstand=\"true\">uuid:6ad066ea-9473-4417-a2d0-0ff24f7d4b4b</wsa:MessageID>\n" +
                        "        <wsa:To env:mustUnderstand=\"true\">http://halibut-api1.l7tech.com:8080/wsman</wsa:To>\n" +
                        "        <wsman:ResourceURI>http://ns.l7tech.com/2010/04/gateway-management/services</wsman:ResourceURI>\n" +
                        "        <wsman:OperationTimeout>P0Y0M0DT0H5M0.000S</wsman:OperationTimeout>\n" +
                        "        <wsman:SelectorSet>\n" +
                        "            <wsman:Selector Name=\"id\">"+new Goid(0,1)+"</wsman:Selector>\n" +
                        "        </wsman:SelectorSet>\n" +
                        "    </env:Header>\n" +
                        "    <env:Body>\n" +
                        "        <l7:PolicyImportContext xmlns:l7=\"http://ns.l7tech.com/2010/04/gateway-management\">\n" +
                        "            <l7:Properties/>\n" +
                        "            <l7:Resource type=\"policyexport\">&lt;?xml version=\"1.0\" encoding=\"UTF-8\"?&gt;\n" +
                        "&lt;exp:Export Version=\"3.0\"\n" +
                        "    xmlns:L7p=\"http://www.layer7tech.com/ws/policy\"\n" +
                        "    xmlns:exp=\"http://www.layer7tech.com/ws/policy/export\" xmlns:wsp=\"http://schemas.xmlsoap.org/ws/2002/12/policy\"&gt;\n" +
                        "    &lt;exp:References&gt;\n" +
                        "        &lt;PrivateKeyReference RefType=\"com.l7tech.console.policy.exporter.PrivateKeyReference\"&gt;\n" +
                        "            &lt;IsDefaultKey&gt;false&lt;/IsDefaultKey&gt;\n" +
                        "            &lt;KeystoreOID&gt;00000000000000000000000000000002&lt;/KeystoreOID&gt;\n" +
                        "            &lt;KeyAlias&gt;import-test1&lt;/KeyAlias&gt;\n" +
                        "        &lt;/PrivateKeyReference&gt;\n" +
                        "    &lt;/exp:References&gt;\n" +
                        "    &lt;wsp:Policy xmlns:L7p=\"http://www.layer7tech.com/ws/policy\" xmlns:wsp=\"http://schemas.xmlsoap.org/ws/2002/12/policy\"&gt;\n" +
                        "        &lt;wsp:All wsp:Usage=\"Required\"&gt;\n" +
                        "            &lt;L7p:WssSignElement&gt;\n" +
                        "                &lt;L7p:KeyAlias stringValue=\"import-test1\"/&gt;\n" +
                        "                &lt;L7p:NonDefaultKeystoreId goidValue=\"00000000000000000000000000000002\"/&gt;\n" +
                        "                &lt;L7p:Target target=\"REQUEST\"/&gt;\n" +
                        "                &lt;L7p:UsesDefaultKeyStore booleanValue=\"false\"/&gt;\n" +
                        "                &lt;L7p:XpathExpression xpathExpressionValue=\"included\"&gt;\n" +
                        "                    &lt;L7p:Expression stringValue=\"/s:Envelope/s:Body\"/&gt;\n" +
                        "                    &lt;L7p:Namespaces mapValue=\"included\"&gt;\n" +
                        "                        &lt;L7p:entry&gt;\n" +
                        "                            &lt;L7p:key stringValue=\"s\"/&gt;\n" +
                        "                            &lt;L7p:value stringValue=\"http://schemas.xmlsoap.org/soap/envelope/\"/&gt;\n" +
                        "                        &lt;/L7p:entry&gt;\n" +
                        "                    &lt;/L7p:Namespaces&gt;\n" +
                        "                &lt;/L7p:XpathExpression&gt;\n" +
                        "            &lt;/L7p:WssSignElement&gt;\n" +
                        "        &lt;/wsp:All&gt;\n" +
                        "    &lt;/wsp:Policy&gt;\n" +
                        "&lt;/exp:Export&gt;\n" +
                        "</l7:Resource>\n" +
                        "            <l7:PolicyReferenceInstructions>\n" +
                        "                <l7:PolicyReferenceInstruction mappedReferenceId=\"00000000000000000000000000000002:importkeytest\"\n" +
                        "                    referenceId=\"00000000000000000000000000000002:import-test1\"\n" +
                        "                    referenceType=\"com.l7tech.console.policy.exporter.PrivateKeyReference\" type=\"Map\"/>\n" +
                        "            </l7:PolicyReferenceInstructions>\n" +
                        "        </l7:PolicyImportContext>\n" +
                        "    </env:Body>\n" +
                        "</env:Envelope>\n" +
                        "\n";

        final Document result = processRequest( "http://ns.l7tech.com/2010/04/gateway-management/policies/ImportPolicy", message );

        final Element soapBody = SoapUtil.getBodyElement(result);
        final Element importResult = XmlUtil.findExactlyOneChildElementByName(soapBody, NS_GATEWAY_MANAGEMENT, "PolicyImportResult");
    }


    @BugId("SSG-7634")
    @Test
    public void testPolicyImportWithInstructionsPre80() throws Exception {
        final String message =
                "<env:Envelope xmlns:env=\"http://www.w3.org/2003/05/soap-envelope\" xmlns:wsa=\"http://schemas.xmlsoap.org/ws/2004/08/addressing\" xmlns:wsman=\"http://schemas.dmtf.org/wbem/wsman/1/wsman.xsd\" xmlns:wxf=\"http://schemas.xmlsoap.org/ws/2004/09/transfer\" xmlns:xs=\"http://www.w3.org/2001/XMLSchema\"><env:Header><wsa:Action env:mustUnderstand=\"true\">http://ns.l7tech.com/2010/04/gateway-management/policies/ImportPolicy</wsa:Action><wsa:ReplyTo><wsa:Address env:mustUnderstand=\"true\">http://schemas.xmlsoap.org/ws/2004/08/addressing/role/anonymous</wsa:Address></wsa:ReplyTo><wsa:MessageID env:mustUnderstand=\"true\">uuid:d0f59849-9eaa-4027-8b2e-f5ec2dfc1f9d</wsa:MessageID><wsa:To env:mustUnderstand=\"true\">http://localhost:8080/wsman</wsa:To><wsman:ResourceURI>http://ns.l7tech.com/2010/04/gateway-management/policies</wsman:ResourceURI><wsman:OperationTimeout>P0Y0M0DT0H5M0.000S</wsman:OperationTimeout><wsman:SelectorSet><wsman:Selector Name=\"id\">"+new Goid(0,1)+"</wsman:Selector></wsman:SelectorSet></env:Header><env:Body><PolicyImportContext xmlns=\"http://ns.l7tech.com/2010/04/gateway-management\"><Properties/><Resource type=\"policyexport\">&lt;?xml version=\"1.0\" encoding=\"UTF-8\"?&gt;\n" +
                        "&lt;exp:Export Version=\"3.0\"\n" +
                        "    xmlns:L7p=\"http://www.layer7tech.com/ws/policy\"\n" +
                        "    xmlns:exp=\"http://www.layer7tech.com/ws/policy/export\" xmlns:wsp=\"http://schemas.xmlsoap.org/ws/2002/12/policy\"&gt;\n" +
                        "    &lt;exp:References>\n" +
                        "        &lt;IDProviderReference RefType=\"com.l7tech.console.policy.exporter.IdProviderReference\">\n" +
                        "            &lt;OID&gt;200&lt;/OID&gt;\n" +
                        "            &lt;Name&gt;Internal Identity Provider&lt;/Name&gt;\n" +
                        "            &lt;TypeVal&gt;2&lt;/TypeVal&gt;\n" +
                        "        &lt;/IDProviderReference&gt;\n" +
                        "        &lt;JdbcConnectionReference RefType=\"com.l7tech.console.policy.exporter.JdbcConnectionReference\"&gt;\n" +
                        "            &lt;ConnectionName&gt;Invalid Connection&lt;/ConnectionName&gt;\n" +
                        "            &lt;DriverClass&gt;invalid&lt;/DriverClass&gt;\n" +
                        "            &lt;JdbcUrl&gt;invalid&lt;/JdbcUrl&gt;\n" +
                        "            &lt;UserName&gt;invalid&lt;/UserName&gt;\n" +
                        "        &lt;/JdbcConnectionReference&gt;\n" +
                        "        &lt;ExternalSchema\n" +
                        "            RefType=\"com.l7tech.console.policy.exporter.ExternalSchemaReference\" schemaLocation=\"invalid.xsd\" targetNamespace=\"urn:invalid\"/&gt;\n" +
                        "        &lt;IncludedPolicyReference\n" +
                        "            RefType=\"com.l7tech.console.policy.exporter.IncludedPolicyReference\"\n" +
                        "            guid=\"006ece03-a64c-4c17-93cf-ce49e7265daa\" included=\"true\"\n" +
                        "            name=\"Imported Policy Include Fragment\" soap=\"false\" type=\"INCLUDE_FRAGMENT\"&gt;\n" +
                        "            &lt;wsp:Policy xmlns:L7p=\"http://www.layer7tech.com/ws/policy\" xmlns:wsp=\"http://schemas.xmlsoap.org/ws/2002/12/policy\"&gt;\n" +
                        "                &lt;wsp:All wsp:Usage=\"Required\"&gt;\n" +
                        "                    &lt;L7p:AuditDetailAssertion&gt;\n" +
                        "                        &lt;L7p:Detail stringValue=\"Policy Fragment: Imported Policy Include Fragment\"/&gt;\n" +
                        "                        &lt;L7p:Detail stringValue=\"This extra assertion makes the policy conflict\"/&gt;\n" +
                        "                    &lt;/L7p:AuditDetailAssertion&gt;\n" +
                        "                &lt;/wsp:All&gt;\n" +
                        "            &lt;/wsp:Policy&gt;\n" +
                        "        &lt;/IncludedPolicyReference&gt;\n" +
                        "        &lt;TrustedCertificateReference RefType=\"com.l7tech.console.policy.exporter.TrustedCertReference\"&gt;\n" +
                        "            &lt;OID&gt;129630208&lt;/OID&gt;\n" +
                        "            &lt;CertificateName&gt;Invalid&lt;/CertificateName&gt;\n" +
                        "            &lt;CertificateIssuerDn&gt;CN=Invalid&lt;/CertificateIssuerDn&gt;\n" +
                        "            &lt;CertificateSerialNum&gt;997089151460209364726264&lt;/CertificateSerialNum&gt;\n" +
                        "        &lt;/TrustedCertificateReference&gt;\n" +
                        "        &lt;JMSConnectionReference RefType=\"com.l7tech.console.policy.exporter.JMSEndpointReference\"&gt;\n" +
                        "            &lt;OID&gt;33&lt;/OID&gt;\n" +
                        "            &lt;InitialContextFactoryClassname&gt;com.context.OtherClassname&lt;/InitialContextFactoryClassname&gt;\n" +
                        "            &lt;JndiUrl&gt;ldap://host/&lt;/JndiUrl&gt;\n" +
                        "            &lt;QueueFactoryUrl&gt;qcf2&lt;/QueueFactoryUrl&gt;\n" +
                        "            &lt;TopicFactoryUrl/&gt;\n" +
                        "            &lt;DestinationFactoryUrl/&gt;\n" +
                        "            &lt;Name&gt;Invalid Test Endpoint&lt;/Name&gt;\n" +
                        "            &lt;DestinationName&gt;Invalid Test Endpoint&lt;/DestinationName&gt;\n" +
                        "        &lt;/JMSConnectionReference&gt;\n" +
                        "        &lt;PrivateKeyReference RefType=\"com.l7tech.console.policy.exporter.PrivateKeyReference\"&gt;\n" +
                        "            &lt;IsDefaultKey&gt;false&lt;/IsDefaultKey&gt;\n" +
                        "            &lt;KeystoreOID&gt;2&lt;/KeystoreOID&gt;\n" +
                        "            &lt;KeyAlias&gt;invalid&lt;/KeyAlias&gt;\n" +
                        "        &lt;/PrivateKeyReference&gt;\n" +
                        "    &lt;/exp:References&gt;\n" +
                        "    &lt;wsp:Policy xmlns:L7p=\"http://www.layer7tech.com/ws/policy\" xmlns:wsp=\"http://schemas.xmlsoap.org/ws/2002/12/policy\"&gt;\n" +
                        "        &lt;wsp:All wsp:Usage=\"Required\"&gt;\n" +
                        "            &lt;L7p:AuditAssertion/&gt;\n" +
                        "            &lt;L7p:Authentication&gt;\n" +
                        "                &lt;L7p:IdentityProviderOid longValue=\"200\"/&gt;\n" +
                        "            &lt;/L7p:Authentication&gt;\n" +
                        "            &lt;L7p:JdbcQuery&gt;\n" +
                        "                &lt;L7p:ConnectionName stringValue=\"Invalid Connection\"/&gt;\n" +
                        "                &lt;L7p:SqlQuery stringValue=\"SELECT 1\"/&gt;\n" +
                        "            &lt;/L7p:JdbcQuery&gt;\n" +
                        "            &lt;L7p:SchemaValidation&gt;\n" +
                        "                &lt;L7p:ResourceInfo globalResourceInfo=\"included\"&gt;\n" +
                        "                    &lt;L7p:Id stringValue=\"invalid.xsd\"/&gt;\n" +
                        "                &lt;/L7p:ResourceInfo&gt;\n" +
                        "                &lt;L7p:Target target=\"REQUEST\"/&gt;\n" +
                        "            &lt;/L7p:SchemaValidation&gt;\n" +
                        "            &lt;L7p:Include&gt;\n" +
                        "                &lt;L7p:PolicyGuid stringValue=\"006ece03-a64c-4c17-93cf-ce49e7265daa\"/&gt;\n" +
                        "            &lt;/L7p:Include&gt;\n" +
                        "            &lt;L7p:WsSecurity&gt;\n" +
                        "                &lt;L7p:RecipientTrustedCertificateOid longValue=\"129630208\"/&gt;\n" +
                        "            &lt;/L7p:WsSecurity&gt;\n" +
                        "            &lt;L7p:JmsRoutingAssertion&gt;\n" +
                        "                &lt;L7p:EndpointName stringValue=\"Invalid Test Endpoint\"/&gt;\n" +
                        "                &lt;L7p:EndpointOid boxedLongValue=\"33\"/&gt;\n" +
                        "                &lt;L7p:RequestJmsMessagePropertyRuleSet jmsMessagePropertyRuleSet=\"included\"&gt;\n" +
                        "                    &lt;L7p:Rules jmsMessagePropertyRuleArray=\"included\"/&gt;\n" +
                        "                &lt;/L7p:RequestJmsMessagePropertyRuleSet&gt;\n" +
                        "                &lt;L7p:ResponseJmsMessagePropertyRuleSet jmsMessagePropertyRuleSet=\"included\"&gt;\n" +
                        "                    &lt;L7p:Rules jmsMessagePropertyRuleArray=\"included\"/&gt;\n" +
                        "                &lt;/L7p:ResponseJmsMessagePropertyRuleSet&gt;\n" +
                        "            &lt;/L7p:JmsRoutingAssertion&gt;\n" +
                        "            &lt;L7p:WssSignElement&gt;\n" +
                        "                &lt;L7p:KeyAlias stringValue=\"invalid\"/&gt;\n" +
                        "                &lt;L7p:NonDefaultKeystoreId longValue=\"2\"/&gt;\n" +
                        "                &lt;L7p:UsesDefaultKeyStore booleanValue=\"false\"/&gt;\n" +
                        "            &lt;/L7p:WssSignElement&gt;\n" +
                        "        &lt;/wsp:All&gt;\n" +
                        "    &lt;/wsp:Policy&gt;\n" +
                        "&lt;/exp:Export&gt;\n" +
                        "</Resource>\n" +
                        "<PolicyReferenceInstructions>\n" +
                        "    <PolicyReferenceInstruction type=\"Map\"    referenceType=\"com.l7tech.console.policy.exporter.IdProviderReference\"     referenceId=\"200\" mappedReferenceId=\"0000000000000000fffffffffffffffe\"/>\n" +
                        "    <PolicyReferenceInstruction type=\"Ignore\" referenceType=\"com.l7tech.console.policy.exporter.JdbcConnectionReference\" referenceId=\"syn:70ab8caf-35e4-3c3f-a3ae-3685e4b296e0\" />\n" +
                        "    <PolicyReferenceInstruction type=\"Delete\" referenceType=\"com.l7tech.console.policy.exporter.ExternalSchemaReference\" referenceId=\"syn:e69a8c36-66c6-3f0b-ba67-74749c1c62b5\" />\n" +
                        "    <PolicyReferenceInstruction type=\"Rename\" referenceType=\"com.l7tech.console.policy.exporter.IncludedPolicyReference\" referenceId=\"006ece03-a64c-4c17-93cf-ce49e7265daa\" mappedName=\"Renamed Imported Policy Include Fragment\"/>\n" +
                        "    <PolicyReferenceInstruction type=\"Ignore\" referenceType=\"com.l7tech.console.policy.exporter.TrustedCertReference\"    referenceId=\"129630208\" />\n" +
                        "    <PolicyReferenceInstruction type=\"Ignore\" referenceType=\"com.l7tech.console.policy.exporter.JMSEndpointReference\"    referenceId=\"33\" />\n" +
                        "    <PolicyReferenceInstruction type=\"Ignore\" referenceType=\"com.l7tech.console.policy.exporter.PrivateKeyReference\"     referenceId=\"2:invalid\" />\n" +
                        "</PolicyReferenceInstructions>\n" +
                        "</PolicyImportContext></env:Body></env:Envelope>";

        final Document result = processRequest( "http://ns.l7tech.com/2010/04/gateway-management/policies/ImportPolicy", message );

        final Element soapBody = SoapUtil.getBodyElement(result);
        final Element importResult = XmlUtil.findExactlyOneChildElementByName(soapBody, NS_GATEWAY_MANAGEMENT, "PolicyImportResult");
        final Element importedPolicyRefs = XmlUtil.findExactlyOneChildElementByName(importResult, NS_GATEWAY_MANAGEMENT, "ImportedPolicyReferences");
        final Element importedPolicyRef = XmlUtil.findExactlyOneChildElementByName(importedPolicyRefs, NS_GATEWAY_MANAGEMENT, "ImportedPolicyReference");

        assertEquals("Imported policy ref GUID", "006ece03-a64c-4c17-93cf-ce49e7265daa", importedPolicyRef.getAttribute( "guid" ));
        assertEquals("Imported policy ref type", "Created", importedPolicyRef.getAttribute( "type" ));
    }

    @Test
    public void testPolicyImportExistingPolicyReference() throws Exception {
        final String message =
                "<env:Envelope xmlns:env=\"http://www.w3.org/2003/05/soap-envelope\" xmlns:wsa=\"http://schemas.xmlsoap.org/ws/2004/08/addressing\" xmlns:wsman=\"http://schemas.dmtf.org/wbem/wsman/1/wsman.xsd\" xmlns:wxf=\"http://schemas.xmlsoap.org/ws/2004/09/transfer\" xmlns:xs=\"http://www.w3.org/2001/XMLSchema\"><env:Header><wsa:Action env:mustUnderstand=\"true\">http://ns.l7tech.com/2010/04/gateway-management/policies/ImportPolicy</wsa:Action><wsa:ReplyTo><wsa:Address env:mustUnderstand=\"true\">http://schemas.xmlsoap.org/ws/2004/08/addressing/role/anonymous</wsa:Address></wsa:ReplyTo><wsa:MessageID env:mustUnderstand=\"true\">uuid:d0f59849-9eaa-4027-8b2e-f5ec2dfc1f9d</wsa:MessageID><wsa:To env:mustUnderstand=\"true\">http://localhost:8080/wsman</wsa:To><wsman:ResourceURI>http://ns.l7tech.com/2010/04/gateway-management/policies</wsman:ResourceURI><wsman:OperationTimeout>P0Y0M0DT0H5M0.000S</wsman:OperationTimeout><wsman:SelectorSet><wsman:Selector Name=\"id\">"+new Goid(0,1)+"</wsman:Selector></wsman:SelectorSet></env:Header><env:Body><PolicyImportContext xmlns=\"http://ns.l7tech.com/2010/04/gateway-management\"><Properties/><Resource type=\"policyexport\">&lt;?xml version=\"1.0\" encoding=\"UTF-8\"?&gt;\n" +
                        "&lt;exp:Export Version=\"3.0\"\n" +
                        "    xmlns:L7p=\"http://www.layer7tech.com/ws/policy\"\n" +
                        "    xmlns:exp=\"http://www.layer7tech.com/ws/policy/export\" xmlns:wsp=\"http://schemas.xmlsoap.org/ws/2002/12/policy\"&gt;\n" +
                        "    &lt;exp:References>\n" +
                     "        &lt;IncludedPolicyReference\n" +
                        "            RefType=\"com.l7tech.console.policy.exporter.IncludedPolicyReference\"\n" +
                        "            guid=\"006ece03-a64c-4c17-93cf-ce49e7265daa\" included=\"true\"\n" +
                        "            name=\"Imported Policy Include Fragment\" soap=\"false\" type=\"INCLUDE_FRAGMENT\"&gt;\n" +
                        "            &lt;wsp:Policy xmlns:L7p=\"http://www.layer7tech.com/ws/policy\" xmlns:wsp=\"http://schemas.xmlsoap.org/ws/2002/12/policy\"&gt;\n" +
                        "                &lt;wsp:All wsp:Usage=\"Required\"&gt;\n" +
                        "                    &lt;L7p:AuditDetailAssertion&gt;\n" +
                        "                        &lt;L7p:Detail stringValue=\"Policy Fragment: Imported Policy Include Fragment\"/&gt;\n" +
                        "                        &lt;L7p:Detail stringValue=\"This extra assertion makes the policy conflict\"/&gt;\n" +
                        "                    &lt;/L7p:AuditDetailAssertion&gt;\n" +
                        "                &lt;/wsp:All&gt;\n" +
                        "            &lt;/wsp:Policy&gt;\n" +
                        "        &lt;/IncludedPolicyReference&gt;\n" +
                        "    &lt;/exp:References&gt;\n" +
                        "    &lt;wsp:Policy xmlns:L7p=\"http://www.layer7tech.com/ws/policy\" xmlns:wsp=\"http://schemas.xmlsoap.org/ws/2002/12/policy\"&gt;\n" +
                        "        &lt;wsp:All wsp:Usage=\"Required\"&gt;\n" +
                        "            &lt;L7p:AuditAssertion/&gt;\n" +
                       "            &lt;L7p:Include&gt;\n" +
                        "                &lt;L7p:PolicyGuid stringValue=\"006ece03-a64c-4c17-93cf-ce49e7265daa\"/&gt;\n" +
                        "            &lt;/L7p:Include&gt;\n" +
                          "        &lt;/wsp:All&gt;\n" +
                        "    &lt;/wsp:Policy&gt;\n" +
                        "&lt;/exp:Export&gt;\n" +
                        "</Resource>\n" +
                        "<PolicyReferenceInstructions>\n" +
                        "    <PolicyReferenceInstruction type=\"Rename\" referenceType=\"com.l7tech.console.policy.exporter.IncludedPolicyReference\" referenceId=\"006ece03-a64c-4c17-93cf-ce49e7265daa\" mappedName=\"Renamed Imported Policy Include Fragment\"/>\n" +
                        "</PolicyReferenceInstructions>\n" +
                        "</PolicyImportContext></env:Body></env:Envelope>";

        // insert policy with same name
        PolicyManagerStub policyManager = applicationContext.getBean("policyManager", PolicyManagerStub.class);
        policyManager.save(policy( new Goid(123,2L), PolicyType.INCLUDE_FRAGMENT, "Imported Policy Include Fragment", true, POLICY) );

        final Document result = processRequest( "http://ns.l7tech.com/2010/04/gateway-management/policies/ImportPolicy", message );

        final Element soapBody = SoapUtil.getBodyElement(result);
        final Element importResult = XmlUtil.findExactlyOneChildElementByName(soapBody, NS_GATEWAY_MANAGEMENT, "PolicyImportResult");
        final Element importedPolicyRefs = XmlUtil.findExactlyOneChildElementByName(importResult, NS_GATEWAY_MANAGEMENT, "ImportedPolicyReferences");
        final Element importedPolicyRef = XmlUtil.findExactlyOneChildElementByName(importedPolicyRefs, NS_GATEWAY_MANAGEMENT, "ImportedPolicyReference");

        assertEquals("Imported policy ref GUID", "006ece03-a64c-4c17-93cf-ce49e7265daa", importedPolicyRef.getAttribute( "guid" ));
        assertEquals("Imported policy ref type", "Created", importedPolicyRef.getAttribute( "type" ));
        Policy policy = policyManager.findByGuid("006ece03-a64c-4c17-93cf-ce49e7265daa");
        assertEquals("Imported policy ref name", "Renamed Imported Policy Include Fragment", policy.getName());

    }

    @BugId("SSG-7630")
    @Test
    public void testPolicyImportRenamePolicyReference() throws Exception {
        final String message =
                "<env:Envelope xmlns:env=\"http://www.w3.org/2003/05/soap-envelope\" xmlns:wsa=\"http://schemas.xmlsoap.org/ws/2004/08/addressing\" xmlns:wsman=\"http://schemas.dmtf.org/wbem/wsman/1/wsman.xsd\" xmlns:wxf=\"http://schemas.xmlsoap.org/ws/2004/09/transfer\" xmlns:xs=\"http://www.w3.org/2001/XMLSchema\"><env:Header><wsa:Action env:mustUnderstand=\"true\">http://ns.l7tech.com/2010/04/gateway-management/policies/ImportPolicy</wsa:Action><wsa:ReplyTo><wsa:Address env:mustUnderstand=\"true\">http://schemas.xmlsoap.org/ws/2004/08/addressing/role/anonymous</wsa:Address></wsa:ReplyTo><wsa:MessageID env:mustUnderstand=\"true\">uuid:d0f59849-9eaa-4027-8b2e-f5ec2dfc1f9d</wsa:MessageID><wsa:To env:mustUnderstand=\"true\">http://localhost:8080/wsman</wsa:To><wsman:ResourceURI>http://ns.l7tech.com/2010/04/gateway-management/policies</wsman:ResourceURI><wsman:OperationTimeout>P0Y0M0DT0H5M0.000S</wsman:OperationTimeout><wsman:SelectorSet><wsman:Selector Name=\"id\">"+new Goid(0,1)+"</wsman:Selector></wsman:SelectorSet></env:Header><env:Body><PolicyImportContext xmlns=\"http://ns.l7tech.com/2010/04/gateway-management\"><Properties/><Resource type=\"policyexport\">&lt;?xml version=\"1.0\" encoding=\"UTF-8\"?&gt;\n" +
                        "&lt;exp:Export Version=\"3.0\"\n" +
                        "    xmlns:L7p=\"http://www.layer7tech.com/ws/policy\"\n" +
                        "    xmlns:exp=\"http://www.layer7tech.com/ws/policy/export\" xmlns:wsp=\"http://schemas.xmlsoap.org/ws/2002/12/policy\"&gt;\n" +
                        "    &lt;exp:References>\n" +
                        "        &lt;IncludedPolicyReference\n" +
                        "            RefType=\"com.l7tech.console.policy.exporter.IncludedPolicyReference\"\n" +
                        "            guid=\"006ece03-a64c-4c17-93cf-ce49e7265daa\" included=\"true\"\n" +
                        "            name=\"Imported Policy Include Fragment\" soap=\"false\" type=\"INCLUDE_FRAGMENT\"&gt;\n" +
                        "            &lt;wsp:Policy xmlns:L7p=\"http://www.layer7tech.com/ws/policy\" xmlns:wsp=\"http://schemas.xmlsoap.org/ws/2002/12/policy\"&gt;\n" +
                        "                &lt;wsp:All wsp:Usage=\"Required\"&gt;\n" +
                        "                    &lt;L7p:AuditDetailAssertion&gt;\n" +
                        "                        &lt;L7p:Detail stringValue=\"Policy Fragment: Imported Policy Include Fragment\"/&gt;\n" +
                        "                        &lt;L7p:Detail stringValue=\"This extra assertion makes the policy conflict\"/&gt;\n" +
                        "                    &lt;/L7p:AuditDetailAssertion&gt;\n" +
                        "                &lt;/wsp:All&gt;\n" +
                        "            &lt;/wsp:Policy&gt;\n" +
                        "        &lt;/IncludedPolicyReference&gt;\n" +
                        "    &lt;/exp:References&gt;\n" +
                        "    &lt;wsp:Policy xmlns:L7p=\"http://www.layer7tech.com/ws/policy\" xmlns:wsp=\"http://schemas.xmlsoap.org/ws/2002/12/policy\"&gt;\n" +
                        "        &lt;wsp:All wsp:Usage=\"Required\"&gt;\n" +
                        "            &lt;L7p:AuditAssertion/&gt;\n" +
                        "            &lt;L7p:Include&gt;\n" +
                        "                &lt;L7p:PolicyGuid stringValue=\"006ece03-a64c-4c17-93cf-ce49e7265daa\"/&gt;\n" +
                        "            &lt;/L7p:Include&gt;\n" +
                        "        &lt;/wsp:All&gt;\n" +
                        "    &lt;/wsp:Policy&gt;\n" +
                        "&lt;/exp:Export&gt;\n" +
                        "</Resource>\n" +
                        "<PolicyReferenceInstructions>\n" +
                        "    <PolicyReferenceInstruction type=\"Rename\" referenceType=\"com.l7tech.console.policy.exporter.IncludedPolicyReference\" referenceId=\"006ece03-a64c-4c17-93cf-ce49e7265daa\" mappedName=\"Renamed Imported Policy Include Fragment\"/>\n" +
                        "</PolicyReferenceInstructions>\n" +
                        "</PolicyImportContext></env:Body></env:Envelope>";

        // insert policy with same name
        PolicyManagerStub policyManager = applicationContext.getBean("policyManager", PolicyManagerStub.class);

        final Document result = processRequest( "http://ns.l7tech.com/2010/04/gateway-management/policies/ImportPolicy", message );

        final Element soapBody = SoapUtil.getBodyElement(result);
        final Element importResult = XmlUtil.findExactlyOneChildElementByName(soapBody, NS_GATEWAY_MANAGEMENT, "PolicyImportResult");
        final Element importedPolicyRefs = XmlUtil.findExactlyOneChildElementByName(importResult, NS_GATEWAY_MANAGEMENT, "ImportedPolicyReferences");
        final Element importedPolicyRef = XmlUtil.findExactlyOneChildElementByName(importedPolicyRefs, NS_GATEWAY_MANAGEMENT, "ImportedPolicyReference");

        assertEquals("Imported policy ref GUID", "006ece03-a64c-4c17-93cf-ce49e7265daa", importedPolicyRef.getAttribute( "guid" ));
        assertEquals("Imported policy ref type", "Created", importedPolicyRef.getAttribute( "type" ));
        Policy policy = policyManager.findByGuid("006ece03-a64c-4c17-93cf-ce49e7265daa");
        assertEquals("Imported policy ref name", "Renamed Imported Policy Include Fragment", policy.getName());

    }

    @Test
    public void testPolicyValidate() throws Exception {
        final String message =
                "<env:Envelope xmlns:env=\"http://www.w3.org/2003/05/soap-envelope\" xmlns:wsa=\"http://schemas.xmlsoap.org/ws/2004/08/addressing\" xmlns:wsman=\"http://schemas.dmtf.org/wbem/wsman/1/wsman.xsd\" xmlns:wxf=\"http://schemas.xmlsoap.org/ws/2004/09/transfer\"><env:Header><wsa:Action env:mustUnderstand=\"true\">http://ns.l7tech.com/2010/04/gateway-management/policies/ValidatePolicy</wsa:Action><wsa:ReplyTo><wsa:Address env:mustUnderstand=\"true\">http://schemas.xmlsoap.org/ws/2004/08/addressing/role/anonymous</wsa:Address></wsa:ReplyTo><wsa:MessageID env:mustUnderstand=\"true\">uuid:61dcea28-f545-4b0f-9436-0e8b59f4370d</wsa:MessageID><wsa:To env:mustUnderstand=\"true\">http://localhost:8080/wsman</wsa:To><wsman:ResourceURI>http://ns.l7tech.com/2010/04/gateway-management/policies</wsman:ResourceURI><wsman:OperationTimeout>P0Y0M0DT0H5M0.000S</wsman:OperationTimeout><wsman:SelectorSet><wsman:Selector Name=\"id\">"+new Goid(0,1)+"</wsman:Selector></wsman:SelectorSet></env:Header><env:Body/></env:Envelope>";

        {
            policyValidator.setErrors( Collections.<PolicyValidatorResult.Error>emptyList() );
            policyValidator.setWarnings( Collections.<PolicyValidatorResult.Warning>emptyList() );

            final Document result = processRequest( "http://ns.l7tech.com/2010/04/gateway-management/policies/ValidatePolicy", message );

            final Element soapBody = SoapUtil.getBodyElement(result);
            final Element validationResult = XmlUtil.findExactlyOneChildElementByName(soapBody, NS_GATEWAY_MANAGEMENT, "PolicyValidationResult");
            final Element validationStatus = XmlUtil.findExactlyOneChildElementByName(validationResult, NS_GATEWAY_MANAGEMENT, "ValidationStatus");
            final Element validationMessages = XmlUtil.findFirstChildElementByName(validationResult, NS_GATEWAY_MANAGEMENT, "ValidationMessages");

            assertEquals("Validation status", "OK", XmlUtil.getTextValue(validationStatus));
            assertNull("Validation messages",validationMessages);
        }
        {
            policyValidator.setErrors( Collections.<PolicyValidatorResult.Error>emptyList() );
            policyValidator.setWarnings( Arrays.asList(
                    new PolicyValidatorResult.Warning( Arrays.asList( 0 ), 1, "Test warning message", null )
            ) );

            final Document result = processRequest( "http://ns.l7tech.com/2010/04/gateway-management/policies/ValidatePolicy", message );

            final Element soapBody = SoapUtil.getBodyElement(result);
            final Element validationResult = XmlUtil.findExactlyOneChildElementByName(soapBody, NS_GATEWAY_MANAGEMENT, "PolicyValidationResult");
            final Element validationStatus = XmlUtil.findExactlyOneChildElementByName(validationResult, NS_GATEWAY_MANAGEMENT, "ValidationStatus");
            final Element validationMessages = XmlUtil.findExactlyOneChildElementByName(validationResult, NS_GATEWAY_MANAGEMENT, "ValidationMessages");
            final Element validationMessage = XmlUtil.findExactlyOneChildElementByName(validationMessages, NS_GATEWAY_MANAGEMENT, "ValidationMessage");
            final Element validationWarning = XmlUtil.findExactlyOneChildElementByName(validationMessage, NS_GATEWAY_MANAGEMENT, "Message");

            assertEquals("Validation status", "Warning", XmlUtil.getTextValue(validationStatus));
            assertEquals("Validation warning message", "Test warning message", XmlUtil.getTextValue(validationWarning));
        }
        {
            policyValidator.setErrors( Arrays.asList(
                    new PolicyValidatorResult.Error( Arrays.asList( 0 ), 1, "Test error message", null )
            ) );
            policyValidator.setWarnings( Arrays.asList(
                    new PolicyValidatorResult.Warning( Arrays.asList( 0 ), 1, "Test warning message 1", null ),
                    new PolicyValidatorResult.Warning( Arrays.asList( 0 ), 1, "Test warning message 2", null )
            ) );

            final Document result = processRequest( "http://ns.l7tech.com/2010/04/gateway-management/policies/ValidatePolicy", message );

            final Element soapBody = SoapUtil.getBodyElement(result);
            final Element validationResult = XmlUtil.findExactlyOneChildElementByName(soapBody, NS_GATEWAY_MANAGEMENT, "PolicyValidationResult");
            final Element validationStatus = XmlUtil.findExactlyOneChildElementByName(validationResult, NS_GATEWAY_MANAGEMENT, "ValidationStatus");
            final Element validationMessages = XmlUtil.findExactlyOneChildElementByName(validationResult, NS_GATEWAY_MANAGEMENT, "ValidationMessages");
            final List<Element> validationMessageElements = XmlUtil.findChildElementsByName( validationMessages, NS_GATEWAY_MANAGEMENT, "ValidationMessage" );

            assertEquals("Validation status", "Error", XmlUtil.getTextValue(validationStatus));
            assertEquals("Validation messages size", 3L, (long) validationMessageElements.size() );
        }
    }

    @BugNumber(11005)
    @Test
    public void testMoveFolder() throws Exception {
        // move from "Test Folder" to the root
        String message = "<s:Envelope xmlns:s=\"http://www.w3.org/2003/05/soap-envelope\" xmlns:wsa=\"http://schemas.xmlsoap.org/ws/2004/08/addressing\" xmlns:wsman=\"http://schemas.dmtf.org/wbem/wsman/1/wsman.xsd\" xmlns:n1=\"http://ns.l7tech.com/2010/04/gateway-management\"><s:Header><wsa:Action s:mustUnderstand=\"true\">http://schemas.xmlsoap.org/ws/2004/09/transfer/Put</wsa:Action><wsa:To s:mustUnderstand=\"true\">http://127.0.0.1:8080/wsman</wsa:To><wsman:ResourceURI s:mustUnderstand=\"true\">http://ns.l7tech.com/2010/04/gateway-management/folders</wsman:ResourceURI><wsa:MessageID s:mustUnderstand=\"true\">uuid:afad2993-7d39-1d39-8002-481688002100</wsa:MessageID><wsa:ReplyTo><wsa:Address>http://schemas.xmlsoap.org/ws/2004/08/addressing/role/anonymous</wsa:Address></wsa:ReplyTo><wsman:SelectorSet><wsman:Selector Name=\"id\">"+new Goid(0,2)+"</wsman:Selector></wsman:SelectorSet><wsman:RequestEPR/></s:Header><s:Body> <n1:Folder xmlns:n1=\"http://ns.l7tech.com/2010/04/gateway-management\" folderId=\""+new Goid(0,-5002)+"\" id=\""+new Goid(0,2)+"\" version=\"0\"><n1:Name>Nested Test Folder</n1:Name></n1:Folder> </s:Body></s:Envelope>";

        final Document result = processRequest( "http://schemas.xmlsoap.org/ws/2004/09/transfer/Put", message );

        final Element soapBody = SoapUtil.getBodyElement(result);
        final Element folder = XmlUtil.findExactlyOneChildElementByName(soapBody, NS_GATEWAY_MANAGEMENT, "Folder");
        final Element name = XmlUtil.findExactlyOneChildElementByName(folder, NS_GATEWAY_MANAGEMENT, "Name");

        assertEquals("FolderId", new Goid(0,-5002).toHexString(), folder.getAttributeNS( null, "folderId" ));
        assertEquals("Name", "Nested Test Folder", XmlUtil.getTextValue(name));
    }

    @Test
    public void testMovePolicy() throws Exception {
        // move policy from the root to "Test Folder"
        String message = "<s:Envelope xmlns:s=\"http://www.w3.org/2003/05/soap-envelope\" xmlns:wsa=\"http://schemas.xmlsoap.org/ws/2004/08/addressing\" xmlns:wsman=\"http://schemas.dmtf.org/wbem/wsman/1/wsman.xsd\" xmlns:l7=\"http://ns.l7tech.com/2010/04/gateway-management\"><s:Header><wsa:Action s:mustUnderstand=\"true\">http://schemas.xmlsoap.org/ws/2004/09/transfer/Put</wsa:Action><wsa:To s:mustUnderstand=\"true\">http://127.0.0.1:8080/wsman</wsa:To><wsman:ResourceURI s:mustUnderstand=\"true\">http://ns.l7tech.com/2010/04/gateway-management/policies</wsman:ResourceURI><wsa:MessageID s:mustUnderstand=\"true\">uuid:afad2993-7d39-1d39-8002-481688002100</wsa:MessageID><wsa:ReplyTo><wsa:Address>http://schemas.xmlsoap.org/ws/2004/08/addressing/role/anonymous</wsa:Address></wsa:ReplyTo><wsman:SelectorSet><wsman:Selector Name=\"id\">"+new Goid(0,2)+"</wsman:Selector></wsman:SelectorSet><wsman:RequestEPR/></s:Header><s:Body>" +
                "<l7:Policy guid=\"c81e728d-9d4c-3f63-af06-7f89cc14862c\"\n" +
                "    id=\""+new Goid(0,2)+"\" version=\"0\">\n" +
                "    <l7:PolicyDetail\n" +
                "        guid=\"c81e728d-9d4c-3f63-af06-7f89cc14862c\"\n" +
                "        id=\""+new Goid(0,2)+"\" folderId=\""+new Goid(0,1)+"\" version=\"0\">\n" +
                "        <l7:Name>Test Policy For Move</l7:Name>\n" +
                "        <l7:PolicyType>Include</l7:PolicyType>\n" +
                "        <l7:Properties>\n" +
                "            <l7:Property key=\"revision\">\n" +
                "                <l7:LongValue>0</l7:LongValue>\n" +
                "            </l7:Property>\n" +
                "            <l7:Property key=\"soap\">\n" +
                "                <l7:BooleanValue>true</l7:BooleanValue>\n" +
                "            </l7:Property>\n" +
                "        </l7:Properties>\n" +
                "    </l7:PolicyDetail>\n" +
                "    <l7:Resources>\n" +
                "        <l7:ResourceSet tag=\"policy\">\n" +
                "            <l7:Resource type=\"policy\">&lt;wsp:Policy xmlns:L7p=\"http://www.layer7tech.com/ws/policy\" xmlns:wsp=\"http://schemas.xmlsoap.org/ws/2002/12/policy\"&gt;&lt;wsp:All wsp:Usage=\"Required\"&gt;&lt;L7p:AuditAssertion/&gt;&lt;/wsp:All&gt;&lt;/wsp:Policy&gt;</l7:Resource>\n" +
                "        </l7:ResourceSet>\n" +
                "    </l7:Resources>\n" +
                "</l7:Policy>\n" +
                "</s:Body></s:Envelope>";

        final Document result = processRequest( "http://schemas.xmlsoap.org/ws/2004/09/transfer/Put", message );

        final Element soapBody = SoapUtil.getBodyElement(result);
        final Element policy = XmlUtil.findExactlyOneChildElementByName(soapBody, NS_GATEWAY_MANAGEMENT, "Policy");
        final Element policyDetail = XmlUtil.findExactlyOneChildElementByName(policy, NS_GATEWAY_MANAGEMENT, "PolicyDetail");
        final Element policyDetailName = XmlUtil.findExactlyOneChildElementByName(policyDetail, NS_GATEWAY_MANAGEMENT, "Name");

        assertEquals("Policy id", new Goid(0,2).toHexString(), policy.getAttribute( "id" ));
        //assertEquals("Policy version", "0", policy.getAttribute( "version" ));
        assertEquals("Policy detail id", new Goid(0,2).toHexString(), policyDetail.getAttribute( "id" ));
        //assertEquals("Policy detail version", "0", policyDetail.getAttribute( "version" ));
        assertEquals("Policy detail name", "Test Policy For Move", XmlUtil.getTextValue(policyDetailName));
        assertEquals("Policy detail folder", new Goid(0,1).toHexString(), policyDetail.getAttributeNS( null, "folderId" ));
    }

    @Test
    public void testMoveService() throws Exception {
        String message = "<s:Envelope xmlns:s=\"http://www.w3.org/2003/05/soap-envelope\" xmlns:wsa=\"http://schemas.xmlsoap.org/ws/2004/08/addressing\" xmlns:wsman=\"http://schemas.dmtf.org/wbem/wsman/1/wsman.xsd\" xmlns:l7=\"http://ns.l7tech.com/2010/04/gateway-management\"><s:Header><wsa:Action s:mustUnderstand=\"true\">http://schemas.xmlsoap.org/ws/2004/09/transfer/Put</wsa:Action><wsa:To s:mustUnderstand=\"true\">http://127.0.0.1:8080/wsman</wsa:To><wsman:ResourceURI s:mustUnderstand=\"true\">http://ns.l7tech.com/2010/04/gateway-management/services</wsman:ResourceURI><wsa:MessageID s:mustUnderstand=\"true\">uuid:afad2993-7d39-1d39-8002-481688002100</wsa:MessageID><wsa:ReplyTo><wsa:Address>http://schemas.xmlsoap.org/ws/2004/08/addressing/role/anonymous</wsa:Address></wsa:ReplyTo><wsman:SelectorSet><wsman:Selector Name=\"id\">"+new Goid(0,1)+"</wsman:Selector></wsman:SelectorSet><wsman:RequestEPR/></s:Header><s:Body>" +
                "<l7:Service id=\""+new Goid(0,1)+"\" version=\"1\">\n" +
                "     <l7:ServiceDetail id=\""+new Goid(0,1)+"\" folderId=\""+new Goid(0,1)+"\" version=\"1\">\n" +
                "         <l7:Name>Test Service 1</l7:Name>\n" +
                "         <l7:Enabled>true</l7:Enabled>\n" +
                "         <l7:ServiceMappings>\n" +
                "             <l7:HttpMapping>\n" +
                "                 <l7:Verbs>\n" +
                "                     <l7:Verb>POST</l7:Verb>\n" +
                "                 </l7:Verbs>\n" +
                "             </l7:HttpMapping>\n" +
                "         </l7:ServiceMappings>\n" +
                "         <l7:Properties>\n" +
                "             <l7:Property key=\"policyRevision\">\n" +
                "                 <l7:LongValue>0</l7:LongValue>\n" +
                "             </l7:Property>\n" +
                "             <l7:Property key=\"wssProcessingEnabled\">\n" +
                "                 <l7:BooleanValue>true</l7:BooleanValue>\n" +
                "             </l7:Property>\n" +
                "             <l7:Property key=\"soap\">\n" +
                "                 <l7:BooleanValue>false</l7:BooleanValue>\n" +
                "             </l7:Property>\n" +
                "             <l7:Property key=\"internal\">\n" +
                "                 <l7:BooleanValue>false</l7:BooleanValue>\n" +
                "             </l7:Property>\n" +
                "         </l7:Properties>\n" +
                "     </l7:ServiceDetail>\n" +
                "     <l7:Resources>\n" +
                "         <l7:ResourceSet tag=\"policy\">\n" +
                "             <l7:Resource type=\"policy\" version=\"0\">&lt;wsp:Policy xmlns:L7p=\"http://www.layer7tech.com/ws/policy\" xmlns:wsp=\"http://schemas.xmlsoap.org/ws/2002/12/policy\"&gt;&lt;wsp:All wsp:Usage=\"Required\"&gt;&lt;L7p:AuditAssertion/&gt;&lt;/wsp:All&gt;&lt;/wsp:Policy&gt;</l7:Resource>\n" +
                "         </l7:ResourceSet>\n" +
                "     </l7:Resources>\n" +
                " </l7:Service>" +
                "</s:Body></s:Envelope>";

        final Document result = processRequest( "http://schemas.xmlsoap.org/ws/2004/09/transfer/Put", message );

        final Element soapBody = SoapUtil.getBodyElement(result);
        final Element service = XmlUtil.findExactlyOneChildElementByName(soapBody, NS_GATEWAY_MANAGEMENT, "Service");
        final Element serviceDetail = XmlUtil.findExactlyOneChildElementByName(service, NS_GATEWAY_MANAGEMENT, "ServiceDetail");
        final Element serviceDetailName = XmlUtil.findExactlyOneChildElementByName(serviceDetail, NS_GATEWAY_MANAGEMENT, "Name");

        assertEquals("Service id", new Goid(0,1).toHexString(), service.getAttribute( "id" ));
        //assertEquals("Service version", "1", service.getAttribute( "version" ));
        assertEquals("Service detail id", new Goid(0,1).toHexString(), serviceDetail.getAttribute( "id" ));
        //assertEquals("Service detail version", "1", serviceDetail.getAttribute( "version" ));
        assertEquals("Service detail name", "Test Service 1", XmlUtil.getTextValue(serviceDetailName));
        assertEquals("Service detail folder", new Goid(0,1).toHexString(), serviceDetail.getAttributeNS( null, "folderId" ));
    }

    @Test
    public void testServicePolicyExport() throws Exception {
        final String message =
                "<env:Envelope xmlns:env=\"http://www.w3.org/2003/05/soap-envelope\" xmlns:wsa=\"http://schemas.xmlsoap.org/ws/2004/08/addressing\" xmlns:wsman=\"http://schemas.dmtf.org/wbem/wsman/1/wsman.xsd\" xmlns:wxf=\"http://schemas.xmlsoap.org/ws/2004/09/transfer\" xmlns:xs=\"http://www.w3.org/2001/XMLSchema\"><env:Header><wsa:Action env:mustUnderstand=\"true\">http://ns.l7tech.com/2010/04/gateway-management/services/ExportPolicy</wsa:Action><wsa:ReplyTo><wsa:Address env:mustUnderstand=\"true\">http://schemas.xmlsoap.org/ws/2004/08/addressing/role/anonymous</wsa:Address></wsa:ReplyTo><wsa:MessageID env:mustUnderstand=\"true\">uuid:d521f4b4-ef2a-4381-b11c-fa72eb760a99</wsa:MessageID><wsa:To env:mustUnderstand=\"true\">http://localhost:8080/wsman</wsa:To><wsman:ResourceURI>http://ns.l7tech.com/2010/04/gateway-management/services</wsman:ResourceURI><wsman:OperationTimeout>P0Y0M0DT0H5M0.000S</wsman:OperationTimeout><wsman:SelectorSet><wsman:Selector Name=\"id\">"+new Goid(0,1)+"</wsman:Selector></wsman:SelectorSet></env:Header><env:Body/></env:Envelope>";

        final Document result = processRequest( "http://ns.l7tech.com/2010/04/gateway-management/services/ExportPolicy", message );

        final Element soapBody = SoapUtil.getBodyElement(result);
        final Element policyExportResult = XmlUtil.findExactlyOneChildElementByName(soapBody, NS_GATEWAY_MANAGEMENT, "PolicyExportResult");
        final Element resource = XmlUtil.findExactlyOneChildElementByName(policyExportResult, NS_GATEWAY_MANAGEMENT, "Resource");

        assertEquals( "resource type", "policyexport", resource.getAttribute("type"));
        assertTrue( "resource content is export", XmlUtil.getTextValue(resource).contains( "<exp:Export " ));
    }

    @Test
    public void testServicePolicyImport() throws Exception {
        final String message =
                "<env:Envelope xmlns:env=\"http://www.w3.org/2003/05/soap-envelope\" xmlns:wsa=\"http://schemas.xmlsoap.org/ws/2004/08/addressing\" xmlns:wsman=\"http://schemas.dmtf.org/wbem/wsman/1/wsman.xsd\" xmlns:wxf=\"http://schemas.xmlsoap.org/ws/2004/09/transfer\" xmlns:xs=\"http://www.w3.org/2001/XMLSchema\"><env:Header><wsa:Action env:mustUnderstand=\"true\">http://ns.l7tech.com/2010/04/gateway-management/services/ImportPolicy</wsa:Action><wsa:ReplyTo><wsa:Address env:mustUnderstand=\"true\">http://schemas.xmlsoap.org/ws/2004/08/addressing/role/anonymous</wsa:Address></wsa:ReplyTo><wsa:MessageID env:mustUnderstand=\"true\">uuid:d0f59849-9eaa-4027-8b2e-f5ec2dfc1f9d</wsa:MessageID><wsa:To env:mustUnderstand=\"true\">http://localhost:8080/wsman</wsa:To><wsman:ResourceURI>http://ns.l7tech.com/2010/04/gateway-management/services</wsman:ResourceURI><wsman:OperationTimeout>P0Y0M0DT0H5M0.000S</wsman:OperationTimeout><wsman:SelectorSet><wsman:Selector Name=\"id\">"+new Goid(0,1)+"</wsman:Selector></wsman:SelectorSet></env:Header><env:Body><PolicyImportContext xmlns=\"http://ns.l7tech.com/2010/04/gateway-management\"><Properties/><Resource type=\"policyexport\">&lt;?xml version=\"1.0\" encoding=\"UTF-8\"?&gt;\n" +
                        "&lt;exp:Export Version=\"3.0\"\n" +
                        "    xmlns:L7p=\"http://www.layer7tech.com/ws/policy\"\n" +
                        "    xmlns:exp=\"http://www.layer7tech.com/ws/policy/export\" xmlns:wsp=\"http://schemas.xmlsoap.org/ws/2002/12/policy\"&gt;\n" +
                        "    &lt;exp:References/&gt;\n" +
                        "    &lt;wsp:Policy xmlns:L7p=\"http://www.layer7tech.com/ws/policy\" xmlns:wsp=\"http://schemas.xmlsoap.org/ws/2002/12/policy\"&gt;\n" +
                        "        &lt;L7p:FalseAssertion/&gt;\n" +
                        "    &lt;/wsp:Policy&gt;\n" +
                        "&lt;/exp:Export&gt;\n" +
                        "</Resource></PolicyImportContext></env:Body></env:Envelope>";

        final Document result = processRequest( "http://ns.l7tech.com/2010/04/gateway-management/services/ImportPolicy", message );

        final Element soapBody = SoapUtil.getBodyElement(result);
        XmlUtil.findExactlyOneChildElementByName(soapBody, NS_GATEWAY_MANAGEMENT, "PolicyImportResult");
    }

    @Test
    public void testServicePolicyValidate() throws Exception {
        final String message =
                "<env:Envelope xmlns:env=\"http://www.w3.org/2003/05/soap-envelope\" xmlns:wsa=\"http://schemas.xmlsoap.org/ws/2004/08/addressing\" xmlns:wsman=\"http://schemas.dmtf.org/wbem/wsman/1/wsman.xsd\" xmlns:wxf=\"http://schemas.xmlsoap.org/ws/2004/09/transfer\"><env:Header><wsa:Action env:mustUnderstand=\"true\">http://ns.l7tech.com/2010/04/gateway-management/services/ValidatePolicy</wsa:Action><wsa:ReplyTo><wsa:Address env:mustUnderstand=\"true\">http://schemas.xmlsoap.org/ws/2004/08/addressing/role/anonymous</wsa:Address></wsa:ReplyTo><wsa:MessageID env:mustUnderstand=\"true\">uuid:61dcea28-f545-4b0f-9436-0e8b59f4370d</wsa:MessageID><wsa:To env:mustUnderstand=\"true\">http://localhost:8080/wsman</wsa:To><wsman:ResourceURI>http://ns.l7tech.com/2010/04/gateway-management/services</wsman:ResourceURI><wsman:OperationTimeout>P0Y0M0DT0H5M0.000S</wsman:OperationTimeout><wsman:SelectorSet><wsman:Selector Name=\"id\">"+new Goid(0,1)+"</wsman:Selector></wsman:SelectorSet></env:Header><env:Body/></env:Envelope>";

        policyValidator.setErrors( Collections.<PolicyValidatorResult.Error>emptyList() );
        policyValidator.setWarnings( Collections.<PolicyValidatorResult.Warning>emptyList() );

        final Document result = processRequest( "http://ns.l7tech.com/2010/04/gateway-management/services/ValidatePolicy", message );

        final Element soapBody = SoapUtil.getBodyElement(result);
        final Element validationResult = XmlUtil.findExactlyOneChildElementByName(soapBody, NS_GATEWAY_MANAGEMENT, "PolicyValidationResult");
        final Element validationStatus = XmlUtil.findExactlyOneChildElementByName(validationResult, NS_GATEWAY_MANAGEMENT, "ValidationStatus");
        final Element validationMessages = XmlUtil.findFirstChildElementByName(validationResult, NS_GATEWAY_MANAGEMENT, "ValidationMessages");

        assertEquals("Validation status", "OK", XmlUtil.getTextValue(validationStatus));
        assertNull("Validation messages",validationMessages);
    }

    @Test
    public void testMissingHeader() throws Exception {
        final String message =
                "<s:Envelope xmlns:s=\"http://www.w3.org/2003/05/soap-envelope\">\n" +
                "  <s:Body/> \n" +
                "</s:Envelope>";

        final Document result = processRequest( "http://schemas.xmlsoap.org/ws/2004/09/transfer/Get", message );

        final Element soapBody = SoapUtil.getBodyElement(result);
        final Element soapFault = XmlUtil.findExactlyOneChildElementByName(soapBody, SOAPConstants.URI_NS_SOAP_1_2_ENVELOPE, "Fault");
        final Element code = XmlUtil.findExactlyOneChildElementByName(soapFault, SOAPConstants.URI_NS_SOAP_1_2_ENVELOPE, "Code");
        final Element subcode = XmlUtil.findExactlyOneChildElementByName(code, SOAPConstants.URI_NS_SOAP_1_2_ENVELOPE, "Subcode");
        final Element value = XmlUtil.findExactlyOneChildElementByName(subcode, SOAPConstants.URI_NS_SOAP_1_2_ENVELOPE, "Value");

        assertEquals("SOAP Fault value", "wsa:MessageInformationHeaderRequired", XmlUtil.getTextValue(value));
    }

    @Test
    public void testCustomCreate() throws Exception {
        String message =
                "<s:Envelope xmlns:s=\"http://www.w3.org/2003/05/soap-envelope\" xmlns:wsa=\"http://schemas.xmlsoap.org/ws/2004/08/addressing\" xmlns:wsman=\"http://schemas.dmtf.org/wbem/wsman/1/wsman.xsd\" xmlns:n1=\"http://ns.l7tech.com/2010/04/gateway-management\">" +
                "<s:Header>" +
                "<wsa:Action s:mustUnderstand=\"true\">http://ns.l7tech.com/2010/04/gateway-management/testResources/Create</wsa:Action>" +
                "<wsa:To s:mustUnderstand=\"true\">http://127.0.0.1:8080/wsman</wsa:To>" +
                "<wsa:MessageID s:mustUnderstand=\"true\">uuid:a711f948-7d39-1d39-8002-481688002100</wsa:MessageID>" +
                "<wsa:ReplyTo><wsa:Address>http://schemas.xmlsoap.org/ws/2004/08/addressing/role/anonymous</wsa:Address></wsa:ReplyTo>" +
                "<wsman:ResourceURI s:mustUnderstand=\"true\">http://ns.l7tech.com/2010/04/gateway-management/testResources</wsman:ResourceURI>" +
                "</s:Header>" +
                "<s:Body></s:Body>" +
                "</s:Envelope>";

        final Document result = processRequest( "http://ns.l7tech.com/2010/04/gateway-management/testResources/Create", message );

        final Element soapBody = SoapUtil.getBodyElement(result);
        final Element resource = XmlUtil.findExactlyOneChildElementByName( soapBody, null, "TestResource" );

        assertEquals("Resource text", "Test resource text", XmlUtil.getTextValue(resource));
    }

    @Test
    public void testCustomCreate2() throws Exception {
        String message =
                "<s:Envelope xmlns:s=\"http://www.w3.org/2003/05/soap-envelope\" xmlns:wsa=\"http://schemas.xmlsoap.org/ws/2004/08/addressing\" xmlns:wsman=\"http://schemas.dmtf.org/wbem/wsman/1/wsman.xsd\" xmlns:n1=\"http://ns.l7tech.com/2010/04/gateway-management\">" +
                "<s:Header>" +
                "<wsa:Action s:mustUnderstand=\"true\">http://ns.l7tech.com/2010/04/gateway-management/testResources/Create2</wsa:Action>" +
                "<wsa:To s:mustUnderstand=\"true\">http://127.0.0.1:8080/wsman</wsa:To>" +
                "<wsa:MessageID s:mustUnderstand=\"true\">uuid:a711f948-7d39-1d39-8002-481688002100</wsa:MessageID>" +
                "<wsa:ReplyTo><wsa:Address>http://schemas.xmlsoap.org/ws/2004/08/addressing/role/anonymous</wsa:Address></wsa:ReplyTo>" +
                "<wsman:ResourceURI s:mustUnderstand=\"true\">http://ns.l7tech.com/2010/04/gateway-management/testResources</wsman:ResourceURI>" +
                "</s:Header>" +
                "<s:Body><n1:ClusterProperty><n1:Name>a</n1:Name><n1:Value>Test resource text2</n1:Value></n1:ClusterProperty></s:Body>" +
                "</s:Envelope>";

        final Document result = processRequest( "http://ns.l7tech.com/2010/04/gateway-management/testResources/Create2", message );

        final Element soapBody = SoapUtil.getBodyElement(result);
        final Element resource = XmlUtil.findExactlyOneChildElementByName( soapBody, NS_GATEWAY_MANAGEMENT, "ClusterProperty" );
        final Element value = XmlUtil.findExactlyOneChildElementByName( resource, NS_GATEWAY_MANAGEMENT, "Value" );

        assertEquals("Resource text", "Test resource text2", XmlUtil.getTextValue(value));
    }

    @Test
    public void testCustomGet() throws Exception {
        String message =
                "<s:Envelope xmlns:s=\"http://www.w3.org/2003/05/soap-envelope\" xmlns:wsa=\"http://schemas.xmlsoap.org/ws/2004/08/addressing\" xmlns:wsman=\"http://schemas.dmtf.org/wbem/wsman/1/wsman.xsd\" xmlns:n1=\"http://ns.l7tech.com/2010/04/gateway-management\">" +
                "<s:Header>" +
                "<wsa:Action s:mustUnderstand=\"true\">http://ns.l7tech.com/2010/04/gateway-management/testResources/Get</wsa:Action>" +
                "<wsa:To s:mustUnderstand=\"true\">http://127.0.0.1:8080/wsman</wsa:To>" +
                "<wsa:MessageID s:mustUnderstand=\"true\">uuid:a711f948-7d39-1d39-8002-481688002100</wsa:MessageID>" +
                "<wsa:ReplyTo><wsa:Address>http://schemas.xmlsoap.org/ws/2004/08/addressing/role/anonymous</wsa:Address></wsa:ReplyTo>" +
                "<wsman:ResourceURI s:mustUnderstand=\"true\">http://ns.l7tech.com/2010/04/gateway-management/testResources</wsman:ResourceURI>" +
                "<wsman:SelectorSet>" +
                "<wsman:Selector Name=\"id\">1</wsman:Selector>" +
                "</wsman:SelectorSet>" +
                "</s:Header>" +
                "<s:Body></s:Body>" +
                "</s:Envelope>";

        final Document result = processRequest( "http://ns.l7tech.com/2010/04/gateway-management/testResources/Get", message );

        final Element soapBody = SoapUtil.getBodyElement(result);
        final Element resource = XmlUtil.findExactlyOneChildElementByName( soapBody, null, "TestResource" );

        assertEquals("Resource text", "Test resource text", XmlUtil.getTextValue(resource));
    }

    @Test
    public void testCustomPut() throws Exception {
        String message =
                "<s:Envelope xmlns:s=\"http://www.w3.org/2003/05/soap-envelope\" xmlns:wsa=\"http://schemas.xmlsoap.org/ws/2004/08/addressing\" xmlns:wsman=\"http://schemas.dmtf.org/wbem/wsman/1/wsman.xsd\" xmlns:n1=\"http://ns.l7tech.com/2010/04/gateway-management\">" +
                "<s:Header>" +
                "<wsa:Action s:mustUnderstand=\"true\">http://ns.l7tech.com/2010/04/gateway-management/testResources/Put</wsa:Action>" +
                "<wsa:To s:mustUnderstand=\"true\">http://127.0.0.1:8080/wsman</wsa:To>" +
                "<wsa:MessageID s:mustUnderstand=\"true\">uuid:a711f948-7d39-1d39-8002-481688002100</wsa:MessageID>" +
                "<wsa:ReplyTo><wsa:Address>http://schemas.xmlsoap.org/ws/2004/08/addressing/role/anonymous</wsa:Address></wsa:ReplyTo>" +
                "<wsman:ResourceURI s:mustUnderstand=\"true\">http://ns.l7tech.com/2010/04/gateway-management/testResources</wsman:ResourceURI>" +
                "<wsman:SelectorSet>" +
                "<wsman:Selector Name=\"id\">1</wsman:Selector>" +
                "</wsman:SelectorSet>" +
                "</s:Header>" +
                "<s:Body><n1:ClusterProperty><n1:Name>a</n1:Name><n1:Value>Test resource text2</n1:Value></n1:ClusterProperty></s:Body>" +
                "</s:Envelope>";

        final Document result = processRequest( "http://ns.l7tech.com/2010/04/gateway-management/testResources/Put", message );

        final Element soapBody = SoapUtil.getBodyElement(result);
        final Element resource = XmlUtil.findExactlyOneChildElementByName( soapBody, NS_GATEWAY_MANAGEMENT, "ClusterProperty" );
        final Element value = XmlUtil.findExactlyOneChildElementByName( resource, NS_GATEWAY_MANAGEMENT, "Value" );

        assertEquals("Resource text", "Test resource text2", XmlUtil.getTextValue(value));
    }

    @Test
    public void testEncassExport() throws Exception {
        final String message =
                "<env:Envelope xmlns:env=\"http://www.w3.org/2003/05/soap-envelope\" xmlns:wsa=\"http://schemas.xmlsoap.org/ws/2004/08/addressing\" xmlns:wsman=\"http://schemas.dmtf.org/wbem/wsman/1/wsman.xsd\" xmlns:wxf=\"http://schemas.xmlsoap.org/ws/2004/09/transfer\" xmlns:xs=\"http://www.w3.org/2001/XMLSchema\"><env:Header><wsa:Action env:mustUnderstand=\"true\">http://ns.l7tech.com/2010/04/gateway-management/encapsulatedAssertions/ExportEncass</wsa:Action><wsa:ReplyTo><wsa:Address env:mustUnderstand=\"true\">http://schemas.xmlsoap.org/ws/2004/08/addressing/role/anonymous</wsa:Address></wsa:ReplyTo><wsa:MessageID env:mustUnderstand=\"true\">uuid:d521f4b4-ef2a-4381-b11c-fa72eb760a99</wsa:MessageID><wsa:To env:mustUnderstand=\"true\">http://localhost:8080/wsman</wsa:To><wsman:ResourceURI>http://ns.l7tech.com/2010/04/gateway-management/encapsulatedAssertions</wsman:ResourceURI><wsman:OperationTimeout>P0Y0M0DT0H5M0.000S</wsman:OperationTimeout><wsman:SelectorSet><wsman:Selector Name=\"id\">"+new Goid(0,1)+"</wsman:Selector></wsman:SelectorSet></env:Header><env:Body/></env:Envelope>";

        final Document result = processRequest( "http://ns.l7tech.com/2010/04/gateway-management/encapsulatedAssertions/ExportEncass", message );

        final Element soapBody = SoapUtil.getBodyElement(result);
        final Element policyExportResult = XmlUtil.findExactlyOneChildElementByName(soapBody, NS_GATEWAY_MANAGEMENT, "EncapsulatedAssertionExportResult");
        final Element resource = XmlUtil.findExactlyOneChildElementByName(policyExportResult, NS_GATEWAY_MANAGEMENT, "Resource");

        assertEquals( "resource type", "encassexport", resource.getAttribute("type"));
        assertTrue( "resource content is export", XmlUtil.getTextValue(resource).contains( "<exp:Export " ));
        assertTrue( "resource content needs to contain policy export", XmlUtil.getTextValue(resource).contains( "<wsp:Policy " ));
    }

    @Test
    public void testEncassImportUsingSelectorsExists() throws Exception {
        final String message =
                "<env:Envelope xmlns:env=\"http://www.w3.org/2003/05/soap-envelope\" xmlns:wsa=\"http://schemas.xmlsoap.org/ws/2004/08/addressing\" xmlns:wsman=\"http://schemas.dmtf.org/wbem/wsman/1/wsman.xsd\" xmlns:wxf=\"http://schemas.xmlsoap.org/ws/2004/09/transfer\" xmlns:xs=\"http://www.w3.org/2001/XMLSchema\"><env:Header><wsa:Action env:mustUnderstand=\"true\">http://ns.l7tech.com/2010/04/gateway-management/encapsulatedAssertions/ImportEncass</wsa:Action><wsa:ReplyTo><wsa:Address env:mustUnderstand=\"true\">http://schemas.xmlsoap.org/ws/2004/08/addressing/role/anonymous</wsa:Address></wsa:ReplyTo><wsa:MessageID env:mustUnderstand=\"true\">uuid:d0f59849-9eaa-4027-8b2e-f5ec2dfc1f9d</wsa:MessageID><wsa:To env:mustUnderstand=\"true\">http://localhost:8080/wsman</wsa:To><wsman:ResourceURI>http://ns.l7tech.com/2010/04/gateway-management/encapsulatedAssertions</wsman:ResourceURI><wsman:OperationTimeout>P0Y0M0DT0H5M0.000S</wsman:OperationTimeout><wsman:SelectorSet><wsman:Selector Name=\"id\">"+new Goid(0,1)+"</wsman:Selector></wsman:SelectorSet></env:Header><env:Body><EncassImportContext xmlns=\"http://ns.l7tech.com/2010/04/gateway-management\"><Properties/><Resource type=\"encassexport\">&lt;?xml version=\"1.0\" encoding=\"UTF-8\"?&gt;\n" +
                        "&lt;exp:Export Version=\"3.0\"\n" +
                        "    xmlns:L7p=\"http://www.layer7tech.com/ws/policy\"\n" +
                        "    xmlns:exp=\"http://www.layer7tech.com/ws/policy/export\" xmlns:wsp=\"http://schemas.xmlsoap.org/ws/2002/12/policy\"&gt;\n" +
                        "    &lt;exp:References/&gt;\n" +
                        "    &lt;wsp:Policy xmlns:L7p=\"http://www.layer7tech.com/ws/policy\" xmlns:wsp=\"http://schemas.xmlsoap.org/ws/2002/12/policy\"&gt;\n" +
                        "        &lt;wsp:All wsp:Usage=\"Required\"&gt;\n" +
                        "            &lt;L7p:testEncassImportUsingSelectorsExists/&gt;\n" +
                        "        &lt;/wsp:All&gt;\n" +
                        "    &lt;/wsp:Policy&gt;\n" +
                        "    &lt;enc:EncapsulatedAssertion guid=\"ABCD-0001\"\n" +
                        "        xmlns:L7=\"http://ns.l7tech.com/secureSpan/1.0/core\"\n" +
                        "        xmlns:enc=\"http://ns.l7tech.com/secureSpan/1.0/encass\"\n" +
                        "        xmlns:xs=\"http://www.w3.org/2001/XMLSchema\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"&gt;\n" +
                        "        &lt;L7:name&gt;Test Encass Config 1&lt;/L7:name&gt;\n" +
                        "        &lt;enc:Policy guid=\"e629f196-dc61-3509-aa9a-61df552423a0\" name=\"Test Policy\"/&gt;\n" +
                        "        &lt;enc:EncapsulatedAssertionArguments/&gt;\n" +
                        "        &lt;enc:EncapsulatedAssertionResults/&gt;\n" +
                        "        &lt;enc:Properties&gt;\n" +
                        "            &lt;entry&gt;\n" +
                        "                &lt;key xsi:type=\"xs:string\"&gt;artifactVersion&lt;/key&gt;\n" +
                        "                &lt;value xsi:type=\"xs:string\"&gt;6b38371b5ce7a8dda67cc6f827fffc29765ea334&lt;/value&gt;\n" +
                        "            &lt;/entry&gt;\n" +
                        "            &lt;entry&gt;\n" +
                        "                &lt;key xsi:type=\"xs:string\"&gt;policyGuid&lt;/key&gt;\n" +
                        "                &lt;value xsi:type=\"xs:string\"&gt;e629f196-dc61-3509-aa9a-61df552423a0&lt;/value&gt;\n" +
                        "            &lt;/entry&gt;\n" +
                        "        &lt;/enc:Properties&gt;\n" +
                        "    &lt;/enc:EncapsulatedAssertion&gt;\n" +
                        "&lt;/exp:Export&gt;" +
                        "</Resource></EncassImportContext></env:Body></env:Envelope>";

        final Document result = processRequest( "http://ns.l7tech.com/2010/04/gateway-management/encapsulatedAssertions/ImportEncass", message );

        final Element soapBody = SoapUtil.getBodyElement(result);
        XmlUtil.findExactlyOneChildElementByName(soapBody, NS_GATEWAY_MANAGEMENT, "EncapsulatedAssertionImportResult");

        EncapsulatedAssertionConfigManagerStub encassManager = applicationContext.getBean("encapsulatedAssertionConfigManager", EncapsulatedAssertionConfigManagerStub.class);
        EncapsulatedAssertionConfig encapsulatedAssertionConfig = encassManager.findByGuid("ABCD-0001");
        Assert.assertTrue(encapsulatedAssertionConfig.getPolicy().getXml().contains("testEncassImportUsingSelectorsExists"));
    }

    @Test
    public void testEncassImportUsingSelectorsNotExists() throws Exception {
        final String message =
                "<env:Envelope xmlns:env=\"http://www.w3.org/2003/05/soap-envelope\" xmlns:wsa=\"http://schemas.xmlsoap.org/ws/2004/08/addressing\" xmlns:wsman=\"http://schemas.dmtf.org/wbem/wsman/1/wsman.xsd\" xmlns:wxf=\"http://schemas.xmlsoap.org/ws/2004/09/transfer\" xmlns:xs=\"http://www.w3.org/2001/XMLSchema\"><env:Header><wsa:Action env:mustUnderstand=\"true\">http://ns.l7tech.com/2010/04/gateway-management/encapsulatedAssertions/ImportEncass</wsa:Action><wsa:ReplyTo><wsa:Address env:mustUnderstand=\"true\">http://schemas.xmlsoap.org/ws/2004/08/addressing/role/anonymous</wsa:Address></wsa:ReplyTo><wsa:MessageID env:mustUnderstand=\"true\">uuid:d0f59849-9eaa-4027-8b2e-f5ec2dfc1f9d</wsa:MessageID><wsa:To env:mustUnderstand=\"true\">http://localhost:8080/wsman</wsa:To><wsman:ResourceURI>http://ns.l7tech.com/2010/04/gateway-management/encapsulatedAssertions</wsman:ResourceURI><wsman:OperationTimeout>P0Y0M0DT0H5M0.000S</wsman:OperationTimeout><wsman:SelectorSet><wsman:Selector Name=\"id\">"+new Goid(0,2)+"</wsman:Selector></wsman:SelectorSet></env:Header><env:Body><EncassImportContext xmlns=\"http://ns.l7tech.com/2010/04/gateway-management\"><Properties/><Resource type=\"encassexport\">&lt;?xml version=\"1.0\" encoding=\"UTF-8\"?&gt;\n" +
                        "&lt;exp:Export Version=\"3.0\"\n" +
                        "    xmlns:L7p=\"http://www.layer7tech.com/ws/policy\"\n" +
                        "    xmlns:exp=\"http://www.layer7tech.com/ws/policy/export\" xmlns:wsp=\"http://schemas.xmlsoap.org/ws/2002/12/policy\"&gt;\n" +
                        "    &lt;exp:References/&gt;\n" +
                        "    &lt;wsp:Policy xmlns:L7p=\"http://www.layer7tech.com/ws/policy\" xmlns:wsp=\"http://schemas.xmlsoap.org/ws/2002/12/policy\"&gt;\n" +
                        "        &lt;wsp:All wsp:Usage=\"Required\"&gt;\n" +
                        "            &lt;L7p:AuditAssertion/&gt;\n" +
                        "        &lt;/wsp:All&gt;\n" +
                        "    &lt;/wsp:Policy&gt;\n" +
                        "    &lt;enc:EncapsulatedAssertion guid=\"ABCD-0001\"\n" +
                        "        xmlns:L7=\"http://ns.l7tech.com/secureSpan/1.0/core\"\n" +
                        "        xmlns:enc=\"http://ns.l7tech.com/secureSpan/1.0/encass\"\n" +
                        "        xmlns:xs=\"http://www.w3.org/2001/XMLSchema\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"&gt;\n" +
                        "        &lt;L7:name&gt;Test Encass Config 1&lt;/L7:name&gt;\n" +
                        "        &lt;enc:Policy guid=\"e629f196-dc61-3509-aa9a-61df552423a0\" name=\"Test Policy\"/&gt;\n" +
                        "        &lt;enc:EncapsulatedAssertionArguments/&gt;\n" +
                        "        &lt;enc:EncapsulatedAssertionResults/&gt;\n" +
                        "        &lt;enc:Properties&gt;\n" +
                        "            &lt;entry&gt;\n" +
                        "                &lt;key xsi:type=\"xs:string\"&gt;artifactVersion&lt;/key&gt;\n" +
                        "                &lt;value xsi:type=\"xs:string\"&gt;6b38371b5ce7a8dda67cc6f827fffc29765ea334&lt;/value&gt;\n" +
                        "            &lt;/entry&gt;\n" +
                        "            &lt;entry&gt;\n" +
                        "                &lt;key xsi:type=\"xs:string\"&gt;policyGuid&lt;/key&gt;\n" +
                        "                &lt;value xsi:type=\"xs:string\"&gt;e629f196-dc61-3509-aa9a-61df552423a0&lt;/value&gt;\n" +
                        "            &lt;/entry&gt;\n" +
                        "        &lt;/enc:Properties&gt;\n" +
                        "    &lt;/enc:EncapsulatedAssertion&gt;\n" +
                        "&lt;/exp:Export&gt;" +
                        "</Resource></EncassImportContext></env:Body></env:Envelope>";

        final Document result = processRequest( "http://ns.l7tech.com/2010/04/gateway-management/encapsulatedAssertions/ImportEncass", message );

        final Element soapBody = SoapUtil.getBodyElement(result);
        Element faultElement = XmlUtil.findExactlyOneChildElementByName(soapBody, NS_SOAP_ENVELOPE, "Fault");
        Element detailElement = XmlUtil.findExactlyOneChildElementByName(faultElement, NS_SOAP_ENVELOPE, "Detail");

        assertTrue("the error message is incorrect", XmlUtil.getTextValue(XmlUtil.findExactlyOneChildElementByName(detailElement, NS_WS_MANAGEMENT, "FaultDetail")).contains("InvalidValue"));

    }

    @Test
    public void testEncassImportNewExisting() throws Exception {
        final String message =
                "<env:Envelope xmlns:env=\"http://www.w3.org/2003/05/soap-envelope\" xmlns:wsa=\"http://schemas.xmlsoap.org/ws/2004/08/addressing\" xmlns:wsman=\"http://schemas.dmtf.org/wbem/wsman/1/wsman.xsd\" xmlns:wxf=\"http://schemas.xmlsoap.org/ws/2004/09/transfer\" xmlns:xs=\"http://www.w3.org/2001/XMLSchema\"><env:Header><wsa:Action env:mustUnderstand=\"true\">http://ns.l7tech.com/2010/04/gateway-management/encapsulatedAssertions/ImportEncass</wsa:Action><wsa:ReplyTo><wsa:Address env:mustUnderstand=\"true\">http://schemas.xmlsoap.org/ws/2004/08/addressing/role/anonymous</wsa:Address></wsa:ReplyTo><wsa:MessageID env:mustUnderstand=\"true\">uuid:d0f59849-9eaa-4027-8b2e-f5ec2dfc1f9d</wsa:MessageID><wsa:To env:mustUnderstand=\"true\">http://localhost:8080/wsman</wsa:To><wsman:ResourceURI>http://ns.l7tech.com/2010/04/gateway-management/encapsulatedAssertions</wsman:ResourceURI><wsman:OperationTimeout>P0Y0M0DT0H5M0.000S</wsman:OperationTimeout></env:Header><env:Body><EncassImportContext xmlns=\"http://ns.l7tech.com/2010/04/gateway-management\"><Properties/><Resource type=\"encassexport\">&lt;?xml version=\"1.0\" encoding=\"UTF-8\"?&gt;\n" +
                        "&lt;exp:Export Version=\"3.0\"\n" +
                        "    xmlns:L7p=\"http://www.layer7tech.com/ws/policy\"\n" +
                        "    xmlns:exp=\"http://www.layer7tech.com/ws/policy/export\" xmlns:wsp=\"http://schemas.xmlsoap.org/ws/2002/12/policy\"&gt;\n" +
                        "    &lt;exp:References/&gt;\n" +
                        "    &lt;wsp:Policy xmlns:L7p=\"http://www.layer7tech.com/ws/policy\" xmlns:wsp=\"http://schemas.xmlsoap.org/ws/2002/12/policy\"&gt;\n" +
                        "        &lt;wsp:All wsp:Usage=\"Required\"&gt;\n" +
                        "            &lt;L7p:AuditAssertion/&gt;\n" +
                        "        &lt;/wsp:All&gt;\n" +
                        "    &lt;/wsp:Policy&gt;\n" +
                        "    &lt;enc:EncapsulatedAssertion guid=\"ABCD-0001\"\n" +
                        "        xmlns:L7=\"http://ns.l7tech.com/secureSpan/1.0/core\"\n" +
                        "        xmlns:enc=\"http://ns.l7tech.com/secureSpan/1.0/encass\"\n" +
                        "        xmlns:xs=\"http://www.w3.org/2001/XMLSchema\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"&gt;\n" +
                        "        &lt;L7:name&gt;Test Encass Config 1&lt;/L7:name&gt;\n" +
                        "        &lt;enc:Policy guid=\"e629f196-dc61-3509-aa9a-61df552423a0\" name=\"Test Policy\"/&gt;\n" +
                        "        &lt;enc:EncapsulatedAssertionArguments/&gt;\n" +
                        "        &lt;enc:EncapsulatedAssertionResults/&gt;\n" +
                        "        &lt;enc:Properties&gt;\n" +
                        "            &lt;entry&gt;\n" +
                        "                &lt;key xsi:type=\"xs:string\"&gt;artifactVersion&lt;/key&gt;\n" +
                        "                &lt;value xsi:type=\"xs:string\"&gt;6b38371b5ce7a8dda67cc6f827fffc29765ea334&lt;/value&gt;\n" +
                        "            &lt;/entry&gt;\n" +
                        "            &lt;entry&gt;\n" +
                        "                &lt;key xsi:type=\"xs:string\"&gt;policyGuid&lt;/key&gt;\n" +
                        "                &lt;value xsi:type=\"xs:string\"&gt;e629f196-dc61-3509-aa9a-61df552423a0&lt;/value&gt;\n" +
                        "            &lt;/entry&gt;\n" +
                        "        &lt;/enc:Properties&gt;\n" +
                        "    &lt;/enc:EncapsulatedAssertion&gt;\n" +
                        "&lt;/exp:Export&gt;" +
                        "</Resource></EncassImportContext></env:Body></env:Envelope>";

        final Document result = processRequest( "http://ns.l7tech.com/2010/04/gateway-management/encapsulatedAssertions/ImportEncass", message );

        final Element soapBody = SoapUtil.getBodyElement(result);
        Element faultElement = XmlUtil.findExactlyOneChildElementByName(soapBody, NS_SOAP_ENVELOPE, "Fault");
        Element detailElement = XmlUtil.findExactlyOneChildElementByName(faultElement, NS_SOAP_ENVELOPE, "Detail");

        assertTrue("the error message is incorrect", XmlUtil.getTextValue(XmlUtil.findExactlyOneChildElementByName(detailElement, NS_SOAP_ENVELOPE, "Text")).contains("EncapsulatedAssertion already exists"));
    }

    @Test
    public void testEncassImportNewExistingPolicy() throws Exception {
        final String message =
                "<env:Envelope xmlns:env=\"http://www.w3.org/2003/05/soap-envelope\" xmlns:wsa=\"http://schemas.xmlsoap.org/ws/2004/08/addressing\" xmlns:wsman=\"http://schemas.dmtf.org/wbem/wsman/1/wsman.xsd\" xmlns:wxf=\"http://schemas.xmlsoap.org/ws/2004/09/transfer\" xmlns:xs=\"http://www.w3.org/2001/XMLSchema\"><env:Header><wsa:Action env:mustUnderstand=\"true\">http://ns.l7tech.com/2010/04/gateway-management/encapsulatedAssertions/ImportEncass</wsa:Action><wsa:ReplyTo><wsa:Address env:mustUnderstand=\"true\">http://schemas.xmlsoap.org/ws/2004/08/addressing/role/anonymous</wsa:Address></wsa:ReplyTo><wsa:MessageID env:mustUnderstand=\"true\">uuid:d0f59849-9eaa-4027-8b2e-f5ec2dfc1f9d</wsa:MessageID><wsa:To env:mustUnderstand=\"true\">http://localhost:8080/wsman</wsa:To><wsman:ResourceURI>http://ns.l7tech.com/2010/04/gateway-management/encapsulatedAssertions</wsman:ResourceURI><wsman:OperationTimeout>P0Y0M0DT0H5M0.000S</wsman:OperationTimeout></env:Header><env:Body><EncassImportContext xmlns=\"http://ns.l7tech.com/2010/04/gateway-management\"><Properties/><Resource type=\"encassexport\">&lt;?xml version=\"1.0\" encoding=\"UTF-8\"?&gt;\n" +
                        "&lt;exp:Export Version=\"3.0\"\n" +
                        "    xmlns:L7p=\"http://www.layer7tech.com/ws/policy\"\n" +
                        "    xmlns:exp=\"http://www.layer7tech.com/ws/policy/export\" xmlns:wsp=\"http://schemas.xmlsoap.org/ws/2002/12/policy\"&gt;\n" +
                        "    &lt;exp:References/&gt;\n" +
                        "    &lt;wsp:Policy xmlns:L7p=\"http://www.layer7tech.com/ws/policy\" xmlns:wsp=\"http://schemas.xmlsoap.org/ws/2002/12/policy\"&gt;\n" +
                        "        &lt;wsp:All wsp:Usage=\"Required\"&gt;\n" +
                        "            &lt;L7p:AuditAssertion/&gt;\n" +
                        "        &lt;/wsp:All&gt;\n" +
                        "    &lt;/wsp:Policy&gt;\n" +
                        "    &lt;enc:EncapsulatedAssertion guid=\"ABCD-0002\"\n" +
                        "        xmlns:L7=\"http://ns.l7tech.com/secureSpan/1.0/core\"\n" +
                        "        xmlns:enc=\"http://ns.l7tech.com/secureSpan/1.0/encass\"\n" +
                        "        xmlns:xs=\"http://www.w3.org/2001/XMLSchema\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"&gt;\n" +
                        "        &lt;L7:name&gt;Test Encass Config 2&lt;/L7:name&gt;\n" +
                        "        &lt;enc:Policy guid=\"e629f196-dc61-3509-aa9a-61df552423a0\" name=\"Test Policy\"/&gt;\n" +
                        "        &lt;enc:EncapsulatedAssertionArguments/&gt;\n" +
                        "        &lt;enc:EncapsulatedAssertionResults/&gt;\n" +
                        "        &lt;enc:Properties&gt;\n" +
                        "            &lt;entry&gt;\n" +
                        "                &lt;key xsi:type=\"xs:string\"&gt;artifactVersion&lt;/key&gt;\n" +
                        "                &lt;value xsi:type=\"xs:string\"&gt;6b38371b5ce7a8dda67cc6f827fffc29765ea334&lt;/value&gt;\n" +
                        "            &lt;/entry&gt;\n" +
                        "            &lt;entry&gt;\n" +
                        "                &lt;key xsi:type=\"xs:string\"&gt;policyGuid&lt;/key&gt;\n" +
                        "                &lt;value xsi:type=\"xs:string\"&gt;e629f196-dc61-3509-aa9a-61df552423a0&lt;/value&gt;\n" +
                        "            &lt;/entry&gt;\n" +
                        "        &lt;/enc:Properties&gt;\n" +
                        "    &lt;/enc:EncapsulatedAssertion&gt;\n" +
                        "&lt;/exp:Export&gt;" +
                        "</Resource></EncassImportContext></env:Body></env:Envelope>";

        final Document result = processRequest( "http://ns.l7tech.com/2010/04/gateway-management/encapsulatedAssertions/ImportEncass", message );

        final Element soapBody = SoapUtil.getBodyElement(result);
        Element faultElement = XmlUtil.findExactlyOneChildElementByName(soapBody, NS_SOAP_ENVELOPE, "Fault");
        Element detailElement = XmlUtil.findExactlyOneChildElementByName(faultElement, NS_SOAP_ENVELOPE, "Detail");

        assertTrue( "the error message is incorrect", XmlUtil.getTextValue(XmlUtil.findExactlyOneChildElementByName(detailElement, NS_SOAP_ENVELOPE, "Text")).contains( "EncapsulatedAssertion backing policy already exists" ));
    }

    @Test
    public void testEncassImportNewExistingPolicyRenameInstruction() throws Exception {
        final String message =
                "<env:Envelope xmlns:env=\"http://www.w3.org/2003/05/soap-envelope\" xmlns:wsa=\"http://schemas.xmlsoap.org/ws/2004/08/addressing\" xmlns:wsman=\"http://schemas.dmtf.org/wbem/wsman/1/wsman.xsd\" xmlns:wxf=\"http://schemas.xmlsoap.org/ws/2004/09/transfer\" xmlns:xs=\"http://www.w3.org/2001/XMLSchema\"><env:Header><wsa:Action env:mustUnderstand=\"true\">http://ns.l7tech.com/2010/04/gateway-management/encapsulatedAssertions/ImportEncass</wsa:Action><wsa:ReplyTo><wsa:Address env:mustUnderstand=\"true\">http://schemas.xmlsoap.org/ws/2004/08/addressing/role/anonymous</wsa:Address></wsa:ReplyTo><wsa:MessageID env:mustUnderstand=\"true\">uuid:d0f59849-9eaa-4027-8b2e-f5ec2dfc1f9d</wsa:MessageID><wsa:To env:mustUnderstand=\"true\">http://localhost:8080/wsman</wsa:To><wsman:ResourceURI>http://ns.l7tech.com/2010/04/gateway-management/encapsulatedAssertions</wsman:ResourceURI><wsman:OperationTimeout>P0Y0M0DT0H5M0.000S</wsman:OperationTimeout></env:Header><env:Body><EncassImportContext xmlns=\"http://ns.l7tech.com/2010/04/gateway-management\"><Properties/><Resource type=\"encassexport\">&lt;?xml version=\"1.0\" encoding=\"UTF-8\"?&gt;\n" +
                        "&lt;exp:Export Version=\"3.0\"\n" +
                        "    xmlns:L7p=\"http://www.layer7tech.com/ws/policy\"\n" +
                        "    xmlns:exp=\"http://www.layer7tech.com/ws/policy/export\" xmlns:wsp=\"http://schemas.xmlsoap.org/ws/2002/12/policy\"&gt;\n" +
                        "    &lt;exp:References/&gt;\n" +
                        "    &lt;wsp:Policy xmlns:L7p=\"http://www.layer7tech.com/ws/policy\" xmlns:wsp=\"http://schemas.xmlsoap.org/ws/2002/12/policy\"&gt;\n" +
                        "        &lt;wsp:All wsp:Usage=\"Required\"&gt;\n" +
                        "            &lt;L7p:testEncassImportNewExistingPolicyRenameInstruction/&gt;\n" +
                        "        &lt;/wsp:All&gt;\n" +
                        "    &lt;/wsp:Policy&gt;\n" +
                        "    &lt;enc:EncapsulatedAssertion guid=\"ABCD-0002\"\n" +
                        "        xmlns:L7=\"http://ns.l7tech.com/secureSpan/1.0/core\"\n" +
                        "        xmlns:enc=\"http://ns.l7tech.com/secureSpan/1.0/encass\"\n" +
                        "        xmlns:xs=\"http://www.w3.org/2001/XMLSchema\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"&gt;\n" +
                        "        &lt;L7:name&gt;Test Encass Config 2&lt;/L7:name&gt;\n" +
                        "        &lt;enc:Policy guid=\"e629f196-dc61-3509-aa9a-61df552423a0\" name=\"Test Policy\"/&gt;\n" +
                        "        &lt;enc:EncapsulatedAssertionArguments/&gt;\n" +
                        "        &lt;enc:EncapsulatedAssertionResults/&gt;\n" +
                        "        &lt;enc:Properties&gt;\n" +
                        "            &lt;entry&gt;\n" +
                        "                &lt;key xsi:type=\"xs:string\"&gt;artifactVersion&lt;/key&gt;\n" +
                        "                &lt;value xsi:type=\"xs:string\"&gt;6b38371b5ce7a8dda67cc6f827fffc29765ea334&lt;/value&gt;\n" +
                        "            &lt;/entry&gt;\n" +
                        "            &lt;entry&gt;\n" +
                        "                &lt;key xsi:type=\"xs:string\"&gt;policyGuid&lt;/key&gt;\n" +
                        "                &lt;value xsi:type=\"xs:string\"&gt;e629f196-dc61-3509-aa9a-61df552423a0&lt;/value&gt;\n" +
                        "            &lt;/entry&gt;\n" +
                        "        &lt;/enc:Properties&gt;\n" +
                        "    &lt;/enc:EncapsulatedAssertion&gt;\n" +
                        "&lt;/exp:Export&gt;" +
                        "</Resource>" +
                        "            <PolicyReferenceInstructions>\n" +
                        "                <PolicyReferenceInstruction mappedName=\"testEncassImportNewExistingPolicyRenameInstructionPolicy\"" +
                        "                    referenceId=\"e629f196-dc61-3509-aa9a-61df552423a0\"\n" +
                        "                    referenceType=\"com.l7tech.policy.exporter.IncludedPolicyReference\" type=\"Rename\"/>\n" +
                        "            </PolicyReferenceInstructions></EncassImportContext></env:Body></env:Envelope>";

        final Document result = processRequest( "http://ns.l7tech.com/2010/04/gateway-management/encapsulatedAssertions/ImportEncass", message );

        final Element soapBody = SoapUtil.getBodyElement(result);
        XmlUtil.findExactlyOneChildElementByName(soapBody, NS_GATEWAY_MANAGEMENT, "EncapsulatedAssertionImportResult");

        EncapsulatedAssertionConfigManagerStub encassManager = applicationContext.getBean("encapsulatedAssertionConfigManager", EncapsulatedAssertionConfigManagerStub.class);
        EncapsulatedAssertionConfig encapsulatedAssertionConfig = encassManager.findByGuid("ABCD-0002");
        Assert.assertNotNull(encapsulatedAssertionConfig);
        Assert.assertEquals("testEncassImportNewExistingPolicyRenameInstructionPolicy", encapsulatedAssertionConfig.getPolicy().getName());
        Assert.assertTrue(encapsulatedAssertionConfig.getPolicy().getXml().contains("testEncassImportNewExistingPolicyRenameInstruction"));
    }

    @Test
    public void testEncassImportNewExistingPolicyDeleteInstruction() throws Exception {
        final String message =
                "<env:Envelope xmlns:env=\"http://www.w3.org/2003/05/soap-envelope\" xmlns:wsa=\"http://schemas.xmlsoap.org/ws/2004/08/addressing\" xmlns:wsman=\"http://schemas.dmtf.org/wbem/wsman/1/wsman.xsd\" xmlns:wxf=\"http://schemas.xmlsoap.org/ws/2004/09/transfer\" xmlns:xs=\"http://www.w3.org/2001/XMLSchema\"><env:Header><wsa:Action env:mustUnderstand=\"true\">http://ns.l7tech.com/2010/04/gateway-management/encapsulatedAssertions/ImportEncass</wsa:Action><wsa:ReplyTo><wsa:Address env:mustUnderstand=\"true\">http://schemas.xmlsoap.org/ws/2004/08/addressing/role/anonymous</wsa:Address></wsa:ReplyTo><wsa:MessageID env:mustUnderstand=\"true\">uuid:d0f59849-9eaa-4027-8b2e-f5ec2dfc1f9d</wsa:MessageID><wsa:To env:mustUnderstand=\"true\">http://localhost:8080/wsman</wsa:To><wsman:ResourceURI>http://ns.l7tech.com/2010/04/gateway-management/encapsulatedAssertions</wsman:ResourceURI><wsman:OperationTimeout>P0Y0M0DT0H5M0.000S</wsman:OperationTimeout></env:Header><env:Body><EncassImportContext xmlns=\"http://ns.l7tech.com/2010/04/gateway-management\"><Properties/><Resource type=\"encassexport\">&lt;?xml version=\"1.0\" encoding=\"UTF-8\"?&gt;\n" +
                        "&lt;exp:Export Version=\"3.0\"\n" +
                        "    xmlns:L7p=\"http://www.layer7tech.com/ws/policy\"\n" +
                        "    xmlns:exp=\"http://www.layer7tech.com/ws/policy/export\" xmlns:wsp=\"http://schemas.xmlsoap.org/ws/2002/12/policy\"&gt;\n" +
                        "    &lt;exp:References/&gt;\n" +
                        "    &lt;wsp:Policy xmlns:L7p=\"http://www.layer7tech.com/ws/policy\" xmlns:wsp=\"http://schemas.xmlsoap.org/ws/2002/12/policy\"&gt;\n" +
                        "        &lt;wsp:All wsp:Usage=\"Required\"&gt;\n" +
                        "            &lt;L7p:testEncassImportNewExistingPolicyDeleteInstruction/&gt;\n" +
                        "        &lt;/wsp:All&gt;\n" +
                        "    &lt;/wsp:Policy&gt;\n" +
                        "    &lt;enc:EncapsulatedAssertion guid=\"ABCD-0002\"\n" +
                        "        xmlns:L7=\"http://ns.l7tech.com/secureSpan/1.0/core\"\n" +
                        "        xmlns:enc=\"http://ns.l7tech.com/secureSpan/1.0/encass\"\n" +
                        "        xmlns:xs=\"http://www.w3.org/2001/XMLSchema\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"&gt;\n" +
                        "        &lt;L7:name&gt;Test Encass Config 2&lt;/L7:name&gt;\n" +
                        "        &lt;enc:Policy guid=\"e629f196-dc61-3509-aa9a-61df552423a0\" name=\"Test Policy\"/&gt;\n" +
                        "        &lt;enc:EncapsulatedAssertionArguments/&gt;\n" +
                        "        &lt;enc:EncapsulatedAssertionResults/&gt;\n" +
                        "        &lt;enc:Properties&gt;\n" +
                        "            &lt;entry&gt;\n" +
                        "                &lt;key xsi:type=\"xs:string\"&gt;artifactVersion&lt;/key&gt;\n" +
                        "                &lt;value xsi:type=\"xs:string\"&gt;6b38371b5ce7a8dda67cc6f827fffc29765ea334&lt;/value&gt;\n" +
                        "            &lt;/entry&gt;\n" +
                        "            &lt;entry&gt;\n" +
                        "                &lt;key xsi:type=\"xs:string\"&gt;policyGuid&lt;/key&gt;\n" +
                        "                &lt;value xsi:type=\"xs:string\"&gt;e629f196-dc61-3509-aa9a-61df552423a0&lt;/value&gt;\n" +
                        "            &lt;/entry&gt;\n" +
                        "        &lt;/enc:Properties&gt;\n" +
                        "    &lt;/enc:EncapsulatedAssertion&gt;\n" +
                        "&lt;/exp:Export&gt;" +
                        "</Resource>" +
                        "            <PolicyReferenceInstructions>\n" +
                        "                <PolicyReferenceInstruction " +
                        "                    referenceId=\"e629f196-dc61-3509-aa9a-61df552423a0\"\n" +
                        "                    referenceType=\"com.l7tech.policy.exporter.IncludedPolicyReference\" type=\"Delete\"/>\n" +
                        "            </PolicyReferenceInstructions></EncassImportContext></env:Body></env:Envelope>";

        final Document result = processRequest( "http://ns.l7tech.com/2010/04/gateway-management/encapsulatedAssertions/ImportEncass", message );

        final Element soapBody = SoapUtil.getBodyElement(result);
        Element faultElement = XmlUtil.findExactlyOneChildElementByName(soapBody, NS_SOAP_ENVELOPE, "Fault");
        Element detailElement = XmlUtil.findExactlyOneChildElementByName(faultElement, NS_SOAP_ENVELOPE, "Detail");

        assertTrue( "the error message is incorrect", XmlUtil.getTextValue(XmlUtil.findExactlyOneChildElementByName(detailElement, NS_SOAP_ENVELOPE, "Text")).contains( "PolicyReferenceInstruction uses unsupported instruction type" ));
    }

    @Test
    public void testEncassNewImportNewPolicy() throws Exception {
        final String message =
                "<env:Envelope xmlns:env=\"http://www.w3.org/2003/05/soap-envelope\" xmlns:wsa=\"http://schemas.xmlsoap.org/ws/2004/08/addressing\" xmlns:wsman=\"http://schemas.dmtf.org/wbem/wsman/1/wsman.xsd\" xmlns:wxf=\"http://schemas.xmlsoap.org/ws/2004/09/transfer\" xmlns:xs=\"http://www.w3.org/2001/XMLSchema\"><env:Header><wsa:Action env:mustUnderstand=\"true\">http://ns.l7tech.com/2010/04/gateway-management/encapsulatedAssertions/ImportEncass</wsa:Action><wsa:ReplyTo><wsa:Address env:mustUnderstand=\"true\">http://schemas.xmlsoap.org/ws/2004/08/addressing/role/anonymous</wsa:Address></wsa:ReplyTo><wsa:MessageID env:mustUnderstand=\"true\">uuid:d0f59849-9eaa-4027-8b2e-f5ec2dfc1f9d</wsa:MessageID><wsa:To env:mustUnderstand=\"true\">http://localhost:8080/wsman</wsa:To><wsman:ResourceURI>http://ns.l7tech.com/2010/04/gateway-management/encapsulatedAssertions</wsman:ResourceURI><wsman:OperationTimeout>P0Y0M0DT0H5M0.000S</wsman:OperationTimeout></env:Header><env:Body><EncassImportContext xmlns=\"http://ns.l7tech.com/2010/04/gateway-management\"><Properties/><Resource type=\"encassexport\">&lt;?xml version=\"1.0\" encoding=\"UTF-8\"?&gt;\n" +
                        "&lt;exp:Export Version=\"3.0\"\n" +
                        "    xmlns:L7p=\"http://www.layer7tech.com/ws/policy\"\n" +
                        "    xmlns:exp=\"http://www.layer7tech.com/ws/policy/export\" xmlns:wsp=\"http://schemas.xmlsoap.org/ws/2002/12/policy\"&gt;\n" +
                        "    &lt;exp:References/&gt;\n" +
                        "    &lt;wsp:Policy xmlns:L7p=\"http://www.layer7tech.com/ws/policy\" xmlns:wsp=\"http://schemas.xmlsoap.org/ws/2002/12/policy\"&gt;\n" +
                        "        &lt;wsp:All wsp:Usage=\"Required\"&gt;\n" +
                        "            &lt;L7p:testEncassNewImportNewPolicy/&gt;\n" +
                        "        &lt;/wsp:All&gt;\n" +
                        "    &lt;/wsp:Policy&gt;\n" +
                        "    &lt;enc:EncapsulatedAssertion guid=\"ABCD-0002\"\n" +
                        "        xmlns:L7=\"http://ns.l7tech.com/secureSpan/1.0/core\"\n" +
                        "        xmlns:enc=\"http://ns.l7tech.com/secureSpan/1.0/encass\"\n" +
                        "        xmlns:xs=\"http://www.w3.org/2001/XMLSchema\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"&gt;\n" +
                        "        &lt;L7:name&gt;Test Encass Config 2&lt;/L7:name&gt;\n" +
                        "        &lt;enc:Policy guid=\"e629f196-dc61-3509-aa9a-61df552423a1\" name=\"testEncassNewImportNewPolicy\"/&gt;\n" +
                        "        &lt;enc:EncapsulatedAssertionArguments/&gt;\n" +
                        "        &lt;enc:EncapsulatedAssertionResults/&gt;\n" +
                        "        &lt;enc:Properties&gt;\n" +
                        "            &lt;entry&gt;\n" +
                        "                &lt;key xsi:type=\"xs:string\"&gt;artifactVersion&lt;/key&gt;\n" +
                        "                &lt;value xsi:type=\"xs:string\"&gt;6b38371b5ce7a8dda67cc6f827fffc29765ea334&lt;/value&gt;\n" +
                        "            &lt;/entry&gt;\n" +
                        "            &lt;entry&gt;\n" +
                        "                &lt;key xsi:type=\"xs:string\"&gt;policyGuid&lt;/key&gt;\n" +
                        "                &lt;value xsi:type=\"xs:string\"&gt;e629f196-dc61-3509-aa9a-61df552423a1&lt;/value&gt;\n" +
                        "            &lt;/entry&gt;\n" +
                        "        &lt;/enc:Properties&gt;\n" +
                        "    &lt;/enc:EncapsulatedAssertion&gt;\n" +
                        "&lt;/exp:Export&gt;" +
                        "</Resource></EncassImportContext></env:Body></env:Envelope>";

        final Document result = processRequest( "http://ns.l7tech.com/2010/04/gateway-management/encapsulatedAssertions/ImportEncass", message );

        final Element soapBody = SoapUtil.getBodyElement(result);
        XmlUtil.findExactlyOneChildElementByName(soapBody, NS_GATEWAY_MANAGEMENT, "EncapsulatedAssertionImportResult");

        EncapsulatedAssertionConfigManagerStub encassManager = applicationContext.getBean("encapsulatedAssertionConfigManager", EncapsulatedAssertionConfigManagerStub.class);
        EncapsulatedAssertionConfig encapsulatedAssertionConfig = encassManager.findByGuid("ABCD-0002");
        Assert.assertNotNull(encapsulatedAssertionConfig);
        Assert.assertEquals("testEncassNewImportNewPolicy", encapsulatedAssertionConfig.getPolicy().getName());
        Assert.assertTrue(encapsulatedAssertionConfig.getPolicy().getXml().contains("testEncassNewImportNewPolicy"));
    }

    @Test
    public void testCreateNewHttpConfiguration() throws Exception {
        String payload =
                        "            <l7:HttpConfiguration xmlns:l7=\"http://ns.l7tech.com/2010/04/gateway-management\" id=\"c49e5455397863a0708cc0012dd98e9f\" version=\"0\">\n" +
                        "               <l7:Host>testwsman</l7:Host>\n" +
                        "               <l7:Port>0</l7:Port>\n" +
                        "               <l7:Path>/test</l7:Path>\n" +
                        "               <l7:TlsKeyUse>default</l7:TlsKeyUse>\n" +
                        "               <l7:TlsKeystoreId>00000000000000000000000000000000</l7:TlsKeystoreId>\n" +
                        "               <l7:ConnectTimeout>-1</l7:ConnectTimeout>\n" +
                        "               <l7:ReadTimeout>-1</l7:ReadTimeout>\n" +
                        "               <l7:FollowRedirects>false</l7:FollowRedirects>\n" +
                        "               <l7:ProxyUse>default</l7:ProxyUse>\n" +
                        "            </l7:HttpConfiguration>";

        doCreate( "http://ns.l7tech.com/2010/04/gateway-management/httpConfigurations", payload, new Goid(0,2).toString() );
    }

    @Test
    public void testDeleteHttpConfiguration() throws Exception {
        String message = "<s:Envelope xmlns:s=\"http://www.w3.org/2003/05/soap-envelope\" xmlns:wsa=\"http://schemas.xmlsoap.org/ws/2004/08/addressing\" xmlns:wsman=\"http://schemas.dmtf.org/wbem/wsman/1/wsman.xsd\"><s:Header><wsa:Action s:mustUnderstand=\"true\">http://schemas.xmlsoap.org/ws/2004/09/transfer/Delete</wsa:Action><wsa:To s:mustUnderstand=\"true\">http://127.0.0.1:8080/wsman</wsa:To><wsman:ResourceURI s:mustUnderstand=\"true\">http://ns.l7tech.com/2010/04/gateway-management/httpConfigurations</wsman:ResourceURI><wsa:MessageID s:mustUnderstand=\"true\">uuid:b2794ffb-7d39-1d39-8002-481688002100</wsa:MessageID><wsa:ReplyTo><wsa:Address>http://schemas.xmlsoap.org/ws/2004/08/addressing/role/anonymous</wsa:Address></wsa:ReplyTo>" +
                "<wsman:SelectorSet><wsman:Selector Name=\"id\">"+new Goid(0,1).toHexString()+"</wsman:Selector></wsman:SelectorSet></s:Header><s:Body/></s:Envelope>";

        final Document result = processRequest( "http://schemas.xmlsoap.org/ws/2004/09/transfer/Delete", message );

        final Element soapBody = SoapUtil.getBodyElement(result);
        assertNotNull("SOAP Body", soapBody);
        assertNull("No body content", soapBody.getFirstChild());
    }

    @Test
    public void testGetHttpConfiguration() throws Exception {
        final String message =
                "<s:Envelope xmlns:s=\"http://www.w3.org/2003/05/soap-envelope\" \n" +
                        "            xmlns:a=\"http://schemas.xmlsoap.org/ws/2004/08/addressing\" \n" +
                        "            xmlns:w=\"http://schemas.dmtf.org/wbem/wsman/1/wsman.xsd\">\n" +
                        "  <s:Header>\n" +
                        "    <a:MessageID>uuid:4ED2993C-4339-4E99-81FC-C2FD3812781A</a:MessageID> \n" +
                        "    <a:To>http://127.0.0.1:8080/wsman</a:To> \n" +
                        "    <a:ReplyTo> \n" +
                        "      <a:Address s:mustUnderstand=\"true\">http://schemas.xmlsoap.org/ws/2004/08/addressing/role/anonymous</a:Address> \n" +
                        "    </a:ReplyTo> \n" +
                        "    <a:Action s:mustUnderstand=\"true\">http://schemas.xmlsoap.org/ws/2004/09/transfer/Get</a:Action> \n" +
                        "    <w:ResourceURI s:mustUnderstand=\"true\">http://ns.l7tech.com/2010/04/gateway-management/httpConfigurations</w:ResourceURI> \n" +
                        "    <w:SelectorSet>\n" +
                        "      <w:Selector Name=\"id\">"+new Goid(0,1).toHexString()+"</w:Selector> \n" +
                        "    </w:SelectorSet>\n" +
                        "    <w:OperationTimeout>PT60.000S</w:OperationTimeout> \n" +
                        "  </s:Header>\n" +
                        "  <s:Body/> \n" +
                        "</s:Envelope>";

        final Document result = processRequest("http://schemas.xmlsoap.org/ws/2004/09/transfer/Get", message);
        System.out.println(XmlUtil.nodeToFormattedString(result));

        final Element soapBody = SoapUtil.getBodyElement(result);
        final Element httpConfigurationElm = XmlUtil.findExactlyOneChildElementByName(soapBody, NS_GATEWAY_MANAGEMENT, "HttpConfiguration");
        System.out.println(XmlUtil.nodeToString(httpConfigurationElm));
        final Element hostElm = XmlUtil.findExactlyOneChildElementByName(httpConfigurationElm, NS_GATEWAY_MANAGEMENT, "Host");

        assertEquals("Host", "TestHost", DomUtils.getTextValue(hostElm));
    }

    @Test
    public void testPutHttpConfiguration() throws Exception {
        final String message = "<s:Envelope xmlns:s=\"http://www.w3.org/2003/05/soap-envelope\" xmlns:wsa=\"http://schemas.xmlsoap.org/ws/2004/08/addressing\" xmlns:wsman=\"http://schemas.dmtf.org/wbem/wsman/1/wsman.xsd\" xmlns:l7=\"http://ns.l7tech.com/2010/04/gateway-management\"><s:Header><wsa:Action s:mustUnderstand=\"true\">http://schemas.xmlsoap.org/ws/2004/09/transfer/Put</wsa:Action><wsa:To s:mustUnderstand=\"true\">http://127.0.0.1:8080/wsman</wsa:To><wsman:ResourceURI s:mustUnderstand=\"true\">http://ns.l7tech.com/2010/04/gateway-management/httpConfigurations</wsman:ResourceURI><wsa:MessageID s:mustUnderstand=\"true\">uuid:afad2993-7d39-1d39-8002-481688002100</wsa:MessageID><wsa:ReplyTo><wsa:Address>http://schemas.xmlsoap.org/ws/2004/08/addressing/role/anonymous</wsa:Address></wsa:ReplyTo><wsman:SelectorSet><wsman:Selector Name=\"id\">"+new Goid(0,1).toHexString()+"</wsman:Selector></wsman:SelectorSet><wsman:RequestEPR/></s:Header><s:Body> "+
                "            <l7:HttpConfiguration id=\""+new Goid(0,1).toHexString()+"\" version=\"0\">\n" +
                "               <l7:Host>NewTestHost</l7:Host>\n" +
                "               <l7:Port>0</l7:Port>\n" +
                "               <l7:Path>/test</l7:Path>\n" +
                "               <l7:TlsKeyUse>default</l7:TlsKeyUse>\n" +
                "               <l7:TlsKeystoreId>00000000000000000000000000000000</l7:TlsKeystoreId>\n" +
                "               <l7:ConnectTimeout>-1</l7:ConnectTimeout>\n" +
                "               <l7:ReadTimeout>-1</l7:ReadTimeout>\n" +
                "               <l7:FollowRedirects>false</l7:FollowRedirects>\n" +
                "               <l7:ProxyUse>default</l7:ProxyUse>\n" +
                "            </l7:HttpConfiguration>"
                +"  </s:Body></s:Envelope>";

        final UnaryVoidThrows<Document,Exception> verifier = new UnaryVoidThrows<Document,Exception>(){
            @Override
            public void call( final Document result ) throws Exception {
                final Element soapBody = SoapUtil.getBodyElement(result);
                final Element httpConfigurationElm = XmlUtil.findExactlyOneChildElementByName(soapBody, NS_GATEWAY_MANAGEMENT, "HttpConfiguration");
                System.out.println(XmlUtil.nodeToString(httpConfigurationElm));
                final Element hostElm = XmlUtil.findExactlyOneChildElementByName(httpConfigurationElm, NS_GATEWAY_MANAGEMENT, "Host");

                assertEquals("Host", "NewTestHost", DomUtils.getTextValue(hostElm));

            }
        };

        putAndVerify( message, verifier, false );
        putAndVerify( message, verifier, true );
    }

    @Test
    public void testCreateRole() throws Exception {
        String payload =
                "        <l7:Role xmlns:l7=\"http://ns.l7tech.com/2010/04/gateway-management\">\n" +
                        "            <l7:name>My Role</l7:name>\n" +
                        "            <l7:description>Test Description</l7:description>\n" +
                        "            <l7:userCreated>false</l7:userCreated>\n" +
                        "            <l7:permissions>\n" +
                        "                <l7:permission id=\"0000000000000000ffffffffffffffff\" version=\"0\">\n" +
                        "                    <l7:operationType>OTHER</l7:operationType>\n" +
                        "                    <l7:otherOperationName>my-operation</l7:otherOperationName>\n" +
                        "                    <l7:entityType>TRUSTED_CERT</l7:entityType>\n" +
                        "                    <l7:predicates>\n" +
                        "                        <l7:predicate>\n" +
                        "                            <l7:type>ObjectIdentityPredicate</l7:type>\n" +
                        "                            <l7:Properties>\n" +
                        "                                <l7:Property key=\"entityId\">\n" +
                        "                                    <l7:StringValue>"+new Goid(0, 1L)+"</l7:StringValue>\n" +
                        "                                </l7:Property>\n" +
                        "                            </l7:Properties>\n" +
                        "                        </l7:predicate>\n" +
                        "                    </l7:predicates>\n" +
                        "                </l7:permission>\n" +
                        "                <l7:permission id=\"0000000000000000ffffffffffffffff\" version=\"0\">\n" +
                        "                    <l7:operationType>DELETE</l7:operationType>\n" +
                        "                    <l7:entityType>SERVICE</l7:entityType>\n" +
                        "                    <l7:predicates>\n" +
                        "                        <l7:predicate>\n" +
                        "                            <l7:type>FolderPredicate</l7:type>\n" +
                        "                            <l7:Properties>\n" +
                        "                                <l7:Property key=\"transitive\">\n" +
                        "                                    <l7:StringValue>false</l7:StringValue>\n" +
                        "                                </l7:Property>\n" +
                        "                                <l7:Property key=\"folderId\">\n" +
                        "                                    <l7:StringValue>"+new Goid(0,-5002L)+"</l7:StringValue>\n" +
                        "                                </l7:Property>\n" +
                        "                            </l7:Properties>\n" +
                        "                        </l7:predicate>\n" +
                        "                    </l7:predicates>\n" +
                        "                </l7:permission>\n" +
                        "                <l7:permission id=\"0000000000000000ffffffffffffffff\" version=\"0\">\n" +
                        "                    <l7:operationType>DELETE</l7:operationType>\n" +
                        "                    <l7:entityType>EMAIL_LISTENER</l7:entityType>\n" +
                        "                    <l7:predicates>\n" +
                        "                        <l7:predicate>\n" +
                        "                            <l7:type>SecurityZonePredicate</l7:type>\n" +
                        "                            <l7:Properties>\n" +
                        "                                <l7:Property key=\"securityZoneId\">\n" +
                        "                                    <l7:StringValue>"+new Goid(0,1)+"</l7:StringValue>\n" +
                        "                                </l7:Property>\n" +
                        "                            </l7:Properties>\n" +
                        "                        </l7:predicate>\n" +
                        "                    </l7:predicates>\n" +
                        "                </l7:permission>\n" +
                        "                <l7:permission id=\"0000000000000000ffffffffffffffff\" version=\"0\">\n" +
                        "                    <l7:operationType>READ</l7:operationType>\n" +
                        "                    <l7:entityType>FOLDER</l7:entityType>\n" +
                        "                    <l7:predicates>\n" +
                        "                        <l7:predicate>\n" +
                        "                            <l7:type>EntityFolderAncestryPredicate</l7:type>\n" +
                        "                            <l7:Properties>\n" +
                        "                                <l7:Property key=\"entityId\">\n" +
                        "                                    <l7:StringValue>"+new Goid(0,1)+"</l7:StringValue>\n" +
                        "                                </l7:Property>\n" +
                        "                                <l7:Property key=\"entityType\">\n" +
                        "                                    <l7:StringValue>JDBC_CONNECTION</l7:StringValue>\n" +
                        "                                </l7:Property>\n" +
                        "                            </l7:Properties>\n" +
                        "                        </l7:predicate>\n" +
                        "                    </l7:predicates>\n" +
                        "                </l7:permission>\n" +
                        "                <l7:permission id=\"0000000000000000ffffffffffffffff\" version=\"0\">\n" +
                        "                    <l7:operationType>CREATE</l7:operationType>\n" +
                        "                    <l7:entityType>ANY</l7:entityType>\n" +
                        "                    <l7:predicates>\n" +
                        "                        <l7:predicate>\n" +
                        "                            <l7:type>AttributePredicate</l7:type>\n" +
                        "                            <l7:Properties>\n" +
                        "                                <l7:Property key=\"value\">\n" +
                        "                                    <l7:StringValue>test</l7:StringValue>\n" +
                        "                                </l7:Property>\n" +
                        "                                <l7:Property key=\"attribute\">\n" +
                        "                                    <l7:StringValue>name</l7:StringValue>\n" +
                        "                                </l7:Property>\n" +
                        "                                <l7:Property key=\"mode\">\n" +
                        "                                    <l7:Value>\n" +
                        "                                    <l7:Item\n" +
                        "                                    xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xsi:nil=\"true\"/>\n" +
                        "                                    </l7:Value>\n" +
                        "                                </l7:Property>\n" +
                        "                            </l7:Properties>\n" +
                        "                        </l7:predicate>\n" +
                        "                    </l7:predicates>\n" +
                        "                </l7:permission>\n" +
                        "            </l7:permissions>\n" +
                        "            <l7:assignments>\n" +
                        "                <l7:assignment>\n" +
                        "                    <l7:providerId>"+new Goid(0, -2L)+"</l7:providerId>\n" +
                        "                    <l7:identityId>00000000000000000000000000000001</l7:identityId>\n" +
                        "                    <l7:entityType>User</l7:entityType>\n" +
                        "                </l7:assignment>\n" +
                        "            </l7:assignments>\n" +
                        "        </l7:Role>";

        doCreate( "http://ns.l7tech.com/2010/04/gateway-management/roles", payload, new Goid(0,3).toString() );
    }

    @Test
    public void testDeleteRole() throws Exception {
        String message = "<s:Envelope xmlns:s=\"http://www.w3.org/2003/05/soap-envelope\" xmlns:wsa=\"http://schemas.xmlsoap.org/ws/2004/08/addressing\" xmlns:wsman=\"http://schemas.dmtf.org/wbem/wsman/1/wsman.xsd\"><s:Header><wsa:Action s:mustUnderstand=\"true\">http://schemas.xmlsoap.org/ws/2004/09/transfer/Delete</wsa:Action><wsa:To s:mustUnderstand=\"true\">http://127.0.0.1:8080/wsman</wsa:To><wsman:ResourceURI s:mustUnderstand=\"true\">http://ns.l7tech.com/2010/04/gateway-management/roles</wsman:ResourceURI><wsa:MessageID s:mustUnderstand=\"true\">uuid:b2794ffb-7d39-1d39-8002-481688002100</wsa:MessageID><wsa:ReplyTo><wsa:Address>http://schemas.xmlsoap.org/ws/2004/08/addressing/role/anonymous</wsa:Address></wsa:ReplyTo>" +
                "<wsman:SelectorSet><wsman:Selector Name=\"id\">"+new Goid(0,2).toHexString()+"</wsman:Selector></wsman:SelectorSet></s:Header><s:Body/></s:Envelope>";

        final Document result = processRequest( "http://schemas.xmlsoap.org/ws/2004/09/transfer/Delete", message );

        final Element soapBody = SoapUtil.getBodyElement(result);
        assertNotNull("SOAP Body", soapBody);
        assertNull("No body content", soapBody.getFirstChild());
    }

    @Test
    public void testDeleteGatewayRole() throws Exception {
        String message = "<s:Envelope xmlns:s=\"http://www.w3.org/2003/05/soap-envelope\" xmlns:wsa=\"http://schemas.xmlsoap.org/ws/2004/08/addressing\" xmlns:wsman=\"http://schemas.dmtf.org/wbem/wsman/1/wsman.xsd\"><s:Header><wsa:Action s:mustUnderstand=\"true\">http://schemas.xmlsoap.org/ws/2004/09/transfer/Delete</wsa:Action><wsa:To s:mustUnderstand=\"true\">http://127.0.0.1:8080/wsman</wsa:To><wsman:ResourceURI s:mustUnderstand=\"true\">http://ns.l7tech.com/2010/04/gateway-management/roles</wsman:ResourceURI><wsa:MessageID s:mustUnderstand=\"true\">uuid:b2794ffb-7d39-1d39-8002-481688002100</wsa:MessageID><wsa:ReplyTo><wsa:Address>http://schemas.xmlsoap.org/ws/2004/08/addressing/role/anonymous</wsa:Address></wsa:ReplyTo>" +
                "<wsman:SelectorSet><wsman:Selector Name=\"id\">"+new Goid(0,1).toHexString()+"</wsman:Selector></wsman:SelectorSet></s:Header><s:Body/></s:Envelope>";

        final Document result = processRequest( "http://schemas.xmlsoap.org/ws/2004/09/transfer/Delete", message );

        final Element soapBody = SoapUtil.getBodyElement(result);
        Element faultElement = XmlUtil.findExactlyOneChildElementByName(soapBody, NS_SOAP_ENVELOPE, "Fault");
        Element detailElement = XmlUtil.findExactlyOneChildElementByName(faultElement, NS_SOAP_ENVELOPE, "Detail");

        assertTrue( "the error message is incorrect", XmlUtil.getTextValue(XmlUtil.findExactlyOneChildElementByName(detailElement, NS_SOAP_ENVELOPE, "Text")).contains( "Cannot delete gateway managed role." ));

    }

    @Test
    public void testGetRole() throws Exception {
        final String message =
                "<s:Envelope xmlns:s=\"http://www.w3.org/2003/05/soap-envelope\" \n" +
                        "            xmlns:a=\"http://schemas.xmlsoap.org/ws/2004/08/addressing\" \n" +
                        "            xmlns:w=\"http://schemas.dmtf.org/wbem/wsman/1/wsman.xsd\">\n" +
                        "  <s:Header>\n" +
                        "    <a:MessageID>uuid:4ED2993C-4339-4E99-81FC-C2FD3812781A</a:MessageID> \n" +
                        "    <a:To>http://127.0.0.1:8080/wsman</a:To> \n" +
                        "    <a:ReplyTo> \n" +
                        "      <a:Address s:mustUnderstand=\"true\">http://schemas.xmlsoap.org/ws/2004/08/addressing/role/anonymous</a:Address> \n" +
                        "    </a:ReplyTo> \n" +
                        "    <a:Action s:mustUnderstand=\"true\">http://schemas.xmlsoap.org/ws/2004/09/transfer/Get</a:Action> \n" +
                        "    <w:ResourceURI s:mustUnderstand=\"true\">http://ns.l7tech.com/2010/04/gateway-management/roles</w:ResourceURI> \n" +
                        "    <w:SelectorSet>\n" +
                        "      <w:Selector Name=\"id\">"+new Goid(0,1).toHexString()+"</w:Selector> \n" +
                        "    </w:SelectorSet>\n" +
                        "    <w:OperationTimeout>PT60.000S</w:OperationTimeout> \n" +
                        "  </s:Header>\n" +
                        "  <s:Body/> \n" +
                        "</s:Envelope>";

        final Document result = processRequest("http://schemas.xmlsoap.org/ws/2004/09/transfer/Get", message);

        final Element soapBody = SoapUtil.getBodyElement(result);
        final Element roleElm = XmlUtil.findExactlyOneChildElementByName(soapBody, NS_GATEWAY_MANAGEMENT, "Role");
        final Element nameElm = XmlUtil.findExactlyOneChildElementByName(roleElm, NS_GATEWAY_MANAGEMENT, "name");

        assertEquals("Name", "My Role", DomUtils.getTextValue(nameElm));
    }

    @Test
    public void testPutRole() throws Exception {
        final String message = "<s:Envelope xmlns:s=\"http://www.w3.org/2003/05/soap-envelope\" xmlns:wsa=\"http://schemas.xmlsoap.org/ws/2004/08/addressing\" xmlns:wsman=\"http://schemas.dmtf.org/wbem/wsman/1/wsman.xsd\" xmlns:l7=\"http://ns.l7tech.com/2010/04/gateway-management\"><s:Header><wsa:Action s:mustUnderstand=\"true\">http://schemas.xmlsoap.org/ws/2004/09/transfer/Put</wsa:Action><wsa:To s:mustUnderstand=\"true\">http://127.0.0.1:8080/wsman</wsa:To><wsman:ResourceURI s:mustUnderstand=\"true\">http://ns.l7tech.com/2010/04/gateway-management/roles</wsman:ResourceURI><wsa:MessageID s:mustUnderstand=\"true\">uuid:afad2993-7d39-1d39-8002-481688002100</wsa:MessageID><wsa:ReplyTo><wsa:Address>http://schemas.xmlsoap.org/ws/2004/08/addressing/role/anonymous</wsa:Address></wsa:ReplyTo><wsman:SelectorSet><wsman:Selector Name=\"id\">"+new Goid(0,2).toHexString()+"</wsman:Selector></wsman:SelectorSet><wsman:RequestEPR/></s:Header><s:Body> "+
                "            <l7:Role id=\"00000000000000000000000000000002\" version=\"0\">\n" +
                "            <l7:name>My custom updated Role</l7:name>\n" +
                "            <l7:description>Test Custom Description</l7:description>\n" +
                "            <l7:userCreated>true</l7:userCreated>\n" +
                "            <l7:permissions/>\n" +
                "            <l7:assignments/>\n" +
                "        </l7:Role>"
                +"  </s:Body></s:Envelope>";

        final UnaryVoidThrows<Document,Exception> verifier = new UnaryVoidThrows<Document,Exception>(){
            @Override
            public void call( final Document result ) throws Exception {
                final Element soapBody = SoapUtil.getBodyElement(result);
                final Element roleElm = XmlUtil.findExactlyOneChildElementByName(soapBody, NS_GATEWAY_MANAGEMENT, "Role");
                final Element nameElm = XmlUtil.findExactlyOneChildElementByName(roleElm, NS_GATEWAY_MANAGEMENT, "name");

                assertEquals("Name", "My custom updated Role", DomUtils.getTextValue(nameElm));

            }
        };

        putAndVerify( message, verifier, false );
        putAndVerify( message, verifier, true );
    }

    @Test
    public void testPutGatewayRole() throws Exception {
        final String message = "<s:Envelope xmlns:s=\"http://www.w3.org/2003/05/soap-envelope\" xmlns:wsa=\"http://schemas.xmlsoap.org/ws/2004/08/addressing\" xmlns:wsman=\"http://schemas.dmtf.org/wbem/wsman/1/wsman.xsd\" xmlns:l7=\"http://ns.l7tech.com/2010/04/gateway-management\"><s:Header><wsa:Action s:mustUnderstand=\"true\">http://schemas.xmlsoap.org/ws/2004/09/transfer/Put</wsa:Action><wsa:To s:mustUnderstand=\"true\">http://127.0.0.1:8080/wsman</wsa:To><wsman:ResourceURI s:mustUnderstand=\"true\">http://ns.l7tech.com/2010/04/gateway-management/roles</wsman:ResourceURI><wsa:MessageID s:mustUnderstand=\"true\">uuid:afad2993-7d39-1d39-8002-481688002100</wsa:MessageID><wsa:ReplyTo><wsa:Address>http://schemas.xmlsoap.org/ws/2004/08/addressing/role/anonymous</wsa:Address></wsa:ReplyTo><wsman:SelectorSet><wsman:Selector Name=\"id\">"+new Goid(0,1).toHexString()+"</wsman:Selector></wsman:SelectorSet><wsman:RequestEPR/></s:Header><s:Body> "+
                "            <l7:Role id=\"00000000000000000000000000000001\" version=\"0\">\n" +
                "            <l7:name>My updated Role</l7:name>\n" +
                "            <l7:description>Test Custom Description</l7:description>\n" +
                "            <l7:userCreated>true</l7:userCreated>\n" +
                "            <l7:permissions/>\n" +
                "            <l7:assignments/>\n" +
                "        </l7:Role>"
                +"  </s:Body></s:Envelope>";

        final Document result = processRequest("http://schemas.xmlsoap.org/ws/2004/09/transfer/Put", message);

        final Element soapBody = SoapUtil.getBodyElement(result);
        Element faultElement = XmlUtil.findExactlyOneChildElementByName(soapBody, NS_SOAP_ENVELOPE, "Fault");
        Element detailElement = XmlUtil.findExactlyOneChildElementByName(faultElement, NS_SOAP_ENVELOPE, "Detail");

        assertTrue("the error message is incorrect", XmlUtil.getTextValue(XmlUtil.findExactlyOneChildElementByName(detailElement, NS_SOAP_ENVELOPE, "Text")).contains("Cannot update gateway managed role."));

    }

    @BugId("SSG-9000")
    @Test
    public void testCreateGatewayRoleInvalidEntityType() throws Exception {
        final String message = "<s:Envelope xmlns:s=\"http://www.w3.org/2003/05/soap-envelope\" xmlns:wsa=\"http://schemas.xmlsoap.org/ws/2004/08/addressing\" xmlns:wsman=\"http://schemas.dmtf.org/wbem/wsman/1/wsman.xsd\" xmlns:l7=\"http://ns.l7tech.com/2010/04/gateway-management\"><s:Header><wsa:Action s:mustUnderstand=\"true\">http://schemas.xmlsoap.org/ws/2004/09/transfer/Create</wsa:Action><wsa:To s:mustUnderstand=\"true\">http://127.0.0.1:8080/wsman</wsa:To><wsman:ResourceURI s:mustUnderstand=\"true\">http://ns.l7tech.com/2010/04/gateway-management/roles</wsman:ResourceURI><wsa:MessageID s:mustUnderstand=\"true\">uuid:afad2993-7d39-1d39-8002-481688002100</wsa:MessageID><wsa:ReplyTo><wsa:Address>http://schemas.xmlsoap.org/ws/2004/08/addressing/role/anonymous</wsa:Address></wsa:ReplyTo><wsman:RequestEPR/></s:Header><s:Body> "+
                "            <l7:Role>\n" +
                "            <l7:name>My Invalid Created Role</l7:name>\n" +
                "            <l7:userCreated>true</l7:userCreated>\n" +
                "            <l7:permissions>\n" +
                "                <l7:permission>\n" +
                "                    <l7:operationType>CREATE</l7:operationType>\n" +
                "                    <l7:entityType>INVALID</l7:entityType>\n" +
                "                </l7:permission>\n" +
                "                <l7:permission>\n" +
                "                    <l7:operationType>READ</l7:operationType>\n" +
                "                    <l7:entityType>INVALID</l7:entityType>\n" +
                "                </l7:permission>\n" +
                "                <l7:permission>\n" +
                "                    <l7:operationType>UPDATE</l7:operationType>\n" +
                "                    <l7:entityType>INVALID</l7:entityType>\n" +
                "                </l7:permission>\n" +
                "                <l7:permission>\n" +
                "                    <l7:operationType>DELETE</l7:operationType>\n" +
                "                    <l7:entityType>INVALID</l7:entityType>\n" +
                "                </l7:permission>\n" +
                "            </l7:permissions>" +
                "            <l7:assignments/>\n" +
                "        </l7:Role>"
                +"  </s:Body></s:Envelope>";

        final Document result = processRequest("http://schemas.xmlsoap.org/ws/2004/09/transfer/Create", message);

        final Element soapBody = SoapUtil.getBodyElement(result);
        Element faultElement = XmlUtil.findExactlyOneChildElementByName(soapBody, NS_SOAP_ENVELOPE, "Fault");
        Element codeElement = XmlUtil.findExactlyOneChildElementByName(faultElement, NS_SOAP_ENVELOPE, "Code");
        Element subcodeElement = XmlUtil.findExactlyOneChildElementByName(codeElement, NS_SOAP_ENVELOPE, "Subcode");
        Element detailElement = XmlUtil.findExactlyOneChildElementByName(faultElement, NS_SOAP_ENVELOPE, "Detail");

        assertTrue("the error message is incorrect", XmlUtil.getTextValue(XmlUtil.findExactlyOneChildElementByName(subcodeElement, NS_SOAP_ENVELOPE, "Value")).contains("InvalidRepresentation"));
        assertTrue("the error message is incorrect", XmlUtil.getTextValue(XmlUtil.findExactlyOneChildElementByName(detailElement, NS_SOAP_ENVELOPE, "Text")).contains("Unknown entity type: INVALID"));

    }

    @Test
    public void testAddAssignmentRole() throws Exception {
        final String message =
                "<env:Envelope xmlns:env=\"http://www.w3.org/2003/05/soap-envelope\"\n" +
                        "    xmlns:wsa=\"http://schemas.xmlsoap.org/ws/2004/08/addressing\"\n" +
                        "    xmlns:wse=\"http://schemas.xmlsoap.org/ws/2004/08/eventing\"\n" +
                        "    xmlns:wsen=\"http://schemas.xmlsoap.org/ws/2004/09/enumeration\"\n" +
                        "    xmlns:wsman=\"http://schemas.dmtf.org/wbem/wsman/1/wsman.xsd\"\n" +
                        "    xmlns:wxf=\"http://schemas.xmlsoap.org/ws/2004/09/transfer\" xmlns:xs=\"http://www.w3.org/2001/XMLSchema\">\n" +
                        "    <env:Header>\n" +
                        "        <wsa:Action env:mustUnderstand=\"true\">http://ns.l7tech.com/2010/04/gateway-management/roles/AddAssignments</wsa:Action>\n" +
                        "        <wsa:ReplyTo>\n" +
                        "            <wsa:Address env:mustUnderstand=\"true\">http://schemas.xmlsoap.org/ws/2004/08/addressing/role/anonymous</wsa:Address>\n" +
                        "        </wsa:ReplyTo>\n" +
                        "        <wsa:MessageID env:mustUnderstand=\"true\">uuid:d9395adf-d7e7-49f3-864b-9bc570827e0b</wsa:MessageID>\n" +
                        "        <wsa:To env:mustUnderstand=\"true\">http://halibut-api1.l7tech.com:8080/wsman</wsa:To>\n" +
                        "        <wsman:ResourceURI>http://ns.l7tech.com/2010/04/gateway-management/roles</wsman:ResourceURI>\n" +
                        "        <wsman:OperationTimeout>P0Y0M0DT0H5M0.000S</wsman:OperationTimeout>\n" +
                        "   \t\t<wsman:SelectorSet>\n" +
                        "\t\t\t<wsman:Selector Name=\"id\">"+new Goid(0,1)+"</wsman:Selector> \n" +
                        "\t\t</wsman:SelectorSet>\n" +
                        "    </env:Header>\n" +
                        "    <env:Body>\n" +
                        "        <l7:AddAssignmentsContext xmlns:l7=\"http://ns.l7tech.com/2010/04/gateway-management\">\n" +
                        "\t\t<l7:assignments>\n" +
                        "            <l7:assignment>\n" +
                        "               <l7:providerId>0000000000000000fffffffffffffffe</l7:providerId>\n" +
                        "               <l7:identityId>00000000000000000000000000000003</l7:identityId>\n" +
                        "               <l7:entityType>User</l7:entityType>\n" +
                        "            </l7:assignment>\n" +
                        "            <l7:assignment>\n" +
                        "               <l7:providerId>0000000000000000fffffffffffffffe</l7:providerId>\n" +
                        "               <l7:identityId>00000000000000000000000000000004</l7:identityId>\n" +
                        "               <l7:entityType>Group</l7:entityType>\n" +
                        "            </l7:assignment>\n" +
                        "\t\t</l7:assignments>\n" +
                        "        </l7:AddAssignmentsContext>\n" +
                        "    </env:Body>\n" +
                        "</env:Envelope>";

        final Document result = processRequest("http://schemas.xmlsoap.org/ws/2004/09/transfer/Get", message);

        final Element soapBody = SoapUtil.getBodyElement(result);
        assertNotNull("SOAP Body", soapBody);
        assertNull("No body content", soapBody.getFirstChild());
    }

    @Test
    public void testRemoveAssignmentRole() throws Exception {
        final String message =
                "<env:Envelope xmlns:env=\"http://www.w3.org/2003/05/soap-envelope\"\n" +
                        "    xmlns:wsa=\"http://schemas.xmlsoap.org/ws/2004/08/addressing\"\n" +
                        "    xmlns:wse=\"http://schemas.xmlsoap.org/ws/2004/08/eventing\"\n" +
                        "    xmlns:wsen=\"http://schemas.xmlsoap.org/ws/2004/09/enumeration\"\n" +
                        "    xmlns:wsman=\"http://schemas.dmtf.org/wbem/wsman/1/wsman.xsd\"\n" +
                        "    xmlns:wxf=\"http://schemas.xmlsoap.org/ws/2004/09/transfer\" xmlns:xs=\"http://www.w3.org/2001/XMLSchema\">\n" +
                        "    <env:Header>\n" +
                        "        <wsa:Action env:mustUnderstand=\"true\">http://ns.l7tech.com/2010/04/gateway-management/roles/RemoveAssignments</wsa:Action>\n" +
                        "        <wsa:ReplyTo>\n" +
                        "            <wsa:Address env:mustUnderstand=\"true\">http://schemas.xmlsoap.org/ws/2004/08/addressing/role/anonymous</wsa:Address>\n" +
                        "        </wsa:ReplyTo>\n" +
                        "        <wsa:MessageID env:mustUnderstand=\"true\">uuid:d9395adf-d7e7-49f3-864b-9bc570827e0b</wsa:MessageID>\n" +
                        "        <wsa:To env:mustUnderstand=\"true\">http://halibut-api1.l7tech.com:8080/wsman</wsa:To>\n" +
                        "        <wsman:ResourceURI>http://ns.l7tech.com/2010/04/gateway-management/roles</wsman:ResourceURI>\n" +
                        "        <wsman:OperationTimeout>P0Y0M0DT0H5M0.000S</wsman:OperationTimeout>\n" +
                        "   \t\t<wsman:SelectorSet>\n" +
                        "\t\t\t<wsman:Selector Name=\"id\">"+new Goid(0,1)+"</wsman:Selector> \n" +
                        "\t\t</wsman:SelectorSet>\n" +
                        "    </env:Header>\n" +
                        "    <env:Body>\n" +
                        "        <l7:RemoveAssignmentsContext xmlns:l7=\"http://ns.l7tech.com/2010/04/gateway-management\">\n" +
                        "\t\t<l7:assignments>\n" +
                        "\t\t\t<l7:assignmentId>"+new Goid(0,1)+"</l7:assignmentId>\n" +
                        "\t\t\t<l7:assignmentId>"+new Goid(0,2)+"</l7:assignmentId>\n" +
                        "\t\t</l7:assignments>\n" +
                        "        </l7:RemoveAssignmentsContext>\n" +
                        "    </env:Body>\n" +
                        "</env:Envelope>\n";

        final Document result = processRequest("http://schemas.xmlsoap.org/ws/2004/09/transfer/Get", message);

        final Element soapBody = SoapUtil.getBodyElement(result);
        assertNotNull("SOAP Body", soapBody);
        assertNull("No body content", soapBody.getFirstChild());
    }

    //- PRIVATE

    private final GenericApplicationContext applicationContext = new GenericApplicationContext();
    private ServerGatewayManagementAssertion managementAssertion;
    private static final PolicyValidatorStub policyValidator = new PolicyValidatorStub();
    private RbacServicesStub rbacService;
    @Mock
    private IdentityProviderFactory identityProviderFactory;
    private static final String NS_WS_TRANSFER = "http://schemas.xmlsoap.org/ws/2004/09/transfer";
    private static final String NS_WS_ADDRESSING = "http://schemas.xmlsoap.org/ws/2004/08/addressing";
    private static final String NS_WS_ENUMERATION = "http://schemas.xmlsoap.org/ws/2004/09/enumeration";
    private static final String NS_WS_MANAGEMENT = "http://schemas.dmtf.org/wbem/wsman/1/wsman.xsd";
    private static final String NS_WS_MANAGEMENT_IDENTITY = "http://schemas.dmtf.org/wbem/wsman/identity/1/wsmanidentity.xsd";
    private static final String NS_GATEWAY_MANAGEMENT = "http://ns.l7tech.com/2010/04/gateway-management";
    private static final String NS_SOAP_ENVELOPE = "http://www.w3.org/2003/05/soap-envelope";
    private static final String WSDL = "<wsdl:definitions xmlns:wsdl=\"http://schemas.xmlsoap.org/wsdl/\" targetNamespace=\"http://warehouse.acme.com/ws\"/>";
    private static final String POLICY = "<wsp:Policy xmlns:L7p=\"http://www.layer7tech.com/ws/policy\" xmlns:wsp=\"http://schemas.xmlsoap.org/ws/2002/12/policy\"><wsp:All wsp:Usage=\"Required\"><L7p:AuditAssertion/></wsp:All></wsp:Policy>";

    private static final String[] RESOURCE_URIS = new String[]{
        "http://ns.l7tech.com/2010/04/gateway-management/clusterProperties",
        "http://ns.l7tech.com/2010/04/gateway-management/folders",
        "http://ns.l7tech.com/2010/04/gateway-management/identityProviders",
        "http://ns.l7tech.com/2010/04/gateway-management/jdbcConnections",
        "http://ns.l7tech.com/2010/04/gateway-management/jmsDestinations",
        "http://ns.l7tech.com/2010/04/gateway-management/policies",
        "http://ns.l7tech.com/2010/04/gateway-management/privateKeys",
        "http://ns.l7tech.com/2010/04/gateway-management/resources",
        "http://ns.l7tech.com/2010/04/gateway-management/services",
        "http://ns.l7tech.com/2010/04/gateway-management/storedPasswords",
        "http://ns.l7tech.com/2010/04/gateway-management/trustedCertificates",
        "http://ns.l7tech.com/2010/04/gateway-management/encapsulatedAssertions",
        "http://ns.l7tech.com/2010/04/gateway-management/customKeyValues",
        "http://ns.l7tech.com/2010/04/gateway-management/httpConfigurations",
        "http://ns.l7tech.com/2010/04/gateway-management/roles"
    };

    Folder testFolder;
    @Before
    @SuppressWarnings({"serial"})
    public void init() throws Exception {
        new AssertionRegistry(); // causes type mappings to be installed for assertions
        final Folder rootFolder = folder( new Goid(0,-5002L), null, "Root Node");
        testFolder = folder( new Goid(0,1L), rootFolder, "Test Folder");
        final Folder emptyFolder = folder( new Goid(0,3L), rootFolder, "Empty Folder");
        final ClusterPropertyManager clusterPropertyManager = new MockClusterPropertyManager(
                prop( new Goid(0,1), "testProp1", "testValue1"),
                prop( new Goid(0,2), "testProp2", "testValue2"),
                prop( new Goid(0,3), "testProp3", "testValue3"),
                prop( new Goid(0,4), "interfaceTags", "localhost(127.0.0.1)"),
                prop( new Goid(0,5), "keyStore.defaultSsl.alias", "0:bob" ) );
        Config serverConfig = new MockConfig( new Properties() );
        applicationContext.getBeanFactory().registerSingleton( "serverConfig", serverConfig );
        final TestTrustedCertManager testTrustedCertManager = new TestTrustedCertManager(
                cert(new Goid(0, 1L), "Alice", TestDocuments.getWssInteropAliceOldCert()),
                cert(new Goid(0, 2L), "Bob", TestDocuments.getWssInteropBobOldCert()));
        applicationContext.getBeanFactory().registerSingleton( "trustedCertManager", testTrustedCertManager);
        applicationContext.getBeanFactory().registerSingleton( "clusterPropertyCache", new ClusterPropertyCache(){{ setClusterPropertyManager( clusterPropertyManager ); }});
        applicationContext.getBeanFactory().registerSingleton( "clusterPropertyManager", clusterPropertyManager);
        applicationContext.getBeanFactory().registerSingleton( "resourceEntryManager", new ResourceEntryManagerStub(
                resource( new Goid(0,1L),"books.xsd", ResourceType.XML_SCHEMA, "urn:books", "<xs:schema xmlns:xs=\"http://www.w3.org/2001/XMLSchema\"><xs:element name=\"book\" type=\"xs:string\"/></xs:schema>", null),
                resource( new Goid(0,2L),"books_refd.xsd", ResourceType.XML_SCHEMA, "urn:booksr", "<xs:schema xmlns:xs=\"http://www.w3.org/2001/XMLSchema\"><xs:element name=\"book\" type=\"xs:string\"/></xs:schema>", "The booksr schema."),
                resource( new Goid(0,3L),"books.dtd", ResourceType.DTD, "books", "<!ELEMENT book ANY>", "The books DTD.")) );
        final FolderManagerStub folderManagerStub = new FolderManagerStub(
                rootFolder,
                testFolder,
                folder(new Goid(0, 2L), testFolder, "Nested Test Folder"),
                emptyFolder);
        applicationContext.getBeanFactory().registerSingleton( "folderManager", folderManagerStub);
        final IdentityProviderConfig identityProviderConfig = provider(new Goid(0, -2L), IdentityProviderType.INTERNAL, "Internal Identity Provider");
        final TestIdentityProviderConfigManager testIdentityProviderConfigManager = new TestIdentityProviderConfigManager(
                identityProviderConfig,
                provider(new Goid(0, -3L), IdentityProviderType.LDAP, "LDAP", "userLookupByCertMode", "CERT"));
        applicationContext.getBeanFactory().registerSingleton( "identityProviderConfigManager", testIdentityProviderConfigManager);
        final TestIdentityProvider testIdentityProvider = new TestIdentityProvider(identityProviderConfig);
        TestIdentityProvider.addUser(new UserBean(new Goid(0, -2L), new Goid(0,1).toString()), new Goid(0,1).toString(), "password".toCharArray());
        final GroupBean gb1 = new GroupBean(new Goid(0, -2L), new Goid(0, 2).toString());
        gb1.setUniqueIdentifier(new Goid(0, 2).toString());
        TestIdentityProvider.addGroup(gb1);
        TestIdentityProvider.addUser(new UserBean(new Goid(0, -2L), new Goid(0,3).toString()), new Goid(0,3).toString(), "password".toCharArray());
        final GroupBean gb2 = new GroupBean(new Goid(0, -2L), new Goid(0, 4).toString());
        gb2.setUniqueIdentifier(new Goid(0, 4).toString());
        TestIdentityProvider.addGroup(gb2);
        Mockito.when(identityProviderFactory.getProvider(Matchers.eq(new Goid(0, -2L)))).thenReturn(testIdentityProvider);
        applicationContext.getBeanFactory().registerSingleton("identityProviderFactory", identityProviderFactory);
        applicationContext.getBeanFactory().registerSingleton( "ssgConnectorManager", new SsgConnectorManagerStub());
        applicationContext.getBeanFactory().registerSingleton( "applicationEventProxy", new ApplicationEventProxy());
        applicationContext.getBeanFactory().registerSingleton( "defaultKey",new TestDefaultKey());
        applicationContext.getBeanFactory().registerSingleton( "jmsConnectionManager",  new JmsConnectionManagerStub(
                jmsConnection( 1L, "Test Endpoint", "com.context.Classname", "qcf", "ldap://jndi", null),
                jmsConnection( 2L, "Test Endpoint 2", "com.context.Classname", "qcf 2", "ldap://jndi2", JmsProviderType.Weblogic)));
        applicationContext.getBeanFactory().registerSingleton( "jmsEndpointManager",  new JmsEndpointManagerStub(
                jmsEndpoint( 1L, 1L, "Test Endpoint"),
                jmsEndpoint( 2L, 2L, "Test Endpoint 2")));
        final JdbcConnectionManagerStub jdbcConnectionManagerStub = new JdbcConnectionManagerStub(
                connection(new Goid(0, 1), "A Test Connection"),
                connection(new Goid(0, 2), "Test Connection"));
        applicationContext.getBeanFactory().registerSingleton( "jdbcConnectionManager", jdbcConnectionManagerStub);
        applicationContext.getBeanFactory().registerSingleton( "policyExporterImporterManager", new PolicyExporterImporterManagerStub() );
        final Policy testPolicy1 = policy(new Goid(0,1L), PolicyType.INCLUDE_FRAGMENT, "Test Policy", true, POLICY);
        applicationContext.getBeanFactory().registerSingleton( "policyManager",  new PolicyManagerStub(
                testPolicy1,
                policy( new Goid(0,2L), PolicyType.INCLUDE_FRAGMENT, "Test Policy For Move", true, POLICY),
                policy( new Goid(0,3L), PolicyType.INCLUDE_FRAGMENT, "Test Policy For Encass Import", true, POLICY)));
        applicationContext.getBeanFactory().registerSingleton("ssgKeyStoreManager", new SsgKeyStoreManagerStub(new SsgKeyFinderStub(Arrays.asList(
                key(new Goid(0, 0), "bob", TestDocuments.getWssInteropBobOldCert(), TestDocuments.getWssInteropBobOldKey())))));
        rbacService = mock( RbacServicesStub.class);
        when(rbacService.isPermittedForAnyEntityOfType(any(User.class),any(OperationType.class),any(EntityType.class))).thenReturn(true);
        when(rbacService.isPermittedForEntitiesOfTypes(any(User.class), any(OperationType.class), anySetOf(EntityType.class))).thenReturn(true);
        when(rbacService.isPermittedForEntity(any(User.class), any(Entity.class),any(OperationType.class), anyString())).thenReturn(true);
        when(rbacService.isPermittedForSomeEntityOfType(any(User.class),any(OperationType.class), any(EntityType.class))).thenReturn(true);

        applicationContext.getBeanFactory().registerSingleton("rbacServices", rbacService);
        applicationContext.getBeanFactory().registerSingleton( "securityFilter", new RbacServicesStub() );
        applicationContext.getBeanFactory().registerSingleton( "serviceDocumentManager", new ServiceDocumentManagerStub() );
        final PublishedService testService1 = service( new Goid(0,1L), "Test Service 1", false, false, null, null);
        applicationContext.getBeanFactory().registerSingleton( "serviceManager", new MockServiceManager(
                testService1,
                service( new Goid(0,2L), "Test Service 2", false, true, "http://localhost:8080/test.wsdl", WSDL) ));
        applicationContext.getBeanFactory().registerSingleton( "policyValidator", policyValidator );
        applicationContext.getBeanFactory().registerSingleton( "uddiServiceWsdlUpdateChecker", new ServiceWsdlUpdateChecker(null, null){
            @Override
            public boolean isWsdlUpdatePermitted( final PublishedService service, final boolean resetWsdlXml ) throws UpdateException {
                return true;
            }
        } );
        applicationContext.getBeanFactory().registerSingleton( "securePasswordManager", new SecurePasswordManagerStub(
                securePassword(new Goid(0,1L), "test", "password", true, SecurePassword.SecurePasswordType.PASSWORD)
        ) );
        applicationContext.getBeanFactory().registerSingleton( "encapsulatedAssertionConfigManager", new EncapsulatedAssertionConfigManagerStub(
                encapsulatedAssertion( new Goid(0,1L), "Test Encass Config 1", "ABCD-0001", testPolicy1, null, null, null)
        ) );
        applicationContext.getBeanFactory().registerSingleton( "httpConfigurationManager", new HttpConfigurationManagerStub(httpConfiguration(new Goid(0, 1L))) );
        MockRoleManager roleManager = new MockRoleManager(null);
        roleManager.save(role());
        final Role customRole = new Role();
        customRole.setDescription("Test Custom Description");
        customRole.setName("My custom Role");
        customRole.setUserCreated(true);
        roleManager.save(customRole);
        applicationContext.getBeanFactory().registerSingleton( "roleManager", roleManager );

        applicationContext.getBeanFactory().registerSingleton("trustedEsmUserManager", new TrustedEsmUserManagerStub());
        applicationContext.getBeanFactory().registerSingleton("logonManager", new LogonInfoManagerStub());
        final TestPasswordHasher testPasswordHasher = new TestPasswordHasher();
        applicationContext.getBeanFactory().registerSingleton("passwordHasher", new TestPasswordHasher());
        final IdentityProviderPasswordPolicyManagerStub idPasswordPolicyManager = new IdentityProviderPasswordPolicyManagerStub();
        applicationContext.getBeanFactory().registerSingleton("passwordPolicyManger", idPasswordPolicyManager);
        applicationContext.getBeanFactory().registerSingleton("passwordEnforcerManager", passwordEnforcerManager(serverConfig,idPasswordPolicyManager, roleManager,testPasswordHasher));

        final Map<String,String> mqMap = new HashMap<String,String>();
        mqMap.put(SsgActiveConnector.PROPERTIES_KEY_MQ_NATIVE_HOST_NAME,"host");
        mqMap.put(SsgActiveConnector.PROPERTIES_KEY_MQ_NATIVE_PORT,"1234");
        mqMap.put(SsgActiveConnector.PROPERTIES_KEY_MQ_NATIVE_QUEUE_MANAGER_NAME,"qManager");
        mqMap.put(SsgActiveConnector.PROPERTIES_KEY_MQ_NATIVE_IS_SSL_ENABLED,"false");

        final Map<String,String> sftpMap = new HashMap<String,String>();
        sftpMap.put(SsgActiveConnector.PROPERTIES_KEY_SFTP_HOST,"host");
        sftpMap.put(SsgActiveConnector.PROPERTIES_KEY_SFTP_PORT,"1234");
        sftpMap.put(SsgActiveConnector.PROPERTIES_KEY_SFTP_DIRECTORY,"dir");
        sftpMap.put(SsgActiveConnector.PROPERTIES_KEY_SFTP_USERNAME,"user");
        sftpMap.put(SsgActiveConnector.PROPERTIES_KEY_SFTP_SECURE_PASSWORD_OID,"1234");

        applicationContext.getBeanFactory().registerSingleton( "ssgActiveConnectorManager", new SsgActiveConnectorManagerStub(
                activeConnector( new Goid(0,1L), "Test MQ Config 1", SsgActiveConnector.ACTIVE_CONNECTOR_TYPE_MQ_NATIVE,new Goid(0,1234L), mqMap),
                activeConnector( new Goid(0,2L), "Test SFTP Config 1", SsgActiveConnector.ACTIVE_CONNECTOR_TYPE_SFTP,new Goid(0,4567L), sftpMap),
                activeConnector( new Goid(0,3L), "Test SFTP Config Bad", "SFTP1", new Goid(0,1234L),sftpMap)
        ));

        final GenericEntity genericEntity = new GenericEntity();
        genericEntity.setGoid(new Goid(0,1));
        genericEntity.setName("My Test Entity");
        genericEntity.setDescription("My test entity description");
        genericEntity.setEnabled(true);
        genericEntity.setEntityClassName(this.getClass().getName());
        genericEntity.setValueXml("<xml>xml value</xml>");

        GenericEntityManagerStub genericEntityManager = new GenericEntityManagerStub(genericEntity);
        genericEntityManager.setRegistedClasses("com.l7tech.external.assertions.gatewaymanagement.server.ServerGatewayManagementAssertionTest");
        applicationContext.getBeanFactory().registerSingleton("genericEntityManager",genericEntityManager );

        applicationContext.getBeanFactory().registerSingleton("customKeyValueStoreManager", new CustomKeyValueStoreManagerStub(
            customKeyValue(new Goid(0,1L), "key.prefix.key1", "<xml>Test value</xml>".getBytes("UTF-8"))
        ) );

        final PolicyVersion policyVersion1 = new PolicyVersion();
        policyVersion1.setGoid(new Goid(0,1));
        policyVersion1.setPolicyGoid(new Goid(0,1l));
        policyVersion1.setOrdinal(1);
        policyVersion1.setActive(true);
        policyVersion1.setName("Comment");
        final PolicyVersion serviceVersion1 = new PolicyVersion();
        serviceVersion1.setGoid(new Goid(0,2));
        serviceVersion1.setPolicyGoid(new Goid(1,1l));
        serviceVersion1.setOrdinal(1);
        serviceVersion1.setActive(true);
        serviceVersion1.setName("Comment");
        applicationContext.getBeanFactory().registerSingleton( "policyVersionManager", new PolicyVersionManagerStub(policyVersion1,serviceVersion1));

        final SecurityZone securityZone1 = new SecurityZone();
        securityZone1.setGoid(new Goid(0,1));
        securityZone1.setName("Test Security Zone 0001");
        securityZone1.setDescription("Canned Testing Security Zone 0001");
        securityZone1.getPermittedEntityTypes().add(EntityType.POLICY);
        securityZone1.getPermittedEntityTypes().add(EntityType.SERVICE);
        securityZone1.getPermittedEntityTypes().add(EntityType.SERVICE_ALIAS);
        securityZone1.getPermittedEntityTypes().add(EntityType.POLICY_ALIAS);
        securityZone1.getPermittedEntityTypes().add(EntityType.ASSERTION_ACCESS);
        securityZone1.getPermittedEntityTypes().add(EntityType.JMS_CONNECTION);
        securityZone1.getPermittedEntityTypes().add(EntityType.JMS_ENDPOINT);
        securityZone1.getPermittedEntityTypes().add(EntityType.JDBC_CONNECTION);

        final SecurityZone securityZone2 = new SecurityZone();
        securityZone2.setGoid(new Goid(0,2));
        securityZone2.setName("Test Security Zone 0002");
        securityZone2.setDescription("Canned Testing Security Zone 0002");
        securityZone2.getPermittedEntityTypes().add(EntityType.ANY);

        final SecurityZoneManagerStub securityZoneManagerStub = new SecurityZoneManagerStub(securityZone1, securityZone2);
        applicationContext.getBeanFactory().registerSingleton("securityZoneManager", securityZoneManagerStub);

        // siteminder
        applicationContext.getBeanFactory().registerSingleton("siteMinderConfigurationManager", new SiteMinderConfigurationManagerStub(
                siteminderConfiguration(new Goid(0, 1L), "Config 1","0.0.0.0","secret","localhost",3),
                siteminderConfiguration(new Goid(0, 2L), "Config 2","0.0.0.0","secret","localhost",3)));

        applicationContext.getBeanFactory().registerSingleton( "entityCrud", new EntityFinderStub(folderManagerStub, securityZoneManagerStub, jdbcConnectionManagerStub, testIdentityProviderConfigManager, testTrustedCertManager) );

        // assertion security zone
        final AssertionAccess assAccess1 = new AssertionAccess();
        assAccess1.setGoid(new Goid(0,1));
        assAccess1.setName(JmsRoutingAssertion.class.getName());
        assAccess1.setSecurityZone(securityZone1);

        final AssertionAccess assAccess2 = new AssertionAccess();
        assAccess2.setGoid(new Goid(0,2));
        assAccess2.setName(GatewayManagementAssertion.class.getName());
        assAccess2.setSecurityZone(securityZone2);
        applicationContext.getBeanFactory().registerSingleton("assertionAccessManager", new AssertionAccessManagerStub(assAccess1,assAccess2));
        final AssertionRegistryStub assertionRegistryStub = new AssertionRegistryStub();
        applicationContext.getBeanFactory().registerSingleton("assertionRegistry", assertionRegistryStub);

        applicationContext.getBeanFactory().registerSingleton( "wspReader", new WspReader(assertionRegistryStub));
        final TestLicenseManager testLicenseManager = new TestLicenseManager();
        applicationContext.getBeanFactory().registerSingleton( "licenseManager", testLicenseManager);
        final ApplicationContextInjector applicationContextInjector = new ApplicationContextInjector();
        applicationContext.getBeanFactory().registerSingleton( "injector", applicationContextInjector);
        applicationContext.getBeanFactory().registerSingleton( "policyFactory", new ServerPolicyFactory(testLicenseManager, applicationContextInjector));

        // policy alias
        final PolicyAlias pAlias1 = new PolicyAlias(testPolicy1,testFolder);
        pAlias1.setGoid(new Goid(0,1));
        applicationContext.getBeanFactory().registerSingleton("policyAliasManager", new PolicyAliasManagerStub(pAlias1));

        // service alias
        final PublishedServiceAlias sAlias1 = new PublishedServiceAlias(testService1,testFolder);
        sAlias1.setGoid(new Goid(0,1));
        applicationContext.getBeanFactory().registerSingleton("serviceAliasManager", new ServiceAliasManagerStub(sAlias1));

        // email listener
        final EmailListener emailListener = new EmailListener(EmailServerType.POP3);
        applicationContext.getBeanFactory().registerSingleton("emailListenerManager", new EmailListenerManagerStub(emailListener){});

        applicationContext.getBeanFactory().registerSingleton( "dependencyAnalyzer", new DependencyAnalyzerImpl());

        applicationContext.getBeanFactory().registerSingleton("entityBundleImporter", new EntityBundleImporterStub());
        applicationContext.getBeanFactory().registerSingleton("entityBundleExporter", new EntityBundleExporterStub());
        applicationContext.getBeanFactory().registerSingleton("clientCertManager", new ClientCertManagerStub() );

        applicationContext.getBeanFactory().registerSingleton("auditRecordManager", new AuditRecordManagerStub());
        applicationContext.getBeanFactory().registerSingleton("uddiServiceControlManager", new UDDIServiceControlManagerStub());

        applicationContext.getBeanFactory().registerSingleton("ssgFirewallRuleManager", new SsgFirewallRulesManagerStub());
        applicationContext.getBeanFactory().registerSingleton("sampleMessageManager", new SampleMessageManagerStub());
        applicationContext.getBeanFactory().registerSingleton("clusterStatusAdmin", new ClusterStatusAdminStub());
        applicationContext.getBeanFactory().registerSingleton("cassandraEntityManager", new CassandraConnectionEntityManagerStub());
        applicationContext.getBeanFactory().registerSingleton("solutionKitManager", new SolutionKitManagerStub());
        applicationContext.getBeanFactory().registerSingleton("policyBackedServiceManager", new PolicyBackedServiceManagerStub());

        applicationContext.refresh();

        final ClassLoader systemClassLoader = ClassLoader.getSystemClassLoader();
        final ResourceClassLoader resourceClassLoader = new ResourceClassLoader(
                new FilterClassLoader(systemClassLoader, systemClassLoader,
                        Arrays.asList("com.l7tech.gateway.common.service.PublishedService$DefaultWsdlStrategy"), true),
                Arrays.asList("com.l7tech.gateway.common.service.PublishedService$DefaultWsdlStrategy")){

            @Override
            public Enumeration<URL> getResources(String name) throws IOException {
                final Enumeration<URL> resources = super.getResources(name);
                final List<URL> urls = new ArrayList<URL>();
                while (resources.hasMoreElements()) {
                    final URL url = resources.nextElement();
                    if (!url.toString().contains("console")) {
                         urls.add(url);
                    }
                }

                return new Enumeration<URL>() {
                    private int index = 0;
                    @Override
                    public boolean hasMoreElements() {
                        return index != urls.size();
                    }

                    @Override
                    public URL nextElement() {
                        return urls.get(index++);
                    }
                };
            }
        };

        Thread.currentThread().setContextClassLoader(resourceClassLoader);
        managementAssertion = new ServerGatewayManagementAssertion(
                new GatewayManagementAssertion(), applicationContext, "testGatewayManagementContext.xml", false );

        GoidUpgradeMapperTestUtil.addPrefix("keystore_file", 0);

    }

    private PasswordEnforcerManager passwordEnforcerManager(Config config,IdentityProviderPasswordPolicyManager passwordPolicyManager,MockRoleManager roleManager, TestPasswordHasher passwordHasher) {
        return new PasswordEnforcerManager(config,passwordPolicyManager,roleManager,passwordHasher,
                IdentityProviderPasswordPolicyManagerStub.testPasswordPolicy,IdentityProviderPasswordPolicyManagerStub.testPasswordPolicy);
    }

    private Role role() throws SaveException {
        final Role role = new Role();
        role.setDescription("Test Description");
        role.setName("My Role");
        role.setUserCreated(false);

        final RoleAssignment userAssignment = new RoleAssignment(role, new Goid(0, -2), new Goid(0, 1).toString(), EntityType.USER);
        userAssignment.setId(new Goid(0, 1).toString());
        role.getRoleAssignments().add(userAssignment);
        final RoleAssignment groupAssignment = new RoleAssignment(role, new Goid(0, -2), new Goid(0, 2).toString(), EntityType.GROUP);
        groupAssignment.setId(new Goid(0, 2).toString());
        role.getRoleAssignments().add(groupAssignment);

        role.addAttributePermission(OperationType.CREATE, EntityType.ANY, "name", "test");
        role.addEntityFolderAncestryPermission(EntityType.JDBC_CONNECTION, new Goid(123,456));
        role.addFolderPermission(OperationType.DELETE, EntityType.SERVICE, new Folder("temp", null), false);
        role.addEntityOtherPermission(EntityType.AUDIT_MESSAGE, new Goid(57,79).toString(), "my-operation");
        SecurityZone securityZone = new SecurityZone();
        securityZone.setGoid(new Goid(123,456));
        role.addSecurityZonePermission(OperationType.DELETE, EntityType.EMAIL_LISTENER, securityZone);
        return role;
    }

    private HttpConfiguration httpConfiguration(Goid goid) {
        HttpConfiguration httpConfiguration = new HttpConfiguration();
        httpConfiguration.setGoid(goid);
        httpConfiguration.setHost("TestHost");
        return httpConfiguration;
    }

    private static TrustedCert cert( final Goid oid, final String name, final X509Certificate x509Certificate ) {
        final TrustedCert cert = new TrustedCert();
        cert.setGoid(oid);
        cert.setName( name );
        cert.setCertificate( x509Certificate );
        cert.setRevocationCheckPolicyType( TrustedCert.PolicyUsageType.USE_DEFAULT );
        return cert;
    }

    private static ClusterProperty prop( final Goid goid, final String name, final String value ) {
        final ClusterProperty prop = new ClusterProperty( name, value );
        prop.setGoid( goid );
        return prop;
    }

    private static Folder folder( final Goid goid, final Folder parent, final String name ) {
        final Folder folder = new Folder( name, parent );
        folder.setGoid(goid);
        return folder;
    }

    private static IdentityProviderConfig provider( final Goid oid, final IdentityProviderType type, final String name, String... props ) {
        final IdentityProviderConfig provider = type == IdentityProviderType.LDAP ? new LdapIdentityProviderConfig() : new IdentityProviderConfig( type );
        provider.setGoid(oid);
        provider.setName( name );
        if (props != null && props.length > 0) {
            int numprops = props.length / 2;
            if (props.length != numprops * 2)
                throw new IllegalArgumentException("An even number of strings must be provided (to be interpreted as test property name,value pairs)");

            for (int i = 0; i < props.length; i+=2) {
                String prop = props[i];
                String val = props[i + 1];
                if ("userLookupByCertMode".equals(prop)) {
                    ((LdapIdentityProviderConfig)provider).setUserLookupByCertMode(LdapIdentityProviderConfig.UserLookupByCertMode.valueOf(val));
                } else {
                    throw new IllegalArgumentException("Unsupported test idp property: " + prop);
                }
            }
        }

        if(type == IdentityProviderType.LDAP){
            LdapIdentityProviderConfig ldap = (LdapIdentityProviderConfig)provider;
            Map<String,String> ntlmProps = new TreeMap<String,String>();
            ntlmProps.put("prop","val");
            ntlmProps.put("prop1","val1");
            ldap.setNtlmAuthenticationProviderProperties(ntlmProps);
        }
        return provider;
    }

    private static JdbcConnection connection( final Goid goid, final String name ) {
        final JdbcConnection connection = new JdbcConnection();
        connection.setGoid(goid);
        connection.setName( name );

        // props must be non empty for resolution during import
        connection.setDriverClass( "d" );
        connection.setJdbcUrl( "j" );
        connection.setUserName( "u" );
        return connection;
    }

    private static JmsConnection jmsConnection(final long oid, final String name, final String contextClassname, final String queueFactory, final String jndiUrl, JmsProviderType providerType) {
        final JmsConnection connection = new JmsConnection();
        connection.setGoid(new Goid(0, oid));
        connection.setName( name );
        connection.setQueueFactoryUrl( queueFactory );
        connection.setInitialContextFactoryClassname( contextClassname );
        connection.setJndiUrl( jndiUrl );
        // Added for SSG-6372
        connection.setProviderType(providerType);
        // Added for SSG-5693
        connection.setUsername("user");
        connection.setPassword("password");
        // Added for SSG-7449
        Properties connectionProperties = connection.properties();
        connectionProperties.setProperty("java.naming.security.credentials","jndi-password");
        connectionProperties.setProperty("com.l7tech.server.jms.prop.hardwired.service.bool","false");
        connection.properties(connectionProperties);
        return connection;
    }

    private static JmsEndpoint jmsEndpoint( final long oid, final long connectionOid, final String queueName) {
        final JmsEndpoint endpoint = new JmsEndpoint();
        endpoint.setGoid(new Goid(0, oid));
        endpoint.setConnectionGoid(new Goid(0, connectionOid));
        endpoint.setName( queueName );
        endpoint.setDestinationName( queueName );
        // Added for SSG-5693
        endpoint.setUsername("user");
        endpoint.setPassword("password");
        return endpoint;
    }

    private static Policy policy( final Goid goid, final PolicyType type, final String name, final boolean soap, final String policyXml ) {
        final Policy policy = new Policy( type, name, policyXml, soap);
        policy.setGoid(goid);
        policy.setGuid(UUID.nameUUIDFromBytes(Goid.toString(goid).getBytes()).toString());
        return policy;
    }

    private static SsgKeyEntry key( final Goid keystoreGoid, final String alias, final X509Certificate cert, final PrivateKey privateKey ) {
        return new SsgKeyEntry( keystoreGoid, alias, new X509Certificate[]{cert}, privateKey);
    }

    private static ResourceEntry resource( final Goid goid, final String uri, final ResourceType type, final String key, final String content, final String desc ) {
        final ResourceEntry entry = new ResourceEntry();
        entry.setGoid(goid);
        entry.setUri( uri );
        entry.setType( type );
        entry.setContentType( type.getMimeType() );
        entry.setResourceKey1( key );
        entry.setContent( content );
        entry.setDescription( desc );
        return entry;
    }

    private static PublishedService service( final Goid goid, final String name, final boolean disabled, final boolean soap, final String wsdlUrl, final String wsdlXml ) {
        final PublishedService service = new PublishedService();
        service.setGoid(goid);
        service.setName( name );
        service.getPolicy().setGoid(new Goid(1,goid.getLow()));
        service.getPolicy().setName( "Policy for " + name + " (#" + goid + ")" );
        service.getPolicy().setGuid( UUID.randomUUID().toString() );
        service.getPolicy().setXml( POLICY );
        service.setDisabled( disabled );
        service.setSoap( soap );
        try {
            service.setWsdlUrl( wsdlUrl );
        } catch ( MalformedURLException e ) {
            throw ExceptionUtils.wrap( e );
        }
        service.setWsdlXml( wsdlXml );
        return service;
    }

    private static SsgActiveConnector activeConnector( final Goid goid,
                                                       final String name,
                                                       final String type,
                                                       final Goid hardWiredServiceGoid,
                                                       final Map<String,String> properties ){
        final SsgActiveConnector connector = new SsgActiveConnector();
        connector.setGoid(goid);
        connector.setName(name);
        connector.setType(type);
        connector.setHardwiredServiceGoid(hardWiredServiceGoid);
        for(String key: properties.keySet()){
            connector.setProperty(key,properties.get(key));
        }

        return connector;
    }

    private static SiteMinderConfiguration siteminderConfiguration(final Goid goid,
                                                                   final String name,
                                                                   final String address,
                                                                   final String secret,
                                                                   final String hostname,
                                                                   final int fipsmode)
    {
        final SiteMinderConfiguration configuration = new SiteMinderConfiguration();
        configuration.setGoid(goid);
        configuration.setName(name);
        configuration.setAddress(address);
        configuration.setSecret(secret);
        configuration.setHostname(hostname);
        configuration.setFipsmode(fipsmode);
        configuration.setProperties(new HashMap<String, String>());
        configuration.putProperty("property","value");
        return configuration;
    }

    private static EncapsulatedAssertionConfig encapsulatedAssertion(final Goid goid, final String name, final String guid, final Policy policy,
                                                                     final Set<EncapsulatedAssertionArgumentDescriptor> args,
                                                                     final Set<EncapsulatedAssertionResultDescriptor> results,
                                                                     final Map<String, String> properties)
    {
        final EncapsulatedAssertionConfig config = new EncapsulatedAssertionConfig();
        config.setGoid(goid);
        config.setName( name );
        config.setGuid( guid );
        config.setPolicy( policy );
        if ( args != null )
            config.setArgumentDescriptors( args );
        if ( results != null )
            config.setResultDescriptors( results );
        if ( properties != null )
            config.setProperties( properties );
        return config;
    }

    private static SecurePassword securePassword( final Goid goid, final String name, final String password, final boolean fromVariable, final SecurePassword.SecurePasswordType type) {
        final SecurePassword securePassword = new SecurePassword();
        securePassword.setGoid(goid);
        securePassword.setName( name );
        securePassword.setEncodedPassword( password );
        securePassword.setUsageFromVariable( fromVariable );
        securePassword.setLastUpdate( System.currentTimeMillis() );
        securePassword.setType(type);
        return securePassword;
    }

    private static CustomKeyValueStore customKeyValue(final Goid goid,
                                                      final String key,
                                                      final byte[] value) {
        final CustomKeyValueStore customKeyValueStore = new CustomKeyValueStore();
        customKeyValueStore.setGoid(goid);
        customKeyValueStore.setName(key);
        customKeyValueStore.setValue(value);

        return customKeyValueStore;
    }

    private void doCreate( final String resourceUri,
                           final String payload,
                           final String... expectedIds ) throws Exception {

        final String message = "<s:Envelope xmlns:s=\"http://www.w3.org/2003/05/soap-envelope\" xmlns:wsa=\"http://schemas.xmlsoap.org/ws/2004/08/addressing\" xmlns:wsman=\"http://schemas.dmtf.org/wbem/wsman/1/wsman.xsd\"><s:Header><wsa:Action s:mustUnderstand=\"true\">http://schemas.xmlsoap.org/ws/2004/09/transfer/Create</wsa:Action><wsa:To s:mustUnderstand=\"true\">http://127.0.0.1:8080/wsman</wsa:To><wsman:ResourceURI s:mustUnderstand=\"true\">{0}</wsman:ResourceURI><wsman:OperationTimeout>PT600.000S</wsman:OperationTimeout><wsa:MessageID s:mustUnderstand=\"true\">uuid:a711f948-7d39-1d39-8002-481688002100</wsa:MessageID><wsa:ReplyTo><wsa:Address>http://schemas.xmlsoap.org/ws/2004/08/addressing/role/anonymous</wsa:Address></wsa:ReplyTo></s:Header><s:Body>{1}</s:Body></s:Envelope>";

        final Document result = processRequest( "http://schemas.xmlsoap.org/ws/2004/09/transfer/Create", MessageFormat.format( message, resourceUri, payload ));

        final Element soapBody = SoapUtil.getBodyElement(result);
        final Element transferResponse = XmlUtil.findExactlyOneChildElementByName(soapBody, NS_WS_TRANSFER, "ResourceCreated");
        final Element refParameters = XmlUtil.findExactlyOneChildElementByName(transferResponse, NS_WS_ADDRESSING, "ReferenceParameters");
        final Element selectorSet = XmlUtil.findExactlyOneChildElementByName(refParameters, NS_WS_MANAGEMENT, "SelectorSet");
        final Element selector = XmlUtil.findExactlyOneChildElementByName(selectorSet, NS_WS_MANAGEMENT, "Selector");

        assertTrue("Identifier in " + Arrays.asList( expectedIds ) + ": "+ XmlUtil.getTextValue(selector), ArrayUtils.containsIgnoreCase( expectedIds, XmlUtil.getTextValue(selector)));
    }

    private void doCreateFail( final String resourceUri,
                               final String payload,
                               final String faultSubcode ) throws Exception {

        doCreateFail(resourceUri, payload, faultSubcode,null);
    }

    private void doCreateFail( final String resourceUri,
                                  final String payload,
                                  final String faultSubcode,
                                  final String faultDetailTextContains) throws Exception {

        final String message = "<s:Envelope xmlns:s=\"http://www.w3.org/2003/05/soap-envelope\" xmlns:wsa=\"http://schemas.xmlsoap.org/ws/2004/08/addressing\" xmlns:wsman=\"http://schemas.dmtf.org/wbem/wsman/1/wsman.xsd\"><s:Header><wsa:Action s:mustUnderstand=\"true\">http://schemas.xmlsoap.org/ws/2004/09/transfer/Create</wsa:Action><wsa:To s:mustUnderstand=\"true\">http://127.0.0.1:8080/wsman</wsa:To><wsman:ResourceURI s:mustUnderstand=\"true\">{0}</wsman:ResourceURI><wsa:MessageID s:mustUnderstand=\"true\">uuid:a711f948-7d39-1d39-8002-481688002100</wsa:MessageID><wsa:ReplyTo><wsa:Address>http://schemas.xmlsoap.org/ws/2004/08/addressing/role/anonymous</wsa:Address></wsa:ReplyTo></s:Header><s:Body>{1}</s:Body></s:Envelope>";

        final Document result = processRequest( "http://schemas.xmlsoap.org/ws/2004/09/transfer/Create", MessageFormat.format( message, resourceUri, payload ));

        final Element soapBody = SoapUtil.getBodyElement(result);
        final Element faultElement = XmlUtil.findExactlyOneChildElementByName(soapBody, SOAPConstants.URI_NS_SOAP_1_2_ENVELOPE, "Fault");
        final Element codeElement = XmlUtil.findExactlyOneChildElementByName(faultElement, SOAPConstants.URI_NS_SOAP_1_2_ENVELOPE, "Code");
        final Element subcodeElement = XmlUtil.findExactlyOneChildElementByName(codeElement, SOAPConstants.URI_NS_SOAP_1_2_ENVELOPE, "Subcode");
        final Element valueElement = XmlUtil.findExactlyOneChildElementByName(subcodeElement, SOAPConstants.URI_NS_SOAP_1_2_ENVELOPE, "Value");

        final String code = XmlUtil.getTextValue( valueElement );

        assertEquals("SOAP Fault subcode", faultSubcode, code);

        if(faultDetailTextContains!=null){
            final Element detailElement = XmlUtil.findExactlyOneChildElementByName(faultElement, SOAPConstants.URI_NS_SOAP_1_2_ENVELOPE, "Detail");
            final Element textElement = XmlUtil.findExactlyOneChildElementByName(detailElement, SOAPConstants.URI_NS_SOAP_1_2_ENVELOPE, "Text");
            final String detailText = XmlUtil.getTextValue( textElement );
            assertTrue("SOAP Fault Detail Text contains" + faultDetailTextContains, detailText.contains(faultDetailTextContains));
        }
    }

    private void putAndVerify( final String message,
                               final UnaryVoidThrows<Document,Exception> verifier,
                               final boolean removeVersionAndId ) throws Exception {
        final String messageToPut;
        if ( removeVersionAndId ) {
            // ensures that id and version attributes are optional for Puts
            messageToPut = message
                    .replaceAll( " id=\"[a-zA-Z0-9:]+\"", "" )
                    .replaceAll( " version=\"[0-9]+\"", "" );
        } else {
            messageToPut = message;
        }

        final Document result = processRequest( "http://schemas.xmlsoap.org/ws/2004/09/transfer/Put", messageToPut );
        verifier.call( result );
    }

    private Document processRequest( final String action,
                                     final String message ) throws Exception {
        System.out.println( XmlUtil.nodeToFormattedString(XmlUtil.parse( message )) );

        final String contentType = ContentTypeHeader.SOAP_1_2_DEFAULT.getFullValue() + "; action=\""+action+"\"";
        final Message request = new Message();
        request.initialize( ContentTypeHeader.parseValue(contentType) , message.getBytes( "utf-8" ));
        final Message response = new Message();

        final MockServletContext servletContext = new MockServletContext();
        final MockHttpServletRequest httpServletRequest = new MockHttpServletRequest(servletContext);
        final MockHttpServletResponse httpServletResponse = new MockHttpServletResponse();

        servletContext.setContextPath( "/" );

        httpServletRequest.setMethod("POST");
        httpServletRequest.setContentType(contentType);
        httpServletRequest.addHeader("Content-Type", contentType);
        httpServletRequest.setRemoteAddr("127.0.0.1");
        httpServletRequest.setServerName( "127.0.0.1" );
        httpServletRequest.setRequestURI("/wsman");
        httpServletRequest.setContent(message.getBytes("UTF-8"));

        final HttpRequestKnob reqKnob = new HttpServletRequestKnob(httpServletRequest);
        request.attachHttpRequestKnob(reqKnob);

        final HttpServletResponseKnob respKnob = new HttpServletResponseKnob(httpServletResponse);
        response.attachHttpResponseKnob(respKnob);

        PolicyEnforcementContext context = null;
        try {
            context = PolicyEnforcementContextFactory.createPolicyEnforcementContext(request, response);

            // fake user authentication
            context.getDefaultAuthenticationContext().addAuthenticationResult( new AuthenticationResult(
                    new UserBean("admin"),
                    new HttpBasicToken("admin", "".toCharArray()), null, false)
            );

            managementAssertion.checkRequest( context );

            Document responseDoc = response.getXmlKnob().getDocumentReadOnly();

            System.out.println( XmlUtil.nodeToFormattedString(responseDoc) );

            return responseDoc;
        } finally {
            ResourceUtils.closeQuietly( context );
        }
    }

    private String getPropertyValue( final Element propertiesElement,
                                     final String propertyName ) {
        String value = null;

        for ( final Element propertyElement : XmlUtil.findChildElementsByName( propertiesElement, NS_GATEWAY_MANAGEMENT, "Property" ) ) {
            if ( propertyName.equals(propertyElement.getAttributeNS( null, "key" )) ) {
                final Element valueElement = XmlUtil.findFirstChildElement( propertyElement );
                if ( valueElement != null ) {
                    value = XmlUtil.getTextValue( valueElement );
                }
                break;
            }
        }

        return value;
    }

    private static class MockServiceManager extends EntityManagerStub<PublishedService,ServiceHeader> implements ServiceManager {
        MockServiceManager( final PublishedService... entitiesIn ) {
            super( entitiesIn );
        }

        @Override
        public String resolveWsdlTarget( final String url ) {
            return null;
        }

        @Override
        public void addManageServiceRole( final PublishedService service ) throws SaveException {
            throw new SaveException("Not implemented");
        }

        @Override
        public Collection<ServiceHeader> findAllHeaders( final boolean includeAliases ) throws FindException {
            throw new FindException("Not implemented");
        }

        @Override
        public Collection<PublishedService> findByRoutingUri(String routingUri) throws FindException {
            throw new FindException("Not implemented");
        }
    }

}
