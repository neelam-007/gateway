package com.l7tech.external.assertions.gatewaymanagement.server;

import com.l7tech.common.io.XmlUtil;
import com.l7tech.gateway.api.ManagedObject;
import com.l7tech.gateway.common.security.rbac.PermissionDeniedException;
import com.l7tech.gateway.common.spring.remoting.RemoteUtils;
import com.l7tech.objectmodel.DuplicateObjectException;
import com.l7tech.objectmodel.EntityType;
import com.l7tech.objectmodel.StaleUpdateException;
import com.l7tech.server.util.JaasUtils;
import com.l7tech.util.BufferPoolByteArrayOutputStream;
import com.l7tech.util.ExceptionUtils;
import com.l7tech.util.ResourceUtils;
import com.sun.ws.management.AccessDeniedFault;
import com.sun.ws.management.AlreadyExistsFault;
import com.sun.ws.management.ConcurrencyFault;
import com.sun.ws.management.InvalidSelectorsFault;
import com.sun.ws.management.SchemaValidationErrorFault;
import com.sun.ws.management.addressing.ActionNotSupportedFault;
import com.sun.ws.management.addressing.DestinationUnreachableFault;
import com.sun.ws.management.framework.transfer.TransferSupport;
import com.sun.ws.management.server.BaseSupport;
import com.sun.ws.management.server.Filter;
import com.sun.ws.management.server.HandlerContext;
import com.sun.ws.management.Management;
import com.sun.ws.management.InternalErrorFault;
import com.sun.ws.management.enumeration.Enumeration;
import com.sun.ws.management.server.NamespaceMap;
import com.sun.ws.management.soap.FaultException;
import com.sun.ws.management.soap.SOAP;
import com.sun.ws.management.transfer.InvalidRepresentationFault;
import com.sun.ws.management.transfer.Transfer;
import com.sun.ws.management.transfer.TransferExtensions;
import com.sun.ws.management.framework.handlers.DefaultHandler;
import com.sun.ws.management.framework.Utilities;
import com.sun.ws.management.framework.enumeration.Enumeratable;
import com.sun.ws.management.framework.enumeration.EnumerationHandler;
import org.dmtf.schemas.wbem.wsman._1.wsman.MixedDataType;
import org.dmtf.schemas.wbem.wsman._1.wsman.SelectorType;
import org.springframework.beans.factory.BeanFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;
import org.xmlsoap.schemas.ws._2004._08.addressing.EndpointReferenceType;

import javax.security.auth.Subject;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.UnmarshalException;
import javax.xml.soap.SOAPElement;
import javax.xml.soap.SOAPException;
import javax.xml.soap.SOAPHeaderElement;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.security.PrivilegedExceptionAction;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.Map;
import java.util.HashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Resource handler that supports enumeration, get and a custom "echo" method.
 */
public class ResourceHandler extends DefaultHandler implements Enumeratable {

    //- PUBLIC

    public static final String MANAGEMENT_NAMESPACE = "http://ns.l7tech.com/2010/01/gateway-management";
    public static final String MANAGEMENT_PREFIX = "l7";

    /**
     * Extend handle to provide the correct context for an administrator action.
     */
    @Override
    public void handle( final String action,
                        final String resourceURI,
                        final HandlerContext context,
                        final Management request,
                        final Management response ) throws Exception {
        final String remoteAddress = (String)context.getRequestProperties().get("com.l7tech.remoteAddr");
        final Subject subject = new Subject();
        if ( context.getPrincipal() != null ) {
            subject.getPrincipals().add( context.getPrincipal() );
        }
        Subject.doAs( subject, new PrivilegedExceptionAction<Void>(){
            @Override
            public Void run() throws Exception {
                final Exception[] exceptionHolder = new Exception[1];
                RemoteUtils.runWithConnectionInfo(remoteAddress, null, new Runnable(){
                    @Override
                    public void run() {
                        try {
                            ResourceHandler.super.handle(action, resourceURI, context, request, response);
                            setOperationInfo( context, null, null, null, "Success" );
                        } catch (Exception e) {
                            exceptionHolder[0] = e;
                        }
                    }
                });

                if ( exceptionHolder[0] != null )
                    throw exceptionHolder[0];

                return null;
            }
        } );
    }

