package com.l7tech.server;

import com.l7tech.common.mime.MimeUtil;
import com.l7tech.common.protocol.SecureSpanConstants;
import com.l7tech.common.util.SoapUtil;
import com.l7tech.service.PublishedService;
import com.mockobjects.dynamic.C;
import com.mockobjects.dynamic.Mock;
import com.mockobjects.dynamic.ConstraintMatcher;
import com.mockobjects.servlet.MockServletInputStream;
import org.springframework.context.ApplicationContext;
import org.springframework.web.context.ContextLoader;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.context.support.WebApplicationContextUtils;

import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.soap.SOAPException;
import javax.xml.soap.SOAPMessage;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.StringReader;
import java.net.InetAddress;
import java.util.Enumeration;

/**
 * Class <code>MockServletApi</code> creates the mock servlet API
 * for testing.
 *
 * @author <a href="mailto:emarceta@layer7-tech.com">Emil Marceta</a>
 */
public class MockServletApi {
    Mock servletRequestMock = new Mock(HttpServletRequest.class);
    Mock servletResponseMock = new Mock(HttpServletResponse.class);
    Mock servletContextMock = new Mock(ServletContext.class);
    Mock servletConfigMock = new Mock(ServletConfig.class);
    protected ApplicationContext applicationContext;
    protected Preparer preparer;

    /**
     * instantiate the <code>MockServletApi</code> using the
     * default preparer
     * @param contextLocation
     */
    public static MockServletApi defaultMessageProcessingServletApi(String contextLocation) {
        MockServletApi ma = new MockServletApi(contextLocation);
        ma.preparer = ma.new DefaultMessageProcessorPreparer();
        ma.preparer.prepare(ma);
        return ma;
    }
    /**
     * instantiate the <code>MockServletApi</code> using the
     * user provided preparer
     */
    public MockServletApi(Preparer preparer) {
        preparer.prepare(this);
    }

    public void reset() {
        servletRequestMock = new Mock(HttpServletRequest.class);
        servletResponseMock = new Mock(HttpServletResponse.class);
        preparer.prepare(this);
    }

    private MockServletApi(String contextLocation) {
        servletConfigMock.matchAndReturn("getServletContext", servletContextMock.proxy());
        servletConfigMock.matchAndReturn("getInitParameter", C.IS_NOT_NULL, null);

        servletContextMock.matchAndReturn("getInitParameter", ContextLoader.CONFIG_LOCATION_PARAM, contextLocation);
        servletContextMock.matchAndReturn("getInitParameter", ContextLoader.CONTEXT_CLASS_PARAM, null);
        servletContextMock.matchAndReturn("getResourceAsStream", C.IS_ANYTHING, getClass().getClassLoader().getResourceAsStream(contextLocation));
        servletContextMock.matchAndReturn("log", C.ANY_ARGS, null);

        servletContextMock.expect("setAttribute", new ConstraintMatcher() {
            public boolean matches(Object[] args) {
                if (args.length !=2) {
                    return false;
                }
                if (WebApplicationContext.ROOT_WEB_APPLICATION_CONTEXT_ATTRIBUTE.equals(args[0])) {
                    applicationContext = (ApplicationContext)args[1];
                }
                servletContextMock.matchAndReturn("getAttribute", WebApplicationContext.ROOT_WEB_APPLICATION_CONTEXT_ATTRIBUTE, applicationContext);
                return true;
            }

            public Object[] getConstraints() {
                return new Object[0];
            }
        });
        ContextLoader loader = new ContextLoader();
        loader.initWebApplicationContext((ServletContext)servletContextMock.proxy());
    }

    public ApplicationContext getApplicationContext() {
        return WebApplicationContextUtils.getWebApplicationContext((ServletContext)servletContextMock.proxy());
    }

