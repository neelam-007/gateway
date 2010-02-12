package com.l7tech.external.assertions.wsmanagement.server;

import com.l7tech.external.assertions.wsmanagement.WsManagementAssertion;
import com.l7tech.message.Message;
import com.l7tech.message.HttpRequestKnob;
import com.l7tech.message.HttpServletRequestKnob;
import com.l7tech.message.HttpServletResponseKnob;
import com.l7tech.common.mime.ContentTypeHeader;
import com.l7tech.common.io.XmlUtil;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.server.service.ServiceManagerStub;
import com.l7tech.gateway.common.service.PublishedService;
import org.junit.Test;
import org.springframework.mock.web.MockServletContext;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.beans.factory.support.StaticListableBeanFactory;

/**
 * Test the WsManagementAssertion.
 */
public class ServerWsManagementAssertionTest {

    private static final StaticListableBeanFactory beanFactory = new StaticListableBeanFactory();
    static {
        beanFactory.addBean( "serviceManager", new ServiceManagerStub( null, new PublishedService(){
            {
                setOid(1);
                setName( "Test Service" );
                setDisabled( false );
                setSoap( true );
            }
        }) );
    }

    @Test
    public void testInvoke() throws Exception {
        String contentType = ContentTypeHeader.SOAP_1_2_DEFAULT.getFullValue() + "; action=\"http://schemas.xmlsoap.org/ws/2004/09/transfer/Get\"";
        String message =
                "<s:Envelope xmlns:s=\"http://www.w3.org/2003/05/soap-envelope\" \n" +
                "            xmlns:a=\"http://schemas.xmlsoap.org/ws/2004/08/addressing\" \n" +
                "            xmlns:w=\"http://schemas.dmtf.org/wbem/wsman/1/wsman.xsd\">\n" +
                "  <s:Header>\n" +
                "    <a:MessageID>uuid:4ED2993C-4339-4E99-81FC-C2FD3812781A</a:MessageID> \n" +
                "    <a:To>http://127.0.0.1:8080/wsman</a:To> \n" +
                "    <a:ReplyTo> \n" +
                "        <a:Address s:mustUnderstand=\"true\">http://schemas.xmlsoap.org/ws/2004/08/addressing/role/anonymous</a:Address> \n" +
                "    </a:ReplyTo> \n" +
                "    <a:Action s:mustUnderstand=\"true\">http://schemas.xmlsoap.org/ws/2004/09/transfer/Get</a:Action> \n" +
                "    <w:ResourceURI s:mustUnderstand=\"true\">http://www.layer7tech.com/management/services</w:ResourceURI> \n" +
                "    <w:SelectorSet>\n" +
                "    <w:Selector Name=\"serviceoid\">1</w:Selector> \n" +
                "    </w:SelectorSet>\n" +
                "    <w:OperationTimeout>PT60.000S</w:OperationTimeout> \n" +
                "  </s:Header>\n" +
                "  <s:Body/> \n" +
                "</s:Envelope>";
        Message request = new Message();
        request.initialize( ContentTypeHeader.parseValue(contentType) , message.getBytes( "utf-8" ));
        Message response = new Message();

        MockServletContext servletContext = new MockServletContext();
        MockHttpServletRequest hrequest = new MockHttpServletRequest(servletContext);
        MockHttpServletResponse hresponse = new MockHttpServletResponse();

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

        PolicyEnforcementContext context = new PolicyEnforcementContext(request, response);
        ServerWsManagementAssertion swma = new ServerWsManagementAssertion( new WsManagementAssertion(), beanFactory );
        swma.checkRequest( context );

        System.out.println( XmlUtil.nodeToFormattedString( XmlUtil.parse( hresponse.getContentAsString( ) ) ) );
    }

}
