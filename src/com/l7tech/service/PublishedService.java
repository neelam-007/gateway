/*
 * Copyright (C) 2003 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.service;

import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.assertion.TrueAssertion;
import com.l7tech.policy.wsp.WspReader;
import com.l7tech.objectmodel.imp.NamedEntityImp;
import com.l7tech.message.Request;
import com.l7tech.util.SoapUtil;

import javax.wsdl.*;
import javax.wsdl.extensions.ExtensibilityElement;
import javax.wsdl.extensions.soap.SOAPAddress;
import java.io.*;
import java.net.URL;
import java.net.MalformedURLException;
import java.util.*;

import org.xml.sax.InputSource;
import org.apache.log4j.Category;

/**
 * @author alex
 * @version $Revision$
 */
public class PublishedService extends NamedEntityImp {
    public synchronized Assertion rootAssertion() throws IOException {
        String policyXml = getPolicyXml();
        if ( policyXml == null ) {
            return new TrueAssertion();
        } else {
            if ( _rootAssertion == null ) _rootAssertion = WspReader.parse( policyXml );
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
            Iterator services = parsedWsdl().getServices().iterator();
            Service wsdlService = null;
            Port wsdlPort = null;

            int numServices = 0;
            while ( services.hasNext() ) {
                int numPorts = 0;
                numServices++;
                if ( wsdlService != null ) continue;

                wsdlService = (Service)services.next();
                Map ports = wsdlService.getPorts();
                if ( ports == null ) continue;

                Iterator portKeys = ports.keySet().iterator();
                String portKey;
                if ( portKeys.hasNext() ) {
                    numPorts++;
                    if ( wsdlPort == null ) {
                        portKey = (String)portKeys.next();
                        wsdlPort = (Port)ports.get(portKey);
                    }
                }
                if ( numPorts > 1 ) _log.warn( "WSDL " + getWsdlUrl() + " has more than one port, used the first." );
            }
            if ( numServices > 1 ) _log.warn( "WSDL " + getWsdlUrl() + " has more than one service, used the first." );
            _wsdlPort = wsdlPort;
        }

        return _wsdlPort;
    }

    public synchronized URL serviceUrl( Request request ) throws WSDLException, MalformedURLException {
        if ( _serviceUrl == null ) {
            Port wsdlPort = wsdlPort( request );
            List elements = wsdlPort.getExtensibilityElements();
            URL url = null;
            ExtensibilityElement eel;
            int num = 0;
            for ( int i = 0; i < elements.size(); i++ ) {
                eel = (ExtensibilityElement)elements.get(i);
                if ( eel instanceof SOAPAddress ) {
                    SOAPAddress sadd = (SOAPAddress)eel;
                    num++;
                    url = new URL( sadd.getLocationURI() );
                }
            }

            if ( url == null ) {
                String err = "WSDL " + getWsdlUrl() + " did not contain a valid URL";
                _log.error( err );
                throw new WSDLException( SoapUtil.FC_SERVER, err );
            }

            _serviceUrl = url;

            if ( num > 1 ) _log.warn( "WSDL " + getWsdlUrl() + " contained multiple <soap:address> elements" );
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

    public String getSoapAction() {
        return _soapAction;
    }

    public void setSoapAction(String soapAction) {
        _soapAction = soapAction;
    }

    public String getUrn() {
        return _urn;
    }

    public void setUrn(String urn) {
        _urn = urn;
    }

    public String toString() {
        return "com.l7tech.service.PublishedService _policyXml=" + _policyXml + " _wsdlUrl=" + _wsdlUrl + " _wsdlXml=" + _wsdlXml;
    }

    // ************************************************
    // PRIVATES
    // ************************************************
    protected String _policyXml;
    protected String _wsdlUrl;
    protected String _wsdlXml;
    protected String _soapAction;
    protected String _urn;

    protected transient Category _log = Category.getInstance( getClass() );
    protected transient Wsdl _parsedWsdl;
    protected transient Port _wsdlPort;
    protected transient URL _serviceUrl;
    protected transient Assertion _rootAssertion;
}
