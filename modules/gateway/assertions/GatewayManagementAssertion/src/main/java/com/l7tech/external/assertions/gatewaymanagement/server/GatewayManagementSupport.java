package com.l7tech.external.assertions.gatewaymanagement.server;

import com.l7tech.common.http.HttpMethod;
import com.l7tech.common.io.XmlUtil;
import com.l7tech.common.mime.ContentTypeHeader;
import com.l7tech.gateway.api.*;
import com.l7tech.gateway.api.impl.AccessorSupport;
import com.l7tech.gateway.api.impl.ValidationUtils;
import com.l7tech.gateway.common.audit.AssertionMessages;
import com.l7tech.gateway.common.audit.Audit;
import com.l7tech.message.HttpResponseKnob;
import com.l7tech.message.Message;
import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.PolicyAssertionException;
import com.l7tech.policy.assertion.RoutingStatus;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.server.policy.assertion.AssertionStatusException;
import com.l7tech.util.BuildInfo;
import com.l7tech.util.ExceptionUtils;
import com.l7tech.util.HexUtils;
import com.sun.ws.management.*;
import com.sun.ws.management.addressing.Addressing;
import com.sun.ws.management.enumeration.EnumerationExtensions;
import com.sun.ws.management.identify.Identify;
import com.sun.ws.management.server.HandlerContext;
import com.sun.ws.management.server.HandlerContextImpl;
import com.sun.ws.management.server.WSManAgent;
import com.sun.ws.management.server.reflective.WSManReflectiveAgent;
import com.sun.ws.management.soap.SOAP;
import com.sun.ws.management.transfer.Transfer;
import com.sun.ws.management.enumeration.Enumeration;
import com.sun.ws.management.transport.ContentType;
import org.dmtf.schemas.wbem.wsman._1.wsman.SelectorType;
import org.jetbrains.annotations.Nullable;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.xml.XmlBeanFactory;
import org.springframework.core.io.ClassPathResource;
import org.w3._2003._05.soap_envelope.Fault;
import org.w3._2003._05.soap_envelope.Reasontext;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
import org.xmlsoap.schemas.ws._2004._08.addressing.EndpointReferenceType;
import org.xmlsoap.schemas.ws._2004._09.enumeration.EnumerateResponse;

import javax.mail.MethodNotSupportedException;
import javax.servlet.http.HttpServletResponse;
import javax.xml.bind.JAXBException;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlSchema;
import javax.xml.soap.*;
import java.io.IOException;
import java.lang.annotation.Annotation;
import java.security.Principal;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 */
public class GatewayManagementSupport {

    private final Map<String,String> resourceURIMap = new HashMap<String,String>();
    public static final long TIMEOUT_DURATION_MS = 1000L;

    public static GatewayManagementSupport createInstance(final Assertion assertion, final Audit audit , final Logger logger, final BeanFactory context) throws PolicyAssertionException {
        return new GatewayManagementSupport(assertion, audit , logger,  context);
    }

    public AssertionStatus processRequest(final PolicyEnforcementContext context,
                                          final Message request,
                                          final Message response,
                                          final Audit audit) throws IOException {

        try{
            AssertionStatus status = AssertionStatus.NONE;
            String URI = request.getHttpRequestKnob().getRequestUri();
            String baseURI = context.getService().getRoutingUri();
            URI = URI.substring(baseURI.length()-1);

            Document requestDocument = request.isEnableOriginalDocument() ? request.getXmlKnob().getOriginalDocument(): null;

            final Principal user = context.getDefaultAuthenticationContext().getLastAuthenticatedUser();

            context.setRoutingStatus(RoutingStatus.ATTEMPTED);
            status = doProcessRequest(request, response, audit, request.getHttpRequestKnob().getMethod(),URI, requestDocument , user);
            context.setRoutingStatus(RoutingStatus.ROUTED);

            return status;
        }catch(Exception e){
            return handleErrors(e,audit,response);
        }

    }

    private AssertionStatus doProcessRequest( Message request, Message response, Audit audit, HttpMethod method, String URI, Document requestDocument, Principal user) throws IOException {
        AssertionStatus status;

        if(!URI.contains("/") && method.equals(HttpMethod.GET)){
            // enumerate
            status =  handleEnumerate( URI, request, response, user, audit);

        } else{
            status = handle(method , URI, request, response, user, audit);
        }
        return status;
    }

