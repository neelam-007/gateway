package com.l7tech.server.search.processors;

import com.l7tech.gateway.common.cluster.ClusterProperty;
import com.l7tech.gateway.common.resources.ResourceEntry;
import com.l7tech.gateway.common.resources.ResourceEntryHeader;
import com.l7tech.gateway.common.security.password.SecurePassword;
import com.l7tech.objectmodel.Entity;
import com.l7tech.objectmodel.EntityHeader;
import com.l7tech.objectmodel.FindException;
import com.l7tech.objectmodel.SsgKeyHeader;
import com.l7tech.policy.AssertionResourceInfo;
import com.l7tech.policy.assertion.*;
import com.l7tech.server.cluster.ClusterPropertyManager;
import com.l7tech.server.globalresources.ResourceEntryManager;
import com.l7tech.server.search.DependencyAnalyzer;
import com.l7tech.server.search.exceptions.CannotReplaceDependenciesException;
import com.l7tech.server.search.exceptions.CannotRetrieveDependenciesException;
import com.l7tech.server.search.objects.Dependency;
import com.l7tech.server.search.objects.DependentAssertion;
import com.l7tech.server.search.objects.DependentObject;
import com.l7tech.server.security.password.SecurePasswordManager;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.Predicate;
import org.jetbrains.annotations.NotNull;

