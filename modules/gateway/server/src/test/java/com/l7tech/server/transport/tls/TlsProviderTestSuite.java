package com.l7tech.server.transport.tls;

import sun.misc.BASE64Decoder;

import javax.net.ssl.*;
import java.io.*;
import java.lang.reflect.Method;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.security.*;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

/**
 * A suite of tests that can be used to test a variety of SSL scenarios.
 * To use it, start a server in one of the configurations, and then run a client in a different configuration.
 */
public class TlsProviderTestSuite {
    boolean server;
    public String host = "127.0.0.1";
    public String port = "17443";
    public String tlsprov = null;
    public String jceprov = "rsa";
    public String tlsversions = null;
    public String ciphers = null;
    public String certtype = "rsa";
    public String clientcert = "no";
    public String debug = "false";
    public String timeoutMillis = "15000";
    public String startupDelayMillis = "2500";
    public String includeRealIssuers = "true";
    public String includeBogusIssuer = "false";
    public String additionalProviders = null;
    public String firstPlaceProviders = null;
    public String tokenPin = null;

    static long starttime;

    static final String testblock = "Test block of exactly 80 bytes: test test test test test test test test test tes";
    static final int testblocklen = testblock.getBytes().length;

    // A test RSA cert to use as a server or client cert
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

    // A test ECC cert to use as a server or client cert
    private static final String EC_P256_CERT =
            "MIIB2TCCAV6gAwIBAgIJAPZWUuHDi+LuMAoGCCqGSM49BAMDMB8xHTAbBgNVBAMTFHAzODQudGVz\n" +
            "dC5sN3RlY2guY29tMB4XDTA5MTEwNjAwNTE0NVoXDTI5MTEwMTAwNTE0NVowHzEdMBsGA1UEAxMU\n" +
            "cDM4NC50ZXN0Lmw3dGVjaC5jb20wdjAQBgcqhkjOPQIBBgUrgQQAIgNiAASi0pX2+iG5oIM+swX2\n" +
            "w2o6+OnON1X3vbok8cFrIdTl0gZp1S1Nsh5bEKur7GnbDr1C4oor/zMepMVbe32U7gm/fAKuMQai\n" +
            "JsguHDlvsOGQnKX7kDEUxekc/DV3fMDRRtejZjBkMA4GA1UdDwEB/wQEAwIF4DASBgNVHSUBAf8E\n" +
            "CDAGBgRVHSUAMB0GA1UdDgQWBBSXe1AMgtNkDsCJxIEvoZEt/kpr9DAfBgNVHSMEGDAWgBSXe1AM\n" +
            "gtNkDsCJxIEvoZEt/kpr9DAKBggqhkjOPQQDAwNpADBmAjEAkw+1JqMs3bvTVFbpOefzPbKSUjmr\n" +
            "W+zlAZHlH34x5/tw6Fw9E+p70GfPK+Am6y93AjEAhFgh9yta3knMsUADb318EainJXlBt9A6kxJq\n" +
            "EfQoXA4ukVpWiwNgUFTU4ySlsXLh";

    private static final String EC_P256_PRIVATE_KEY =
            "ME4CAQAwEAYHKoZIzj0CAQYFK4EEACIENzA1AgEBBDDcXN71Qoa5XupsGUbrrVkCgGww2I3Acy2V\n" +
            "nHm/Vpl6FEhWeQAlK/G/SRxeJFELe6w=";

