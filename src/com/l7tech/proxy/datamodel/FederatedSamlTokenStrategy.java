package com.l7tech.proxy.datamodel;

import java.security.cert.CertificateEncodingException;
import javax.net.ssl.SSLException;

import com.l7tech.common.security.token.SecurityTokenType;
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
    public abstract void handleSslException(SslPeer sslPeer, Exception e) throws SSLException, OperationCanceledException, CertificateEncodingException;
}
