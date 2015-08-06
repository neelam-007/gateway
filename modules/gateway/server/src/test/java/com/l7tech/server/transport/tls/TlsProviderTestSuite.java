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
import java.util.concurrent.*;

/**
 * A suite of tests that can be used to test a variety of SSL scenarios.
 * To use it, start a server in one of the configurations, and then run a client in a different configuration.
 */
public class TlsProviderTestSuite {
    boolean server;
    public String host = "127.0.0.1";
    public String port = "17443";
    public String tlsprov = null;
    public String jceprov = "default";
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
    public String repeatCount = null;
    public String poolSize = "1";
    public String secureRandom = null;

    static long starttime;

    static final String testblock = "Test block of exactly 80 bytes: test test test test test test test test test tes";
    static final int testblocklen = testblock.getBytes().length;

    // A test RSA cert to use as a server or client cert
    public static final String RSA_2048_CERT_X509_B64 =
        "MIIDGDCCAgCgAwIBAgIIB2dtRKOWrgswDQYJKoZIhvcNAQEMBQAwGDEWMBQGA1UEAwwNdGVzdF9y" +
        "c2FfMjA0ODAeFw0wOTEyMTYyMzQxMThaFw0zNDEyMTAyMzQxMThaMBgxFjAUBgNVBAMMDXRlc3Rf" +
        "cnNhXzIwNDgwggEiMA0GCSqGSIb3DQEBAQUAA4IBDwAwggEKAoIBAQCxNW9CQzGqUV38EirNbD0x" +
        "xEVIamH4WGUeCbCYq7np1FWz7uBG137duU7ZOr1oMgkdaCv1l2D8mNc5u6S9pgOptNFSZRodxB9N" +
        "13vl1pV8b+ZmltN7zgEYVNNbK7DegonNgF8qjzol03gU5z1qAAt1txMrDO5yFwSJz5NMk4jCudvA" +
        "3ZCnI++Cm3FtZpssKY6ma41M4i4PPtsatIF+3ao6zqfrXaRQgS8gcONgiGn0PNDJZBJgD6CTlzk2" +
        "kVC2SQts6EILjkXqumvA06XCEe1h3AA0QUROzaNpJ5zmr9502itTEv28kyVrr2aMDgK0QjXb3jlb" +
        "foiY8qSkxivoYRpbAgMBAAGjZjBkMA4GA1UdDwEB/wQEAwIF4DASBgNVHSUBAf8ECDAGBgRVHSUA" +
        "MB0GA1UdDgQWBBRNqj1i9aNTI2d/4CX81FGCuHfvbDAfBgNVHSMEGDAWgBRNqj1i9aNTI2d/4CX8" +
        "1FGCuHfvbDANBgkqhkiG9w0BAQwFAAOCAQEAUKvuuvPdjFFEXcdht4GR76W7++MaTc0QUVegTbDT" +
        "+E3CcAfBWqTi7Q6djRSqAOP6xoDmGUpKX4ef3tDonFJ+nz8HPAkxV//yAVzC7asbfOtMZshrG5YV" +
        "k/WbFzz09YqJbs6JIibMgj6SR7GoYV86YVv80ZT1Zc94AECWid3Thvc1OHzcndRkCBX1b4nkWffX" +
        "cblHP5dXnfISImULWf0R88IEP+G/aLEMfZ5SHWdO5TVgztrXeEnR9Efiz/MSRA8JY4eBywexXjMF" +
        "f0fvJNenDoIY7Dxp3mRqBXf9smKmvF0YqQ29u/l7ZlvF1D2dEhAHqX2yotJISjirZE5VbQKFBw==";