    // A certificate unrelated to the server or client certs
    public static final String RSA_1536_CERT_X509_B64 =
        "MIICkzCCAbygAwIBAgIIfWqcn+A0noswDQYJKoZIhvcNAQEMBQAwGDEWMBQGA1UEAwwNdGVzdF9y" +
        "c2FfMTUzNjAeFw0wOTEyMTYyMzQxMThaFw0zNDEyMTAyMzQxMThaMBgxFjAUBgNVBAMMDXRlc3Rf" +
        "cnNhXzE1MzYwgd8wDQYJKoZIhvcNAQEBBQADgc0AMIHJAoHBAKao+zmD0aGU01+3A5oP0bCbIkXs" +
        "K6kMVmvPLIkDF8CqNiy8cbo8vOzhioxgKvAMVyUfS/2N4xxsYRBw4QgPkWdYx5h2TzX0xFmrza34" +
        "PbLPfwmU9OT/EgWxxLebsnZOUSBeVzXoCp7IVVlnf9lWSr0fpRVKOug6wgCKHb3xlr85+cvfwesO" +
        "Dd0ioCnL5kg87/rHOQ4bdLAe8gKwm4iczr/IwMa1aFKezAwLclFskq1m3W+ckSnGauPtBLtZc6fb" +
        "fwIDAQABo2YwZDAOBgNVHQ8BAf8EBAMCBeAwEgYDVR0lAQH/BAgwBgYEVR0lADAdBgNVHQ4EFgQU" +
        "ZC4c/Jqu0OqKm4hhU/rV/mcEzBswHwYDVR0jBBgwFoAUZC4c/Jqu0OqKm4hhU/rV/mcEzBswDQYJ" +
        "KoZIhvcNAQEMBQADgcEAU6snBQGrdeQeDNATet43UqmtXx8Dv6TLtIEi9Wx9gZE/9nCaFRpvn9Fl" +
        "cgESzKCvKpWWnbf6PN3vqDuNnyddZJfnYLSVxxhhx20sSUvAOlVGvU+igQdxNnS84FDFobE0WYzH" +
        "DQeBAk8bxd6sCiPJgb4mcyPjaW3FJXfvV2hEevVMF4B5JHzcSVtoTJ8dnDDEVeRWYKM4buWE8owh" +
        "M7k06VZ4UbqXmGilfpM+SsTL7qraLKH8cW+ZxtVbZQVczuly";

    static PrivateKey getPrivateKey(boolean ecc) throws GeneralSecurityException, IOException {
        return getPrivateKey(ecc, ecc ? EC_P256_PRIVATE_KEY : RSA_1024_KEY_PKCS8_B64);
    }

    static PrivateKey getPrivateKey(boolean ecc, String b64) throws InvalidKeySpecException, NoSuchAlgorithmException, IOException {
        return KeyFactory.getInstance(ecc ? "EC" : "RSA").generatePrivate(new PKCS8EncodedKeySpec(new BASE64Decoder().decodeBuffer(b64)));
    }

    static X509Certificate getCertificate(boolean ecc) throws CertificateException, IOException {
        return getCertificate(ecc ? EC_P256_CERT : RSA_1024_CERT_X509_B64);
    }

    static X509Certificate getCertificate(String b64) throws CertificateException, IOException {
        return (X509Certificate) CertificateFactory.getInstance("X.509").generateCertificate(new ByteArrayInputStream(new BASE64Decoder().decodeBuffer(b64)));
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
        private X509Certificate[] issuers;
        TrustEverythingTrustManager(X509Certificate[] issuers) { this.issuers = issuers; }
        @Override public void checkClientTrusted(X509Certificate[] x509Certificates, String s) throws CertificateException { System.out.println("trusting client"); }
        @Override public void checkServerTrusted(X509Certificate[] x509Certificates, String s) throws CertificateException { System.out.println("trusting server"); }
        @Override public X509Certificate[] getAcceptedIssuers() { return issuers; }
    }

    public TlsProviderTestSuite(String clientOrServer) {
        if ("server".equals(clientOrServer)) {
            this.server = true;
        } else if ("client".equals(clientOrServer)) {
            this.server = false;
        } else
            usage();
    }

