package com.l7tech.common.http;

import org.apache.commons.httpclient.protocol.SecureProtocolSocketFactory;
import org.apache.commons.httpclient.params.HttpConnectionParams;
import org.apache.commons.httpclient.ConnectTimeoutException;

import javax.net.ssl.*;
import java.net.Socket;
import java.net.UnknownHostException;
import java.net.InetAddress;
import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.security.KeyManagementException;

/**
 * Ever-trusting SSL socket factory for testing
 */
public class PermissiveSSLSocketFactory extends SSLSocketFactory implements SecureProtocolSocketFactory {
    private final SSLSocketFactory defaultSslSocketFactory = (SSLSocketFactory)SSLSocketFactory.getDefault();
    private final SSLContext sslContext;

    public PermissiveSSLSocketFactory() {
        try {
            sslContext = SSLContext.getInstance("SSL");
            X509TrustManager trustManager = new PermissiveX509TrustManager();
            sslContext.init(null,
                            new X509TrustManager[] {trustManager},
                            null);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        } catch (KeyManagementException e) {
            throw new RuntimeException(e);
        }
    }

    public String[] getDefaultCipherSuites() {
        return defaultSslSocketFactory.getDefaultCipherSuites();
    }

    public String[] getSupportedCipherSuites() {
        return defaultSslSocketFactory.getSupportedCipherSuites();
    }

    private SSLSocketFactory socketFactory() {
        return sslContext.getSocketFactory();
    }

    public Socket createSocket(Socket socket, String host, int port, boolean autoClose)
            throws IOException, UnknownHostException
    {
        return (SSLSocket) socketFactory().createSocket(socket, host, port, autoClose);
    }

    public Socket createSocket(String host, int port, InetAddress clientHost, int clientPort)
            throws IOException, UnknownHostException
    {
        return (SSLSocket) socketFactory().createSocket(host, port, clientHost, clientPort);
    }

    public Socket createSocket(InetAddress inetAddress, int i, InetAddress inetAddress1, int i1) throws IOException
    {
        return (SSLSocket) socketFactory().createSocket(inetAddress, i, inetAddress1, i1);
    }

    public Socket createSocket(String host, int port) throws IOException, UnknownHostException {
        return (SSLSocket) socketFactory().createSocket(host, port);
    }

    public Socket createSocket(InetAddress inetAddress, int i) throws IOException {
        return (SSLSocket) socketFactory().createSocket(inetAddress, i);
    }

    public Socket createSocket(String host, int port, InetAddress clientHost, int clientPort, HttpConnectionParams httpConnectionParams) throws IOException, UnknownHostException, ConnectTimeoutException {
        return createSocket(host, port, clientHost, clientPort);
    }

    public Socket createSocket() throws IOException {
        return (SSLSocket) socketFactory().createSocket();
    }
}
