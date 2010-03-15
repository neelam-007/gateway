package com.l7tech.external.assertions.gatewaymanagement.server;

import com.l7tech.common.http.HttpConstants;
import com.l7tech.common.mime.ContentTypeHeader;
import com.l7tech.common.mime.NoSuchPartException;
import com.l7tech.external.assertions.gatewaymanagement.GatewayManagementAssertion;
import com.l7tech.gateway.api.impl.ValidationUtils;
import com.l7tech.gateway.common.audit.AssertionMessages;
import com.l7tech.message.HttpRequestKnob;
import com.l7tech.message.HttpResponseKnob;
import com.l7tech.message.Message;
import com.l7tech.message.MimeKnob;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.PolicyAssertionException;
import com.l7tech.policy.assertion.RoutingStatus;
import com.l7tech.server.audit.Auditor;
import com.l7tech.server.audit.LogOnlyAuditor;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.server.policy.assertion.AbstractServerAssertion;
import com.l7tech.server.policy.assertion.AssertionStatusException;
import com.l7tech.util.BuildInfo;
import com.l7tech.util.ExceptionUtils;
import com.l7tech.util.ResourceUtils;
import com.sun.ws.management.InternalErrorFault;
import com.sun.ws.management.addressing.Addressing;
import com.sun.ws.management.transport.ContentType;
import com.sun.ws.management.Management;
import com.sun.ws.management.identify.Identify;
import com.sun.ws.management.soap.SOAP;
import com.sun.ws.management.server.HandlerContext;
import com.sun.ws.management.server.HandlerContextImpl;
import com.sun.ws.management.server.WSManAgent;
import com.sun.ws.management.server.reflective.WSManReflectiveAgent;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.xml.XmlBeanFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.core.io.ClassPathResource;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;
import org.xmlsoap.schemas.ws._2004._08.addressing.EndpointReferenceType;

import javax.servlet.http.HttpServletResponse;
import javax.xml.soap.SOAPElement;
import javax.xml.soap.SOAPException;
import javax.xml.bind.JAXBException;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Iterator;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.Map;
import java.util.HashMap;
import java.security.Principal;

/**
 * Server side implementation of the GatewayManagementAssertion.
 *
 * @see GatewayManagementAssertion
 */
public class ServerGatewayManagementAssertion extends AbstractServerAssertion<GatewayManagementAssertion> {

    //- PUBLIC

    public ServerGatewayManagementAssertion( final GatewayManagementAssertion assertion,
                                             final BeanFactory context ) throws PolicyAssertionException {
        this( assertion, context, "gatewayManagementContext.xml" );
    }

    @Override
    public AssertionStatus checkRequest( final PolicyEnforcementContext context ) throws IOException, PolicyAssertionException {
        final Message request = context.getRequest();
        final Message response = context.getResponse();
        final MimeKnob mimeKnob = request.getMimeKnob();

        // Validate content type
        final String contentTypeText = mimeKnob.getOuterContentType().getFullValue();
        final ContentType contentType = ContentType.createFromHttpContentType(contentTypeText);
		if (contentType == null || !contentType.isAcceptable()) {
            auditor.logAndAudit( AssertionMessages.GATEWAYMANAGEMENT_ERROR, "Content-Type not supported : " + contentTypeText );
			return AssertionStatus.FALSIFIED;
		}

        //
        final ClassLoader contextClassLoader = Thread.currentThread().getContextClassLoader();
        Thread.currentThread().setContextClassLoader( assertion.getClass().getClassLoader() );
        try {
            return handle( context, contentType, request, response );
        } finally {
            Thread.currentThread().setContextClassLoader( contextClassLoader );
        }
    }

    //- PROTECTED

    protected ServerGatewayManagementAssertion( final GatewayManagementAssertion assertion,
                                                final BeanFactory context,
                                                final String assertionContextResource ) throws PolicyAssertionException {
        super(assertion);

        this.auditor = context instanceof ApplicationContext ?
                new Auditor( this, (ApplicationContext)context, logger ) :
                new LogOnlyAuditor( logger );
        this.assertion = assertion;
        this.agent = buildAgent();
        this.assertionContext = new XmlBeanFactory(new ClassPathResource(assertionContextResource, ServerGatewayManagementAssertion.class), context);
        this.assertionContext.preInstantiateSingletons();
    }    

    //- PRIVATE

    private static final Logger logger = Logger.getLogger(ServerGatewayManagementAssertion.class.getName());

    private final Auditor auditor;
    private final GatewayManagementAssertion assertion;
    private final XmlBeanFactory assertionContext;
    private final WSManAgent agent;