    /**
     * Get resource.
     */
    @Override
    public void get( final HandlerContext context,
                     final Management request,
                     final Management response ) {
        final OperationContext opContext = new OperationContext( context, request );
        final ResourceFactory<?> factory = opContext.getResourceFactory();
        final Map<String,String> selectorMap = buildSelectorMap( context, request, factory.getSelectors() );
        setOperationInfo( context, "Read", null, getEntityType(factory), null );

        try {
            final TransferExtensions transferRequest = new TransferExtensions(request);
            final TransferExtensions transferResponse = new TransferExtensions(response);

            final Object resource = factory.getResource( selectorMap );
            setOperationInfo( context, null, getEntityId(resource), null, null );

            addNamespaces(transferResponse);
            transferResponse.setFragmentGetResponse( transferRequest.getFragmentHeader(), resource );
        } catch (Exception e) {
            handleOperationException(context, e);
        }
    }

    /**
     * Put resource.
     */
    @Override
    public void put( final HandlerContext context,
                     final Management request,
                     final Management response ) {
        final OperationContext opContext = new OperationContext( context, request );
        final ResourceFactory<?> factory = opContext.getResourceFactory();
        setOperationInfo( context, "Update", null, getEntityType(factory), null );

        if ( factory.isReadOnly() ) {
            throw new ActionNotSupportedFault(Transfer.PUT_ACTION_URI);
        }

        final Map<String,String> selectorMap = buildSelectorMap( context, request, factory.getSelectors() );

        try {
            final TransferExtensions transferRequest = new TransferExtensions(request);
            final TransferExtensions transferResponse = new TransferExtensions(response);
            final SOAPHeaderElement fragmentHeader = transferRequest.getFragmentHeader();

            final Object resourcePayload = getResourceWithFragmentUpdate( factory, selectorMap, transferRequest, fragmentHeader );

            final Object resource = factory.putResource( selectorMap, resourcePayload );
            setOperationInfo( context, null, getEntityId(resource), null, null );

            addNamespaces(transferResponse);
            if ( resource == null ) {
                transferResponse.setPutResponse();
            } else if ( fragmentHeader != null ) {
			    transferResponse.setFragmentPutResponse( fragmentHeader, resource );
            } else {
                transferResponse.setPutResponse( resource );
            }
        } catch (Exception e) {
            handleOperationException(context, e);
        }
    }

    /**
     * Delete resource.
     */
    @Override
    public void delete( final HandlerContext context,
                        final Management request,
                        final Management response ) {
        final OperationContext opContext = new OperationContext( context, request );
        final ResourceFactory<?> factory = opContext.getResourceFactory();
        setOperationInfo( context, "Delete", null, getEntityType(factory), null );

        if ( factory.isReadOnly() ) {
            throw new ActionNotSupportedFault(Transfer.DELETE_ACTION_URI);
        }

        final Map<String,String> selectorMap = buildSelectorMap( context, request, factory.getSelectors() );

        try {
            final TransferExtensions transferResponse = new TransferExtensions(response);

            String id = factory.deleteResource( selectorMap );
            setOperationInfo( context, null, id, null, null );

            transferResponse.setDeleteResponse();
        } catch (Exception e) {
            handleOperationException(context, e);
        }
    }

    /**
     * Create resource.
     */
    @Override
    public void create( final HandlerContext context,
                        final Management request,
                        final Management response ) {
        final OperationContext opContext = new OperationContext( context, request );
        final ResourceFactory<?> factory = opContext.getResourceFactory();
        setOperationInfo( context, "Create", null, getEntityType(factory), null );

        if ( factory.isReadOnly() ) {
            throw new ActionNotSupportedFault(Transfer.CREATE_ACTION_URI);
        }

        try {
            final TransferExtensions transferRequest = new TransferExtensions(request);
            final TransferExtensions transferResponse = new TransferExtensions(response);

            final Object resourcePayload = getResource( transferRequest );

            final Map<String,String> selectorMap = factory.createResource( resourcePayload );
            setOperationInfo( context, null, selectorMap.get( "id" ), null, null );

            final EndpointReferenceType epr = TransferSupport.createEpr(
					request.getTo(),
                    request.getResourceURI(),
                    selectorMap);

	        transferResponse.setCreateResponse(epr);
        } catch (Exception e) {
            handleOperationException(context, e);
        }
    }

