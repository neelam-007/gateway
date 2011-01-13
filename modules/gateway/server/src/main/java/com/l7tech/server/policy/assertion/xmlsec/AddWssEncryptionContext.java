package com.l7tech.server.policy.assertion.xmlsec;

import com.l7tech.policy.assertion.xmlsec.XmlSecurityRecipientContext;
import com.l7tech.security.token.SigningSecurityToken;
import com.l7tech.security.token.X509SigningSecurityToken;

import java.security.cert.X509Certificate;

/**
*
*/
public class AddWssEncryptionContext {
    final X509Certificate clientCert;
    final SigningSecurityToken signingSecurityToken;
    final String keyEncryptionAlgorithm;
    final XmlSecurityRecipientContext recipientContext;

    /**
     * Create a new AddWssEncryptionContext
     *
     * @param clientCert client cert to encrypt to, or null to use alternate means
     * @param signingSecurityToken  The token to encrypt to, or null if not available
     * @param keyEncryptionAlgorithm The key encryption algorithm to use in the response (if X.509 cert)
     * @param recipientContext the intended recipient for the Security header to create
     */
    AddWssEncryptionContext( final X509Certificate clientCert,
                               final SigningSecurityToken signingSecurityToken,
                               final String keyEncryptionAlgorithm,
                               final XmlSecurityRecipientContext recipientContext ) {
        this.clientCert = clientCert;
        this.signingSecurityToken = signingSecurityToken;
        this.keyEncryptionAlgorithm = keyEncryptionAlgorithm;
        this.recipientContext = recipientContext;
    }

    public boolean isCertificate() {
        return clientCert != null || signingSecurityToken instanceof X509SigningSecurityToken;
    }

    public X509Certificate getCertificate() {
        return clientCert != null ?
                clientCert :
                signingSecurityToken instanceof X509SigningSecurityToken ?
                        ((X509SigningSecurityToken)signingSecurityToken).getMessageSigningCertificate() :
                        null;
    }

    public boolean hasEncryptionKey() {
        return clientCert != null || signingSecurityToken != null;
    }

    public XmlSecurityRecipientContext getRecipientContext() {
        return recipientContext;
    }


}
