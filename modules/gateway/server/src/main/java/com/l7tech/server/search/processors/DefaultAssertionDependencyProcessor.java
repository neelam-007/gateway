package com.l7tech.server.search.processors;

import com.l7tech.gateway.common.cluster.ClusterProperty;
import com.l7tech.gateway.common.custom.ClassNameToEntitySerializer;
import com.l7tech.gateway.common.custom.CustomAssertionsRegistrar;
import com.l7tech.gateway.common.entity.EntitiesResolver;
import com.l7tech.gateway.common.module.AssertionModuleInfo;
import com.l7tech.gateway.common.module.ServerModuleFile;
import com.l7tech.gateway.common.resources.ResourceEntry;
import com.l7tech.gateway.common.resources.ResourceEntryHeader;
import com.l7tech.gateway.common.security.keystore.SsgKeyEntry;
import com.l7tech.gateway.common.security.password.SecurePassword;
import com.l7tech.objectmodel.*;
import com.l7tech.policy.AssertionResourceInfo;
import com.l7tech.policy.assertion.*;
import com.l7tech.policy.assertion.ext.CustomAssertion;
import com.l7tech.policy.assertion.ext.entity.CustomEntitySerializer;
import com.l7tech.server.DefaultKey;
import com.l7tech.server.EntityFinder;
import com.l7tech.server.EntityHeaderUtils;
import com.l7tech.server.cluster.ClusterPropertyManager;
import com.l7tech.server.globalresources.ResourceEntryManager;
import com.l7tech.server.module.AssertionModuleFinder;
import com.l7tech.server.module.ServerModuleFileManager;
import com.l7tech.server.policy.CustomKeyValueStoreManager;
import com.l7tech.server.policy.module.ModularAssertionModule;
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
import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.inject.Inject;
import javax.inject.Named;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.l7tech.policy.variable.BuiltinVariables.*;


