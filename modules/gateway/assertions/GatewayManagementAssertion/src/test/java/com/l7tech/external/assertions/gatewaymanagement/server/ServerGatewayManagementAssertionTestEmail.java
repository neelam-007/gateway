package com.l7tech.external.assertions.gatewaymanagement.server;

import com.l7tech.common.io.XmlUtil;
import com.l7tech.gateway.common.transport.email.EmailListener;
import com.l7tech.gateway.common.transport.email.EmailServerType;
import com.l7tech.objectmodel.Goid;
import com.l7tech.objectmodel.SaveException;
import com.l7tech.server.transport.email.EmailListenerManager;
import com.l7tech.util.DomUtils;
import com.l7tech.util.Functions.UnaryVoidThrows;
import com.l7tech.util.MissingRequiredElementException;
import com.l7tech.xml.soap.SoapUtil;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import java.text.MessageFormat;

import static org.junit.Assert.*;

/**
 * Test the GatewayManagementAssertion for EmailListenerMO entity.
 */
@RunWith(MockitoJUnitRunner.class)
public class ServerGatewayManagementAssertionTestEmail extends ServerGatewayManagementAssertionTestBase {

    private static final String RESOURCE_URI = "http://ns.l7tech.com/2010/04/gateway-management/emailListeners";
    private EmailListenerManager emailListenerManager;

    //- PUBLIC
    @Test
    public void testCreate() throws Exception {
        String payload =
                "<l7:EmailListener xmlns:l7=\"http://ns.l7tech.com/2010/04/gateway-management\" id=\"7eb12b2c2e5c3bba725bb9577cad21fc\" version=\"0\">\n" +
                        "    <l7:Name>qwert</l7:Name>\n" +
                        "    <l7:Active>false</l7:Active>\n" +
                        "    <l7:Hostname>asdf</l7:Hostname>\n" +
                        "    <l7:Port>993</l7:Port>\n" +
                        "    <l7:ServerType>IMAP</l7:ServerType>\n" +
                        "    <l7:UseSsl>true</l7:UseSsl>\n" +
                        "    <l7:DeleteOnReceive>true</l7:DeleteOnReceive>\n" +
                        "    <l7:Username>asdf</l7:Username>\n" +
                        "    <l7:Password>asdf</l7:Password>\n" +
                        "    <l7:Folder>asedf</l7:Folder>\n" +
                        "    <l7:PollInterval>60</l7:PollInterval>\n" +
                        "    <l7:Properties>\n" +
                        "        <l7:Property key=\"com.l7tech.server.jms.prop.hardwired.service.id\">\n" +
                        "            <l7:StringValue>"+new Goid(0,2).toString()+"</l7:StringValue>\n" +
                        "        </l7:Property>\n" +
                        "        <l7:Property key=\"com.l7tech.server.jms.prop.hardwired.service.bool\">\n" +
                        "            <l7:StringValue>true</l7:StringValue>\n" +
                        "        </l7:Property>\n" +
                        "        <l7:Property key=\"com.l7tech.server.email.prop.request.sizeLimit\">\n" +
                        "            <l7:StringValue>2621440</l7:StringValue>\n" +
                        "        </l7:Property>\n" +
                        "    </l7:Properties>\n" +
                        "</l7:EmailListener>";
        doCreate( RESOURCE_URI, payload, new Goid(0,2).toString(), new Goid(0,3).toString() );
    }

