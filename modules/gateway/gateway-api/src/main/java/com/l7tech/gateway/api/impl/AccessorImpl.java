package com.l7tech.gateway.api.impl;

import com.l7tech.gateway.api.AccessibleObject;
import com.l7tech.gateway.api.Accessor;
import com.l7tech.gateway.api.ManagedObject;
import com.l7tech.gateway.api.ManagedObjectFactory;
import com.l7tech.util.ConfigFactory;
import com.l7tech.util.ExceptionUtils;
import com.sun.ws.management.client.EnumerationCtx;
import com.sun.ws.management.client.Resource;
import com.sun.ws.management.client.ResourceFactory;
import com.sun.ws.management.client.ResourceState;
import com.sun.ws.management.client.exceptions.FaultException;
import com.sun.ws.management.server.EnumerationItem;
import org.dmtf.schemas.wbem.wsman._1.wsman.SelectorSetType;
import org.dmtf.schemas.wbem.wsman._1.wsman.SelectorType;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import javax.xml.XMLConstants;
import javax.xml.bind.JAXBException;
import javax.xml.bind.annotation.XmlSchema;
import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.soap.SOAPException;
import java.io.Closeable;
import java.io.IOException;
import java.io.Serializable;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

/**
 * 
 */
class AccessorImpl<AO extends AccessibleObject> implements Accessor<AO> {

    //- PUBLIC

    @Override
    public Class<AO> getType() {
        return typeClass;
    }

    @Override
    public AO get( final String identifier ) throws AccessorException {
        require( "identifier", identifier );
        return get( Collections.<String,Object>singletonMap(ID_SELECTOR, identifier) );
    }

    @Override
    public AO get( final String property, final Object value ) throws AccessorException {
        require( "property", property );
        require( "value", value );
        return get( Collections.singletonMap( property, value ));
    }

    @Override
    public AO get( final Map<String, Object> propertyMap ) throws AccessorException {
        require( "propertyMap", propertyMap );
        return invoke(new AccessorMethod<AO>(){
            @Override
            public AO invoke() throws DatatypeConfigurationException, FaultException, JAXBException, SOAPException, IOException, AccessorException {
                final Resource resource =
                        getResourceFactory().find( url, resourceUri, timeout, asSelectorMap(propertyMap))[0];

                return ManagedObjectFactory.read( fixNs(resource.get().getDocument()), typeClass);
            }
        });
    }

    @Override
    public AO put( final AO item ) throws AccessorException {
        require( "item", item );
        return invoke(new AccessorMethod<AO>(){
            @Override
            public AO invoke() throws DatatypeConfigurationException, FaultException, JAXBException, SOAPException, IOException {
                final Resource resource =
                        getResourceFactory().find( url, resourceUri, timeout, Collections.singletonMap(ID_SELECTOR, item.getId()))[0];

                final ResourceState resourceState = resource.put( ManagedObjectFactory.write( item ) );

                return ManagedObjectFactory.read( resourceState.getDocument(), typeClass );
            }
        });
    }

    @Override
    public String create( final AO item ) throws AccessorException {
        require( "item", item );
        return invoke(new AccessorMethod<String>(){
            @Override
            public String invoke() throws DatatypeConfigurationException, FaultException, JAXBException, SOAPException, IOException {
                final Resource resource =
                        getResourceFactory().create( url, resourceUri, timeout, ManagedObjectFactory.write( item ), null);

                return getId(resource);
            }
        });
    }

    @Override
    public void delete( final String identifier ) throws AccessorException {
        require( "identifier", identifier );
        delete( Collections.<String,Object>singletonMap(ID_SELECTOR, identifier) );
    }

    @Override
    public void delete( final String property, final Object value ) throws AccessorException {
        require( "property", property );
        require( "value", value );
        delete( Collections.singletonMap( property, value ));
    }

    @Override
    public void delete( final Map<String, Object> propertyMap ) throws AccessorException {
        require( "propertyMap", propertyMap );
        invoke(new AccessorMethod<AO>(){
            @Override
            public AO invoke() throws DatatypeConfigurationException, FaultException, JAXBException, SOAPException, IOException, AccessorException {
                final ResourceFactory resourceFactory = getResourceFactory();
                final Resource resource =
                        resourceFactory.find( url, resourceUri, timeout, asSelectorMap(propertyMap))[0];

                resourceFactory.delete( resource );
                return null;
            }
        });
    }

    @Override
    public Iterator<AO> enumerate() throws AccessorException {
        return new EnumeratingIterator<AO>( url, resourceUri, timeout, typeClass, resourceTracker, getResourceFactory() );
    }

    //- PACKAGE

