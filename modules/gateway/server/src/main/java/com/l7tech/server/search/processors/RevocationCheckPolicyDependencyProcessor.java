package com.l7tech.server.search.processors;

import com.l7tech.gateway.common.security.RevocationCheckPolicy;
import com.l7tech.gateway.common.security.RevocationCheckPolicyItem;
import com.l7tech.objectmodel.*;
import com.l7tech.security.cert.TrustedCert;
import com.l7tech.security.cert.TrustedCertManager;
import com.l7tech.server.search.exceptions.CannotReplaceDependenciesException;
import com.l7tech.server.search.exceptions.CannotRetrieveDependenciesException;
import com.l7tech.server.search.objects.Dependency;
import com.l7tech.util.CollectionUtils;
import com.l7tech.util.Functions;
import org.jetbrains.annotations.NotNull;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * This is a dependency processor for Revocation check policy
 *
 */
public class RevocationCheckPolicyDependencyProcessor extends DefaultDependencyProcessor<RevocationCheckPolicy>  {

    @Inject
    private TrustedCertManager trustedCertManager;

    @Override
    @NotNull
    public List<Dependency> findDependencies(@NotNull final RevocationCheckPolicy object, @NotNull final DependencyFinder finder) throws FindException, CannotRetrieveDependenciesException {
        final List<Dependency> dependencies = super.findDependencies(object,finder);

        final List<DependencyFinder.FindResults> certs = new ArrayList<>();

        for(RevocationCheckPolicyItem item: object.getRevocationCheckItems()){
            certs.addAll(Functions.map(item.getTrustedSigners(),new Functions.UnaryThrows<DependencyFinder.FindResults, Goid, FindException>() {
                @Override
                public DependencyFinder.FindResults call(final Goid goid) throws FindException {
                    return DependencyFinder.FindResults.create(trustedCertManager.findByPrimaryKey(goid), new EntityHeader(goid,EntityType.TRUSTED_CERT,null,null));
                }
            }));
        }
        if (!certs.isEmpty()) {
            dependencies.addAll(finder.getDependenciesFromObjects(object, finder, certs));
        }

        return dependencies;
    }

    @Override
    public void replaceDependencies(@NotNull final RevocationCheckPolicy object, @NotNull final Map<EntityHeader, EntityHeader> replacementMap, @NotNull final DependencyFinder finder, final boolean replaceAssertionsDependencies) throws CannotReplaceDependenciesException {
        super.replaceDependencies(object, replacementMap, finder, replaceAssertionsDependencies);

        for(RevocationCheckPolicyItem item: object.getRevocationCheckItems()){
            item.setTrustedSigners(Functions.map(item.getTrustedSigners(), new Functions.Unary<Goid, Goid>() {
                @Override
                public Goid call(final Goid goid) {
                    EntityHeader header = new EntityHeader(goid,EntityType.TRUSTED_CERT,"","");
                    EntityHeader replace = replacementMap.get(header);
                    if (replace!=null)
                        return replace.getGoid();
                    return goid;
                }
            }));
        }
    }
}
