package com.l7tech.external.assertions.gatewaymanagement.server;

import com.l7tech.common.http.HttpMethod;
import com.l7tech.common.mime.ContentTypeHeader;
import com.l7tech.external.assertions.gatewaymanagement.RESTGatewayManagementAssertion;
import com.l7tech.gateway.common.service.PublishedService;
import com.l7tech.message.HttpRequestKnob;
import com.l7tech.message.HttpServletRequestKnob;
import com.l7tech.message.HttpServletResponseKnob;
import com.l7tech.message.Message;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.server.message.PolicyEnforcementContextFactory;
import com.l7tech.util.Pair;
import org.junit.Ignore;
import org.junit.Test;
import org.springframework.beans.factory.support.StaticListableBeanFactory;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockServletContext;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertTrue;
import static junit.framework.Assert.fail;

public class ServerRESTGatewayManagementAssertionTest {

    private String testURI = "resource/selector";
    @Test
    public void testContextVariableURI() throws Exception{
        RESTGatewayManagementAssertion ass = new RESTGatewayManagementAssertion();

        PolicyEnforcementContext pec = getPolicyEnforcementContext( getMockServletRequest());
        pec.setVariable(ass.getVariablePrefix()+"."+RESTGatewayManagementAssertion.SUFFIX_URI,testURI );

        String returnURI =  ServerRESTGatewayManagementAssertion.getURI(pec,pec.getRequest(),ass);
        assertEquals(returnURI,testURI);
    }

    @Test
    public void testMessageURI() throws Exception {
        RESTGatewayManagementAssertion ass = new RESTGatewayManagementAssertion();

        String routingURI = "/service/";
        MockHttpServletRequest servletRequest = getMockServletRequest();
        servletRequest.setRequestURI(routingURI+testURI);

        PolicyEnforcementContext pec = getPolicyEnforcementContext( servletRequest);
        pec.setService(getService(routingURI + "*"));

        String returnURI =  ServerRESTGatewayManagementAssertion.getURI(pec,pec.getRequest(),ass);
        assertEquals(testURI, returnURI);
    }

    private PublishedService getService(String routingURI) {
        PublishedService service = new PublishedService();
        service.setRoutingUri(routingURI);
        return service;
    }

    @Test
    public  void testURIResolution(){
        Pair<String, String> resourceInput = ServerRESTGatewayManagementAssertion.getResourceInputs("resource/selector");
        assertEquals("resource",resourceInput.left);
        assertEquals("selector",resourceInput.right);

        resourceInput = ServerRESTGatewayManagementAssertion.getResourceInputs("resource/sel/ec/tor");
        assertEquals("resource",resourceInput.left);
        assertEquals("sel/ec/tor",resourceInput.right);

        resourceInput = ServerRESTGatewayManagementAssertion.getResourceInputs("resource");
        assertEquals("resource",resourceInput.left);
        assertEquals(null,resourceInput.right);
    }

    @Test
    public void testContextVariableAction()throws Exception{
        RESTGatewayManagementAssertion ass = new RESTGatewayManagementAssertion();

        // should ignore value from request message
        MockHttpServletRequest servletRequest = getMockServletRequest();
        servletRequest.setMethod(HttpMethod.OTHER.toString());
        PolicyEnforcementContext pec = getPolicyEnforcementContext(servletRequest );

        pec.setVariable(ass.getVariablePrefix()+"."+RESTGatewayManagementAssertion.SUFFIX_ACTION, "GET" );
        HttpMethod method =  ServerRESTGatewayManagementAssertion.getAction(pec, pec.getRequest(), ass);
        assertEquals(HttpMethod.GET,method);

        pec.setVariable(ass.getVariablePrefix()+"."+RESTGatewayManagementAssertion.SUFFIX_ACTION, "DELETE" );
        method =  ServerRESTGatewayManagementAssertion.getAction(pec, pec.getRequest(), ass);
        assertEquals(HttpMethod.DELETE,method);

        pec.setVariable(ass.getVariablePrefix()+"."+RESTGatewayManagementAssertion.SUFFIX_ACTION, "POST" );
        method =  ServerRESTGatewayManagementAssertion.getAction(pec, pec.getRequest(), ass);
        assertEquals(HttpMethod.POST,method);
    }


    @Test
    public  void testMessageAction()throws Exception{
        RESTGatewayManagementAssertion ass = new RESTGatewayManagementAssertion();

        MockHttpServletRequest servletRequest = getMockServletRequest();
        servletRequest.setMethod(HttpMethod.GET.toString());
        PolicyEnforcementContext pec = getPolicyEnforcementContext(servletRequest );

        HttpMethod method =  ServerRESTGatewayManagementAssertion.getAction(pec, pec.getRequest(), ass);
        assertEquals(HttpMethod.GET,method);

        servletRequest.setMethod(HttpMethod.POST.toString());
        pec = getPolicyEnforcementContext(servletRequest );
        method =  ServerRESTGatewayManagementAssertion.getAction(pec, pec.getRequest(), ass);
        assertEquals(HttpMethod.POST,method);

        servletRequest.setMethod(HttpMethod.DELETE.toString());
        pec = getPolicyEnforcementContext(servletRequest );
        method =  ServerRESTGatewayManagementAssertion.getAction(pec, pec.getRequest(), ass);
        assertEquals(HttpMethod.DELETE,method);

        servletRequest.setMethod(HttpMethod.OTHER.toString());
        pec = getPolicyEnforcementContext(servletRequest );
        try{
            method =  ServerRESTGatewayManagementAssertion.getAction(pec, pec.getRequest(), ass);
            fail("Exception expected");
        }catch (IllegalArgumentException e){
            // do nothing, expected
        }

        servletRequest.setMethod("asdf");
        pec = getPolicyEnforcementContext(servletRequest );
        try{
            method =  ServerRESTGatewayManagementAssertion.getAction(pec, pec.getRequest(), ass);
            fail("Exception expected");
        }catch (IllegalArgumentException e){
            // do nothing, expected
        }
    }

    @Ignore
    @Test
    public void testContextVariableRequestDocument()throws Exception{
        // todo
    }

    @Ignore
    @Test
    public void testMessageRequestDocument()throws Exception{
        // todo
    }


    private PolicyEnforcementContext getPolicyEnforcementContext(HttpServletRequest servletRequest) throws IOException {
        final String contentType = ContentTypeHeader.SOAP_1_2_DEFAULT.getFullValue();
        final Message request = new Message();
        request.initialize( ContentTypeHeader.parseValue(contentType) , new byte[0]);
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
