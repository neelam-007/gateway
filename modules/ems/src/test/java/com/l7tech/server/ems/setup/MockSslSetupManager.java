package com.l7tech.server.ems.setup;

import java.security.PrivateKey;
import java.security.cert.X509Certificate;

/**
 * 
 */
public class MockSslSetupManager implements SslSetupManager {
    @Override
    public String saveSsl( PrivateKey key, X509Certificate[] certificateChain) throws SetupException {
        return "SSL";
    }

    @Override
    public String generateSsl(String hostname, RsaKeySize rsaKeySize) throws SetupException {
        return "SSL";
    }

    @Override
    public void setSslAlias(String alias) throws SetupException {
    }

}