    static final String ID_SELECTOR = "id";

    AccessorImpl( final String url,
                  final String resourceUri,
                  final Class<AO> typeClass,
                  final ResourceFactory resourceFactory,
                  final ResourceTracker resourceTracker ) {
        this.url = url;
        this.resourceUri = resourceUri;
        this.typeClass = typeClass;
        this.timeout = DEFAULT_TIMEOUT;
        this.resourceTracker = resourceTracker;
        this.resourceFactory = resourceFactory;
    }

    String getUrl() {
        return url;
    }

    String getResourceUri() {
        return resourceUri;
    }

    int getTimeout() {
        return timeout;
    }

    void require( final String description, final Object value ) throws AccessorException {
        if ( value == null ) throw new AccessorException( description + " is required" );        
    }

    String getNamespace() {
        return AccessorImpl.class.getPackage().getAnnotation(XmlSchema.class).namespace();
    }

    String buildResourceScopedActionUri( final String name ) {
        return resourceUri + "/" + name;
    }

    Document newDocument() {
        try {
            return DocumentBuilderFactory.newInstance().newDocumentBuilder().newDocument();
        } catch ( ParserConfigurationException e ) {
            throw new AccessorRuntimeException(e);
        }
    }

    ResourceFactory getResourceFactory() {
        return resourceFactory;
    }

    <R> R invoke( final AccessorMethod<R> accessMethod ) throws AccessorException {
        try {
            return accessMethod.invoke();
        } catch ( DatatypeConfigurationException e ) {
            throw new AccessorRuntimeException(e);
        } catch ( FaultException e ) {
            if ( e.getMessage().contains("InvalidSelectors") &&
                 e.getMessage().contains("http://schemas.dmtf.org/wbem/wsman/1/wsman/faultDetail/InvalidValue")) {
                throw new AccessorNotFoundException("Item not found");
            }
            throw new AccessorSOAPFaultException(e);
        } catch ( JAXBException e ) {
            throw new AccessorRuntimeException(e);
        } catch ( SOAPException e ) {
            throw new AccessorRuntimeException(e);
        } catch ( IOException e ) {
            if ( isNetworkException(e) ) {
                throw new AccessorNetworkException(e);
            }
            throw new AccessorRuntimeException(e);
        }
    }

    interface AccessorMethod<R> {
        R invoke() throws AccessorException, DatatypeConfigurationException, FaultException, JAXBException, SOAPException, IOException;
    }

    //- PRIVATE

    private static final int DEFAULT_TIMEOUT = ConfigFactory.getIntProperty( "com.l7tech.gateway.api.operationTimeout", 300000 );

    private final String url;
    private final String resourceUri;
    private final Class<AO> typeClass;
    private final int timeout;
    private final ResourceTracker resourceTracker;
    private final ResourceFactory resourceFactory;

    private String getId( final Resource resource ) {
        return getId( resource.getSelectorSet() );
    }

    private String getId( final SelectorSetType selectorSet ) {
        String id = null;

        final List<SelectorType> selectors = selectorSet.getSelector();
        if ( selectors != null ) {
            for ( final SelectorType selector : selectors ) {
                if ( ID_SELECTOR.equals( selector.getName() ) ) {
                    final List<Serializable> values = selector.getContent();
                    if ( values != null && !values.isEmpty() ) {
                        final Serializable valueSer = values.get(0);
                        if ( valueSer instanceof String ) {
                            id = (String) valueSer;
                            break;
                        }
                    }
                }
            }
        }

        return id;
    }
                    
    private String asSelectorValue( final Object valueObject ) throws AccessorException {
        String value;

        if ( valueObject instanceof String ) {
            value = (String) valueObject;
        } else if ( valueObject instanceof Integer ) {
            value = Integer.toString((Integer)valueObject);            
        } else {
            throw new AccessorException("Unsupported value type '"+(valueObject==null ? "<null>" : valueObject.getClass().getName())+"'.");
        }

        return value;
    }

    private Map<String,String> asSelectorMap( final Map<String, Object> propertyMap ) throws AccessorException {
        //TODO document where this size restriction came from and if it exists as a real limit downstream
        if ( propertyMap.size() > 10 ) throw new AccessorException("Too many properties");
        final Map<String,String> selectorMap = new HashMap<String,String>();

        for ( Map.Entry<String,Object> entry : propertyMap.entrySet() ) {
            selectorMap.put( entry.getKey(), asSelectorValue(entry.getValue()) );                   
        }

        return selectorMap;
    }

