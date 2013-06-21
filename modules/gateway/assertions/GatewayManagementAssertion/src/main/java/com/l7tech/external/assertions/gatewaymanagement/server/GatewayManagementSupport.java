package com.l7tech.external.assertions.gatewaymanagement.server;

import com.l7tech.common.io.XmlUtil;
import com.l7tech.common.mime.ContentTypeHeader;
import com.l7tech.gateway.api.*;
import com.l7tech.gateway.api.impl.AccessorSupport;
import com.l7tech.gateway.api.impl.MarshallingUtils;
import com.l7tech.gateway.api.impl.ValidationUtils;
import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.assertion.PolicyAssertionException;
import com.l7tech.server.policy.assertion.AssertionStatusException;
import com.l7tech.util.BuildInfo;
import com.l7tech.util.ExceptionUtils;
import com.l7tech.util.HexUtils;
import com.l7tech.util.Pair;
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
import javax.management.modelmbean.InvalidTargetObjectTypeException;
import javax.xml.bind.JAXBException;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlSchema;
import javax.xml.soap.*;
import javax.xml.transform.dom.DOMSource;
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

    private static final Logger logger = Logger.getLogger( GatewayManagementSupport.class.getName() );

    // resource name -> Pair < resource URI, ManagedObject class>
    private final Map<String,Pair<String,Class<ManagedObject>>> resourceURIMap = new HashMap<String,Pair<String,Class<ManagedObject>>>();
    public static final long TIMEOUT_DURATION_MS = 1000L;

    public static GatewayManagementSupport createInstance(final Assertion assertion,  final BeanFactory context) throws PolicyAssertionException {
        return new GatewayManagementSupport(assertion, context);
    }

    /**
     * Get the specified resource by name
     * @param resourceType  resource type
     * @param selectorValue name of resource
     * @param user  user to authenticate against
     * @param remoteAddr   remote address, used for contextual logging
     * @return  List of managed object and the resource document
     * @throws Exception
     */
    public Pair<Document,List<ManagedObject>> getResource( String resourceType, String selectorValue,Principal user, String remoteAddr) throws Exception {
        return handleResourceOperation(Transfer.GET_ACTION_URI, resourceType, selectorValue, null, user, remoteAddr) ;
    }

    /**
     * Deletes the specified resource by name
     * @param resourceType  resource type
     * @param selectorValue name of resource
     * @param user  user to authenticate against
     * @param remoteAddr   remote address, used for contextual logging
     * @throws Exception
     */
    public void deleteResource(String resourceType, String selectorValue,Principal user, String remoteAddr ) throws Exception {
        handleResourceOperation(Transfer.DELETE_ACTION_URI, resourceType, selectorValue, null, user, remoteAddr) ;
    }

    public Pair<Document,List<ManagedObject>> createResource( String resourceType, String selectorValue, Document requestDocument, Principal user, String remoteAddr) throws Exception {
        // todo check for namespace ?
        //        XmlUtil.findAllNamespaces(requestDocument).containsKey()
        throw new UnsupportedOperationException("Not yet implemented");
//        return handleResourceOperation(Transfer.CREATE_ACTION_URI, resourceType, selectorValue, requestDocument, user, remoteAddr) ;
    }

    /**
     * Get a list of the specified resource type
     * @param resourceType  resource type
     * @param user  user to authenticate against
     * @param remoteAddr   remote address, used for contextual logging
     * @throws Exception
     */
    public Pair<Document,List<ManagedObject>> getResourceList(String resourceType, Principal user, String remoteAddr) throws Exception {
        final String resourceURI = getResourceUri(resourceType);
        final String actionUri =  Enumeration.ENUMERATE_ACTION_URI;
        final String handlerURL = resourceType;

        final Management managementRequest;
        final HandlerContext handlerContext;

        managementRequest = createManagementRequest( actionUri, resourceURI, TIMEOUT_DURATION_MS, null, null);
        handlerContext = getHandlerContext(handlerURL, remoteAddr, user, managementRequest.getContentType());

        // Get Enum context
        Object enumContext = null;
        Enumeration enumerationRequest = new Enumeration(managementRequest);
        enumerationRequest.setAction(Enumeration.ENUMERATE_ACTION_URI);
        enumerationRequest.setEnumerate(null, null, null);
        Addressing enumerateResponse = (Addressing)agent.handleRequest(new Management(enumerationRequest.getMessage()), handlerContext);

        Enumeration enumResponse = new Enumeration(enumerateResponse);
        EnumerateResponse  getEnumerateResponse= enumResponse.getEnumerateResponse();
        enumContext = getEnumerateResponse.getEnumerationContext().getContent().get(0);
        if(enumContext == null){
            throw new IOException("Enumeration Context not found"); // todo
        }

        // Get items
        final Document responseDoc = createResultDocument();
        final int PullElementBatch = 10;

        boolean done = true;
        List<ManagedObject> managedObjects = new ArrayList<>();
        do{
            Enumeration pullRequest = new Enumeration(managementRequest);
            pullRequest.setAction(Enumeration.PULL_ACTION_URI);
            pullRequest.setPull(enumContext,0,PullElementBatch,managementRequest.getTimeout());

            Addressing pullResponse = (Addressing)agent.handleRequest(new Management(pullRequest.getMessage()), handlerContext);

            Fault fault = pullResponse.getFault();
            if(fault!=null)
            {
                throwException(fault,handlerContext);
            }

            Element elmItems = XmlUtil.findFirstDescendantElement(pullResponse.getBody(), Enumeration.NS_URI, EnumerationExtensions.ITEMS.getLocalPart());
            NodeList itemNodes = elmItems.getChildNodes();
            managedObjects.addAll(appendChildNodes(itemNodes, responseDoc));

            Element endElem = XmlUtil.findFirstDescendantElement(pullResponse.getBody(), Enumeration.NS_URI, EnumerationExtensions.END_OF_SEQUENCE.getLocalPart());
            done =  endElem != null;

        }while(!done);

        return new Pair<Document,List<ManagedObject>>(responseDoc,managedObjects);
    }

    private  GatewayManagementSupport(final Assertion assertion, final BeanFactory context) throws PolicyAssertionException {
        this.agent = GatewayManagementSupport.getAgent( assertion);
        this.assertionContext = GatewayManagementSupport.buildContext( context, "gatewayManagementContext.xml" );
    }

    private Pair<Document,List<ManagedObject>> handleResourceOperation(final String actionUri,
                                             final String resourceType,
                                             final String selectorValue,
                                             final Document body,
                                             final Principal user,
                                             final String remoteAddr) throws Exception, MethodNotSupportedException, SOAPException {
        final Management managementRequest;
        final HandlerContext handlerContext;

        final String resourceURI = getResourceUri(resourceType);
        final String selectorProperty;
        try {
            selectorProperty = HexUtils.urlDecode(selectorValue);
        } catch (IOException e) {
            throw new InvalidPropertiesFormatException(e); //todo
        }

        final String selectorType = "name";
        final String url = resourceType+"/"+selectorValue;

        managementRequest = createManagementRequest(actionUri, resourceURI, TIMEOUT_DURATION_MS, selectorProperty, selectorType);
        handlerContext = getHandlerContext(url, remoteAddr, user,  managementRequest.getContentType());

        if(!actionUri.equals(Transfer.GET_ACTION_URI) && !actionUri.equals(Transfer.DELETE_ACTION_URI) ){
            // todo attach body to request
            managementRequest.getBody().addDocument(body);
        }


        SOAP soapResponse = (SOAP) agent.handleRequest(managementRequest, handlerContext);
        return getResponse(soapResponse, handlerContext);
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


    private String getResourceUri(String resourceType) throws InvalidTargetObjectTypeException {
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
                resourceURIMap.put(element.toLowerCase(), new Pair<String,Class<ManagedObject>>(uri,MO));
            }
        }
        String resourceURI =  resourceURIMap.get(resourceType.toLowerCase()).left;
        if(resourceURI==null)
            throw new InvalidTargetObjectTypeException("Invalid resource type: "+resourceType) ;
        return resourceURI;

    }

    /**
     * Throws exception if there was an error in the management response
     */
    private Pair<Document,List<ManagedObject>> getResponse(final SOAP managementResponse,
                                                          final HandlerContext handlerContext)
            throws Exception {

        if ( managementResponse instanceof Identify) {
            throw new UnsupportedOperationException("Identify operation not supported");
        }

        if ( managementResponse.getBody().hasFault()) {
            throwException(managementResponse.getFault(), handlerContext);
        }

        List<ManagedObject> managedObjects = new ArrayList<>();
        Document responseDoc = createResultDocument();
        Node body = managementResponse.getBody();
        NodeList childNodes = body.getChildNodes();
        managedObjects.addAll(appendChildNodes(childNodes, responseDoc));
        responseDoc.normalizeDocument();
        return new Pair<Document,List<ManagedObject>>(responseDoc,managedObjects);

    }

    // Throws the exception from the handler context.  If that is not present, it creates and throws a runtime exception with the fault message
    private static void throwException(Fault fault, HandlerContext handlerContext) throws Exception {
        final Map<String,Object> properties = (Map<String,Object>)handlerContext.getRequestProperties();
        final Object exception = properties.get( "com.l7tech.status.exception" );
        if(exception==null){
            List<Reasontext> reasons = fault.getReason().getText();
            String faultReason = "";
            for(Reasontext reason: reasons){
                faultReason += reason.getValue() +" ";
            }

            throw new RuntimeException(faultReason);
        }
        throw (Exception)exception;
    }


    private List<ManagedObject> appendChildNodes(NodeList nodes, Document parentDocument) {
        List<ManagedObject> managedObjects = new ArrayList<>();
        for (int i = 0; i < nodes.getLength(); i++) {
            Node node = nodes.item(i);
            node = parentDocument.importNode(node, true);
            parentDocument.getDocumentElement().appendChild(node);
            DOMSource domSource = new DOMSource();
            domSource.setNode(node);
            Class<ManagedObject> MOclass = getMOClass(node.getLocalName());
            try {
                ManagedObject mo = MarshallingUtils.unmarshal(MOclass, domSource);
                managedObjects.add(mo);
            } catch (IOException e) {
                e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
            }
        }
        return managedObjects;

    }

    private Class<ManagedObject> getMOClass(String MOname) {
        return resourceURIMap.get(MOname.toLowerCase()).right;
    }

    private static Document createResultDocument() {
        return XmlUtil.createEmptyDocument("ManagementResult", ResourceHandler.MANAGEMENT_PREFIX, ResourceHandler.MANAGEMENT_NAMESPACE);
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

    public static WSManReflectiveAgent getAgent(  final Assertion assertion ) throws PolicyAssertionException {
        WSManReflectiveAgent agent = sharedAgent.get();
        if ( agent == null ) {
            agent = buildAgent( assertion );
            sharedAgent.compareAndSet( null, agent );
        }
        return agent;
    }

    private static WSManReflectiveAgent buildAgent( final Assertion assertion ) throws PolicyAssertionException {
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
                        validateAddressing(request );
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
    private static void validateAddressing( final Management request ) throws  Exception, SOAPException, JAXBException {
        ensureAnonymous( request.getReplyTo() );
        if ( request.getReplyTo() == null ) ensureAnonymous(  request.getFrom() );
        ensureAnonymous(  request.getFaultTo() );
    }

    private static void ensureAnonymous( final EndpointReferenceType endpointReference ) throws Exception  {
        if ( endpointReference != null &&
                endpointReference.getAddress() != null &&
                !Addressing.ANONYMOUS_ENDPOINT_URI.equals(endpointReference.getAddress().getValue()) ) {
            throw new Exception("Unsupported response endpoint address '" + endpointReference.getAddress().getValue() + "'");  // todo
        }
    }



    private static class SchemaValidationException extends Exception {
        private SchemaValidationException( final String message, final Throwable cause ) {
            super( message, cause );
        }
    }


}
