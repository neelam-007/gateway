package com.l7tech.server.security.signer;

import com.l7tech.gateway.common.security.signer.TrustedSignerCertsManager;
import com.l7tech.objectmodel.FindException;
import org.jetbrains.annotations.NotNull;

import java.security.cert.X509Certificate;
import java.util.Collection;

/**
 * <p>Wrapper for {@code TrustedSignerCertsManager}.<br/>
 * Basically acts as a proxy class, allowing you to set the real {@code SignatureVerifier},
 * using the {@link #setProxyManager(TrustedSignerCertsManager)}.</p>
 *
 * Throws {@code IllegalStateException} if the proxy object {@link #proxyManager} is not set.
 */
public class TrustedSignerCertsManagerWrapper implements TrustedSignerCertsManager {

    private TrustedSignerCertsManager proxyManager;

    @Override
    public Collection<X509Certificate> lookUpTrustedSigningCerts() throws FindException {
        if (proxyManager == null) {
            throw new IllegalStateException("proxyManager not set");
        }
        return proxyManager.lookUpTrustedSigningCerts();
    }

    public void setProxyManager(@NotNull final TrustedSignerCertsManager proxyManager) {
        this.proxyManager = proxyManager;
    }
}
