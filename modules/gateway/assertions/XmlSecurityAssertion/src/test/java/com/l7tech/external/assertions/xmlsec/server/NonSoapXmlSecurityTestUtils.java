package com.l7tech.external.assertions.xmlsec.server;

import com.l7tech.common.TestKeys;
import com.l7tech.gateway.common.security.keystore.SsgKeyEntry;
import com.l7tech.objectmodel.Goid;
import com.l7tech.security.cert.TestCertificateGenerator;
import com.l7tech.security.prov.JceProvider;
import com.l7tech.security.xml.SimpleSecurityTokenResolver;
import com.l7tech.server.TestDefaultKey;
import com.l7tech.server.identity.cert.TestTrustedCertManager;
import com.l7tech.server.identity.cert.TrustedCertCache;
import com.l7tech.server.security.keystore.SsgKeyFinderStub;
import com.l7tech.server.security.keystore.SsgKeyStoreManager;
import com.l7tech.server.security.keystore.SsgKeyStoreManagerStub;
import com.l7tech.util.Pair;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.cert.X509Certificate;
import java.util.Arrays;

/**
 *
 */
public class NonSoapXmlSecurityTestUtils {
    public static final String TEST_KEY_ALIAS = "test";
    public static final String DATA_KEY_ALIAS = "data";
    public static final String ECDSA_KEY_ALIAS = "ecdsa";
    public static final String TEST_RSA_1024_ALIAS = "test_rsa_1024";

    /**
     * @return the test key from TEST_KEYSTORE.
     */
    public static SsgKeyEntry getTestKey() throws IOException, GeneralSecurityException {
        final Pair<X509Certificate,PrivateKey> k = TestCertificateGenerator.convertFromBase64Pkcs12(TEST_KEYSTORE);
        return new SsgKeyEntry(new Goid(0,99), TEST_KEY_ALIAS, new X509Certificate[] { k.left }, k.right);
    }

    /**
     * @return the test key from DATA_KEYSTORE.
     */
    public static SsgKeyEntry getDataKey() throws IOException, GeneralSecurityException {
        final Pair<X509Certificate,PrivateKey> k = TestCertificateGenerator.convertFromBase64Pkcs12(DATA_KEYSTORE);
        return new SsgKeyEntry(new Goid(0,99), DATA_KEY_ALIAS, new X509Certificate[] { k.left }, k.right);
    }

    /**
     * @return the test key from TestKeys.getCertAndKey("RSA_1024")
     */
    public static SsgKeyEntry getTestRsa1024Key() {
        Pair<X509Certificate, PrivateKey> k = TestKeys.getCertAndKey("RSA_1024");
        return new SsgKeyEntry(new Goid(0,99), TEST_RSA_1024_ALIAS, new X509Certificate[] { k.left }, k.right);
    }

    public static SsgKeyEntry getEcdsaKey() throws IOException, GeneralSecurityException {
        ensureEcProviderAvailable();
        Pair<X509Certificate, PrivateKey> k = TestCertificateGenerator.convertFromBase64Pkcs12(ECDSA_KEYSTORE);
        return new SsgKeyEntry(new Goid(0,99), ECDSA_KEY_ALIAS, new X509Certificate[] { k.left }, k.right);
    }

    private static void ensureEcProviderAvailable() {
        try {
            KeyFactory.getInstance("EC");
        } catch (NoSuchAlgorithmException e) {
            JceProvider.init();
        }
    }

    /**
     * @return a SecurityTokenResolver that will resolve the test keys known to this class.
     */
    public static SimpleSecurityTokenResolver makeSecurityTokenResolver() throws IOException, GeneralSecurityException {
        return new SimpleSecurityTokenResolver(null, getAllKeys());
    }

    /**
     * @return An SsgKeyStoreManager that will provide access to the test keys known to this class.
     */
    public static SsgKeyStoreManager makeSsgKeyStoreManager() throws IOException, GeneralSecurityException {
        return new SsgKeyStoreManagerStub(new SsgKeyFinderStub(Arrays.asList(getAllKeys())));
    }

