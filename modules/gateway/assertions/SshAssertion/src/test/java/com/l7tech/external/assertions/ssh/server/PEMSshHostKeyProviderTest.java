package com.l7tech.external.assertions.ssh.server;

import com.l7tech.external.assertions.ssh.keyprovider.PemSshHostKeyProvider;
import org.apache.sshd.common.KeyPairProvider;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * Test PEM read/write from a Java String
 */
public class PEMSshHostKeyProviderTest {

    @Test
    public void testDSA() {
        String dsaPrivateKey = "-----BEGIN DSA PRIVATE KEY-----\n" +
                "MIIBuAIBAAKBgQCdLQXCHbKaPF1zEZZvI+SAz3+5REvINDDKPAprwZ+aRNvg52SH\n" +
                "dB2PZEXgDm5iaGn8f0oMfTGRnuiPVL+DDDYZIskvkBnpLqVYbb9OUBpk5SzB3Uvk\n" +
                "53Mm60TlXOKV264eSjlE6OXTJPQibqfJj2jUSl3/waprJVeNtda8tORt2QIVAPDk\n" +
                "/uF6wJEzrh1Vmer6CHYCgI3HAoGAGzdvwb8E2ANRiF217NLIuUQm5xdGbU7TYe2i\n" +
                "914Zb406DEEJ4iDAiyurU/fM8fmvVSngP0tB703HAp4KneMGhWdeoKEVN4tYgNRG\n" +
                "WUVYxW/runIaiQaydTAwdfUj3ff2+lDgX0qmsxElsZS7jmo60BKOmBTQloYcpPCR\n" +
                "75ns5n8Cfyp4Cee7Md6a0NjBpTLmgWrtAqyOhZcs9FnaorN1769xtkhePPFYDnEC\n" +
                "BweMkbl+Qr5LjmKycKCkIWcaWt64S8YQpUBBX1xyuOLhNio/JAzg0uEujomNkWCM\n" +
                "Mg2yvUZindG036mgqsSBU/VjyRREg8S+YS3zhh9L1PnCwTE6dDQCFBDa8w8JpjuD\n" +
                "o5y2zveC+jduWFNm\n" +
                "-----END DSA PRIVATE KEY-----";

        // Read existing
        PemSshHostKeyProvider provider = new PemSshHostKeyProvider();
        provider.setPrivateKey(dsaPrivateKey);
        provider.setAlgorithm("DSA");
        provider.setKeySize(512);
        assertEquals(KeyPairProvider.SSH_DSS, provider.getKeyTypes());
        assertNotNull(provider.loadKey(KeyPairProvider.SSH_DSS));
    }

    @Test
    public void testRSA() {
        String rsaPrivateKey = "-----BEGIN RSA PRIVATE KEY-----\n" +
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

        // Read existing
        PemSshHostKeyProvider provider = new PemSshHostKeyProvider();
        provider.setPrivateKey(rsaPrivateKey);
        provider.setAlgorithm("RSA");
        provider.setKeySize(32);
        assertEquals(KeyPairProvider.SSH_RSA, provider.getKeyTypes());
        assertNotNull(provider.loadKey(KeyPairProvider.SSH_RSA));
    }
}