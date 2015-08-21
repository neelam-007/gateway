package com.l7tech.server.security.signer;

import com.l7tech.common.io.NullOutputStream;
import com.l7tech.gateway.common.security.signer.SignerUtils;
import com.l7tech.util.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.*;
import java.security.*;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Properties;

/**
 * Implementation of {@link SignatureVerifier}
 */
public class SignatureVerifierImpl implements SignatureVerifier {

    /**
     * Trust store file containing trusted signers certs
     */
    private final File trustedSignersStore;

    /**
     * KeyStore type for file used as a trust store.  Since certs are public and are not cryptographic secrets, it is
     * fine (preferable, in fact, for performance) to just use JKS format instead of something like PKCS#12.
     */
    private final String trustedSignersStoreType;

    /**
     * A password is required in order to load or save a KeyStore, even in JKS format.
     * But, for a trust store (containing only trusted certs) this is superfluous for security since
     * the certificates it contains are almost always public information, or at least not cryptographic secrets.
     * <br/>
     * Since we don't care about secrecy of certificates we will just use the string "changeit".
     * This also happens to be the passphrase used for the "cacerts" trust store file that comes with the JVM.
     */
    private final char[] trustedSignersStorePassword;

    /**
     * Default constructor.
     *
     * @param trustedSignersStore    location of the trusted signers {@code KeyStore}.  Required and cannot be {@code null}.
     * @param type                   the type of the trusted signers {@code KeyStore}.  Required and cannot be {@code null}.
     * @param password               trusted signers {@code KeyStore} password to use for decrypting or unlocking the key store.
     *                               Generally required by software (file-based) key stores.
     */
    SignatureVerifierImpl(
            final File trustedSignersStore,
            final String type,
            char[] password
    ) throws CertificateException, NoSuchAlgorithmException, KeyStoreException, IOException {
        if (trustedSignersStore == null || !validateTrustStoreFile(trustedSignersStore, type, password)) {
            throw new IllegalArgumentException("trustedSignersStore doesn't exist or is not a file: " + trustedSignersStore);
        }
        this.trustedSignersStore = trustedSignersStore;
        this.trustedSignersStoreType = type;
        this.trustedSignersStorePassword = password;
    }

    /**
     * Ensure the specified trust store exists as a valid jks file with the expected password.
     *
     * @param trustStoreFile    trust store file to verify.  Required and cannot be {@code null}.
     * @param type              the type of the trusted signers {@code KeyStore}.  Required and cannot be {@code null}.
     * @param password          trusted signers {@code KeyStore} password to use for decrypting or unlocking the key store.
     *                          Generally required by software (file-based) key stores.
     * @return {@code true} if a valid trust store file already exists at this location.
     *         {@code false} if a file did not exist at this location.
     * @throws IOException if there is an unexpected error reading or creating the file (including invalid path, permission denied, etc)
     * @throws KeyStoreException if the trust store file already exists but the file format is invalid
     * @throws CertificateException if the trust store file already exists but contains at least one certificate that can't be loaded
     * @throws NoSuchAlgorithmException if a needed cryptographic primitive is unavailable in the current environment
     */
    private boolean validateTrustStoreFile(
            @NotNull final File trustStoreFile,
            @NotNull final String type,
            @Nullable char[] password
    ) throws IOException, KeyStoreException, CertificateException, NoSuchAlgorithmException {
        final KeyStore ks = KeyStore.getInstance(type);
        try {
            SignerUtils.loadKeyStoreFromFile(trustStoreFile, ks, password);
            return true;
        } catch (final FileNotFoundException fe) {
            return false;
        }
    }

    /**
     * Validates signature and also verifies that signer cert is trusted.<br/>
     * The .ZIP file must be created using our signer tool, as the content of the zip must be in specified order.
     *
     * @param zipToVerify    a {@code InputStream} containing .ZIP file as produced by the signer tool.  Required and cannot be {@code null}.
     * @throws java.security.SignatureException if signature cannot be validated or signer cert is not trusted.
     */
    public void verify(@NotNull final InputStream zipToVerify) throws SignatureException {
        final X509Certificate sawSigner;
        try {
            sawSigner = SignerUtils.verifyZip(zipToVerify);
        } catch (final SignatureException e) {
            throw e;
        } catch (final Exception e) {
            throw new SignatureException("Failed to verify and extracts signer certificate", e);
        }

        try {
            SignerUtils.verifySignerCertIsTrusted(trustedSignersStore, trustedSignersStoreType, trustedSignersStorePassword, sawSigner);
        } catch (final Exception e) {
            throw new SignatureException("Failed to verify signer certificate", e);
        }
    }

    /**
     * Validates signature and also verifies that signer cert is trusted.
     *
     * @param digest                Calculated digest of the file content to check signature.  Required and cannot be {@code null}.
     * @param signatureProperties   A {@code String} with the signature properties, as produced by the signer tool.
     *                              Optional and can be {@code null} if module is not signed.
     * @throws SignatureException if signature cannot be validated or signer cert is not trusted.
     */
    @Override
    public void verify(@NotNull final byte[] digest, @Nullable final String signatureProperties) throws SignatureException {
        // if no signature provided throw
        if (StringUtils.isBlank(signatureProperties)) {
            throw new SignatureException("Module is not signed");
        }

        // extract content signer cert
        final X509Certificate sawSigner;
        assert signatureProperties != null;
        try (final StringReader reader = new StringReader(signatureProperties)) {
            final Properties sigProps = new Properties();
            sigProps.load(reader);
            sawSigner = SignerUtils.verifySignatureWithDigest(digest, sigProps);
        } catch (final SignatureException e) {
            throw e;
        } catch (final Exception e) {
            throw new SignatureException("Failed to verify and extract signer certificate", e);
        }

        // verify that content signer cert is trusted
        try {
            SignerUtils.verifySignerCertIsTrusted(trustedSignersStore, trustedSignersStoreType, trustedSignersStorePassword, sawSigner);
        } catch (final Exception e) {
            throw new SignatureException("Failed to verify signer certificate", e);
        }
    }

    /**
     * Validates signature and also verifies that signer cert is trusted.
     *
     * @param content               {@code InputStream} of the file content to check signature.  Required and cannot be {@code null}.
     * @param signatureProperties   A {@code String} with the signature properties, as produced by the signer tool.
     *                              Optional and can be {@code null} if module is not signed.
     * @throws SignatureException if signature cannot be validated or signer cert is not trusted.
     */
    @Override
    public void verify(@NotNull final InputStream content, @Nullable final String signatureProperties) throws SignatureException {
        // if no signature provided throw
        if (StringUtils.isBlank(signatureProperties)) {
            throw new SignatureException("Module is not signed");
        }

        // get or compute digest
        final byte[] computedDigest;
        // if content is provided calculate digest
        try {
            final DigestInputStream dis = new DigestInputStream(content, MessageDigest.getInstance("SHA-256"));
            IOUtils.copyStream(dis, new NullOutputStream());
            computedDigest = dis.getMessageDigest().digest();
        } catch (final NoSuchAlgorithmException | IOException e) {
            throw new SignatureException("Failed to calculate content digest", e);
        }

        verify(computedDigest, signatureProperties);
    }
}
