package com.l7tech.gateway.common.security.signer;

import com.l7tech.objectmodel.FindException;
import org.jetbrains.annotations.NotNull;

import java.security.cert.X509Certificate;
import java.util.Collection;
import java.util.Collections;

/**
 * Simple {@link TrustedSignerCertsManager} utility class.
 */
public class TrustedSignerCertsHelper {
    /**
     * Utility method for getting trusted certs and throws RuntimeException on error.
     */
    @NotNull
    public static Collection<X509Certificate> getTrustedCertificates(@NotNull final TrustedSignerCertsManager manager) {
        try {
            return Collections.unmodifiableCollection(manager.lookUpTrustedSigningCerts());
        } catch (final FindException e) {
            // shouldn't happen so throw a RuntimeException
            throw new RuntimeException(e);
        }
    }

    /**
     * Utility method for getting trusted certs and throws RuntimeException on error.
     *
     * @see com.l7tech.gateway.common.security.signer.TrustedSignerCertsAdmin#lookUpTrustedSigningCertsForServerModuleFiles()
     */
    @NotNull
    public static Collection<X509Certificate> getTrustedCertificatesForServerModuleFiles(@NotNull final TrustedSignerCertsAdmin admin) {
        try {
            return Collections.unmodifiableCollection(admin.lookUpTrustedSigningCertsForServerModuleFiles());
        } catch (final FindException e) {
            // shouldn't happen so throw a RuntimeException
            throw new RuntimeException(e);
        }
    }

    /**
     * Utility method for getting trusted certs and throws RuntimeException on error.
     *
     * @see com.l7tech.gateway.common.security.signer.TrustedSignerCertsAdmin#lookUpTrustedSigningCertsForSolutionKits()
     */
    @NotNull
    public static Collection<X509Certificate> getTrustedCertificatesForSolutionKits(@NotNull final TrustedSignerCertsAdmin admin) {
        try {
            return Collections.unmodifiableCollection(admin.lookUpTrustedSigningCertsForSolutionKits());
        } catch (final FindException e) {
            // shouldn't happen so throw a RuntimeException
            throw new RuntimeException(e);
        }
    }
}
