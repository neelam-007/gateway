/*
 * Copyright (C) 2003 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.service;

import com.l7tech.logging.LogManager;
import com.l7tech.message.Request;
import com.l7tech.objectmodel.imp.NamedEntityImp;
import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.assertion.FalseAssertion;
import com.l7tech.policy.wsp.WspReader;
import com.l7tech.util.SoapUtil;
import com.l7tech.server.policy.assertion.ServerAssertion;
import com.l7tech.server.policy.assertion.ServerFalseAssertion;
import com.l7tech.server.policy.ServerPolicyFactory;
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
    public synchronized ServerAssertion rootAssertion() throws IOException {
        // TODO: Transform the abstract policy tree into the Server version!
        String policyXml = getPolicyXml();
        if ( policyXml == null || policyXml.length() == 0 ) {
            _log.warning( "Service " + _oid + " has an invalid or empty policy_xml field.  Using null policy." );
            return new ServerFalseAssertion( FalseAssertion.getInstance() );
        } else {
            if ( _rootAssertion == null ) {
                Assertion ass = WspReader.parse( policyXml );
                _rootAssertion = ServerPolicyFactory.getInstance().makeServerPolicy( ass );
            }
        }

        return _rootAssertion;
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
            URL url = null;
            try {
                url = new URL(_wsdlUrl);
            } catch ( MalformedURLException mue ) {
                throw new IOException(mue.toString());
            }

            Reader r = null;
            try {
                r = new BufferedReader( new InputStreamReader( url.openStream() ) );
                StringBuffer xml = new StringBuffer();
                char[] buf = new char[4096];
                int num;
                while ( ( num = r.read( buf ) ) != -1 ) {
                    xml.append( buf, 0, num );
                }
                _wsdlXml = xml.toString();
            } finally {
                if ( r != null ) r.close();
            }
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
                _log.severe( err );
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

    protected transient Logger _log = LogManager.getInstance().getSystemLogger();
    protected transient Wsdl _parsedWsdl;
    protected transient Port _wsdlPort;
    protected transient URL _serviceUrl;
    protected transient ServerAssertion _rootAssertion;
}
