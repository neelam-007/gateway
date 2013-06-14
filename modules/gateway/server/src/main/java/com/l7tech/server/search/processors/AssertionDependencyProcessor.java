package com.l7tech.server.search.processors;

import com.l7tech.gateway.common.cluster.ClusterProperty;
import com.l7tech.gateway.common.resources.ResourceEntry;
import com.l7tech.objectmodel.Entity;
import com.l7tech.objectmodel.EntityHeader;
import com.l7tech.objectmodel.FindException;
import com.l7tech.policy.AssertionResourceInfo;
import com.l7tech.policy.assertion.*;
import com.l7tech.server.cluster.ClusterPropertyManager;
import com.l7tech.server.globalresources.ResourceEntryManager;
import com.l7tech.server.search.objects.Dependency;
import com.l7tech.server.search.objects.DependentAssertion;
import com.l7tech.server.search.objects.DependentObject;
import org.jetbrains.annotations.NotNull;

import javax.inject.Inject;
import java.util.List;

/**
 * The assertion dependencyProcessor finds the dependencies that an assertions has.
 *
 * @author Victor Kazakov
 */
public class AssertionDependencyProcessor extends GenericDependencyProcessor<Assertion> implements DependencyProcessor<Assertion> {

    @Inject
    private ClusterPropertyManager clusterPropertyManager;

    @Inject
    private ResourceEntryManager resourceEntryManager;

    /**
     * Finds the dependencies that an assertion has. First finds the dependencies by looking at the methods defined by
     * this assertion. If the assertion implements {@link UsesEntities} then the entities returned by getEntitiesUsed
     * method are assumed to be dependencies. If the assertion implements {@link UsesVariables} then all the variables
     * used that are cluster properties are returned as dependencies
     *
     * @param assertion The assertion to find dependencies for.
     * @param finder    The finder that if performing the current dependency search
     * @return The list of dependencies that this assertion has
     * @throws FindException This is thrown if an entity cannot be found
     */
    @NotNull
    @Override
    public List<Dependency> findDependencies(Assertion assertion, DependencyFinder finder) throws FindException {
        //uses the generic dependency processor to find dependencies using the methods defined by the assertion.
        final List<Dependency> dependencies = super.findDependencies(assertion, finder);

        //if the assertion implements UsesEntities then use the getEntitiesUsed method to find the entities used by the assertion.
        if (assertion instanceof UsesEntities) {
            for (EntityHeader header : ((UsesEntities) assertion).getEntitiesUsed()) {
                final Entity entity = loadEntity(header);
                if (entity != null) {
                    Dependency dependency = finder.getDependency(entity);
                    if (!dependencies.contains(dependency))
                        dependencies.add(dependency);
                }
            }
        }
        //If the assertion implements UsesVariables then all cluster properties used be the assertion as considered to be dependencies.
        if (assertion instanceof UsesVariables) {
            for (String variable : ((UsesVariables) assertion).getVariablesUsed()) {
                ClusterProperty property = clusterPropertyManager.findByUniqueName(variable);
                if (property != null) {
                    Dependency dependency = finder.getDependency(property);
                    if (!dependencies.contains(dependency))
                        dependencies.add(dependency);
                }
            }
        }
        //If the assertion implements UsesResourceInfo then add the used resource as a dependent.
        if (assertion instanceof UsesResourceInfo) {
            AssertionResourceInfo assertionResourceInfo = ((UsesResourceInfo) assertion).getResourceInfo();
            if (assertionResourceInfo != null && assertionResourceInfo.getType().equals(AssertionResourceType.GLOBAL_RESOURCE)) {
                String uri = ((GlobalResourceInfo) assertionResourceInfo).getId();
                //Passing null as the resource type should be ok as resources as unique by uri anyways.
                @SuppressWarnings("NullableProblems")
                ResourceEntry resourceEntry = resourceEntryManager.findResourceByUriAndType(uri, null);
                if (resourceEntry != null) {
                    Dependency dependency = finder.getDependency(resourceEntry);
                    if (!dependencies.contains(dependency))
                        dependencies.add(dependency);
                }
            }
        }

        return dependencies;
    }

    @Override
    public DependentObject createDependentObject(Assertion assertion) {
        final AssertionNodeNameFactory assertionNodeNameFactory = (AssertionNodeNameFactory) assertion.meta().get(AssertionMetadata.POLICY_NODE_NAME_FACTORY);
        //noinspection unchecked
        return new DependentAssertion((String) assertion.meta().get(AssertionMetadata.SHORT_NAME), assertionNodeNameFactory != null ? assertionNodeNameFactory.getAssertionName(assertion, true) : null);
    }

    /**
     * This throws an exception. It should not be called. Assertions cannot be found the same way other entities can.
     */
    @SuppressWarnings("unchecked")
    public List<? extends Entity> find(@NotNull Object searchValue, com.l7tech.search.Dependency dependency) {
        throw new UnsupportedOperationException("Assertions cannot be loaded as entities");
    }
}
