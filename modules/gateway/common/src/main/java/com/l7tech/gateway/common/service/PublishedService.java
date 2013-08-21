package com.l7tech.gateway.common.service;

import com.l7tech.common.http.HttpMethod;
import com.l7tech.objectmodel.Goid;
import com.l7tech.objectmodel.folder.Folder;
import com.l7tech.objectmodel.folder.HasFolder;
import com.l7tech.objectmodel.imp.PersistentEntityUtil;
import com.l7tech.objectmodel.imp.ZoneableNamedEntityImp;
import com.l7tech.objectmodel.migration.Migration;
import com.l7tech.objectmodel.migration.PropertyResolver;
import com.l7tech.policy.Policy;
import com.l7tech.policy.PolicyType;
import com.l7tech.search.Dependency;
import com.l7tech.util.Functions.TernaryThrows;
import com.l7tech.wsdl.SerializableWSDLLocator;
import com.l7tech.wsdl.Wsdl;
import com.l7tech.xml.soap.SoapUtil;
import com.l7tech.xml.soap.SoapVersion;

import javax.persistence.Transient;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;
import javax.wsdl.BindingOperation;
import javax.wsdl.Port;
import javax.wsdl.WSDLException;
import javax.wsdl.extensions.ExtensibilityElement;
import javax.wsdl.extensions.soap.SOAPOperation;
import javax.wsdl.extensions.soap12.SOAP12Operation;
import javax.xml.bind.annotation.XmlRootElement;
import java.io.Flushable;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectOutputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Logger;

import static com.l7tech.common.http.HttpMethod.*;
import static com.l7tech.objectmodel.migration.MigrationMappingSelection.NONE;

/**
 * A service that is published by the SecureSpan Gateway.  Primarily contains references to a WSDL and a policy.
 * Must be treated as immutable and thread-safe from the point of view of the SSG's message processing subsystem.
 *
 * @author alex
 */
@SuppressWarnings( { "NonJaxWsWebServices" } )
@XmlRootElement
public class PublishedService extends ZoneableNamedEntityImp implements Flushable, HasFolder {
    //private static final long serialVersionUID = 8711916262379377867L;
    private static final Logger logger = Logger.getLogger(PublishedService.class.getName());

    public static final EnumSet<HttpMethod> METHODS_SOAP = EnumSet.of(POST);
    public static final EnumSet<HttpMethod> METHODS_REST = EnumSet.of(POST, GET, PUT, DELETE, HEAD);

    private Policy policy;

    public PublishedService() {
        setVersion(1);
        policy = new Policy(PolicyType.PRIVATE_SERVICE, null, null, false);
    }

    /**
     * Create a PublishedService that is a copy of the given PublishedService.
     *
     * <p>This will copy the identity of the original, if you don't want
     * this you will need to reset the id and version (and the id and version
     * of the policy).</p>
     * @param objToCopy source object.  required
     */
    public PublishedService( final PublishedService objToCopy ) {
        this(objToCopy, false);
    }

    /**
     * Create a PublishedService that is a copy of the given PublishedService.
     *
     * <p>This will copy the identity of the original, if you don't want
     * this you will need to reset the id and version (and the id and version
     * of the policy).</p>
     * @param objToCopy source object.  required
     * @param lock true to create a read-only service
     */
    public PublishedService( final PublishedService objToCopy, boolean lock) {
        super(objToCopy);
        setDisabled(objToCopy.isDisabled());
        setHttpMethods(objToCopy.getHttpMethodsReadOnly());
        setLaxResolution(objToCopy.isLaxResolution());
        setWssProcessingEnabled(objToCopy.isWssProcessingEnabled());
        setTracingEnabled(objToCopy.isTracingEnabled());
        setPolicy(objToCopy.getPolicy()==null ? null : new Policy(objToCopy.getPolicy()));
        setRoutingUri(objToCopy.getRoutingUri());
        setSoap(objToCopy.isSoap());
        setInternal(objToCopy.isInternal());
        _soapVersion = objToCopy.getSoapVersion();
        setFolder(objToCopy.getFolder());
        _wsdlUrl = objToCopy._wsdlUrl;
        setWsdlXml(objToCopy.getWsdlXml());
        setSecurityZone(objToCopy.getSecurityZone());
        if (lock) {
            PersistentEntityUtil.lock(policy);
            lock();
        }
    }

