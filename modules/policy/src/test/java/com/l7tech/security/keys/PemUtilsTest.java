package com.l7tech.security.keys;

import static com.l7tech.security.keys.PemUtils.*;
import static org.junit.Assert.*;
import org.junit.Test;

import java.security.KeyPair;

/**
 * Unit tests for PemUtils
 */
public class PemUtilsTest {
    private final String sshRsaPrivateKey = "-----BEGIN RSA PRIVATE KEY-----\n" +
            "MIIEpgIBAAKCAQEA+fb6rRyNJZxlHbfa9ZhGJmz9gL4l24aJG6sFmx9imWmo+3EH\n" +
            "9kCvt8HbhUWuJt4CkbMpuckjhu35t9hoS2+VAEuR6/K7GTUQYO5SFIax4jmng3BV\n" +
            "r0tpg1z3mCKJ4ZbRmXWhGdQtfyGcfYuw2kVYvDmdAjOgDOdLmER+CUGkCdvRhWhN\n" +
            "q8FTm3cQypf8nQN79IgacQPW648is4qqW7aYjveNmBQyCCEnciUvfUbCND1fa96l\n" +
            "1Dzhq8ESPPJ5UIFrUYucbVukziGOnZSVgbq65yHgoWEFYpJSwBd+CyugyuNaCccc\n" +
            "hyMPOf9sis6lKZvg4A/ERlocpVEjbDD24BbuqwIDAQABAoIBAQD37ZOHnpkVJAFb\n" +
            "L5/7FvUFafcq+e78xX06ty/RQ5j9h0J3Ww5FnrVrMtm0X3+zx2KO90C8qJcXXvTf\n" +
            "98LCh8MnTs1GVTRkdTBwpBE/kLXhJ8RR51rliov2IoDmIePWoEv2xShsQPp7bXAV\n" +
            "Sje41y2DTDXCGEh4Y/Wj6tOEBNpadvqM84XMFdoKOr8tZm1UGu2qIsdvnSl3lzQq\n" +
            "rrM/PllXKcptA9ppqq5yV+l9HAJadEYh/B4DGz4fxb7RzpMShA13pfCSyNnHEO8y\n" +
            "bxIBJBKLwQrHK4uhWKEc6IlsW3Kligv9+sEBjN+THh3FUj9oC8OOuGgay3GcSLa9\n" +
            "+tV8gDfpAoGBAP9jfkRcVhNh8vHr61dgxN/2Sz7TI+eHCfdXnTnS3jQbuNGsg4vi\n" +
            "kgVFxWa/JmEofpvwZcVVOIXT/OnofipGZcLTHz35s7dhxA+TFyfWkGizUEoEn5qu\n" +
            "3U4EiXUKy/P5GC42hY7yd8O+NkJ3ktL1GkCtUzOD9RydeOAO8v//cH0FAoGBAPqQ\n" +
            "KYCzYrxe6TDlDlPiVilCSxu274O/pjtwhXMYO1TcRkcJZ38JnNqgYveJiGEdbPKN\n" +
            "NpBvGfeaC8jeJZhGf0PlRhmNe0Xjoxy4akdcMdMjcObGceRUC5aDicHDKFmgmQzw\n" +
            "7ZXFaulPZQy+utz7pxWVp40uvwsZKP1p6MMEVwvvAoGBAMf6qobxHt2yl0BkkjYr\n" +
            "qj4NaEJbpwPHNECgNJdwzVpUUtaslZ1V3y2NwtN/3pe509pb1fU9lDMHGkY3LYQA\n" +
            "9/Iky5QGEXoJbjMb5MfnNdEmiDpNgITpZJWQ6+ngeHAkn5CgVNjoeGuoaGiHpUrX\n" +
            "bqUyk7IFJEwx6tLYIePfq7/ZAoGBAMVGYdbZ5eAn3fPINGqpJmtfraEJfDS/3NKp\n" +
            "ufgvWO9sasydQ+ZKnDup0aHRoBXORCwIMLCxOMGwgJzLAtCMmMDNME8IDMmu+4qu\n" +
            "S8cZeIUjP04DakJ6RZFgyUJMNGW0wyvkOTsVbyJ4hzfsp1U7sYaWCJBpALNtQM+c\n" +
            "5k09CofzAoGBAIJdv/hv/uLb6DirtTHw2+ZEl4d3vYlFKWi6CpmXFelM2CYgkbE5\n" +
            "CQ4e4DWuCtp+UEf+AAwy7ilwfSlmBQuNZVMRUbEmPPS7LdwipqFI9SiflHkcpnY3\n" +
            "mOX95u/xGpBzwgSt9CGoe1HF20+vrSdKjHUkiwFNdGlCvao9W0aIxP3M\n" +
            "-----END RSA PRIVATE KEY-----";

