package com.l7tech.server.search.processors;

import com.l7tech.objectmodel.EntityHeader;
import com.l7tech.objectmodel.FindException;
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
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * Dependency processor for LookupTrustedCertificateAssertion
 */
public class AssertionLookupTrustedCertificateProcessor extends DefaultAssertionDependencyProcessor<LookupTrustedCertificateAssertion> implements DependencyProcessor<LookupTrustedCertificateAssertion> {

    @Inject
    private TrustedCertManager trustedCertManager;

    @NotNull
    @Override
    public List<Dependency> findDependencies(@NotNull final LookupTrustedCertificateAssertion assertion, @NotNull final DependencyFinder finder) throws FindException, CannotRetrieveDependenciesException {
        //TODO should the super.findDependencies be called here?

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
            //TODO: does this need to be done?
            throw new CannotRetrieveDependenciesException(TrustedCert.class, assertion.getClass(), "", e);
        }
    }

    @Override
    public void replaceDependencies(@NotNull final LookupTrustedCertificateAssertion assertion, @NotNull final Map<EntityHeader, EntityHeader> replacementMap, @NotNull final DependencyFinder finder) throws CannotReplaceDependenciesException {
        // todo ???
//        try {
//            Collection<TrustedCert> certificates = // what is it dependent on from the original gateway?????
//            if(certificates.isEmpty()) return;
//            if(certificates.size()>1) {
//                logger.info("Dependency not replaced for LookupTrustedCertificateAssertion, more then one certificate found"); // todo
//                return;
//            }
//            EntityHeader replacementHeader = replacementMap.get(EntityHeaderUtils.fromEntity(CollectionUtils.toList(certificates).get(0)));
//            if(replacementHeader == null) return;
//            TrustedCert cert = (TrustedCert)loadEntity(replacementHeader);
//
//            switch (assertion.getLookupType()) {
//                case CERT_ISSUER_SERIAL:
//                    assertion.setCertIssuerDn(cert.getIssuerDn());
//                    assertion.setCertSerialNumber(cert.getSerial().toString());
//                    break;
//
//                case CERT_SKI:
//                    assertion.setCertSubjectKeyIdentifier(cert.getSki());
//                    break;
//
//                case CERT_SUBJECT_DN:
//                    assertion.setCertSubjectDn(cert.getSubjectDn());
//                    break;
//
//                case CERT_THUMBPRINT_SHA1:
//                    assertion.setCertThumbprintSha1(cert.getThumbprintSha1());
//                    break;
//
//                case TRUSTED_CERT_NAME:
//                default:
//                    assertion.setTrustedCertificateName(cert.getName());
//                    break;
//            }
//
//        } catch (FindException e) {
//            // todo
//        }
    }
}
