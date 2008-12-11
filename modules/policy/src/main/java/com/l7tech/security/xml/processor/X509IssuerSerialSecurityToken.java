/**
 * Copyright (C) 2008 Layer 7 Technologies Inc.
 */
package com.l7tech.security.xml.processor;

import com.l7tech.security.token.SecurityTokenType;
import com.l7tech.xml.soap.SoapUtil;
import org.w3c.dom.Element;

import java.security.cert.X509Certificate;

/**
 * A security token representing an //Signature/KeyInfo/SecurityTokenReference/X509Data/X509IssuerSerial.
 *
 * @author alex
 */
public class X509IssuerSerialSecurityToken extends X509SigningSecurityTokenImpl {
    private final X509Certificate cert;
    private final String id;

    X509IssuerSerialSecurityToken(Element x509dataElement, X509Certificate cert) {
        super(x509dataElement);
        this.cert = cert;
        this.id = SoapUtil.getElementWsuId(x509dataElement);
    }

    @Override
    public String getElementId() {
        return id;
    }

    @Override
    public SecurityTokenType getType() {
        return SecurityTokenType.X509_ISSUER_SERIAL;
    }

    @Override
    public X509Certificate getMessageSigningCertificate() {
        return cert;
    }
}