    static void usage() {
        System.err.println("Usage: TlsProviderTestSuite <server|client> [additional options]\n" +
                "  options that apply to both server and client:\n" +
                "    host <hostname|ip>              server listens on, or client connects to, specified host or address\n" +
                "    port <port>                     server listens on, or client connects to, specified port\n" +
                "    tlsversions <TLSv1|TLSv1.2>     comma delimited specific version of TLS to use, otherwise uses defaults\n" +
                "    tlsprov <sun|rsa>               JSSE provider to use, either SunJSSE or RsaJsse\n" +
                "    jceprov <sun|rsa|luna4|luna5>   extra JCE provider to register for test\n" +
                "    tokenPin <token PIN>            Luna partition PIN; required if using jceprov luna4 or luna5\n" +
                "    ciphers <suite,suite>           comma delimited cipher suites to enable, otherwise uses defaults\n" +
                "    certtype <rsa|ecc>              whether to use an ECC or RSA key for this side's certificate\n" +
                "    debug <true|false>              whether to output extra debugging information\n" +
                "    includeRealIssuers <true|false> whether to include the ECC and RSA certs in the accepted issuers list\n" +
                "    includeBogusIssuer <true|false> whether to include an unrelated RSA cert in the accepted issuers list\n" +
                "    additionalProviders <com.abc.D> comma delimited extra Security Provider classnames to install\n" +
                "    firstPlaceProviders <com.abc.D> comma delimited extra Provider classnames to install as first-preference\n" +
                "    timeoutMillis <milliseconds>    number of milliseconds to use for socket timeouts\n" +
                "    startupDelayMillis <millis>     minimum number of milliseconds to delay startup, to allow peer to init\n" +
                "\n" +
                "  server options:\n" +
                "    clientcert <yes|no|optional>    whether to send a client cert challenge, and whether to require client cert\n" +
                "  client options:\n" +
                "    clientcert <yes|no>             whether to present a client cert, if challenged by server\n\n" +
                "Examples:\n" +
                "  TlsProviderTestSuite server tlsprov rsa tlsversions TLSv1,TLSv1.1,TLSv1.2 clientcert optional &\n" +
                "  TlsProviderTestSuite client tlsprov sun tlsversions TLSv1 clientcert no\n");
        System.exit(1);
    }

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

    /*
     * Main entry point
     */
    public static void main(String[] args) throws Exception {
        starttime = System.currentTimeMillis();
        if (args.length < 1) usage();
        Iterator<String> it = Arrays.asList(args).iterator();
        TlsProviderTestSuite suite = new TlsProviderTestSuite(it.next());
        while (it.hasNext()) {
            String field = it.next();
            String value = it.next();
            suite.getClass().getField(field).set(suite, value);
        }
        suite.start();
    }

    private void addProv(String providerClassname, boolean insertAsHighestPreference) throws Exception {
        Provider prov = (Provider) Class.forName(providerClassname).newInstance();
        if (insertAsHighestPreference) {
            Security.insertProviderAt(prov, 1);
        } else {
            Security.addProvider(prov);
        }
    }

    private void addProvs(String provsList, boolean insertAsHighestPreference) throws Exception {
        if (provsList != null && provsList.trim().length() > 0) {
            String[] provs = provsList.trim().split("\\s*\\,\\s*");
            for (String prov : provs) {
                addProv(prov, insertAsHighestPreference);
            }
        }
    }

    private void initLuna(boolean useLuna5) throws Exception {
        String classname = useLuna5
                ? "com.safenetinc.luna.LunaSlotManager"
                : "com.chrysalisits.crypto.LunaTokenManager";
        Class tokenManagerClass = Class.forName(classname);
        Method getInstanceMethod = tokenManagerClass.getMethod("getInstance", new Class[0]);
        Object manager = getInstanceMethod.invoke(null);

        String loginMethodName = useLuna5 ? "login" : "Login";
        Method loginMethod = manager.getClass().getMethod(loginMethodName, new Class[] { String.class });
        loginMethod.invoke(manager, tokenPin);

        String secretKeysMethodName = useLuna5 ? "setSecretKeysExtractable" : "SetSecretKeysExtractable";
        Method secretKeysMethod = manager.getClass().getMethod(secretKeysMethodName, new Class[] { boolean.class });
        secretKeysMethod.invoke(manager, true);
    }

    private void start() throws Exception {
        if (Boolean.valueOf(debug)) System.setProperty("javax.net.debug", "ssl");

        if ("rsa".equals(jceprov)) {
            addProv("com.rsa.jsafe.provider.JsafeJCE", false);
        } else if ("luna4".equals(jceprov)) {
            addProv("com.chrysalisits.cryptox.LunaJCEProvider", true);
            addProv("com.chrysalisits.crypto.LunaJCAProvider", true);
            initLuna(false);
        } else if ("luna5".equals(jceprov)) {
            addProv("com.safenetinc.luna.provider.LunaProvider", true);
            initLuna(true);
        }

        String expectTlsProv = null;
        if ("rsa".equals(tlsprov)) {
            Security.removeProvider("SunJSSE");
            addProv("com.rsa.jsse.JsseProvider", true);
            expectTlsProv = "RsaJsse";
        } else if ("sun".equals(tlsprov)) {
            expectTlsProv = "SunJSSE";
        }

        addProvs(additionalProviders, false);
        addProvs(firstPlaceProviders, true);

        SSLContext sslContext = SSLContext.getInstance("TLS");
        String gotTlsProv = sslContext.getProvider().getName();
        System.out.println("Using TLS provider: " + gotTlsProv);
        if (expectTlsProv != null) assertEquals(expectTlsProv, gotTlsProv);

        boolean ecc = "ecc".equals(certtype);
        X509KeyManager[] km = {new SingleCertX509KeyManager(getCertificate(ecc), getPrivateKey(ecc))};
        if (!server && !"yes".equals(clientcert)) km = null;

        sslContext.init(
                km,
                new X509TrustManager[] { new TrustEverythingTrustManager(createAcceptedIssuersList()) },
                new SecureRandom());

        if (server)
            runServer(sslContext);
        else
            runClient(sslContext);
    }

