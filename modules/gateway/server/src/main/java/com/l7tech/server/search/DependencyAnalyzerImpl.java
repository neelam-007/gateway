package com.l7tech.server.search;

import com.l7tech.gateway.common.cluster.ClusterProperty;
import com.l7tech.gateway.common.jdbc.JdbcConnection;
import com.l7tech.gateway.common.resources.HttpConfiguration;
import com.l7tech.gateway.common.security.RevocationCheckPolicy;
import com.l7tech.gateway.common.security.keystore.SsgKeyEntry;
import com.l7tech.gateway.common.security.password.SecurePassword;
import com.l7tech.gateway.common.security.rbac.Role;
import com.l7tech.gateway.common.service.PublishedService;
import com.l7tech.gateway.common.service.PublishedServiceAlias;
import com.l7tech.gateway.common.siteminder.SiteMinderConfiguration;
import com.l7tech.gateway.common.transport.InterfaceTag;
import com.l7tech.gateway.common.transport.SsgActiveConnector;
import com.l7tech.gateway.common.transport.SsgConnector;
import com.l7tech.gateway.common.transport.email.EmailListener;
import com.l7tech.gateway.common.transport.jms.JmsEndpoint;
import com.l7tech.identity.IdentityProviderConfig;
import com.l7tech.identity.internal.InternalGroup;
import com.l7tech.identity.internal.InternalUser;
import com.l7tech.objectmodel.*;
import com.l7tech.objectmodel.encass.EncapsulatedAssertionConfig;
import com.l7tech.objectmodel.folder.Folder;
import com.l7tech.policy.*;
import com.l7tech.security.cert.TrustedCert;
import com.l7tech.server.EntityCrud;
import com.l7tech.server.EntityHeaderUtils;
import com.l7tech.server.folder.FolderManager;
import com.l7tech.server.search.exceptions.CannotReplaceDependenciesException;
import com.l7tech.server.search.exceptions.CannotRetrieveDependenciesException;
import com.l7tech.server.search.objects.Dependency;
import com.l7tech.server.search.objects.DependencySearchResults;
import com.l7tech.server.search.objects.DependentObject;
import com.l7tech.server.search.processors.DependencyFinder;
import com.l7tech.server.search.processors.DependencyProcessorStore;
import com.l7tech.util.Functions;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.inject.Inject;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * This service is used to find dependencies of different entities.
 *
 * @author Victor Kazakov
 */
public class DependencyAnalyzerImpl implements DependencyAnalyzer {
    private static final Logger logger = Logger.getLogger(DependencyAnalyzerImpl.class.getName());

    @Inject
    private EntityCrud entityCrud;

    @Inject
    private FolderManager folderManager;

    @Inject
    private DependencyProcessorStore processorStore;

    private static final List<Class<? extends Entity>> entityClasses = Arrays.asList(
            //Omit policy version for now.
            //folders need to be done first to ensure folder order.
            Folder.class,
            SsgActiveConnector.class,
            AssertionAccess.class,
            TrustedCert.class,
            ClusterProperty.class,
            CustomKeyValueStore.class,
//            ServiceDocument.class,
            EmailListener.class,
            EncapsulatedAssertionConfig.class,
            GenericEntity.class,
            HttpConfiguration.class,
            IdentityProviderConfig.class,
            InterfaceTag.class,
            JdbcConnection.class,
//            JmsConnection.class,
            JmsEndpoint.class,
            SsgConnector.class,
            PolicyAlias.class,
            Policy.class,
            SsgKeyEntry.class,
            RevocationCheckPolicy.class,
            PublishedService.class,
            Role.class,
            SiteMinderConfiguration.class,
            SecurePassword.class,
            SecurityZone.class,
            PublishedServiceAlias.class,
            InternalUser.class,
            InternalGroup.class
    );

