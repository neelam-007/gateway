package com.l7tech.server.transport.tls;

import sun.misc.BASE64Decoder;

import javax.net.ssl.*;
import java.io.*;
import java.lang.reflect.Method;
import java.net.Socket;
import java.security.KeyFactory;
import java.security.Principal;
import java.security.PrivateKey;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * A tiny HTTPS server that works only with HTTP 1.0 GET requests, always returns the same text/plain response,
 * and always closes the socket after sending the response but without sending a TLS close_notify record.
 * <p/>
 * This is to repro Bug #12041, SSL-J "Inbound closed before receiving peer's close_notify: possible truncation attack?" error.
 */
public class RsaReproInboundClosedTruncationAttack {
    public static void main(String[] args) throws Exception {
        runServer();
    }

    static void runServer() throws Exception {
        X509Certificate cert = (X509Certificate) CertificateFactory.getInstance("X.509").generateCertificate(new ByteArrayInputStream(decode(RSA_1024_CERT_X509_B64)));
        PrivateKey key = KeyFactory.getInstance("RSA").generatePrivate(new PKCS8EncodedKeySpec(decode(RSA_1024_KEY_PKCS8_B64)));

        SSLContext sslContext = SSLContext.getInstance("TLS");
        sslContext.init(
            new KeyManager[] { new SingleCertKeyManager(cert, key) },
            new TrustManager[] { new TrustAllTrustManager() },
            null);

        SSLServerSocket serverSocket = (SSLServerSocket) sslContext.getServerSocketFactory().createServerSocket(14443);

        ExecutorService pool = Executors.newCachedThreadPool();

        for (;;) {
            final SSLSocket sock = (SSLSocket) serverSocket.accept();
            pool.submit(new Callable<Void>() {
                @Override
                public Void call() throws Exception {
                    handle(sock);
                    return null;
                }
            });
        }
    }

    private static void handle(SSLSocket sock) throws IOException {
        try {
            readRequestAndSendResponse(sock);
            sock.getOutputStream().flush();
            callPrivateMethod(callPrivateMethod(sock, "getImpl"), "close");
        } catch (Exception e) {
            System.out.println("Error handling request: " + e.getMessage());
            e.printStackTrace(System.out);
        } finally {
            sock.close();
        }
    }

    private static Object callPrivateMethod(Object obj, String methodName) throws Exception {
        Method method = null;
        Class c = obj.getClass();
        while (c != null && c != Object.class) {
            try {
                method = c.getDeclaredMethod(methodName);
                break;
            } catch (NoSuchMethodException e) {
                c = c.getSuperclass();
                if (c == null || c == Object.class)
                    throw e;
            }
        }
        method.setAccessible(true);
        return method.invoke(obj);
    }

    private static void readRequestAndSendResponse(SSLSocket sock) throws IOException {
        sock.setSoLinger(false, 0);

        BufferedReader br = new BufferedReader(new InputStreamReader(sock.getInputStream(), "UTF-8"));

        // Read request
        String firstLine = br.readLine();
        if (!firstLine.startsWith("GET")) {
            // Fail immediately if method not GET to avoid deadlock since we aren't planning to read a request body
            System.out.println("Ignoring non-GET request: " + firstLine);
            return;
        }

        // Read headers
        String line;
        do {
            line = br.readLine();
        } while (line.length() > 0);

        // Write response
        final OutputStream outputStream = sock.getOutputStream();
        outputStream.write("HTTP/1.0 200 Ok\r\nContent-Type: text/plain\r\n\r\nHere is a response for you!\r\n".getBytes("UTF-8"));
        outputStream.flush();
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

    private static byte[] decode(String in) throws IOException {
        return new BASE64Decoder().decodeBuffer(new ByteArrayInputStream(in.getBytes()));
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
