package com.l7tech.server.transport.ftp;

import com.l7tech.server.transport.tls.SsgConnectorSslHelper;
import org.apache.ftpserver.ssl.ClientAuth;
import org.apache.ftpserver.ssl.SslConfiguration;
import org.jetbrains.annotations.NotNull;

import javax.net.ssl.KeyManager;
import javax.net.ssl.SSLContext;
import java.security.GeneralSecurityException;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.Provider;
import java.util.ArrayList;
import java.util.HashMap;

/**
 * SslConfiguration implementation for the SSG FTP Server.
 *
 * @author Steve Jones
 * @author Jamie Williams - jamie.williams2@ca.com
 */
public class FtpSsl implements SslConfiguration {
    private final SsgConnectorSslHelper sslHelper;
    private final KeyManager sslHelperSmuggler;
    private final ClientAuth clientAuth;

    private static Provider proxySslContextProvider = new Provider("FtpSslWrapper", 1.0, "Provides SSLContext wrapper that can be used to preconfigure the socket factory used by FtpServer") {{
        putService(new Service(this, "SSLContext", "L7Wrapped", FtpSslContextWrapper.class.getName(), new ArrayList<String>(), new HashMap<String, String>()));
    }};

    public FtpSsl(@NotNull SsgConnectorSslHelper sslHelper, ClientAuth clientAuth) {
        this.sslHelper = sslHelper;
        this.clientAuth = clientAuth;

        sslHelperSmuggler = new DelegateSmugglingKeyManager(sslHelper);
    }

    @Override
    public SSLContext getSSLContext() throws GeneralSecurityException {
        return getProxySSLContext();
    }

    @Override
    public SSLContext getSSLContext(String protocol) throws GeneralSecurityException {
        return getProxySSLContext();
    }

    @Override
    public String[] getEnabledCipherSuites() {
        return sslHelper.getEnabledCiphers();
    }

    @Override
    public ClientAuth getClientAuth() {
        return clientAuth;
    }

    private SSLContext getProxySSLContext() throws NoSuchAlgorithmException, KeyManagementException {
        SSLContext proxy = SSLContext.getInstance("L7Wrapped", proxySslContextProvider);
        proxy.init(new KeyManager[] { sslHelperSmuggler }, null, null);
        return proxy;
    }
}
