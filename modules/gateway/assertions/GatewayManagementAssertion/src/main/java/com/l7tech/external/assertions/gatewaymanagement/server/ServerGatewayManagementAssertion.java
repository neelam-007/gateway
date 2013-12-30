package com.l7tech.external.assertions.gatewaymanagement.server;

import com.l7tech.common.mime.ContentTypeHeader;
import com.l7tech.external.assertions.gatewaymanagement.GatewayManagementAssertion;
import com.l7tech.gateway.api.impl.ValidationUtils;
import com.l7tech.gateway.common.audit.AssertionMessages;
import com.l7tech.gateway.common.audit.Audit;
import com.l7tech.message.HttpRequestKnob;
import com.l7tech.message.HttpResponseKnob;
import com.l7tech.message.Message;
import com.l7tech.message.MimeKnob;
import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.PolicyAssertionException;
import com.l7tech.policy.assertion.RoutingStatus;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.server.policy.assertion.AbstractServerAssertion;
import com.l7tech.server.policy.assertion.AssertionStatusException;
import com.l7tech.util.BuildInfo;
import com.l7tech.util.ExceptionUtils;
import com.sun.ws.management.InternalErrorFault;
import com.sun.ws.management.Management;
import com.sun.ws.management.SchemaValidationErrorFault;
import com.sun.ws.management.addressing.Addressing;
import com.sun.ws.management.identify.Identify;
import com.sun.ws.management.server.HandlerContext;
import com.sun.ws.management.server.HandlerContextImpl;
import com.sun.ws.management.server.WSManAgent;
import com.sun.ws.management.server.reflective.WSManReflectiveAgent;
import com.sun.ws.management.soap.SOAP;
import com.sun.ws.management.transport.ContentType;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
import org.xmlsoap.schemas.ws._2004._08.addressing.EndpointReferenceType;

import javax.servlet.http.HttpServletResponse;
import javax.xml.bind.JAXBException;
import javax.xml.soap.*;
import javax.xml.transform.dom.DOMSource;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.Charset;
import java.security.Principal;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Server side implementation of the GatewayManagementAssertion.
 *
 * @see GatewayManagementAssertion
 */
public class ServerGatewayManagementAssertion extends AbstractServerAssertion<GatewayManagementAssertion> {

    //- PUBLIC

    public ServerGatewayManagementAssertion( final GatewayManagementAssertion assertion,
                                             final ApplicationContext applicationContext ) throws PolicyAssertionException {
        this( assertion, applicationContext, "gatewayManagementContext.xml", true );
    }

    @Override
    public AssertionStatus checkRequest( final PolicyEnforcementContext context ) throws IOException, PolicyAssertionException {
        final Message request = context.getRequest();
        final Message response = context.getResponse();
        final MimeKnob mimeKnob = request.getMimeKnob();

        // Validate content type
        final ContentTypeHeader contentTypeHeader = mimeKnob.getOuterContentType();
        final String contentTypeText = contentTypeHeader.getFullValue();
        final ContentType contentType = ContentType.createFromHttpContentType(contentTypeText);
        if (contentType == null || !contentType.isAcceptable()) {
            logAndAudit( AssertionMessages.GATEWAYMANAGEMENT_ERROR, "Content-Type not supported : " + contentTypeText );
            return AssertionStatus.FALSIFIED;
        }

        //
        final ClassLoader contextClassLoader = Thread.currentThread().getContextClassLoader();
        try {
            if (maskContextClassLoader) {
                Thread.currentThread().setContextClassLoader(assertion.getClass().getClassLoader());
            }
            return handle(context, contentTypeHeader, contentType, request, response);
        } finally {
            Thread.currentThread().setContextClassLoader( contextClassLoader );
        }
    }

    //- PROTECTED

    protected ServerGatewayManagementAssertion( final GatewayManagementAssertion assertion,
                                                final ApplicationContext context,
                                                final String assertionContextResource,
                                                final boolean maskContextClassLoader ) throws PolicyAssertionException {
        super(assertion);
        this.agent = getAgent( getAudit(), logger, assertion );
        this.assertionContext = buildContext( context, assertionContextResource );
        this.maskContextClassLoader = maskContextClassLoader;
    }