    @Test
    public void testPut() throws Exception {
        final Goid goid = new Goid(0, 1);
        final String message =
              "<s:Envelope xmlns:s=\"http://www.w3.org/2003/05/soap-envelope\" xmlns:wsa=\"http://schemas.xmlsoap.org/ws/2004/08/addressing\" xmlns:wsman=\"http://schemas.dmtf.org/wbem/wsman/1/wsman.xsd\" xmlns:l7=\"http://ns.l7tech.com/2010/04/gateway-management\">" +
              "    <s:Header>\n" +
              "        <wsa:Action s:mustUnderstand=\"true\">http://schemas.xmlsoap.org/ws/2004/09/transfer/Put</wsa:Action>\n" +
              "        <wsa:To s:mustUnderstand=\"true\">http://127.0.0.1:8080/wsman</wsa:To>\n" +
              "        <wsman:ResourceURI s:mustUnderstand=\"true\">"+RESOURCE_URI+"</wsman:ResourceURI>\n" +
              "        <wsa:MessageID s:mustUnderstand=\"true\">uuid:afad2993-7d39-1d39-8002-481688002100</wsa:MessageID>\n" +
              "        <wsa:ReplyTo>\n" +
              "            <wsa:Address>http://schemas.xmlsoap.org/ws/2004/08/addressing/role/anonymous</wsa:Address>\n" +
              "        </wsa:ReplyTo>\n" +
              "        <wsman:SelectorSet>\n" +
              "            <wsman:Selector Name=\"id\">"+ goid.toString()+"</wsman:Selector>\n" +
              "        </wsman:SelectorSet>\n" +
              "    </s:Header>\n" +
              "    <s:Body>" +
              "        <l7:EmailListener id=\"00000000000000000000000000000001\" version=\"0\">\n" +
              "            <l7:Name>New Name</l7:Name>\n" +
              "            <l7:Active>false</l7:Active>\n" +
              "            <l7:Hostname>newHost</l7:Hostname>\n" +
              "            <l7:Port>9876</l7:Port>\n" +
              "            <l7:ServerType>IMAP</l7:ServerType>\n" +
              "            <l7:UseSsl>true</l7:UseSsl>\n" +
              "            <l7:DeleteOnReceive>false</l7:DeleteOnReceive>\n" +
              "            <l7:Username>admin</l7:Username>\n" +
              "            <l7:PollInterval>60</l7:PollInterval>\n" +
              "            <l7:Properties/>\n" +
              "        </l7:EmailListener>" +
              "    </s:Body>" +
              "</s:Envelope>";

        final UnaryVoidThrows<Document,Exception> verifier = new UnaryVoidThrows<Document,Exception>(){
            private int expectedVersion = 1;

            @Override
            public void call( final Document result ) throws Exception {
                final Element soapBody = SoapUtil.getBodyElement(result);
                final Element emailListener = XmlUtil.findExactlyOneChildElementByName(soapBody, NS_GATEWAY_MANAGEMENT, "EmailListener");
                final Element listerName = XmlUtil.findExactlyOneChildElementByName(emailListener, NS_GATEWAY_MANAGEMENT, "Name");
                final Element listerActive = XmlUtil.findExactlyOneChildElementByName(emailListener, NS_GATEWAY_MANAGEMENT, "Active");
                final Element listerHostname = XmlUtil.findExactlyOneChildElementByName(emailListener, NS_GATEWAY_MANAGEMENT, "Hostname");
                final Element listerUsername = XmlUtil.findExactlyOneChildElementByName(emailListener, NS_GATEWAY_MANAGEMENT, "Username");

                assertEquals("Email Listener id", goid.toString(), emailListener.getAttribute( "id" ));
                assertEquals("Email Listener version", Integer.toString( expectedVersion++ ), emailListener.getAttribute( "version" ));
                assertEquals("Email Listener name", "New Name", XmlUtil.getTextValue(listerName));
                assertEquals("Email Listener active", "false", XmlUtil.getTextValue(listerActive));
                assertEquals("Email Listener hostname", "newHost", XmlUtil.getTextValue(listerHostname));
                assertEquals("Email Listener username", "admin", XmlUtil.getTextValue(listerUsername));

                try{
                    XmlUtil.findExactlyOneChildElementByName(emailListener, NS_GATEWAY_MANAGEMENT, "Password");
                    fail("Password element found");
                }catch(MissingRequiredElementException e){
                    EmailListener listener = emailListenerManager.findByPrimaryKey(goid);
                    assertNotNull("Password not preserved",listener.getPassword());
                }

            }
        };

        putAndVerify( message, verifier, false );
        putAndVerify( message, verifier, true );
    }

    @Test
    public void testGetById() throws Exception {
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
                        "    <w:ResourceURI s:mustUnderstand=\"true\">"+RESOURCE_URI+"</w:ResourceURI> \n" +
                        "    <w:SelectorSet>\n" +
                        "      <w:Selector Name=\"id\">"+idStr+"</w:Selector> \n" +
                        "    </w:SelectorSet>\n" +
                        "    <w:OperationTimeout>PT60.000S</w:OperationTimeout> \n" +
                        "  </s:Header>\n" +
                        "  <s:Body/> \n" +
                        "</s:Envelope>";

        final Document result = processRequest("http://schemas.xmlsoap.org/ws/2004/09/transfer/Get", message);
        final Element soapBody = SoapUtil.getBodyElement(result);
        final Element siteminderConfigurationElm = XmlUtil.findExactlyOneChildElementByName(soapBody, NS_GATEWAY_MANAGEMENT, "EmailListener");
        final Element nameElm = XmlUtil.findExactlyOneChildElementByName(siteminderConfigurationElm, NS_GATEWAY_MANAGEMENT, "Name");
        final Element hostElm = XmlUtil.findExactlyOneChildElementByName(siteminderConfigurationElm, NS_GATEWAY_MANAGEMENT, "Hostname");

        assertEquals("emailListener1", DomUtils.getTextValue(nameElm));
        assertEquals("host", DomUtils.getTextValue(hostElm));
    }

