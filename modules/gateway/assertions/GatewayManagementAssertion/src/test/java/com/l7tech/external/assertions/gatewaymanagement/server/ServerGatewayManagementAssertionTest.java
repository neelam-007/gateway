package com.l7tech.external.assertions.gatewaymanagement.server;

import com.l7tech.common.TestDocuments;
import com.l7tech.common.io.XmlUtil;
import com.l7tech.external.assertions.gatewaymanagement.GatewayManagementAssertion;
import com.l7tech.gateway.common.cluster.ClusterProperty;
import com.l7tech.gateway.common.jdbc.JdbcConnection;
import com.l7tech.gateway.common.schema.SchemaEntry;
import com.l7tech.gateway.common.security.keystore.SsgKeyEntry;
import com.l7tech.gateway.common.service.ServiceHeader;
import com.l7tech.gateway.common.service.ServiceTemplate;
import com.l7tech.gateway.common.transport.jms.JmsConnection;
import com.l7tech.gateway.common.transport.jms.JmsEndpoint;
import com.l7tech.identity.IdentityProviderConfig;
import com.l7tech.identity.IdentityProviderType;
import com.l7tech.identity.UserBean;
import com.l7tech.message.Message;
import com.l7tech.message.HttpRequestKnob;
import com.l7tech.message.HttpServletRequestKnob;
import com.l7tech.message.HttpServletResponseKnob;
import com.l7tech.common.mime.ContentTypeHeader;
import com.l7tech.objectmodel.FindException;
import com.l7tech.objectmodel.SaveException;
import com.l7tech.objectmodel.UpdateException;
import com.l7tech.objectmodel.folder.Folder;
import com.l7tech.policy.AssertionRegistry;
import com.l7tech.policy.Policy;
import com.l7tech.policy.PolicyType;
import com.l7tech.policy.PolicyValidatorResult;
import com.l7tech.policy.PolicyValidatorStub;
import com.l7tech.security.cert.TrustedCert;
import com.l7tech.security.token.http.HttpBasicToken;
import com.l7tech.server.EntityManagerStub;
import com.l7tech.server.MockClusterPropertyManager;
import com.l7tech.server.communityschemas.SchemaEntryManagerStub;
import com.l7tech.server.identity.AuthenticationResult;
import com.l7tech.server.identity.TestIdentityProviderConfigManager;
import com.l7tech.server.identity.cert.TestTrustedCertManager;
import com.l7tech.server.jdbc.JdbcConnectionManagerStub;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.server.message.PolicyEnforcementContextFactory;
import com.l7tech.server.policy.PolicyManagerStub;
import com.l7tech.server.security.keystore.SsgKeyFinderStub;
import com.l7tech.server.security.keystore.SsgKeyStoreManagerStub;
import com.l7tech.server.security.rbac.FolderManagerStub;
import com.l7tech.server.security.rbac.RbacServicesStub;
import com.l7tech.server.service.ServiceDocumentManagerStub;
import com.l7tech.server.service.ServiceManager;
import com.l7tech.gateway.common.service.PublishedService;
import com.l7tech.server.transport.jms.JmsConnectionManagerStub;
import com.l7tech.server.transport.jms.JmsEndpointManagerStub;
import com.l7tech.server.uddi.ServiceWsdlUpdateChecker;
import com.l7tech.util.ArrayUtils;
import com.l7tech.util.ExceptionUtils;
import com.l7tech.util.ResourceUtils;
import com.l7tech.xml.soap.SoapUtil;
import org.springframework.mock.web.MockServletContext;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.beans.factory.support.StaticListableBeanFactory;
import org.junit.Test;
import static org.junit.Assert.*;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import java.net.MalformedURLException;
import java.security.PrivateKey;
import java.security.cert.X509Certificate;
import java.text.MessageFormat;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import javax.xml.soap.SOAPConstants;