    //- PRIVATE

    private static final AtomicReference<WSManReflectiveAgent> sharedAgent = new AtomicReference<WSManReflectiveAgent>();

    private final WSManAgent agent;
    private final ApplicationContext assertionContext;
    private final boolean maskContextClassLoader;

    private static ApplicationContext buildContext( final ApplicationContext context,
                                              final String assertionContextResource ) {
        return new ClassPathXmlApplicationContext(new String[] {assertionContextResource}, ServerGatewayManagementAssertion.class, context);
    }

    private static WSManReflectiveAgent getAgent( final Audit audit, final Logger logger, final Assertion assertion ) throws PolicyAssertionException {
        WSManReflectiveAgent agent = sharedAgent.get();
        if ( agent == null ) {
            agent = buildAgent( audit, logger, assertion );
            sharedAgent.compareAndSet( null, agent );
        }
        return agent;
    }

    private static WSManReflectiveAgent buildAgent( final Audit audit, final Logger logger, final Assertion assertion ) throws PolicyAssertionException {
        try {
            return new WSManReflectiveAgent(null, ValidationUtils.getSchemaSources(), null){
                @SuppressWarnings({"ThrowableInstanceNeverThrown"})
                @Override
                public com.sun.ws.management.Message handleRequest( final Management managementRequest, final HandlerContext context ) {
                    com.sun.ws.management.Message response;

                    managementRequest.setXmlBinding( getXmlBinding() );
                    Management request = ensureSOAPFormat(managementRequest);

                    try {
                        validateManagementHeaders( request );
                        validateAddressing( audit, request );
                        response = processForIdentify( request );
                    } catch (Throwable th) {
                        if ( th instanceof AssertionStatusException ) throw (AssertionStatusException) th;
                        try {
                            Management managementResponse = new Management();
                            if ( th instanceof SchemaValidationException )  {
                                managementResponse.setFault((SchemaValidationErrorFault)new SchemaValidationErrorFault(SOAP.createFaultDetail(ExceptionUtils.getMessage(th), null, ExceptionUtils.getDebugException(th), null)).initCause(th));
                            } else {
                                managementResponse.setFault((InternalErrorFault)new InternalErrorFault(SOAP.createFaultDetail(ExceptionUtils.getMessage(th), null, ExceptionUtils.getDebugException(th), null)).initCause(th));
                            }
                            response = managementResponse;
                        } catch ( Exception e ) {
                            throw ExceptionUtils.wrap( e );
                        }
                    }

                    if ( response == null ) {
                        // fyi - this request will be processed in a thread pool local to the WSManAgent
                        // the current message processing thread will be parked while this happens
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
                                    final ContentTypeHeader contentTypeHeader,
                                    final ContentType contentType,
                                    final Message request,
                                    final Message response ) throws IOException {
        AssertionStatus status = AssertionStatus.NONE;

        boolean processingResponse = false;
        try {
            final MessageFactory factory = MessageFactory.newInstance(SOAPConstants.SOAP_1_2_PROTOCOL);
            final SOAPMessage message = factory.createMessage();
            final SOAPPart part = message.getSOAPPart();
            part.setContent( new DOMSource( request.getXmlKnob().getDocumentReadOnly() ) );

            final Management managementRequest = new Management( message );
            managementRequest.setXmlBinding(agent.getXmlBinding());
            managementRequest.setContentType(contentType);

            final String contentTypeStr = contentTypeHeader.getFullValue();
            final Principal user = context.getDefaultAuthenticationContext().getLastAuthenticatedUser();
            final Charset charEncoding = contentTypeHeader.getEncoding();
            final HttpRequestKnob httpRequestKnob = request.getHttpRequestKnob();
            final String url = httpRequestKnob.getRequestUrl();
            final Map<String, Object> properties = new HashMap<String, Object>();
            properties.put( "com.sun.ws.management.server.handler", "com.l7tech.external.assertions.wsmanagment.invalidhandlers" );
            properties.put( "com.l7tech.context", assertionContext );
            properties.put( "com.l7tech.remoteAddr", httpRequestKnob.getRemoteAddress() );
            final HandlerContext handlerContext = new HandlerContextImpl(user, contentTypeStr, charEncoding.name(), url, properties);

            SOAP soapResponse = (SOAP) agent.handleRequest(managementRequest, handlerContext);

            setVariables( context, handlerContext.getRequestProperties() );

            processingResponse = true;
            sendResponse(soapResponse, response);
            context.setRoutingStatus( RoutingStatus.ROUTED);
        } catch ( IOException e ) {
            if ( !processingResponse ) throw e;
            logAndAudit( AssertionMessages.GATEWAYMANAGEMENT_ERROR, new String[]{ExceptionUtils.getMessage(e)}, ExceptionUtils.getDebugException(e) );
            return AssertionStatus.FAILED;
        } catch ( SAXException e ) {
            logAndAudit( AssertionMessages.GATEWAYMANAGEMENT_ERROR, new String[]{ExceptionUtils.getMessage(e)}, ExceptionUtils.getDebugException(e) );
            return AssertionStatus.FAILED;
        } catch ( SOAPException e ) {
            logAndAudit( AssertionMessages.GATEWAYMANAGEMENT_ERROR, new String[]{ExceptionUtils.getMessage(e)}, e );
            return AssertionStatus.FAILED;
        } catch ( JAXBException e ) {
            logAndAudit( AssertionMessages.GATEWAYMANAGEMENT_ERROR, new String[]{ExceptionUtils.getMessage(e)}, e );
            return AssertionStatus.FAILED;
        }

        return status;
    }

    /**
     * Validate management headers early by accessing them, we'll handle
     * this error so we don't return a generic fault.
     */
    private static void validateManagementHeaders( final Management request ) throws SOAPException, JAXBException, SchemaValidationException {
        try {
            request.getTimeout();
            request.getResourceURI();
            request.getOptions();
            request.getSelectors();
            request.getMaxEnvelopeSize();
            request.getLocale();
        } catch ( JAXBException e ) {
            if ( ExceptionUtils.causedBy( e, SAXParseException.class )) {
                throw new SchemaValidationException( ExceptionUtils.getMessage(e), e);
            } else {
                throw e;
            }
        }
    }

    /**
     * Wiseman supports async messages but we don't, so fail if the response
     * is not the anonymous address.
     */
    private static void validateAddressing( final Audit audit, final Management request ) throws SOAPException, JAXBException {
        ensureAnonymous( audit, request.getReplyTo() );
        if ( request.getReplyTo() == null ) ensureAnonymous( audit, request.getFrom() );
        ensureAnonymous( audit, request.getFaultTo() );
    }

    private static void ensureAnonymous( final Audit audit, final EndpointReferenceType endpointReference )  {
        if ( endpointReference != null &&
             endpointReference.getAddress() != null &&
             !Addressing.ANONYMOUS_ENDPOINT_URI.equals(endpointReference.getAddress().getValue()) ) {
            audit.logAndAudit( AssertionMessages.GATEWAYMANAGEMENT_ERROR, "Unsupported response endpoint address '" + endpointReference.getAddress().getValue() + "'" );
            throw new AssertionStatusException( AssertionStatus.FALSIFIED );
        }
    }

    private void setVariables( final PolicyEnforcementContext context,
                               final Map<String,?> properties ) {
        final String prefix = assertion.getVariablePrefix();
        if ( prefix != null && prefix.length() > 0 ) {
            context.setVariable( prefix + "." + GatewayManagementAssertion.SUFFIX_ACTION, properties.get( "com.l7tech.status.action" ) );
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
            sendManagementResponse( (Management) soapResponse, response );
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
        if ( managementResponse.getHeader() != null && managementResponse.getAction() != null ) {
            response.initialize(
                    ContentTypeHeader.parseValue(ContentTypeHeader.SOAP_1_2_DEFAULT.getFullValue() + "; action="+managementResponse.getAction()), 
                    responseData );
        } else {
            response.initialize( ContentTypeHeader.SOAP_1_2_DEFAULT, responseData );
        }
    }

    private static class SchemaValidationException extends Exception {
        private SchemaValidationException( final String message, final Throwable cause ) {
            super( message, cause );
        }
    }
}
