/*
 * Copyright (C) 2003 Layer 7 Technologies Inc.
 *
 */

package com.l7tech.service;

import com.l7tech.common.util.SoapFaultUtils;
import com.l7tech.common.xml.Wsdl;
import com.l7tech.objectmodel.imp.NamedEntityImp;
import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.assertion.FalseAssertion;
import com.l7tech.policy.assertion.MimeMultipartAssertion;
import com.l7tech.policy.wsp.WspReader;
import org.xml.sax.InputSource;

import javax.wsdl.Port;
import javax.wsdl.WSDLException;
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
public class PublishedService extends NamedEntityImp {
    //private static final long serialVersionUID = 8711916262379377867L;
    private static final Logger logger = Logger.getLogger(PublishedService.class.getName());

    public static final String METHODNAMES_SOAP = "POST";
    public static final String METHODNAMES_REST = "POST,GET,PUT,DELETE,HEAD";

    /** Used to split up the method name lists. */
    private static final Pattern SPLIT_COMMAS = Pattern.compile("\\s*,\\s*");

    public PublishedService() {
        setVersion(1);
    }

    /**
     * Parses the policy and returns the {@link Assertion} at its root.
     *
     * @return the {@link Assertion} at the root of the policy. May be null.
     * @throws IOException if the policy cannot be deserialized
     */
    public synchronized Assertion rootAssertion() throws IOException {
        String policyXml = getPolicyXml();
        if (policyXml == null || policyXml.length() == 0) {
            logger.warning("Service " + _oid + " has an invalid or empty policy_xml field.  Using null policy.");
            return FalseAssertion.getInstance();
        }

        if (_rootAssertion == null)
            _rootAssertion = WspReader.getDefault().parsePermissively(policyXml);

        return _rootAssertion;
    }

    public synchronized void forcePolicyRecompile() {
        _rootAssertion = null;
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
                throw new WSDLException(SoapFaultUtils.FC_SERVER, err);
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

    /**
     * Gets the XML serialized policy for this service.
     *
     * @return the XML serialized policy for this service.
     */
    public String getPolicyXml() {
        return _policyXml;
    }

    /**
     * Sets the XML serialized policy for this service.
     *
     * @param policyXml the XML serialized policy for this service.
     */
    public void setPolicyXml(String policyXml) {
        _policyXml = policyXml;
        // Invalidate stale Root Assertion
        _rootAssertion = null;
    }

    public String toString() {
        return _name;
    }

    /**
     * allows to set all properties from another object
     * the version is not copied and should be copied manually when needed
     *
     * @param objToCopy
     */
    public void copyFrom(PublishedService objToCopy) throws IOException {
        setName(objToCopy.getName());
        setPolicyXml(objToCopy.getPolicyXml());
        setWsdlUrl(objToCopy.getWsdlUrl());
        setWsdlXml(objToCopy.getWsdlXml());
        setDisabled(objToCopy.isDisabled());
        setRoutingUri(objToCopy.getRoutingUri());
        setHttpMethods(objToCopy.getHttpMethods());
    }

    public boolean isDisabled() {
        return _disabled;
    }

    public void setDisabled(boolean disabled) {
        _disabled = disabled;
    }

    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof PublishedService)) return false;

        final PublishedService publishedService = (PublishedService)o;

        if (_wsdlUrl != null ? !_wsdlUrl.equals(publishedService._wsdlUrl) : publishedService._wsdlUrl != null) return false;
        if (_oid != DEFAULT_OID ? _oid == publishedService._oid : publishedService._oid != DEFAULT_OID) return false;

        return true;
    }

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
    public String getHttpMethodNames() {
        return httpMethodNames;
    }

    /**
     * Setter for Hibernate use only.
     *
     * @deprecated for persistence use only; do not call this.
     * @param httpMethodNames comma-separated list of HTTP method names like "GET,POST,PUT,DELETE", or null.
     */
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
     * <p>Note that calling this method will cause the WSDL and Policy objects to
     * be constructed.</p>
     *
     * @return true if multipart data is allowed
     * @see #rootAssertion
     * @see #parsedWsdl
     */
    public boolean isMultipart() throws ServiceException {
        if (multipart == null) {
            try {
                Wsdl wsdl = parsedWsdl();
                Assertion assertion = rootAssertion();

                if (!isSoap()) {
                    multipart = Boolean.TRUE;
                } else if (wsdl != null && wsdl.hasMultipartOperations()) {
                    multipart = Boolean.TRUE;
                } else if (assertion != null && Assertion.contains(assertion, MimeMultipartAssertion.class)) {
                    multipart = Boolean.TRUE;
                } else {
                    multipart = Boolean.FALSE;
                }
            } catch(WSDLException we) {
                throw new ServiceException("Cannot determine multipart flag, could not process WSDL.", we);
            } catch(IOException ioe) {
                throw new ServiceException("Cannot determine multipart flag, could not process Policy.", ioe);
            }
        }

        return multipart.booleanValue();
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

    // ************************************************
    // PRIVATES
    // ************************************************

    private String _policyXml;
    private String _wsdlUrl;
    private String _wsdlXml;
    private boolean _disabled;
    private boolean soap = true;
    private String routingUri;
    private String httpMethodNames = METHODNAMES_SOAP; // invariants: never null, always in sync with httpMethods

    private transient WsdlStrategy wsdlStrategy;
    private transient Wsdl _parsedWsdl;
    private transient Port _wsdlPort;
    private transient URL _serviceUrl;
    private transient Assertion _rootAssertion;
    private transient Set<String> httpMethods; // invariants: never null, always in sync with httpMethodNames
    private transient Boolean multipart;

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
            return Wsdl.newInstance(uri, new InputSource(new StringReader(wsdl)));
        }
    }
}
