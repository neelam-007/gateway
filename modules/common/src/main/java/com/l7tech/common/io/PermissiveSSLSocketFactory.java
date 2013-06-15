package com.l7tech.common.io;

import org.apache.http.conn.ConnectTimeoutException;
import org.apache.http.conn.scheme.SchemeLayeredSocketFactory;
import org.apache.http.conn.ssl.X509HostnameVerifier;
import org.apache.http.params.HttpParams;

import javax.net.ssl.*;
import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.X509Certificate;

/**
 * Ever-trusting SSL socket factory for testing
 */
public class PermissiveSSLSocketFactory extends SSLSocketFactory implements SchemeLayeredSocketFactory {
    private final SSLSocketFactory defaultSslSocketFactory = (SSLSocketFactory)SSLSocketFactory.getDefault();
    private final SSLContext sslContext;

    public PermissiveSSLSocketFactory() {
        try {
            sslContext = SSLContext.getInstance("TLS");
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

    private org.apache.http.conn.ssl.SSLSocketFactory socketFactoryWrapper() {
        return new org.apache.http.conn.ssl.SSLSocketFactory(sslContext.getSocketFactory(), new X509HostnameVerifier() {
            @Override
            public void verify(String s, SSLSocket sslSocket) throws IOException {
            }

            @Override
            public void verify(String s, X509Certificate x509Certificate) throws SSLException {
            }

            @Override
            public void verify(String s, String[] strings, String[] strings1) throws SSLException {
            }

            @Override
            public boolean verify(String hostname, SSLSession session) {
                return true;
            }
        });
    }

    @Override
    public Socket createSocket(String host, int port) throws IOException, UnknownHostException {
        return socketFactory().createSocket(host, port);
    }

    @Override
    public Socket createSocket(String host, int port, InetAddress localHost, int localPort) throws IOException, UnknownHostException {
        return socketFactory().createSocket(host, port, localHost, localPort);
    }

    @Override
    public Socket createSocket(InetAddress host, int port) throws IOException {
        return socketFactory().createSocket(host, port);
    }

    @Override
    public Socket createSocket(InetAddress address, int port, InetAddress localAddress, int localPort) throws IOException {
        return socketFactory().createSocket(address, port, localAddress, localPort);
    }

    @Override
    public Socket createSocket(Socket s, String host, int port, boolean autoClose) throws IOException {
        return socketFactory().createSocket(s, host, port, autoClose);
    }

    @Override
    public Socket createLayeredSocket(Socket socket, String s, int i, HttpParams httpParams) throws IOException, UnknownHostException {
        return socketFactoryWrapper().createLayeredSocket(socket, s, i, httpParams);
    }

    @Override
    public Socket createSocket(HttpParams httpParams) throws IOException {
        return socketFactoryWrapper().createSocket(httpParams) ;
    }

    @Override
    public Socket connectSocket(Socket socket, InetSocketAddress inetSocketAddress, InetSocketAddress inetSocketAddress1, HttpParams httpParams) throws IOException, UnknownHostException, ConnectTimeoutException {
        return socketFactoryWrapper().connectSocket(socket, inetSocketAddress, inetSocketAddress1, httpParams);
    }

    @Override
    public boolean isSecure(Socket socket) throws IllegalArgumentException {
        return socketFactoryWrapper().isSecure(socket);
    }
}