/**
 * Test the GatewayManagementAssertion.
 */
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
                "      <w:Selector Name=\"id\">2</w:Selector> \n" +
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

        assertEquals("Service name", "Test Service 2", XmlUtil.getTextValue(name));
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

        assertEquals("SOAP Fault value", "wsman:InvalidSelectors", XmlUtil.getTextValue(value));    }

    @Test
    public void testCreateClusterProperty() throws Exception {
        String resourceUri = "http://ns.l7tech.com/2010/04/gateway-management/clusterProperties";
        String payload = "<n1:ClusterProperty xmlns:n1=\"http://ns.l7tech.com/2010/04/gateway-management\"><n1:Name>test</n1:Name><n1:Value>value</n1:Value></n1:ClusterProperty>";
        String expectedId = "4";
        doCreate( resourceUri, payload, expectedId );
    }

    @Test
    public void testCreateService() throws Exception {
        String resourceUri = "http://ns.l7tech.com/2010/04/gateway-management/services";
        String payload = "<Service xmlns=\"http://ns.l7tech.com/2010/04/gateway-management\"><ServiceDetail><Name>Warehouse Service</Name><Enabled>true</Enabled><ServiceMappings><HttpMapping><UrlPattern>/waremulti</UrlPattern><Verbs><Verb>POST</Verb></Verbs></HttpMapping></ServiceMappings><Properties><Property key=\"soap\"><BooleanValue>false</BooleanValue></Property></Properties></ServiceDetail><Resources><ResourceSet tag=\"policy\"><Resource type=\"policy\">&lt;?xml version=&quot;1.0&quot; encoding=&quot;UTF-8&quot;?&gt;\n" +
                "&lt;wsp:Policy xmlns:L7p=&quot;http://www.layer7tech.com/ws/policy&quot; xmlns:wsp=&quot;http://schemas.xmlsoap.org/ws/2002/12/policy&quot;&gt;\n" +
                "    &lt;wsp:All wsp:Usage=&quot;Required&quot;&gt;\n" +
                "        &lt;L7p:EchoRoutingAssertion/&gt;\n" +
                "    &lt;/wsp:All&gt;\n" +
                "&lt;/wsp:Policy&gt;\n" +
                "</Resource></ResourceSet></Resources></Service>";
        String expectedId = "3";
        doCreate( resourceUri, payload, expectedId );
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
        doCreate( resourceUri, payload, "2", "3" );
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
        doCreate( resourceUri, payload, "2", "3"  );
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
        String payload = "<Policy xmlns=\"http://ns.l7tech.com/2010/04/gateway-management\"><PolicyDetail><Name>Policy Name</Name><PolicyType>Include</PolicyType><Properties><Property key=\"soap\"><BooleanValue>true</BooleanValue></Property></Properties></PolicyDetail><Resources><ResourceSet tag=\"policy\"><Resource type=\"policy\">&lt;?xml version=&quot;1.0&quot; encoding=&quot;UTF-8&quot;?&gt;\n" +
                "&lt;wsp:Policy xmlns:L7p=&quot;http://www.layer7tech.com/ws/policy&quot; xmlns:wsp=&quot;http://schemas.xmlsoap.org/ws/2002/12/policy&quot;&gt;\n" +
                "    &lt;wsp:All wsp:Usage=&quot;Required&quot;&gt;\n" +
                "        &lt;L7p:CommentAssertion&gt;\n" +
                "            &lt;L7p:Comment stringValue=&quot;Comment&quot;/&gt;\n" +
                "        &lt;/L7p:CommentAssertion&gt;\n" +
                "    &lt;/wsp:All&gt;\n" +
                "&lt;/wsp:Policy&gt;\n" +
                "</Resource></ResourceSet></Resources></Policy>";
        String expectedId = "2";
        doCreate( resourceUri, payload, expectedId );
    }

    @Test
    public void testCreateSchema() throws Exception {
        String resourceUri = "http://ns.l7tech.com/2010/04/gateway-management/resources";
        String payload =
                "<ResourceDocument xmlns=\"http://ns.l7tech.com/2010/04/gateway-management\">\n" +
                "    <Resource sourceUrl=\"books2.xsd\" type=\"xmlschema\">&lt;xs:schema targetNamespace=\"urn:books2\" xmlns:xs=\"http://www.w3.org/2001/XMLSchema\"&gt;&lt;xs:element name=\"book\" type=\"xs:string\"/&gt;&lt;/xs:schema&gt;</Resource>\n" +
                "</ResourceDocument>";
        String expectedId = "3";
        doCreate( resourceUri, payload, expectedId );
    }

    @Test
    public void testPutClusterProperty() throws Exception {
        String message = "<s:Envelope xmlns:s=\"http://www.w3.org/2003/05/soap-envelope\" xmlns:wsa=\"http://schemas.xmlsoap.org/ws/2004/08/addressing\" xmlns:wsman=\"http://schemas.dmtf.org/wbem/wsman/1/wsman.xsd\" xmlns:n1=\"http://ns.l7tech.com/2010/04/gateway-management\"><s:Header><wsa:Action s:mustUnderstand=\"true\">http://schemas.xmlsoap.org/ws/2004/09/transfer/Put</wsa:Action><wsa:To s:mustUnderstand=\"true\">http://127.0.0.1:8080/wsman</wsa:To><wsman:ResourceURI s:mustUnderstand=\"true\">http://ns.l7tech.com/2010/04/gateway-management/clusterProperties</wsman:ResourceURI><wsa:MessageID s:mustUnderstand=\"true\">uuid:afad2993-7d39-1d39-8002-481688002100</wsa:MessageID><wsa:ReplyTo><wsa:Address>http://schemas.xmlsoap.org/ws/2004/08/addressing/role/anonymous</wsa:Address></wsa:ReplyTo><wsman:SelectorSet><wsman:Selector Name=\"id\">1</wsman:Selector></wsman:SelectorSet><wsman:RequestEPR/></s:Header><s:Body> <n1:ClusterProperty id=\"1\" version=\"0\"><n1:Name>test</n1:Name><n1:Value>value2</n1:Value></n1:ClusterProperty>  </s:Body></s:Envelope>";

        final Document result = processRequest( "http://schemas.xmlsoap.org/ws/2004/09/transfer/Put", message );

        final Element soapBody = SoapUtil.getBodyElement(result);
        final Element clusterProperty = XmlUtil.findExactlyOneChildElementByName(soapBody, NS_GATEWAY_MANAGEMENT, "ClusterProperty");
        final Element value = XmlUtil.findExactlyOneChildElementByName(clusterProperty, NS_GATEWAY_MANAGEMENT, "Value");

        assertEquals("Property value", "value2", XmlUtil.getTextValue(value));
    }

    @Test
    public void testPutJmsDestination() throws Exception {
        String message = "<s:Envelope xmlns:s=\"http://www.w3.org/2003/05/soap-envelope\" xmlns:wsa=\"http://schemas.xmlsoap.org/ws/2004/08/addressing\" xmlns:wsman=\"http://schemas.dmtf.org/wbem/wsman/1/wsman.xsd\" xmlns:l7=\"http://ns.l7tech.com/2010/04/gateway-management\"><s:Header><wsa:Action s:mustUnderstand=\"true\">http://schemas.xmlsoap.org/ws/2004/09/transfer/Put</wsa:Action><wsa:To s:mustUnderstand=\"true\">http://127.0.0.1:8080/wsman</wsa:To><wsman:ResourceURI s:mustUnderstand=\"true\">http://ns.l7tech.com/2010/04/gateway-management/jmsDestinations</wsman:ResourceURI><wsa:MessageID s:mustUnderstand=\"true\">uuid:afad2993-7d39-1d39-8002-481688002100</wsa:MessageID><wsa:ReplyTo><wsa:Address>http://schemas.xmlsoap.org/ws/2004/08/addressing/role/anonymous</wsa:Address></wsa:ReplyTo><wsman:SelectorSet><wsman:Selector Name=\"id\">1</wsman:Selector></wsman:SelectorSet><wsman:RequestEPR/></s:Header><s:Body>" +
              "    <l7:JMSDestination id=\"1\" version=\"0\">\n" +
              "        <l7:JMSDestinationDetail id=\"1\" version=\"0\">\n" +
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
              "        <l7:JMSConnection id=\"1\" version=\"0\">\n" +
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

        final Document result = processRequest( "http://schemas.xmlsoap.org/ws/2004/09/transfer/Put", message );

        final Element soapBody = SoapUtil.getBodyElement(result);
        final Element jmsDestination = XmlUtil.findExactlyOneChildElementByName(soapBody, NS_GATEWAY_MANAGEMENT, "JMSDestination");
        final Element jmsDestinationDetail = XmlUtil.findExactlyOneChildElementByName(jmsDestination, NS_GATEWAY_MANAGEMENT, "JMSDestinationDetail");
        final Element jmsDestinationDetailName = XmlUtil.findExactlyOneChildElementByName(jmsDestinationDetail, NS_GATEWAY_MANAGEMENT, "Name");

        assertEquals("JMS destination id", "1", jmsDestination.getAttribute( "id" ));
        assertEquals("JMS destination version", "0", jmsDestination.getAttribute( "version" ));
        assertEquals("JMS destination detail id", "1", jmsDestinationDetail.getAttribute( "id" ));
        assertEquals("JMS destination detail version", "0", jmsDestinationDetail.getAttribute( "version" ));
        assertEquals("JMS destination detail name", "Test Endpoint 1", XmlUtil.getTextValue(jmsDestinationDetailName));
    }

    @Test
    public void testPutPolicy() throws Exception {
        String message = "<s:Envelope xmlns:s=\"http://www.w3.org/2003/05/soap-envelope\" xmlns:wsa=\"http://schemas.xmlsoap.org/ws/2004/08/addressing\" xmlns:wsman=\"http://schemas.dmtf.org/wbem/wsman/1/wsman.xsd\" xmlns:l7=\"http://ns.l7tech.com/2010/04/gateway-management\"><s:Header><wsa:Action s:mustUnderstand=\"true\">http://schemas.xmlsoap.org/ws/2004/09/transfer/Put</wsa:Action><wsa:To s:mustUnderstand=\"true\">http://127.0.0.1:8080/wsman</wsa:To><wsman:ResourceURI s:mustUnderstand=\"true\">http://ns.l7tech.com/2010/04/gateway-management/policies</wsman:ResourceURI><wsa:MessageID s:mustUnderstand=\"true\">uuid:afad2993-7d39-1d39-8002-481688002100</wsa:MessageID><wsa:ReplyTo><wsa:Address>http://schemas.xmlsoap.org/ws/2004/08/addressing/role/anonymous</wsa:Address></wsa:ReplyTo><wsman:SelectorSet><wsman:Selector Name=\"id\">1</wsman:Selector></wsman:SelectorSet><wsman:RequestEPR/></s:Header><s:Body>" +
                "<l7:Policy id=\"1\" guid=\"c4ca4238-a0b9-3382-8dcc-509a6f75849b\" version=\"0\">\n" +
                "    <l7:PolicyDetail id=\"1\" guid=\"c4ca4238-a0b9-3382-8dcc-509a6f75849b\" version=\"0\">\n" +
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

        final Document result = processRequest( "http://schemas.xmlsoap.org/ws/2004/09/transfer/Put", message );

        final Element soapBody = SoapUtil.getBodyElement(result);
        final Element policy = XmlUtil.findExactlyOneChildElementByName(soapBody, NS_GATEWAY_MANAGEMENT, "Policy");
        final Element policyDetail = XmlUtil.findExactlyOneChildElementByName(policy, NS_GATEWAY_MANAGEMENT, "PolicyDetail");
        final Element policyDetailName = XmlUtil.findExactlyOneChildElementByName(policyDetail, NS_GATEWAY_MANAGEMENT, "Name");

        assertEquals("Policy id", "1", policy.getAttribute( "id" ));
        assertEquals("Policy version", "0", policy.getAttribute( "version" ));
        assertEquals("Policy detail id", "1", policyDetail.getAttribute( "id" ));
        assertEquals("Policy detail version", "0", policyDetail.getAttribute( "version" ));
        assertEquals("Policy detail name", "Test Policy 1", XmlUtil.getTextValue(policyDetailName));
    }

    @Test
    public void testPutResourceDocument() throws Exception {
        String message = "<s:Envelope xmlns:s=\"http://www.w3.org/2003/05/soap-envelope\" xmlns:wsa=\"http://schemas.xmlsoap.org/ws/2004/08/addressing\" xmlns:wsman=\"http://schemas.dmtf.org/wbem/wsman/1/wsman.xsd\" xmlns:l7=\"http://ns.l7tech.com/2010/04/gateway-management\"><s:Header><wsa:Action s:mustUnderstand=\"true\">http://schemas.xmlsoap.org/ws/2004/09/transfer/Put</wsa:Action><wsa:To s:mustUnderstand=\"true\">http://127.0.0.1:8080/wsman</wsa:To><wsman:ResourceURI s:mustUnderstand=\"true\">http://ns.l7tech.com/2010/04/gateway-management/resources</wsman:ResourceURI><wsa:MessageID s:mustUnderstand=\"true\">uuid:afad2993-7d39-1d39-8002-481688002100</wsa:MessageID><wsa:ReplyTo><wsa:Address>http://schemas.xmlsoap.org/ws/2004/08/addressing/role/anonymous</wsa:Address></wsa:ReplyTo><wsman:SelectorSet><wsman:Selector Name=\"id\">1</wsman:Selector></wsman:SelectorSet><wsman:RequestEPR/></s:Header><s:Body>" +
                "    <l7:ResourceDocument id=\"1\" version=\"0\">\n" +
                "        <l7:Resource sourceUrl=\"books2.xsd\" type=\"xmlschema\">&lt;xs:schema xmlns:xs=\"http://www.w3.org/2001/XMLSchema\"&gt;&lt;xs:element name=\"book\" type=\"xs:string\"/&gt;&lt;/xs:schema&gt;</l7:Resource>\n" +
                "    </l7:ResourceDocument>\n" +
                "</s:Body></s:Envelope>";

        final Document result = processRequest( "http://schemas.xmlsoap.org/ws/2004/09/transfer/Put", message );

        final Element soapBody = SoapUtil.getBodyElement(result);
        final Element resourceDocument = XmlUtil.findExactlyOneChildElementByName(soapBody, NS_GATEWAY_MANAGEMENT, "ResourceDocument");
        final Element resource = XmlUtil.findExactlyOneChildElementByName(resourceDocument, NS_GATEWAY_MANAGEMENT, "Resource");

        assertEquals("Resource document id", "1", resourceDocument.getAttribute( "id" ));
        assertEquals("Resource document version", "0", resourceDocument.getAttribute( "version" ));
        assertEquals("Resource document resource sourceUrl", "books2.xsd", resource.getAttribute( "sourceUrl" ));
    }

    @Test
    public void testPutService() throws Exception {
        String message = "<s:Envelope xmlns:s=\"http://www.w3.org/2003/05/soap-envelope\" xmlns:wsa=\"http://schemas.xmlsoap.org/ws/2004/08/addressing\" xmlns:wsman=\"http://schemas.dmtf.org/wbem/wsman/1/wsman.xsd\" xmlns:l7=\"http://ns.l7tech.com/2010/04/gateway-management\"><s:Header><wsa:Action s:mustUnderstand=\"true\">http://schemas.xmlsoap.org/ws/2004/09/transfer/Put</wsa:Action><wsa:To s:mustUnderstand=\"true\">http://127.0.0.1:8080/wsman</wsa:To><wsman:ResourceURI s:mustUnderstand=\"true\">http://ns.l7tech.com/2010/04/gateway-management/services</wsman:ResourceURI><wsa:MessageID s:mustUnderstand=\"true\">uuid:afad2993-7d39-1d39-8002-481688002100</wsa:MessageID><wsa:ReplyTo><wsa:Address>http://schemas.xmlsoap.org/ws/2004/08/addressing/role/anonymous</wsa:Address></wsa:ReplyTo><wsman:SelectorSet><wsman:Selector Name=\"id\">2</wsman:Selector></wsman:SelectorSet><wsman:RequestEPR/></s:Header><s:Body>" +
                "<l7:Service id=\"2\" version=\"1\">\n" +
                "            <l7:ServiceDetail id=\"2\" version=\"1\">\n" +
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

        final Document result = processRequest( "http://schemas.xmlsoap.org/ws/2004/09/transfer/Put", message );

        final Element soapBody = SoapUtil.getBodyElement(result);
        final Element service = XmlUtil.findExactlyOneChildElementByName(soapBody, NS_GATEWAY_MANAGEMENT, "Service");
        final Element serviceDetail = XmlUtil.findExactlyOneChildElementByName(service, NS_GATEWAY_MANAGEMENT, "ServiceDetail");
        final Element serviceDetailName = XmlUtil.findExactlyOneChildElementByName(serviceDetail, NS_GATEWAY_MANAGEMENT, "Name");

        assertEquals("Service id", "2", service.getAttribute( "id" ));
        assertEquals("Service version", "1", service.getAttribute( "version" ));
        assertEquals("Service detail id", "2", serviceDetail.getAttribute( "id" ));
        assertEquals("Service detail version", "1", serviceDetail.getAttribute( "version" ));
        assertEquals("Service detail name", "Test Service 2", XmlUtil.getTextValue(serviceDetailName));
    }

    @Test
    public void testDelete() throws Exception {        
        String message = "<s:Envelope xmlns:s=\"http://www.w3.org/2003/05/soap-envelope\" xmlns:wsa=\"http://schemas.xmlsoap.org/ws/2004/08/addressing\" xmlns:wsman=\"http://schemas.dmtf.org/wbem/wsman/1/wsman.xsd\"><s:Header><wsa:Action s:mustUnderstand=\"true\">http://schemas.xmlsoap.org/ws/2004/09/transfer/Delete</wsa:Action><wsa:To s:mustUnderstand=\"true\">http://127.0.0.1:8080/wsman</wsa:To><wsman:ResourceURI s:mustUnderstand=\"true\">http://ns.l7tech.com/2010/04/gateway-management/clusterProperties</wsman:ResourceURI><wsa:MessageID s:mustUnderstand=\"true\">uuid:b2794ffb-7d39-1d39-8002-481688002100</wsa:MessageID><wsa:ReplyTo><wsa:Address>http://schemas.xmlsoap.org/ws/2004/08/addressing/role/anonymous</wsa:Address></wsa:ReplyTo><wsman:SelectorSet><wsman:Selector Name=\"id\">2</wsman:Selector></wsman:SelectorSet></s:Header><s:Body/></s:Envelope>";

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
                "      <w:Selector Name=\"id\">1</w:Selector> \n" +
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
        String message = "<s:Envelope xmlns:s=\"http://www.w3.org/2003/05/soap-envelope\" xmlns:wsa=\"http://schemas.xmlsoap.org/ws/2004/08/addressing\" xmlns:wsman=\"http://schemas.dmtf.org/wbem/wsman/1/wsman.xsd\" xmlns:n1=\"http://ns.l7tech.com/2010/04/gateway-management\"><s:Header><wsa:Action s:mustUnderstand=\"true\">http://schemas.xmlsoap.org/ws/2004/09/transfer/Put</wsa:Action><wsa:To s:mustUnderstand=\"true\">http://127.0.0.1:8080/wsman</wsa:To><wsman:ResourceURI s:mustUnderstand=\"true\">http://ns.l7tech.com/2010/04/gateway-management/clusterProperties</wsman:ResourceURI><wsa:MessageID s:mustUnderstand=\"true\">uuid:afad2993-7d39-1d39-8002-481688002100</wsa:MessageID><wsa:ReplyTo><wsa:Address>http://schemas.xmlsoap.org/ws/2004/08/addressing/role/anonymous</wsa:Address></wsa:ReplyTo><wsman:SelectorSet><wsman:Selector Name=\"id\">1</wsman:Selector></wsman:SelectorSet><wsman:FragmentTransfer s:mustUnderstand=\"true\">n1:Value[1]</wsman:FragmentTransfer></s:Header><s:Body><wsman:XmlFragment><n1:Value>value3</n1:Value></wsman:XmlFragment></s:Body></s:Envelope>";

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
        String message = "<s:Envelope xmlns:s=\"http://www.w3.org/2003/05/soap-envelope\" xmlns:wsa=\"http://schemas.xmlsoap.org/ws/2004/08/addressing\" xmlns:wsman=\"http://schemas.dmtf.org/wbem/wsman/1/wsman.xsd\" xmlns:n1=\"http://ns.l7tech.com/2010/04/gateway-management\"><s:Header><wsa:Action s:mustUnderstand=\"true\">http://schemas.xmlsoap.org/ws/2004/09/transfer/Put</wsa:Action><wsa:To s:mustUnderstand=\"true\">http://127.0.0.1:8080/wsman</wsa:To><wsman:ResourceURI s:mustUnderstand=\"true\">http://ns.l7tech.com/2010/04/gateway-management/clusterProperties</wsman:ResourceURI><wsa:MessageID s:mustUnderstand=\"true\">uuid:afad2993-7d39-1d39-8002-481688002100</wsa:MessageID><wsa:ReplyTo><wsa:Address>http://schemas.xmlsoap.org/ws/2004/08/addressing/role/anonymous</wsa:Address></wsa:ReplyTo><wsman:SelectorSet><wsman:Selector Name=\"id\">1</wsman:Selector></wsman:SelectorSet><wsman:FragmentTransfer s:mustUnderstand=\"true\">n1:Value/text()</wsman:FragmentTransfer></s:Header><s:Body><wsman:XmlFragment>value3text</wsman:XmlFragment></s:Body></s:Envelope>";

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
                "<env:Envelope xmlns:env=\"http://www.w3.org/2003/05/soap-envelope\" xmlns:wsa=\"http://schemas.xmlsoap.org/ws/2004/08/addressing\" xmlns:wsman=\"http://schemas.dmtf.org/wbem/wsman/1/wsman.xsd\" xmlns:wxf=\"http://schemas.xmlsoap.org/ws/2004/09/transfer\" xmlns:xs=\"http://www.w3.org/2001/XMLSchema\"><env:Header><wsa:Action env:mustUnderstand=\"true\">http://ns.l7tech.com/2010/04/gateway-management/policies/ExportPolicy</wsa:Action><wsa:ReplyTo><wsa:Address env:mustUnderstand=\"true\">http://schemas.xmlsoap.org/ws/2004/08/addressing/role/anonymous</wsa:Address></wsa:ReplyTo><wsa:MessageID env:mustUnderstand=\"true\">uuid:d521f4b4-ef2a-4381-b11c-fa72eb760a99</wsa:MessageID><wsa:To env:mustUnderstand=\"true\">http://localhost:8080/wsman</wsa:To><wsman:ResourceURI>http://ns.l7tech.com/2010/04/gateway-management/policies</wsman:ResourceURI><wsman:OperationTimeout>P0Y0M0DT0H5M0.000S</wsman:OperationTimeout><wsman:SelectorSet><wsman:Selector Name=\"id\">1</wsman:Selector></wsman:SelectorSet></env:Header><env:Body/></env:Envelope>";

        final Document result = processRequest( "http://ns.l7tech.com/2010/04/gateway-management/policies/ExportPolicy", message );

        final Element soapBody = SoapUtil.getBodyElement(result);
        final Element policyExportResult = XmlUtil.findExactlyOneChildElementByName(soapBody, NS_GATEWAY_MANAGEMENT, "PolicyExportResult");
        final Element resource = XmlUtil.findExactlyOneChildElementByName(policyExportResult, NS_GATEWAY_MANAGEMENT, "Resource");

        assertEquals( "resource type", "policyexport", resource.getAttribute("type"));
        assertTrue( "resource content is export", XmlUtil.getTextValue(resource).contains( "<exp:Export " ));
    }

    @Test
    public void testPolicyImport() throws Exception {
        final String message =
                "<env:Envelope xmlns:env=\"http://www.w3.org/2003/05/soap-envelope\" xmlns:wsa=\"http://schemas.xmlsoap.org/ws/2004/08/addressing\" xmlns:wsman=\"http://schemas.dmtf.org/wbem/wsman/1/wsman.xsd\" xmlns:wxf=\"http://schemas.xmlsoap.org/ws/2004/09/transfer\" xmlns:xs=\"http://www.w3.org/2001/XMLSchema\"><env:Header><wsa:Action env:mustUnderstand=\"true\">http://ns.l7tech.com/2010/04/gateway-management/policies/ImportPolicy</wsa:Action><wsa:ReplyTo><wsa:Address env:mustUnderstand=\"true\">http://schemas.xmlsoap.org/ws/2004/08/addressing/role/anonymous</wsa:Address></wsa:ReplyTo><wsa:MessageID env:mustUnderstand=\"true\">uuid:d0f59849-9eaa-4027-8b2e-f5ec2dfc1f9d</wsa:MessageID><wsa:To env:mustUnderstand=\"true\">http://localhost:8080/wsman</wsa:To><wsman:ResourceURI>http://ns.l7tech.com/2010/04/gateway-management/policies</wsman:ResourceURI><wsman:OperationTimeout>P0Y0M0DT0H5M0.000S</wsman:OperationTimeout><wsman:SelectorSet><wsman:Selector Name=\"id\">1</wsman:Selector></wsman:SelectorSet></env:Header><env:Body><PolicyImportContext xmlns=\"http://ns.l7tech.com/2010/04/gateway-management\"><Properties/><Resource type=\"policyexport\">&lt;?xml version=\"1.0\" encoding=\"UTF-8\"?&gt;\n" +
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
                "<env:Envelope xmlns:env=\"http://www.w3.org/2003/05/soap-envelope\" xmlns:wsa=\"http://schemas.xmlsoap.org/ws/2004/08/addressing\" xmlns:wsman=\"http://schemas.dmtf.org/wbem/wsman/1/wsman.xsd\" xmlns:wxf=\"http://schemas.xmlsoap.org/ws/2004/09/transfer\" xmlns:xs=\"http://www.w3.org/2001/XMLSchema\"><env:Header><wsa:Action env:mustUnderstand=\"true\">http://ns.l7tech.com/2010/04/gateway-management/policies/ImportPolicy</wsa:Action><wsa:ReplyTo><wsa:Address env:mustUnderstand=\"true\">http://schemas.xmlsoap.org/ws/2004/08/addressing/role/anonymous</wsa:Address></wsa:ReplyTo><wsa:MessageID env:mustUnderstand=\"true\">uuid:d0f59849-9eaa-4027-8b2e-f5ec2dfc1f9d</wsa:MessageID><wsa:To env:mustUnderstand=\"true\">http://localhost:8080/wsman</wsa:To><wsman:ResourceURI>http://ns.l7tech.com/2010/04/gateway-management/policies</wsman:ResourceURI><wsman:OperationTimeout>P0Y0M0DT0H5M0.000S</wsman:OperationTimeout><wsman:SelectorSet><wsman:Selector Name=\"id\">1</wsman:Selector></wsman:SelectorSet></env:Header><env:Body><PolicyImportContext xmlns=\"http://ns.l7tech.com/2010/04/gateway-management\"><Properties/><Resource type=\"policyexport\">&lt;?xml version=\"1.0\" encoding=\"UTF-8\"?&gt;\n" +
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
                        "            &lt;OID&gt;2&lt;/OID&gt;\n" +
                        "            &lt;CertificateName&gt;Bob&lt;/CertificateName&gt;\n" +
                        "            &lt;CertificateIssuerDn&gt;CN=OASIS Interop Test CA, O=OASIS&lt;/CertificateIssuerDn&gt;\n" +
                        "            &lt;CertificateSerialNum&gt;127901500862700997089151460209364726264&lt;/CertificateSerialNum&gt;\n" +
                        "        &lt;/TrustedCertificateReference&gt;\n" +
                        "        &lt;JMSConnectionReference RefType=\"com.l7tech.console.policy.exporter.JMSEndpointReference\"&gt;\n" +
                        "            &lt;OID&gt;1&lt;/OID&gt;\n" +
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
                        "                &lt;L7p:EndpointOid boxedLongValue=\"1\"/&gt;\n" +
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
        final Element importedPolicyRef = XmlUtil.findExactlyOneChildElementByName(importedPolicyRefs, NS_GATEWAY_MANAGEMENT, "ImportedPolicyReference");

        assertEquals("Imported policy ref GUID", "886ece03-a64c-4c17-93cf-ce49e7265daa", importedPolicyRef.getAttribute( "guid" ));
        assertEquals("Imported policy ref type", "Created", importedPolicyRef.getAttribute( "type" ));
    }

    @Test
    public void testPolicyImportWithInstructions() throws Exception {
        final String message =
                "<env:Envelope xmlns:env=\"http://www.w3.org/2003/05/soap-envelope\" xmlns:wsa=\"http://schemas.xmlsoap.org/ws/2004/08/addressing\" xmlns:wsman=\"http://schemas.dmtf.org/wbem/wsman/1/wsman.xsd\" xmlns:wxf=\"http://schemas.xmlsoap.org/ws/2004/09/transfer\" xmlns:xs=\"http://www.w3.org/2001/XMLSchema\"><env:Header><wsa:Action env:mustUnderstand=\"true\">http://ns.l7tech.com/2010/04/gateway-management/policies/ImportPolicy</wsa:Action><wsa:ReplyTo><wsa:Address env:mustUnderstand=\"true\">http://schemas.xmlsoap.org/ws/2004/08/addressing/role/anonymous</wsa:Address></wsa:ReplyTo><wsa:MessageID env:mustUnderstand=\"true\">uuid:d0f59849-9eaa-4027-8b2e-f5ec2dfc1f9d</wsa:MessageID><wsa:To env:mustUnderstand=\"true\">http://localhost:8080/wsman</wsa:To><wsman:ResourceURI>http://ns.l7tech.com/2010/04/gateway-management/policies</wsman:ResourceURI><wsman:OperationTimeout>P0Y0M0DT0H5M0.000S</wsman:OperationTimeout><wsman:SelectorSet><wsman:Selector Name=\"id\">1</wsman:Selector></wsman:SelectorSet></env:Header><env:Body><PolicyImportContext xmlns=\"http://ns.l7tech.com/2010/04/gateway-management\"><Properties/><Resource type=\"policyexport\">&lt;?xml version=\"1.0\" encoding=\"UTF-8\"?&gt;\n" +
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
                        "    <PolicyReferenceInstruction type=\"Map\"    referenceType=\"com.l7tech.console.policy.exporter.IdProviderReference\"     referenceId=\"200\" mappedReferenceId=\"-2\"/>\n" +
                        "    <PolicyReferenceInstruction type=\"Ignore\" referenceType=\"com.l7tech.console.policy.exporter.JdbcConnectionReference\" referenceId=\"syn:7dffbacb-72ba-3848-84d5-4a86247eb67e\" />\n" +
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
    public void testPolicyValidate() throws Exception {
        final String message =
                "<env:Envelope xmlns:env=\"http://www.w3.org/2003/05/soap-envelope\" xmlns:wsa=\"http://schemas.xmlsoap.org/ws/2004/08/addressing\" xmlns:wsman=\"http://schemas.dmtf.org/wbem/wsman/1/wsman.xsd\" xmlns:wxf=\"http://schemas.xmlsoap.org/ws/2004/09/transfer\"><env:Header><wsa:Action env:mustUnderstand=\"true\">http://ns.l7tech.com/2010/04/gateway-management/policies/ValidatePolicy</wsa:Action><wsa:ReplyTo><wsa:Address env:mustUnderstand=\"true\">http://schemas.xmlsoap.org/ws/2004/08/addressing/role/anonymous</wsa:Address></wsa:ReplyTo><wsa:MessageID env:mustUnderstand=\"true\">uuid:61dcea28-f545-4b0f-9436-0e8b59f4370d</wsa:MessageID><wsa:To env:mustUnderstand=\"true\">http://localhost:8080/wsman</wsa:To><wsman:ResourceURI>http://ns.l7tech.com/2010/04/gateway-management/policies</wsman:ResourceURI><wsman:OperationTimeout>P0Y0M0DT0H5M0.000S</wsman:OperationTimeout><wsman:SelectorSet><wsman:Selector Name=\"id\">1</wsman:Selector></wsman:SelectorSet></env:Header><env:Body/></env:Envelope>";

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
                    new PolicyValidatorResult.Warning( Arrays.asList( 0 ), 1, 1, "Test warning message", null )
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
                    new PolicyValidatorResult.Error( Arrays.asList( 0 ), 1, 1, "Test error message", null )
            ) );
            policyValidator.setWarnings( Arrays.asList(
                    new PolicyValidatorResult.Warning( Arrays.asList( 0 ), 1, 1, "Test warning message 1", null ),
                    new PolicyValidatorResult.Warning( Arrays.asList( 0 ), 1, 1, "Test warning message 2", null )
            ) );

            final Document result = processRequest( "http://ns.l7tech.com/2010/04/gateway-management/policies/ValidatePolicy", message );

            final Element soapBody = SoapUtil.getBodyElement(result);
            final Element validationResult = XmlUtil.findExactlyOneChildElementByName(soapBody, NS_GATEWAY_MANAGEMENT, "PolicyValidationResult");
            final Element validationStatus = XmlUtil.findExactlyOneChildElementByName(validationResult, NS_GATEWAY_MANAGEMENT, "ValidationStatus");
            final Element validationMessages = XmlUtil.findExactlyOneChildElementByName(validationResult, NS_GATEWAY_MANAGEMENT, "ValidationMessages");
            final List<Element> validationMessageElements = XmlUtil.findChildElementsByName( validationMessages, NS_GATEWAY_MANAGEMENT, "ValidationMessage" );

            assertEquals("Validation status", "Error", XmlUtil.getTextValue(validationStatus));
            assertEquals("Validation messages size", 3, validationMessageElements.size());
        }
    }

    @Test
    public void testServicePolicyExport() throws Exception {
        final String message =
                "<env:Envelope xmlns:env=\"http://www.w3.org/2003/05/soap-envelope\" xmlns:wsa=\"http://schemas.xmlsoap.org/ws/2004/08/addressing\" xmlns:wsman=\"http://schemas.dmtf.org/wbem/wsman/1/wsman.xsd\" xmlns:wxf=\"http://schemas.xmlsoap.org/ws/2004/09/transfer\" xmlns:xs=\"http://www.w3.org/2001/XMLSchema\"><env:Header><wsa:Action env:mustUnderstand=\"true\">http://ns.l7tech.com/2010/04/gateway-management/services/ExportPolicy</wsa:Action><wsa:ReplyTo><wsa:Address env:mustUnderstand=\"true\">http://schemas.xmlsoap.org/ws/2004/08/addressing/role/anonymous</wsa:Address></wsa:ReplyTo><wsa:MessageID env:mustUnderstand=\"true\">uuid:d521f4b4-ef2a-4381-b11c-fa72eb760a99</wsa:MessageID><wsa:To env:mustUnderstand=\"true\">http://localhost:8080/wsman</wsa:To><wsman:ResourceURI>http://ns.l7tech.com/2010/04/gateway-management/services</wsman:ResourceURI><wsman:OperationTimeout>P0Y0M0DT0H5M0.000S</wsman:OperationTimeout><wsman:SelectorSet><wsman:Selector Name=\"id\">1</wsman:Selector></wsman:SelectorSet></env:Header><env:Body/></env:Envelope>";

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
                "<env:Envelope xmlns:env=\"http://www.w3.org/2003/05/soap-envelope\" xmlns:wsa=\"http://schemas.xmlsoap.org/ws/2004/08/addressing\" xmlns:wsman=\"http://schemas.dmtf.org/wbem/wsman/1/wsman.xsd\" xmlns:wxf=\"http://schemas.xmlsoap.org/ws/2004/09/transfer\" xmlns:xs=\"http://www.w3.org/2001/XMLSchema\"><env:Header><wsa:Action env:mustUnderstand=\"true\">http://ns.l7tech.com/2010/04/gateway-management/services/ImportPolicy</wsa:Action><wsa:ReplyTo><wsa:Address env:mustUnderstand=\"true\">http://schemas.xmlsoap.org/ws/2004/08/addressing/role/anonymous</wsa:Address></wsa:ReplyTo><wsa:MessageID env:mustUnderstand=\"true\">uuid:d0f59849-9eaa-4027-8b2e-f5ec2dfc1f9d</wsa:MessageID><wsa:To env:mustUnderstand=\"true\">http://localhost:8080/wsman</wsa:To><wsman:ResourceURI>http://ns.l7tech.com/2010/04/gateway-management/services</wsman:ResourceURI><wsman:OperationTimeout>P0Y0M0DT0H5M0.000S</wsman:OperationTimeout><wsman:SelectorSet><wsman:Selector Name=\"id\">1</wsman:Selector></wsman:SelectorSet></env:Header><env:Body><PolicyImportContext xmlns=\"http://ns.l7tech.com/2010/04/gateway-management\"><Properties/><Resource type=\"policyexport\">&lt;?xml version=\"1.0\" encoding=\"UTF-8\"?&gt;\n" +
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
                "<env:Envelope xmlns:env=\"http://www.w3.org/2003/05/soap-envelope\" xmlns:wsa=\"http://schemas.xmlsoap.org/ws/2004/08/addressing\" xmlns:wsman=\"http://schemas.dmtf.org/wbem/wsman/1/wsman.xsd\" xmlns:wxf=\"http://schemas.xmlsoap.org/ws/2004/09/transfer\"><env:Header><wsa:Action env:mustUnderstand=\"true\">http://ns.l7tech.com/2010/04/gateway-management/services/ValidatePolicy</wsa:Action><wsa:ReplyTo><wsa:Address env:mustUnderstand=\"true\">http://schemas.xmlsoap.org/ws/2004/08/addressing/role/anonymous</wsa:Address></wsa:ReplyTo><wsa:MessageID env:mustUnderstand=\"true\">uuid:61dcea28-f545-4b0f-9436-0e8b59f4370d</wsa:MessageID><wsa:To env:mustUnderstand=\"true\">http://localhost:8080/wsman</wsa:To><wsman:ResourceURI>http://ns.l7tech.com/2010/04/gateway-management/services</wsman:ResourceURI><wsman:OperationTimeout>P0Y0M0DT0H5M0.000S</wsman:OperationTimeout><wsman:SelectorSet><wsman:Selector Name=\"id\">1</wsman:Selector></wsman:SelectorSet></env:Header><env:Body/></env:Envelope>";

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

    //- PRIVATE

    private static final StaticListableBeanFactory beanFactory = new StaticListableBeanFactory();
    private static final PolicyValidatorStub policyValidator = new PolicyValidatorStub();
    private static final String NS_WS_TRANSFER = "http://schemas.xmlsoap.org/ws/2004/09/transfer";
    private static final String NS_WS_ADDRESSING = "http://schemas.xmlsoap.org/ws/2004/08/addressing";
    private static final String NS_WS_ENUMERATION = "http://schemas.xmlsoap.org/ws/2004/09/enumeration";
    private static final String NS_WS_MANAGEMENT = "http://schemas.dmtf.org/wbem/wsman/1/wsman.xsd";
    private static final String NS_WS_MANAGEMENT_IDENTITY = "http://schemas.dmtf.org/wbem/wsman/identity/1/wsmanidentity.xsd";
    private static final String NS_GATEWAY_MANAGEMENT = "http://ns.l7tech.com/2010/04/gateway-management";
    private static final String WSDL = "<wsdl:definitions xmlns:wsdl=\"http://schemas.xmlsoap.org/wsdl/\" targetNamespace=\"http://warehouse.acme.com/ws\"/>";

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
        "http://ns.l7tech.com/2010/04/gateway-management/trustedCertificates"
    };

    static {
        try {
            init();
        } catch ( Exception e ) {
            throw ExceptionUtils.wrap( e );
        }
    }

    @SuppressWarnings({"serial"})
    private static void init() throws Exception {
        new AssertionRegistry(); // causes type mappings to be installed for assertions
        Folder rootFolder = folder(-5002, null, "Root Node");
        beanFactory.addBean( "trustedCertManager", new TestTrustedCertManager(
                cert(1, "Alice", TestDocuments.getWssInteropAliceCert()),
                cert(2, "Bob", TestDocuments.getWssInteropBobCert()) ) );
        beanFactory.addBean( "clusterPropertyManager", new MockClusterPropertyManager(
                prop(1, "testProp1", "testValue1"),
                prop(2, "testProp2", "testValue2"),
                prop(3, "testProp3", "testValue3")));
        beanFactory.addBean( "schemaEntryManager", new SchemaEntryManagerStub(
                schema(1,"books.xsd", "urn:books", "<xs:schema xmlns:xs=\"http://www.w3.org/2001/XMLSchema\"><xs:element name=\"book\" type=\"xs:string\"/></xs:schema>"),
                schema(2,"books_refd.xsd", "urn:booksr", "<xs:schema xmlns:xs=\"http://www.w3.org/2001/XMLSchema\"><xs:element name=\"book\" type=\"xs:string\"/></xs:schema>")) );
        beanFactory.addBean( "folderManager", new FolderManagerStub(
                rootFolder,
                folder(1, rootFolder, "Test Folder") ) );
        beanFactory.addBean( "identityProviderConfigManager", new TestIdentityProviderConfigManager(
                provider(-2, IdentityProviderType.INTERNAL, "Internal Identity Provider") ) );
        beanFactory.addBean( "jmsConnectionManager",  new JmsConnectionManagerStub(
                jmsConnection(1, "Test Endpoint", "com.context.Classname", "qcf", "ldap://jndi") ));
        beanFactory.addBean( "jmsEndpointManager",  new JmsEndpointManagerStub(
                jmsEndpoint(1, 1, "Test Endpoint") ));
        beanFactory.addBean( "jdbcConnectionManager", new JdbcConnectionManagerStub(
                connection(1, "Test Connection") ) );
        beanFactory.addBean( "policyManager",  new PolicyManagerStub(
                policy(1, PolicyType.INCLUDE_FRAGMENT, "Test Policy", true, "<wsp:Policy xmlns:L7p=\"http://www.layer7tech.com/ws/policy\" xmlns:wsp=\"http://schemas.xmlsoap.org/ws/2002/12/policy\"><wsp:All wsp:Usage=\"Required\"><L7p:AuditAssertion/></wsp:All></wsp:Policy>") ));
        beanFactory.addBean( "ssgKeyStoreManager", new SsgKeyStoreManagerStub( new SsgKeyFinderStub( Arrays.asList(
                key( 0, "bob", TestDocuments.getWssInteropBobCert(), TestDocuments.getWssInteropBobKey()) ) )) );
        beanFactory.addBean( "rbacServices", new RbacServicesStub() );
        beanFactory.addBean( "securityFilter", new RbacServicesStub() );
        beanFactory.addBean( "serviceDocumentManager", new ServiceDocumentManagerStub() );
        beanFactory.addBean( "serviceManager", new MockServiceManager(
                service(1, "Test Service 1", false, false, null, null),
                service(2, "Test Service 2", false, true, "http://localhost:8080/test.wsdl", WSDL) ));
        beanFactory.addBean( "policyValidator", policyValidator );
        beanFactory.addBean( "serviceWsdlUpdateChecker", new ServiceWsdlUpdateChecker(null, null){
            @Override
            public boolean isWsdlUpdatePermitted( final PublishedService service, final boolean resetWsdlXml ) throws UpdateException {
                return true;
            }
        } );
    }

    private static TrustedCert cert( final long oid, final String name, final X509Certificate x509Certificate ) {
        final TrustedCert cert = new TrustedCert();
        cert.setOid( oid );
        cert.setName( name );
        cert.setCertificate( x509Certificate );
        return cert;
    }

    private static ClusterProperty prop( final long oid, final String name, final String value ) {
        final ClusterProperty prop = new ClusterProperty( name, value );
        prop.setOid( oid );
        return prop;
    }

    private static Folder folder( final long oid, final Folder parent, final String name ) {
        final Folder folder = new Folder( name, parent );
        folder.setOid( oid );
        return folder;
    }

    private static IdentityProviderConfig provider( final long oid, final IdentityProviderType type, final String name ) {
        final IdentityProviderConfig provider = new IdentityProviderConfig( type );
        provider.setOid( oid );
        provider.setName( name );
        return provider;
    }

    private static JdbcConnection connection( final long oid, final String name ) {
        final JdbcConnection connection = new JdbcConnection();
        connection.setOid( oid );
        connection.setName( name );

        // props must be non empty for resolution during import
        connection.setDriverClass( "d" );
        connection.setJdbcUrl( "j" );
        connection.setUserName( "u" );
        return connection;
    }

    private static JmsConnection jmsConnection( final long oid, final String name, final String contextClassname, final String queueFactory, final String jndiUrl) {
        final JmsConnection connection = new JmsConnection();
        connection.setOid( oid );
        connection.setName( name );
        connection.setQueueFactoryUrl( queueFactory );
        connection.setInitialContextFactoryClassname( contextClassname );
        connection.setJndiUrl( jndiUrl );
        return connection;
    }

    private static JmsEndpoint jmsEndpoint( final long oid, final long connectionOid, final String queueName) {
        final JmsEndpoint endpoint = new JmsEndpoint();
        endpoint.setOid( oid );
        endpoint.setConnectionOid( connectionOid );
        endpoint.setName( queueName );
        endpoint.setDestinationName( queueName );
        return endpoint;
    }

    private static Policy policy( final long oid, final PolicyType type, final String name, final boolean soap, final String policyXml ) {
        final Policy policy = new Policy( type, name, policyXml, soap);
        policy.setOid( oid );
        policy.setGuid( UUID.nameUUIDFromBytes( Long.toString(oid).getBytes() ).toString() );
        return policy;
    }

    private static SsgKeyEntry key( final long keystoreOid, final String alias, final X509Certificate cert, final PrivateKey privateKey ) {
        return new SsgKeyEntry( keystoreOid, alias, new X509Certificate[]{cert}, privateKey);
    }

    private static SchemaEntry schema( final long oid, final String systemId, final String tns, final String schemaXml ) {
        final SchemaEntry entry = new SchemaEntry();
        entry.setOid( oid );
        entry.setName( systemId );
        entry.setTns( tns );
        entry.setSchema( schemaXml );
        return entry;
    }

    private static PublishedService service( final long oid, final String name, final boolean disabled, final boolean soap, final String wsdlUrl, final String wsdlXml ) {
        final PublishedService service = new PublishedService();
        service.setOid( oid );
        service.setName( name );
        service.getPolicy().setName( "Policy for " + name + " (#" + oid + ")" );
        service.getPolicy().setGuid( UUID.randomUUID().toString() );
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

    private void doCreate( final String resourceUri,
                           final String payload,
                           final String... expectedIds ) throws Exception {

        final String message = "<s:Envelope xmlns:s=\"http://www.w3.org/2003/05/soap-envelope\" xmlns:wsa=\"http://schemas.xmlsoap.org/ws/2004/08/addressing\" xmlns:wsman=\"http://schemas.dmtf.org/wbem/wsman/1/wsman.xsd\"><s:Header><wsa:Action s:mustUnderstand=\"true\">http://schemas.xmlsoap.org/ws/2004/09/transfer/Create</wsa:Action><wsa:To s:mustUnderstand=\"true\">http://127.0.0.1:8080/wsman</wsa:To><wsman:ResourceURI s:mustUnderstand=\"true\">{0}</wsman:ResourceURI><wsa:MessageID s:mustUnderstand=\"true\">uuid:a711f948-7d39-1d39-8002-481688002100</wsa:MessageID><wsa:ReplyTo><wsa:Address>http://schemas.xmlsoap.org/ws/2004/08/addressing/role/anonymous</wsa:Address></wsa:ReplyTo></s:Header><s:Body>{1}</s:Body></s:Envelope>";

        final Document result = processRequest( "http://schemas.xmlsoap.org/ws/2004/09/transfer/Create", MessageFormat.format( message, resourceUri, payload ));

        final Element soapBody = SoapUtil.getBodyElement(result);
        final Element transferResponse = XmlUtil.findExactlyOneChildElementByName(soapBody, NS_WS_TRANSFER, "ResourceCreated");
        final Element refParameters = XmlUtil.findExactlyOneChildElementByName(transferResponse, NS_WS_ADDRESSING, "ReferenceParameters");
        final Element selectorSet = XmlUtil.findExactlyOneChildElementByName(refParameters, NS_WS_MANAGEMENT, "SelectorSet");
        final Element selector = XmlUtil.findExactlyOneChildElementByName(selectorSet, NS_WS_MANAGEMENT, "Selector");

        assertTrue("Identifier in " + Arrays.asList( expectedIds ), ArrayUtils.containsIgnoreCase( expectedIds, XmlUtil.getTextValue(selector)));
    }

    private void doCreateFail( final String resourceUri,
                               final String payload,
                               final String faultSubcode ) throws Exception {

        final String message = "<s:Envelope xmlns:s=\"http://www.w3.org/2003/05/soap-envelope\" xmlns:wsa=\"http://schemas.xmlsoap.org/ws/2004/08/addressing\" xmlns:wsman=\"http://schemas.dmtf.org/wbem/wsman/1/wsman.xsd\"><s:Header><wsa:Action s:mustUnderstand=\"true\">http://schemas.xmlsoap.org/ws/2004/09/transfer/Create</wsa:Action><wsa:To s:mustUnderstand=\"true\">http://127.0.0.1:8080/wsman</wsa:To><wsman:ResourceURI s:mustUnderstand=\"true\">{0}</wsman:ResourceURI><wsa:MessageID s:mustUnderstand=\"true\">uuid:a711f948-7d39-1d39-8002-481688002100</wsa:MessageID><wsa:ReplyTo><wsa:Address>http://schemas.xmlsoap.org/ws/2004/08/addressing/role/anonymous</wsa:Address></wsa:ReplyTo></s:Header><s:Body>{1}</s:Body></s:Envelope>";

        final Document result = processRequest( "http://schemas.xmlsoap.org/ws/2004/09/transfer/Create", MessageFormat.format( message, resourceUri, payload ));

        final Element soapBody = SoapUtil.getBodyElement(result);
        final Element faultElement = XmlUtil.findExactlyOneChildElementByName(soapBody, SOAPConstants.URI_NS_SOAP_1_2_ENVELOPE, "Fault");
        final Element codeElement = XmlUtil.findExactlyOneChildElementByName(faultElement, SOAPConstants.URI_NS_SOAP_1_2_ENVELOPE, "Code");
        final Element subcodeElement = XmlUtil.findExactlyOneChildElementByName(codeElement, SOAPConstants.URI_NS_SOAP_1_2_ENVELOPE, "Subcode");
        final Element valueElement = XmlUtil.findExactlyOneChildElementByName(subcodeElement, SOAPConstants.URI_NS_SOAP_1_2_ENVELOPE, "Value");

        final String code = XmlUtil.getTextValue( valueElement );

        assertEquals("SOAP Fault subcode", faultSubcode, code);
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
            final ServerGatewayManagementAssertion managementAssertion = new ServerGatewayManagementAssertion(
                    new GatewayManagementAssertion(), beanFactory, "testGatewayManagementContext.xml" );

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
    }

}
