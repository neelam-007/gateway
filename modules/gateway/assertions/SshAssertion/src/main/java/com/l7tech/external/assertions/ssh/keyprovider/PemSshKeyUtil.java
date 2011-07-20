package com.l7tech.external.assertions.ssh.keyprovider;

import com.l7tech.security.prov.JceProvider;
import org.bouncycastle.openssl.PEMReader;
import org.bouncycastle.openssl.PEMWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.crypto.Cipher;
import javax.crypto.NoSuchPaddingException;
import java.io.*;
import java.security.*;

/**
 * PEM SSH key utility class.
 */
public class PemSshKeyUtil {
    public static final String PEM = "PEM";

    private static final Logger LOG = LoggerFactory.getLogger(PemSshKeyUtil.class);
    private static final String ALGORITHM_DSA = "DSA";
    private static final String ALGORITHM_RSA = "RSA";
    private static final String PEM_BEGIN = "-----BEGIN ";

    /**
     * Determine algorithm of a given private key.
     * @param pemPrivateKey a private key string in PEM format
     * @return an algorithm string for RSA or DSA
     */
    public static String getPemAlgorithm(String pemPrivateKey) {
        if (pemPrivateKey.indexOf(PEM_BEGIN + ALGORITHM_RSA) >= 0) {
            return ALGORITHM_RSA;
        } else if (pemPrivateKey.indexOf(PEM_BEGIN + ALGORITHM_DSA) >= 0) {
            return ALGORITHM_DSA;
        } else {
            return null;
        }
    }

    /**
     * TODO add to JceProvider as a utility method as recommended by ML 2011/07/14
     * Get the provider name for AES service.
     * @return a String for the provider name
     */
    public static String getSymProvider() throws NoSuchProviderException, NoSuchAlgorithmException, NoSuchPaddingException {
        Provider sp = JceProvider.getInstance().getProviderFor("Cipher.AES");
        return sp != null ? sp.getName() : Cipher.getInstance("AES").getProvider().getName();
    }

    /**
     * TODO add to JceProvider as a utility method as recommended by ML 2011/07/14
     * Get the provider name for RSA service.
     * Note: Cipher.getInstance("RSA").getProvider().getName() throws error when using Bouncy Castle's PEMReader
     * @return a String for the provider name
     */
    public static String getAsymProvider() throws NoSuchProviderException, NoSuchAlgorithmException, NoSuchPaddingException {
        Provider ap = JceProvider.getInstance().getProviderFor("Cipher.RSA");
        return ap != null ? ap.getName() : Cipher.getInstance("RSA/NONE/NoPadding").getProvider().getName();
    }

    public static KeyPair doReadKeyPair(String privateKey) throws Exception {
        InputStream is = new ByteArrayInputStream(privateKey.getBytes());
        PEMReader r = new PEMReader(new InputStreamReader(is), null, getSymProvider(), getAsymProvider());
        return (KeyPair) r.readObject();
    }

    public static String writeKey(PublicKey publicKey) {
        OutputStream os = new ByteArrayOutputStream();
        String pemPublicKey = null;
        try {
            PEMWriter w = new PEMWriter(new OutputStreamWriter(os));
            w.writeObject(publicKey);
            w.flush();
            return os.toString();
        } catch (Exception e) {
            LOG.info("Unable to write key: {}", e);
        } finally {
            close(os);
        }
        return pemPublicKey;
    }

    private static void close(Closeable c) {
        try {
            if (c != null) {
                c.close();
            }
        } catch (IOException e) {
            // Ignore
        }
    }
}
