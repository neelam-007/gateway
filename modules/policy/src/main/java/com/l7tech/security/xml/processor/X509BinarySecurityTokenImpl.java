package com.l7tech.security.xml.processor;

import com.l7tech.security.token.SecurityTokenType;
import com.l7tech.security.token.X509SecurityToken;
import com.l7tech.util.DomUtils;
import com.l7tech.util.HexUtils;
import com.l7tech.util.SoapConstants;
import com.l7tech.xml.soap.SoapUtil;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import java.security.cert.CertificateEncodingException;
import java.security.cert.X509Certificate;

/**
 * X509SecurityToken that represents any X.509 signing token.
 *
 * <p>Note that this is used for BinarySecurityTokens, SubjectKeyIdentifier
 * references and Issuer Name / Serial Number references.</p>
 *
 * <p>A "virtual" BST can be created for use with an STR-Transform using the
 * provided static method.</p>
 */
public class X509BinarySecurityTokenImpl extends X509SigningSecurityTokenImpl implements X509SecurityToken {
    private final X509Certificate finalcert;
    private final SecurityTokenType securityTokenType;

    public X509BinarySecurityTokenImpl( final X509Certificate finalcert,
                                        final Element binarySecurityTokenElement ) {
        this( finalcert, binarySecurityTokenElement, SecurityTokenType.WSS_X509_BST );
    }

    public X509BinarySecurityTokenImpl( final X509Certificate finalcert,
                                        final Element binarySecurityTokenElement,
                                        final SecurityTokenType securityTokenType ) {
        super( binarySecurityTokenElement );
        if ( securityTokenType != SecurityTokenType.WSS_X509_BST &&
             securityTokenType != SecurityTokenType.X509_ISSUER_SERIAL ) throw new IllegalArgumentException("Token type must be BST or Issuer/Serial");
        this.finalcert = finalcert;
        this.securityTokenType = securityTokenType;
    }

    @Override
    public SecurityTokenType getType() {
        return securityTokenType;
    }

    @Override
    public String getElementId() {
        return SoapUtil.getElementWsuId(asElement());
    }

    @Override
    public X509Certificate getMessageSigningCertificate() {
        return finalcert;
    }

    @Override
    public X509Certificate getCertificate() {
        return finalcert;
    }

    @Override
    public String toString() {
        return "X509SecurityToken: " + finalcert.toString();
    }

    /**
     * Generate a X509BinarySecurityTokenImpl for the given info.
     *
     * <p>The <code>asElement</code> method will return a DOM suitable for use
     * with the STR-Transform.</p>
     *
     * @param domFactory The Document that will own the BST fragment.
     * @param certificate The certificate for the BST
     * @param wssePrefix The namespace prefix for the WS-Security namespace
     * @param wssePrefix The namespace URI for the WS-Security namespace
     * @return A new X509SigningSecurityTokenImpl
     */
    public static X509SigningSecurityTokenImpl createBinarySecurityToken( final Document domFactory,
                                                                          final X509Certificate certificate,
                                                                          final String wssePrefix,
                                                                          final String wsseNs) throws CertificateEncodingException {
        return createBinarySecurityToken( domFactory,
                                          certificate,
                                          wssePrefix,
                                          wsseNs,
                                          SecurityTokenType.WSS_X509_BST );
    }

    /**
     * Generate a X509BinarySecurityTokenImpl for the given info.
     *
     * <p>The <code>asElement</code> method will return a DOM suitable for use
     * with the STR-Transform.</p>
     *
     * @param domFactory The Document that will own the BST fragment.
     * @param certificate The certificate for the BST
     * @param wssePrefix The namespace prefix for the WS-Security namespace
     * @param wssePrefix The namespace URI for the WS-Security namespace
     * @return A new X509SigningSecurityTokenImpl 
     */
    public static X509SigningSecurityTokenImpl createBinarySecurityToken( final Document domFactory,
                                                                          final X509Certificate certificate,
                                                                          final String wssePrefix,
                                                                          final String wsseNs,
                                                                          final SecurityTokenType securityTokenType ) throws CertificateEncodingException {
        X509SigningSecurityTokenImpl signingCertToken;
        final Element bst;
        if (wssePrefix == null) {
            bst = domFactory.createElementNS(wsseNs, "BinarySecurityToken");
            bst.setAttributeNS(DomUtils.XMLNS_NS, "xmlns", wsseNs);
        } else {
            bst = domFactory.createElementNS(wsseNs, wssePrefix+":BinarySecurityToken");
            bst.setAttributeNS(DomUtils.XMLNS_NS, "xmlns:"+wssePrefix, wsseNs);
        }
        bst.setAttribute("ValueType", SoapConstants.VALUETYPE_X509);
        DomUtils.setTextContent(bst, HexUtils.encodeBase64(certificate.getEncoded(), true));

        signingCertToken = new X509BinarySecurityTokenImpl(certificate, bst, securityTokenType);
        return signingCertToken;
    }
}
