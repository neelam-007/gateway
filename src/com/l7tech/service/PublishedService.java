/*
 * Copyright (C) 2003 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.service;

import com.l7tech.common.util.SoapFaultUtils;
import com.l7tech.common.xml.Wsdl;
import com.l7tech.objectmodel.imp.NamedEntityImp;
import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.assertion.FalseAssertion;
import com.l7tech.policy.wsp.WspReader;
import org.xml.sax.InputSource;

import javax.wsdl.Port;
import javax.wsdl.WSDLException;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.logging.Logger;

/**
 * A service that is published by the SecureSpan Gateway.  Primarily contains references to a WSDL and a policy.
 *
 * @author alex
 * @version $Revision$
 */
public class PublishedService extends NamedEntityImp {
    public static final String ROUTINGURI_PREFIX = "/xml/";

    /**
     * Please do not change this logger type. Instead, provide the
     * {@link java.util.logging.Handler} implementation - as governed by
     * the JDK 1.4 logging API - if you wish to provide custom behaviour.
     */
    static final Logger logger = Logger.getLogger(PublishedService.class.getName());

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
        } else {
            if (_rootAssertion == null)
                _rootAssertion = WspReader.parse(policyXml);
        }

        return _rootAssertion;
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
                _parsedWsdl = Wsdl.newInstance(getBaseURI(), new InputSource(new StringReader(cachedWsdl)));
            }
        }
        return _parsedWsdl;
    }

    /**
     * Gets the SOAP {@link Port} from this service's WSDL.
     *
     * @return the {@link Port} for this service. May be null.
     * @throws WSDLException if the WSDL cannot be parsed
     */
    public synchronized Port wsdlPort() throws WSDLException {
        if (_wsdlPort == null) {
            Port soapPort = parsedWsdl().getSoapPort();
            _wsdlPort = soapPort;
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
        return "com.l7tech.service.PublishedService policyXml=" + _policyXml + " _wsdlUrl=" + _wsdlUrl + " _wsdlXml=" + _wsdlXml;
    }

    /**
     * allows to set all properties from another object
     * the version is not copied and should be copied manually when needed
     *
     * @param objToCopy
     */
    public void copyFrom(PublishedService objToCopy) throws MalformedURLException, IOException {
        setName(objToCopy.getName());
        setPolicyXml(objToCopy.getPolicyXml());
        setWsdlUrl(objToCopy.getWsdlUrl());
        setWsdlXml(objToCopy.getWsdlXml());
        setDisabled(objToCopy.isDisabled());
        setRoutingUri(objToCopy.getRoutingUri());
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

    // ************************************************
    // PRIVATES
    // ************************************************
    private String _policyXml;
    private String _wsdlUrl;
    private String _wsdlXml;
    private boolean _disabled;
    private boolean soap = true;
    private String routingUri;

    private transient Wsdl _parsedWsdl;
    private transient Port _wsdlPort;
    private transient URL _serviceUrl;
    private transient Assertion _rootAssertion;
}