    @Override
    @Transient
    @Migration(mapName = NONE, mapValue = NONE, resolver = PropertyResolver.Type.SERVICE_DOCUMENT)
    public String getId() {
        return super.getId();
    }

    @Size(min = 1, max = 255)
    @Override
    public String getName() {
        return super.getName();
    }

    /**
     * Attempts to open a connection to the protected service
     *
     * @throws IOException   if the connection to the protected service cannot be established
     * @throws WSDLException if no valid URL can be found in the service's WSDL document.
     */
    public void ping() throws IOException, WSDLException {
        InputStream is = null;
        try {
            URL url = serviceUrl();
            is = url.openStream();
        } finally {
            if (is != null) is.close();
        }
    }

    /**
     * Gets the URL from which the WSDL was originally downloaded
     *
     * @return the URL from which the WSDL was originally downloaded.  Never null, but could be empty.
     */
    @Size(max=4096)
    public String getWsdlUrl() {
        if (_wsdlUrl == null) _wsdlUrl = ""; // to satisfy the db
        return _wsdlUrl;
    }

    /**
     * Sets the WSDL URL for this service
     *
     * @param wsdlUrl the WSDL URL for this service
     * @throws MalformedURLException if the URL cannot be parsed
     */
    public void setWsdlUrl(String wsdlUrl) throws MalformedURLException {
        checkLocked();
        if (_wsdlUrl != null && !_wsdlUrl.equals(wsdlUrl)) _wsdlXml = null;
        if (wsdlUrl != null && wsdlUrl.length() > 0) {
            new URL(wsdlUrl);
            _wsdlUrl = wsdlUrl;
        } else {
            _wsdlUrl = null;
        }
    }

    /**
     * Returns the contents of the WSDL document for this service.
     *
     * @return A String containing the WSDL document or  null if not set.
     */
    @NotNull(groups=SoapValidationGroup.class, message="WSDL is required for SOAP services")
    @Size(max=5242880)
    public String getWsdlXml() {
        return _wsdlXml;
    }

    /**
     * Sets the contents of the WSDL document for this service.
     *
     * @param wsdlXml the contents of the WSDL document for this service.
     */
    public synchronized void setWsdlXml(String wsdlXml) {
        checkLocked();
        _wsdlXml = wsdlXml;
        _parsedWsdl.set(null);
        _wsdlLocator.set(null);
    }

    private static SoapVersion guessSoapVersionFromWsdl(Wsdl wsdl) {
 	    SoapVersion ret = SoapVersion.UNKNOWN;
        if (wsdl == null)
            return ret;

        for(BindingOperation bindingOperation : wsdl.getBindingOperations()) {

            Iterator eels = bindingOperation.getExtensibilityElements().iterator();
            ExtensibilityElement ee;
            while ( eels.hasNext() ) {
                ee = (ExtensibilityElement)eels.next();
                if ( ee instanceof SOAPOperation) {
                    ret = SoapVersion.SOAP_1_1;
                    break;
                } else if( ee instanceof SOAP12Operation) {
                    ret = SoapVersion.SOAP_1_2;
                    break;
                }
            }
            if(ret != SoapVersion.UNKNOWN) {
                break;
            }
        }
        return ret;
    }

    public synchronized SoapVersion getSoapVersion() {
        if (_soapVersion == null)
 	 	    try {
 	 	        _soapVersion = guessSoapVersionFromWsdl(parsedWsdl());
 	 	    } catch (WSDLException e) {
 	 	        _soapVersion = SoapVersion.UNKNOWN;
 	 	    }
        return _soapVersion;
    }

    public void setSoapVersion(SoapVersion soapVersion) {
        checkLocked();
        _soapVersion = soapVersion;        
    }

    public boolean soapVersionSet() {
        return _soapVersion!=null;
    }

