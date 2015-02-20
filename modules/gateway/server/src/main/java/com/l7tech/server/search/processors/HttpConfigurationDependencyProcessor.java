package com.l7tech.server.search.processors;

import com.l7tech.gateway.common.resources.HttpConfiguration;
import com.l7tech.gateway.common.security.keystore.SsgKeyEntry;
import com.l7tech.objectmodel.FindException;
import com.l7tech.server.DefaultKey;
import com.l7tech.server.EntityHeaderUtils;
import com.l7tech.server.search.exceptions.CannotRetrieveDependenciesException;
import com.l7tech.server.search.objects.Dependency;
import org.jetbrains.annotations.NotNull;

import javax.inject.Inject;
import java.io.IOException;
import java.util.List;

/**
 * Created by vkazakov on 2/18/2015.
 */
public class HttpConfigurationDependencyProcessor extends DefaultDependencyProcessor<HttpConfiguration> {

    @Inject
    private DefaultKey defaultKey;

    @Override
    @NotNull
    public List<Dependency> findDependencies(@NotNull final HttpConfiguration httpConfiguration, @NotNull final DependencyFinder finder) throws FindException, CannotRetrieveDependenciesException {
        //find the default dependencies
        final List<Dependency> dependencies = super.findDependencies(httpConfiguration, finder);

        //if the httpConfiguration used the default private key add it as a dependency
        if(HttpConfiguration.Option.DEFAULT.equals(httpConfiguration.getTlsKeyUse())) {
            final SsgKeyEntry defaultSslKeyEntry;
            try {
                defaultSslKeyEntry = defaultKey.getSslInfo();
            } catch (IOException e) {
                throw new FindException("Could got det Default ssl Key", e);
            }
            final Dependency dependency = finder.getDependency(DependencyFinder.FindResults.create(defaultSslKeyEntry, EntityHeaderUtils.fromEntity(defaultSslKeyEntry)));
            dependencies.add(dependency);
        }

        return dependencies;
    }
}