    /**
     * {@inheritDoc}
     */
    @NotNull
    @Override
    public DependencySearchResults getDependencies(@NotNull final EntityHeader entityHeader) throws FindException, CannotRetrieveDependenciesException {
        final DependencySearchResults dependencySearchResults = getDependencies(entityHeader, Collections.<String, Object>emptyMap());
        if (dependencySearchResults == null) {
            // This should never happen. The only time the dependencyFinder.process(entities) method can return null DependencySearchResults
            // is if the entity is null or if the entity is ignored in the search options. Neither of which happens here
            throw new IllegalStateException("Returned null dependency search results. This should not have happened.");
        }
        return dependencySearchResults;
    }

    /**
     * {@inheritDoc}
     */
    @NotNull
    @Override
    public DependencySearchResults getDependencies(@NotNull final EntityHeader entityHeader, @NotNull final Map<String, Object> searchOptions) throws FindException, CannotRetrieveDependenciesException {
        return getDependencies(Arrays.asList(entityHeader), searchOptions).get(0);
    }

    /**
     * {@inheritDoc}
     */
    @NotNull
    @Override
    public List<DependencySearchResults> getDependencies(@NotNull final List<EntityHeader> entityHeaders) throws FindException, CannotRetrieveDependenciesException {
        return getDependencies(entityHeaders, Collections.<String, Object>emptyMap());
    }

    /**
     * {@inheritDoc}
     */
    @NotNull
    @Override
    public List<DependencySearchResults> getDependencies(@NotNull final List<EntityHeader> entityHeaders, @NotNull final Map<String, Object> searchOptions) throws FindException, CannotRetrieveDependenciesException {
        logger.log(Level.FINE, "Finding dependencies for {0}", entityHeaders.isEmpty() ? "full gateway" : entityHeaders);

        final List<List<EntityHeader>> headerLists;
        if (entityHeaders.isEmpty()) {
            headerLists = loadAllGatewayEntities();
        } else {
            headerLists = new ArrayList<>();
            headerLists.add(entityHeaders);
        }

        //create a new dependency finder to perform the search
        final DependencyFinder dependencyFinder = new DependencyFinder(searchOptions, processorStore);
        final List<DependencySearchResults> results = new ArrayList<>();

        //Load the entities from the given entity headers.
        for (final List<EntityHeader> headerList : headerLists) {
            final ArrayList<Entity> entities = new ArrayList<>(headerList.size());
            for (final EntityHeader entityHeader : headerList) {
                final Entity entity = entityCrud.find(entityHeader);
                if (entity == null) {
                    throw new ObjectNotFoundException("Could not find Entity with header: " + entityHeader.toStringVerbose());
                }
                entities.add(entity);
            }
            results.addAll(dependencyFinder.process(entities));
        }
        return results;
    }