    public static final String RSA_2048_KEY_PKCS8_B64 =
        "MIIEvwIBADANBgkqhkiG9w0BAQEFAASCBKkwggSlAgEAAoIBAQCxNW9CQzGqUV38EirNbD0xxEVI" +
        "amH4WGUeCbCYq7np1FWz7uBG137duU7ZOr1oMgkdaCv1l2D8mNc5u6S9pgOptNFSZRodxB9N13vl" +
        "1pV8b+ZmltN7zgEYVNNbK7DegonNgF8qjzol03gU5z1qAAt1txMrDO5yFwSJz5NMk4jCudvA3ZCn" +
        "I++Cm3FtZpssKY6ma41M4i4PPtsatIF+3ao6zqfrXaRQgS8gcONgiGn0PNDJZBJgD6CTlzk2kVC2" +
        "SQts6EILjkXqumvA06XCEe1h3AA0QUROzaNpJ5zmr9502itTEv28kyVrr2aMDgK0QjXb3jlbfoiY" +
        "8qSkxivoYRpbAgMBAAECggEAKAVqYCuuvslrkW9E7WnhlCjAgO9NuvmztIn5sTEvZqjGxkFPs5Ad" +
        "ndOpBBRpDGwodNS/ANM0Wzfle6tuNEqXDy/ACny83jYZ38mnuKuyzQy0mzy6/H3071rQ9Qk0A7y1" +
        "hIzWcVUxi7NrrkTfMN5buDUlhhaAj5G1O65+lM/JFjjlEH2L6rwfcPAiyD1dyDzjXd4k8acv0JPD" +
        "JANJ2hLjXnOHFDR/cCxCOysdGHQuH9aMIdeNz0JGyiWLD6SP+9va0G49DwSG7xL7bdnbDC812XUo" +
        "wqVhksg4yjRkvL3ZExG25XQwkdJ01QU4atz4QJdqYVy/X/Vt7SG1kJoyMDWKgQKBgQDiIaxbkDnf" +
        "Vr1aDZMUZ3/dKWePclbSqQoUExgfidhYoJn92j+rNbLoIseyBe8nBMeBuLSsyFQJU81ZdvpRaGMH" +
        "jltef/cGY7DZtoPx/WpKjIJxGH0Hk3/kJMWxhcar0eXyuXDj+rNGy6wp7yZFnZlr70SRsD1AxdaE" +
        "x400Vl8RmwKBgQDInYEBMj3TRUhqWspgXEB4cy67zaOh+LYxJTbkLGPi5rm/tX1QxGZJNvT/RZRA" +
        "vQuy/tdJqaZrZWtP/z8AQ/2Ya7zcHu0oqz1YrAIG7WH75ZupOp7SaNbcZ1ojHyQTtmYDTs8ErFK0" +
        "YTEzylpOYjKAQ4uvKcv/XIiuIv/OlrwGQQKBgQDfUqgYiVhONCiujedqaEjDz0dCSIZsZ5rXdoAF" +
        "baom5P0P0gG9ATxduzOCog+sdjDd8N8mIHW1/Hg52aGe0juy06lyq2f3fG7EpFasnzvgweF09d1M" +
        "pSPR2WsQRfCN8a5pxzAxRn7U9QJjK5ade+ZvzQ3n36iulnOkEDtoq8AZ7wKBgQCm6AuVcDRp2sGV" +
        "4rVvGDF3RPVDwKH8Nw11s+2IRrpP4//0VM3O7afgD/4jh8MBXYcnQ8jf+2p+/aEbrFPBJ9AMCM7X" +
        "IE/VvypJ5MnG86bKyUwJrsDGc/0W4FHo2JbOY7lZ1S59R9WDRz2FRjx97Erx1cCYWiDj8xuwLWRA" +
        "f74tQQKBgQCfGlni4CM9YerW+YI8MRbWSydvXPZ3uirRUVmXiODp8XoUbzKeohg4dDw4Ns0VykRJ" +
        "sUTOApGOlL1pxHoTUoxwLLrVa580+RZeDt9b4Q4j9ZuMNbndNUQOqcifv7hHEQTPXEqgTx3i4t6p" +
        "sOX8p9A1peKQeUL75mr1HwLYi+NHig==";

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

    public static final String EC_secp384r1_CERT_X509_B64 =
        "MIIB0zCCAVigAwIBAgIJAKnrAXajzLwoMAoGCCqGSM49BAMDMBwxGjAYBgNVBAMMEXRlc3RfZWNf" +
        "c2VjcDM4NHIxMB4XDTA5MTIxNjIzNDEyMFoXDTM0MTIxMDIzNDEyMFowHDEaMBgGA1UEAwwRdGVz" +
        "dF9lY19zZWNwMzg0cjEwdjAQBgcqhkjOPQIBBgUrgQQAIgNiAASp92Vv7qoY0+khqH8o3AY8zO17" +
        "oIc2a9cdsvEqhUET4gmVuEmzjumtXKsNuichgF7v8D7fjcYYY8+df+bOUcoFduRKWMfIZFFThTKG" +
        "PjyK3ZPNhmEVeOpTqCV43JoHNq+jZjBkMA4GA1UdDwEB/wQEAwIF4DASBgNVHSUBAf8ECDAGBgRV" +
        "HSUAMB0GA1UdDgQWBBQ+sq0AVeZtKAcjXIXICsqVoC+MeTAfBgNVHSMEGDAWgBQ+sq0AVeZtKAcj" +
        "XIXICsqVoC+MeTAKBggqhkjOPQQDAwNpADBlAjB6WrVTujB/QSFW7DUeLn1Cq2BWRFdqEvO7gIrb" +
        "+Wc63Z4kUCYAzyvbsbY/hEM6IokCMQC3YWsaZ7uBe94fsDWv5Q4lyPpYyDRgf/7Tb2s+6E/wyckE" +
        "MKAwqiaw+a36GodQWvMA";

