package com.l7tech.server.transport.tls;

import com.rsa.jsafe.provider.JsafeJCE;
import com.rsa.jsse.JsseProvider;
import com.rsa.ssl.SSLParams;
import sun.misc.BASE64Decoder;

import javax.net.ssl.*;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.Socket;
import java.security.*;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.security.spec.PKCS8EncodedKeySpec;

/**
 * An SSL-J TLS server that attempts to send an optional client certificate challenge during the handshake.
 */
public class RsaReproClientCertServer {
    public static final String RSA_1024_CERT_X509_B64 =
        "MIICFDCCAX2gAwIBAgIJANA/LVIWYlZMMA0GCSqGSIb3DQEBDAUAMBgxFjAUBgNVBAMMDXRlc3Rf" +
        "cnNhXzEwMjQwHhcNMDkxMjE2MjM0MTE4WhcNMzQxMjEwMjM0MTE4WjAYMRYwFAYDVQQDDA10ZXN0" +
        "X3JzYV8xMDI0MIGfMA0GCSqGSIb3DQEBAQUAA4GNADCBiQKBgQCDh9hs9BnqyPvL7qoHARHjKwq9" +
        "ZwGeeWDU+oed9H4/Qjnw5PZ54ZgXfU+pEisDxADHfvHXnMrUNuSOXNaH1Lyg+EjOWwQVRW7EbQFK" +
        "paMP6d6FLy70/ErA616i1dPE+gmdtQEZiAqoe+5gch0oZVVu5V6cREFcjzVSv3K5Uo5PhQIDAQAB" +
        "o2YwZDAOBgNVHQ8BAf8EBAMCBeAwEgYDVR0lAQH/BAgwBgYEVR0lADAdBgNVHQ4EFgQU3bG81B25" +
        "MHuoBRi9apZWR2bVqHMwHwYDVR0jBBgwFoAU3bG81B25MHuoBRi9apZWR2bVqHMwDQYJKoZIhvcN" +
        "AQEMBQADgYEADeL5oHQBkkqkojQ+GQBFOpYuDq6yi4QkAe1CKlt4ieXczmoPd1NmhWY8U+AyORdu" +
        "9I8H+N/OAwfCHNqS9a7xBjd55gObOJ1ZDYJEVXSJ/gx0vRwm166BY5A6hF/7F24Me5ItDiwQbK1c" +
        "J7t7E2C6q1B2qkLUujTACbCAyCpv5B4=";

    public static final String RSA_1024_KEY_PKCS8_B64 =
        "MIICdgIBADANBgkqhkiG9w0BAQEFAASCAmAwggJcAgEAAoGBAIOH2Gz0GerI+8vuqgcBEeMrCr1n" +
        "AZ55YNT6h530fj9COfDk9nnhmBd9T6kSKwPEAMd+8decytQ25I5c1ofUvKD4SM5bBBVFbsRtAUql" +
        "ow/p3oUvLvT8SsDrXqLV08T6CZ21ARmICqh77mByHShlVW7lXpxEQVyPNVK/crlSjk+FAgMBAAEC" +
        "gYAE60ix0nNBr6CTIOrk9ipIF60AJmEOHzX64R+/TYyHKx/lnXqGVmSMxFf9V7uaGXN6Aopi6O9A" +
        "/oiPtnMjg1ZGlP7ONFyaf0ZsaMs4jm7FAfDHtnaemEJkDEadYSvppN8oB1bPm1NYe6mAvaui3PiM" +
        "EGkkq+MSgms5j8RFHyUvBQJBANYoloJJ6hfhRGJiTSyP1TRYoKHrf2mVFnEyHxALhK32+TnasKZI" +
        "CkVLcAPhqfnNQKwn3nATfc0ZXeI9BMTwHNMCQQCdOoSZCNUFjslXOC1zS+XtoiE1znjtlIYqGR+h" +
        "TLWdJQkdyoB5ATOtL3uj+7muwMmKc7rmsW6imAqxxwDBmctHAkABMVatQRYhreqAlcWSQvbQBNJY" +
        "NISQJPlsBfhwUXAau+5laRdkxa/w9NuZ2e7lakQ68Tnm6+TeeI6yTN6y7hdrAkBmgWtHdomjSPcd" +
        "RQPkwlvSNLygHs+aXRWnRp/ngmJ5ZFbwNEDUIyN0yps6SvhA5XHAMTluA8nUeXmnc82batArAkEA" +
        "gW39CjSVbeUpgwEpt2CAz0qa08IQ56clJcyCHiwLQelfezLrQkwX+k6Lkf11JxOsf9a4A2ToElml" +
        "KyZu+sPE/g==";

