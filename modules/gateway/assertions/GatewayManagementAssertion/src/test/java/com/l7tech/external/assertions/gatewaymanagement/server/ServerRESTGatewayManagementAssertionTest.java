package com.l7tech.external.assertions.gatewaymanagement.server;

import com.l7tech.common.http.HttpMethod;
import com.l7tech.common.mime.ContentTypeHeader;
import com.l7tech.external.assertions.gatewaymanagement.RESTGatewayManagementAssertion;
import com.l7tech.gateway.common.audit.Audit;
import com.l7tech.gateway.common.audit.LoggingAudit;
import com.l7tech.gateway.common.service.PublishedService;
import com.l7tech.message.HttpRequestKnob;
import com.l7tech.message.HttpServletRequestKnob;
import com.l7tech.message.HttpServletResponseKnob;
import com.l7tech.message.Message;
import com.l7tech.policy.assertion.TargetMessageType;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.server.message.PolicyEnforcementContextFactory;
import org.junit.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockServletContext;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.net.URI;
import java.util.logging.Logger;

import static junit.framework.Assert.assertEquals;

public class ServerRESTGatewayManagementAssertionTest {

    private String testURI = "resource/selector";
    private static final Logger logger = Logger.getLogger(ServerRestGatewayManagementAssertionContextVariableTest.class.getName());

    private Audit auditor = new LoggingAudit(logger).getAuditor();

    @Test
    public void testContextVariableURI() throws Exception{
        RESTGatewayManagementAssertion ass = new RESTGatewayManagementAssertion();
        ass.setTarget(TargetMessageType.OTHER);
        ass.setOtherTargetMessageVariable("other");

        PolicyEnforcementContext pec = getPolicyEnforcementContext( getMockServletRequest(),null);
        pec.setVariable(ass.getVariablePrefix() + "." + RESTGatewayManagementAssertion.SUFFIX_URI, testURI);

        URI returnURI =  ServerRESTGatewayManagementAssertion.getURI(pec, pec.getVariableMap(ass.getVariablesUsed(),auditor ), pec.getRequest(), ass);
        assertEquals(testURI, returnURI.toString());

        pec.setVariable(ass.getVariablePrefix() + "." + RESTGatewayManagementAssertion.SUFFIX_URI, "/"+testURI);
        returnURI =  ServerRESTGatewayManagementAssertion.getURI(pec, pec.getVariableMap(ass.getVariablesUsed(),auditor ), pec.getRequest(), ass);
        assertEquals(testURI, returnURI.toString());
    }

    @Test
    public void testMessageURI() throws Exception {
        RESTGatewayManagementAssertion ass = new RESTGatewayManagementAssertion();

        String routingURI = "/service/";
        MockHttpServletRequest servletRequest = getMockServletRequest();
        servletRequest.setRequestURI(routingURI+testURI);

        PolicyEnforcementContext pec = getPolicyEnforcementContext( servletRequest,null);
        pec.setService(getService(routingURI + "*"));

        URI returnURI =  ServerRESTGatewayManagementAssertion.getURI(pec,pec.getVariableMap(ass.getVariablesUsed(),auditor ),pec.getRequest(),ass);
        assertEquals(testURI, returnURI.toString());
    }

    private PublishedService getService(String routingURI) {
        PublishedService service = new PublishedService();
        service.setRoutingUri(routingURI);
        return service;
    }