    private AssertionStatus handleEnumerate(String URI,Message request, Message response, Principal user, Audit audit) throws IOException {
        final String resourceURI = getResourceUri(URI);
        final String actionUri =  Enumeration.ENUMERATE_ACTION_URI;

        AssertionStatus status = AssertionStatus.NONE;
        final Management managementRequest;
        final HandlerContext handlerContext;
        try {

            managementRequest = createManagementRequest( actionUri, resourceURI, TIMEOUT_DURATION_MS, null, null);
            handlerContext = getHandlerContext(request.getHttpRequestKnob().getRequestUrl(), request.getHttpRequestKnob().getRemoteAddress(), user, managementRequest.getContentType());

        }catch ( Exception e){
            audit.logAndAudit( AssertionMessages.GATEWAYMANAGEMENT_ERROR, new String[]{ExceptionUtils.getMessage(e)}, ExceptionUtils.getDebugException(e) );
            setResponseStatus(response, HttpServletResponse.SC_BAD_REQUEST,ExceptionUtils.getMessage(e));
            return AssertionStatus.FAILED;
        }

        // Get Enum context
        Object enumContext = null;
        try{

            Enumeration enumerationRequest = new Enumeration(managementRequest);
            enumerationRequest.setAction(Enumeration.ENUMERATE_ACTION_URI);
            enumerationRequest.setEnumerate(null,null,null);
            Addressing enumerateResponse = (Addressing)agent.handleRequest(new Management(enumerationRequest.getMessage()), handlerContext);

            Enumeration enumResponse = new Enumeration(enumerateResponse);
            EnumerateResponse  getEnumerateResponse= enumResponse.getEnumerateResponse();
            enumContext = getEnumerateResponse.getEnumerationContext().getContent().get(0);
            if(enumContext == null){
                audit.logAndAudit( AssertionMessages.GATEWAYMANAGEMENT_ERROR, new String[]{"Failed to get enumeration context",""});
                setResponseStatus(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR,"Failed to get enumeration context");
                return AssertionStatus.FAILED;
            }

        } catch ( Exception e){
            audit.logAndAudit( AssertionMessages.GATEWAYMANAGEMENT_ERROR, new String[]{ExceptionUtils.getMessage(e)}, ExceptionUtils.getDebugException(e) );
            setResponseStatus(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR,ExceptionUtils.getMessage(e));
            return AssertionStatus.FAILED;
        }

        // Get items
        final Document responseDoc = createResultDocument();
        final int PullElementBatch = 10;
        try{
            boolean done = true;
            do{

                Enumeration pullRequest = new Enumeration(managementRequest);
                pullRequest.setAction(Enumeration.PULL_ACTION_URI);
                pullRequest.setPull(enumContext,0,PullElementBatch,managementRequest.getTimeout());

                Addressing pullResponse = (Addressing)agent.handleRequest(new Management(pullRequest.getMessage()), handlerContext);

                Fault fault = pullResponse.getFault();
                if(fault!=null)
                {
                    setResponseStatus(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR,fault.getReason().getText().get(0).getValue());
                    return AssertionStatus.FAILED;
                }

                Element elmItems = XmlUtil.findFirstDescendantElement(pullResponse.getBody(), Enumeration.NS_URI, EnumerationExtensions.ITEMS.getLocalPart());
                NodeList itemNodes = elmItems.getChildNodes();
                for (int i = 0; i < itemNodes.getLength(); i++) {
                    appendChildNode(itemNodes.item(i),responseDoc);
                }

                Element endElem = XmlUtil.findFirstDescendantElement(pullResponse.getBody(), Enumeration.NS_URI, EnumerationExtensions.END_OF_SEQUENCE.getLocalPart());
                done =  endElem != null;

            }while(!done);

            response.initialize(ContentTypeHeader.XML_DEFAULT, XmlUtil.toByteArray(responseDoc));

        }catch ( Exception e ){
            audit.logAndAudit( AssertionMessages.GATEWAYMANAGEMENT_ERROR, new String[]{ExceptionUtils.getMessage(e)}, ExceptionUtils.getDebugException(e) );
            setResponseStatus(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR,ExceptionUtils.getMessage(e));
            return AssertionStatus.FAILED;
        }
        return status;
    }

