package com.l7tech.security.keys;

import com.l7tech.security.prov.JceProvider;

import com.l7tech.util.ExceptionUtils;
import com.l7tech.util.SyspropUtil;
import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.openssl.PEMKeyPair;
import org.bouncycastle.openssl.PEMParser;
import org.bouncycastle.openssl.jcajce.JcaPEMKeyConverter;
import org.bouncycastle.openssl.jcajce.JcaPEMWriter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.*;
import java.security.KeyPair;
import java.security.PrivateKey;
import java.security.Provider;
import java.security.PublicKey;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Utility methods for working with PEM keys.
 *
 * <p>Use of these utility methods may cause a JceProvider to be initialized.</p>
 */
public class PemUtils {
    private static final Logger LOG = Logger.getLogger(PemUtils.class.getName());

    private static final String ALGORITHM_DSA = "DSA";
    private static final String ALGORITHM_RSA = "RSA";
    private static final String PEM_BEGIN = "-----BEGIN ";

    /**
     * Determine algorithm of a given private key.
     *
     * @param pemPrivateKey a private key string in PEM format (must not be null)
     * @return an algorithm string for RSA or DSA or null if unknown
     */
    @Nullable
    public static String getPemPrivateKeyAlgorithm(String pemPrivateKey) {
        if ( pemPrivateKey.contains( PEM_BEGIN + ALGORITHM_RSA ) ) {
            return ALGORITHM_RSA;
        } else if ( pemPrivateKey.contains( PEM_BEGIN + ALGORITHM_DSA ) ) {
            return ALGORITHM_DSA;
        } else {
            return null;
        }
    }

    /**
     * Read a key pair from a PEM format.
     *
     * @param privateKey The key to read (required)
     * @return The key pair
     * @throws IOException If an error occurs
     */
    @NotNull
    public static KeyPair doReadKeyPair( final String privateKey ) throws IOException {
        final Reader reader = new StringReader(privateKey);
        try (final PEMParser pemParser = new PEMParser(reader)) {
            final Object obj = pemParser.readObject();
            if (!(obj instanceof PEMKeyPair)) {
                throw new IOException("A valid key pair was not found.");
            }
            return new JcaPEMKeyConverter().getKeyPair((PEMKeyPair) obj);
        }
    }

    /**
     * Read a certificate from a PEM format.
     *
     * @param certificate The certificate to read (required)
     * @return X509Certificate
     * @throws IOException If an error occurs while reading
     */
    @NotNull
    public static X509Certificate doReadCertificate(final String certificate) throws CertificateException, IOException {
        final Reader reader = new StringReader(certificate);
        try (final PEMParser pemParser = new PEMParser(reader)) {
            final Object obj = pemParser.readObject();
            if (!(obj instanceof X509CertificateHolder)) {
                throw new CertificateException("A valid certificate was not found.");
            }
            return new JcaX509CertificateConverter().getCertificate((X509CertificateHolder) obj);
        }
    }

    /**
     * Write the given private key as PEM.
     *
     * <p>Note that the output is a PEM private key but the corresponding
     * <code>doReadKeyPair</code> method should be used to read the output.</p>
     *
     * @param privateKey The private key to write (required)
     * @return The PEM private key
     * @throws IOException If an error occurs while writing
     */
    @NotNull
    public static String doWriteKeyPair( final PrivateKey privateKey ) throws IOException {
        final StringWriter writer = new StringWriter();
        try (final JcaPEMWriter pemWriter = new JcaPEMWriter(writer)) {
            pemWriter.writeObject(privateKey);
        }
        return writer.toString();
    }

    /**
     * Writes key in a PEM format with or without linebreaks.
     *
     * @param publicKey The key to write (required)
     * @param newlines True to include newlines
     * @return The PEMish text or null
     */
    @Nullable
    public static String writeKey( final PublicKey publicKey, final boolean newlines ) {
        final OutputStream os = new ByteArrayOutputStream();
        try (final JcaPEMWriter pemWriter = new JcaPEMWriter(new OutputStreamWriter(os))) {
            pemWriter.writeObject(publicKey);
            pemWriter.flush();
            String pemKey = os.toString();
            if (!newlines) {
                pemKey = pemKey.replace(SyspropUtil.getProperty("line.separator"), "");
            }
            return pemKey;
        } catch (IOException e) {
            LOG.log(Level.INFO, "Unable to write key: " + ExceptionUtils.getMessage(e), ExceptionUtils.getDebugException(e));
        }
        return null;
    }

    /**
     * Writes key in a PEM format but without any linebreaks.
     *
     * @param publicKey The key to write (required)
     * @return The PEMish text or null
     */
    @Nullable
    public static String writeKey( final PublicKey publicKey ) {
        return writeKey( publicKey, false );
    }

    @Nullable
    public static String getProviderNameForService( final String service ) {
        final Provider provider = JceProvider.getInstance().getPreferredProvider( service );
        return provider == null ? null : provider.getName();
    }
}
