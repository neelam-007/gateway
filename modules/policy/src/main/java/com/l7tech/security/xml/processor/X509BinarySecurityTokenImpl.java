package com.l7tech.security.xml.processor;

import com.l7tech.security.token.SecurityTokenType;
import com.l7tech.security.token.X509SecurityToken;
import com.l7tech.xml.soap.SoapUtil;
import org.w3c.dom.Element;

import java.security.cert.X509Certificate;

/**
 *
 */
class X509BinarySecurityTokenImpl extends X509SigningSecurityTokenImpl implements X509SecurityToken {
    private final X509Certificate finalcert;

    public X509BinarySecurityTokenImpl(X509Certificate finalcert, Element binarySecurityTokenElement) {
        super(binarySecurityTokenElement);
        this.finalcert = finalcert;
    }

    public SecurityTokenType getType() {
        return SecurityTokenType.WSS_X509_BST;
    }

    public String getElementId() {
        return SoapUtil.getElementWsuId(asElement());
    }

    public X509Certificate getMessageSigningCertificate() {
        return finalcert;
    }

    public X509Certificate getCertificate() {
        return finalcert;
    }

    public String toString() {
        return "X509SecurityToken: " + finalcert.toString();
    }
}