    private  GatewayManagementSupport(final Assertion assertion, final Audit audit , final Logger logger, final BeanFactory context) throws PolicyAssertionException {
        this.agent = GatewayManagementSupport.getAgent(audit, logger, assertion);
        this.assertionContext = GatewayManagementSupport.buildContext( context, "gatewayManagementContext.xml" );
    }

    private AssertionStatus handle( final HttpMethod method,
                                   final String URI,
                                   final Message request,
                                   final Message response,
                                   final Principal user,
                                   final Audit audit) throws IOException
    {
        AssertionStatus status = AssertionStatus.NONE;
        final Management managementRequest;
        final HandlerContext handlerContext;
        try {
            final String actionUri ;
            if(method==HttpMethod.GET){
                actionUri = Transfer.GET_ACTION_URI;
            }
            else if(method==HttpMethod.DELETE){
                actionUri = Transfer.DELETE_ACTION_URI;
            }
            else {
                // todo  more
                throw new MethodNotSupportedException();
            }

            String resource = URI.substring(0,URI.indexOf("/"));
            final String resourceURI = getResourceUri(resource);

            String selectorProperty = URI.substring(URI.indexOf("/")+1);
            try {
                selectorProperty = HexUtils.urlDecode(selectorProperty);
            } catch (IOException e) {
                throw new InvalidPropertiesFormatException(e);
            }

            String selectorType = getSelectorType(request);
            managementRequest = createManagementRequest(actionUri, resourceURI, TIMEOUT_DURATION_MS, selectorProperty, selectorType);
            handlerContext = getHandlerContext(request.getHttpRequestKnob().getRequestUrl(), request.getHttpRequestKnob().getRemoteAddress(), user,  managementRequest.getContentType());

        }catch ( Exception e){
            audit.logAndAudit( AssertionMessages.GATEWAYMANAGEMENT_ERROR, new String[]{ExceptionUtils.getMessage(e)}, ExceptionUtils.getDebugException(e) );
            setResponseStatus(response, HttpServletResponse.SC_BAD_REQUEST,ExceptionUtils.getMessage(e));
            return AssertionStatus.FAILED;
        }

        try{
            SOAP soapResponse = (SOAP) agent.handleRequest(managementRequest, handlerContext);
            GatewayManagementSupport.sendResponse(soapResponse, response, audit);
        } catch ( Exception e){
            audit.logAndAudit( AssertionMessages.GATEWAYMANAGEMENT_ERROR, new String[]{ExceptionUtils.getMessage(e)}, ExceptionUtils.getDebugException(e) );
            setResponseStatus(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR,ExceptionUtils.getMessage(e));
            return AssertionStatus.FAILED;
        }

        return status;
    }

    // search by 'id' if parameter t=id. default searches by 'name'
    private String getSelectorType(Message request) throws IOException {

        String typeParam = request.getHttpRequestKnob().getParameter("t");
        if(typeParam!=null && typeParam.equalsIgnoreCase("id"))
            return "id";
        return "name";
    }

    private HandlerContext getHandlerContext(String URL, String remoteAddr, Principal user, ContentType contentTypeHeader) {
        final String contentTypeStr = contentTypeHeader.getMimeType();
        final String charEncoding = contentTypeHeader.getEncoding();
        final Map<String, Object> properties = new HashMap<String, Object>();
        properties.put( "com.sun.ws.management.server.handler", "com.l7tech.external.assertions.wsmanagment.invalidhandlers" );
        properties.put( "com.l7tech.context", assertionContext );
        properties.put( "com.l7tech.remoteAddr",remoteAddr );
        return new HandlerContextImpl(user, contentTypeStr, charEncoding, URL , properties);
    }

