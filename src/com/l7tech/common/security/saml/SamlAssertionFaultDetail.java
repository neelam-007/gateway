/*
 * Copyright (C) 2004 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.common.security.saml;

import com.l7tech.common.protocol.SecureSpanConstants;
import com.l7tech.common.util.XmlUtil;
import com.l7tech.common.xml.SoapFaultDetail;
import org.w3c.dom.Element;

/**
 * FaultDetail used to report problems with a SAML assertion.
 */
public class SamlAssertionFaultDetail implements SoapFaultDetail {
    private final String assertionId;
    private final String faultCode;
    private final String faultString;
    private final String reason;
    private final Element faultDetail;
    private String faultActor;

    /** Create a SamlAssertionFaultDetail with a default faultstring and the faultcode of InvalidSecurityToken. */
    public SamlAssertionFaultDetail(String assertionId) {
        this(assertionId, "The SAML ticket with AssertionID " + assertionId + " was not valid.");
    }

    /** Create a SamlAssertionFaultDetail with the specified faultstring and the faultcode of InvalidSecurityToken. */
    public SamlAssertionFaultDetail(String assertionId, String faultString) {
        this(assertionId, SecureSpanConstants.FAULTCODE_INVALIDSECURITYTOKEN, faultString);
    }

    public SamlAssertionFaultDetail(String assertionId, String faultString, String faultCode) {
        this(assertionId, faultCode, faultString, null);
    }

    /** Create a SamlAssertionFaultDetail with the specified faultstring, faultcode, and reason. */
    public SamlAssertionFaultDetail(String assertionId, String faultString, String faultCode, String reasonCode) {
        this.faultString = faultString;
        this.assertionId = assertionId;
        this.faultCode = faultCode;
        this.reason = reasonCode;
        final String pf = "l7saml";
        final String ns = SecureSpanConstants.FAULTDETAIL_SAML_NS;
        Element detail = XmlUtil.createEmptyDocument(SecureSpanConstants.FAULTDETAIL_SAML, pf, ns).getDocumentElement();
        Element badAss = XmlUtil.createAndAppendElementNS(detail, SecureSpanConstants.FAULTDETAIL_SAML_ASSERTIONID, ns, pf);
        badAss.appendChild(XmlUtil.createTextNode(badAss, assertionId));
        if (reasonCode != null) {
            Element reason = XmlUtil.createAndAppendElementNS(detail, SecureSpanConstants.FAULTDETAIL_SAML_REASON,  ns, pf);
            reason.appendChild(XmlUtil.createTextNode(reason, reasonCode));
        }
        faultDetail = detail;
    }

    /** @return The AssertionID that was problematic. */
    public String getAssertionId() {
        return assertionId;
    }

    /** @return the Reason code for this saml fault, or null if there isn't one. */
    public String getReason() {
        return reason;
    }

    public String getFaultCode() {
        return faultCode;
    }

    public String getFaultString() {
        return faultString;
    }

    public Element getFaultDetail() {
        return faultDetail;
    }

    public String getFaultActor() {
        return faultActor;
    }

    public String getFaultActor(String defaultActor) {
        return faultActor != null ? faultActor : defaultActor;
    }

    public void setFaultActor(String faultActor) {
        this.faultActor = faultActor;
    }
}
