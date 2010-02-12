package com.l7tech.external.assertions.wsmanagement.server;

import com.l7tech.external.assertions.wsmanagement.WsManagementAssertion;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.PolicyAssertionException;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.server.policy.assertion.AbstractServerAssertion;
import com.l7tech.server.service.ServiceManager;
import com.l7tech.message.HttpServletRequestKnob;
import com.l7tech.message.HttpServletResponseKnob;
import com.l7tech.util.ExceptionUtils;
import com.l7tech.util.ResourceUtils;
import com.sun.ws.management.transport.ContentType;
import com.sun.ws.management.Management;
import com.sun.ws.management.Message;
import com.sun.ws.management.identify.Identify;
import com.sun.ws.management.soap.SOAP;
import com.sun.ws.management.server.HandlerContext;
import com.sun.ws.management.server.HandlerContextImpl;
import com.sun.ws.management.server.WSManAgent;
import com.sun.ws.management.server.reflective.WSManReflectiveAgent;
import org.springframework.beans.factory.BeanFactory;
import org.xml.sax.SAXException;

import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.xml.soap.SOAPException;
import javax.xml.bind.JAXBException;
import java.io.IOException;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.Map;
import java.util.HashMap;
import java.security.Principal;

/**
 * Server side implementation of the WsManagementAssertion.
 *
 * @see com.l7tech.external.assertions.wsmanagement.WsManagementAssertion
 */
public class ServerWsManagementAssertion extends AbstractServerAssertion<WsManagementAssertion> {

    //- PUBLIC

    public ServerWsManagementAssertion( final WsManagementAssertion assertion,
                                        final BeanFactory context ) throws PolicyAssertionException {
        super(assertion);

        this.assertion = assertion;
        this.agent = buildAgent();
        this.serviceManager = (ServiceManager) context.getBean( "serviceManager", ServiceManager.class );
    }

    @Override
    public AssertionStatus checkRequest( final PolicyEnforcementContext context ) throws IOException, PolicyAssertionException {

        final HttpServletRequest req = ((HttpServletRequestKnob)context.getRequest().getHttpRequestKnob()).getHttpServletRequest();
        final HttpServletResponse resp = ((HttpServletResponseKnob)context.getResponse().getHttpResponseKnob()).getHttpServletResponse();
        final String reqCT = req.getContentType();
        final ContentType contentType = ContentType.createFromHttpContentType(reqCT);
		if (contentType == null || !contentType.isAcceptable()) {
            logger.log( Level.WARNING, "Content-Type not supported : " + reqCT);
			return AssertionStatus.FALSIFIED;
		}

        resp.setStatus(HttpServletResponse.SC_OK);
        resp.setContentType(contentType.toString());
        final ByteArrayOutputStream bos = new ByteArrayOutputStream();

        InputStream is = null;
        OutputStream os = null;
        final ClassLoader contextClassLoader = Thread.currentThread().getContextClassLoader();
        Thread.currentThread().setContextClassLoader( assertion.getClass().getClassLoader() );
        try {
            is = new BufferedInputStream(req.getInputStream());
            os = new BufferedOutputStream(resp.getOutputStream());
            handle(is, contentType, bos, req, resp);
            final byte[] content = bos.toByteArray();
            resp.setContentLength(content.length);
            os.write(content);
        } catch (Throwable th) {
            logger.log( Level.WARNING, ExceptionUtils.getMessage(th), th );
        } finally {
            Thread.currentThread().setContextClassLoader( contextClassLoader );
            ResourceUtils.closeQuietly( is );
            ResourceUtils.closeQuietly( os );
        }

        return AssertionStatus.NONE;
    }

    //- PRIVATE

    private static final Logger logger = Logger.getLogger(ServerWsManagementAssertion.class.getName());

    private final WsManagementAssertion assertion;
    private final ServiceManager serviceManager;
    private final WSManAgent agent;

    private WSManReflectiveAgent buildAgent() throws PolicyAssertionException {
        try {
            return new WSManReflectiveAgent();
        } catch (SAXException e) {
            throw new PolicyAssertionException( assertion, "Error creating WS-Management agent '"+ExceptionUtils.getMessage( e )+"'", e );
        }
    }

    private void handle( final InputStream is,
                         final ContentType contentType,
                         final OutputStream os,
                         final HttpServletRequest req,
                         final HttpServletResponse resp)
            throws SOAPException, JAXBException, IOException {

        final Management request = new Management(is);
        request.setXmlBinding(agent.getXmlBinding());

        request.setContentType(contentType);

        String contentype = req.getContentType();
        final Principal user = req.getUserPrincipal();
        String charEncoding = req.getCharacterEncoding();
        String url = req.getRequestURL().toString();
        Map<String, Object> props = new HashMap<String, Object>(1);
        props.put( "com.sun.ws.management.server.handler", "com.l7tech.external.assertions.wsmanagment.invalidhandlers" ); 
        props.put( "com.l7tech.serviceManager", serviceManager ); 
        final HandlerContext context = new HandlerContextImpl(user, contentype,
                charEncoding, url, props);

        Message response = agent.handleRequest(request, context);

        sendResponse(response, os, resp);
    }

    private static void sendResponse( final Message response,
                                      final OutputStream os,
                                      final HttpServletResponse resp )
            throws SOAPException, JAXBException, IOException {

        if(response instanceof Identify ) {
            response.writeTo(os);
            return;
        }

        Management mgtResp = (Management) response;

        sendResponse(mgtResp, os, resp);
    }

   private static void sendResponse( final Management response,
                                     final OutputStream os,
                                     final HttpServletResponse resp )
           throws SOAPException, JAXBException, IOException {

        resp.setStatus(HttpServletResponse.SC_OK);
        if (response.getBody().hasFault()) {
            // sender faults need to set error code to BAD_REQUEST for client errors
            if ( SOAP.SENDER.equals(response.getBody().getFault().getFaultCodeAsQName())) {
                resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            } else {
                resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            }
        }

        final ByteArrayOutputStream baos = new ByteArrayOutputStream();
        response.writeTo(baos);
        final byte[] content = baos.toByteArray();

        // response being null means that no reply is to be sent back.
        // The reply has been handled asynchronously
        if(os != null)
             os.write(content);
    }
}
