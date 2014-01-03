package com.l7tech.server.search.processors;

import com.l7tech.gateway.common.service.PublishedService;
import com.l7tech.objectmodel.FindException;
import com.l7tech.server.search.DependencyAnalyzer;
import com.l7tech.server.search.objects.Dependency;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

/**
 * This is used to remove policies as dependencies of services. The policies dependencies will be given to the service.
 *
 * @author Victor Kazakov
 */
public class ServiceDependencyProcessor extends GenericDependencyProcessor<PublishedService> implements DependencyProcessor<PublishedService> {

    @Override
    @NotNull
    public List<Dependency> findDependencies(PublishedService object, DependencyFinder finder) throws FindException {
        List<Dependency> dependencies;
        if (!finder.getOption(DependencyAnalyzer.ReturnServicePoliciesAsDependencies, Boolean.class, false)) {
            List<Dependency> dependenciesFound = super.findDependencies(object, finder);
            dependencies = new ArrayList<>();
            for (Dependency dependency : dependenciesFound) {
                if (com.l7tech.search.Dependency.DependencyType.POLICY.equals(dependency.getDependent().getDependencyType()) && dependency.getDependencies() != null) {
                    for (Dependency policyDependency : dependency.getDependencies()) {
                        dependencies.add(policyDependency);
                    }
                } else {
                    dependencies.add(dependency);
                }
            }
        } else {
            dependencies = super.findDependencies(object, finder);
        }
        return dependencies;
    }
}
