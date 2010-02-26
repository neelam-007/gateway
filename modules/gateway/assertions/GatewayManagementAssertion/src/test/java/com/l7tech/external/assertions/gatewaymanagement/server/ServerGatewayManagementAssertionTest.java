package com.l7tech.external.assertions.gatewaymanagement.server;

import com.l7tech.common.io.XmlUtil;
import com.l7tech.external.assertions.gatewaymanagement.GatewayManagementAssertion;
import com.l7tech.gateway.common.cluster.ClusterProperty;
import com.l7tech.identity.UserBean;
import com.l7tech.message.Message;
import com.l7tech.message.HttpRequestKnob;
import com.l7tech.message.HttpServletRequestKnob;
import com.l7tech.message.HttpServletResponseKnob;
import com.l7tech.common.mime.ContentTypeHeader;
import com.l7tech.security.token.http.HttpBasicToken;
import com.l7tech.server.MockClusterPropertyManager;
import com.l7tech.server.identity.AuthenticationResult;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.server.message.PolicyEnforcementContextFactory;
import com.l7tech.server.policy.PolicyManager;
import com.l7tech.server.policy.PolicyManagerStub;
import com.l7tech.server.security.rbac.RbacServicesStub;
import com.l7tech.server.service.ServiceDocumentManagerStub;
import com.l7tech.server.service.ServiceManagerStub;
import com.l7tech.gateway.common.service.PublishedService;
import com.l7tech.server.transport.jms.JmsConnectionManagerStub;
import com.l7tech.server.transport.jms.JmsEndpointManagerStub;
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

import java.text.MessageFormat;
import javax.xml.soap.SOAPConstants;


/**
 * Test the GatewayManagementAssertion.
 */
public class ServerGatewayManagementAssertionTest {

    //- PUBLIC

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
                "    <w:ResourceURI s:mustUnderstand=\"true\">http://ns.l7tech.com/2010/01/gateway-management/services</w:ResourceURI> \n" +
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
                "    <w:ResourceURI s:mustUnderstand=\"true\">http://ns.l7tech.com/2010/01/gateway-management/services</w:ResourceURI> \n" +
                "    <w:SelectorSet>\n" +
                "      <w:Selector Name=\"id\">1</w:Selector> \n" +
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

        assertEquals("Service name", "Test Service", XmlUtil.getTextValue(name));
    }

    @Test
    public void testCreate() throws Exception {
        String resourceUri = "http://ns.l7tech.com/2010/01/gateway-management/clusterProperties";
        String payload = "<n1:ClusterProperty xmlns:n1=\"http://ns.l7tech.com/2010/01/gateway-management\"><n1:Name>test</n1:Name><n1:Value>value</n1:Value></n1:ClusterProperty>";
        String expectedId = "4";
        doCreate( resourceUri, payload, expectedId );
    }

