package com.l7tech.gateway.common.security.signer;

import com.l7tech.gateway.common.admin.Administrative;
import com.l7tech.gateway.common.security.rbac.MethodStereotype;
import com.l7tech.gateway.common.security.rbac.Secured;
import com.l7tech.objectmodel.EntityType;
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
     * The requirement is to make the trusted signer cert list available to any admin user that has permission
     * to CREATE or UPDATE an entity of type Solution Kit or Server Module File.<br/>
     * To avoid creating a CustomRbacInterceptor to cover both Solution Kit or Server Module File,
     * we are going to have two distinct methods for each entity.
     * <p/>
     * This method is dedicated for Server Module File i.e. for {@link EntityType#SERVER_MODULE_FILE} RBAC.
     *
     * @return a collection containing every X.509 certificate from a trusted certificate entry within the trust store, never {@code null}.
     * @throws FindException if an error happens while loading trusted certs from the Gateway's configured trust store file.
     * @see TrustedSignerCertsManager#lookUpTrustedSigningCerts()
     */
    @NotNull
    @Administrative(background = true)
    @Secured(types = EntityType.SERVER_MODULE_FILE, stereotype = MethodStereotype.SAVE_OR_UPDATE)
    public Collection<X509Certificate> lookUpTrustedSigningCertsForServerModuleFiles() throws FindException;

    /**
     * Get a read-only list of all trusted certificates from Gateway's configured trust store file.
     * <p/>
     * The requirement is to make the trusted signer cert list available to any admin user that has permission
     * to CREATE or UPDATE an entity of type Solution Kit or Server Module File.<br/>
     * To avoid creating a CustomRbacInterceptor to cover both Solution Kit or Server Module File,
     * we are going to have two distinct methods for each entity.
     * <p/>
     * This method is dedicated for Solution Kit i.e. for {@link EntityType#SOLUTION_KIT} RBAC.
     *
     *
     * @return a collection containing every X.509 certificate from a trusted certificate entry within the trust store, never {@code null}.
     * @throws FindException if an error happens while loading trusted certs from the Gateway's configured trust store file.
     * @see TrustedSignerCertsManager#lookUpTrustedSigningCerts()
     */
    @NotNull
    @Administrative(background = true)
    @Secured(types = EntityType.SOLUTION_KIT, stereotype = MethodStereotype.SAVE_OR_UPDATE)
    public Collection<X509Certificate> lookUpTrustedSigningCertsForSolutionKits() throws FindException;

    // TODO: if more entities require signature (i.e. needs trusted signer cert list) consider creating dedicated CustomRbacInterceptor
}