    /**
     * Custom resource method 
     */
    @Override
    public boolean customDispatch( final String action,
                                   final HandlerContext context,
                                   final Management request,
                                   final Management response ) throws Exception {
        boolean handled = false;
        setOperationInfo( context, "Invoke", null, null, null );

        if ( action != null && action.startsWith(MANAGEMENT_NAMESPACE+"/") ) {
            final OperationContext opContext = new OperationContext( context, request );
            final ResourceFactory<?> factory = opContext.getResourceFactory();
            final String resourceUri = opContext.getResourceUri();

            final String customMethod = action.startsWith(resourceUri+"/") ?
                    action.substring(resourceUri.length()+1) :
                    action.substring(MANAGEMENT_NAMESPACE.length()+1);
            final Method method = getCustomMethod( factory.getClass(), customMethod );
            final ResourceFactory.ResourceMethod resourceMethod = method.getAnnotation(ResourceFactory.ResourceMethod.class);

            setOperationInfo( context, "Invoke ("+customMethod+")", null, getEntityType(factory), null );
            response.setAction( action + "Response" );

            try {
                final TransferExtensions transferRequest = new TransferExtensions(request);
                final TransferExtensions transferResponse = new TransferExtensions(response);

                final Collection<Object> parameters = new ArrayList<Object>();
                if ( resourceMethod.selectors() ) {
                    parameters.add( buildSelectorMap( context, request, factory.getSelectors() ) );
                }
                if ( resourceMethod.resource() ) {
                    parameters.add( getResource( transferRequest ) );
                }

                final Object customResponse;
                try {
                    customResponse = method.invoke( factory, parameters.toArray(new Object[parameters.size()]) );
                } catch ( IllegalArgumentException iae ) {
                    throw new InvalidRepresentationFault(InvalidRepresentationFault.Detail.INVALID_NAMESPACE);
                }

                addNamespaces(transferResponse);
                if( customResponse instanceof Document ) {
                    transferResponse.getBody().addDocument((Document)customResponse);
                } else if ( customResponse != null ) {
                    transferResponse.getXmlBinding().marshal(customResponse, transferResponse.getBody());
                }
            } catch (InvocationTargetException ite) {
                handleOperationException(context, ite.getCause());
            } catch (Exception e) {
                handleOperationException(context, e);
            }

            handled = true;
        }

        return handled;
    }

    /**
     * Enumeration support
     */
    @Override
    public void release( final HandlerContext context, final Enumeration enuRequest, final Enumeration enuResponse ) {
        setOperationInfo( context, "Enumerate (Release)", null, null, null );
        enumHandler.release( context, enuRequest, enuResponse );
    }

    /**
     * Enumeration support
     */
    @Override
    public void pull( final HandlerContext context, final Enumeration enuRequest, final Enumeration enuResponse ) {
        setOperationInfo( context, "Enumerate (Pull)", null, null, null );
        enumHandler.pull( context, enuRequest, enuResponse );
    }

    /**
     * Enumeration support
     */
    @Override
    public void enumerate( final HandlerContext context, final Enumeration enuRequest, final Enumeration enuResponse ) {
        setOperationInfo( context, "Enumerate", null, null, null );
        enumHandler.enumerate( context, enuRequest, enuResponse );
    }

    /**
     * Enumeration support
     */
    @Override
    public void getStatus( final HandlerContext context, final Enumeration enuRequest, final Enumeration enuResponse ) {
        setOperationInfo( context, "Enumerate (Status)", null, null, null );
        enumHandler.getStatus( context, enuRequest, enuResponse );
    }

    /**
     * Enumeration support
     */
    @Override
    public void renew( final HandlerContext context, final Enumeration enuRequest, final Enumeration enuResponse ) {
        setOperationInfo( context, "Enumerate (Renew)", null, null, null );
        enumHandler.renew( context, enuRequest, enuResponse );
    }

    //- PRIVATE

    private static final Logger logger = Logger.getLogger( ResourceHandler.class.getName() );

    private final ResourceEnumerationHandler enumHandler = new ResourceEnumerationHandler();