    /**
     * @return a Trusted Cert Cache object containing all cached certificates.
     */
    public static TrustedCertCache makeTrustedCertCache() throws Exception {
        return new TestTrustedCertManager(new TestDefaultKey());
    }

    /**
     * @return An array of all known test keys.
     */
    public static SsgKeyEntry[] getAllKeys() throws IOException, GeneralSecurityException {
        return new SsgKeyEntry[] {
                getTestKey(),
                getDataKey(),
                getEcdsaKey(),
                getTestRsa1024Key()
        };
    }

    public static final String TEST_KEYSTORE =
            "MIIGVgIBAzCCBhAGCSqGSIb3DQEHAaCCBgEEggX9MIIF+TCCAx4GCSqGSIb3DQEHAaCCAw8EggML\n" +
            "MIIDBzCCAwMGCyqGSIb3DQEMCgECoIICsjCCAq4wKAYKKoZIhvcNAQwBAzAaBBQaj46rsqxvGtdc\n" +
            "F4A6PViGXJOoPgICBAAEggKAliSccagp7F+1Ifv0z7DgVOtIuuQ1M/i+g+Hdcg+8novUPMfBoWES\n" +
            "eCSZ/9qgINHvmFioMzoWKcMmUF0qPlB/gRRNq6NW5o+LxPLmpQkFACMkp2sp3mbkqeY6lvhuqxh4\n" +
            "EM1q4gAhDgmLrFLg3+m6qO+HiHcnXq+gNMc9LcQKubOaw1lYerskMdRIQLAdL68orbo+1o+EKqXS\n" +
            "JPRekRHPVkEro12r1R0xTVXzGY6L5apuyP4+dEVnSV+xZWHNaHS5mMl/2TeH08Vp9HFNQpQFU1oP\n" +
            "XSsXFcAP6rycBrw/nwujz8lFucNcEG9wBsmXScYs6Mfjz58iHj5MEwfeIu5mmN3tPAwOq74zN7Yn\n" +
            "sJ4mp+E7cJCkLq252EP0YbOm/KmwEqpdSMSD8M59BVRTbMG6tF4NHBZjpf2OeYAfo4aDQsYzRdh7\n" +
            "mcBJfokHBo3VExqHwgAtolYRDx+3xAcABvZYibLrUYqKF7m6dQM18UpFGbe3vtfDazWAk+CalNXc\n" +
            "GlyaL8F/8kKM5tdNwEeJDCZ0UjQmg1QOusWfdBWzN7+wYhJ52GKMtO0Jj3v3HeYCv9KUoe3jpXmD\n" +
            "x24Q/ZahE5jQ4sXL4H1Ul598SMA2hPz4U3PxlQWKRA9JK6r38Shg8j+RQdIcvuZHHrYHy3tXS7h0\n" +
            "LWcjbn9PsN9/Go1LWqXOHkFB8NgwRgQV6tTLAVug9vZiz4E8nsKzvlr9aOyJEmk0w6Z+fBwdYIIB\n" +
            "0fv2G8NJwZl6DDwT/22P1MMnYqRr7BZ6K3jvBKSnm6Vn+6BYuxB7DIxMnZxIYxhSSx7e31wq/Wz0\n" +
            "MvbC3LEpZQgDMqVd97QcJdFDO5OJnd9hxig+UswKbjE+MBkGCSqGSIb3DQEJFDEMHgoAZQBuAHQA\n" +
            "cgB5MCEGCSqGSIb3DQEJFTEUBBJUaW1lIDEyNDgyMzI2NDkyMzIwggLTBgkqhkiG9w0BBwagggLE\n" +
            "MIICwAIBADCCArkGCSqGSIb3DQEHATAoBgoqhkiG9w0BDAEGMBoEFFrUmi4ha436gkKpymSinqVr\n" +
            "DOiiAgIEAICCAoBeuxKc8iWN3aswqQpHWq9n657XqqpRpbTby5ASJSR3M3+YIQ4AJ6bhY4BIVTto\n" +
            "es5GFtTLH5l8VYbhNCHRh5gSmqKFIml5E2zqPkkXCTIgH+0sb1IkD6pjNs05n3vQkd4N/pMECueI\n" +
            "h8X04ojWgJE0/1RvVJrolPX7za1OhQu9T/kw6dBEqsKcG8Ik5tosnG/UUqX0Z02cnetX7aMEzRfv\n" +
            "DJLEEdP9SxQzC0aCj9CIPcVMWYLEBpg8fjESo/GC2OZQ9bI7t3CiSYpzqLA3Y1s+VOa1i+93+NE2\n" +
            "ncjejXzl5OKm/dA6pAu0qTjwjMEyLXFzT2y6W891x5a5cQocsUmQhNJcF2QRnt71pZDF5/XDRfD0\n" +
            "KcraNYqRpWuxyGSnXdbkUVCMbvuyvOq28lN037SOfvAqKWAGACur4hLZt+gd07UDVKfUXY9xKwgD\n" +
            "scux5ZdCE0l/FIYSLrw/fuj8zf71/6cEVCpf3ARsG9f3nx3ApNvDa7cqPfU2T3TfjPrcikmcVj0l\n" +
            "mADN/kP324yGGfzw9OWLJaF4uF3USIgYy2ljg0dhVfcpnxacZAWW8HNP59ISqs25+Rb4pTqd1+CX\n" +
            "tcAgfP2+Lr1QRxyNXpe7v/6TMuCbGQ8Wpf6fONXjXEg1pRj7G0OgTS77k5IWdnPTA8Lb4aaFTlha\n" +
            "JeykDheCjbPUBlhEktRjgSLHmkzR4cBT47Gfu0XJyT+NQw+ppBGnf9Bb5WlANNjSIO/yJXIp1Xks\n" +
            "3Xr00G9ELzHVkv4AIr/A5WBtQnutuExLJ4wg2PRlQrRTamuq6Vh0sZFxpaE6lRGPD94DIB4vMhsq\n" +
            "Wno7Ntev5R0t2yJ7AKLK9da73tcxT0j+MD0wITAJBgUrDgMCGgUABBSnAGLsWZ+fsDZQznWusnp8\n" +
            "/zG5ZwQUHITgA39NzDQicvtCtoyVlDfEF7gCAgQA";

