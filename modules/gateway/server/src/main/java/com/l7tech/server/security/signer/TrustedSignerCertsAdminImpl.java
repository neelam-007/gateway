package com.l7tech.server.security.signer;

import com.l7tech.gateway.common.security.signer.TrustedSignerCertsAdmin;
import com.l7tech.gateway.common.security.signer.TrustedSignerCertsManager;
import com.l7tech.objectmodel.FindException;
import org.jetbrains.annotations.NotNull;

import javax.inject.Inject;
import javax.inject.Named;
import java.security.cert.X509Certificate;
import java.util.Collection;

/**
 * {@code TrustedSignerCertsAdmin} implementation.<br/>
 * Simply proxies over {@code TrustedSignerCertsManager}.
 */
public class TrustedSignerCertsAdminImpl implements TrustedSignerCertsAdmin {

    /**
     * Our proxy {@link com.l7tech.gateway.common.security.signer.TrustedSignerCertsManager}.
     */
    @NotNull
    private TrustedSignerCertsManager trustedSignerCertsManager;

    @NotNull
    @Override
    public Collection<X509Certificate> lookUpTrustedSigningCertsForServerModuleFiles() throws FindException {
        return internalLookUpTrustedSigningCerts();
    }

    @NotNull
    @Override
    public Collection<X509Certificate> lookUpTrustedSigningCertsForSolutionKits() throws FindException {
        return internalLookUpTrustedSigningCerts();
    }

    private Collection<X509Certificate> internalLookUpTrustedSigningCerts() throws FindException {
        return trustedSignerCertsManager.lookUpTrustedSigningCerts();
    }

    @Inject
    @Named("trustedSignerCertsManager")
    public void setTrustedSignerCertsManager(@NotNull final TrustedSignerCertsManager trustedSignerCertsManager) {
        this.trustedSignerCertsManager = trustedSignerCertsManager;
    }
}
