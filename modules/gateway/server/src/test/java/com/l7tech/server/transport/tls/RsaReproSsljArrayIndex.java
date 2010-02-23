package com.l7tech.server.transport.tls;

import com.rsa.jsafe.provider.JsafeJCE;
import com.rsa.jsse.JsseProvider;
import sun.misc.BASE64Decoder;

import javax.net.ssl.SSLContext;
import javax.net.ssl.X509KeyManager;
import javax.net.ssl.X509TrustManager;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.Socket;
import java.security.*;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.security.spec.PKCS8EncodedKeySpec;

/**
 * Test case that reproduces a bug in SSL-J 5.0.2 that causes an ArrayIndexOutOfBoundsException when 
 * initializing an SSL context with a custom KeyManager.
 * <p/>
 * The bug has been fixed as of SSL-J 5.1 which is not FIPS certified at time of writing.
 */
public class RsaReproSsljArrayIndex {
    private static final String P256_CERT =
            "MIIB2TCCAV6gAwIBAgIJAPZWUuHDi+LuMAoGCCqGSM49BAMDMB8xHTAbBgNVBAMTFHAzODQudGVz\n" +
            "dC5sN3RlY2guY29tMB4XDTA5MTEwNjAwNTE0NVoXDTI5MTEwMTAwNTE0NVowHzEdMBsGA1UEAxMU\n" +
            "cDM4NC50ZXN0Lmw3dGVjaC5jb20wdjAQBgcqhkjOPQIBBgUrgQQAIgNiAASi0pX2+iG5oIM+swX2\n" +
            "w2o6+OnON1X3vbok8cFrIdTl0gZp1S1Nsh5bEKur7GnbDr1C4oor/zMepMVbe32U7gm/fAKuMQai\n" +
            "JsguHDlvsOGQnKX7kDEUxekc/DV3fMDRRtejZjBkMA4GA1UdDwEB/wQEAwIF4DASBgNVHSUBAf8E\n" +
            "CDAGBgRVHSUAMB0GA1UdDgQWBBSXe1AMgtNkDsCJxIEvoZEt/kpr9DAfBgNVHSMEGDAWgBSXe1AM\n" +
            "gtNkDsCJxIEvoZEt/kpr9DAKBggqhkjOPQQDAwNpADBmAjEAkw+1JqMs3bvTVFbpOefzPbKSUjmr\n" +
            "W+zlAZHlH34x5/tw6Fw9E+p70GfPK+Am6y93AjEAhFgh9yta3knMsUADb318EainJXlBt9A6kxJq\n" +
            "EfQoXA4ukVpWiwNgUFTU4ySlsXLh";

    private static final String P256_PRIVATE_KEY =
            "ME4CAQAwEAYHKoZIzj0CAQYFK4EEACIENzA1AgEBBDDcXN71Qoa5XupsGUbrrVkCgGww2I3Acy2V\n" +
            "nHm/Vpl6FEhWeQAlK/G/SRxeJFELe6w=";

    static PrivateKey getPrivateKey() throws GeneralSecurityException, IOException {
        return KeyFactory.getInstance("EC").generatePrivate(new PKCS8EncodedKeySpec(new BASE64Decoder().decodeBuffer(P256_PRIVATE_KEY)));
    }

    static X509Certificate getCertificate() throws CertificateException, IOException {
        return (X509Certificate) CertificateFactory.getInstance("X.509").generateCertificate(new ByteArrayInputStream(new BASE64Decoder().decodeBuffer(P256_CERT)));
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
        //Security.insertProviderAt(new com.rsa.jcp.RSAJCP(), 1);

        // Try to initialize default SSL context
        SSLContext sslContext = SSLContext.getInstance("TLS");
        assertEquals("RsaJsse", sslContext.getProvider().getName());

        // The following line always throws java.lang.ArrayIndexOutOfBoundsException: 1 from com.rsa.sslj.x.J.<init>
        sslContext.init(
                new X509KeyManager[] { new SingleCertX509KeyManager(getCertificate(), getPrivateKey()) },
                new X509TrustManager[] { new TrustEverythingTrustManager() },
                null);

        /*
Exception in thread "main" java.lang.ArrayIndexOutOfBoundsException: 1
	at com.rsa.sslj.x.J.<init>(Unknown Source)
	at com.rsa.sslj.x.u.a(Unknown Source)
	at com.rsa.sslj.x.bj.engineInit(Unknown Source)
	at javax.net.ssl.SSLContext.init(SSLContext.java:248)
	at com.l7tech.server.transport.tls.RsaReproSsljArrayIndex.main(RsaReproSsljArrayIndex.java:78)
	at sun.reflect.NativeMethodAccessorImpl.invoke0(Native Method)
	at sun.reflect.NativeMethodAccessorImpl.invoke(NativeMethodAccessorImpl.java:39)
	at sun.reflect.DelegatingMethodAccessorImpl.invoke(DelegatingMethodAccessorImpl.java:25)
	at java.lang.reflect.Method.invoke(Method.java:597)
	at com.intellij.rt.execution.application.AppMain.main(AppMain.java:110)
         */
    }
}
