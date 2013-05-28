package com.l7tech.common.io;

import com.l7tech.common.TestDocuments;

import javax.net.ssl.*;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.security.KeyStore;

public class TestSSLSocketFactory extends SSLSocketFactory {

    private final SSLSocketFactory sslSocketFactory;
    private final SSLContext sslContext;

    public TestSSLSocketFactory() {
        try {


            sslContext = SSLContext.getInstance("TLS");
            char[] password = "7layer]".toCharArray();
            KeyStore keyStore = TestDocuments.getMockSSLServerKeyStore();

            // setup the key manager factory
            KeyManagerFactory kmf = KeyManagerFactory.getInstance("SunX509");
            kmf.init(keyStore, password);

            // setup the trust manager factory
            TrustManagerFactory tmf = TrustManagerFactory.getInstance("SunX509");
            tmf.init(keyStore);

            sslContext.init(kmf.getKeyManagers(), tmf.getTrustManagers(), null);
            sslSocketFactory = sslContext.getSocketFactory();

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public String[] getDefaultCipherSuites() {
        return sslSocketFactory.getDefaultCipherSuites();
    }

    @Override
    public String[] getSupportedCipherSuites() {
        return sslSocketFactory.getSupportedCipherSuites();
    }

    @Override
    public Socket createSocket(Socket s, String host, int port, boolean autoClose) throws IOException {
        return sslSocketFactory.createSocket(s, host, port, autoClose);
    }

    @Override
    public Socket createSocket(String host, int port) throws IOException, UnknownHostException {
        return sslSocketFactory.createSocket(host, port);
    }

    @Override
    public Socket createSocket(String host, int port, InetAddress localHost, int localPort) throws IOException, UnknownHostException {
        return sslSocketFactory.createSocket(host, port, localHost, localPort);
    }

    @Override
    public Socket createSocket(InetAddress host, int port) throws IOException {
        return sslSocketFactory.createSocket(host, port);
    }

    @Override
    public Socket createSocket(InetAddress address, int port, InetAddress localAddress, int localPort) throws IOException {
        return sslSocketFactory.createSocket(address, port, localAddress, localPort);
    }
}
