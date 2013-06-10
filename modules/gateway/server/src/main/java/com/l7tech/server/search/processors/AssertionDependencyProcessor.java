package com.l7tech.server.search.processors;

import com.l7tech.gateway.common.cluster.ClusterProperty;
import com.l7tech.objectmodel.Entity;
import com.l7tech.objectmodel.EntityHeader;
import com.l7tech.objectmodel.FindException;
import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.assertion.UsesEntities;
import com.l7tech.policy.assertion.UsesVariables;
import com.l7tech.server.EntityCrud;
import com.l7tech.server.cluster.ClusterPropertyManager;
import com.l7tech.server.search.objects.Dependency;
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
    private EntityCrud entityCrud;

    @Inject
    private ClusterPropertyManager clusterPropertyManager;

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
                final Entity entity = entityCrud.find(header);
                if (entity != null) {
                    Dependency dependency = finder.getDependencyHelper(entity);
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
                    Dependency dependency = finder.getDependencyHelper(property);
                    if (!dependencies.contains(dependency))
                        dependencies.add(dependency);
                }
            }
        }

        return dependencies;
    }

    /**
     * This throws an exception. It should not be called. Assertions cannot be found the same way other entities can.
     */
    @SuppressWarnings("unchecked")
    public Entity find(@NotNull Object searchValue, com.l7tech.search.Dependency dependency) {
        throw new UnsupportedOperationException("Assertions cannot be loaded as entities");
    }
}
