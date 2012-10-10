package com.l7tech.server.transport.tls;

import javax.net.ssl.*;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Arrays;

/**
 * This uses the default providers (SSLContext.TLS from SunJSSE provider and KeyAgreement.DH from SunJCE provider)
 * to perform an outbound TLS 1.0 handshake to a target URL using TLS_DHE_RSA_WITH_AES_256_CBC_SHA.
 */
public class RsaReproDhPrimeSizeClient {
    public static final String HOST = "127.0.0.1";
    public static final int PORT = 14443;

    public static void main(String[] args) throws Exception {
        // Allow self-signed server cert
        SSLContext sslContext = SSLContext.getInstance("TLS");
        sslContext.init(null, new TrustManager[] { new TrustAllTrustManager() }, null);

        SSLSocket socket = (SSLSocket)sslContext.getSocketFactory().createSocket(HOST, PORT);
        socket.setEnabledCipherSuites(new String[] {"TLS_DHE_RSA_WITH_AES_256_CBC_SHA"});
        socket.setEnabledProtocols(new String[]{"TLSv1"});

        byte[] reqblock = new byte[1024];
        byte[] req = "Test data blah blah blah".getBytes();
        System.arraycopy(req, 0, reqblock, 0, req.length);

        String suite = socket.getSession().getCipherSuite();

        socket.getOutputStream().write(reqblock);
        byte[] resblock = new byte[1024];
        int got = socket.getInputStream().read(resblock);
        assert got > 0;
        assert Arrays.equals(reqblock, resblock);

        System.out.println("Got successful echo using " + suite);
    }

    private static class TrustAllTrustManager implements X509TrustManager {
        public void checkClientTrusted(X509Certificate[] chain, String authType) throws CertificateException {}
        public void checkServerTrusted(X509Certificate[] chain, String authType) throws CertificateException {}
        public X509Certificate[] getAcceptedIssuers() {return null;}
    }
}
