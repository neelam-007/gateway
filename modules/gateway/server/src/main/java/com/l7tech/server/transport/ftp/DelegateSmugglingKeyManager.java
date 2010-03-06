package com.l7tech.server.transport.ftp;

import com.l7tech.server.transport.tls.SsgConnectorSslHelper;

import javax.net.ssl.KeyManager;

/**
* A fake KeyManager that can be used to smuggle a delegate SSLContext into our proxy SSLContextSpi via the SSLContext init method.
*/
class DelegateSmugglingKeyManager implements KeyManager {
    final SsgConnectorSslHelper delegate;

    public DelegateSmugglingKeyManager(SsgConnectorSslHelper delegate) {
        this.delegate = delegate;
    }
}