    @Test
    public void testContextVariableAction()throws Exception{
        RESTGatewayManagementAssertion ass = new RESTGatewayManagementAssertion();
        ass.setTarget(TargetMessageType.OTHER);
        ass.setOtherTargetMessageVariable("other");

        // should ignore value from request message
        MockHttpServletRequest servletRequest = getMockServletRequest();
        servletRequest.setMethod(HttpMethod.OTHER.toString());
        PolicyEnforcementContext pec = getPolicyEnforcementContext(servletRequest,null );

        pec.setVariable(ass.getVariablePrefix()+"."+RESTGatewayManagementAssertion.SUFFIX_ACTION, "GET" );
        HttpMethod method =  ServerRESTGatewayManagementAssertion.getAction(pec.getVariableMap(ass.getVariablesUsed(),auditor ), pec.getRequest(), ass);
        assertEquals(HttpMethod.GET,method);

        pec.setVariable(ass.getVariablePrefix()+"."+RESTGatewayManagementAssertion.SUFFIX_ACTION, "DELETE" );
        method =  ServerRESTGatewayManagementAssertion.getAction(pec.getVariableMap(ass.getVariablesUsed(),auditor ), pec.getRequest(), ass);
        assertEquals(HttpMethod.DELETE,method);

        pec.setVariable(ass.getVariablePrefix()+"."+RESTGatewayManagementAssertion.SUFFIX_ACTION, "POST" );
        method =  ServerRESTGatewayManagementAssertion.getAction(pec.getVariableMap(ass.getVariablesUsed(),auditor ), pec.getRequest(), ass);
        assertEquals(HttpMethod.POST,method);
    }


    @Test
    public  void testMessageAction()throws Exception{
        RESTGatewayManagementAssertion ass = new RESTGatewayManagementAssertion();

        MockHttpServletRequest servletRequest = getMockServletRequest();
        servletRequest.setMethod(HttpMethod.GET.toString());
        PolicyEnforcementContext pec = getPolicyEnforcementContext(servletRequest,null );

        HttpMethod method =  ServerRESTGatewayManagementAssertion.getAction(pec.getVariableMap(ass.getVariablesUsed(),auditor ), pec.getRequest(), ass);
        assertEquals(HttpMethod.GET,method);

        servletRequest.setMethod(HttpMethod.POST.toString());
        pec = getPolicyEnforcementContext(servletRequest,null );
        method =  ServerRESTGatewayManagementAssertion.getAction(pec.getVariableMap(ass.getVariablesUsed(),auditor ), pec.getRequest(), ass);
        assertEquals(HttpMethod.POST,method);

        servletRequest.setMethod(HttpMethod.DELETE.toString());
        pec = getPolicyEnforcementContext(servletRequest,null );
        method =  ServerRESTGatewayManagementAssertion.getAction(pec.getVariableMap(ass.getVariablesUsed(),auditor ), pec.getRequest(), ass);
        assertEquals(HttpMethod.DELETE,method);
    }

    private static final String body_xml =
            "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
            "<ClusterProperty id=\"a88ec121c86a9cd502c4748084b012d6\" version=\"0\">\n" +
            "   <Name>propertyName1</Name>\n" +
            "   <Value>hihi</Value>\n" +
            "</ClusterProperty>\n";

    private PolicyEnforcementContext getPolicyEnforcementContext(HttpServletRequest servletRequest, String requestMessage) throws IOException {
        final String contentType = ContentTypeHeader.SOAP_1_2_DEFAULT.getFullValue();
        final Message request = new Message();
        request.initialize( ContentTypeHeader.parseValue(contentType) , requestMessage==null? new byte[0]: requestMessage.getBytes( "utf-8" ));
        final Message response = new Message();

        final MockHttpServletResponse httpServletResponse = new MockHttpServletResponse();

        final HttpRequestKnob reqKnob = new HttpServletRequestKnob(servletRequest);
        request.attachHttpRequestKnob(reqKnob);

        final HttpServletResponseKnob respKnob = new HttpServletResponseKnob(httpServletResponse);
        response.attachHttpResponseKnob(respKnob);

        return PolicyEnforcementContextFactory.createPolicyEnforcementContext(request, response);
    }

    private MockHttpServletRequest getMockServletRequest()
    {
        final MockServletContext servletContext = new MockServletContext();
        servletContext.setContextPath( "/" );

        final MockHttpServletRequest httpServletRequest = new MockHttpServletRequest(servletContext);
        final String contentType = ContentTypeHeader.SOAP_1_2_DEFAULT.getFullValue();

        httpServletRequest.setMethod("POST");
        httpServletRequest.setContentType(contentType);
        httpServletRequest.addHeader("Content-Type", contentType);
        httpServletRequest.setRemoteAddr("127.0.0.1");
        httpServletRequest.setServerName( "127.0.0.1" );
        httpServletRequest.setRequestURI("/wsman");
//        httpServletRequest.setContent(message.getBytes("UTF-8"));

        return httpServletRequest;
    }


}
