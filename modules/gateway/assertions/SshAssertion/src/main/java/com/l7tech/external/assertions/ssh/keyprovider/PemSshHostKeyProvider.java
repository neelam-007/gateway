package com.l7tech.external.assertions.ssh.keyprovider;

import com.l7tech.security.prov.JceProvider;
import org.bouncycastle.openssl.PEMReader;
import org.bouncycastle.openssl.PasswordFinder;

import javax.crypto.Cipher;
import javax.crypto.NoSuchPaddingException;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.security.KeyPair;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.Provider;
import java.util.Arrays;

/**
 * L7 customized PEM SSH host key provider class for use in Apache SSHD.
 * Heavily borrowed from org.apache.sshd.server.keyprovider.PEMGeneratorHostKeyProvider
 */
public class PemSshHostKeyProvider extends AbstractSshHostKeyProvider {
    private static final String PEM_BEGIN = "-----BEGIN ";
    private static final String ALGORITHM_RSA = "RSA";
    private static final String ALGORITHM_DSA = "DSA";

    public static final String PEM = "PEM";

    public static String getPemAlgorithm(String pemPrivateKey) {
        if (pemPrivateKey.indexOf(PEM_BEGIN + ALGORITHM_RSA) >= 0) {
            return ALGORITHM_RSA;
        } else if (pemPrivateKey.indexOf(PEM_BEGIN + ALGORITHM_DSA) >= 0) {
            return ALGORITHM_DSA;
        } else {
            return null;
        }
    }

    private static class DefaultPasswordFinder implements PasswordFinder {
        private final char [] password;

        private DefaultPasswordFinder(char [] password) {
            this.password = password;
        }

        @Override
        public char[] getPassword() {
            return Arrays.copyOf(password, password.length);
        }
    }

    public PemSshHostKeyProvider() {
    }

    public PemSshHostKeyProvider(String privateKey) {
        super(privateKey);
    }

    public PemSshHostKeyProvider(String privateKey, String algorithm) {
        super(privateKey, algorithm);
    }

    public PemSshHostKeyProvider(String privateKey, String algorithm, int keySize) {
        super(privateKey, algorithm, keySize);
    }

    protected KeyPair doReadKeyPair(String privateKey) throws Exception {
        InputStream is = new ByteArrayInputStream(privateKey.getBytes());
        PEMReader r = new PEMReader(new InputStreamReader(is), null, getSymProvider(), getAsymProvider());
        return (KeyPair) r.readObject();
    }

    /**
     * TODO add to JceProvider as a utility method as recommended by ML 2011/07/14
     * Get the provider name for AES service.
     * @return a String for the provider name
     */
    private String getSymProvider() throws NoSuchProviderException, NoSuchAlgorithmException, NoSuchPaddingException {
        Provider sp = JceProvider.getInstance().getProviderFor("Cipher.AES");
        return sp != null ? sp.getName() : Cipher.getInstance("AES").getProvider().getName();
    }

    /**
     * TODO add to JceProvider as a utility method as recommended by ML 2011/07/14
     * Get the provider name for RSA service.
     * Note: Cipher.getInstance("RSA").getProvider().getName() throws error when using Bouncy Castle's PEMReader
     * @return a String for the provider name
     */
    private String getAsymProvider() throws NoSuchProviderException, NoSuchAlgorithmException, NoSuchPaddingException {
        Provider ap = JceProvider.getInstance().getProviderFor("Cipher.RSA");
        return ap != null ? ap.getName() : Cipher.getInstance("RSA/NONE/NoPadding").getProvider().getName();
    }
}