    // todo
    private AssertionStatus handleErrors(Exception e, Audit audit, Message response){
        try{
            if ( e instanceof InvalidPropertiesFormatException) {
                audit.logAndAudit( AssertionMessages.GATEWAYMANAGEMENT_ERROR, new String[]{ExceptionUtils.getMessage(e)}, ExceptionUtils.getDebugException(e) );
                setResponseStatus(response, HttpServletResponse.SC_BAD_REQUEST,ExceptionUtils.getMessage(e));
            } else if ( e instanceof MethodNotSupportedException   ) {
                audit.logAndAudit( AssertionMessages.GATEWAYMANAGEMENT_ERROR, new String[]{ExceptionUtils.getMessage(e)}, ExceptionUtils.getDebugException(e) );
                setResponseStatus(response, HttpServletResponse.SC_METHOD_NOT_ALLOWED,ExceptionUtils.getMessage(e));
            }else{
                audit.logAndAudit(AssertionMessages.GATEWAYMANAGEMENT_ERROR, new String[]{ExceptionUtils.getMessage(e)}, ExceptionUtils.getDebugException(e) );
                setResponseStatus(response, HttpServletResponse.SC_BAD_REQUEST, ExceptionUtils.getMessage(e));
            }
            return AssertionStatus.FAILED;
        }catch(IOException ex){
            audit.logAndAudit(AssertionMessages.GATEWAYMANAGEMENT_ERROR, new String[]{"Error handling error:"+ExceptionUtils.getMessage(ex)}, ExceptionUtils.getDebugException(ex) );
            return AssertionStatus.FAILED;
        }

    }

    private void setResponseStatus(Message response, int httpStatus, String message) throws IOException {
        // todo
        response.getHttpResponseKnob().setStatus(httpStatus);
        response.initialize(ContentTypeHeader.TEXT_DEFAULT, message.getBytes());
    }

    /**
     * Creates a wiseman request
     * @param actionUri  action uri for this request
     * @param resourceURI  resource uri for this request
     * @param timeoutDurationMS  the default timeout in milliseconds
     * @param selectorProperty  the selector value, may be null
     * @param selectorPropertyType  the selector type(name, id...), may be null
     * @return
     */
    private Management createManagementRequest
            ( String actionUri, String resourceURI, long timeoutDurationMS, @Nullable String selectorProperty, @Nullable String selectorPropertyType) {
        try{

            ManagementMessageValues settings = new ManagementMessageValues();
            settings.setReplyTo("http://schemas.xmlsoap.org/ws/2004/08/addressing/role/anonymous");
            settings.setResourceUri(resourceURI);
            settings.setTo("https://localhost:8443/wsman");
            settings.setTimeout(timeoutDurationMS );
            if(selectorProperty!=null && !selectorProperty.isEmpty())
            {
                Set<SelectorType> selectors = new HashSet<SelectorType>();
                final SelectorType selector = new SelectorType();
                selector.setName(selectorPropertyType);
                selector.getContent().add(selectorProperty);
                selectors.add(selector);
                settings.setSelectorSet(selectors);
            }

            final Management managementRequest = ManagementUtility.buildMessage(null,settings);
            managementRequest.setAction(actionUri);
            return managementRequest;

        } catch (Exception e) {
            throw new RuntimeException(e); // todo better exception?
        }
    }

    public static String getResourceName( final Class<? extends AccessibleObject> managedObjectClass ) {
        final AccessorSupport.AccessibleResource resource = managedObjectClass.getAnnotation( AccessorSupport.AccessibleResource.class );
        if ( resource == null ) {
            throw new ManagementRuntimeException("Missing annotation for resource '"+managedObjectClass.getName()+"'.");
        }
        return resource.name();
    }

    public static String getResourceUri( final Class<? extends AccessibleObject> managedObjectClass ) {
        final XmlSchema schema = Accessor.class.getPackage().getAnnotation(XmlSchema.class);
        if ( schema == null ) {
            throw new ManagementRuntimeException("Missing annotation for API package.");
        }
        return schema.namespace() + "/" + getResourceName(managedObjectClass);
    }


