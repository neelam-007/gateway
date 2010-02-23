package com.l7tech.server.transport.tls;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.X509TrustManager;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.SecureRandom;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

/**
 * A TLS client that uses SunJSSE and attempts to connect to an SSL-J server that sends an optional
 * client certificate challenge during the handshake.
 */
public class RsaReproClientCertClient {
    static void assertEquals(Object a, Object b) {
        if (!a.equals(b)) throw new AssertionError("Objects not equal: " + a + " and " + b);
    }

    static void copy(InputStream in, OutputStream out) throws IOException {
        byte[] buf = new byte[512];
        int got;
        while ((got = in.read(buf)) > 0)
            out.write(buf, 0, got);
    }

    static byte[] slurp(InputStream in) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        copy(in, out);
        return out.toByteArray();
    }

    static class TrustEverythingTrustManager implements X509TrustManager {
        @Override public void checkClientTrusted(X509Certificate[] x509Certificates, String s) throws CertificateException { }
        @Override public void checkServerTrusted(X509Certificate[] x509Certificates, String s) throws CertificateException { }
        @Override public X509Certificate[] getAcceptedIssuers() { return new X509Certificate[0]; }
    }

    public static void main(String[] args) throws Exception {
        SSLContext sslContext = SSLContext.getInstance("TLS");
        assertEquals("SunJSSE", sslContext.getProvider().getName());

        sslContext.init(
                null,
                new X509TrustManager[] { new TrustEverythingTrustManager() },
                new SecureRandom());

        SSLSocketFactory csf = sslContext.getSocketFactory();

        System.out.println("Attempting to connect to localhost:17443 ...");
        SSLSocket sock = (SSLSocket) csf.createSocket("localhost", 17443);

        byte[] buf = "Test block of exactly 80 bytes: test test test test test test test test test tes".getBytes();
        assertEquals(80, buf.length);
        sock.getOutputStream().write(buf); // Exception occurs on this line if server has wantClientAuth enabled
        sock.getOutputStream().flush();
        String response = new String(slurp(sock.getInputStream()));
        System.out.println("Got response: " + response);
    }

    /*
    Exception is similar to the following (when using SunJSSE for the client):

Exception in thread "main" javax.net.ssl.SSLException: Received fatal alert: illegal_parameter
	at com.sun.net.ssl.internal.ssl.Alerts.getSSLException(Alerts.java:190)
	at com.sun.net.ssl.internal.ssl.Alerts.getSSLException(Alerts.java:136)
	at com.sun.net.ssl.internal.ssl.SSLSocketImpl.recvAlert(SSLSocketImpl.java:1682)
	at com.sun.net.ssl.internal.ssl.SSLSocketImpl.readRecord(SSLSocketImpl.java:932)
	at com.sun.net.ssl.internal.ssl.SSLSocketImpl.performInitialHandshake(SSLSocketImpl.java:1112)
	at com.sun.net.ssl.internal.ssl.SSLSocketImpl.writeRecord(SSLSocketImpl.java:623)
	at com.sun.net.ssl.internal.ssl.AppOutputStream.write(AppOutputStream.java:59)
	at java.io.OutputStream.write(OutputStream.java:58)
	at com.l7tech.server.transport.tls.RsaReproClientCertClient.main(RsaReproClientCertClient.java:59)
	at sun.reflect.NativeMethodAccessorImpl.invoke0(Native Method)
	at sun.reflect.NativeMethodAccessorImpl.invoke(NativeMethodAccessorImpl.java:39)
	at sun.reflect.DelegatingMethodAccessorImpl.invoke(DelegatingMethodAccessorImpl.java:25)
	at java.lang.reflect.Method.invoke(Method.java:597)
	at com.intellij.rt.execution.application.AppMain.main(AppMain.java:110)
     */
}