    private WSManReflectiveAgent buildAgent() throws PolicyAssertionException {
        try {
            return new WSManReflectiveAgent(null, ValidationUtils.getSchemaSources(), null){
                @SuppressWarnings({"ThrowableInstanceNeverThrown"})
                @Override
                public com.sun.ws.management.Message handleRequest( final Management managementRequest, final HandlerContext context ) {
                    com.sun.ws.management.Message response;

                    managementRequest.setXmlBinding( getXmlBinding() );
                    Management request = ensureSOAPFormat(managementRequest);

                    try {
                        validateAddressing( request );
                        response = processForIdentify( request );
                    } catch (Throwable th) {
                        if ( th instanceof AssertionStatusException ) throw (AssertionStatusException) th;
                        try {
                            Management managementResponse = new Management();
                            managementResponse.setFault(new InternalErrorFault(SOAP.createFaultDetail(ExceptionUtils.getMessage(th), null, ExceptionUtils.getDebugException(th), null)));
                            response = managementResponse;
                        } catch ( Exception e ) {
                            throw ExceptionUtils.wrap( e );
                        }
                    }

                    if ( response == null ) {
                        response = super.handleRequest(request, context);
                    }

                    return response;
                }

                private Identify processForIdentify( final Management request ) throws SecurityException, JAXBException, SOAPException {
                    Identify response = null;

                    // Is this an identify request?
                    final Identify identify = new Identify(request);
                    identify.setXmlBinding(request.getXmlBinding());

                    final SOAPElement id = identify.getIdentify();
                    if ( id != null ) {
                        response = new Identify();
                        response.setXmlBinding(request.getXmlBinding());

                        response.setIdentifyResponse(
                            "Layer 7 Technologies",
                            BuildInfo.getFormalProductVersion(),
                            Management.NS_URI,
                            null );

                        // Fix response message so it is schema valid
                        SOAPElement identifyResponseBody = response.getIdentifyResponse();
                        Iterator elements = identifyResponseBody.getChildElements( Identify.PROTOCOL_VERSION );
                        while ( elements.hasNext() ) {
                            Element element = (Element) elements.next();
                            identifyResponseBody.removeChild( element );
                            identifyResponseBody.insertBefore( element, identifyResponseBody.getFirstChild() );
                        }
                    }

                    return response;
                }

                /**
                 * Wiseman NPEs on missing SOAP header or body and has an ArrayStoreException if
                 * there is anything other than element content in the SOAP body.
                 */
                private Management ensureSOAPFormat( final Management request ) {
                    Management result = request;

                    // Wiseman NPEs on missing SOAP header so if it is missing create an empty one
                    if ( request.getHeader() == null ) {
                        try {
                            request.getEnvelope().addHeader();
                            result = new Management( request );
                            result.setXmlBinding( request.getXmlBinding() );
                        } catch ( SOAPException e ) {
                            logger.log( Level.WARNING, "Unable to add SOAP header to request message." );
                        }
                    }

                    // Wiseman NPEs on missing SOAP body so if it is missing create an empty one
                    if ( request.getBody() == null ) {
                        try {
                            request.getEnvelope().addBody();
                            result = new Management( request );
                            result.setXmlBinding( request.getXmlBinding() );
                        } catch ( SOAPException e ) {
                            logger.log( Level.WARNING, "Unable to add SOAP header to request message." );
                        }
                    } else {
                        // Wiseman has an ArrayStoreException if the body contains non-element
                        // nodes so remove them
                        NodeList nodeList = request.getBody().getChildNodes();
                        for ( int i=0; i<nodeList.getLength(); i++ ) {
                            Node node = nodeList.item( i );
                            if ( node.getNodeType() != Node.ELEMENT_NODE ) {
                                node.getParentNode().removeChild( node );
                            }
                        }
                    }

                    return result;
                }
            };
        } catch (SAXException e) {
            throw new PolicyAssertionException( assertion, "Error creating WS-Management agent '"+ExceptionUtils.getMessage( e )+"'", e );
        }
    }

    private AssertionStatus handle( final PolicyEnforcementContext context,
                                    final ContentType contentType,
                                    final Message request,
                                    final Message response ) throws IOException {
        AssertionStatus status = AssertionStatus.NONE;

        boolean processingResponse = false;
        InputStream is = null;
        try {
            final MimeKnob mimeKnob = request.getMimeKnob();
            is = mimeKnob.getEntireMessageBodyAsInputStream();

            final Management managementRequest = new Management( is );
            managementRequest.setXmlBinding(agent.getXmlBinding());
            managementRequest.setContentType(contentType);

            final ContentTypeHeader contentTypeHeader = mimeKnob.getOuterContentType();
            final String contentTypeStr = contentTypeHeader.getFullValue();
            final Principal user = context.getDefaultAuthenticationContext().getLastAuthenticatedUser();
            final String charEncoding = contentTypeHeader.getEncoding();
            final HttpRequestKnob httpRequestKnob = request.getHttpRequestKnob();
            final String url = httpRequestKnob.getRequestUrl();
            final Map<String, Object> properties = new HashMap<String, Object>(1);
            properties.put( "com.sun.ws.management.server.handler", "com.l7tech.external.assertions.wsmanagment.invalidhandlers" );
            properties.put( "com.l7tech.context", assertionContext );
            properties.put( "com.l7tech.remoteAddr", httpRequestKnob.getRemoteAddress() );
            final HandlerContext handlerContext = new HandlerContextImpl(user, contentTypeStr, charEncoding, url, properties);

            SOAP soapResponse = (SOAP) agent.handleRequest(managementRequest, handlerContext);

            setVariables( context, handlerContext.getRequestProperties() );

            processingResponse = true;
            sendResponse(soapResponse, response);
            context.setRoutingStatus( RoutingStatus.ROUTED);
        } catch ( IOException e ) {
            if ( !processingResponse ) throw e;
            auditor.logAndAudit( AssertionMessages.GATEWAYMANAGEMENT_ERROR, new String[]{ExceptionUtils.getMessage(e)}, ExceptionUtils.getDebugException(e) );
            return AssertionStatus.FAILED;
        } catch ( NoSuchPartException e ) {
            auditor.logAndAudit( AssertionMessages.GATEWAYMANAGEMENT_ERROR, new String[]{ExceptionUtils.getMessage(e)}, ExceptionUtils.getDebugException(e) );
            return AssertionStatus.FAILED;
        } catch ( SOAPException e ) {
            auditor.logAndAudit( AssertionMessages.GATEWAYMANAGEMENT_ERROR, new String[]{ExceptionUtils.getMessage(e)}, e );
            return AssertionStatus.FAILED;
        } catch ( JAXBException e ) {
            auditor.logAndAudit( AssertionMessages.GATEWAYMANAGEMENT_ERROR, new String[]{ExceptionUtils.getMessage(e)}, e );
            return AssertionStatus.FAILED;
        } finally {
            ResourceUtils.closeQuietly( is );
        }

        return status;
    }