    public static final String EC_secp384r1_KEY_PKCS8_B64 =
        "ME4CAQAwEAYHKoZIzj0CAQYFK4EEACIENzA1AgEBBDAhyd5V2T6UYn8t8G4Lj1qaICzJAxNNheJT" +
        "Z1cvDsRYKxdMmcmMGh6Yl5slgqjUXZE=";

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

    static PrivateKey getPrivateKey(String name) throws GeneralSecurityException, IOException {
        if ("rsa".equals(name)) {
            return getPrivateKey(false, RSA_2048_KEY_PKCS8_B64);
        } else if ("ecc".equals(name)) {
            return getPrivateKey(true, EC_P256_PRIVATE_KEY);
        } else if ("ecc2".equals(name)) {
            return getPrivateKey(true, EC_secp384r1_KEY_PKCS8_B64);
        } else {
            throw new IllegalArgumentException("No private key named " + name);
        }
    }

    static PrivateKey getPrivateKey(boolean ecc, String b64) throws InvalidKeySpecException, NoSuchAlgorithmException, IOException {
        return KeyFactory.getInstance(ecc ? "EC" : "RSA").generatePrivate(new PKCS8EncodedKeySpec(new BASE64Decoder().decodeBuffer(b64)));
    }

    static X509Certificate getCertificate(String name) throws CertificateException, IOException {
        if ("rsa".equals(name)) {
            return getCertificateFromB64(RSA_2048_CERT_X509_B64);
        } else if ("ecc".equals(name)) {
            return getCertificateFromB64(EC_P256_CERT);
        } else if ("ecc2".equals(name)) {
            return getCertificateFromB64(EC_secp384r1_CERT_X509_B64);
        } else {
            throw new IllegalArgumentException("No cert named " + name);
        }
    }

    static X509Certificate getCertificateFromB64(String b64) throws CertificateException, IOException {
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
                "  TlsProviderTestSuite server tlsprov rsa tlsversions TLSv1,TLSv1.1,TLSv1.2 certtype ecc clientcert optional &\n" +
                "  TlsProviderTestSuite client tlsprov sun tlsversions TLSv1 certtype rsa clientcert no\n");
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

    private void initCryptoJFipsMode() throws Exception {
        Class cryptojClass = Class.forName("com.rsa.jsafe.crypto.CryptoJ");
        Method setModeMethod = cryptojClass.getMethod("setMode", int.class);
        int fips140SslEcc = (Integer)cryptojClass.getField("FIPS140_SSL_ECC_MODE").get(null);
        setModeMethod.invoke(null, fips140SslEcc);
    }

