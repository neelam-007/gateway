package com.l7tech.external.assertions.ssh.server;

import com.l7tech.external.assertions.ssh.server.keyprovider.PemSshHostKeyProvider;
import com.l7tech.external.assertions.ssh.keyprovider.SshKeyUtil;
import com.l7tech.util.SyspropUtil;
import org.apache.sshd.common.KeyPairProvider;
import org.bouncycastle.openssl.PEMReader;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.security.KeyPair;
import java.security.PublicKey;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * Test PEM read/write from a Java String
 */
public class PEMSshHostKeyProviderTest {
    final String lineSeparator = SyspropUtil.getProperty("line.separator");
    final String sshRsaPrivateKey = "-----BEGIN RSA PRIVATE KEY-----" + lineSeparator +
            "MIIEpgIBAAKCAQEA+fb6rRyNJZxlHbfa9ZhGJmz9gL4l24aJG6sFmx9imWmo+3EH" + lineSeparator +
            "9kCvt8HbhUWuJt4CkbMpuckjhu35t9hoS2+VAEuR6/K7GTUQYO5SFIax4jmng3BV" + lineSeparator +
            "r0tpg1z3mCKJ4ZbRmXWhGdQtfyGcfYuw2kVYvDmdAjOgDOdLmER+CUGkCdvRhWhN" + lineSeparator +
            "q8FTm3cQypf8nQN79IgacQPW648is4qqW7aYjveNmBQyCCEnciUvfUbCND1fa96l" + lineSeparator +
            "1Dzhq8ESPPJ5UIFrUYucbVukziGOnZSVgbq65yHgoWEFYpJSwBd+CyugyuNaCccc" + lineSeparator +
            "hyMPOf9sis6lKZvg4A/ERlocpVEjbDD24BbuqwIDAQABAoIBAQD37ZOHnpkVJAFb" + lineSeparator +
            "L5/7FvUFafcq+e78xX06ty/RQ5j9h0J3Ww5FnrVrMtm0X3+zx2KO90C8qJcXXvTf" + lineSeparator +
            "98LCh8MnTs1GVTRkdTBwpBE/kLXhJ8RR51rliov2IoDmIePWoEv2xShsQPp7bXAV" + lineSeparator +
            "Sje41y2DTDXCGEh4Y/Wj6tOEBNpadvqM84XMFdoKOr8tZm1UGu2qIsdvnSl3lzQq" + lineSeparator +
            "rrM/PllXKcptA9ppqq5yV+l9HAJadEYh/B4DGz4fxb7RzpMShA13pfCSyNnHEO8y" + lineSeparator +
            "bxIBJBKLwQrHK4uhWKEc6IlsW3Kligv9+sEBjN+THh3FUj9oC8OOuGgay3GcSLa9" + lineSeparator +
            "+tV8gDfpAoGBAP9jfkRcVhNh8vHr61dgxN/2Sz7TI+eHCfdXnTnS3jQbuNGsg4vi" + lineSeparator +
            "kgVFxWa/JmEofpvwZcVVOIXT/OnofipGZcLTHz35s7dhxA+TFyfWkGizUEoEn5qu" + lineSeparator +
            "3U4EiXUKy/P5GC42hY7yd8O+NkJ3ktL1GkCtUzOD9RydeOAO8v//cH0FAoGBAPqQ" + lineSeparator +
            "KYCzYrxe6TDlDlPiVilCSxu274O/pjtwhXMYO1TcRkcJZ38JnNqgYveJiGEdbPKN" + lineSeparator +
            "NpBvGfeaC8jeJZhGf0PlRhmNe0Xjoxy4akdcMdMjcObGceRUC5aDicHDKFmgmQzw" + lineSeparator +
            "7ZXFaulPZQy+utz7pxWVp40uvwsZKP1p6MMEVwvvAoGBAMf6qobxHt2yl0BkkjYr" + lineSeparator +
            "qj4NaEJbpwPHNECgNJdwzVpUUtaslZ1V3y2NwtN/3pe509pb1fU9lDMHGkY3LYQA" + lineSeparator +
            "9/Iky5QGEXoJbjMb5MfnNdEmiDpNgITpZJWQ6+ngeHAkn5CgVNjoeGuoaGiHpUrX" + lineSeparator +
            "bqUyk7IFJEwx6tLYIePfq7/ZAoGBAMVGYdbZ5eAn3fPINGqpJmtfraEJfDS/3NKp" + lineSeparator +
            "ufgvWO9sasydQ+ZKnDup0aHRoBXORCwIMLCxOMGwgJzLAtCMmMDNME8IDMmu+4qu" + lineSeparator +
            "S8cZeIUjP04DakJ6RZFgyUJMNGW0wyvkOTsVbyJ4hzfsp1U7sYaWCJBpALNtQM+c" + lineSeparator +
            "5k09CofzAoGBAIJdv/hv/uLb6DirtTHw2+ZEl4d3vYlFKWi6CpmXFelM2CYgkbE5" + lineSeparator +
            "CQ4e4DWuCtp+UEf+AAwy7ilwfSlmBQuNZVMRUbEmPPS7LdwipqFI9SiflHkcpnY3" + lineSeparator +
            "mOX95u/xGpBzwgSt9CGoe1HF20+vrSdKjHUkiwFNdGlCvao9W0aIxP3M" + lineSeparator +
            "-----END RSA PRIVATE KEY-----";
    final String sshRsaPemPublicKey = "-----BEGIN PUBLIC KEY-----" + lineSeparator +
            "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEA+fb6rRyNJZxlHbfa9ZhG" + lineSeparator +
            "Jmz9gL4l24aJG6sFmx9imWmo+3EH9kCvt8HbhUWuJt4CkbMpuckjhu35t9hoS2+V" + lineSeparator +
            "AEuR6/K7GTUQYO5SFIax4jmng3BVr0tpg1z3mCKJ4ZbRmXWhGdQtfyGcfYuw2kVY" + lineSeparator +
            "vDmdAjOgDOdLmER+CUGkCdvRhWhNq8FTm3cQypf8nQN79IgacQPW648is4qqW7aY" + lineSeparator +
            "jveNmBQyCCEnciUvfUbCND1fa96l1Dzhq8ESPPJ5UIFrUYucbVukziGOnZSVgbq6" + lineSeparator +
            "5yHgoWEFYpJSwBd+CyugyuNaCccchyMPOf9sis6lKZvg4A/ERlocpVEjbDD24Bbu" + lineSeparator +
            "qwIDAQAB" + lineSeparator +
            "-----END PUBLIC KEY-----";
    final String sshDsaPrivateKey = "-----BEGIN DSA PRIVATE KEY-----" + lineSeparator +
            "MIIBuwIBAAKBgQCotOa8d/U3olErx6qJ4KPp3Ol1bGfA1TSsPGmQ2r4V0sxZdOHX" + lineSeparator +
            "Np34VFxXOugSZKpBRhkSsntCKtq8ecklvwt7HcoAY7h0+5JXu4pt23dvGB8VYNTs" + lineSeparator +
            "LMdQZRIgmoilIineKEgi4st0qLBewpCqQGBpMgYaZ3YGV7D2473Rf2QnewIVAMlf" + lineSeparator +
            "I+nxpAoD3JwVH/8s/6eoBfZlAoGARCtVx/Pd0jX9K+x2aJAiKAGXRcD1Yj+63Rr3" + lineSeparator +
            "no4tmWSPtLxbuex21ExCOZALwB8mZa3u6y9w1EfmTXaALhZYsPVOjc9irFb6nLwG" + lineSeparator +
            "SQmZCWlG+hNwELtyAUnIiLS5UBNxyWDsiGPa3IqQpIzMLN1bMB2UJLH+JN3yQ89+" + lineSeparator +
            "eJKDUzwCgYBPoksRewYwbLfXsbMPRQ0TT5Sz+Hp7HT+LdKCjZ8kvpTTxxB/OnLJ7" + lineSeparator +
            "6X6xauFmnAc3MJlxbwOYSCl8tJwpTiRUVCYAaCr3BbGtEmophT2MveM426hNhoyu" + lineSeparator +
            "Sv6xYSSFJexQQUeWcjzWnIcZ/2SiTzDPcADb+f8+zq0XQbd22v0QNQIVAIbSsqR2" + lineSeparator +
            "hoHLW6hd4HMkem4vqaqA" + lineSeparator +
            "-----END DSA PRIVATE KEY-----" + lineSeparator;
    final String sshDsaPemPublicKey = "-----BEGIN PUBLIC KEY-----" + lineSeparator +
            "MIIBtjCCASsGByqGSM44BAEwggEeAoGBAKi05rx39TeiUSvHqongo+nc6XVsZ8DV" + lineSeparator +
            "NKw8aZDavhXSzFl04dc2nfhUXFc66BJkqkFGGRKye0Iq2rx5ySW/C3sdygBjuHT7" + lineSeparator +
            "kle7im3bd28YHxVg1Owsx1BlEiCaiKUiKd4oSCLiy3SosF7CkKpAYGkyBhpndgZX" + lineSeparator +
            "sPbjvdF/ZCd7AhUAyV8j6fGkCgPcnBUf/yz/p6gF9mUCgYBEK1XH893SNf0r7HZo" + lineSeparator +
            "kCIoAZdFwPViP7rdGveeji2ZZI+0vFu57HbUTEI5kAvAHyZlre7rL3DUR+ZNdoAu" + lineSeparator +
            "Fliw9U6Nz2KsVvqcvAZJCZkJaUb6E3AQu3IBSciItLlQE3HJYOyIY9rcipCkjMws" + lineSeparator +
            "3VswHZQksf4k3fJDz354koNTPAOBhAACgYBPoksRewYwbLfXsbMPRQ0TT5Sz+Hp7" + lineSeparator +
            "HT+LdKCjZ8kvpTTxxB/OnLJ76X6xauFmnAc3MJlxbwOYSCl8tJwpTiRUVCYAaCr3" + lineSeparator +
            "BbGtEmophT2MveM426hNhoyuSv6xYSSFJexQQUeWcjzWnIcZ/2SiTzDPcADb+f8+" + lineSeparator +
            "zq0XQbd22v0QNQ==" + lineSeparator +
            "-----END PUBLIC KEY-----";