    public static final String DATA_KEYSTORE =
            "MIACAQMwgAYJKoZIhvcNAQcBoIAkgASCA+gwgDCABgkqhkiG9w0BBwGggCSABIIDCTCCAwUwggMB\n" +
            "BgsqhkiG9w0BDAoBAqCCArIwggKuMCgGCiqGSIb3DQEMAQMwGgQUg/8v7fUN0dEZxeU8zekx+evu\n" +
            "rfMCAgQABIICgBmDuRvh05wdJ93ia4jBe0UlwDtx7PzJRl5bGY3HuekP7ScwEItKPULNAkCSAaWe\n" +
            "V4PfsptD/isgKOjxbzN0fdHSPedmqdSL51X/XlMl37v7lCJ+8FAf5BBxgxa53ffyJUMtXYxNr484\n" +
            "6qKcwdMX8osUyeTLBqkB0VAbgHzFm2Z40Q675wlZ+N3afvbybFM9tBzODym/T/SBe1DHNSRy729U\n" +
            "5mVpwgPhoD44T2vhPE4NNMY2oRPjzlWvnj+s7mMOqR4ZPY6tEeiy6YA/5Y6+KsGCosbOFemUMEHD\n" +
            "vXwoXviKwbZD9Ldia3RDc6mzsttYBaoA2vBwHAgDY3iqay2k8+B+fael924s4BrirMfWoslcH8ME\n" +
            "CI/yEqPvq/BhNz0VshYQIo2ClYlICrPeBxG/eS2QuaOhEHr843fFjg9u6oXhNHaMDHmP0uS1eLpm\n" +
            "chbCf8QxbhznoSrqr4R3/92/eWb5zRIiIUxpzuj/sbSOiikDw/N1zgLCqwMgKsuZwHfdiN2/v5Eh\n" +
            "clAPLy1N1Xhjn/NNU1fILCsXBcTJ5pSAiiBfjGUKelb4cahvZoUMrVxt/9kEM5JqWqzQI3cEqgWW\n" +
            "NL44cNMiEpRVrwJSlHgxAFfGA1CrJbAFMazdzjuLx9SpocYYJtJiCBTiZz6oxD0UsEQTjWUp6w/5\n" +
            "YL3JoAMehQbtcQU0sLauJWXJERKCWs9/6qkb7fLiyPTdo0VRnKpArbJ2u33MMy8117VX0yDPLDN8\n" +
            "SK/R0MITd1CgRhBi3iGOT/re9Dhfo0brWXnRi6ytuqg+CiM29Mu+uPeae0weE5lGHNT5XyWU/leO\n" +
            "7WI61aEIE4umddeXCZciZ99u4eIJOU8xPDAVBgkqhkiG9w0BCRQxCB4GAHMAcwBsMCMGCSqGSIb3\n" +
            "DQEJFTEWBBRCSBdXGhmyJ33I3tjg158bM57PsgAAAAAAADCABgkqhkiG9w0BBwaggDCAAgEAMIAG\n" +
            "CSqGSIb3DQEHATAoBgoqhkiG9w0BDAEGMBoEFANSVdxVsXfz2+DmmHbMUsbpQ5dXAgIEAKCABIIC\n" +
            "aOQs9S/QyR0bqIcj5UaFhGzZp+OvpH3NTy8K3xQr/b3kDEYw9h5suLmq7v9uHI6r/lkifXGuOnAr\n" +
            "ot6WJvz04tgzJrWAB/k2R/rrZrnua2eIhBJ3yYqj57CZvFjcsnd8GTqbl2bnMqdmeQh48wYM/fOk\n" +
            "BIICA+LSXy2XkRoi7yiJXcsEAgIdHT6IF9qeFGwn0BFI3GKkCtGsGVCSYohCFvG6hCH/deskSxp6\n" +
            "NerYrenOs+97MVImMBewiwJ5g7CV176yhuWWCG8A4rkCqJLbTLLMMO2oiLfdLsysQ0C0sPWB6Mzs\n" +
            "EtBW4CasSdQD14UDVR/xIjdXNO44YwqyXqoRnGyQWNgXQNwusGZHsCJ70VpVar61wcaAGQ+HVjim\n" +
            "h7j7+nE03wj4ZuymKqq375gALgmq3Sr0CrT4bF9578GMp7ENcuYi32mhuiqSMJuxfuLYRGz/9sRt\n" +
            "5vZr1kOFXrx1ckgkxVrASHP7UERxVg8VJix9yldBjYiQA3/tg7RhtWzYcS3iSh4VC2sGGtvtR/K2\n" +
            "AE8vga8yAP4QY5XachMyr1BwTy/fgM/3X4hT3sziS2nqDnJz/zoPRbN2e+IRaaH9MQzyfzKikzoW\n" +
            "UubzoqJ0SYNqmZsgwitPbSMNfLB/qMQ5UqjXqoabljAdhSgyRzcQFtY0uCvYTHzTpF5uX0xqNxQ8\n" +
            "LG+Oy4/Dl0PKCG6Q9WXjgc4qtQ1nkSFp+EKbXEjlqjxuFqm9vlVGbMIYV8hSQkp4Zvf/Z7tubbNE\n" +
            "AeZ8iutWsWGrl+L74sec9bVuOrUBihLtxe2Hl9ukn8VYbDVOmrqu2Y26Npiw8fzP/zRXAAAAAAAA\n" +
            "AAAAAAAAAAAAAAAAMD0wITAJBgUrDgMCGgUABBQ3T/1UECawLciqzMgBw4qafQrElAQUiqH1WtTr\n" +
            "OSLH0cErDr/i3Gx8Wq4CAgQAAAA=";

