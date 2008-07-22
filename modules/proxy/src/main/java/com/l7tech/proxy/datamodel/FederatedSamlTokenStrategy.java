package com.l7tech.proxy.datamodel;

import java.security.cert.CertificateEncodingException;
import java.security.cert.X509Certificate;
import java.security.cert.CertificateException;
import javax.net.ssl.SSLException;

import com.l7tech.security.token.SecurityTokenType;
import com.l7tech.proxy.datamodel.exceptions.OperationCanceledException;
import com.l7tech.proxy.ssl.SslPeer;

/**
 * Extends AbstractSamlTokenStrategy with remote functionality.
 *
 * @author $Author$
 * @version $Revision$
 */
public abstract class FederatedSamlTokenStrategy extends AbstractSamlTokenStrategy {

    /**
     *
     */
    public FederatedSamlTokenStrategy(SecurityTokenType tokenType, Object lock) {
        super(tokenType, lock);
    }

    /**
     *
     */
    public Object clone() throws CloneNotSupportedException {
        return super.clone();
    }

    /**
     *
     */
    public abstract X509Certificate getTokenServerCert() throws CertificateException;

    /**
     *
     */
    public abstract void storeTokenServerCert(X509Certificate tokenServerCert) throws CertificateEncodingException;

    /**
     *
     */
    public abstract void handleSslException(SslPeer sslPeer, Exception e) throws SSLException, OperationCanceledException, CertificateEncodingException;
}
