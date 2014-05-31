package com.l7tech.server.search.processors;

import com.l7tech.objectmodel.EntityHeader;
import com.l7tech.objectmodel.FindException;
import com.l7tech.policy.assertion.AssertionMetadata;
import com.l7tech.policy.assertion.xmlsec.LookupTrustedCertificateAssertion;
import com.l7tech.security.cert.TrustedCert;
import com.l7tech.security.cert.TrustedCertManager;
import com.l7tech.server.search.exceptions.CannotReplaceDependenciesException;
import com.l7tech.server.search.exceptions.CannotRetrieveDependenciesException;
import com.l7tech.server.search.objects.Dependency;
import com.l7tech.server.search.objects.DependentAssertion;
import com.l7tech.server.search.objects.DependentObject;
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
public class AssertionLookupTrustedCertificateProcessor extends DefaultDependencyProcessor<LookupTrustedCertificateAssertion> implements DependencyProcessor<LookupTrustedCertificateAssertion> {

    @Inject
    private TrustedCertManager trustedCertManager;

    @NotNull
    @Override
    public List<Dependency> findDependencies(@NotNull LookupTrustedCertificateAssertion assertion, @NotNull DependencyFinder finder) throws FindException {
        Collection<TrustedCert> certificates = getCertificatesUsed(assertion);

        ArrayList<Dependency> dependencies = new ArrayList<>();
        if(!certificates.isEmpty()){
            dependencies.addAll(finder.getDependenciesFromObjects(assertion, finder, CollectionUtils.<Object>toList(certificates)));
        }
        return dependencies;
    }

    private Collection<TrustedCert> getCertificatesUsed(LookupTrustedCertificateAssertion assertion) throws FindException {
        final Collection<TrustedCert> certificates;
        try {
            switch (assertion.getLookupType()) {
                case CERT_ISSUER_SERIAL:
                    certificates = trustedCertManager.findByIssuerAndSerial(new X500Principal(assertion.getCertIssuerDn()), new BigInteger(assertion.getCertSerialNumber()));
                    break;

                case CERT_SKI:
                    certificates = trustedCertManager.findBySki(assertion.getCertSubjectKeyIdentifier());
                    break;

                case CERT_SUBJECT_DN:
                    certificates = trustedCertManager.findBySubjectDn(assertion.getCertSubjectDn());
                    break;

                case CERT_THUMBPRINT_SHA1:
                    certificates = trustedCertManager.findByThumbprint(assertion.getCertThumbprintSha1());
                    break;

                case TRUSTED_CERT_NAME:
                default:
                    certificates = trustedCertManager.findByName(assertion.getTrustedCertificateName());
                    break;
            }

            return certificates;
        }catch(IllegalArgumentException e){
            // do nothing
        }
        return new ArrayList<>();
    }

    @NotNull
    @Override
    public DependentObject createDependentObject(@NotNull LookupTrustedCertificateAssertion assertion) {
        return new DependentAssertion<>((String) assertion.meta().get(AssertionMetadata.SHORT_NAME), assertion.getClass());
    }

    @Override
    public void replaceDependencies(@NotNull LookupTrustedCertificateAssertion assertion, @NotNull Map<EntityHeader, EntityHeader> replacementMap, @NotNull DependencyFinder finder) throws CannotRetrieveDependenciesException, CannotReplaceDependenciesException {
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
