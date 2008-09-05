/*
 * Copyright (C) 2003 Layer 7 Technologies Inc.
 *
 */

package com.l7tech.gateway.common.service;

import com.l7tech.policy.Policy;
import com.l7tech.policy.PolicyType;
import com.l7tech.xml.soap.SoapUtil;
import com.l7tech.wsdl.Wsdl;
import com.l7tech.objectmodel.imp.NamedEntityImp;
import com.l7tech.objectmodel.Aliasable;
import com.l7tech.objectmodel.folder.HasFolder;
import org.xml.sax.InputSource;

import javax.wsdl.Port;
import javax.wsdl.WSDLException;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlTransient;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.*;
import java.util.logging.Logger;
import java.util.regex.Pattern;

/**
 * A service that is published by the SecureSpan Gateway.  Primarily contains references to a WSDL and a policy.
 * Must be treated as immutable and thread-safe from the point of view of the SSG's message processing subsystem.
 *
 * @author alex
 */
@SuppressWarnings( { "NonJaxWsWebServices" } )
@XmlRootElement
public class PublishedService extends NamedEntityImp implements HasFolder, Aliasable
{
    //private static final long serialVersionUID = 8711916262379377867L;
    private static final Logger logger = Logger.getLogger(PublishedService.class.getName());

    public static final String METHODNAMES_SOAP = "POST";
    public static final String METHODNAMES_REST = "POST,GET,PUT,DELETE,HEAD";

    private Policy policy;

    /** Used to split up the method name lists. */
    private static final Pattern SPLIT_COMMAS = Pattern.compile("\\s*,\\s*");

    public PublishedService() {
        setVersion(1);
        policy = new Policy(PolicyType.PRIVATE_SERVICE, null, null, false);
        isAlias = false;
    }

