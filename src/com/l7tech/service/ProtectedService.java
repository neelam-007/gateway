/*
 * Copyright (C) 2003 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.service;

import org.xml.sax.InputSource;

import javax.wsdl.WSDLException;
import java.net.URL;
import java.net.MalformedURLException;
import java.io.StringReader;

/**
 * A reference to an existing web service.
 * 
 * @author alex
 */
public class ProtectedService {
    public long getOid() {
        return oid;
    }

    public void setOid(long oid) {
        this.oid = oid;
    }

    public Wsdl parsedWsdl() throws WSDLException {
        if ( parsedWsdl == null ) {
            try {
                String cachedWsdl = getWsdl();
                parsedWsdl = Wsdl.newInstance( null, new InputSource( new StringReader(cachedWsdl) ) );
            } catch ( MalformedURLException mue ) {
                throw new WSDLException( mue.getMessage(), mue.toString(), mue );
            }
        }
        return parsedWsdl;
    }

    public String getWsdl() throws MalformedURLException {
        return wsdl;
    }

    public void setWsdl( String wsdl ) {
        this.wsdl = wsdl;
    }

    public String getWsdlUrl() {
        return wsdlUrl;
    }

    public void setWsdlUrl( String wsdlUrl ) throws MalformedURLException {
        this.url = new URL( wsdlUrl );
        this.wsdlUrl = wsdlUrl;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    // ************************************************
    // PRIVATES
    // ************************************************
    private String name;
    private long oid;
    private String wsdlUrl;
    private String wsdl;

    private transient URL url;
    private transient Wsdl parsedWsdl;
}
