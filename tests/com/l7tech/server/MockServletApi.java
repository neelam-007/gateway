package com.l7tech.server;

import com.l7tech.common.protocol.SecureSpanConstants;
import com.l7tech.common.util.SoapUtil;
import com.l7tech.service.PublishedService;
import org.springframework.context.ApplicationContext;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockServletConfig;
import org.springframework.mock.web.MockServletContext;
import org.springframework.web.context.ContextLoader;
import org.springframework.web.context.support.WebApplicationContextUtils;

import javax.xml.soap.SOAPException;
import javax.xml.soap.SOAPMessage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

/**
 * Class <code>MockServletApi</code> creates the mock servlet API
 * for testing.
 *
 * @author <a href="mailto:emarceta@layer7-tech.com">Emil Marceta</a>
 */
public class MockServletApi {
    MockServletContext servletContextMock = new MockServletContext();
    MockServletConfig servletConfigMock = new MockServletConfig(servletContextMock);
    MockHttpServletRequest servletRequestMock;
    MockHttpServletResponse servletResponseMock;
    protected ApplicationContext applicationContext;

    /**
     * instantiate the <code>MockServletApi</code> using the
     * default preparer
     *
     * @param contextLocation
     */
    public static MockServletApi defaultMessageProcessingServletApi(String contextLocation) {
        MockServletApi ma = new MockServletApi(contextLocation);
        return ma;
    }

    public void reset() {
        servletRequestMock = new MockHttpServletRequest(servletContextMock);
        servletResponseMock = new MockHttpServletResponse();
        servletRequestMock.addHeader("Authorization", new String[] {});
    }

    private MockServletApi(String contextLocation) {
        servletContextMock.addInitParameter(ContextLoader.CONFIG_LOCATION_PARAM, contextLocation);
        ContextLoader loader = new ContextLoader();
        loader.initWebApplicationContext(servletContextMock);
        reset();
    }

    public ApplicationContext getApplicationContext() {
        return WebApplicationContextUtils.getWebApplicationContext(servletContextMock);
    }


    /**
     * set the puiblished service associated with this request and neccessary
     * parameters version etc.
     *
     * @param service the published service to set
     */
    public void setPublishedService(PublishedService service) {
        final String routingUri = service.getRoutingUri();
        if (routingUri !=null) {
            servletRequestMock.addHeader(SecureSpanConstants.HttpHeaders.ORIGINAL_URL, routingUri);
        }
        String version = "" + service.getOid() + "|" + service.getVersion();
        servletRequestMock.addHeader(SecureSpanConstants.HttpHeaders.POLICY_VERSION, version);
    }

    /**
     * set the soiap requesty and the soap action to process
     *
     * @param soapMessage the soap message
     * @param soapAction  the soap action
     * @throws IOException   thrown on io error
     * @throws SOAPException thrown on soap related error
     */
    public void setSoapRequest(SOAPMessage soapMessage, String soapAction) throws IOException, SOAPException {
        ByteArrayOutputStream bo = new ByteArrayOutputStream();
        soapMessage.writeTo(bo);
        servletRequestMock.setContent(bo.toByteArray());
        servletRequestMock.addHeader(SecureSpanConstants.HttpHeaders.ORIGINAL_URL, "http://localhost/whatever");
        servletRequestMock.addHeader(SoapUtil.SOAPACTION, "urn:soapaction.com:whoopee");
    }

    /**
     * get the servlet response mock
     *
     * @return the servlet response mock
     */
    public MockHttpServletResponse getServletResponse() {
        return servletResponseMock;
    }

    /**
     * get the servlet context mock
     *
     * @return the servlet context mock
     */
    public MockServletContext getServletContext() {
        return servletContextMock;
    }

    /**
     * return the servlet request mock
     *
     * @return the servlet request mock
     */
    public MockHttpServletRequest getServletRequest() {
        return servletRequestMock;
    }

    /**
     * return the servlet config mock
     *
     * @return the servlet config mock
     */
    public MockServletConfig getServletConfig() {
        return servletConfigMock;
    }
}
