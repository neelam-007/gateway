package com.l7tech.skunkworks;

import com.l7tech.common.io.CertUtils;
import com.l7tech.util.IOUtils;
import com.l7tech.security.xml.SignerInfo;
import com.l7tech.util.HexUtils;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.security.KeyStore;
import java.security.cert.X509Certificate;
import java.util.concurrent.Callable;

/**
 * A utility class for testing that makes it easy to convert keystore info to and from Base64.
 */
public class Base64Keystore {
    /**
     * Convert a keystore file to base64.
     *
     * @param pkcs12Path  path to the file to convert.  Required.
     * @return a base64 representation of the keystore bytes.
     * @throws IOException if there is a problem reading the file
     */
    public static String toBase64(File pkcs12Path) throws IOException {
        return HexUtils.encodeBase64(IOUtils.slurpFile(pkcs12Path));
    }

    /**
     * Decode a private key and cert chain from a base64 keystore representation as returned from
     * {@link #toBase64}.
     *
     * @param pkcs12Base64  base64'ed PKCS#12 file with optional prepended password.  Required.
     * @param password password to use. Required.
     * @return the first private key and cert chain from the decoded keystore.
     */
    public static SignerInfo fromBase64(String pkcs12Base64, String password) throws RuntimeException {

        try {
            final char[] passchars = password.toCharArray();
            final byte[] p12bytes = HexUtils.decodeBase64(pkcs12Base64, true);

            KeyStore.PrivateKeyEntry entry = CertUtils.loadPrivateKey(new Callable<InputStream>() {
                public InputStream call() throws Exception {
                    return new ByteArrayInputStream(p12bytes);
                }
            }, "PKCS12", passchars, null, passchars);

            return new SignerInfo(entry.getPrivateKey(), (X509Certificate[]) entry.getCertificateChain());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static void main(String[] args) throws Exception {
        String path = args[0];

        System.out.println(toBase64(new File(path)));
    }
}