    private final String sshDsaPrivateKey = "-----BEGIN DSA PRIVATE KEY-----\n" +
        "MIIBuwIBAAKBgQCotOa8d/U3olErx6qJ4KPp3Ol1bGfA1TSsPGmQ2r4V0sxZdOHX\n" +
        "Np34VFxXOugSZKpBRhkSsntCKtq8ecklvwt7HcoAY7h0+5JXu4pt23dvGB8VYNTs\n" +
        "LMdQZRIgmoilIineKEgi4st0qLBewpCqQGBpMgYaZ3YGV7D2473Rf2QnewIVAMlf\n" +
        "I+nxpAoD3JwVH/8s/6eoBfZlAoGARCtVx/Pd0jX9K+x2aJAiKAGXRcD1Yj+63Rr3\n" +
        "no4tmWSPtLxbuex21ExCOZALwB8mZa3u6y9w1EfmTXaALhZYsPVOjc9irFb6nLwG\n" +
        "SQmZCWlG+hNwELtyAUnIiLS5UBNxyWDsiGPa3IqQpIzMLN1bMB2UJLH+JN3yQ89+\n" +
        "eJKDUzwCgYBPoksRewYwbLfXsbMPRQ0TT5Sz+Hp7HT+LdKCjZ8kvpTTxxB/OnLJ7\n" +
        "6X6xauFmnAc3MJlxbwOYSCl8tJwpTiRUVCYAaCr3BbGtEmophT2MveM426hNhoyu\n" +
        "Sv6xYSSFJexQQUeWcjzWnIcZ/2SiTzDPcADb+f8+zq0XQbd22v0QNQIVAIbSsqR2\n" +
        "hoHLW6hd4HMkem4vqaqA\n" +
        "-----END DSA PRIVATE KEY-----";

    @Test
    public void testGetPemPrivateKeyAlgorithm() {
        assertEquals( "RSA key", "RSA", getPemPrivateKeyAlgorithm(sshRsaPrivateKey) );
        assertEquals( "DSA key", "DSA", getPemPrivateKeyAlgorithm(sshDsaPrivateKey) );
        assertNull( "Unknown key", getPemPrivateKeyAlgorithm("werwetwew") );
    }

    @Test
    public void testReadWriteKeyPair() throws Exception {
        final KeyPair rsaKeyPair = doReadKeyPair( sshRsaPrivateKey );
        final String rsaKeyPairPem = doWriteKeyPair( rsaKeyPair.getPrivate() );
        assertEquals( "RSA key round tripped", "RSA", getPemPrivateKeyAlgorithm(rsaKeyPairPem) );

        final KeyPair dsaKeyPair = doReadKeyPair( sshDsaPrivateKey );
        final String dsaKeyPairPem = doWriteKeyPair( dsaKeyPair.getPrivate() );
        assertEquals( "DSA key round tripped", "DSA", getPemPrivateKeyAlgorithm(dsaKeyPairPem) );
    }

    @Test
    public void testWritePublicKey() throws Exception {
        final KeyPair rsaKeyPair = doReadKeyPair( sshRsaPrivateKey );
        final String rsaKeyPemOneLine = writeKey( rsaKeyPair.getPublic() );
        final String rsaKeyPem = writeKey( rsaKeyPair.getPublic(), true );
        assertNotNull( "RSA key PEM (one line)", rsaKeyPemOneLine );
        assertTrue( "RSA key PEM (one line)", rsaKeyPemOneLine.contains( "BEGIN PUBLIC KEY" ) );
        assertFalse( "RSA key PEM line breaks (one line)", rsaKeyPemOneLine.contains( "\n" ) );
        assertNotNull( "RSA key PEM", rsaKeyPem );
        assertTrue( "RSA key PEM", rsaKeyPem.contains( "BEGIN PUBLIC KEY" ) );
        assertTrue( "RSA key PEM line breaks", rsaKeyPem.contains( "\n" ) );

        final KeyPair dsaKeyPair = doReadKeyPair( sshDsaPrivateKey );
        final String dsaKeyPemOneLine = writeKey( dsaKeyPair.getPublic() );
        final String dsaKeyPem = writeKey( rsaKeyPair.getPublic(), true );
        assertNotNull( "DSA key PEM (one line)", dsaKeyPemOneLine );
        assertTrue( "DSA key PEM (one line)", dsaKeyPemOneLine.contains( "BEGIN PUBLIC KEY" ) );
        assertFalse( "DSA key PEM line breaks (one line)", dsaKeyPemOneLine.contains( "\n" ) );
        assertNotNull( "DSA key PEM", dsaKeyPem );
        assertTrue( "DSA key PEM", dsaKeyPem.contains( "BEGIN PUBLIC KEY" ) );
        assertTrue( "DSA key PEM line breaks", dsaKeyPem.contains( "\n" ) );
    }
}
