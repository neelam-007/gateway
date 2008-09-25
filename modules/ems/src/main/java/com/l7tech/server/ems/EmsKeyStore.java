package com.l7tech.server.ems;

import com.l7tech.security.xml.SignerInfo;
import com.l7tech.util.HexUtils;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;

/**
 * Bean that holds the EMS SSL certificate and private key.
 * <p/>
 * For now, this just hardcodes the WSS interop "Alice" certificate.
 */
public class EmsKeyStore {
    private final SignerInfo sslKey;

    private static final String ALICE_KEYSTORE =
            "MIIHCQIBAzCCBs8GCSqGSIb3DQEHAaCCBsAEgga8MIIGuDCCA7cGCSqGSIb3DQEHBqCCA6gwggOk\n" +
                    "AgEAMIIDnQYJKoZIhvcNAQcBMBwGCiqGSIb3DQEMAQYwDgQINHgOt2kfZEECAggAgIIDcLWTu9wq\n" +
                    "v9aA3uQrUVjbhNEm3xUd+nDmgNqSHvvCgCrxXCcNUZeZj6R2BaXI17AN/KGdGr9QupaRsbMJj0WD\n" +
                    "/kFiEPAwBgDEc5qZHaU1qiQ+vi/tfi7ILANqLNqZbB58qUkokA2oi7TE4gVS6icdliBvC87tvvOh\n" +
                    "IbZNglnKhc9kKoxfB4JYbmS3kfYW79wtrccfv1A6vhKvs1Lk6+OmvXgLH+34+D3CvU69QxGEhN1j\n" +
                    "iNyboZDs8ggSFgbPMos/s/acQRZFJq1LHGoKwGFIdUccbQSHhnMvw15NqJqAJXUmvGtPTV7j5JQu\n" +
                    "A1IcRPgEniteBuwuSIi95XT1c30UOwFQFNlR25y/uQX/qXZhsWmuuUAryxruH3JOvO0j3wrLpmo1\n" +
                    "pb46YsUyf1gIUq2JE2nqlW4ZPpczERqwM3R8HdtMzc2ASue/2OwXZG8yEQfS9nsJi2ydVgpcoQcZ\n" +
                    "aWp5V1RzSpGrjCAJ7h8v2WjtQG2FWS1PtpwtSmFYmLoHeFlGqGRKERlBnYHOpS6ZmdGJBQbrc1bn\n" +
                    "LjO645ZWMASQ+jK053PddX9uXDllWSsFAnzXs92An7qFlttwMkc4CTBW291dqrrGtgokNx5OXDgA\n" +
                    "fZEeIJDdWWpBdnkNHDpqqdhAnVErWk9Uyqg7ozRu+3TUyoxzOSwb4er5qUY7+XDJdzgxRlb4Kfhm\n" +
                    "sfcOqF0gIy2KkFm/loK0mnWIHEE/ifRNJdpFajhdiME79NZidVtfCF6q7PGW9YjefKJlTdMCHP7j\n" +
                    "uxDedtCLX5XOB+svI7yLdvAJc0xVHEQCBoUY1iOl+0VVoJq34y3vOrx1gnmnIzZMMjvbUeHgGK+1\n" +
                    "MJTu4SDcyyBCmXgJ+OzSk6fwuoYVBr5Q+8D6tlCF+DVdhXak0bFO/nG1pzgqJLZFpYG+BmhyuZbR\n" +
                    "kLCN9ztNGyph5dAEElR4PuwViCCIUeaVIXDdG5BZpBb6Ppsfgqcn0ZKOJUio7vAVlHhMV//VQiPJ\n" +
                    "BMyzm85f1kHFtt75LiC+AsO7+FPuPAfZsLrtCvd15/qublEBjwKm17pcYcbT65NenBhxmydzytLv\n" +
                    "pz5tXJmdoSBKYadPoOsW+VppeoNgWV6fH60klTC9H2P7EHRHSHwdr3UiumLMWAAFAUBg/b+uQW2Q\n" +
                    "XUZxdsXTjs42p7uErCsvyX8vN70wggL5BgkqhkiG9w0BBwGgggLqBIIC5jCCAuIwggLeBgsqhkiG\n" +
                    "9w0BDAoBAqCCAqYwggKiMBwGCiqGSIb3DQEMAQMwDgQIchZ5VKrvKvYCAggABIICgEiF2+GfTvxi\n" +
                    "vnKZvHGE4FNAxPq3bykAB2+UUYmsFNOoX2pJjOuism2iOlJn7ATvKv9/3Ap1I3nuwh5CF3wNvwMT\n" +
                    "A9q5ppoOEdXfjaBrQ06Tx/upgEfNhU6FLOEsYf8ltUubkz2cqyw/Z+PjhgDoYd789es4kRZiPAyw\n" +
                    "zexLnQi5xr3IjWdP3wyqpI3CTV5wIBOFD8OROynmOhXA4OYBBnVj3Gmz5p3NRqiwgDq9pxBKby/i\n" +
                    "yiZPFu4gv2d3l0a90jOfCOS0IP5Xtgu7GPyyEjdU3fYLiDexzxpTljtI5313pizjPHO/ouqbw6uY\n" +
                    "knB5HiCq6bR645u+eFMQ9zJ3yftO+JX4IYPAnpjZeNPaI+BU4Oj7D7v18dfLehtF6vwPqOOLLjo4\n" +
                    "ZuiPCXAzjH+R0gNdte800JGc547zO4JjCqgCkr2igziaKPPWieeAc9xGkBMl0qlI+lBPoOVuhnhY\n" +
                    "4gVYRUsbkoY6OCechPeLkBhPHw7QGmkAMN/p+jRgAS6FBMbg3k40kvlZwFfuq0OQdCSMLi7nDUPl\n" +
                    "1bDD58TeyI4gEhH1b+ZKg4b6RVJPXLZHE4FRSY+6zcGC36VDOxHlWB1RIHIEQIHDhV3W1JiDGIQT\n" +
                    "6HqmuTBPvKBtKyPIgLDrm0lKO4uWN3fCq59eoSWoQ7Utgq4wf0psdzU8MN3i1w6D6pDCN1tHmUhJ\n" +
                    "/CGY3lhlh/gJl3JWm2UhEiqqhzmHoQSEzL/PsR/60tWKb+/RSCeOUHaq/5+BcRoMiPxsvooKHfXM\n" +
                    "ev7auYlJ3adfRangKidRdU7rhUjm91MJohv+y6tdbVbRZzLrgyZNvngniGcfpuPnvIoCmz0bF7/G\n" +
                    "i8X9besxJTAjBgkqhkiG9w0BCRUxFgQUbg6I8267h0TUcPYvYE0D6k6+UJQwMTAhMAkGBSsOAwIa\n" +
                    "BQAEFMHGVEb7ALktCwHTFrwUvM1WicllBAhBygPhEIcRvwICCAA=";

    private static final char[] ALICE_KEYSTORE_PASS = "password".toCharArray();

    public EmsKeyStore() throws GeneralSecurityException, IOException {
        KeyStore ks = KeyStore.getInstance("PKCS12");
        ks.load(new ByteArrayInputStream(HexUtils.decodeBase64(ALICE_KEYSTORE, true)), ALICE_KEYSTORE_PASS);
        String alias = ks.aliases().nextElement();
        PrivateKey privateKey = (PrivateKey) ks.getKey(alias, ALICE_KEYSTORE_PASS);
        Certificate[] chain = ks.getCertificateChain(alias);
        X509Certificate[] x509chain = new X509Certificate[chain.length];
        //noinspection SuspiciousSystemArraycopy
        System.arraycopy(chain, 0, x509chain, 0, chain.length);
        this.sslKey = new SignerInfo(privateKey, x509chain);
    }

    public SignerInfo getSslKey() {
        return sslKey;
    }

    public static void main(String[] args) throws IOException, GeneralSecurityException {
        SignerInfo si = new EmsKeyStore().getSslKey();
        System.out.println(si.getPrivate());
        System.out.println(si.getCertificate());
    }
}
