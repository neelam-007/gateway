/*
 * Copyright (C) 2003 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.proxy;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpException;
import org.apache.commons.httpclient.ConnectTimeoutException;
import org.apache.commons.httpclient.params.HttpConnectionParams;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.protocol.Protocol;
import org.apache.commons.httpclient.protocol.SecureProtocolSocketFactory;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.X509KeyManager;
import javax.net.ssl.X509TrustManager;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.security.*;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.logging.Logger;

/**
 * User: mike
 * Date: Jul 29, 2003
 * Time: 10:37:49 AM
 */
public class ClientCertSslClient {
    public static final Logger log = Logger.getLogger(ClientCertSslClient.class.getName());
    public static final String CONFIG_DIR =
      System.getProperties().getProperty("user.home") + File.separator + ".l7tech";
    public static final File TRUST_FILE =
      new File(CONFIG_DIR + File.separator + "trustStore");
    public static final String TRUST_PASS = "password";
    public static final String KEYSTORE = "C:/tomcatSsl";
    public static final String KEYPASS = "tralala";
    private static final String KEYNAME = "tomcat";

    private static class MyKeyManager implements X509KeyManager {
        X509KeyManager defaultKeyManager = null;
        KeyStore ks = null;

        private KeyStore getKeyStore() {
            try {
                if (ks == null) {
                    ks = KeyStore.getInstance("JKS");
                    FileInputStream ksFile = new FileInputStream(KEYSTORE);
                    ks.load(ksFile, KEYPASS.toCharArray());
                }
                return ks;
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }

        public PrivateKey getPrivateKey(String s) {
            log.info("MyKeyManager: getClientCertPrivateKey: s=" + s);
            try {
                PrivateKey key = (PrivateKey)getKeyStore().getKey(s, KEYPASS.toCharArray());
                if (key == null)
                    throw new RuntimeException("Could not find private key");
                log.info("Found client private key");
                return key;
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }

        public X509Certificate[] getCertificateChain(String s) {
            log.info("MyKeyManager: getCertificateChain: s=" + s);
            try {
                X509Certificate cert = (X509Certificate)getKeyStore().getCertificate(s);
                log.info("Using client certificate " + cert);
                return new X509Certificate[]{cert};
            } catch (KeyStoreException e) {
                throw new RuntimeException(e);
            }
        }

        public String[] getClientAliases(String s, Principal[] principals) {
            log.info("MyKeyManager: getClientAliases");
            return new String[]{"tomcat"};
        }

        public String[] getServerAliases(String s, Principal[] principals) {
            log.info("MyKeyManager: getServerAliases");
            return new String[]{"tomcat"};
        }

        public String chooseServerAlias(String s, Principal[] principals, Socket socket) {
            log.info("MyKeyManager: chooseServerAlias: s=" + s + "  principals=" + principals + "  socket=" + socket);
            return "tomcat";
        }

        public String chooseClientAlias(String[] strings, Principal[] principals, Socket socket) {
            InetAddress ia = socket.getInetAddress();
            String hostname = ia.getHostName();
            log.info("MyKeyManager: chooseClientAlias: ia=" + ia + "  hostname=" + hostname);

            return "tomcat";
        }
    }

    private static class MyTrustManager implements X509TrustManager {
        public X509Certificate[] getAcceptedIssuers() {
            log.info("MyTrustManager.getAcceptedIssuers()");
            return new X509Certificate[0];
        }

        public void checkClientTrusted(X509Certificate[] x509Certificates, String s) throws CertificateException {
            log.info("MyTrustManager.checkClientTrusted()");
        }

        public void checkServerTrusted(X509Certificate[] x509Certificates, String s) throws CertificateException {
            log.info("MyTrustManager.checkServerTrusted()");
        }
    }

    private static class MySocketFactory implements SecureProtocolSocketFactory {
        private SSLContext sslContext;

        private MySocketFactory(SSLContext ctx) {
            this.sslContext = ctx;
        }

        public Socket createSocket(Socket socket, String host, int port, boolean autoClose)
          throws IOException, UnknownHostException {
            log.info("MySocketFactory.createSocket1(): host=" + host);
            final SSLSocket sock = (SSLSocket)sslContext.getSocketFactory().createSocket(socket, host, port, autoClose);
            log.info("Socket is type: " + sock.getClass());
            return sock;
        }

        public Socket createSocket(String host, int port, InetAddress clientHost, int clientPort)
          throws IOException, UnknownHostException {
            log.info("MySocketFactory.createSocket2(): host=" + host);
            final SSLSocket sock = (SSLSocket)sslContext.getSocketFactory().createSocket(host, port, clientHost, clientPort);
            log.info("Socket is type: " + sock.getClass());
            return sock;
        }

        public Socket createSocket(String host, int port) throws IOException, UnknownHostException {
            log.info("MySocketFactory.createSocket3(): host=" + host);
            final SSLSocket sock = (SSLSocket)sslContext.getSocketFactory().createSocket(host, port);
            log.info("Socket is type: " + sock.getClass());
            return sock;
        }

        public Socket createSocket(String host, int port, InetAddress localAddress, int localPort, HttpConnectionParams params) throws IOException, UnknownHostException, ConnectTimeoutException {
            log.info("MySocketFactory.createSocket4(): host=" + host);
            final SSLSocket sock = (SSLSocket)sslContext.getSocketFactory().createSocket(host, port, localAddress, localPort);
            log.info("Socket is type: " + sock.getClass());
            return sock;
        }

    }

    public static void main(String[] args) {
        try {
            doMain();
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(1);
        }
    }

    private static void doMain()
      throws NoSuchProviderException, NoSuchAlgorithmException,
      KeyManagementException, IOException, HttpException {
        // Set up SSL trust store
        MyKeyManager keyManager = new MyKeyManager();
        MyTrustManager trustManager = new MyTrustManager();
        SSLContext sslContext = SSLContext.getInstance("SSL", "SunJSSE");
        sslContext.init(new X509KeyManager[]{keyManager},
          new X509TrustManager[]{trustManager},
          null);
        Protocol https = new Protocol("https", new MySocketFactory(sslContext), 443);
        Protocol.registerProtocol("https", https);

        HttpClient httpClient = new HttpClient();
        GetMethod gm = new GetMethod("https://127.0.0.1:5443/foo/bar/baz");
        httpClient.executeMethod(gm);
        System.out.println("Got back: " + gm.getResponseBody());

    }
}
