/*
 * Copyright (C) 2003 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.proxy;

import org.apache.log4j.Category;
import org.mortbay.http.HttpContext;
import org.mortbay.http.HttpException;
import org.mortbay.http.HttpRequest;
import org.mortbay.http.HttpResponse;
import org.mortbay.http.HttpServer;
import org.mortbay.http.SunJsseListener;
import org.mortbay.http.handler.AbstractHttpHandler;
import org.mortbay.util.InetAddrPort;
import org.mortbay.util.MultiException;
import org.mortbay.util.MultiMap;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLServerSocket;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.X509KeyManager;
import javax.net.ssl.X509TrustManager;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.NoSuchAlgorithmException;
import java.security.Principal;
import java.security.PrivateKey;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Collection;
import java.util.Enumeration;
import java.util.Iterator;

/**
 *
 * User: mike
 * Date: Jul 28, 2003
 * Time: 3:02:31 PM
 */
public class ClientCertSslServer {
    private static final Category log = Category.getInstance(ClientCertSslServer.class);
    private static final String KEYSTORE = "C:/tomcatSsl";
    private static final String KEYPASS = "tralala";
    private static final String KEYNAME = "tomcat";
    private static final int PORT = 5443;

    private static class MyKeyManager implements X509KeyManager {
        X509KeyManager defaultKeyManager = null;
        KeyStore ks;

        public PrivateKey getPrivateKey(String s) {
            log.info("MyKeyManager: getClientCertPrivateKey: s=" + s);
            try {
                ks = KeyStore.getInstance("JKS");
                FileInputStream ksFile = new FileInputStream(KEYSTORE);
                ks.load(ksFile, KEYPASS.toCharArray());
                PrivateKey key = (PrivateKey) ks.getKey(s, KEYPASS.toCharArray());
                return key;
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }

        public X509Certificate[] getCertificateChain(String s) {
            log.info("MyKeyManager: getCertificateChain: s=" + s);
            return null;
        }

        public String[] getClientAliases(String s, Principal[] principals) {
            log.info("MyKeyManager: getClientAliases");
            return new String[] {"tomcat"};
        }

        public String[] getServerAliases(String s, Principal[] principals) {
            log.info("MyKeyManager: getServerAliases");
            return new String[] {"tomcat"};
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

    public static void main(String[] args) {
        try {
            mainw();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static class MyHandler extends AbstractHttpHandler {
        /** Handle a request.
         *
         * Note that Handlers are tried in order until one has handled the
         * request. i.e. until request.isHandled() returns true.
         *
         * In broad terms this means, either a response has been commited
         * or request.setHandled(true) has been called.
         *
         * @param pathInContext The context path
         * @param pathParams Path parameters such as encoded Session ID
         * @param request The HttpRequest request
         * @param response The HttpResponse response
         */
        public void handle(String pathInContext,
                           String pathParams,
                           HttpRequest request,
                           HttpResponse response)
                throws HttpException, IOException
        {
            log.info("Got request: " + request);
            log.info("isConfidential=" + request.isConfidential());
            Enumeration names = request.getFieldNames();
            while (names.hasMoreElements()) {
                String s = (String) names.nextElement();
                log.info("Field " + s + ":" + request.getField(s));
            }

            Enumeration attrs = request.getAttributeNames();
            while (attrs.hasMoreElements()) {
                String s = (String) attrs.nextElement();
                log.info("Attribute " + s + ":" + request.getAttribute(s));
            }

            MultiMap parms = request.getParameters();
            Collection parmKeys = parms.keySet();
            for (Iterator i = parmKeys.iterator(); i.hasNext();) {
                String s = (String) i.next();
                log.info("Parameter " + s + ":" + parms.get(s));
            }


            //for (int i = 0; i < certs.length; i++) {
            //    Certificate cert = certs[i];
            //    log.info("Got cert[" + i + "]: " + cert);
            //}

            response.setStatus(200);
            response.getOutputStream().write("Woo hoo hoo!".getBytes());
            response.commit();
        }
    }

    // Entry point for using Jetty.
    private static void mainw() throws MultiException {
        HttpServer httpServer = new HttpServer();
        SunJsseListener jl = new SunJsseListener(getInetAddrPort());
        jl.setKeystore(KEYSTORE);
        jl.setKeyPassword(KEYPASS);
        jl.setPassword(KEYPASS);
        jl.setName(KEYNAME);
        httpServer.addListener(jl);
        HttpContext ctx = new HttpContext(httpServer, "/");
        ctx.addHandler(new MyHandler());

        httpServer.start();
        SSLServerSocket sss = (SSLServerSocket) jl.getServerSocket();
        sss.setWantClientAuth(true);
        //sss.setNeedClientAuth(true);

        Object obj = new Object();
        synchronized (obj) {
            try {
                obj.wait();
            } catch (InterruptedException e) {
            }
        }
    }

    private static InetAddrPort getInetAddrPort() {
        try {
            return new InetAddrPort("127.0.0.1", PORT);
        } catch (UnknownHostException e) {
            e.printStackTrace(); // can't happen
            throw new RuntimeException(e);
        }
    }

    // Entry point for playing with SslSocket server.
    public static void mainSslSocket() throws NoSuchAlgorithmException, IOException, KeyManagementException {
        //Properties props = System.getProperties();
        //props.put("java.protocol.handler.pkgs", "com.sun.net.ssl.internal.www.protocol");
        //props.put("javax.net.ssl.keyStore", KEYSTORE);
        //props.put("javax.net.ssl.keyStorePassword", KEYPASS);
        SSLContext ctx = SSLContext.getInstance("ssl");
        ctx.init(new X509KeyManager[] {new MyKeyManager()}, new X509TrustManager[] {new MyTrustManager()}, null);
        SSLServerSocket sock = (SSLServerSocket) ctx.getServerSocketFactory().createServerSocket();
        SSLSocket remote;
        while ((remote = (SSLSocket)sock.accept()) != null) {
            Certificate[] certs = remote.getSession().getPeerCertificates();
            for (int i = 0; i < certs.length; i++) {
                Certificate cert = certs[i];
                log.info("Got cert[" + i + "]: " + cert);
            }
        }

    }
}