    /**
     * Build a map of the selectors present in the given message
     */
    private Map<String,String> buildSelectorMap( final HandlerContext context,
                                                 final Management message,
                                                 final Collection<String> selectorNames ) throws InternalErrorFault {
        final Map<String,String> selectorMap = new HashMap<String,String>();

        final Set<SelectorType> selectors;
        try {
            selectors = message.getSelectors();
        } catch ( Exception e ) {
            handleOperationException(context, e);
            return null; //not reached
        }

        for ( final String name : selectorNames ) {
            final SelectorType selector = selectors==null ? null : Utilities.getSelectorByName( name, selectors);

            if ( selector != null ) {
                final List<Serializable> values = selector.getContent();
                if ( values != null && !values.isEmpty() ) {
                    final Serializable valueSer = values.get(0);
                    if ( valueSer instanceof String ) {
                        selectorMap.put( name, (String) valueSer );
                    }
                }
            }
        }

        return selectorMap;
    }

    /**
     * Get the resource payload from the request as an Object or a Document
     */
    private Object getResource( final TransferExtensions transferRequest ) throws SOAPException {
        Object resource = null;

        try {
            resource = transferRequest.getResource();
        } catch (JAXBException je) {
            logger.log( Level.FINE,
                    "Request resource JAXB error : " + ExceptionUtils.getMessage( je ),
                    ExceptionUtils.getDebugException( je ));

            if ( je instanceof UnmarshalException ) {
                throw new SchemaValidationErrorFault( SOAP.createFaultDetail(ExceptionUtils.getMessage( je ), null, null, null) );
            }

            // Fall back to processing as a Document
            SOAPElement[] elements = transferRequest.getChildren( transferRequest.getBody() );
            if ( elements.length > 0 && elements[0] != null ) {
                SOAPElement element = elements[0];
                BufferPoolByteArrayOutputStream out = null;
                InputStream in = null;
                try {
                    out = new BufferPoolByteArrayOutputStream(4096);
                    XmlUtil.canonicalize( element, out );
                    in = out.toInputStream();
                    resource = XmlUtil.parse( in );
                } catch (SAXException e) {
                    throw ExceptionUtils.wrap( e );
                } catch (IOException e) {
                    throw ExceptionUtils.wrap( e );
                } finally {
                    ResourceUtils.closeQuietly( out );
                    ResourceUtils.closeQuietly( in );
                }
            }
        }

        return resource;
    }

    /**
     * Get the updated resource representation either from the request or from the current state plus a fragment update.
     */
    @SuppressWarnings({ "unchecked" })
    private Object getResourceWithFragmentUpdate( final ResourceFactory<?> factory,
                                                  final Map<String, String> selectorMap,
                                                  final TransferExtensions transferRequest,
                                                  final SOAPHeaderElement fragmentHeader ) throws SOAPException, ResourceFactory.InvalidResourceException, ResourceFactory.ResourceNotFoundException, JAXBException {
        Object resourcePayload = getResource( transferRequest );

        if ( fragmentHeader != null ) {
            if ( !(resourcePayload instanceof JAXBElement) ||
                !MixedDataType.class.equals(((JAXBElement)resourcePayload).getDeclaredType()) ) {
                throw new ResourceFactory.InvalidResourceException(ResourceFactory.InvalidResourceException.ExceptionType.UNEXPECTED_TYPE, "unexpected fragment");
            }

            // Build filter
            final MixedDataType type = ((JAXBElement<MixedDataType>)resourcePayload).getValue();
            final NamespaceMap nsMap = new NamespaceMap(fragmentHeader);
            final NodeList nodes = fragmentHeader.getChildNodes();
            final List<Node> nodeList = new ArrayList<Node>(nodes.getLength());
            for (int i = 0; i < nodes.getLength(); i++) {
                nodeList.add(nodes.item(i));
            }
            final Filter filter = BaseSupport.createFilter(transferRequest.getFragmentDialect(), nodeList, nsMap);

            // Get resource as DOM
            final Object resource = factory.getResource( selectorMap );
            final Document doc = XmlUtil.createEmptyDocument();
            transferRequest.getXmlBinding().marshal(resource, doc);

            // Find and update the matched fragments
            final NodeList filtered = filter.evaluate( doc.getDocumentElement() );

            // Fail if the filter and resource fragment are not single items, we cannot
            // safely update multiple items
            if ( filtered.getLength() != 1 ||
                 filtered.item( 0 ).getParentNode() == null ||
                 type.getContent().size() != 1 ||
                 (!(type.getContent().get(0) instanceof Node) &&
                  !(type.getContent().get(0) instanceof String) ) ) {
                throw new ResourceFactory.InvalidResourceException(ResourceFactory.InvalidResourceException.ExceptionType.INVALID_VALUES, "invalid fragment");
            }
            final Node target = filtered.item( 0 );
            final Node parent = target.getParentNode();
            Object sourceObj = type.getContent().get(0);
            if ( sourceObj instanceof String ) {
                sourceObj = target.getOwnerDocument().createTextNode( (String) sourceObj );
            }
            final Node source = (Node) sourceObj;
            if ( source.getNodeType() != target.getNodeType() ) {
                throw new ResourceFactory.InvalidResourceException(ResourceFactory.InvalidResourceException.ExceptionType.UNEXPECTED_TYPE, "fragment type mismatch");
            }
            parent.replaceChild( parent.getOwnerDocument().importNode(source, true), target );
            resourcePayload = transferRequest.getXmlBinding().unmarshal( doc.getDocumentElement() );
        }

        return resourcePayload;
    }

