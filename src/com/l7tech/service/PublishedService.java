/*
 * Copyright (C) 2003 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.service;

import com.l7tech.common.util.SoapUtil;
import com.l7tech.common.util.Locator;
import com.l7tech.common.xml.Wsdl;
import com.l7tech.message.Request;
import com.l7tech.objectmodel.imp.NamedEntityImp;
import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.assertion.FalseAssertion;
import com.l7tech.policy.wsp.WspReader;
import org.xml.sax.InputSource;

import javax.wsdl.Port;
import javax.wsdl.WSDLException;
import java.io.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.logging.Logger;

/**
 * @author alex
 * @version $Revision$
 */
public class PublishedService extends NamedEntityImp {
    /**
     * Please do not change this logger type. Instead, provide the
     * {@link java.util.logging.Handler} implementation - as governed by
     * the JDK 1.4 logging API - if you wish to provide custom behaviour.
     */
    static final Logger logger = Logger.getLogger(PublishedService.class.getName());

    public PublishedService() {
        setVersion(1);
    }

    public synchronized Assertion rootAssertion() throws IOException {
        String policyXml = getPolicyXml();
        if ( policyXml == null || policyXml.length() == 0 ) {
            logger.warning( "Service " + _oid + " has an invalid or empty policy_xml field.  Using null policy." );
            return FalseAssertion.getInstance();
        } else {
            if ( _rootAssertion == null )
                _rootAssertion = WspReader.parse( policyXml );
        }

        return _rootAssertion;
    }

    public void ping() throws IOException, WSDLException {
        InputStream is = null;
        try {
            URL url = serviceUrl( null );
            is = url.openStream();
        } finally {
            if ( is != null ) is.close();
        }
    }

    public String getWsdlUrl() {
        return _wsdlUrl;
    }

    public synchronized void setWsdlUrl( String wsdlUrl ) throws MalformedURLException {
        if ( _wsdlUrl != null && !_wsdlUrl.equals(wsdlUrl) ) _wsdlXml = null;
        _wsdlUrl = wsdlUrl;
        new URL( wsdlUrl );
    }

    /**
     * Loads the WSDL document from the _wsdlUrl if necessary.
     * @return A String containing the WSDL document.
     * @throws IOException
     */
    public synchronized String getWsdlXml() throws IOException {
        if ( _wsdlXml == null ) {
            // we must get the actual wsdl. delegate to the ServiceAdmin
            // this avoids resolving the wsdl on the client side
            ServiceManager serviceManagerInstance = (ServiceManager)Locator.getDefault().
                                        lookup(com.l7tech.service.ServiceManager.class);
            _wsdlXml = serviceManagerInstance.resolveWsdlTarget(_wsdlUrl);
        }
        return _wsdlXml;
    }

    public synchronized void setWsdlXml( String wsdlXml ) {
        _wsdlXml = wsdlXml;
        _parsedWsdl = null;
    }

    public synchronized Wsdl parsedWsdl() throws WSDLException {
        if ( _parsedWsdl == null ) {
            try {
                String cachedWsdl = getWsdlXml();
                _parsedWsdl = Wsdl.newInstance( null, new InputSource( new StringReader(cachedWsdl) ) );
            } catch ( IOException ioe ) {
                throw new WSDLException( ioe.getMessage(), ioe.toString(), ioe );
            }
        }
        return _parsedWsdl;
    }

    public synchronized Port wsdlPort( Request request ) throws WSDLException {
        // TODO: Get the right Port for this request, rather than just the first one!

        if ( _wsdlPort == null ) {
            Port soapPort = parsedWsdl().getSoapPort();
            _wsdlPort = soapPort;
        }

        return _wsdlPort;
    }

    public synchronized URL serviceUrl( Request request ) throws WSDLException, MalformedURLException {
        if ( _serviceUrl == null ) {
            Port wsdlPort = wsdlPort( request );
            URL url = parsedWsdl().getUrlFromPort( wsdlPort );

            if ( url == null ) {
                String err = "WSDL " + getWsdlUrl() + " did not contain a valid URL";
                logger.severe( err );
                throw new WSDLException( SoapUtil.FC_SERVER, err );
            }

            _serviceUrl = url;
        }
        return _serviceUrl;
    }


    public String getPolicyXml() {
        return _policyXml;
    }

    public void setPolicyXml( String policyXml ) {
        _policyXml = policyXml;
        // Invalidate stale Root Assertion
        _rootAssertion = null;
    }

    public String toString() {
        return "com.l7tech.service.PublishedService _policyXml=" + _policyXml + " _wsdlUrl=" + _wsdlUrl + " _wsdlXml=" + _wsdlXml;
    }

    /**
     * allows to set all properties from another object
     * @param objToCopy
     */
    public void copyFrom(PublishedService objToCopy) throws MalformedURLException, IOException {
        setName(objToCopy.getName());
        setPolicyXml(objToCopy.getPolicyXml());
        setWsdlUrl(objToCopy.getWsdlUrl());
        setWsdlXml(objToCopy.getWsdlXml());
        setVersion(objToCopy.getVersion());
        setDisabled(objToCopy.isDisabled());
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

        final PublishedService publishedService = (PublishedService) o;

        if (_wsdlUrl != null ? !_wsdlUrl.equals(publishedService._wsdlUrl) : publishedService._wsdlUrl != null) return false;
        if (_oid != DEFAULT_OID ? _oid == publishedService._oid : publishedService._oid != DEFAULT_OID ) return false;

        return true;
    }

    public int hashCode() {
        int result;
        result = (_wsdlUrl != null ? _wsdlUrl.hashCode() : 0);
        result = 29 * result + (int)_oid;
        return result;
    }

    // ************************************************
    // PRIVATES
    // ************************************************
    protected String _policyXml;
    protected String _wsdlUrl;
    protected String _wsdlXml;
    protected boolean _disabled;

    protected transient Wsdl _parsedWsdl;
    protected transient Port _wsdlPort;
    protected transient URL _serviceUrl;
    protected transient Assertion _rootAssertion;
}
