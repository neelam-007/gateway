package com.l7tech.external.assertions.extensiblesocketconnectorassertion.server;

import java.net.InetSocketAddress;
import java.security.cert.X509Certificate;

/**
 * Created by IntelliJ IDEA.
 * User: njordan
 * Date: 07/12/11
 * Time: 2:57 PM
 * To change this template use File | Settings | File Templates.
 */
public class SslSocketTcpKnob extends SocketTcpKnob {
    private X509Certificate clientCert;

    public SslSocketTcpKnob(InetSocketAddress localAddress, InetSocketAddress remoteAddress, X509Certificate clientCert) {
        super(localAddress, remoteAddress);
        this.clientCert = clientCert;
    }

    public X509Certificate getClientCert() {
        return clientCert;
    }
}