    private X509Certificate[] createAcceptedIssuersList() throws CertificateException, IOException {
        List<X509Certificate> issuers = new ArrayList<X509Certificate>();

        if (Boolean.valueOf(includeRealIssuers)) {
            issuers.add(getCertificate(false));
            issuers.add(getCertificate(true));
        }

        if (Boolean.valueOf(includeBogusIssuer)) {
            issuers.add(getCertificate(RSA_1536_CERT_X509_B64));
        }

        return issuers.toArray(new X509Certificate[issuers.size()]);
    }

    private void delay() throws InterruptedException {
        if (startupDelayMillis != null && startupDelayMillis.trim().length() > 0) {
            long totalStartupTime = Long.parseLong(startupDelayMillis);
            long startupTime = System.currentTimeMillis() - starttime;
            long delayTime = totalStartupTime - startupTime;
            if (delayTime > 0) {
                System.out.println("Waiting for " + delayTime + " millis to allow peer to settle...");
                Thread.sleep(delayTime);
            }
        }
    }

    private void runServer(SSLContext sslContext) throws IOException, InterruptedException {
        SSLServerSocketFactory ssf = sslContext.getServerSocketFactory();
        SSLServerSocket sock = (SSLServerSocket) ssf.createServerSocket(Integer.parseInt(port), 5, InetAddress.getByName(host));
        if (tlsversions != null) sock.setEnabledProtocols(tlsversions.split(","));
        if (ciphers != null) sock.setEnabledCipherSuites(new String[] {ciphers});
        if ("yes".equals(clientcert)) sock.setNeedClientAuth(true);
        if ("optional".equals(clientcert)) sock.setWantClientAuth(true);
        if ("no".equals(clientcert)) { sock.setNeedClientAuth(false); sock.setWantClientAuth(false); }

        byte buf[] = new byte[testblocklen];
        delay();
        System.out.println("Awaiting connection on " + sock.getLocalSocketAddress());
        sock.setSoTimeout(Integer.parseInt(timeoutMillis));
        SSLSocket s = (SSLSocket) sock.accept();
        System.out.println("Connected: " + s.getSession().getProtocol() + ": " + s.getSession().getCipherSuite());
        assertEquals(testblocklen, s.getInputStream().read(buf));
        String request = new String(buf);
        System.out.println("Read: " + request);
        try {
            System.out.println("Client presented certificate: " + s.getSession().getPeerCertificateChain()[0].getSubjectDN());
        } catch (SSLPeerUnverifiedException e) {
            System.out.println("No client certificate was presented.");
        }
        s.getOutputStream().write(("Echoing request: " + request).getBytes());
        System.out.println("Closing connection");
        s.close();
        System.out.println("Test successful on server side.");
    }

    private void runClient(SSLContext sslContext) throws Exception {
        SSLSocketFactory csf = sslContext.getSocketFactory();
        SSLSocket sock = (SSLSocket) csf.createSocket();
        sock.setSoTimeout(Integer.parseInt(timeoutMillis));
        delay();
        sock.connect(new InetSocketAddress(host, Integer.parseInt(port)));
        if (tlsversions != null) sock.setEnabledProtocols(tlsversions.split(","));
        if (ciphers != null) sock.setEnabledCipherSuites(ciphers.split(","));
        sock.setWantClientAuth("yes".equals(clientcert));

        sock.getOutputStream().write(testblock.getBytes());
        sock.getOutputStream().flush();
        String response = new String(slurp(sock.getInputStream()));
        System.out.println("Got response: " + response);
        System.out.println("Test successful on client side.");
    }
}