    @Test
    public void testGetByName() throws Exception {
        final String nameStr = "emailListener1";
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
                        "    <w:ResourceURI s:mustUnderstand=\"true\">"+RESOURCE_URI+"</w:ResourceURI> \n" +
                        "    <w:SelectorSet>\n" +
                        "      <w:Selector Name=\"name\">"+nameStr+"</w:Selector> \n" +
                        "    </w:SelectorSet>\n" +
                        "    <w:OperationTimeout>PT60.000S</w:OperationTimeout> \n" +
                        "  </s:Header>\n" +
                        "  <s:Body/> \n" +
                        "</s:Envelope>";

        final Document result = processRequest("http://schemas.xmlsoap.org/ws/2004/09/transfer/Get", message);
        final Element soapBody = SoapUtil.getBodyElement(result);
        final Element siteminderConfigurationElm = XmlUtil.findExactlyOneChildElementByName(soapBody, NS_GATEWAY_MANAGEMENT, "EmailListener");
        final Element nameElm = XmlUtil.findExactlyOneChildElementByName(siteminderConfigurationElm, NS_GATEWAY_MANAGEMENT, "Name");
        final Element portElm = XmlUtil.findExactlyOneChildElementByName(siteminderConfigurationElm, NS_GATEWAY_MANAGEMENT, "Port");

        assertEquals("emailListener1", DomUtils.getTextValue(nameElm));
        assertEquals("1234", DomUtils.getTextValue(portElm));
    }


    @Test
    public void testDelete() throws Exception {
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
                        "    <w:ResourceURI s:mustUnderstand=\"true\">"+RESOURCE_URI+"</w:ResourceURI> \n" +
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
    public void testEnumerateAll() throws Exception {
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

        final Document result = processRequest( "http://schemas.xmlsoap.org/ws/2004/09/enumeration/Enumerate", MessageFormat.format(message, RESOURCE_URI) );

        final Element soapBody = SoapUtil.getBodyElement(result);
        final Element enumerateResponse = XmlUtil.findExactlyOneChildElementByName(soapBody, NS_WS_ENUMERATION, "EnumerateResponse");
        final Element enumerationContext = XmlUtil.findExactlyOneChildElementByName(enumerateResponse, NS_WS_ENUMERATION, "EnumerationContext");

        final String context = XmlUtil.getTextValue(enumerationContext);

        assertNotNull("Valid enumeration context " + RESOURCE_URI, context);
        assertFalse("Valid enumeration context " + RESOURCE_URI, context.trim().isEmpty());

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

        final Document pullResult = processRequest( "http://schemas.xmlsoap.org/ws/2004/09/enumeration/Pull", MessageFormat.format(pullMessage, RESOURCE_URI, context));

        final Element soapBody2 = SoapUtil.getBodyElement(pullResult);
        final Element pullResponse = XmlUtil.findExactlyOneChildElementByName(soapBody2, NS_WS_ENUMERATION, "PullResponse");
        final Element items = XmlUtil.findExactlyOneChildElementByName(pullResponse, NS_WS_ENUMERATION, "Items");
        assertTrue( "Enumeration not empty " + RESOURCE_URI, items.hasChildNodes() );
        XmlUtil.findExactlyOneChildElementByName(pullResponse, NS_WS_ENUMERATION, "EndOfSequence");
    }

    //- PROTECTED

    protected void moreInit() throws SaveException {
        final EmailListener emailListener = new EmailListener(EmailServerType.POP3);
        emailListener.setGoid(new Goid(0, 1));
        emailListener.setName("emailListener1");
        emailListener.setHost("host");
        emailListener.setPort(1234);
        emailListener.setUsername("username");
        emailListener.setPassword("password");

        emailListenerManager = applicationContext.getBean("emailListenerManager", EmailListenerManager.class);
        emailListenerManager.save(emailListener);
    }
}
