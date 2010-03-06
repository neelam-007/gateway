package com.l7tech.server.transport.ftp;

import com.l7tech.server.transport.tls.SsgConnectorSslHelper;

import javax.net.ssl.*;
import java.security.KeyManagementException;
import java.security.SecureRandom;

/**
*
*/
public class FtpSslContextWrapper extends SSLContextSpi {
    SsgConnectorSslHelper sslHelper;
    SSLContext delegate;

    @Override
    protected void engineInit(KeyManager[] keyManagers, TrustManager[] trustManagers, SecureRandom secureRandom) throws KeyManagementException {
        if (keyManagers == null || keyManagers.length < 1 || !(keyManagers[0] instanceof DelegateSmugglingKeyManager))
            throw new KeyManagementException("Must pass a single DelegateSmugglingKeyManager");
        this.sslHelper = ((DelegateSmugglingKeyManager)keyManagers[0]).delegate;
        this.delegate = sslHelper.getSslContext();
    }

    @Override
    protected SSLSocketFactory engineGetSocketFactory() {
        check();
        return sslHelper.getSocketFactory();
    }

    @Override
    protected SSLServerSocketFactory engineGetServerSocketFactory() {
        check();
        return sslHelper.getSslServerSocketFactory();
    }

    @Override
    protected SSLEngine engineCreateSSLEngine() {
        check();
        return sslHelper.createSSLEngine(null, 0);
    }

    @Override
    protected SSLEngine engineCreateSSLEngine(String peerName, int peerPort) {
        check();
        return sslHelper.createSSLEngine(peerName, peerPort);
    }

    @Override
    protected SSLSessionContext engineGetServerSessionContext() {
        check();
        return delegate.getServerSessionContext();
    }

    @Override
    protected SSLSessionContext engineGetClientSessionContext() {
        check();
        return delegate.getClientSessionContext();
    }

    @Override
    protected SSLParameters engineGetDefaultSSLParameters() {
        check();
        return delegate.getDefaultSSLParameters();
    }

    @Override
    protected SSLParameters engineGetSupportedSSLParameters() {
        check();
        return delegate.getSupportedSSLParameters();
    }

    private void check() {
        if (delegate == null)
            throw new IllegalStateException("No delegate set -- init with DelegateSmugglingKeyManager");
    }
}