    @Test
    public void testDSAHostKeyProvider() {
        // read existing
        PemSshHostKeyProvider provider = new PemSshHostKeyProvider();
        provider.setPrivateKey(sshDsaPrivateKey);
        provider.setAlgorithm("DSA");
        provider.setKeySize(512);
        assertEquals(KeyPairProvider.SSH_DSS, provider.getKeyTypes());
        assertNotNull(provider.loadKey(KeyPairProvider.SSH_DSS));
    }

    @Test
    public void testRSAHostKeyProvider() {
        // read existing
        PemSshHostKeyProvider provider = new PemSshHostKeyProvider();
        provider.setPrivateKey(sshRsaPrivateKey);
        provider.setAlgorithm("RSA");
        provider.setKeySize(32);
        assertEquals(KeyPairProvider.SSH_RSA, provider.getKeyTypes());
        assertNotNull(provider.loadKey(KeyPairProvider.SSH_RSA));
    }

    @Test
    public void testRsaPemReadWrite() throws Exception {
        // pem read rsa private key
        InputStream is = new ByteArrayInputStream(sshRsaPrivateKey.getBytes());
        PEMReader r = new PEMReader(new InputStreamReader(is), null, SshKeyUtil.getSymProvider(), SshKeyUtil.getAsymProvider());
        KeyPair keyPair =  (KeyPair) r.readObject();
        PublicKey publicKey = keyPair.getPublic();

        // pem write rsa public key should match
        String writtenRemRsaPublicKey = SshKeyUtil.writeKey(publicKey);
        assertTrue(writtenRemRsaPublicKey.contains(sshRsaPemPublicKey));
    }

    @Test
    public void testDsaPemReadWrite() throws Exception {
        // pem read dsa private key
        InputStream is = new ByteArrayInputStream(sshDsaPrivateKey.getBytes());
        PEMReader r = new PEMReader(new InputStreamReader(is), null, SshKeyUtil.getSymProvider(), SshKeyUtil.getAsymProvider());
        KeyPair keyPair =  (KeyPair) r.readObject();
        PublicKey publicKey = keyPair.getPublic();

        // pem write dsa public key should match
        String writtenRemDsaPublicKey = SshKeyUtil.writeKey(publicKey);
        assertTrue(writtenRemDsaPublicKey.contains(sshDsaPemPublicKey));
    }

    @Test
    public void testGetPemPrivateKeyAlgorithm() throws Exception {
        assertNotNull(SshKeyUtil.getPemPrivateKeyAlgorithm(sshRsaPrivateKey));
        assertNotNull(SshKeyUtil.getPemPrivateKeyAlgorithm(sshDsaPrivateKey));
    }
}