    /**
     * Get the Method for the requested custom method.
     */
    private static Method getCustomMethod( final Class<?> resourceFactoryClass, final String customMethodName ) {
        Method customMethod = null;

        for ( final Method method : resourceFactoryClass.getDeclaredMethods() ) {
            ResourceFactory.ResourceMethod resourceMethod = method.getAnnotation(ResourceFactory.ResourceMethod.class);
            if ( resourceMethod != null && resourceMethod.name().equals(customMethodName) ) {
                customMethod = method;
                break;
            }
        }

        if ( customMethod == null )
            throw new InvalidSelectorsFault(InvalidSelectorsFault.Detail.INVALID_VALUE);

        return customMethod;
    }

    private void addNamespaces( final TransferExtensions transferResponse ) throws SOAPException {
        transferResponse.addNamespaceDeclarations( new HashMap<String,String>(){{
            put(MANAGEMENT_PREFIX, MANAGEMENT_NAMESPACE);
            //put("xs", XMLConstants.W3C_XML_SCHEMA_NS_URI);
        }} );
    }

    private String getEntityType( final ResourceFactory<?> factory ) {
        final EntityType type = factory.getType();
        return type==null ? null : type.getName();
    }

    private String getEntityId( final Object resource ) {
        String id = null;

        if ( resource instanceof ManagedObject ) {
            id = ((ManagedObject) resource).getId();
        }

        return id;
    }

    @SuppressWarnings({ "unchecked" })
    private void setOperationInfo( final HandlerContext context,
                                   final String action,
                                   final String entityId,
                                   final String entityType,
                                   final String message ) {
        Map<String,Object> properties = (Map<String,Object>) context.getRequestProperties();
        if ( action != null )
            properties.put( "com.l7tech.status.action", action );
        if ( entityId != null )
            properties.put( "com.l7tech.status.entityId", entityId );
        if ( entityType != null )
            properties.put( "com.l7tech.status.entityType", entityType );
        if ( message != null )
            properties.put( "com.l7tech.status.message", message );
    }

