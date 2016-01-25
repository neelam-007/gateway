package com.l7tech.server.security.signer;

import com.l7tech.gateway.common.security.signer.SignerUtils;
import com.l7tech.gateway.common.security.signer.TrustedSignerCertsManager;
import com.l7tech.objectmodel.FindException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Collection;
import java.util.Collections;

/**
 * Implementation of {@link com.l7tech.gateway.common.security.signer.TrustedSignerCertsManager}
 */
public class TrustedSignerCertsManagerImpl implements TrustedSignerCertsManager {

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
    TrustedSignerCertsManagerImpl(
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

    @Override
    public Collection<X509Certificate> lookUpTrustedSigningCerts() throws FindException {
        try {
            return Collections.unmodifiableCollection(
                    SignerUtils.loadTrustedCertsFromTrustStore(
                            trustedSignersStore,
                            trustedSignersStoreType,
                            trustedSignersStorePassword
                    )
            );
        } catch (IOException | CertificateException | NoSuchAlgorithmException | KeyStoreException e) {
            throw new FindException("Failed to load trusted certs from the configured key store", e);
        }
    }
}
