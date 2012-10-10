package com.l7tech.server.transport.tls;

import com.rsa.jsafe.provider.JsafeJCE;
import com.rsa.jsse.JsseProvider;
import sun.misc.BASE64Decoder;

import javax.net.ssl.*;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.security.*;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.security.spec.PKCS8EncodedKeySpec;

/**
 * This binds a TLS 1.0 + 1.1 server socket with default cipher suites that uses the SSLContext.TLS implementation
 * from the SSL-J 6.0 provider, with Crypto-J 6.0.0.1 registered as an additional (least-preference) security provider.
 */
public class RsaReproDhPrimeSizeServer {
    public static final int PORT = 14443;

    public static void main(String[] args) throws Exception {
        configureBsafeProviders();

        // Get server cert and key
        X509Certificate cert = (X509Certificate) CertificateFactory.getInstance("X.509").generateCertificate(new ByteArrayInputStream(decode(RSA_1024_CERT_X509_B64)));
        PrivateKey key = KeyFactory.getInstance("RSA").generatePrivate(new PKCS8EncodedKeySpec(decode(RSA_1024_KEY_PKCS8_B64)));

        runTlsEchoServer(createSslContext(cert, key), PORT);
    }

    private static void configureBsafeProviders() {
        // Install Crypto-J as least-preference
        Security.addProvider(new JsafeJCE());

        // Install SSL-J as highest preference, move SunJSSE to least-preference
        Security.insertProviderAt(new JsseProvider(), 1);
        Provider sunjsse = Security.getProvider("SunJSSE");
        if (sunjsse != null) {
            // Move SunJSSE provider to the end of the list.
            Security.removeProvider("SunJSSE");
            Security.addProvider(sunjsse);
        }
    }

    private static SSLContext createSslContext(X509Certificate cert, PrivateKey key) throws NoSuchAlgorithmException, KeyManagementException {
        // Create SSL context with permissive trust
        SSLContext sslContext = SSLContext.getInstance("TLS");
        sslContext.init(new KeyManager[] {new SingleCertKeyManager(cert,key)}, new TrustManager[] {new TrustAllTrustManager()}, null);
        return sslContext;
    }

    private static void runTlsEchoServer(SSLContext sslContext, int port) throws IOException {
        System.out.println("Listening on " + port);
        final ServerSocket serverSocket = sslContext.getServerSocketFactory().createServerSocket(port);
        for (;;) {
            final Socket socket = serverSocket.accept();
            System.out.println("Connection from " + socket.getInetAddress().getHostAddress() + " port " + socket.getPort());
            try {
                handleRequest(socket);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private static void handleRequest(Socket socket) throws IOException {
        final SSLSocket sslSocket = (SSLSocket) socket;
        sslSocket.setEnabledProtocols(new String[]{"TLSv1", "TLSv1.1"});

        String suite = sslSocket.getSession().getCipherSuite();

        // Read and echo 1024 bytes
        byte[] buf = new byte[1024];
        int got = socket.getInputStream().read(buf);
        OutputStream os = socket.getOutputStream();
        if (got > 0) {
            os.write(buf, 0, got);
            os.flush();
        }
        socket.close();

        System.out.println("Successfully echoed using " + suite);
    }

    private static byte[] decode(String in) throws IOException {
        return new BASE64Decoder().decodeBuffer(new ByteArrayInputStream(in.getBytes()));
    }

    private static class TrustAllTrustManager implements X509TrustManager {
        public void checkClientTrusted(X509Certificate[] chain, String authType) throws CertificateException {}
        public void checkServerTrusted(X509Certificate[] chain, String authType) throws CertificateException {}
        public X509Certificate[] getAcceptedIssuers() {return new X509Certificate[0];}
    }

    private static class SingleCertKeyManager implements X509KeyManager {
        final X509Certificate cert;
        final PrivateKey privateKey;

        private SingleCertKeyManager(X509Certificate cert, PrivateKey privateKey) {
            this.cert = cert;
            this.privateKey = privateKey;
        }

        public String[] getClientAliases(String keyType, Principal[] issuers) {return new String[] { "a" };}
        public String chooseClientAlias(String[] keyType, Principal[] issuers, Socket socket) {return "a";}
        public String[] getServerAliases(String keyType, Principal[] issuers) {return new String[] { "a" };}
        public String chooseServerAlias(String keyType, Principal[] issuers, Socket socket) {return "a";}
        public X509Certificate[] getCertificateChain(String alias) {return new X509Certificate[] { cert };}
        public PrivateKey getPrivateKey(String alias) {return privateKey;}
    }

    // Server key and cert
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
}