    public static final String ECDSA_KEYSTORE =
            "MIACAQMwgAYJKoZIhvcNAQcBoIAkgASCA5AwgDCABgkqhkiG9w0BBwGggCSABIHfMIHcMIHZBgsq\n" +
            "hkiG9w0BDAoBAqCBhzCBhDAoBgoqhkiG9w0BDAEDMBoEFEaLiGWnOVlBG13a5lgLb9iKoGfpAgIE\n" +
            "AARYioJ8HLiWJH61SPCuJ8kA1c0ChWabOaqZwRi446HEvfDsoGzSgSEkUo5JPx3rlNWtZ8FBi027\n" +
            "ZVvwJJjuIF37iWn2NPjHes4XBUyzsCRs/YvCPbNxuMYlkTFAMBkGCSqGSIb3DQEJFDEMHgoAZQBu\n" +
            "AHQAcgB5MCMGCSqGSIb3DQEJFTEWBBSxICMdkP6HesLjcsKr//Q+yU4vvAAAAAAAADCABgkqhkiG\n" +
            "9w0BBwaggDCAAgEAMIAGCSqGSIb3DQEHATAoBgoqhkiG9w0BDAEGMBoEFJD4MwrykVAVNdy+NzDO\n" +
            "cTf4SJU8AgIEAKCABIICOKlmXp+RjSTMYaCm1jX2CvyXlST/PGPMPFSrxkAA7lKMXzTWxqr5FJbb\n" +
            "7fRZwe1KQnmBudlAhmRgcrwBm8Y6xWxe2zaLJn02vfYeSGdeYCw+PWOdJvAy9S+9AmuYnBx14aPg\n" +
            "w6GxMWO3JiyuLEallWkipyph+L5f0uhTJqIX2M2XIZCOi3SVU8+dl55yAEKNqg3KV1wwbWGb0XY7\n" +
            "URkiCjC4QFU+CbWLGNnmSGLYYx3rrWqm6PlWPefFwX6xixfKqlhqj1TrRAI1fbZG/mlD8MXsUyRM\n" +
            "JY3T5gLjwHC7yQ30+GoD4Zko5XnyrPE0i19ewKWbPDAyn7gzGmU6zTToEI76lAmpvYaaNW2YlCQ1\n" +
            "YcfCl6Qa63sA3v4lBVZeVHXSPJQzBe2wCJqoCicFkkGG04cgEq2HQbVyoAn4Ktyx++RudKomv5nV\n" +
            "uGEwhtDjhumN/3k6O3rQx2NFGE2+HV1cvsWPdoDAHih9wzkROAthT+Mm0yu3t1uvXTxWG6/VbQtl\n" +
            "XSvLmwkDnwFNH56k7PYZqquR2x9yFHaAWl/KTDz+JudkfW4J1J7gXzqNFUqD/HPZb6W3P5s18e+S\n" +
            "EH4SPJmX4+XfS+bIu8t5Z3/CUeJaC5m06FhOgxOVEMlfxilbRR79QEASEdlgQGMC3klBfdpgcmC8\n" +
            "7FBfEtdrM0IEvhTTwNtTcNR59yJNxlSWe42h+vymnQIENHsJKwyGCMMSzIfwM1ifr2Rcx5idmfr/\n" +
            "rU4tlsO4XyLjw7/a9/cAAAAAAAAAAAAAAAAAAAAAAAAwPTAhMAkGBSsOAwIaBQAEFPQl9JNmRzlF\n" +
            "RRGWMnrqUwFidZd6BBRPEe/kyB/G2Y03G5UkfyCEBiH5yAICBAAAAA==";
}
