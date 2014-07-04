package com.l7tech.server.search.processors;

import com.l7tech.objectmodel.EntityHeader;
import com.l7tech.objectmodel.EntityType;
import com.l7tech.objectmodel.FindException;
import com.l7tech.objectmodel.Goid;
import com.l7tech.policy.assertion.xmlsec.LookupTrustedCertificateAssertion;
import com.l7tech.security.cert.TrustedCert;
import com.l7tech.security.cert.TrustedCertManager;
import com.l7tech.server.search.exceptions.CannotReplaceDependenciesException;
import com.l7tech.server.search.exceptions.CannotRetrieveDependenciesException;
import com.l7tech.server.search.objects.Dependency;
import com.l7tech.util.CollectionUtils;
import org.jetbrains.annotations.NotNull;

import javax.inject.Inject;
import javax.security.auth.x500.X500Principal;
import java.math.BigInteger;
import java.util.*;

/**
 * Dependency processor for LookupTrustedCertificateAssertion
 */
public class AssertionLookupTrustedCertificateProcessor implements DependencyProcessor<LookupTrustedCertificateAssertion> {

    @Inject
    private TrustedCertManager trustedCertManager;

    @NotNull
    @Override
    public List<Dependency> findDependencies(@NotNull final LookupTrustedCertificateAssertion assertion, @NotNull final DependencyFinder finder) throws FindException, CannotRetrieveDependenciesException {
        final Collection<TrustedCert> certificates = getCertificatesUsed(assertion);

        final ArrayList<Dependency> dependencies = new ArrayList<>();
        if (!certificates.isEmpty()) {
            dependencies.addAll(finder.getDependenciesFromObjects(assertion, finder, CollectionUtils.<Object>toList(certificates)));
        }
        return dependencies;
    }

    @NotNull
    private Collection<TrustedCert> getCertificatesUsed(@NotNull final LookupTrustedCertificateAssertion assertion) throws FindException, CannotRetrieveDependenciesException {
        try {
            switch (assertion.getLookupType()) {
                case CERT_ISSUER_SERIAL:
                    return trustedCertManager.findByIssuerAndSerial(new X500Principal(assertion.getCertIssuerDn()), new BigInteger(assertion.getCertSerialNumber()));
                case CERT_SKI:
                    return trustedCertManager.findBySki(assertion.getCertSubjectKeyIdentifier());
                case CERT_SUBJECT_DN:
                    return trustedCertManager.findBySubjectDn(assertion.getCertSubjectDn());
                case CERT_THUMBPRINT_SHA1:
                    return trustedCertManager.findByThumbprint(assertion.getCertThumbprintSha1());
                case TRUSTED_CERT_NAME:
                default:
                    return trustedCertManager.findByName(assertion.getTrustedCertificateName());
            }
        } catch (IllegalArgumentException e) {
            // do nothing, certificate not found
            return Collections.emptyList();
        }
    }

    @Override
    public void replaceDependencies(@NotNull final LookupTrustedCertificateAssertion assertion, @NotNull final Map<EntityHeader, EntityHeader> replacementMap, @NotNull final DependencyFinder finder, final boolean replaceAssertionsDependencies) throws CannotReplaceDependenciesException {
        if(!replaceAssertionsDependencies) return;

        // only do replace dependency for lookup by name
        if(assertion.getLookupType().equals(LookupTrustedCertificateAssertion.LookupType.TRUSTED_CERT_NAME) ){
            final EntityHeader entityUsed = new EntityHeader(Goid.DEFAULT_GOID, EntityType.TRUSTED_CERT,assertion.getTrustedCertificateName(),"");
            final EntityHeader newEntity = DependencyProcessorUtils.findMappedHeader(replacementMap, entityUsed);
            if(newEntity!=null){
                assertion.setTrustedCertificateName(newEntity.getName());
            }
        }
    }
}
