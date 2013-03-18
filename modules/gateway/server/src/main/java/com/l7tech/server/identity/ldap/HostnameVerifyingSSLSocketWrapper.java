package com.l7tech.server.identity.ldap;

import com.l7tech.common.io.SSLSocketWrapper;
import com.l7tech.util.InetAddressUtil;
import com.l7tech.util.ResourceUtils;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLSocket;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;

/**
 * Ldap-specific SSLSocketWrapper which performs hostname verification on connect.
 */

// TODO: Integrate the functionality of this class (HostnameVerifyingSSLSocketWrapper) into LdapSslClientSocketFactorySupport::doVerifyHostname
//      -  Add a switch to LdapSslClientSocketFactorySupport so that it will behave as
//         it does (verifies on connect) or as this does (verifies on on startHandshake).

public class HostnameVerifyingSSLSocketWrapper extends SSLSocketWrapper {
    private SSLSocket socket;
    private HostnameVerifier hostnameVerifier;

    public HostnameVerifyingSSLSocketWrapper(SSLSocket wrapped,HostnameVerifier hostnameVerifier) {
        super(wrapped);
        this.socket = wrapped;
        this.hostnameVerifier = hostnameVerifier;
    }

    @Override
    public void startHandshake() throws IOException {
        super.startHandshake();
        verifyHost();
    }

    private void verifyHost() throws IOException {
        final SocketAddress inetEndpoint = socket.getRemoteSocketAddress();

        if ( inetEndpoint instanceof InetSocketAddress ) {
            final String host = InetAddressUtil.getHost((InetSocketAddress) inetEndpoint);
            if ( !hostnameVerifier.verify(host, socket.getSession()) ) {
                ResourceUtils.closeQuietly(socket);
                throw new IOException("Host name does not match certificate '" + host + "'.");
            }
        }
    }
}
