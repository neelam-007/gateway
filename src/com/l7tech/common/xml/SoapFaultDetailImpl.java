/*
 * Copyright (C) 2004 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.common.xml;

import org.w3c.dom.Element;

/**
 * Mutable bean implementation for SoapFaultDetail.
 */
public class SoapFaultDetailImpl implements SoapFaultDetail {
    private String faultcode;
    private String faultstring;
    private Element faultdetail;
    private String faultactor;

    /**
     * Create a SoapFaultDetail copied from the specified SoapFaultDetail.
     *
     * @param soapFaultDetail the source to copy from.  May not be null.
     */
    public SoapFaultDetailImpl(SoapFaultDetail soapFaultDetail) {
        this(soapFaultDetail.getFaultCode(),
             soapFaultDetail.getFaultString(),
             soapFaultDetail.getFaultDetail(),
             soapFaultDetail.getFaultActor());
    }

    /**
     * Create a SoapFaultDetail with the specified faultcode, which may not be null or empty.
     *
     * @param faultcode the new faultcode.  May not be null or empty.
     */
    public SoapFaultDetailImpl(String faultcode) {
        this(faultcode, null, null, null);
    }

    /**
     * Create a SoapFaultDetail with the specified faultcode, faultstring, and faultdetail.
     *
     * @param faultcode the new faultcode.  May not be null or empty.
     * @param faultstring the new faultstring.  May be null, in which case the empty string will be used.
     * @param faultdetail the new faultdetail.  May be null.
     */
    public SoapFaultDetailImpl(String faultcode, String faultstring, Element faultdetail) {
        this(faultcode, faultstring, faultdetail, null);
    }

    /**
     *
     * @param faultcode the new faultcode.  May not be null or empty.
     * @param faultstring the new faultstring.  May be null, in which case the empty string will be used.
     * @param faultdetail the new faultdetail.  May be null.
     * @param faultactor the faultactor, or null if not yet known.
     */
    public SoapFaultDetailImpl(String faultcode, String faultstring, Element faultdetail, String faultactor) {
        setFaultCode(faultcode);
        setFaultString(faultstring);
        setFaultDetail(faultdetail);
        setFaultActor(faultactor);
    }

    public String getFaultCode() {
        return faultcode;
    }

    public String getFaultString() {
        return faultstring;
    }

    public Element getFaultDetail() {
        return faultdetail;
    }

    public String getFaultActor() {
        return faultactor;
    }

    public String getFaultActor(String defaultActor) {
        if (faultactor == null) {
            if (defaultActor == null || defaultActor.length() < 1)
                throw new IllegalArgumentException("defaultActor may not be null or empty");
            faultactor = defaultActor;
        }
        return faultactor;
    }

    /**
     * Set the fault code.  Every soap fault must have a non-empty fault code.
     *
     * @param faultcode the new faultcode.  May not be null or empty.
     */
    public void setFaultCode(String faultcode) {
        if (faultcode == null || faultcode.length() < 1)
            throw new IllegalArgumentException("faultcode may not be null or empty");
        this.faultcode = faultcode;
    }

    /**
     * Set the fault string.
     *
     * @param faultstring the new faultstring.  May be null, in which case the empty string will be used.
     */
    public void setFaultString(String faultstring) {
        this.faultstring = faultstring == null ? "" : faultstring;
    }

    /**
     * Set the fault detail.
     *
     * @param faultdetail the new faultdetail.  May be null.
     */
    public void setFaultDetail(Element faultdetail) {
        this.faultdetail = faultdetail;
    }

    /**
     * Set the fault actor.
     *
     * @param faultactor the faultactor, or null if not yet known.
     */
    public void setFaultActor(String faultactor) {
        this.faultactor = faultactor;
    }
}