    /**
     * Translate internal exceptions into ws-management faults
     */
    private void handleOperationException( final HandlerContext context,
                                           final Throwable e ) {
        if ( e instanceof ResourceFactory.InvalidResourceSelectors ) {
            setOperationInfo( context, null, null, null, "Entity not found" );
            throw new InvalidSelectorsFault(InvalidSelectorsFault.Detail.INSUFFICIENT_SELECTORS);
        } else if ( e instanceof ResourceFactory.ResourceNotFoundException ) {
            setOperationInfo( context, null, null, null, "Entity not found" );
            throw new InvalidSelectorsFault(InvalidSelectorsFault.Detail.INVALID_VALUE);
        } else if ( e instanceof ResourceFactory.InvalidResourceException ) {
            ResourceFactory.InvalidResourceException ire = (ResourceFactory.InvalidResourceException) e;
            String detail = null;
            switch (ire.getType()) {
                case INVALID_VALUES:
                    detail = InvalidRepresentationFault.Detail.INVALID_VALUES.toString();
                    break;
                case MISSING_VALUES:
                    detail = InvalidRepresentationFault.Detail.MISSING_VALUES.toString();
                    break;
                case UNEXPECTED_TYPE:
                    detail = InvalidRepresentationFault.Detail.INVALID_NAMESPACE.toString();
                    break;
            }
            setOperationInfo( context, null, null, null, ExceptionUtils.getMessage( e ) );
            throw new InvalidRepresentationFault(SOAP.createFaultDetail(ExceptionUtils.getMessage( e ), detail, null, null));
        } else if ( e instanceof PermissionDeniedException ) {
            String userId = JaasUtils.getCurrentUser()==null ? "<unauthenticated>" : JaasUtils.getCurrentUser().getLogin();
            logger.warning( ExceptionUtils.getMessage(e) + ", for user '"+userId+"'.");
            setOperationInfo( context, null, null, null, "Access denied" );
            throw new AccessDeniedFault();
        } else if ( e instanceof ResourceFactory.ResourceAccessException ) {
            Throwable cause = e.getCause();
            if ( cause instanceof DuplicateObjectException ) {
                setOperationInfo( context, null, null, null, ExceptionUtils.getMessage( cause ) );
                throw new AlreadyExistsFault(SOAP.createFaultDetail(ExceptionUtils.getMessage( cause ), null, null, null));
            } else if ( cause instanceof StaleUpdateException ) {
                setOperationInfo( context, null, null, null, "Incorrect version (stale update)" );
                throw new ConcurrencyFault();
            } else {
                setOperationInfo( context, null, null, null, "Error: " + ExceptionUtils.getMessage(e) );
                throw new InternalErrorFault(ExceptionUtils.getMessage(e));
            }
        } else if ( e instanceof FaultException ) {
            setOperationInfo( context, null, null, null, ExceptionUtils.getMessage(e) );
            throw (FaultException) e;
        } else {
            logger.log( Level.WARNING, "Error processing management request", e );
            setOperationInfo( context, null, null, null, "Error processing request '" + ExceptionUtils.getMessage(e) + "'" );
            throw new InternalErrorFault(SOAP.createFaultDetail(ExceptionUtils.getMessage(e), null, ExceptionUtils.getDebugException(e), null));
        }
    }

    /**
     * Context for processing a single operation.
     */
    private static final class OperationContext {
        private final HandlerContext context;
        private final String resourceUri;
        private BeanFactory beanFactory;
        private ResourceFactoryRegistry resourceFactoryRegistry;

        private OperationContext( final HandlerContext context, final Management request ) {
            this.context = context;
            this.resourceUri = getResourceUri( request );
        }

        private String getResourceUri( final Management request ) {
            String resourceUri;
            try {
                resourceUri = request.getResourceURI();
            } catch (SOAPException e) {
                throw new DestinationUnreachableFault("Error accessing ResourceURI",
                        DestinationUnreachableFault.Detail.INVALID_RESOURCE_URI);
            } catch (JAXBException e) {
                throw new DestinationUnreachableFault("Error accessing ResourceURI",
                        DestinationUnreachableFault.Detail.INVALID_RESOURCE_URI);
            }

            if ( resourceUri==null ) {
                throw new DestinationUnreachableFault("ResourceURI missing or empty",
                        DestinationUnreachableFault.Detail.INVALID_RESOURCE_URI);
            }
            return resourceUri;
        }

        private String getResourceUri() {
            return resourceUri;
        }

        private ResourceFactory<?> getResourceFactory() {
            final ResourceFactory<?> factory = getResourceFactoryRegistry().getResourceFactory(resourceUri);
            if ( factory == null ) {
                throw new DestinationUnreachableFault("ResourceURI: " + resourceUri,
                        DestinationUnreachableFault.Detail.INVALID_RESOURCE_URI);
            }
            return factory;
        }

        private BeanFactory getBeanFactory() {
            if ( beanFactory == null ) {
                beanFactory = (BeanFactory) context.getRequestProperties().get("com.l7tech.context");
            }
            return beanFactory;
        }

        private ResourceFactoryRegistry getResourceFactoryRegistry() {
            if ( resourceFactoryRegistry == null ) {
                resourceFactoryRegistry =
                    (ResourceFactoryRegistry) getBeanFactory().getBean("resourceFactoryRegistry", ResourceFactoryRegistry.class);
            }
            return resourceFactoryRegistry;
        }
    }

    /**
     * EnumerationHandler has a protected constructor
     */
    private static final class ResourceEnumerationHandler extends EnumerationHandler {}
}
