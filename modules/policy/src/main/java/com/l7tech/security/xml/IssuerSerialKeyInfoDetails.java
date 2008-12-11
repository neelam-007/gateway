/**
 * Copyright (C) 2008 Layer 7 Technologies Inc.
 */
package com.l7tech.security.xml;

import com.l7tech.util.DomUtils;
import com.l7tech.util.NamespaceFactory;
import com.l7tech.xml.soap.SoapUtil;
import org.w3c.dom.Element;

import java.security.cert.X509Certificate;

/**
 * Populates a ds:KeyInfo with <code>[wsse:SecurityTokenReference/]ds:X509Data/ ...</code>
 * <pre>
 * &lt;ds:X509IssuerSerial&gt;
 *   &lt;ds:X509IssuerName&gt;CN=...&lt;/ds:X509IssuerName&gt;
 *   &lt;ds:X509SerialNumber&gt;1234&lt;/ds:X509SerialNumber&gt;
 * &lt;/ds:X509IssuerSerial&gt;
 * </pre>
 * @author alex
 */
public class IssuerSerialKeyInfoDetails extends KeyInfoDetails {
    private final X509Certificate cert;
    private final boolean includeStr;

    public IssuerSerialKeyInfoDetails(X509Certificate certificate, boolean includeStr) {
        this.cert = certificate;
        this.includeStr = includeStr;
    }

    @Override
    public Element populateExistingKeyInfoElement(NamespaceFactory nsf, Element keyInfo) {
        final Element x509DataParent;
        if (includeStr) {
            x509DataParent = createStr(nsf, keyInfo);
        } else {
            x509DataParent = keyInfo;
        }
        final Element x509DataElement = DomUtils.createAndAppendElementNS(x509DataParent, "X509Data", SoapUtil.DIGSIG_URI, "ds");
        final Element issuerSerialElement = DomUtils.createAndAppendElementNS(x509DataElement, "X509IssuerSerial", SoapUtil.DIGSIG_URI, "ds");
        final Element issuerElement = DomUtils.createAndAppendElementNS(issuerSerialElement, "X509IssuerName", SoapUtil.DIGSIG_URI, "ds");
        issuerElement.setTextContent(cert.getIssuerDN().getName());
        final Element serialElement = DomUtils.createAndAppendElementNS(issuerSerialElement, "X509SerialNumber", SoapUtil.DIGSIG_URI, "ds");
        serialElement.setTextContent(cert.getSerialNumber().toString());
        return keyInfo;
    }
}
