package com.l7tech.gateway.common.security.signer;

import com.l7tech.gateway.common.admin.Administrative;
import com.l7tech.gateway.common.security.rbac.MethodStereotype;
import com.l7tech.gateway.common.security.rbac.Secured;
import com.l7tech.objectmodel.FindException;
import org.jetbrains.annotations.NotNull;

import java.security.cert.X509Certificate;
import java.util.Collection;

/**
 * Admin interface for getting a list of all trusted signer certs configured in the Gateway.
 */
@Secured
@Administrative
public interface TrustedSignerCertsAdmin {

    /**
     * Get a read-only list of all trusted certificates from Gateway's configured trust store file.
     * <p/>
     * TODO: Add proper RBAC roles as per SSG-12714:
     * TODO: make the trusted signer cert list available to any admin user that has permission to CREATE or UPDATE an entity of type Solution Kit or Server Module File.
     * TODO: probably needs a CustomRbacInterceptor to cover Solution Kit or Server Module File
     *
     * @return a collection containing every X.509 certificate from a trusted certificate entry within the trust store, never {@code null}.
     * @throws FindException if an error happens while loading trusted certs from the Gateway's configured trust store file.
     * @see TrustedSignerCertsManager#lookUpTrustedSigningCerts()
     */
    @NotNull
    @Administrative(background = true)
    @Secured(stereotype = MethodStereotype.UNCHECKED_WIDE_OPEN)
    public Collection<X509Certificate> lookUpTrustedSigningCerts() throws FindException;
}
