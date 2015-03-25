package com.l7tech.server.search.processors;

import com.l7tech.gateway.common.cluster.ClusterProperty;
import com.l7tech.gateway.common.custom.ClassNameToEntitySerializer;
import com.l7tech.gateway.common.custom.CustomAssertionsRegistrar;
import com.l7tech.gateway.common.entity.EntitiesResolver;
import com.l7tech.gateway.common.resources.ResourceEntry;
import com.l7tech.gateway.common.resources.ResourceEntryHeader;
import com.l7tech.gateway.common.security.keystore.SsgKeyEntry;
import com.l7tech.gateway.common.security.password.SecurePassword;
import com.l7tech.objectmodel.*;
import com.l7tech.policy.AssertionResourceInfo;
import com.l7tech.policy.assertion.*;
import com.l7tech.policy.assertion.ext.entity.CustomEntitySerializer;
import com.l7tech.server.DefaultKey;
import com.l7tech.server.EntityHeaderUtils;
import com.l7tech.server.cluster.ClusterPropertyManager;
import com.l7tech.server.globalresources.ResourceEntryManager;
import com.l7tech.server.policy.CustomKeyValueStoreManager;
import com.l7tech.server.search.DependencyAnalyzer;
import com.l7tech.server.search.exceptions.CannotReplaceDependenciesException;
import com.l7tech.server.search.exceptions.CannotRetrieveDependenciesException;
import com.l7tech.server.search.objects.BrokenDependency;
import com.l7tech.server.search.objects.Dependency;
import com.l7tech.server.search.objects.DependentAssertion;
import com.l7tech.server.search.objects.DependentObject;
import com.l7tech.server.security.password.SecurePasswordManager;
import com.l7tech.server.store.CustomKeyValueStoreImpl;
import com.l7tech.util.Functions;
import org.jetbrains.annotations.NotNull;

import javax.inject.Inject;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.l7tech.policy.variable.BuiltinVariables.PREFIX_CLUSTER_PROPERTY;
import static com.l7tech.policy.variable.BuiltinVariables.PREFIX_GATEWAY_TIME;
import static com.l7tech.policy.variable.BuiltinVariables.PREFIX_GATEWAY_RANDOM;


/**
 * The assertion dependencyProcessor finds the dependencies that an assertions has.
 *
 * @author Victor Kazakov
 */
public class DefaultAssertionDependencyProcessor<A extends Assertion> extends DefaultDependencyProcessor<A> implements DependencyProcessor<A> {

    private static final Pattern SECPASS_PLAINTEXT_PATTERN = Pattern.compile("^secpass\\.([a-zA-Z_][a-zA-Z0-9_\\-]*)\\.plaintext$");
    @Inject
    private ClusterPropertyManager clusterPropertyManager;
    @Inject
    private ResourceEntryManager resourceEntryManager;
    @Inject
    private SecurePasswordManager securePasswordManager;
    @Inject
    private CustomKeyValueStoreManager customKeyValueStoreManager;
    @Inject
    private CustomAssertionsRegistrar customAssertionRegistrar;
    @Inject
    private DefaultKey defaultKey;