    /**
     * This will return a set lists of all entity headers for each entity type.
     *
     * @return The list of all entity headers.
     * @throws FindException
     */
    private List<List<EntityHeader>> loadAllGatewayEntities() throws FindException {
        final List<List<EntityHeader>> headerLists = new ArrayList<>();
        for (final Class<? extends Entity> entityClass : entityClasses) {
            final EntityHeaderSet<EntityHeader> entityHeaders;
            if(Policy.class.equals(entityClass)) {
                //exclude private service policies
                entityHeaders = Functions.reduce(entityCrud.findAll(entityClass), new EntityHeaderSet<>(), new Functions.Binary<EntityHeaderSet<EntityHeader>, EntityHeaderSet<EntityHeader>, EntityHeader>() {
                    @Override
                    public EntityHeaderSet<EntityHeader> call(EntityHeaderSet<EntityHeader> objects, EntityHeader entityHeader) {
                        if (!PolicyType.PRIVATE_SERVICE.equals(((PolicyHeader) entityHeader).getPolicyType())) {
                            objects.add(entityHeader);
                        }
                        return objects;
                    }
                });
            } else if(Folder.class.equals(entityClass)) {
                //folders only need to include the root folder.
                entityHeaders = new EntityHeaderSet<>(EntityHeaderUtils.fromEntity(folderManager.findRootFolder()));
            } else {
                entityHeaders = entityCrud.findAll(entityClass);
            }
            headerLists.add(new ArrayList<>(entityHeaders));
        }
        return headerLists;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <E extends Entity> void replaceDependencies(@NotNull final E entity, @NotNull final Map<EntityHeader, EntityHeader> replacementMap, final boolean replaceAssertionsDependencies) throws CannotReplaceDependenciesException {
        if (replacementMap.isEmpty()) {
            //nothing to replace, just shortcut to returning
            return;
        }

        //create a new dependency finder to perform the replacement
        final DependencyFinder dependencyFinder = new DependencyFinder(Collections.<String, Object>emptyMap(), processorStore);
        dependencyFinder.replaceDependencies(entity, replacementMap, replaceAssertionsDependencies);
    }

    /**
     * {@inheritDoc}
     */
    @NotNull
    @Override
    public List<Dependency> flattenDependencySearchResults(@NotNull final DependencySearchResults dependencySearchResult, final boolean includeRootNode) {
        return flattenDependencySearchResults(Arrays.asList(dependencySearchResult), includeRootNode);
    }

    /**
     * {@inheritDoc}
     */
    @NotNull
    @Override
    public List<Dependency> flattenDependencySearchResults(@NotNull final List<DependencySearchResults> dependencySearchResults, final boolean includeRootNode) {
        //the flat list of dependency object
        final List<Dependency> dependencyObjects = new ArrayList<>();
        for (final DependencySearchResults dependencySearchResult : dependencySearchResults) {
            if (dependencySearchResult != null) {
                if (includeRootNode) {
                    //get all security zone dependencies
                    getSecurityZoneDependencies(dependencyObjects, dependencySearchResult.getDependent(), dependencySearchResult.getDependencies(), new ArrayList<DependentObject>());
                    //get all folder dependencies
                    getFolderDependencies(dependencyObjects, dependencySearchResult.getDependent(), dependencySearchResult.getDependencies(), new ArrayList<DependentObject>());
                    //include the DependencySearchResults.dependentObject
                    buildDependentObjectsList(dependencyObjects, dependencySearchResult.getDependent(), dependencySearchResult.getDependencies(), new ArrayList<DependentObject>());
                } else if (dependencySearchResult.getDependencies() != null) {
                    //if we are not including the dependent object in the DependencySearchResults then process only the dependencies.
                    //keep track of processed object, this will improve processing time.
                    final ArrayList<DependentObject> processedObjects = new ArrayList<>();
                    final ArrayList<DependentObject> processedSecurityZones = new ArrayList<>();
                    final ArrayList<DependentObject> processedFolders = new ArrayList<>();
                    //loop throw all the dependencies to build up the dependent objects list.
                    for (final Dependency dependency : dependencySearchResult.getDependencies()) {
                        //get all security zone dependencies
                        getSecurityZoneDependencies(dependencyObjects, dependency.getDependent(), dependency.getDependencies(), processedSecurityZones);
                        //get all folder dependencies
                        getFolderDependencies(dependencyObjects, dependency.getDependent(), dependency.getDependencies(), processedFolders);
                        buildDependentObjectsList(dependencyObjects, dependency.getDependent(), dependency.getDependencies(), processedObjects);
                    }
                }
            }
        }
        return dependencyObjects;
    }

    /**
     * This will return an ordered list of all the folder dependencies.
     *
     * @param dependencyObjects The list of dependency objects built so far.
     * @param dependent         The dependent currently being processed.
     * @param dependencies      current dependent's immediate dependencies.
     * @param processed         The List of dependent objects already processed.
     */
    private void getFolderDependencies(@NotNull final List<Dependency> dependencyObjects, @NotNull final DependentObject dependent, @Nullable final List<Dependency> dependencies, @NotNull final List<DependentObject> processed) {
        // check if dependency is already processed.
        if (processed.contains(dependent)) {
            return;
        }
        //add to the processed list before processing to avoid circular dependency issues.
        processed.add(dependent);

        //if it is a folder dependency add it to the dependency list before processing its children
        if (com.l7tech.search.Dependency.DependencyType.FOLDER.equals(dependent.getDependencyType())) {
            addDependentToDependencyList(dependencyObjects, dependent, dependencies);
        }
        //loop through the folders children to add all other folders to the list.
        if (dependencies != null) {
            for (final Dependency dependency : dependencies) {
                getFolderDependencies(dependencyObjects, dependency.getDependent(), dependency.getDependencies(), processed);
            }
        }
    }

    /**
     * This will return a list of all the security zone dependencies.
     *
     * @param dependencyObjects The list of dependency objects built so far.
     * @param dependent         The dependent currently being processed.
     * @param dependencies      current dependent's immediate dependencies.
     * @param processed         The List of dependent objects already processed.
     */
    private void getSecurityZoneDependencies(@NotNull final List<Dependency> dependencyObjects, @NotNull final DependentObject dependent, @Nullable final List<Dependency> dependencies, @NotNull final List<DependentObject> processed) {
        // check if dependency is already processed.
        if (processed.contains(dependent)) {
            return;
        }
        //add to the processed list before processing to avoid circular dependency issues.
        processed.add(dependent);

        //if the dependent is a security zone add it to the dependencies list.
        if (com.l7tech.search.Dependency.DependencyType.SECURITY_ZONE.equals(dependent.getDependencyType())) {
            addDependentToDependencyList(dependencyObjects, dependent, dependencies);
        }
        //recourse through the other dependencies to find the rest of the security zones.
        if (dependencies != null) {
            for (final Dependency dependency : dependencies) {
                getSecurityZoneDependencies(dependencyObjects, dependency.getDependent(), dependency.getDependencies(), processed);
            }
        }
    }

    /**
     * Builds a list of dependency objects. This is called recursively to process the given dependent and list of
     * dependencies.
     *
     * @param dependencyObjects The list of dependency objects built so far.
     * @param dependent         The dependent currently being processed.
     * @param dependencies      current dependent's immediate dependencies.
     * @param processed         The List of dependent objects already processed.
     */
    private void buildDependentObjectsList(@NotNull final List<Dependency> dependencyObjects, @NotNull final DependentObject dependent, @Nullable final List<Dependency> dependencies, @NotNull final List<DependentObject> processed) {
        // check if dependency is already found.
        if (processed.contains(dependent)) {
            return;
        }
        //add to the processed list before processing to avoid circular dependency issues.
        processed.add(dependent);

        //process the dependent objects dependencies.
        if (dependencies != null) {
            for (final Dependency dependency : dependencies) {
                buildDependentObjectsList(dependencyObjects, dependency.getDependent(), dependency.getDependencies(), processed);
            }
        }
        //A folder or security zone would already have been added to the dependency object list so only do this if it is not a folder or security zone.
        if (!com.l7tech.search.Dependency.DependencyType.FOLDER.equals(dependent.getDependencyType()) &&
                !com.l7tech.search.Dependency.DependencyType.SECURITY_ZONE.equals(dependent.getDependencyType())) {
            addDependentToDependencyList(dependencyObjects, dependent, dependencies);
        }
    }

    /**
     * Adds the dependent object the the dependencyObjects list if it has not been added already. The added dependency
     * will have its immediate dependencies set as well
     *
     * @param dependencyObjects The list of dependency object built so far
     * @param dependent         The dependent object to add to the list
     * @param dependencies      The dependent objects dependencies
     */
    private void addDependentToDependencyList(@NotNull final List<Dependency> dependencyObjects, @NotNull final DependentObject dependent, @Nullable final List<Dependency> dependencies) {
        //Find if the dependent object has been added to the dependencyObjects list.
        Dependency dependency = Functions.grepFirst(dependencyObjects, new Functions.Unary<Boolean, Dependency>() {
            @Override
            public Boolean call(Dependency dependency) {
                return dependency.getDependent().equals(dependent);
            }
        });
        //if it has not been added then add it.
        if (dependency == null) {
            dependency = new Dependency(dependent);
            //add the immediate dependencies to the dependency
            final ArrayList<Dependency> immediateDependencies = new ArrayList<>();
            if (dependencies != null && !dependencies.isEmpty()) {
                for (final Dependency immediateDependency : dependencies) {
                    immediateDependencies.add(new Dependency(immediateDependency.getDependent()));
                }
            }
            //set the immediate dependencies
            dependency.setDependencies(immediateDependencies);
            //add the dependency to the dependencyObjects list
            dependencyObjects.add(dependency);
        }
    }
}