    /**
     * get base URI
     *
     * @return the base URI from the WSDL, or null
     */
    public String getBaseURI() {
         if (_wsdlUrl == null) return null;
         return Wsdl.extractBaseURI(_wsdlUrl);
    }

    /**
     * Gets the {@link Wsdl} object generated from this service's WSDL document or
     * <code>null</code> if wsdl xml document has not been set.
     *
     * @return the {@link Wsdl} object generated from this service's WSDL document.
     * @throws WSDLException if the WSDL cannot be parsed
     */
    public Wsdl parsedWsdl() throws WSDLException {
        return accessWsdlItem( _parsedWsdl, new TernaryThrows<Wsdl,WsdlStrategy,String,String,WSDLException>(){
            @Override
            public Wsdl call( final WsdlStrategy strategy, final String url, final String content ) throws WSDLException {
                return strategy.parseWsdl( PublishedService.this, url, content );
            }
        } );
    }

    /**
     * Gets a WSDLLocator that can be used to generate this service's WSDL
     *
     * @return The WSDLLocator or null
     * @throws WSDLException If an error occurs
     */
    public SerializableWSDLLocator wsdlLocator() throws WSDLException {
        return accessWsdlItem( _wsdlLocator, new TernaryThrows<SerializableWSDLLocator,WsdlStrategy,String,String,WSDLException>(){
            @Override
            public SerializableWSDLLocator call( final WsdlStrategy strategy, final String url, final String content ) throws WSDLException {
                return strategy.wsdlLocator( PublishedService.this, url, content );
            }
        } );
    }

    /**
     * Set the WsdlStrategy to use when parsing the wsdl for the service.
     *
     * @param strategy The new strategy (null for default)
     */
    public void parseWsdlStrategy(WsdlStrategy strategy) {
        wsdlStrategy = strategy;
    }

    /**
     * Get the WsdlStrategy to use when parsing the wsdl for the service.
     *
     * @return The strategy in use (null if default)
     */
    public WsdlStrategy parseWsdlStrategy() {
        return wsdlStrategy;
    }

    /**
     * Gets the SOAP {@link Port} from this service's WSDL.
     *
     * @return the {@link Port} for this service. May be null.
     * @throws WSDLException if the WSDL cannot be parsed
     */
    public Port wsdlPort() throws WSDLException {
        Port port = _wsdlPort.get();
        if (port != null)
            return port;

        Wsdl wsdl = parsedWsdl();
        if (wsdl == null)
            return null;

        synchronized (this) {
            port = _wsdlPort.get();
            if (port != null)
                return port;

            port = wsdl.getSoapPort();
            _wsdlPort.set(port);
        }

        return port;
    }

    /**
     * Get the URL of the protected service from this service's WSDL or configured default value.
     *
     * @return the protected service URL. May be null.
     * @throws WSDLException         if the WSDL could not be parsed
     * @throws MalformedURLException if the protected service URL could not be parsed
     */
    public synchronized URL serviceUrl() throws WSDLException, MalformedURLException {
        if ( defaultRoutingUrl != null && _serviceUrl == null ) {
            _serviceUrl = new URL(defaultRoutingUrl);
        }

        if (!isSoap()) return _serviceUrl;
        if (_serviceUrl == null) {
            Port wsdlPort = wsdlPort();
            if (wsdlPort == null) return null;
            String uri = parsedWsdl().getUriFromPort(wsdlPort);

            if (uri == null) {
                String err = "WSDL " + getWsdlUrl() + " did not contain a valid URL";
                logger.severe(err);
                throw new WSDLException( SoapUtil.FC_SERVER, err);
            }

            String baseURI = getWsdlUrl();
            if(baseURI != null && baseURI.toLowerCase().startsWith("http")) {
                if(uri.toLowerCase().startsWith("http")) {
                    _serviceUrl = new URL(uri);
                } else {
                    _serviceUrl = new URL(new URL(baseURI),uri);
                }
            } else {
                _serviceUrl = new URL(uri);
            }
        }
        return _serviceUrl;
    }

    @Override
    public String toString() {
        return _name;
    }

    @Valid
    @Migration(mapName = NONE, mapValue = NONE, resolver = PropertyResolver.Type.POLICY)
    public Policy getPolicy() {
        return policy;
    }