import javax.inject.Inject;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.l7tech.policy.variable.BuiltinVariables.PREFIX_CLUSTER_PROPERTY;
import static com.l7tech.policy.variable.BuiltinVariables.PREFIX_GATEWAY_TIME;

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

    @Inject
    private SecurePasswordManager securePasswordManager;

    private static final Pattern SECPASS_PLAINTEXT_PATTERN = Pattern.compile("^secpass\\.([a-zA-Z_][a-zA-Z0-9_\\-]*)\\.plaintext$");
    private static final Pattern SECPASS_DESCRIPTION_PATTERN = Pattern.compile("^secpass\\.([a-zA-Z_][a-zA-Z0-9_\\-]*)\\.description");

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
                    if (dependency != null && !dependencies.contains(dependency))
                        dependencies.add(dependency);
                }
            }
        }
        //If the assertion implements UsesVariables then all cluster properties or secure passwords used be the assertion as considered to be dependencies.
        Boolean doSecPasswordPlaintext = finder.getOption(DependencyAnalyzer.FindSecurePasswordDependencyFromContextVariablePlaintextOptionKey, Boolean.class, true);
        Boolean doSecPasswordDesc = finder.getOption(DependencyAnalyzer.FindSecurePasswordDependencyFromContextVariableDescriptionOptionKey, Boolean.class, true);

        if (assertion instanceof UsesVariables) {
            for (String variable : ((UsesVariables) assertion).getVariablesUsed()) {
                if (variable.startsWith(PREFIX_CLUSTER_PROPERTY) &&
                        variable.length() > PREFIX_CLUSTER_PROPERTY.length() &&
                        !variable.startsWith(PREFIX_GATEWAY_TIME) /* special case exclude, because PREFIX_GATEWAY_TIME.startsWith(PREFIX_CLUSTER_PROPERTY) */) {
                    String cpName = variable.substring(PREFIX_CLUSTER_PROPERTY.length()+1);

                    // try to get cluster property reference
                    ClusterProperty property = clusterPropertyManager.findByUniqueName(cpName);
                    if (property != null) {
                        Dependency dependency = finder.getDependency(property);
                        if (dependency != null && !dependencies.contains(dependency)){
                            dependencies.add(dependency);
                        }
                }
                }else if(doSecPasswordPlaintext){
                    // try get secure password reference
                    Matcher matcher = SECPASS_PLAINTEXT_PATTERN.matcher(variable);
                    if (matcher.matches()) {
                        String alias = matcher.group(1);
                        final SecurePassword securePassword = securePasswordManager.findByUniqueName(alias);
                        if (securePassword != null) {
                            Dependency dependency = finder.getDependency(securePassword);
                            if (dependency != null && !dependencies.contains(securePassword)){
                                dependencies.add(dependency);
                            }
                        }
                    }
                }else if (doSecPasswordDesc){
                    Matcher descMatcher = SECPASS_DESCRIPTION_PATTERN.matcher(variable);
                    String alias = descMatcher.group(1);
                    final SecurePassword securePassword = securePasswordManager.findByUniqueName(alias);
                    if (securePassword != null) {
                        Dependency dependency = finder.getDependency(securePassword);
                        if (dependency != null && !dependencies.contains(securePassword)){
                            dependencies.add(dependency);
                        }
                    }
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
                    if (dependency != null && !dependencies.contains(dependency))
                        dependencies.add(dependency);
                }
            }
        }

        //Add any private keys to the assertion dependencies
        if (assertion instanceof PrivateKeyable) {
            PrivateKeyable privateKeyable = (PrivateKeyable) assertion;
            if ((!(privateKeyable instanceof OptionalPrivateKeyable) || ((OptionalPrivateKeyable) privateKeyable).isUsesNoKey()) && privateKeyable.getKeyAlias() != null) {
                final Entity keyEntry = loadEntity(new SsgKeyHeader(privateKeyable.getNonDefaultKeystoreId() + ":" + privateKeyable.getKeyAlias(), privateKeyable.getNonDefaultKeystoreId(), privateKeyable.getKeyAlias(), privateKeyable.getKeyAlias()));
                if (keyEntry != null) {
                    Dependency dependency = finder.getDependency(keyEntry);
                    if (dependency != null && !dependencies.contains(dependency))
                        dependencies.add(dependency);
                }
            }
        }

        return dependencies;
    }

    @Override
    public DependentObject createDependentObject(Assertion assertion) {
        final AssertionNodeNameFactory assertionNodeNameFactory = assertion.meta().get(AssertionMetadata.POLICY_NODE_NAME_FACTORY);
        //noinspection unchecked
        return new DependentAssertion((String) assertion.meta().get(AssertionMetadata.SHORT_NAME), assertionNodeNameFactory != null ? assertionNodeNameFactory.getAssertionName(assertion, true) : null);
    }

    /**
     * This throws an exception. It should not be called. Assertions cannot be found the same way other entities can.
     */
    @SuppressWarnings("unchecked")
    public List<? extends Entity> find(@NotNull Object searchValue, com.l7tech.search.Dependency.DependencyType dependencyType, com.l7tech.search.Dependency.MethodReturnType searchValueType) {
        throw new UnsupportedOperationException("Assertions cannot be loaded as entities");
    }

    @Override
    public void replaceDependencies(@NotNull Assertion assertion, @NotNull Map<EntityHeader, EntityHeader> replacementMap, DependencyFinder finder) throws CannotRetrieveDependenciesException, CannotReplaceDependenciesException {
        super.replaceDependencies(assertion, replacementMap, finder);

        if (assertion instanceof UsesEntities) {
            UsesEntities usesEntities = (UsesEntities) assertion;
            EntityHeader[] entitiesUsed = usesEntities.getEntitiesUsed();
            for (EntityHeader entityUsed : entitiesUsed) {
                EntityHeader newEntity = findMappedHeader(replacementMap,entityUsed);
                if (newEntity != null) {
                    usesEntities.replaceEntity(entityUsed, newEntity);
                }
            }
        }

        if (assertion instanceof UsesResourceInfo) {
            AssertionResourceInfo assertionResourceInfo = ((UsesResourceInfo) assertion).getResourceInfo();
            if (assertionResourceInfo != null && assertionResourceInfo.getType().equals(AssertionResourceType.GLOBAL_RESOURCE)) {
                final String uri = ((GlobalResourceInfo) assertionResourceInfo).getId();
                Object found = CollectionUtils.find(replacementMap.keySet(),new Predicate() {
                    @Override
                    public boolean evaluate(Object o) {
                        return o instanceof ResourceEntryHeader && ((ResourceEntryHeader)o).getUri().equals(uri);
                    }
                });
                if(found!=null){
                    GlobalResourceInfo newResourceInfo = new GlobalResourceInfo();
                    EntityHeader replace = findMappedHeader(replacementMap,(EntityHeader)found);
                    if(replace instanceof ResourceEntryHeader){
                        newResourceInfo.setId(((ResourceEntryHeader)replace).getUri());
                        ((UsesResourceInfo) assertion).setResourceInfo(newResourceInfo);
                    }
                }
            }
        }

    }
}
