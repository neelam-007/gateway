package com.l7tech.server.search.processors;

import com.l7tech.objectmodel.FindException;
import com.l7tech.policy.Policy;
import com.l7tech.policy.assertion.Assertion;
import com.l7tech.server.search.DependencyAnalyzer;
import com.l7tech.server.search.objects.Dependency;
import com.l7tech.util.EmptyIterator;
import com.l7tech.util.ExceptionUtils;
import com.l7tech.util.Functions;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * This will find the dependencies that a policy has. It will iterate through all the assertion in the policy and add them as dependencies.
 *
 * @author Victor Kazakov
 */
public class PolicyDependencyProcessor extends GenericDependencyProcessor<Policy> implements DependencyProcessor<Policy> {

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

        boolean assertionsAsDependencies = processor.getBooleanOption(DependencyAnalyzer.ReturnAssertionsAsDependenciesOptionKey);

        //iterate for each assertion.
        while (assit.hasNext()) {
            final Assertion currentAssertion = (Assertion) assit.next();
            if(assertionsAsDependencies){
                dependencies.add(processor.getDependency(currentAssertion));
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
        return dependencies;
    }
}