    /**
     * Fix namespace issue that occurs due to extracting a Document from the SOAP Body.
     *
     * <p>This can be removed if the xs prefix is declared on the first child element of the SOAP body.</p>
     *
     * @see javax.xml.soap.SOAPBody#extractContentAsDocument()
     */
    private Document fixNs( final Document document ) {
        document.getDocumentElement().setAttributeNS(
                XMLConstants.XMLNS_ATTRIBUTE_NS_URI,
                "xmlns:xs",
                XMLConstants.W3C_XML_SCHEMA_NS_URI );
        return document;
    }

    private static boolean isNetworkException( final IOException exception ) {
        return ExceptionUtils.causedBy( exception, UnknownHostException.class ) ||
               ExceptionUtils.causedBy( exception, SocketException.class ) ||
               ExceptionUtils.causedBy( exception, SocketTimeoutException.class ) ||
               (ExceptionUtils.causedBy( exception, IOException.class ) && ExceptionUtils.getMessage( exception ).startsWith( "Content-Type of response is not acceptable" ));
    }

    /**
     *  
     */
    private static final class EnumeratingIterator<IMO extends ManagedObject> implements Iterator<IMO>, Closeable {

        //- PUBLIC

        @Override
        public boolean hasNext() {
            if ( nextItems.isEmpty() && !complete ) {
                fetchNext();
            }
            return !nextItems.isEmpty();
        }

        @Override
        public IMO next() {
            if ( nextItems.isEmpty() && !complete ) {
                fetchNext();
            }

            IMO nextItem;
            if ( nextItems.isEmpty() ) {
                throw new NoSuchElementException();
            } else {
                nextItem = nextItems.remove( 0 );
            }

            return nextItem;
        }

        @Override
        public void remove() {
            throw new UnsupportedOperationException();
        }

        @Override
        public void close() throws IOException {
            if ( !complete ) {
                try {
                    final Resource resource =
                        resourceFactory.find( url, resourceUri, timeout, (SelectorSetType) null )[0];

                    if ( context != null ) {
                        resource.release( context );
                    }

                    complete = true;
                    context = null;
                } catch ( DatatypeConfigurationException e ) {
                    throw new IOException( e );
                } catch ( FaultException e ) {
                    throw new IOException( e );
                } catch ( JAXBException e ) {
                    throw new IOException( e );
                } catch ( SOAPException e ) {
                    throw new IOException( e );
                }
            }
        }

        //- PRIVATE

        private final String url;
        private final String resourceUri;
        private final Class<IMO> typeClass;
        private final int timeout;
        private final ResourceTracker resourceTracker;
        private final ResourceFactory resourceFactory;
        private final int batchSize = ConfigFactory.getIntProperty( "com.l7tech.gateway.api.enumerationBatchSize", 10 );
        private final List<IMO> nextItems = new ArrayList<IMO>(batchSize);

        private EnumerationCtx context;
        private boolean complete = false;

        private EnumeratingIterator( final String url,
                                     final String resourceUri,
                                     final int timeout,
                                     final Class<IMO> typeClass,
                                     final ResourceTracker resourceTracker,
                                     final ResourceFactory resourceFactory ) {
            this.url = url;
            this.resourceUri = resourceUri;
            this.typeClass = typeClass;
            this.timeout = timeout;
            this.resourceTracker = resourceTracker;
            this.resourceFactory = resourceFactory;
            resourceTracker.registerCloseable( this );
        }

        private void fetchNext() {
            if ( !complete ) {
                try {
                    final Resource resource =
                        resourceFactory.find( url, resourceUri, timeout, (SelectorSetType) null )[0];

                    if ( context == null ) {
                        context = resource.enumerate( null, null, null, false, true );
                    }

                    final List<EnumerationItem> items = resource.pullItems( context, timeout, batchSize, 0, false );
                    for ( EnumerationItem item : items ) {
                        nextItems.add( ManagedObjectFactory.read( ((Element) item.getItem()).getOwnerDocument(), typeClass ));
                    }

                    if ( resource.isEndOfSequence() || nextItems.isEmpty() ) {
                        complete = true;
                        context = null;
                        resourceTracker.unregisterCloseable(this);
                    }
                } catch ( DatatypeConfigurationException e ) {
                    throw new AccessorRuntimeException( e );
                } catch ( FaultException e ) {
                    throw new AccessorSOAPFaultException( e );
                } catch ( JAXBException e ) {
                    throw new AccessorRuntimeException( e );
                } catch ( SOAPException e ) {
                    throw new AccessorRuntimeException( e );
                } catch ( IOException e ) {
                    if ( isNetworkException(e) ) {
                        throw new AccessorNetworkException(e);
                    }
                    throw new AccessorRuntimeException( e );
                }
            }
        }
    }
}
