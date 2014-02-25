package com.l7tech.server.search.processors;

import com.l7tech.objectmodel.EntityHeader;
import com.l7tech.objectmodel.FindException;
import com.l7tech.objectmodel.SecurityZone;
import com.l7tech.policy.Policy;
import com.l7tech.policy.assertion.Assertion;
import com.l7tech.server.EntityHeaderUtils;
import com.l7tech.server.search.DependencyAnalyzer;
import com.l7tech.server.search.exceptions.CannotReplaceDependenciesException;
import com.l7tech.server.search.exceptions.CannotRetrieveDependenciesException;
import com.l7tech.server.search.objects.Dependency;
import com.l7tech.server.security.rbac.SecurityZoneManager;
import com.l7tech.util.EmptyIterator;
import com.l7tech.util.ExceptionUtils;
import com.l7tech.util.Functions;
import org.jetbrains.annotations.NotNull;

import javax.inject.Inject;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * This will find the dependencies that a policy has. It will iterate through all the assertion in the policy and add
 * them as dependencies.
 *
 * @author Victor Kazakov
 */
public class PolicyDependencyProcessor extends GenericDependencyProcessor<Policy> implements DependencyProcessor<Policy> {

    @Inject
    private SecurityZoneManager securityZoneManager;

    /**
     * Finds the dependencies in a policy by looking at the assertions contained in the policy
     *
     * @throws com.l7tech.objectmodel.FindException
     *
     */
    @NotNull
    @Override
    public List<Dependency> findDependencies(Policy policy, DependencyFinder processor) throws FindException {
        final Assertion assertion;
        try {
            assertion = policy.getAssertion();
        } catch (IOException e) {
            throw new RuntimeException("Invalid policy with id " + policy.getGuid() + ": " + ExceptionUtils.getMessage(e), e);
        }

        final ArrayList<Dependency> dependencies = new ArrayList<>();
        final Iterator assit = assertion != null ? assertion.preorderIterator() : new EmptyIterator();

        boolean assertionsAsDependencies = processor.getOption(DependencyAnalyzer.ReturnAssertionsAsDependenciesOptionKey, Boolean.class, true);

        //iterate for each assertion.
        while (assit.hasNext()) {
            final Assertion currentAssertion = (Assertion) assit.next();
            if (assertionsAsDependencies) {
                Dependency dependency = processor.getDependency(currentAssertion);
                if(dependency != null) {
                    dependencies.add(dependency);
                }
            } else {
                //for all the dependencies in the assertion if the dependency is not already found add it to the list of dependencies.
                Functions.forall(processor.getDependencies(currentAssertion), new Functions.Unary<Boolean, Dependency>() {
                    @Override
                    public Boolean call(Dependency dependency) {
                        if (!dependencies.contains(dependency))
                            dependencies.add(dependency);
                        return true;
                    }
                });
            }
        }

        SecurityZone securityZone = policy.getSecurityZone();
        if (securityZone != null) {
            final Dependency securityZoneDependency = processor.getDependency(securityZone);
            if (securityZoneDependency != null && !dependencies.contains(securityZoneDependency))
                dependencies.add(securityZoneDependency);
        }

        return dependencies;
    }

    @Override
    public void replaceDependencies(@NotNull Policy policy, @NotNull Map<EntityHeader, EntityHeader> replacementMap, DependencyFinder finder) throws CannotRetrieveDependenciesException, CannotReplaceDependenciesException {
        final Assertion assertion;
        try {
            assertion = policy.getAssertion();
        } catch (IOException e) {
            throw new RuntimeException("Invalid policy with id " + policy.getGuid() + ": " + ExceptionUtils.getMessage(e), e);
        }

        final Iterator assit = assertion != null ? assertion.preorderIterator() : new EmptyIterator();

        //iterate for each assertion.
        while (assit.hasNext()) {
            final Assertion currentAssertion = (Assertion) assit.next();
            //replace dependencies in each assertion
            finder.replaceDependencies(currentAssertion, replacementMap);
        }
        try {
            //This will recreate the policy xml if any of the assertions were updated.
            policy.flush();
        } catch (IOException e) {
            throw new RuntimeException("Invalid policy with id " + policy.getGuid() + ": " + ExceptionUtils.getMessage(e), e);
        }

        //replace the security zone
        SecurityZone securityZone = policy.getSecurityZone();
        if (securityZone != null) {
            EntityHeader securityZoneHeaderToUse = replacementMap.get(EntityHeaderUtils.fromEntity(securityZone));
            if(securityZoneHeaderToUse != null) {
                try {
                    securityZone = securityZoneManager.findByHeader(securityZoneHeaderToUse);
                } catch (FindException e) {
                    throw new CannotRetrieveDependenciesException(securityZoneHeaderToUse.getName(), SecurityZone.class, policy.getClass(), "Cannot find security zone", e);
                }
                policy.setSecurityZone(securityZone);
            }
        }
    }
}
