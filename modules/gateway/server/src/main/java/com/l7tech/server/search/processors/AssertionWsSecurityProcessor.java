package com.l7tech.server.search.processors;

import com.l7tech.objectmodel.EntityHeader;
import com.l7tech.objectmodel.FindException;
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
        //TODO should the super.findDependencies be called here?

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
            //TODO: does this need to be done?
            throw new CannotRetrieveDependenciesException(TrustedCert.class, assertion.getClass(), "", e);
        }
        return null;
    }

    @Override
    public void replaceDependencies(@NotNull final WsSecurity assertion, @NotNull final Map<EntityHeader, EntityHeader> replacementMap, @NotNull final DependencyFinder finder) throws CannotRetrieveDependenciesException, CannotReplaceDependenciesException {
        // todo for reference by name?
        //todo: should super.replaceDependencies be called?
        // only replace referencing by goid for now.
        if (assertion.getRecipientTrustedCertificateGoid() != null) {
            final EntityHeader[] entitiesUsed = assertion.getEntitiesUsed();
            for (EntityHeader entityUsed : entitiesUsed) {
                EntityHeader newEntity = findMappedHeader(replacementMap, entityUsed);
                if (newEntity != null) {
                    assertion.replaceEntity(entityUsed, newEntity);
                }
            }
        }
    }
}
