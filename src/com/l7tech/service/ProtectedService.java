/*
 * Copyright (C) 2003 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.service;

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

    public String getWsdl() {
        return wsdl;
    }

    public void setWsdl(String wsdl) {
        this.wsdl = wsdl;
    }

    public String getOriginalWSDLLocation() {
        return originalWSDLLocation;
    }

    public void setOriginalWSDLLocation(String originalWSDLLocation) {
        this.originalWSDLLocation = originalWSDLLocation;
    }

    // ************************************************
    // PRIVATES
    // ************************************************
    private long oid;
    private String wsdl;
    private String originalWSDLLocation;
}
