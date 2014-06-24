package com.l7tech.server.search.processors;

import com.l7tech.objectmodel.EntityHeader;
import com.l7tech.objectmodel.EntityType;
import com.l7tech.objectmodel.FindException;
import com.l7tech.objectmodel.Goid;
import com.l7tech.policy.assertion.xmlsec.WsSecurity;
import com.l7tech.security.cert.TrustedCert;
import com.l7tech.security.cert.TrustedCertManager;
import com.l7tech.server.search.exceptions.CannotReplaceDependenciesException;
import com.l7tech.server.search.exceptions.CannotRetrieveDependenciesException;
import com.l7tech.server.search.objects.Dependency;
import com.l7tech.util.CollectionUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * Dependency processor for WsSecurity Assertion
 */
public class AssertionWsSecurityProcessor extends DefaultAssertionDependencyProcessor<WsSecurity> implements DependencyProcessor<WsSecurity> {

    @Inject
    private TrustedCertManager trustedCertManager;

    @NotNull
    @Override
    public List<Dependency> findDependencies(@NotNull final WsSecurity assertion, @NotNull final DependencyFinder finder) throws FindException, CannotRetrieveDependenciesException {
        super.findDependencies(assertion,finder);

        final TrustedCert certificate = getCertificateUsed(assertion);

        final ArrayList<Dependency> dependencies = new ArrayList<>();
        if (certificate != null) {
            dependencies.addAll(finder.getDependenciesFromObjects(assertion, finder, CollectionUtils.<Object>list(certificate)));
        }
        return dependencies;
    }

    @Nullable
    private TrustedCert getCertificateUsed(@NotNull final WsSecurity assertion) throws FindException, CannotRetrieveDependenciesException {
        try {
            if (assertion.getRecipientTrustedCertificateGoid() != null) {
                return trustedCertManager.findByPrimaryKey(assertion.getRecipientTrustedCertificateGoid());
            } else if (assertion.getRecipientTrustedCertificateName() != null) {
                final Collection<TrustedCert> trustedCertificates = trustedCertManager.findByName(assertion.getRecipientTrustedCertificateName());
                if (trustedCertificates != null && !trustedCertificates.isEmpty()) {
                    return trustedCertificates.iterator().next();
                }
            }
        } catch (IllegalArgumentException e) {
            // do nothing, certificate not found
        }
        return null;
    }

    @Override
    public void replaceDependencies(@NotNull final WsSecurity assertion, @NotNull final Map<EntityHeader, EntityHeader> replacementMap, @NotNull final DependencyFinder finder, final boolean replaceAssertionsDependencies) throws CannotReplaceDependenciesException {
        if(!replaceAssertionsDependencies) return;

        super.replaceDependencies(assertion,replacementMap,finder,replaceAssertionsDependencies);
        if (assertion.getRecipientTrustedCertificateGoid() != null) {
            final EntityHeader[] entitiesUsed = assertion.getEntitiesUsed();
            for (EntityHeader entityUsed : entitiesUsed) {
                EntityHeader newEntity = findMappedHeader(replacementMap, entityUsed);
                if (newEntity != null) {
                    assertion.replaceEntity(entityUsed, newEntity);
                }
            }
        }else if(assertion.getRecipientTrustedCertificateName() != null){
            final EntityHeader entityUsed = new EntityHeader(Goid.DEFAULT_GOID, EntityType.TRUSTED_CERT,assertion.getRecipientTrustedCertificateName(),"");
            final EntityHeader newEntity = findMappedHeader(replacementMap, entityUsed);
            if(newEntity!=null){
                assertion.setRecipientTrustedCertificateName(newEntity.getName());
            }
        }
    }
}
