package com.l7tech.security.xml.processor;

import com.l7tech.security.token.SecurityTokenType;
import com.l7tech.security.token.X509SecurityToken;
import com.l7tech.xml.soap.SoapUtil;
import com.l7tech.util.SoapConstants;
import com.l7tech.util.DomUtils;
import com.l7tech.util.HexUtils;
import org.w3c.dom.Element;
import org.w3c.dom.Document;

import java.security.cert.X509Certificate;
import java.security.cert.CertificateEncodingException;

/**
 *
 */
public class X509BinarySecurityTokenImpl extends X509SigningSecurityTokenImpl implements X509SecurityToken {
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

    public static X509SigningSecurityTokenImpl createBinarySecurityToken(Document domFactory, X509Certificate signingCert, String wssePrefix, String wsseNs) throws CertificateEncodingException {
        X509SigningSecurityTokenImpl signingCertToken;
        final Element bst;
        if (wssePrefix == null) {
            bst = domFactory.createElementNS(wsseNs, "BinarySecurityToken");
            bst.setAttribute("xmlns", wsseNs);
        } else {
            bst = domFactory.createElementNS(wsseNs, wssePrefix+":BinarySecurityToken");
            bst.setAttribute("xmlns:"+wssePrefix, wsseNs);
        }
        bst.setAttribute("ValueType", SoapConstants.VALUETYPE_X509);
        DomUtils.setTextContent(bst, HexUtils.encodeBase64(signingCert.getEncoded(), true));

        signingCertToken = new X509BinarySecurityTokenImpl(signingCert, bst);
        return signingCertToken;
    }
}