    public void setPolicy(Policy policy) {
        checkLocked();
        this.policy = policy;
        if ( policy != null ) {
            policy.setFolder( folder );
        }
    }

    public boolean isDisabled() {
        return _disabled;
    }

    public void setDisabled(boolean disabled) {
        checkLocked();
        _disabled = disabled;
    }

    @SuppressWarnings( { "RedundantIfStatement" } )
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof PublishedService)) return false;

        final PublishedService publishedService = (PublishedService)o;

        if (_wsdlUrl != null ? !_wsdlUrl.equals(publishedService._wsdlUrl) : publishedService._wsdlUrl != null) return false;
        if (!Goid.isDefault(getGoid()) ? !Goid.equals(getGoid(),publishedService.getGoid()) :!Goid.isDefault(publishedService.getGoid())) return false;
        if (securityZone != null ? !securityZone.equals(publishedService.securityZone) : publishedService.securityZone != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result;
        result = (_wsdlUrl != null ? _wsdlUrl.hashCode() : 0);
        result = 29 * result + (getGoid() != null ? getGoid().hashCode() : 0);
        result = 29 * result + (securityZone != null ? securityZone.hashCode() : 0);
        return result;
    }

    /**
     * Gets the flag indicating whether or not this service is a SOAP service.
     * If not, the service does not have a WSDL and can only be resolved through its routing URI property.
     *
     * @return true if the service is SOAP (i.e. has a WSDL), false otherwise.
     */
    public boolean isSoap() {
        return soap;
    }

    /**
     * Sets the flag indicating whether or not this service is a SOAP service.
     * If not, then service does not have a WSDL and can only be resolved through its routing URI property.
     *
     * @param isSoap true if the service is SOAP (i.e. has a WSDL), false otherwise.
     */
    public void setSoap(boolean isSoap) {
        checkLocked();
        this.soap = isSoap;
        if ( policy != null )
            policy.setSoap(isSoap);
    }

    /**
     * Gets the flag indicating whether or not this service is internal. An internal service is one that whose WSDL and
     * other details are already specified at the time of publishing. Modular assertions can add templates for internal
     * services which will then be made available for publishing via the "publish internal service" wizard.
     * @return true if this service is an internal service.
     */
    public boolean isInternal() {
        return internal;
    }

    /**
     * Sets the flag indicating whether or not this service is internal.
     * @param internal  flag value to set
     */
    public void setInternal(boolean internal) {
        checkLocked();
        this.internal = internal;
    }

    /**
     * Get the descriptive name for this service.
     *
     * <p>This will include sufficient information to identify a service whilst
     * still being brief enough for use in a UI.</p>
     *
     * @return The services display name.
     */
    public String displayName() {
        String displayName = getName();

        String routingUri = getRoutingUri();
        if (routingUri != null) {
            displayName += " [" + routingUri + "]";
        }

        return displayName;
    }

    /**
     * URI portion of the requests that determine whether or not requests are meant for this service.
     *
     * @return the HTTP URI (the part of a URL after the hostname) for this service.
     */
    @Size(max=128)
    @Pattern.List( {
            @Pattern(regexp="(?!/ssg).*", message="must not start with '/ssg'"),
            @Pattern(regexp="(?!.*?/service/\\d+$).*"),
            @Pattern(regexp="/.+")
    } )
    public String getRoutingUri() {
        return routingUri;
    }

    /**
     * URI portion of the requests that determine whether or not requests are meant for this service.
     *
     * @param routingUri the HTTP URI (the part of a URL after the hostname) for this service.
     */
    public void setRoutingUri(String routingUri) {
        checkLocked();
        this.routingUri = routingUri;
    }

    /**
     * Get the default routing URL for the service (If any).
     *
     * @return The default routing URL
     * @see #serviceUrl() <code>serviceUrl</code>, which should be used to get the URL with fallback to WSDL endpoint
     */
    @Size(max=4096)
    public String getDefaultRoutingUrl() {
        return defaultRoutingUrl;
    }

    public void setDefaultRoutingUrl( final String defaultRoutingUrl ) {
        checkLocked();
        this.defaultRoutingUrl = defaultRoutingUrl;
    }

    /**
     * Check if the specified HTTP method is allowed with this published service.
     *
     * @param method the method to check.  Must not be null.
     * @return true iff. the specified method is in the set of allowed methods for this published service.
     */
    public boolean isMethodAllowed(HttpMethod method) {
        return httpMethods.contains(method);
    }

    /**
     * Get a read-write copy of the set of method names supported by this published service.
     *
     * @return a read-only set of zero or more Strings such as "PUT", "GET", "DELETE" and "POST".  May be empty but never null.
     * @deprecated only meant for serialization (by Hibernate and JAXB)
     */
    @Deprecated
    public Set<HttpMethod>getHttpMethods() {
        return httpMethods;
    }

    /**
     * Get a read-only copy of the set of method names supported by this published service.
     *
     * @return a read-only set of zero or more Strings such as "PUT", "GET", "DELETE" and "POST".  May be empty but never null.
     */
    public Set<HttpMethod>getHttpMethodsReadOnly() {
        return Collections.unmodifiableSet(httpMethods);
    }

    /**
     * Replace the set of method names supported by this published service.
     * Never modify the set inside a live PublishedService instance being used by a running MessageProcessor.
     *
     * @param set a set of Strings such as "GET", "POST", "PUT".  Will be converted to all upper-case.
     */
    public void setHttpMethods(Set<HttpMethod> set) {
        checkLocked();
        httpMethods = set;
    }

    /**
     * Does this service allow / process multipart data.
     *
     * <p>Note that calling this method will cause the WSDL to be parsed.</p>
     *
     * <p>This value is a hint, and may be overridden if the services policy
     * permits multipart data.</p>
     *
     * @return true if multipart data is expected
     * @see #parsedWsdl
     * @throws com.l7tech.gateway.common.service.PublishedService.ServiceException if unable to parse WSDL to check multipart flag
     */
    public boolean isMultipart() throws ServiceException {
        if (multipart == null) {
            try {
                Wsdl wsdl = parsedWsdl();

                if (!isSoap()) {
                    multipart = Boolean.TRUE;
                } else if (wsdl != null && wsdl.hasMultipartOperations()) {
                    multipart = Boolean.TRUE;
                } else {
                    multipart = Boolean.FALSE;
                }
            } catch(WSDLException we) {
                throw new ServiceException("Cannot determine multipart flag, could not process WSDL.", we);
            }
        }

        return multipart;
    }

    public boolean isLaxResolution() {
        return laxResolution;
    }

    public void setLaxResolution(boolean laxResolution) {
        checkLocked();
        this.laxResolution = laxResolution;
    }

    public boolean isWssProcessingEnabled() {
        return wssProcessingEnabled;
    }

    public void setWssProcessingEnabled(boolean wssProcessingEnabled) {
        checkLocked();
        this.wssProcessingEnabled = wssProcessingEnabled;
    }

    public boolean isTracingEnabled() {
        return tracingEnabled;
    }

    public void setTracingEnabled(boolean tracingEnabled) {
        checkLocked();
        this.tracingEnabled = tracingEnabled;
    }

    @Override
    @Migration(mapName = NONE, mapValue = NONE, resolver = PropertyResolver.Type.ASSERTION)
    @Dependency(isDependency = false)
    public Folder getFolder() {
        return folder;
    }

    @Override
    public void setFolder(Folder folder) {
        checkLocked();
        this.folder = folder;
        if ( policy != null ) {
            policy.setFolder( folder );
        }
    }

    /**
     *
     */
    public static class ServiceException extends Exception {
        public ServiceException(String message, Throwable cause) {
            super(message, cause);
        }
    }

    /**
     *
     */
    public static interface WsdlStrategy {
        public Wsdl parseWsdl(PublishedService service, String uri, String wsdl) throws WSDLException;
        public SerializableWSDLLocator wsdlLocator(PublishedService service, String uri, String wsdl) throws WSDLException;
    }

    /**
     * Generate a suitable name for a Policy associated with this service.
     *
     * @return The generated unique name
     */
    public String generatePolicyName() {
        StringBuilder builder = new StringBuilder(300);

        builder.append( "Policy for service #" );
        builder.append( getId() );
        builder.append( ", " );
        builder.append( getName() );
        if (builder.length() > 255)
            builder.setLength( 255 );

        return builder.toString();
    }

    @Override
    public void flush() throws IOException {
        checkLocked();
        if ( policy != null ) {
            policy.flush();
        }
    }

    /**
     * Validation group with additional constraints for SOAP services.
     */
    public interface SoapValidationGroup {}

    // ************************************************
    // PRIVATES
    // ************************************************
    private String _wsdlUrl;
    private String _wsdlXml;
    private SoapVersion _soapVersion = null;
    private boolean _disabled;
    private boolean soap = true;
    private boolean internal = false;
    private String routingUri;
    private String defaultRoutingUrl;
    private Set<HttpMethod> httpMethods = EnumSet.copyOf(METHODS_SOAP);
    private boolean laxResolution;
    private boolean wssProcessingEnabled = true;
    private boolean tracingEnabled = false;
    private Folder folder;

    private transient WsdlStrategy wsdlStrategy;
    private final AtomicReference<Wsdl> _parsedWsdl = new AtomicReference<Wsdl>(null);
    private final AtomicReference<SerializableWSDLLocator> _wsdlLocator = new AtomicReference<SerializableWSDLLocator>(null);
    private final AtomicReference<Port> _wsdlPort = new AtomicReference<Port>(null);
    private transient URL _serviceUrl;

    private transient Boolean multipart;

    /**
     * Clear WSDL and Port before serializing, the Port can cause errors when
     * serialized (e.g. bug 9152), the WSDL is safe but redundant (and could be
     * large). 
     */
    private void writeObject( final ObjectOutputStream out ) throws IOException {
        _parsedWsdl.set( null );
        _wsdlLocator.set( null );
        _wsdlPort.set( null );
        out.defaultWriteObject();
    }

    private <WI> WI accessWsdlItem( final AtomicReference<WI> reference,
                                    final TernaryThrows<WI,WsdlStrategy,String,String,WSDLException> builder ) throws WSDLException {
        WI wsdlItem = reference.get();
        if (wsdlItem != null)
            return wsdlItem;

        String cachedWsdl = getWsdlXml();
        if (cachedWsdl == null)
            return null;

        synchronized (this) {
            wsdlItem = reference.get();
            if (wsdlItem != null)
                return wsdlItem;

            WsdlStrategy strategy = wsdlStrategy;
            if (strategy == null)
                strategy = new DefaultWsdlStrategy();
            wsdlItem = builder.call( strategy, getWsdlUrl(), cachedWsdl);
            reference.set(wsdlItem);
        }

        return wsdlItem;
    }

    /**
     * The default strategy resolves based on lookup of an resolver in the
     * local environment.
     */
    private static class DefaultWsdlStrategy implements WsdlStrategy {
        private final WsdlStrategy delegate;

        private DefaultWsdlStrategy() {
            WsdlStrategy strategy = null;
            final ServiceLoader<WsdlStrategy> load = ServiceLoader.load(WsdlStrategy.class);
            Iterator providerIter = load.iterator();
            if ( providerIter != null && providerIter.hasNext() ) {
                strategy = (WsdlStrategy) providerIter.next();
            }
            delegate = strategy;
        }

        @Override
        public Wsdl parseWsdl(PublishedService service, String uri, String wsdl) throws WSDLException {
            if ( delegate == null )
                throw new WSDLException( WSDLException.CONFIGURATION_ERROR, "Missing strategy to load WSDL for '"+uri+"'." );

            return delegate.parseWsdl( service, uri, wsdl );
        }

        @Override
        public SerializableWSDLLocator wsdlLocator(PublishedService service, String uri, String wsdl) throws WSDLException {
            if ( delegate == null )
                throw new WSDLException( WSDLException.CONFIGURATION_ERROR, "Missing strategy to load WSDL for '"+uri+"'." );

            return delegate.wsdlLocator( service, uri, wsdl );
        }
    }
}