    /**
     * Finds the dependencies that an assertion has. First finds the dependencies by looking at the methods defined by
     * this assertion. If the assertion implements {@link UsesEntities} or {@link CustomAssertionHolder} then the
     * entities returned by {@link EntitiesResolver#getEntitiesUsed(com.l7tech.policy.assertion.Assertion)
     * getEntitiesUsed} method are assumed to be dependencies. If the assertion implements {@link UsesVariables} then
     * all the variables used that are cluster properties are returned as dependencies.
     *
     * @param assertion The assertion to find dependencies for.
     * @param finder    The finder that if performing the current dependency search
     * @return The list of dependencies that this assertion has
     * @throws FindException This is thrown if an entity cannot be found
     */
    @NotNull
    @Override
    public List<Dependency> findDependencies(@NotNull final A assertion, @NotNull final DependencyFinder finder) throws FindException, CannotRetrieveDependenciesException {
        //uses the generic dependency processor to find dependencies using the methods defined by the assertion.
        final List<Dependency> dependencies = super.findDependencies(assertion, finder);

        //use the entity resolver to resolve entities used by assertions
        final EntitiesResolver entitiesResolver = EntitiesResolver
                .builder()
                .keyValueStore(new CustomKeyValueStoreImpl(customKeyValueStoreManager))
                .classNameToSerializer(new ClassNameToEntitySerializer() {
                    @Override
                    public CustomEntitySerializer getSerializer(final String className) {
                        return customAssertionRegistrar.getExternalEntitySerializer(className);
                    }
                })
                .build();
        for (final EntityHeader header : entitiesResolver.getEntitiesUsed(assertion)) {
            final Entity entity = loadEntity(header);
            Dependency dependency = finder.getDependency(DependencyFinder.FindResults.create(entity, header ));
            if (dependency != null && !dependencies.contains(dependency)) {
                dependencies.add(dependency);
            }
        }

        //If the assertion implements UsesVariables then all cluster properties or secure passwords used be the assertion are considered to be dependencies.
        if (assertion instanceof UsesVariables) {
            final boolean doSecPasswordPlaintext = finder.getOption(DependencyAnalyzer.FindSecurePasswordDependencyFromContextVariablePlaintextOptionKey, Boolean.class, true);

            for (final String variable : ((UsesVariables) assertion).getVariablesUsed()) {
                if (variable.startsWith(PREFIX_CLUSTER_PROPERTY) &&
                        variable.length() > PREFIX_CLUSTER_PROPERTY.length() &&
                        !variable.startsWith(PREFIX_GATEWAY_RANDOM) &&
                        !variable.startsWith(PREFIX_GATEWAY_TIME) /* special case exclude, because PREFIX_GATEWAY_TIME.startsWith(PREFIX_CLUSTER_PROPERTY) */) {
                    final String cpName = variable.substring(PREFIX_CLUSTER_PROPERTY.length() + 1);

                    // try to get cluster property reference
                    final ClusterProperty property = clusterPropertyManager.findByUniqueName(cpName);
                    final Dependency dependency = finder.getDependency(DependencyFinder.FindResults.create(property, new EntityHeader(Goid.DEFAULT_GOID,EntityType.CLUSTER_PROPERTY,cpName,null)));
                    if (dependency != null && !dependencies.contains(dependency)) {
                        dependencies.add(dependency);
                    }
                } else if (doSecPasswordPlaintext) {
                    // try get secure password reference
                    final Matcher matcher = SECPASS_PLAINTEXT_PATTERN.matcher(variable);
                    if (matcher.matches()) {
                        final String alias = matcher.group(1);
                        final SecurePassword securePassword = securePasswordManager.findByUniqueName(alias);
                        final Dependency dependency = finder.getDependency(DependencyFinder.FindResults.create(securePassword, new EntityHeader(Goid.DEFAULT_GOID,EntityType.SECURE_PASSWORD,alias,null)));
                        if (dependency != null && !dependencies.contains(dependency)) {
                            dependencies.add(dependency);
                        }
                    }
                }
            }
        }
        //If the assertion implements UsesResourceInfo then add the used resource as a dependent.
        if (assertion instanceof UsesResourceInfo) {
            final AssertionResourceInfo assertionResourceInfo = ((UsesResourceInfo) assertion).getResourceInfo();
            if (assertionResourceInfo != null && assertionResourceInfo.getType().equals(AssertionResourceType.GLOBAL_RESOURCE)) {
                final String uri = ((GlobalResourceInfo) assertionResourceInfo).getId();
                //Passing null as the resource type should be ok as resources as unique by uri anyways.
                final ResourceEntry resourceEntry = resourceEntryManager.findResourceByUriAndType(uri, null);
                final Dependency dependency = finder.getDependency(DependencyFinder.FindResults.create(resourceEntry, new EntityHeader(Goid.DEFAULT_GOID,EntityType.RESOURCE_ENTRY,uri,null)));
                if (dependency != null && !dependencies.contains(dependency))
                    dependencies.add(dependency);

            }
        }

        //Add any private keys to the assertion dependencies
        if (assertion instanceof PrivateKeyable) {
            final PrivateKeyable privateKeyable = (PrivateKeyable) assertion;
            if ((!(privateKeyable instanceof OptionalPrivateKeyable) || !((OptionalPrivateKeyable) privateKeyable).isUsesNoKey()) && !privateKeyable.isUsesDefaultKeyStore()) {
                SsgKeyHeader keyHeader = new SsgKeyHeader(privateKeyable.getNonDefaultKeystoreId() + ":" + privateKeyable.getKeyAlias(), privateKeyable.getNonDefaultKeystoreId(), privateKeyable.getKeyAlias(), privateKeyable.getKeyAlias());
                try {
                    final Entity keyEntry = loadEntity(keyHeader);
                    if(keyEntry == null){
                        dependencies.add(new BrokenDependency(keyHeader));
                    }else {
                        final Dependency dependency = finder.getDependency(DependencyFinder.FindResults.create(keyEntry, keyHeader));
                        if (dependency != null && !dependencies.contains(dependency))
                            dependencies.add(dependency);
                    }

                } catch ( FindException e){
                    dependencies.add(new BrokenDependency(keyHeader));
                }
            }else if((!(privateKeyable instanceof OptionalPrivateKeyable) || !((OptionalPrivateKeyable) privateKeyable).isUsesNoKey()) && privateKeyable.isUsesDefaultKeyStore()){
                // get the default key
                try {
                    SsgKeyEntry keyEntry = defaultKey.getSslInfo();
                    final Dependency dependency = finder.getDependency(DependencyFinder.FindResults.create(keyEntry, EntityHeaderUtils.fromEntity(keyEntry)));
                    if (dependency != null && !dependencies.contains(dependency))
                        dependencies.add(dependency);
                } catch ( IOException e){
                    throw new CannotRetrieveDependenciesException(assertion.getClass(),e.getMessage());
                }
            }
        }

        return dependencies;
    }