    private String getResourceUri(String resourceType) throws InvalidPropertiesFormatException {
        if(resourceURIMap.isEmpty()){
            Class[] MOclasses = ManagedObjectFactory.getAllManagedObjectClasses();

            for(Class MO: MOclasses){
                Annotation[] annotations = MO.getAnnotations();
                String element = null;

                for(Annotation ann: annotations){
                    if( ann instanceof XmlRootElement){
                        element = ((XmlRootElement) ann).name();
                        break;
                    }
                }
                String uri = getResourceUri(MO);
                resourceURIMap.put(element.toLowerCase(), uri);
            }
        }
        String resourceURI =  resourceURIMap.get(resourceType.toLowerCase());
        if(resourceURI==null)
            throw new InvalidPropertiesFormatException("Invalid resource type: "+resourceType) ;
        return resourceURI;

    }


    private final WSManAgent agent;

    private final XmlBeanFactory assertionContext;

    private static final AtomicReference<WSManReflectiveAgent> sharedAgent = new AtomicReference<WSManReflectiveAgent>();


    public static XmlBeanFactory buildContext( final BeanFactory context,
                                                final String assertionContextResource ) {
        XmlBeanFactory assertionContext = new XmlBeanFactory(new ClassPathResource(assertionContextResource, ServerRESTGatewayManagementAssertion.class), context);
        assertionContext.preInstantiateSingletons();
        return assertionContext;
    }

    public static WSManReflectiveAgent getAgent( final Audit audit, final Logger logger, final Assertion assertion ) throws PolicyAssertionException {
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
            if ( ExceptionUtils.causedBy(e, SAXParseException.class)) {
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
            audit.logAndAudit(AssertionMessages.GATEWAYMANAGEMENT_ERROR, "Unsupported response endpoint address '" + endpointReference.getAddress().getValue() + "'");
            throw new AssertionStatusException( AssertionStatus.FALSIFIED );
        }
    }

    public static void sendResponse( final SOAP soapResponse,
                                      final Message response,
                                      final Audit audit )
            throws SOAPException, JAXBException, IOException {

        if ( soapResponse instanceof Identify) {
            response.getHttpResponseKnob().setStatus(HttpServletResponse.SC_BAD_REQUEST);
            audit.logAndAudit(AssertionMessages.GATEWAYMANAGEMENT_ERROR, new String[]{"Identify not supported"});
        } else {
            sendManagementResponse((Management) soapResponse, response, audit);
        }
    }

    private static void sendManagementResponse( final Management managementResponse,
                                                final Message response,
                                                final Audit audit )
            throws SOAPException, JAXBException, IOException {
        final HttpResponseKnob httpResponseKnob = response.getHttpResponseKnob();

        if ( managementResponse.getBody().hasFault()) {
            // sender faults need to set error code to BAD_REQUEST for client errors
            if ( SOAP.SENDER.equals(managementResponse.getBody().getFault().getFaultCodeAsQName())) {
                httpResponseKnob.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            } else {
                httpResponseKnob.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            }

            Fault fault = managementResponse.getFault();
            List<Reasontext> reasons = fault.getReason().getText();
            String faultReason = "";
            for(Reasontext reason: reasons){
                faultReason += reason.getValue() +" ";
            }


            response.initialize(ContentTypeHeader.TEXT_DEFAULT, faultReason.getBytes());
            audit.logAndAudit(AssertionMessages.GATEWAYMANAGEMENT_ERROR, new String[]{faultReason});
            return;
        }

        Node body = managementResponse.getBody();
        Node managedObjectXml =  body.getFirstChild();
        Document responseDoc = createResultDocument();
        appendChildNode(managedObjectXml, responseDoc);
        responseDoc.normalizeDocument();


        response.initialize(responseDoc);
    }

    private static void appendChildNode(Node managedObjectXml, Document parentDocument) {
        managedObjectXml = parentDocument.importNode(managedObjectXml, true);
        parentDocument.getDocumentElement().appendChild(managedObjectXml);
    }

    private static Document createResultDocument() {
        return XmlUtil.createEmptyDocument("ManagementResult", ResourceHandler.MANAGEMENT_PREFIX, ResourceHandler.MANAGEMENT_NAMESPACE);
    }

    private static class SchemaValidationException extends Exception {
        private SchemaValidationException( final String message, final Throwable cause ) {
            super( message, cause );
        }
    }


}