    public class DefaultMessageProcessorPreparer implements Preparer {
        public void prepare(MockServletApi servletApi) {
            try {
                servletRequestMock.matchAndReturn("getContentType", "text/xml");
                servletRequestMock.matchAndReturn("getStatus", new Integer(200));
                servletRequestMock.matchAndReturn("isSecure", true);

                // Authorization
                servletRequestMock.matchAndReturn("getHeader", "Authorization", null);
                servletRequestMock.matchAndReturn("getHeader", "WWW-Authenticate", null);

                servletRequestMock.matchAndReturn("getHeader", MimeUtil.CONTENT_TYPE, null);
                servletRequestMock.matchAndReturn("getScheme", "http");
                servletRequestMock.matchAndReturn("getServerName", InetAddress.getLocalHost().getHostName());
                servletRequestMock.matchAndReturn("getServerPort", 8080);
                servletRequestMock.matchAndReturn("getRemoteAddr", InetAddress.getLocalHost().getHostAddress());
                servletRequestMock.matchAndReturn("getRequestURL", new StringBuffer("/ssg/soap"));
                servletRequestMock.matchAndReturn("getRequestURI", "/ssg/soap");

                servletRequestMock.matchAndReturn("getContextPath", "/ssg");

                servletResponseMock.matchAndReturn("setContentType", "text/xml; charset=utf-8", null);
                servletResponseMock.matchAndReturn("setStatus", C.IS_ANYTHING, null);
                servletResponseMock.matchAndReturn("sendError", C.ANY_ARGS, null);
                servletResponseMock.matchAndReturn("setHeader", C.ANY_ARGS, null);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }

    /**
     * set the puiblished service associated with this request and neccessary
     * parameters version etc.
     *
     * @param service the published service to set
     */
    public void setPublishedService(PublishedService service) {
        servletRequestMock.matchAndReturn("getHeader", SecureSpanConstants.HttpHeaders.ORIGINAL_URL, service.getRoutingUri());
        /*
        String version = "" + service.getOid() + "|" + service.getVersion();
        servletRequestMock.matchAndReturn("getAttribute", Request.PARAM_HTTP_POLICY_VERSION, version);
        */
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
        StringReader sr = new StringReader(bo.toString());
        servletRequestMock.matchAndReturn("getReader", new BufferedReader(sr));
        servletRequestMock.matchAndReturn("getHeaders",
                                           SecureSpanConstants.HttpHeaders.ORIGINAL_URL,
                                           new HardcodedEnumeration(new String[] { "http://localhost/whatever"} ));
        servletRequestMock.matchAndReturn("getHeaders",
                                           SoapUtil.SOAPACTION,
                                           new HardcodedEnumeration(new String[] { "urn:soapaction.com:whoopee"} ));

        final MockServletInputStream mockServletInputStream = new MockServletInputStream();
        mockServletInputStream.setupRead(bo.toByteArray());
        servletRequestMock.matchAndReturn("getInputStream", mockServletInputStream);
    }

    /**
     * get the servlet response mock
     *
     * @return the servlet response mock
     */
    public Mock getServletResponseMock() {
        return servletResponseMock;
    }

    /**
     * get the servlet context mock
     *
     * @return the servlet context mock
     */
    public Mock getServletContextMock() {
        return servletContextMock;
    }

    /**
     * return the servlet request mock
     *
     * @return the servlet request mock
     */
    public Mock getServletRequestMock() {
        return servletRequestMock;
    }

    /**
     * return the servlet config mock
     *
     * @return the servlet config mock
     */
    public Mock getServletConfigMock() {
        return servletConfigMock;
    }

    /**
     * get the servlet request
     *
     * @return the servlet request
     */
    public HttpServletRequest getServletRequest() {
        return (HttpServletRequest)servletRequestMock.proxy();
    }

    /**
     * get the servlet response
     *
     * @return the servlet response
     */
    public HttpServletResponse getServletResponse() {
        return (HttpServletResponse)servletResponseMock.proxy();
    }

    /**
     * get the servlet config
     *
     * @return the servlet config
     */
    public ServletConfig getServletConfig() {
        return (ServletConfig)servletConfigMock.proxy();
    }

    /**
     * Implementatios prepare the servlet api (set the expected parameters and attributes)
     */
    public interface Preparer {
        void prepare(MockServletApi servletApi);
    }

    private static class HardcodedEnumeration implements Enumeration {
        public HardcodedEnumeration(Object[] values) {
            this.values = values;
        }

        private final Object[] values;
        int returned = 0;

        public boolean hasMoreElements() {
            return returned < values.length;
        }

        public Object nextElement() {
            return values[returned++];
        }
    }
}