    @NotNull
    @Override
    public DependentObject createDependentObject(@NotNull final A assertion) {
        return new DependentAssertion<>((String) assertion.meta().get(AssertionMetadata.SHORT_NAME), assertion.getClass());
    }

    @NotNull
    @Override
    public List<DependentObject> createDependentObjects(@NotNull final Object searchValue, @NotNull final com.l7tech.search.Dependency.DependencyType dependencyType, @NotNull final com.l7tech.search.Dependency.MethodReturnType searchValueType) {
        throw new UnsupportedOperationException("AssertionDependent Objects cannot be created from a search value.");
    }

    /**
     * This throws an exception. It should not be called. Assertions cannot be found the same way other entities can.
     */
    @NotNull
    @Override
    public List<DependencyFinder.FindResults<A>> find(@NotNull final Object searchValue, @NotNull final com.l7tech.search.Dependency.DependencyType dependencyType, @NotNull final com.l7tech.search.Dependency.MethodReturnType searchValueType) {
        throw new UnsupportedOperationException("Assertions cannot be loaded as entities");
    }

    @Override
    public void replaceDependencies(@NotNull final A assertion, @NotNull final Map<EntityHeader, EntityHeader> replacementMap, @NotNull final DependencyFinder finder, final boolean replaceAssertionsDependencies) throws CannotReplaceDependenciesException {
        if(!replaceAssertionsDependencies) return;

        super.replaceDependencies(assertion, replacementMap, finder, replaceAssertionsDependencies);

        //use the entity resolver to find the entities used.
        final EntitiesResolver entitiesResolver = EntitiesResolver
                .builder()
                .keyValueStore(new CustomKeyValueStoreImpl(customKeyValueStoreManager))
                .classNameToSerializer(new ClassNameToEntitySerializer() {
                    @Override
                    public CustomEntitySerializer getSerializer(final String className) {
                        return customAssertionRegistrar.getExternalEntitySerializer(className);
                    }
                })
                .build();
        final EntityHeader[] entitiesUsed = entitiesResolver.getEntitiesUsed(assertion);
        for (final EntityHeader entityUsed : entitiesUsed) {
            final EntityHeader newEntity = DependencyProcessorUtils.findMappedHeader(replacementMap, entityUsed);
            if (newEntity != null) {
                entitiesResolver.replaceEntity(assertion, entityUsed, newEntity);
            }
        }

        if (assertion instanceof UsesResourceInfo) {
            final AssertionResourceInfo assertionResourceInfo = ((UsesResourceInfo) assertion).getResourceInfo();
            if (assertionResourceInfo != null && assertionResourceInfo.getType().equals(AssertionResourceType.GLOBAL_RESOURCE)) {
                final String uri = ((GlobalResourceInfo) assertionResourceInfo).getId();
                final EntityHeader found = Functions.grepFirst(replacementMap.keySet(), new Functions.Unary<Boolean, EntityHeader>() {
                    @Override
                    public Boolean call(EntityHeader entityHeader) {
                        return entityHeader instanceof ResourceEntryHeader && ((ResourceEntryHeader) entityHeader).getUri().equals(uri);
                    }
                });
                if (found != null) {
                    final GlobalResourceInfo newResourceInfo = new GlobalResourceInfo();
                    final EntityHeader replace = DependencyProcessorUtils.findMappedHeader(replacementMap, found);
                    if (replace instanceof ResourceEntryHeader) {
                        newResourceInfo.setId(((ResourceEntryHeader) replace).getUri());
                        ((UsesResourceInfo) assertion).setResourceInfo(newResourceInfo);
                    }
                }
            }
        }

        if (assertion instanceof PrivateKeyable) {
            final PrivateKeyable privateKeyable = (PrivateKeyable) assertion;
            if ((!(privateKeyable instanceof OptionalPrivateKeyable) || ((OptionalPrivateKeyable) privateKeyable).isUsesNoKey()) && privateKeyable.getKeyAlias() != null) {
                final SsgKeyHeader privateKeyHeader = new SsgKeyHeader(privateKeyable.getNonDefaultKeystoreId() + ":" + privateKeyable.getKeyAlias(), privateKeyable.getNonDefaultKeystoreId(), privateKeyable.getKeyAlias(), privateKeyable.getKeyAlias());
                EntityHeader mappedHeader = DependencyProcessorUtils.findMappedHeader(replacementMap, privateKeyHeader);
                if(mappedHeader != null) {
                    if(!(mappedHeader instanceof SsgKeyHeader)){
                        throw new CannotReplaceDependenciesException(assertion.getClass(), "Attempting to replace ssg key but mapped header in not an SsgKeyHeader.");
                    }
                    privateKeyable.setNonDefaultKeystoreId(((SsgKeyHeader)mappedHeader).getKeystoreId());
                    privateKeyable.setKeyAlias(((SsgKeyHeader)mappedHeader).getAlias());
                }
            }
        }
    }
}