/**
 * The assertion dependencyProcessor finds the dependencies that an assertion has.
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
    @Named("customAssertionRegistrar")
    private CustomAssertionsRegistrar customAssertionRegistrar;
    @Inject
    @Named("modularAssertionModuleFinder")
    private AssertionModuleFinder<ModularAssertionModule> modularAssertionModuleFinder;
    @Inject
    private DefaultKey defaultKey;
    @Inject
    private ServerModuleFileManager serverModuleFileManager;
    @Inject
    private EntityFinder entityFinder;

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

        if (assertion instanceof UsesEntitiesAtDesignTime) {
            loadEntitiesUsedAtDesignTime((UsesEntitiesAtDesignTime) assertion);
        }

        loadEntityDependencies(assertion, finder, dependencies);

        if (assertion instanceof UsesVariables) {
            loadVariableDependencies((UsesVariables) assertion, finder, dependencies);
        }
        if (assertion instanceof UsesResourceInfo) {
            loadResourceDependencies((UsesResourceInfo) assertion, finder, dependencies);
        }

        if (assertion instanceof PrivateKeyable) {
            loadPrivateKeyDependencies((PrivateKeyable) assertion, finder, dependencies);
        }

        // determine whether the assertion is from a module uploaded via Policy Manager (i.e. ServerModuleFile)
        if (assertion instanceof CustomAssertionHolder) {
            // this is a Custom Assertion so get the module entity through the assertion descriptor
            final CustomAssertion customAssertion = ((CustomAssertionHolder) assertion).getCustomAssertion();
            if (customAssertion != null) {
                final AssertionModuleInfo moduleInfo = customAssertionRegistrar.getModuleInfoForAssertionClass(customAssertion.getClass().getName());
                if (moduleInfo != null && moduleInfo.isFromDb()) {
                    processServerModuleFileDependency(moduleInfo.getModuleEntityName(), finder, dependencies);
                }
            }
        } else {
            // this is a Modular Assertion so get the module entity through the assertion module
            final ModularAssertionModule module = modularAssertionModuleFinder.getModuleForClassLoader(assertion.getClass().getClassLoader());
            if (module != null && module.isFromDb()) {
                processServerModuleFileDependency(module.getEntityName(), finder, dependencies);
            }
        }

        return dependencies;
    }

    /**
     * Loads the entities used by an assertion at design time
     * @param assertion the assertion containing entities used at design time
     */
    private void loadEntitiesUsedAtDesignTime(final UsesEntitiesAtDesignTime assertion) {
        final EntityHeader[] headers = assertion.getEntitiesUsedAtDesignTime();
        if (headers != null) {
            for (final EntityHeader header : headers) {
                if (assertion.needsProvideEntity(header)) {
                    try {
                        final Entity entity = entityFinder.find(header);
                        if (entity == null) {
                            logger.warning(new Supplier<String>() {
                                @Override
                                public String get() {
                                    return "Unable to find entity matching header of type " + header.getType() + " with ID " + header.getStrId();
                                }
                            });
                        } else {
                            assertion.provideEntity(header, entity);
                        }
                    } catch (final FindException e) {
                        logger.warning(new Supplier<String>() {
                            @Override
                            public String get() {
                                return "Encountered an exception fetching entity matching header of type " + header.getType() + " with ID " + header.getStrId();
                            }
                        });
                    }
                }
            }
        }
    }

    /**
     * Add entities used by an assertion to the list of its dependencies
     *
     * @param assertion    The assertion to find entities for
     * @param finder       The finder performing the current dependency search
     * @param dependencies List containing dependencies found so far for this assertion
     * @throws CannotRetrieveDependenciesException if there is a problem attempting to retrieve a dependency
     * @throws FindException                       if an entity cannot be found
     */
    private void loadEntityDependencies(@NotNull final A assertion, @NotNull final DependencyFinder finder, final List<Dependency> dependencies) throws FindException, CannotRetrieveDependenciesException {
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
            final Dependency dependency = finder.getDependency(DependencyFinder.FindResults.create(entity, header));
            if (dependency != null && !dependencies.contains(dependency)) {
                dependencies.add(dependency);
            }
        }
    }

    /**
     * Add resources to the assertion dependencies
     *
     * @param assertion    The assertion to find resource dependencies for
     * @param finder       The finder performing the current dependency search
     * @param dependencies List containing dependencies found so far for this assertion
     * @throws CannotRetrieveDependenciesException if there is a problem attempting to retrieve a dependency
     * @throws FindException                       if an entity cannot be found
     */
    private void loadVariableDependencies(final UsesVariables assertion, @NotNull final DependencyFinder finder, final List<Dependency> dependencies) throws FindException, CannotRetrieveDependenciesException {
        final boolean doSecPasswordPlaintext = finder.getOption(DependencyAnalyzer.FindSecurePasswordDependencyFromContextVariablePlaintextOptionKey, Boolean.class, true);

        for (final String variable : assertion.getVariablesUsed()) {

            final boolean variableIsClusterProperty = variable.startsWith(PREFIX_CLUSTER_PROPERTY) &&
                    variable.length() > PREFIX_CLUSTER_PROPERTY.length() &&
                    !variable.startsWith(PREFIX_GATEWAY_RANDOM) &&
                    !variable.startsWith(PREFIX_GATEWAY_TIME); /* special case exclude, because PREFIX_GATEWAY_TIME.startsWith(PREFIX_CLUSTER_PROPERTY) */

            if (variableIsClusterProperty) {
                loadClusterPropertyDependency(finder, dependencies, variable);
            } else if (doSecPasswordPlaintext) {
                loadSecurePasswordReferenceDependency(finder, dependencies, variable);

            }
        }
    }

    /**
     * Given a variable containing a cluster property, add that cluster property to the list of dependencies
     *
     * @param variable     The variable containing the cluster property
     * @param finder       The finder performing the current dependency search
     * @param dependencies List containing dependencies found so far
     * @throws CannotRetrieveDependenciesException if there is a problem attempting to retrieve a dependency
     * @throws FindException                       if an entity cannot be found
     */
    private void loadClusterPropertyDependency(@NotNull final DependencyFinder finder, final List<Dependency> dependencies, final String variable) throws FindException, CannotRetrieveDependenciesException {
        final String cpName = variable.substring(PREFIX_CLUSTER_PROPERTY.length() + 1);

        // try to get cluster property reference
        final ClusterProperty property = clusterPropertyManager.findByUniqueName(cpName);
        final Dependency dependency = finder.getDependency(DependencyFinder.FindResults.create(property, new EntityHeader(Goid.DEFAULT_GOID, EntityType.CLUSTER_PROPERTY, cpName, null)));
        if (dependency != null && !dependencies.contains(dependency)) {
            dependencies.add(dependency);
        }
    }

    /**
     * Given a variable containing a secure password reference, add that secure password reference to the list of dependencies
     *
     * @param variable     The variable containing the secure password reference
     * @param finder       The finder performing the current dependency search
     * @param dependencies List containing dependencies found so far
     * @throws CannotRetrieveDependenciesException if there is a problem attempting to retrieve a dependency
     * @throws FindException                       if an entity cannot be found
     */
    private void loadSecurePasswordReferenceDependency(@NotNull final DependencyFinder finder, final List<Dependency> dependencies, final String variable) throws FindException, CannotRetrieveDependenciesException {
        // try get secure password reference
        final Matcher matcher = SECPASS_PLAINTEXT_PATTERN.matcher(variable);
        if (matcher.matches()) {
            final String alias = matcher.group(1);
            final SecurePassword securePassword = securePasswordManager.findByUniqueName(alias);
            final Dependency dependency = finder.getDependency(DependencyFinder.FindResults.create(securePassword, new EntityHeader(Goid.DEFAULT_GOID, EntityType.SECURE_PASSWORD, alias, null)));
            if (dependency != null && !dependencies.contains(dependency)) {
                dependencies.add(dependency);
            }
        }
    }

    /**
     * Add resources to the assertion dependencies
     *
     * @param assertion    The assertion to find resource dependencies for
     * @param finder       The finder performing the current dependency search
     * @param dependencies List containing dependencies found so far for this assertion
     * @throws CannotRetrieveDependenciesException if there is a problem attempting to retrieve a dependency
     * @throws FindException                       if an entity cannot be found
     */
    private void loadResourceDependencies(@NotNull final UsesResourceInfo assertion, @NotNull final DependencyFinder finder, final List<Dependency> dependencies) throws FindException, CannotRetrieveDependenciesException {
        final AssertionResourceInfo assertionResourceInfo = assertion.getResourceInfo();
        if (assertionResourceInfo != null && assertionResourceInfo.getType().equals(AssertionResourceType.GLOBAL_RESOURCE)) {
            final String uri = ((GlobalResourceInfo) assertionResourceInfo).getId();
            //Passing null as the resource type should be ok as resources as unique by uri anyways.
            final ResourceEntry resourceEntry = resourceEntryManager.findResourceByUriAndType(uri, null);
            final Dependency dependency = finder.getDependency(DependencyFinder.FindResults.create(resourceEntry, new EntityHeader(Goid.DEFAULT_GOID, EntityType.RESOURCE_ENTRY, uri, null)));
            if (dependency != null && !dependencies.contains(dependency))
                dependencies.add(dependency);

        }
    }

    /**
     * Add private keys to the assertion dependencies
     *
     * @param assertion    The assertion to find private key dependencies for
     * @param finder       The finder performing the current dependency search
     * @param dependencies List containing dependencies found so far for this assertion
     * @throws CannotRetrieveDependenciesException if there is a problem attempting to retrieve a dependency
     * @throws FindException                       if an entity cannot be found
     */
    private void loadPrivateKeyDependencies(@NotNull final PrivateKeyable assertion, @NotNull final DependencyFinder finder, final List<Dependency> dependencies) throws CannotRetrieveDependenciesException, FindException {
        if ((!(assertion instanceof OptionalPrivateKeyable) || !((OptionalPrivateKeyable) assertion).isUsesNoKey()) && !assertion.isUsesDefaultKeyStore()) {
            loadNonDefaultKeyDependency(assertion, finder, dependencies);
        } else if ((!(assertion instanceof OptionalPrivateKeyable) || !((OptionalPrivateKeyable) assertion).isUsesNoKey()) && assertion.isUsesDefaultKeyStore()) {
            loadDefaultKeyDependency(assertion, finder, dependencies);

        }
    }

    /**
     * Add non-default private key dependencies to the assertion dependencies
     *
     * @param assertion    The assertion to find private key dependencies for
     * @param finder       The finder performing the current dependency search
     * @param dependencies List containing dependencies found so far for this assertion
     * @throws CannotRetrieveDependenciesException if there is a problem attempting to retrieve a dependency
     */
    private void loadNonDefaultKeyDependency(@NotNull final PrivateKeyable assertion, @NotNull final DependencyFinder finder, final List<Dependency> dependencies) throws CannotRetrieveDependenciesException {
        final SsgKeyHeader keyHeader = new SsgKeyHeader(assertion.getNonDefaultKeystoreId() + ":" + assertion.getKeyAlias(), assertion.getNonDefaultKeystoreId(), assertion.getKeyAlias(), assertion.getKeyAlias());
        try {
            final Entity keyEntry = loadEntity(keyHeader);
            if (keyEntry == null) {
                dependencies.add(new BrokenDependency(keyHeader));
            } else {
                final Dependency dependency = finder.getDependency(DependencyFinder.FindResults.create(keyEntry, keyHeader));
                if (dependency != null && !dependencies.contains(dependency))
                    dependencies.add(dependency);
            }

        } catch (final FindException e) {
            dependencies.add(new BrokenDependency(keyHeader));
        }
    }

    /**
     * Add default private key dependencies to the assertion dependencies
     *
     * @param assertion    The assertion to find private key dependencies for
     * @param finder       The finder performing the current dependency search
     * @param dependencies List containing dependencies found so far for this assertion
     * @throws CannotRetrieveDependenciesException if there is a problem attempting to retrieve a dependency
     * @throws FindException                       if an entity cannot be found
     */
    private void loadDefaultKeyDependency(@NotNull final PrivateKeyable assertion, @NotNull final DependencyFinder finder, final List<Dependency> dependencies) throws FindException, CannotRetrieveDependenciesException {
        // get the default key
        try {
            final SsgKeyEntry keyEntry = defaultKey.getSslInfo();
            final Dependency dependency = finder.getDependency(DependencyFinder.FindResults.create(keyEntry, EntityHeaderUtils.fromEntity(keyEntry)));
            if (dependency != null && !dependencies.contains(dependency))
                dependencies.add(dependency);
        } catch (final IOException e) {
            throw new CannotRetrieveDependenciesException(assertion.getClass(), e.getMessage());
        }
    }

    /**
     * Utility method for processing Assertion dependencies of the {@code ServerModuleFile} specified with the {@code moduleName}.
     *
     * @param moduleName   The module name i.e. {@code ServerModuleFile} entity name.  Optional and can be {@code null}, in which case nothing is checked.
     * @param finder       The finder that is performing the current dependency search.
     * @param dependencies List of dependencies found so far.
     */
    private void processServerModuleFileDependency(
            @Nullable final String moduleName,
            @NotNull final DependencyFinder finder,
            @NotNull final List<Dependency> dependencies
    ) throws FindException, CannotRetrieveDependenciesException {
        if (StringUtils.isNotBlank(moduleName)) {
            final ServerModuleFile moduleFile = serverModuleFileManager.findByUniqueName(moduleName);
            if (moduleFile != null) {
                final Dependency dependency = finder.getDependency(
                        DependencyFinder.FindResults.create(
                                moduleFile,
                                new EntityHeader(Goid.DEFAULT_GOID, EntityType.SERVER_MODULE_FILE, moduleName, null)
                        )
                );
                // add the dependency if not processed yet.
                if (dependency != null && !dependencies.contains(dependency)) {
                    dependencies.add(dependency);
                }
            }
        }
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
        if (!replaceAssertionsDependencies) return;

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
                    public Boolean call(final EntityHeader entityHeader) {
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
            if (!(privateKeyable instanceof OptionalPrivateKeyable && ((OptionalPrivateKeyable) privateKeyable).isUsesNoKey()) && privateKeyable.getKeyAlias() != null) {
                final SsgKeyHeader privateKeyHeader = new SsgKeyHeader(privateKeyable.getNonDefaultKeystoreId() + ":" + privateKeyable.getKeyAlias(), privateKeyable.getNonDefaultKeystoreId(), privateKeyable.getKeyAlias(), privateKeyable.getKeyAlias());
                final EntityHeader mappedHeader = DependencyProcessorUtils.findMappedHeader(replacementMap, privateKeyHeader);
                if (mappedHeader != null) {
                    if (!(mappedHeader instanceof SsgKeyHeader)) {
                        throw new CannotReplaceDependenciesException(assertion.getClass(), "Attempting to replace ssg key but mapped header in not an SsgKeyHeader.");
                    }
                    privateKeyable.setNonDefaultKeystoreId(((SsgKeyHeader) mappedHeader).getKeystoreId());
                    privateKeyable.setKeyAlias(((SsgKeyHeader) mappedHeader).getAlias());
                }
            }
        }
    }
}