// TODO [steve] Create testing entity manager for services and re-enable    
//    @Test
//    public void testCreateService() throws Exception {
//        String resourceUri = "http://ns.l7tech.com/2010/01/gateway-management/services";
//        String payload = "<Service xmlns=\"http://ns.l7tech.com/2010/01/gateway-management\"><ServiceDetail><Name>Warehouse Service</Name><Enabled>true</Enabled><ServiceMappings><HttpMapping><UrlPattern>/waremulti</UrlPattern><Verbs><Verb>POST</Verb></Verbs></HttpMapping></ServiceMappings><Properties><Property key=\"soap\"><BooleanValue>false</BooleanValue></Property></Properties></ServiceDetail><Resources><ResourceSet tag=\"policy\"><Resource type=\"policy\">&lt;?xml version=&quot;1.0&quot; encoding=&quot;UTF-8&quot;?&gt;\n" +
//                "&lt;wsp:Policy xmlns:L7p=&quot;http://www.layer7tech.com/ws/policy&quot; xmlns:wsp=&quot;http://schemas.xmlsoap.org/ws/2002/12/policy&quot;&gt;\n" +
//                "    &lt;wsp:All wsp:Usage=&quot;Required&quot;&gt;\n" +
//                "        &lt;L7p:EchoRoutingAssertion/&gt;\n" +
//                "    &lt;/wsp:All&gt;\n" +
//                "&lt;/wsp:Policy&gt;\n" +
//                "</Resource></ResourceSet></Resources></Service>";
//        String expectedId = "";
//        doCreate( resourceUri, payload, expectedId );
//    }

    @Test
    public void testCreateJMSDestination() throws Exception {
        String resourceUri = "http://ns.l7tech.com/2010/01/gateway-management/jmsDestinations";
        String payload =
                "<JMSDestination xmlns=\"http://ns.l7tech.com/2010/01/gateway-management\">\n" +
                "    <JMSDestinationDetails version=\"0\" id=\"48037888\">\n" +
                "        <DestinationName>QueueName</DestinationName>\n" +
                "        <Inbound>false</Inbound>\n" +
                "        <Enabled>true</Enabled>\n" +
                "    </JMSDestinationDetails>\n" +
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
        String expectedId = "1";
        doCreate( resourceUri, payload, expectedId );
    }

    @Test
    public void testCreatePolicy() throws Exception {
        String resourceUri = "http://ns.l7tech.com/2010/01/gateway-management/policies";
        String payload = "<Policy xmlns=\"http://ns.l7tech.com/2010/01/gateway-management\"><PolicyDetail><Name>Policy Name</Name><PolicyType>Include</PolicyType><Properties><Property key=\"soap\"><BooleanValue>true</BooleanValue></Property></Properties></PolicyDetail><Resources><ResourceSet tag=\"policy\"><Resource type=\"policy\">&lt;?xml version=&quot;1.0&quot; encoding=&quot;UTF-8&quot;?&gt;\n" +
                "&lt;wsp:Policy xmlns:L7p=&quot;http://www.layer7tech.com/ws/policy&quot; xmlns:wsp=&quot;http://schemas.xmlsoap.org/ws/2002/12/policy&quot;&gt;\n" +
                "    &lt;wsp:All wsp:Usage=&quot;Required&quot;&gt;\n" +
                "        &lt;L7p:CommentAssertion&gt;\n" +
                "            &lt;L7p:Comment stringValue=&quot;Comment&quot;/&gt;\n" +
                "        &lt;/L7p:CommentAssertion&gt;\n" +
                "    &lt;/wsp:All&gt;\n" +
                "&lt;/wsp:Policy&gt;\n" +
                "</Resource></ResourceSet></Resources></Policy>";
        String expectedId = "1";
        doCreate( resourceUri, payload, expectedId );
    }

    @Test
    public void testPut() throws Exception {
        String message = "<s:Envelope xmlns:s=\"http://www.w3.org/2003/05/soap-envelope\" xmlns:wsa=\"http://schemas.xmlsoap.org/ws/2004/08/addressing\" xmlns:wsman=\"http://schemas.dmtf.org/wbem/wsman/1/wsman.xsd\" xmlns:n1=\"http://ns.l7tech.com/2010/01/gateway-management\"><s:Header><wsa:Action s:mustUnderstand=\"true\">http://schemas.xmlsoap.org/ws/2004/09/transfer/Put</wsa:Action><wsa:To s:mustUnderstand=\"true\">http://127.0.0.1:8080/wsman</wsa:To><wsman:ResourceURI s:mustUnderstand=\"true\">http://ns.l7tech.com/2010/01/gateway-management/clusterProperties</wsman:ResourceURI><wsa:MessageID s:mustUnderstand=\"true\">uuid:afad2993-7d39-1d39-8002-481688002100</wsa:MessageID><wsa:ReplyTo><wsa:Address>http://schemas.xmlsoap.org/ws/2004/08/addressing/role/anonymous</wsa:Address></wsa:ReplyTo><wsman:SelectorSet><wsman:Selector Name=\"id\">1</wsman:Selector></wsman:SelectorSet><wsman:RequestEPR/></s:Header><s:Body> <n1:ClusterProperty id=\"1\" version=\"0\"><n1:Name>test</n1:Name><n1:Value>value2</n1:Value></n1:ClusterProperty>  </s:Body></s:Envelope>";

        final Document result = processRequest( "http://schemas.xmlsoap.org/ws/2004/09/transfer/Put", message );

        final Element soapBody = SoapUtil.getBodyElement(result);
        final Element clusterProperty = XmlUtil.findExactlyOneChildElementByName(soapBody, NS_GATEWAY_MANAGEMENT, "ClusterProperty");
        final Element value = XmlUtil.findExactlyOneChildElementByName(clusterProperty, NS_GATEWAY_MANAGEMENT, "Value");

        assertEquals("Property value", "value2", XmlUtil.getTextValue(value));
    }

    @Test
    public void testDelete() throws Exception {        
        String message = "<s:Envelope xmlns:s=\"http://www.w3.org/2003/05/soap-envelope\" xmlns:wsa=\"http://schemas.xmlsoap.org/ws/2004/08/addressing\" xmlns:wsman=\"http://schemas.dmtf.org/wbem/wsman/1/wsman.xsd\"><s:Header><wsa:Action s:mustUnderstand=\"true\">http://schemas.xmlsoap.org/ws/2004/09/transfer/Delete</wsa:Action><wsa:To s:mustUnderstand=\"true\">http://127.0.0.1:8080/wsman</wsa:To><wsman:ResourceURI s:mustUnderstand=\"true\">http://ns.l7tech.com/2010/01/gateway-management/clusterProperties</wsman:ResourceURI><wsa:MessageID s:mustUnderstand=\"true\">uuid:b2794ffb-7d39-1d39-8002-481688002100</wsa:MessageID><wsa:ReplyTo><wsa:Address>http://schemas.xmlsoap.org/ws/2004/08/addressing/role/anonymous</wsa:Address></wsa:ReplyTo><wsman:SelectorSet><wsman:Selector Name=\"id\">2</wsman:Selector></wsman:SelectorSet></s:Header><s:Body/></s:Envelope>";

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
                "            xmlns:l7=\"http://ns.l7tech.com/2010/01/gateway-management\" \n" +
                "            xmlns:w=\"http://schemas.dmtf.org/wbem/wsman/1/wsman.xsd\">\n" +
                "  <s:Header>\n" +
                "    <a:MessageID>uuid:4ED2993C-4339-4E99-81FC-C2FD3812781A</a:MessageID> \n" +
                "    <a:To>http://127.0.0.1:8080/wsman</a:To> \n" +
                "    <a:ReplyTo> \n" +
                "      <a:Address s:mustUnderstand=\"true\">http://schemas.xmlsoap.org/ws/2004/08/addressing/role/anonymous</a:Address> \n" +
                "    </a:ReplyTo> \n" +
                "    <a:Action s:mustUnderstand=\"true\">http://schemas.xmlsoap.org/ws/2004/09/transfer/Get</a:Action> \n" +
                "    <w:ResourceURI s:mustUnderstand=\"true\">http://ns.l7tech.com/2010/01/gateway-management/services</w:ResourceURI> \n" +
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
        String message = "<s:Envelope xmlns:s=\"http://www.w3.org/2003/05/soap-envelope\" xmlns:wsa=\"http://schemas.xmlsoap.org/ws/2004/08/addressing\" xmlns:wsman=\"http://schemas.dmtf.org/wbem/wsman/1/wsman.xsd\" xmlns:n1=\"http://ns.l7tech.com/2010/01/gateway-management\"><s:Header><wsa:Action s:mustUnderstand=\"true\">http://schemas.xmlsoap.org/ws/2004/09/transfer/Put</wsa:Action><wsa:To s:mustUnderstand=\"true\">http://127.0.0.1:8080/wsman</wsa:To><wsman:ResourceURI s:mustUnderstand=\"true\">http://ns.l7tech.com/2010/01/gateway-management/clusterProperties</wsman:ResourceURI><wsa:MessageID s:mustUnderstand=\"true\">uuid:afad2993-7d39-1d39-8002-481688002100</wsa:MessageID><wsa:ReplyTo><wsa:Address>http://schemas.xmlsoap.org/ws/2004/08/addressing/role/anonymous</wsa:Address></wsa:ReplyTo><wsman:SelectorSet><wsman:Selector Name=\"id\">1</wsman:Selector></wsman:SelectorSet><wsman:FragmentTransfer s:mustUnderstand=\"true\">n1:Value[1]</wsman:FragmentTransfer></s:Header><s:Body><wsman:XmlFragment><n1:Value>value3</n1:Value></wsman:XmlFragment></s:Body></s:Envelope>";

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
        String message = "<s:Envelope xmlns:s=\"http://www.w3.org/2003/05/soap-envelope\" xmlns:wsa=\"http://schemas.xmlsoap.org/ws/2004/08/addressing\" xmlns:wsman=\"http://schemas.dmtf.org/wbem/wsman/1/wsman.xsd\" xmlns:n1=\"http://ns.l7tech.com/2010/01/gateway-management\"><s:Header><wsa:Action s:mustUnderstand=\"true\">http://schemas.xmlsoap.org/ws/2004/09/transfer/Put</wsa:Action><wsa:To s:mustUnderstand=\"true\">http://127.0.0.1:8080/wsman</wsa:To><wsman:ResourceURI s:mustUnderstand=\"true\">http://ns.l7tech.com/2010/01/gateway-management/clusterProperties</wsman:ResourceURI><wsa:MessageID s:mustUnderstand=\"true\">uuid:afad2993-7d39-1d39-8002-481688002100</wsa:MessageID><wsa:ReplyTo><wsa:Address>http://schemas.xmlsoap.org/ws/2004/08/addressing/role/anonymous</wsa:Address></wsa:ReplyTo><wsman:SelectorSet><wsman:Selector Name=\"id\">1</wsman:Selector></wsman:SelectorSet><wsman:FragmentTransfer s:mustUnderstand=\"true\">n1:Value/text()</wsman:FragmentTransfer></s:Header><s:Body><wsman:XmlFragment>value3text</wsman:XmlFragment></s:Body></s:Envelope>";

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
                "    <w:ResourceURI s:mustUnderstand=\"true\">http://ns.l7tech.com/2010/01/gateway-management/services</w:ResourceURI> \n" +
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
                "    <w:ResourceURI s:mustUnderstand=\"true\">http://ns.l7tech.com/2010/01/gateway-management/services</w:ResourceURI> \n" +
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
                "<s:Envelope xmlns:s=\"http://www.w3.org/2003/05/soap-envelope\" xmlns:wsa=\"http://schemas.xmlsoap.org/ws/2004/08/addressing\" xmlns:wsman=\"http://schemas.dmtf.org/wbem/wsman/1/wsman.xsd\" xmlns:n1=\"http://ns.l7tech.com/2010/01/gateway-management\">" +
                "<s:Header>" +
                "<wsa:Action s:mustUnderstand=\"true\">http://ns.l7tech.com/2010/01/gateway-management/testResources/Create</wsa:Action>" +
                "<wsa:To s:mustUnderstand=\"true\">http://127.0.0.1:8080/wsman</wsa:To>" +
                "<wsa:MessageID s:mustUnderstand=\"true\">uuid:a711f948-7d39-1d39-8002-481688002100</wsa:MessageID>" +
                "<wsa:ReplyTo><wsa:Address>http://schemas.xmlsoap.org/ws/2004/08/addressing/role/anonymous</wsa:Address></wsa:ReplyTo>" +
                "<wsman:ResourceURI s:mustUnderstand=\"true\">http://ns.l7tech.com/2010/01/gateway-management/testResources</wsman:ResourceURI>" +
                "</s:Header>" +
                "<s:Body></s:Body>" +
                "</s:Envelope>";

        final Document result = processRequest( "http://ns.l7tech.com/2010/01/gateway-management/testResources/Create", message );

        final Element soapBody = SoapUtil.getBodyElement(result);
        final Element resource = XmlUtil.findExactlyOneChildElementByName( soapBody, null, "TestResource" );

        assertEquals("Resource text", "Test resource text", XmlUtil.getTextValue(resource));
    }

    @Test
    public void testCustomCreate2() throws Exception {
        String message =
                "<s:Envelope xmlns:s=\"http://www.w3.org/2003/05/soap-envelope\" xmlns:wsa=\"http://schemas.xmlsoap.org/ws/2004/08/addressing\" xmlns:wsman=\"http://schemas.dmtf.org/wbem/wsman/1/wsman.xsd\" xmlns:n1=\"http://ns.l7tech.com/2010/01/gateway-management\">" +
                "<s:Header>" +
                "<wsa:Action s:mustUnderstand=\"true\">http://ns.l7tech.com/2010/01/gateway-management/testResources/Create2</wsa:Action>" +
                "<wsa:To s:mustUnderstand=\"true\">http://127.0.0.1:8080/wsman</wsa:To>" +
                "<wsa:MessageID s:mustUnderstand=\"true\">uuid:a711f948-7d39-1d39-8002-481688002100</wsa:MessageID>" +
                "<wsa:ReplyTo><wsa:Address>http://schemas.xmlsoap.org/ws/2004/08/addressing/role/anonymous</wsa:Address></wsa:ReplyTo>" +
                "<wsman:ResourceURI s:mustUnderstand=\"true\">http://ns.l7tech.com/2010/01/gateway-management/testResources</wsman:ResourceURI>" +
                "</s:Header>" +
                "<s:Body><n1:ClusterProperty><n1:Name>a</n1:Name><n1:Value>Test resource text2</n1:Value></n1:ClusterProperty></s:Body>" +
                "</s:Envelope>";

        final Document result = processRequest( "http://ns.l7tech.com/2010/01/gateway-management/testResources/Create2", message );

        final Element soapBody = SoapUtil.getBodyElement(result);
        final Element resource = XmlUtil.findExactlyOneChildElementByName( soapBody, NS_GATEWAY_MANAGEMENT, "ClusterProperty" );
        final Element value = XmlUtil.findExactlyOneChildElementByName( resource, NS_GATEWAY_MANAGEMENT, "Value" );

        assertEquals("Resource text", "Test resource text2", XmlUtil.getTextValue(value));
    }

    @Test
    public void testCustomGet() throws Exception {
        String message =
                "<s:Envelope xmlns:s=\"http://www.w3.org/2003/05/soap-envelope\" xmlns:wsa=\"http://schemas.xmlsoap.org/ws/2004/08/addressing\" xmlns:wsman=\"http://schemas.dmtf.org/wbem/wsman/1/wsman.xsd\" xmlns:n1=\"http://ns.l7tech.com/2010/01/gateway-management\">" +
                "<s:Header>" +
                "<wsa:Action s:mustUnderstand=\"true\">http://ns.l7tech.com/2010/01/gateway-management/testResources/Get</wsa:Action>" +
                "<wsa:To s:mustUnderstand=\"true\">http://127.0.0.1:8080/wsman</wsa:To>" +
                "<wsa:MessageID s:mustUnderstand=\"true\">uuid:a711f948-7d39-1d39-8002-481688002100</wsa:MessageID>" +
                "<wsa:ReplyTo><wsa:Address>http://schemas.xmlsoap.org/ws/2004/08/addressing/role/anonymous</wsa:Address></wsa:ReplyTo>" +
                "<wsman:ResourceURI s:mustUnderstand=\"true\">http://ns.l7tech.com/2010/01/gateway-management/testResources</wsman:ResourceURI>" +
                "<wsman:SelectorSet>" +
                "<wsman:Selector Name=\"id\">1</wsman:Selector>" +
                "</wsman:SelectorSet>" +
                "</s:Header>" +
                "<s:Body></s:Body>" +
                "</s:Envelope>";

        final Document result = processRequest( "http://ns.l7tech.com/2010/01/gateway-management/testResources/Get", message );

        final Element soapBody = SoapUtil.getBodyElement(result);
        final Element resource = XmlUtil.findExactlyOneChildElementByName( soapBody, null, "TestResource" );

        assertEquals("Resource text", "Test resource text", XmlUtil.getTextValue(resource));
    }

    @Test
    public void testCustomPut() throws Exception {
        String message =
                "<s:Envelope xmlns:s=\"http://www.w3.org/2003/05/soap-envelope\" xmlns:wsa=\"http://schemas.xmlsoap.org/ws/2004/08/addressing\" xmlns:wsman=\"http://schemas.dmtf.org/wbem/wsman/1/wsman.xsd\" xmlns:n1=\"http://ns.l7tech.com/2010/01/gateway-management\">" +
                "<s:Header>" +
                "<wsa:Action s:mustUnderstand=\"true\">http://ns.l7tech.com/2010/01/gateway-management/testResources/Put</wsa:Action>" +
                "<wsa:To s:mustUnderstand=\"true\">http://127.0.0.1:8080/wsman</wsa:To>" +
                "<wsa:MessageID s:mustUnderstand=\"true\">uuid:a711f948-7d39-1d39-8002-481688002100</wsa:MessageID>" +
                "<wsa:ReplyTo><wsa:Address>http://schemas.xmlsoap.org/ws/2004/08/addressing/role/anonymous</wsa:Address></wsa:ReplyTo>" +
                "<wsman:ResourceURI s:mustUnderstand=\"true\">http://ns.l7tech.com/2010/01/gateway-management/testResources</wsman:ResourceURI>" +
                "<wsman:SelectorSet>" +
                "<wsman:Selector Name=\"id\">1</wsman:Selector>" +
                "</wsman:SelectorSet>" +
                "</s:Header>" +
                "<s:Body><n1:ClusterProperty><n1:Name>a</n1:Name><n1:Value>Test resource text2</n1:Value></n1:ClusterProperty></s:Body>" +
                "</s:Envelope>";

        final Document result = processRequest( "http://ns.l7tech.com/2010/01/gateway-management/testResources/Put", message );

        final Element soapBody = SoapUtil.getBodyElement(result);
        final Element resource = XmlUtil.findExactlyOneChildElementByName( soapBody, NS_GATEWAY_MANAGEMENT, "ClusterProperty" );
        final Element value = XmlUtil.findExactlyOneChildElementByName( resource, NS_GATEWAY_MANAGEMENT, "Value" );

        assertEquals("Resource text", "Test resource text2", XmlUtil.getTextValue(value));
    }

    //- PRIVATE

    private static final StaticListableBeanFactory beanFactory = new StaticListableBeanFactory();
    private static final String NS_WS_TRANSFER = "http://schemas.xmlsoap.org/ws/2004/09/transfer";
    private static final String NS_WS_ADDRESSING = "http://schemas.xmlsoap.org/ws/2004/08/addressing";
    private static final String NS_WS_ENUMERATION = "http://schemas.xmlsoap.org/ws/2004/09/enumeration";
    private static final String NS_WS_MANAGEMENT = "http://schemas.dmtf.org/wbem/wsman/1/wsman.xsd";
    private static final String NS_WS_MANAGEMENT_IDENTITY = "http://schemas.dmtf.org/wbem/wsman/identity/1/wsmanidentity.xsd";
    private static final String NS_GATEWAY_MANAGEMENT = "http://ns.l7tech.com/2010/01/gateway-management";

    static {
        init();
    }

    @SuppressWarnings({"serial"})
    private static void init() {
        PolicyManager policyManager = new PolicyManagerStub();
        beanFactory.addBean( "clusterPropertyManager", new MockClusterPropertyManager( new ClusterProperty("testProp1", "testValue1"){
            {
                setOid(1);
            }
        },new ClusterProperty("testProp2", "testValue2"){
            {
                setOid(2);
            }
        },new ClusterProperty("testProp3", "testValue3"){
            {
                setOid(3);
            }
        }));
        beanFactory.addBean( "jmsConnectionManager",  new JmsConnectionManagerStub());
        beanFactory.addBean( "jmsEndpointManager",  new JmsEndpointManagerStub());
        beanFactory.addBean( "policyManager",  policyManager);
        beanFactory.addBean( "rbacServices", new RbacServicesStub() );
        beanFactory.addBean( "securityFilter", new RbacServicesStub() );
        beanFactory.addBean( "serviceDocumentManager", new ServiceDocumentManagerStub() );
        beanFactory.addBean( "serviceManager", new ServiceManagerStub( policyManager, new PublishedService(){
            {
                setOid(1);
                setName( "Test Service" );
                setDisabled( false );
                setSoap( false );
            }
        }) );
    }

    private void doCreate( final String resourceUri,
                           final String payload,
                           final String expectedId ) throws Exception {

        final String message = "<s:Envelope xmlns:s=\"http://www.w3.org/2003/05/soap-envelope\" xmlns:wsa=\"http://schemas.xmlsoap.org/ws/2004/08/addressing\" xmlns:wsman=\"http://schemas.dmtf.org/wbem/wsman/1/wsman.xsd\"><s:Header><wsa:Action s:mustUnderstand=\"true\">http://schemas.xmlsoap.org/ws/2004/09/transfer/Create</wsa:Action><wsa:To s:mustUnderstand=\"true\">http://127.0.0.1:8080/wsman</wsa:To><wsman:ResourceURI s:mustUnderstand=\"true\">{0}</wsman:ResourceURI><wsa:MessageID s:mustUnderstand=\"true\">uuid:a711f948-7d39-1d39-8002-481688002100</wsa:MessageID><wsa:ReplyTo><wsa:Address>http://schemas.xmlsoap.org/ws/2004/08/addressing/role/anonymous</wsa:Address></wsa:ReplyTo></s:Header><s:Body>{1}</s:Body></s:Envelope>";

        final Document result = processRequest( "http://schemas.xmlsoap.org/ws/2004/09/transfer/Create", MessageFormat.format( message, resourceUri, payload ));

        final Element soapBody = SoapUtil.getBodyElement(result);
        final Element transferResponse = XmlUtil.findExactlyOneChildElementByName(soapBody, NS_WS_TRANSFER, "ResourceCreated");
        final Element refParameters = XmlUtil.findExactlyOneChildElementByName(transferResponse, NS_WS_ADDRESSING, "ReferenceParameters");
        final Element selectorSet = XmlUtil.findExactlyOneChildElementByName(refParameters, NS_WS_MANAGEMENT, "SelectorSet");
        final Element selector = XmlUtil.findExactlyOneChildElementByName(selectorSet, NS_WS_MANAGEMENT, "Selector");

        assertEquals("Identifier ", expectedId, XmlUtil.getTextValue(selector));
    }

    private Document processRequest( final String action,
                                     final String message ) throws Exception {
        System.out.println( XmlUtil.nodeToFormattedString(XmlUtil.parse( message )) );

        final String contentType = ContentTypeHeader.SOAP_1_2_DEFAULT.getFullValue() + "; action=\""+action+"\"";
        final Message request = new Message();
        request.initialize( ContentTypeHeader.parseValue(contentType) , message.getBytes( "utf-8" ));
        final Message response = new Message();

        final MockServletContext servletContext = new MockServletContext();
        final MockHttpServletRequest hrequest = new MockHttpServletRequest(servletContext);
        final MockHttpServletResponse hresponse = new MockHttpServletResponse();

        servletContext.setContextPath( "/" );

        hrequest.setMethod("POST");
        hrequest.setContentType(contentType);
        hrequest.addHeader("Content-Type", contentType);
        hrequest.setRemoteAddr("127.0.0.1");
        hrequest.setServerName( "127.0.0.1" );
        hrequest.setRequestURI("/wsman");
        hrequest.setContent(message.getBytes("UTF-8"));

        final HttpRequestKnob reqKnob = new HttpServletRequestKnob(hrequest);
        request.attachHttpRequestKnob(reqKnob);

        final HttpServletResponseKnob respKnob = new HttpServletResponseKnob(hresponse);
        response.attachHttpResponseKnob(respKnob);

        PolicyEnforcementContext context = null; 
        try {
            context = PolicyEnforcementContextFactory.createPolicyEnforcementContext(request, response);
            final ServerGatewayManagementAssertion swma = new ServerGatewayManagementAssertion(
                    new GatewayManagementAssertion(), beanFactory, "testGatewayManagementContext.xml" );

            // fake user auth
            context.getDefaultAuthenticationContext().addAuthenticationResult( new AuthenticationResult(
                    new UserBean("admin"),
                    new HttpBasicToken("admin", "".toCharArray()), null, false)
            );

            swma.checkRequest( context );

            Document responseDoc = response.getXmlKnob().getDocumentReadOnly();

            System.out.println( XmlUtil.nodeToFormattedString(responseDoc) );

            return responseDoc;            
        } finally {
            ResourceUtils.closeQuietly( context );
        }
    }

}