    /**
     * Wiseman supports async messages but we don't, so fail if the response
     * is not the anonymous address.
     */
    private void validateAddressing( final Management request ) throws SOAPException, JAXBException {
        ensureAnonymous( request.getReplyTo() );
        if ( request.getReplyTo() == null ) ensureAnonymous( request.getFrom() );
        ensureAnonymous( request.getFaultTo() );
    }

    private void ensureAnonymous( final EndpointReferenceType endpointReference )  {
        if ( endpointReference != null &&
             endpointReference.getAddress() != null &&
             !Addressing.ANONYMOUS_ENDPOINT_URI.equals(endpointReference.getAddress().getValue()) ) {
            auditor.logAndAudit( AssertionMessages.GATEWAYMANAGEMENT_ERROR, "Unsupported response endpoint address '"+endpointReference.getAddress().getValue()+"'" );
            throw new AssertionStatusException( AssertionStatus.FALSIFIED );
        }
    }

    private void setVariables( final PolicyEnforcementContext context,
                               final Map<String,?> properties ) {
        final String prefix = assertion.getVariablePrefix();
        if ( prefix != null && prefix.length() > 0 ) {
            context.setVariable( prefix + "." + GatewayManagementAssertion.SUFFIX_ACTION, properties.get( "com.l7tech.status.action" )  );
            context.setVariable( prefix + "." + GatewayManagementAssertion.SUFFIX_ENTITY_ID, properties.get( "com.l7tech.status.entityId" )  );
            context.setVariable( prefix + "." + GatewayManagementAssertion.SUFFIX_ENTITY_TYPE, properties.get( "com.l7tech.status.entityType" )  );
            context.setVariable( prefix + "." + GatewayManagementAssertion.SUFFIX_MESSAGE, properties.get( "com.l7tech.status.message" ) );           
        }
    }

    private static void sendResponse( final SOAP soapResponse,
                                      final Message response )
            throws SOAPException, JAXBException, IOException {

        if ( soapResponse instanceof Identify ) {
            ByteArrayOutputStream os = new ByteArrayOutputStream();
            soapResponse.writeTo( os );
            response.initialize( ContentTypeHeader.SOAP_1_2_DEFAULT, os.toByteArray() );
        } else {
            sendManagementResponse( (Management) soapResponse, response);
        }
    }

    private static void sendManagementResponse( final Management managementResponse,
                                                final Message response )
            throws SOAPException, JAXBException, IOException {
        final HttpResponseKnob httpResponseKnob = response.getHttpResponseKnob();

        if ( managementResponse.getBody().hasFault()) {
            // sender faults need to set error code to BAD_REQUEST for client errors
            if ( SOAP.SENDER.equals(managementResponse.getBody().getFault().getFaultCodeAsQName())) {
                httpResponseKnob.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            } else {
                httpResponseKnob.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            }
        }

        final ByteArrayOutputStream os = new ByteArrayOutputStream();
        managementResponse.writeTo(os);
        final byte[] responseData = os.toByteArray();
        httpResponseKnob.setHeader( HttpConstants.HEADER_CONTENT_LENGTH, Integer.toString(responseData.length) );
        if ( managementResponse.getHeader() != null && managementResponse.getAction() != null ) {
            response.initialize(
                    ContentTypeHeader.parseValue(ContentTypeHeader.SOAP_1_2_DEFAULT.getFullValue() + "; action="+managementResponse.getAction()), 
                    responseData );
        } else {
            response.initialize( ContentTypeHeader.SOAP_1_2_DEFAULT, responseData );
        }
    }    
}