    /**
     * Create a PublishedService that is a copy of the given PublishedService.
     *
     * <p>This will copy the identity of the orginal, if you don't want
     * this you will need to reset the id and version (and the id and version
     * of the policy).</p>
     */
    public PublishedService( final PublishedService objToCopy ) {
        super(objToCopy);
        setDisabled(objToCopy.isDisabled());
        setHttpMethods(objToCopy.getHttpMethods());
        setLaxResolution(objToCopy.isLaxResolution());
        setPolicy(objToCopy.getPolicy()==null ? null : new Policy(objToCopy.getPolicy()));
        setRoutingUri(objToCopy.getRoutingUri());
        setSoap(objToCopy.isSoap());
        _wsdlUrl = objToCopy._wsdlUrl;
        setWsdlXml(objToCopy.getWsdlXml());
        setAlias(objToCopy.isAlias());
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
    public String getWsdlXml() {
        return _wsdlXml;
    }

    /**
     * Sets the contents of the WSDL document for this service.
     *
     * @param wsdlXml the contents of the WSDL document for this service.
     */
    public synchronized void setWsdlXml(String wsdlXml) {
        _wsdlXml = wsdlXml;
        _parsedWsdl = null;
    }

    /**
     * get base URI
     *
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
     * @throws WSDLException
     */
    public synchronized Wsdl parsedWsdl() throws WSDLException {
        if (_parsedWsdl == null) {
            String cachedWsdl = getWsdlXml();
            if (cachedWsdl != null) {
                WsdlStrategy strategy = wsdlStrategy;
                if (strategy == null)
                    strategy = new DefaultWsdlStrategy();
                _parsedWsdl = strategy.parseWsdl(getBaseURI(), cachedWsdl);
            }
        }
        return _parsedWsdl;
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
    public synchronized Port wsdlPort() throws WSDLException {
        if (_wsdlPort == null) {
            _wsdlPort = parsedWsdl().getSoapPort();
        }

        return _wsdlPort;
    }

    /**
     * Gets the URL of the protected service from this service's WSDL.
     *
     * @return the protected service URL. May be null.
     * @throws WSDLException         if the WSDL could not be parsed
     * @throws MalformedURLException if the protected service URL could not be parsed
     */
    public synchronized URL serviceUrl() throws WSDLException, MalformedURLException {
        if (!isSoap()) return null;
        if (_serviceUrl == null) {
            Port wsdlPort = wsdlPort();
            if (wsdlPort == null) return null;
            String uri = parsedWsdl().getUriFromPort(wsdlPort);

            if (uri == null) {
                String err = "WSDL " + getWsdlUrl() + " did not contain a valid URL";
                logger.severe(err);
                throw new WSDLException( SoapUtil.FC_SERVER, err);
            }

            String baseURI = Wsdl.extractBaseURI(getWsdlUrl());
            if(baseURI != null && baseURI.startsWith("http") || uri.startsWith("HTTP")) {
                if(uri.startsWith("http") || uri.startsWith("HTTP")) {
                    _serviceUrl = new URL(uri);
                } else {
                    _serviceUrl = new URL( baseURI + uri);
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

    public Policy getPolicy() {
        return policy;
    }

    public void setPolicy(Policy policy) {
        this.policy = policy;
    }

    public boolean isDisabled() {
        return _disabled;
    }

    public void setDisabled(boolean disabled) {
        _disabled = disabled;
    }

    @SuppressWarnings( { "RedundantIfStatement" } )
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof PublishedService)) return false;

        final PublishedService publishedService = (PublishedService)o;

        if (_wsdlUrl != null ? !_wsdlUrl.equals(publishedService._wsdlUrl) : publishedService._wsdlUrl != null) return false;
        if (_oid != DEFAULT_OID ? _oid != publishedService._oid : publishedService._oid != DEFAULT_OID) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result;
        result = (_wsdlUrl != null ? _wsdlUrl.hashCode() : 0);
        result = 29 * result + (int)_oid;
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
     * @param internal
     */
    public void setInternal(boolean internal) {
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
    public String getRoutingUri() {
        return routingUri;
    }

    /**
     * URI portion of the requests that determine whether or not requests are meant for this service.
     *
     * @param routingUri the HTTP URI (the part of a URL after the hostname) for this service.
     */
    public void setRoutingUri(String routingUri) {
        this.routingUri = routingUri;
    }

    /**
     * Accessor for Hibernate use only.  Returns the string name.
     *
     * @deprecated for persistence use only; do not call this.
     * @return comma-separated list of HTTP method names like "GET,POST,PUT,DELETE", or null.
     */
    @Deprecated
    public String getHttpMethodNames() {
        return httpMethodNames;
    }

    /**
     * Setter for Hibernate use only.
     *
     * @deprecated for persistence use only; do not call this.
     * @param httpMethodNames comma-separated list of HTTP method names like "GET,POST,PUT,DELETE", or null.
     */
    @Deprecated
    public void setHttpMethodNames(String httpMethodNames) {
        if (httpMethodNames == null) httpMethodNames = METHODNAMES_SOAP;
        this.httpMethodNames = httpMethodNames;
        this.httpMethods = createSetFromMethods(httpMethodNames);
    }

    /**
     * Check if the specified HTTP method is allowed with this published service.
     *
     * @param methodName the method name to check.  Must not be null.
     * @return true iff. the specified method is in the set of allowed methods for this published service.
     */
    public boolean isMethodAllowed(String methodName) {
        if (httpMethodNames == null) httpMethodNames = METHODNAMES_SOAP;
        if (httpMethods == null) httpMethods = createSetFromMethods(httpMethodNames);
        return httpMethods.contains(methodName.trim().toUpperCase());
    }

    /**
     * Get a read-only copy of the set of method names supported by this published service.
     *
     * @return a read-only set of zero or more Strings such as "PUT", "GET", "DELETE" and "POST".  May be empty but never null.
     */
    @XmlTransient
    public Set<String> getHttpMethods() {
        if (httpMethodNames == null) httpMethodNames = METHODNAMES_SOAP;
        if (httpMethods == null) httpMethods = createSetFromMethods(httpMethodNames);
        return Collections.unmodifiableSet(httpMethods);
    }

    /**
     * Replace the set of method names supported by this published service.
     * Never modify the set inside a live PublishedService instance being used by a running MessageProcessor.
     *
     * @param set a set of Strings such as "GET", "POST", "PUT".  Will be converted to all upper-case.
     */
    public void setHttpMethods(Set<String> set) {
        httpMethods = new HashSet<String>();
        if (set != null) {
            for (String s : set) {
                httpMethods.add(s.trim().toUpperCase());
            }
        }
        httpMethodNames = getMethodsFromSet(httpMethods);
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
        this.laxResolution = laxResolution;
    }

    public Long getFolderOid() {
        return folderOid;
    }

    public void setFolderOid(Long policyFolderOid) {
        this.folderOid = policyFolderOid;
    }

    public void setAlias(boolean isAlias) {
        this.isAlias = isAlias;
    }

    public boolean isAlias() {
        return isAlias;
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
        public Wsdl parseWsdl(String uri, String wsdl) throws WSDLException;
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

    // ************************************************
    // PRIVATES
    // ************************************************
    private String _wsdlUrl;
    private String _wsdlXml;
    private boolean _disabled;
    private boolean soap = true;
    private boolean internal = false;
    private String routingUri;
    private String httpMethodNames = METHODNAMES_SOAP; // invariants: never null, always in sync with httpMethods
    private boolean laxResolution;
    private Long folderOid = -5002L;

    private transient WsdlStrategy wsdlStrategy;
    private transient Wsdl _parsedWsdl;
    private transient Port _wsdlPort;
    private transient URL _serviceUrl;

    private transient Set<String> httpMethods; // invariants: never null, always in sync with httpMethodNames
    private transient Boolean multipart;

    private boolean isAlias;
    
    /**
     * Create a new set from the specified comma-delimited list of HTTP method names.
     *
     * @param methods  the methods to include in the set, ie "GET,PUT,POST".  Must not be null but may be empty.
     * @return a Set containing the uppercased method names.  May be empty but never null.
     */
    private static Set<String> createSetFromMethods(String methods) {
        Set<String> s = new HashSet<String>();
        addMethodsToSet(s, methods);
        return s;
    }

    /**
     * Add the specified methods to the specified set.
     *
     * @param set     the set to add the strings to.  Must not be null but may (probably often will) be empty.
     * @param methods a comma-separated list of HTTP method names.  Will be converted to upper case.  May
     *                not be null or empty.
     */
    private static void addMethodsToSet(Set<String> set, String methods) {
        set.addAll(Arrays.asList(SPLIT_COMMAS.split(methods.trim().toUpperCase())));
    }

    /**
     * Convert the specified set into a comma-delimited string.
     *
     * @param set the set of Strings to join.  Must not be null.  Must contain only Strings.
     * @return a comma-separated list, ie "POST,GET,PUT".  Order is not guaranteed.  Never null but may be empty.
     */
    private static String getMethodsFromSet(Set set) {
        StringBuffer sb = new StringBuffer();
        for (Iterator i = set.iterator(); i.hasNext();) {
            String s = (String)i.next();
            sb.append(s);
            if (i.hasNext()) sb.append(",");
        }
        return sb.toString();
    }

    /**
     * 
     */
    private static class DefaultWsdlStrategy implements WsdlStrategy {
        public Wsdl parseWsdl(String uri, String wsdl) throws WSDLException {
            InputSource source = new InputSource();
            source.setSystemId(uri);
            source.setCharacterStream(new StringReader(wsdl));
            return Wsdl.newInstance(uri, source);
        }
    }
}
