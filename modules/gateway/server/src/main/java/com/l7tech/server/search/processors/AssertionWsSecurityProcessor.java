package com.l7tech.server.search.processors;

import com.l7tech.objectmodel.Entity;
import com.l7tech.objectmodel.EntityHeader;
import com.l7tech.objectmodel.FindException;
import com.l7tech.policy.assertion.AssertionMetadata;
import com.l7tech.policy.assertion.AssertionNodeNameFactory;
import com.l7tech.policy.assertion.xmlsec.WsSecurity;
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
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * Dependency processor for WsSecurity Assertion
 */
public class AssertionWsSecurityProcessor extends GenericDependencyProcessor<WsSecurity> implements DependencyProcessor<WsSecurity> {

    @Inject
    private TrustedCertManager trustedCertManager;

    @NotNull
    @Override
    public List<Dependency> findDependencies(WsSecurity assertion, DependencyFinder finder) throws FindException {
        TrustedCert certificate = getCertificateUsed(assertion);

        ArrayList<Dependency> dependencies = new ArrayList<>();
        if(certificate!=null){
            dependencies.addAll(finder.getDependenciesFromEntities(assertion, finder, CollectionUtils.<Entity>list(certificate)));
        }
        return dependencies;
    }

    private TrustedCert getCertificateUsed(WsSecurity assertion) throws FindException {
        try {
            if(assertion.getRecipientTrustedCertificateGoid()!=null){
                return trustedCertManager.findByPrimaryKey(assertion.getRecipientTrustedCertificateGoid());
            }else if(assertion.getRecipientTrustedCertificateName()!=null){
               Collection<TrustedCert> trustedCertificates =  trustedCertManager.findByName(assertion.getRecipientTrustedCertificateName());
                if(trustedCertificates!= null && !trustedCertificates.isEmpty()){
                    return trustedCertificates.iterator().next();
                }
            }
        }catch(IllegalArgumentException e){
            // do nothing
        }
        return null;
    }

    @Override
    public DependentObject createDependentObject(WsSecurity assertion) {
        final AssertionNodeNameFactory assertionNodeNameFactory = assertion.meta().get(AssertionMetadata.POLICY_NODE_NAME_FACTORY);
        //noinspection unchecked
        return new DependentAssertion((String) assertion.meta().get(AssertionMetadata.SHORT_NAME), assertionNodeNameFactory != null ? assertionNodeNameFactory.getAssertionName(assertion, true) : null);
    }

    @Override
    public void replaceDependencies(@NotNull WsSecurity assertion, @NotNull Map<EntityHeader, EntityHeader> replacementMap, DependencyFinder finder) throws CannotRetrieveDependenciesException, CannotReplaceDependenciesException {
        // todo for reference by name?

        // only replace referencing by goid for now.
        if(assertion.getRecipientTrustedCertificateGoid()!=null) {
            final EntityHeader[] entitiesUsed = assertion.getEntitiesUsed();
            for (EntityHeader entityUsed : entitiesUsed) {
                EntityHeader newEntity = findMappedHeader(replacementMap,entityUsed);
                if (newEntity != null) {
                    assertion.replaceEntity( entityUsed, newEntity);
                }
            }
        }
    }
}