    static PrivateKey getPrivateKey() throws GeneralSecurityException, IOException {
        return KeyFactory.getInstance("RSA").generatePrivate(new PKCS8EncodedKeySpec(new BASE64Decoder().decodeBuffer(RSA_1024_KEY_PKCS8_B64)));
    }

    static X509Certificate getCertificate() throws CertificateException, IOException {
        return (X509Certificate) CertificateFactory.getInstance("X.509").generateCertificate(new ByteArrayInputStream(new BASE64Decoder().decodeBuffer(RSA_1024_CERT_X509_B64)));
    }

    static void assertEquals(Object a, Object b) {
        if (!a.equals(b)) throw new AssertionError("Objects not equal: " + a + " and " + b);
    }

    static class SingleCertX509KeyManager implements X509KeyManager {
        final X509Certificate cert;
        final PrivateKey key;
        SingleCertX509KeyManager(X509Certificate cert, PrivateKey key) { this.cert = cert; this.key = key; }
        @Override public String[] getClientAliases(String s, Principal[] principals) { return new String[] { "x" }; }
        @Override public String chooseClientAlias(String[] strings, Principal[] principals, Socket socket) { return "x"; }
        @Override public String[] getServerAliases(String s, Principal[] principals) { return new String[] { "x" }; }
        @Override public String chooseServerAlias(String s, Principal[] principals, Socket socket) { return "x"; }
        @Override public X509Certificate[] getCertificateChain(String s) { return new X509Certificate[] { cert }; }
        @Override public PrivateKey getPrivateKey(String s) { return key; }
    }

    static class TrustEverythingTrustManager implements X509TrustManager {
        @Override public void checkClientTrusted(X509Certificate[] x509Certificates, String s) throws CertificateException { }
        @Override public void checkServerTrusted(X509Certificate[] x509Certificates, String s) throws CertificateException { }
        @Override public X509Certificate[] getAcceptedIssuers() { return new X509Certificate[0]; }
    }

    public static void main(String[] args) throws Exception {
        // Configure providers
        Security.removeProvider("SunJSSE");
        Security.insertProviderAt(new JsafeJCE(), 1);
        Security.insertProviderAt(new JsseProvider(), 1);
        SSLParams.setDebug(SSLParams.DEBUG_STATE | SSLParams.DEBUG_DATA);

        // Try to initialize default SSL context
        SSLContext sslContext = SSLContext.getInstance("TLS");
        assertEquals("RsaJsse", sslContext.getProvider().getName());

        sslContext.init(
                new X509KeyManager[] { new SingleCertX509KeyManager(getCertificate(), getPrivateKey()) },
                new X509TrustManager[] { new TrustEverythingTrustManager() },
                new SecureRandom());

        SSLServerSocketFactory ssf = sslContext.getServerSocketFactory();
        SSLServerSocket sock = (SSLServerSocket) ssf.createServerSocket(17443);
        sock.setEnabledProtocols("TLSv1,TLSv1.1,TLSv1.2".split(","));
        sock.setNeedClientAuth(false);

        // Setting this to true causes server to send "illegal parameter" alert immediately after sending its "certificate chain" message
        sock.setWantClientAuth(true);

        byte buf[] = new byte[80];
        for (;;) {
            System.out.println("Awaiting connection on " + sock.getLocalSocketAddress());
            SSLSocket s = (SSLSocket) sock.accept();
            System.out.println("Connected: " + s.getSession().getProtocol() + ": " + s.getSession().getCipherSuite());
            assertEquals(80, s.getInputStream().read(buf));
            String request = new String(buf);
            System.out.println("Read: " + request);
            s.getOutputStream().write(("Echoing request: " + request).getBytes());
            System.out.println("Closing connection");
            s.close();
        }
    }
}
