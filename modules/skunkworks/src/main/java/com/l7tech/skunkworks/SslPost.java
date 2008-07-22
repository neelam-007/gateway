package com.l7tech.skunkworks;

/*
 * Copyright (C) 2005 Layer 7 Technologies Inc.
 *
 */

import org.bouncycastle.openssl.PEMReader;
import org.bouncycastle.openssl.PasswordFinder;
import org.bouncycastle.jce.provider.BouncyCastleProvider;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.X509KeyManager;
import javax.net.ssl.X509TrustManager;
import java.io.*;
import java.net.Socket;
import java.security.Principal;
import java.security.PrivateKey;
import java.security.KeyPair;
import java.security.Security;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.security.cert.CertificateFactory;
import java.security.interfaces.RSAPrivateKey;

/**
 * Test command line utility that posts a post file to a URL with a client cert.
 */
public class SslPost {

    private static void usage() {
        throw new IllegalArgumentException(
                "Usage: SslPost clientCert.pem clientKey.pem clientKeyPassword postFileIncludingHeaders.dat host port");
    }

    public static void main(String[] args) throws Exception {
        Security.addProvider(new BouncyCastleProvider());
        if (args.length < 6) usage();

        String clientCertPath = args[0];
        String clientKeyPath = args[1];
        String clientKeyPass = args[2];
        String postFilePath = args[3];
        String host = args[4];
        int port = Integer.parseInt(args[5]);

        final X509Certificate clientCert = loadClientCert(clientCertPath);
        final PrivateKey clientKey = loadClientKey(clientKeyPath, clientKeyPass.toCharArray());
        byte[] postBytes = slurpStream(new FileInputStream(postFilePath));

        final SSLContext sslContext = SSLContext.getInstance("SSL");
        X509KeyManager keyManager = new SimpleX509KeyManager(clientCert, clientKey);
        X509TrustManager trustManager = new PermissiveX509TrustManager();

        sslContext.init(new X509KeyManager[] {keyManager}, new X509TrustManager[] {trustManager}, null);

        SSLSocket sock = (SSLSocket)sslContext.getSocketFactory().createSocket(host, port);
        final OutputStream os = sock.getOutputStream();
        os.write(postBytes);
        os.flush();

        copyStream(sock.getInputStream(), System.out);
    }

    private static RSAPrivateKey loadClientKey(String clientKeyPath, final char[] keyPass) throws IOException
    {
        PEMReader pemReader = new PEMReader(new InputStreamReader(new FileInputStream(clientKeyPath)), new PasswordFinder() {
            public char[] getPassword() {
                return keyPass;
            }
        });

        Object object = pemReader.readObject();
        if (object == null)
            throw new IllegalArgumentException("Client cert private key PEM file didn't contain any recognizable object"); // shouldn't be possible
        if (object instanceof KeyPair)
            object = ((KeyPair)object).getPrivate();
        if (!(object instanceof RSAPrivateKey))
            throw new IllegalArgumentException("Client cert private key PEM file did not contain an RSAPrivateKey.  Instead it contained: " + object.getClass());
        return (RSAPrivateKey)object;
    }

    private static X509Certificate loadClientCert(String clientCertPath) throws FileNotFoundException, CertificateException {
        return (X509Certificate)CertificateFactory.getInstance("X.509").generateCertificate(new FileInputStream(clientCertPath));
    }

    private static class PermissiveX509TrustManager implements X509TrustManager {
        public void checkClientTrusted(X509Certificate[] x509Certificates, String s) throws CertificateException {
            return; // permissive
        }

        public void checkServerTrusted(X509Certificate[] x509Certificates, String s) throws CertificateException {
            return; // permissive
        }

        public X509Certificate[] getAcceptedIssuers() {
            return new X509Certificate[0];
        }
    }

    private static class SimpleX509KeyManager implements X509KeyManager {
        private final X509Certificate clientCert;
        private final PrivateKey clientKey;

        public SimpleX509KeyManager(X509Certificate clientCert, PrivateKey clientKey) {
            this.clientCert = clientCert;
            this.clientKey = clientKey;
        }

        public String[] getClientAliases(String s, Principal[] principals) {
            return new String[] { "me" };
        }

        public String chooseClientAlias(String[] strings, Principal[] principals, Socket socket) {
            return "me";
        }

        public String[] getServerAliases(String s, Principal[] principals) {
            return new String[] { "me" };
        }

        public String chooseServerAlias(String s, Principal[] principals, Socket socket) {
            return "me";
        }

        public X509Certificate[] getCertificateChain(String s) {
            return new X509Certificate[] { clientCert };
        }

        public PrivateKey getPrivateKey(String s) {
            return clientKey;
        }
    }

    public static byte[] slurpStream(InputStream stream) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream(4096);
        copyStream(stream, out);
        return out.toByteArray();
    }

    public static long copyStream(InputStream in, OutputStream out) throws IOException {
        return copyStream(in, out, 4096);
    }

    public static long copyStream(InputStream in, OutputStream out, int blocksize) throws IOException {
        if (blocksize < 1) throw new IllegalArgumentException("blocksize must be positive");
        if (in == null || out == null) throw new NullPointerException("in and out must both be non-null");
        byte[] buf = new byte[blocksize];
        int got;
        long total = 0;
        while ((got = in.read(buf)) > 0) {
            out.write(buf, 0, got);
            total += got;
        }
        return total;
    }



}