    private void start() throws Exception {
        if (Boolean.valueOf(debug)) System.setProperty( "javax.net.debug", "ssl" );

        if ("rsa".equals(jceprov)) {
            addProv("com.rsa.jsafe.provider.JsafeJCE", false);
        } else if ("rsafips".equals(jceprov)) {
            addProv("com.rsa.jsafe.provider.JsafeJCE", true);
            initCryptoJFipsMode();
        } else if ("luna4".equals(jceprov)) {
            addProv("com.chrysalisits.cryptox.LunaJCEProvider", true);
            addProv("com.chrysalisits.crypto.LunaJCAProvider", true);
            initLuna(false);
        } else if ("luna5".equals(jceprov)) {
            addProv("com.safenetinc.luna.provider.LunaProvider", true);
            initLuna(true);
        } else if ("bc".equals(jceprov)) {
            addProv("org.bouncycastle.jce.provider.BouncyCastleProvider", false);
        } else if ("sun".equals(jceprov) || "default".equals(jceprov)) {
            // Do nothing
        } else {
            throw new IllegalArgumentException("Unknown jceprov: " + jceprov);
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

        X509KeyManager[] km = {new SingleCertX509KeyManager(getCertificate(certtype), getPrivateKey(certtype))};
        if (!server && !"yes".equals(clientcert)) km = null;

        SecureRandom secRand = secureRandom != null ? SecureRandom.getInstance(secureRandom) : new SecureRandom();
        sslContext.init(
                km,
                new X509TrustManager[] { new TrustEverythingTrustManager(createAcceptedIssuersList()) },
                secRand);

        System.out.println("using SecureRandom algorithm: " + secRand.getAlgorithm());

        if (server)
            runServer(sslContext);
        else
            runClient(sslContext);
    }

    private X509Certificate[] createAcceptedIssuersList() throws CertificateException, IOException {
        List<X509Certificate> issuers = new ArrayList<X509Certificate>();

        if (Boolean.valueOf(includeRealIssuers)) {
            issuers.add(getCertificate("rsa"));
            issuers.add(getCertificate("ecc"));
            issuers.add(getCertificate("ecc2"));
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

        int runsLeft = repeatCount == null ? 1 : Integer.parseInt(repeatCount);
        final boolean sout = runsLeft < 2;
        ExecutorService executorService = makePool();

        if (sout) System.out.println("Awaiting connections on " + sock.getLocalSocketAddress());
        sock.setSoTimeout(Integer.parseInt(timeoutMillis));

        while (--runsLeft >= 0) {
            final SSLSocket s = (SSLSocket) sock.accept();
            executorService.submit(new Runnable() {
                @Override
                public void run() {
                    try {
                        handleRequest(sout, s);
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                }
            });
        }

        shutdownExecutor(executorService);
    }

    private ExecutorService makePool() {
        BlockingQueue<Runnable> mutationQueue = new ArrayBlockingQueue<Runnable>(100, false);
        int poolSizeInt = Integer.parseInt(poolSize);
        return new ThreadPoolExecutor(poolSizeInt, poolSizeInt, 5 * 60, TimeUnit.SECONDS, mutationQueue, new ThreadPoolExecutor.CallerRunsPolicy());
    }

    private void handleRequest(boolean sout, SSLSocket s) throws IOException {
        try {
            final byte buf[] = new byte[testblocklen];
            System.out.println("Connected: " + s.getSession().getProtocol() + ": " + s.getSession().getCipherSuite());
            s.getSession().invalidate();
            assertEquals(testblocklen, s.getInputStream().read(buf));
            String request = new String(buf);
            if (sout) System.out.println("Read: " + request);
            try {
                if (sout) System.out.println("Client presented certificate: " + s.getSession().getPeerCertificateChain()[0].getSubjectDN());
            } catch (SSLPeerUnverifiedException e) {
                System.out.println("No client certificate was presented.");
            }
            s.getOutputStream().write(("Echoing request: " + request).getBytes());
            if (sout) System.out.println("Closing connection");
            s.close();
            if (sout) System.out.println("Test successful on server side.");
        } finally {
            s.close();
        }
    }

    private void runClient(SSLContext sslContext) throws Exception {
        int runsLeft = repeatCount == null ? 1 : Integer.parseInt(repeatCount);
        int totalRuns = runsLeft;
        final boolean sout = runsLeft < 2;

        ExecutorService executorService = makePool();
        final SSLSocketFactory csf = sslContext.getSocketFactory();

        long startTime = System.currentTimeMillis();

        while (--runsLeft >= 0) {
            executorService.submit(new Runnable() {
                @Override
                public void run() {
                    try {
                        sendRequest(sout, csf);
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }
                }
            });
        }

        shutdownExecutor(executorService);

        long endTime = System.currentTimeMillis();

        long totalTime = endTime - startTime;
        System.out.println("Total time: " + totalTime + " ms (" + (totalTime/totalRuns) + " ms/req)");
    }

    private void shutdownExecutor(ExecutorService executorService) throws InterruptedException {
        executorService.shutdown();
        if (!executorService.awaitTermination(120, TimeUnit.SECONDS)) {
            System.err.println("Timed out awaiting executor shutdown");
            System.exit(1);
        }
    }

    private void sendRequest(boolean sout, SSLSocketFactory csf) throws IOException, InterruptedException {
        SSLSocket sock = (SSLSocket) csf.createSocket();
        try {
            sock.setSoTimeout(Integer.parseInt(timeoutMillis));
            if (sout) System.out.println("Delaying");
            delay();
            if (sout) System.out.println("Connecting to server " + host);
            sock.connect(new InetSocketAddress(host, Integer.parseInt(port)));
            if (tlsversions != null) sock.setEnabledProtocols(tlsversions.split(","));
            if (ciphers != null) sock.setEnabledCipherSuites(ciphers.split(","));
            sock.setWantClientAuth("yes".equals(clientcert));

            sock.getOutputStream().write(testblock.getBytes());
            sock.getOutputStream().flush();
            String response = new String(slurp(sock.getInputStream()));
            sock.getSession().invalidate();
            if (sout) System.out.println("Got response: " + response);
            if (sout) System.out.println("Test successful on client side.");
        } finally {
            sock.close();
        }
    }
}
