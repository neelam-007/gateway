package com.l7tech.server.search.processors;

import com.l7tech.objectmodel.EntityHeader;
import com.l7tech.objectmodel.EntityType;
import com.l7tech.objectmodel.FindException;
import com.l7tech.objectmodel.polback.PolicyBackedService;
import com.l7tech.objectmodel.polback.PolicyBackedServiceOperation;
import com.l7tech.policy.Policy;
import com.l7tech.server.policy.PolicyManager;
import com.l7tech.server.search.exceptions.CannotReplaceDependenciesException;
import com.l7tech.server.search.exceptions.CannotRetrieveDependenciesException;
import com.l7tech.server.search.objects.Dependency;
import org.jetbrains.annotations.NotNull;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * This is used to find PolicyBackedService's dependencies
 *
 * @author vkazakov
 */
public class PolicyBackedServiceDependencyProcessor extends DefaultDependencyProcessor<PolicyBackedService> implements DependencyProcessor<PolicyBackedService> {

    @Inject
    private PolicyManager policyManager;

    @NotNull
    @Override
    public List<Dependency> findDependencies(@NotNull PolicyBackedService policyBackedService, @NotNull DependencyFinder finder) throws FindException, CannotRetrieveDependenciesException {
        final ArrayList<Dependency> dependencies = new ArrayList<>();
        dependencies.addAll(super.findDependencies(policyBackedService, finder));

        //find the policies for this PBS
        for(PolicyBackedServiceOperation policyBackedServiceOperation : policyBackedService.getOperations()) {
            Policy policy = policyManager.findByPrimaryKey(policyBackedServiceOperation.getPolicyGoid());
            Dependency dependency = finder.getDependency(DependencyFinder.FindResults.create(policy, null));
            if (dependency != null) {
                dependencies.add(dependency);
            }
        }

        return dependencies;
    }

    @Override
    public void replaceDependencies(@NotNull PolicyBackedService policyBackedService, @NotNull Map<EntityHeader, EntityHeader> replacementMap, @NotNull DependencyFinder finder, boolean replaceAssertionsDependencies) throws CannotReplaceDependenciesException {
        //replace mapped policies for this pbs
        for(PolicyBackedServiceOperation policyBackedServiceOperation : policyBackedService.getOperations()) {
            EntityHeader policyHeader = new EntityHeader(policyBackedServiceOperation.getPolicyGoid(), EntityType.POLICY, null, null);
            EntityHeader replace = replacementMap.get(policyHeader);
            if(replace != null){
                policyBackedServiceOperation.setPolicyGoid(replace.getGoid());
            }
        }

        super.replaceDependencies(policyBackedService, replacementMap, finder, replaceAssertionsDependencies);
    }
}
