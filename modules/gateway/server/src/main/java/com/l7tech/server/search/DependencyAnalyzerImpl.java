package com.l7tech.server.search;

import com.l7tech.gateway.common.cassandra.CassandraConnection;
import com.l7tech.gateway.common.cluster.ClusterProperty;
import com.l7tech.gateway.common.jdbc.JdbcConnection;
import com.l7tech.gateway.common.resources.HttpConfiguration;
import com.l7tech.gateway.common.resources.ResourceEntry;
import com.l7tech.gateway.common.security.RevocationCheckPolicy;
import com.l7tech.gateway.common.security.keystore.SsgKeyEntry;
import com.l7tech.gateway.common.security.password.SecurePassword;
import com.l7tech.gateway.common.security.rbac.Role;
import com.l7tech.gateway.common.security.rbac.RoleEntityHeader;
import com.l7tech.gateway.common.service.PublishedService;
import com.l7tech.gateway.common.service.PublishedServiceAlias;
import com.l7tech.gateway.common.service.SampleMessage;
import com.l7tech.gateway.common.siteminder.SiteMinderConfiguration;
import com.l7tech.gateway.common.transport.InterfaceTag;
import com.l7tech.gateway.common.transport.SsgActiveConnector;
import com.l7tech.gateway.common.transport.SsgConnector;
import com.l7tech.gateway.common.transport.email.EmailListener;
import com.l7tech.gateway.common.transport.firewall.SsgFirewallRule;
import com.l7tech.gateway.common.transport.jms.JmsEndpoint;
import com.l7tech.identity.Group;
import com.l7tech.identity.IdentityProvider;
import com.l7tech.identity.IdentityProviderConfig;
import com.l7tech.identity.fed.FederatedGroup;
import com.l7tech.identity.fed.FederatedUser;
import com.l7tech.identity.fed.VirtualGroup;
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
import com.l7tech.server.identity.IdentityProviderFactory;
import com.l7tech.server.identity.fed.FederatedIdentityProvider;
import com.l7tech.server.policy.PolicyManager;
import com.l7tech.server.search.exceptions.CannotReplaceDependenciesException;
import com.l7tech.server.search.exceptions.CannotRetrieveDependenciesException;
import com.l7tech.server.search.objects.DependencySearchResults;
import com.l7tech.server.search.objects.DependentObject;
import com.l7tech.server.search.processors.DependencyFinder;
import com.l7tech.server.search.processors.DependencyProcessorStore;
import com.l7tech.util.ConfigFactory;
import com.l7tech.util.ExceptionUtils;
import com.l7tech.util.Functions;
import org.jetbrains.annotations.NotNull;

import javax.inject.Inject;
import java.text.ParseException;
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
    private static final List<Class<? extends Entity>> entityClasses = Arrays.asList(
            //Omit policy version for now.
            //folders need to be done first to ensure folder order.
            Folder.class,
            SsgActiveConnector.class,
            AssertionAccess.class,
            TrustedCert.class,
            ClusterProperty.class,
            CustomKeyValueStore.class,
//            ServiceDocument.class, These are not treated as restman resources
            EmailListener.class,
            EncapsulatedAssertionConfig.class,
            GenericEntity.class,
            HttpConfiguration.class,
            IdentityProviderConfig.class,
            InterfaceTag.class,
            CassandraConnection.class,
            JdbcConnection.class,
//            JmsConnection.class, JMS connections are not needed here they are treated as part of the endpoint
            JmsEndpoint.class,
            SsgConnector.class,
            PolicyAlias.class,
            Policy.class,
            SsgKeyEntry.class,
            RevocationCheckPolicy.class,
            PublishedService.class,
            InternalUser.class,
            InternalGroup.class,
            FederatedUser.class,
            FederatedGroup.class,
            //Users and groups need to come before roles! This is so that role assignments can be properly mapped on import
            Role.class,
            SiteMinderConfiguration.class,
            SecurePassword.class,
            SecurityZone.class,
            PublishedServiceAlias.class,
            SsgFirewallRule.class,
            SampleMessage.class,
            ResourceEntry.class
    );
    @Inject
    private EntityCrud entityCrud;
    @Inject
    private IdentityProviderFactory identityProviderFactory;
    @Inject
    private FolderManager folderManager;
    @Inject
    private DependencyProcessorStore processorStore;
    @Inject
    private PolicyManager policyManager;

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
            headerLists = loadAllGatewayEntities(searchOptions);
        } else {
            headerLists = new ArrayList<>();
            headerLists.add(entityHeaders);
        }

        //create a new dependency finder to perform the search
        final DependencyFinder dependencyFinder = new DependencyFinder(searchOptions, processorStore);
        final List<DependencySearchResults> results = new ArrayList<>();

        //Load the entities from the given entity headers.
        for (final List<EntityHeader> headerList : headerLists) {
            final ArrayList<DependencyFinder.FindResults> entities = new ArrayList<>(entityHeaders.size());
            for (final EntityHeader entityHeader : headerList) {
                final Entity entity = entityCrud.find(entityHeader);
                if (entity == null) {
                    throw new ObjectNotFoundException("Could not find Entity with header: " + entityHeader.toStringVerbose());
                }
                entities.add(DependencyFinder.FindResults.create(entity, entityHeader));
            }
            results.addAll(dependencyFinder.process(entities));
        }
        return results;
    }

    /**
     * Returns the list of dependencies for the given entities.
     *
     * @param entities      The list of entities to get dependencies for. If the entities list is empty the full
     *                      gateway dependencies will be returned.
     * @param searchOptions The search options. These can be used to customize the search. It can be used to specify
     *                      that some entities can be ignored, or to make it so that individual assertions are returned
     *                      as dependencies.
     * @return The list of dependency search results. This list will be the same size as the entities list.
     * @throws FindException This is thrown if an entity cannot be found by the entity managers.
     */
    @NotNull
    public List<DependencySearchResults> getDependenciesFromEntities(@NotNull final List<Entity> entities, @NotNull final Map<String, Object> searchOptions) throws CannotRetrieveDependenciesException, FindException {
        if (entities.isEmpty()) {
            return getDependencies(Collections.<EntityHeader>emptyList(), searchOptions);
        }
        logger.log(Level.FINE, "Finding dependencies for {0}", entities.toString());
        //create a new dependency finder to perform the search
        final DependencyFinder dependencyFinder = new DependencyFinder(searchOptions, processorStore);
        final List<DependencySearchResults> results = new ArrayList<>();

        results.addAll(dependencyFinder.process(Functions.map(entities, new Functions.Unary<DependencyFinder.FindResults, Entity>() {
            @Override
            public DependencyFinder.FindResults call(Entity entity) {
                return DependencyFinder.FindResults.create(entity, null);
            }
        })));
        return results;
    }

    /**
     * This will return a set lists of all entity headers for each entity type.
     *
     * @return The list of all entity headers.
     * @throws FindException
     */
    private List<List<EntityHeader>> loadAllGatewayEntities(@NotNull final Map<String, Object> searchOptions) throws FindException {
        final List<List<EntityHeader>> headerLists = new ArrayList<>();
        for (final Class<? extends Entity> entityClass : entityClasses) {
            final EntityHeaderSet<EntityHeader> entityHeaders;
            if (Policy.class.equals(entityClass)) {
                //exclude private service policies
                EntityHeaderSet<EntityHeader> policyHeaders = entityCrud.findAll(entityClass);
                entityHeaders = policyHeaders == null ? null : Functions.reduce(policyHeaders, new EntityHeaderSet<>(), new Functions.Binary<EntityHeaderSet<EntityHeader>, EntityHeaderSet<EntityHeader>, EntityHeader>() {
                    @Override
                    public EntityHeaderSet<EntityHeader> call(EntityHeaderSet<EntityHeader> objects, EntityHeader entityHeader) {
                        if (!PolicyType.PRIVATE_SERVICE.equals(((PolicyHeader) entityHeader).getPolicyType())) {
                            objects.add(entityHeader);
                        }
                        return objects;
                    }
                });
            } else if (Folder.class.equals(entityClass)) {
                //folders only need to include the root folder.
                entityHeaders = new EntityHeaderSet<>(EntityHeaderUtils.fromEntity(folderManager.findRootFolder()));
            } else if (ClusterProperty.class.equals(entityClass)){
                //should not export hidden cluster properties
                @SuppressWarnings("unchecked")
                final List<ClusterProperty> clusterProperties = (List<ClusterProperty>) entityCrud.findAll(entityClass, null, 0, -1, null, null);
                //remove the hidden cluster properties
                entityHeaders = Functions.reduce(clusterProperties, new EntityHeaderSet<>(), new Functions.Binary<EntityHeaderSet<EntityHeader>, EntityHeaderSet<EntityHeader>, ClusterProperty>() {
                    @Override
                    public EntityHeaderSet<EntityHeader> call(final EntityHeaderSet<EntityHeader> entityHeaders, ClusterProperty clusterProperty) {
                        if (!clusterProperty.isHiddenProperty()) {
                            entityHeaders.add(EntityHeaderUtils.fromEntity(clusterProperty));
                        }
                        return entityHeaders;
                    }
                });
            } else if (FederatedUser.class.equals(entityClass)){
                //find all federated identity providers
                final List<IdentityProvider> federatedIdentityProviders = Functions.grep(identityProviderFactory.findAllIdentityProviders(), new Functions.Unary<Boolean, IdentityProvider>() {
                    @Override
                    public Boolean call(final IdentityProvider identityProvider) {
                        return identityProvider instanceof FederatedIdentityProvider;
                    }
                });
                entityHeaders = new EntityHeaderSet<>();
                for(final IdentityProvider identityProvider : federatedIdentityProviders){
                    entityHeaders.addAll(identityProvider.getUserManager().findAllHeaders());
                }
            } else if (FederatedGroup.class.equals(entityClass)){
                final List<IdentityProvider> federatedIdentityProviders = Functions.grep(identityProviderFactory.findAllIdentityProviders(), new Functions.Unary<Boolean, IdentityProvider>() {
                    @Override
                    public Boolean call(final IdentityProvider identityProvider) {
                        return identityProvider instanceof FederatedIdentityProvider;
                    }
                });
                entityHeaders = new EntityHeaderSet<>();
                for(final IdentityProvider identityProvider : federatedIdentityProviders){
                    // filter out virtual groups
                    final List<Group> fedGroups = Functions.grep(identityProvider.getGroupManager().findAll(), new Functions.Unary<Boolean, Group>() {
                        @Override
                        public Boolean call(final Group group) {
                            return ! (group instanceof VirtualGroup);
                        }
                    });
                    entityHeaders.addAll(Functions.map(fedGroups,new Functions.Unary<EntityHeader, Group>() {
                        @Override
                        public EntityHeader call(Group group) {
                            return EntityHeaderUtils.fromEntity(group);
                        }
                    }));
                }
            } else if (InterfaceTag.class.equals(entityClass)) {
                final String stringForm = ConfigFactory.getUncachedConfig().getProperty(InterfaceTag.PROPERTY_NAME);
                Set<InterfaceTag> tags;
                try {
                    tags = stringForm == null ? Collections.<InterfaceTag>emptySet() : InterfaceTag.parseMultiple(stringForm);
                } catch (ParseException e) {
                    throw new FindException("Could not load InterfaceTags: " + ExceptionUtils.getMessageWithCause(e), e);
                }
                entityHeaders = new EntityHeaderSet<>();
                if (tags != null) {
                    for (final InterfaceTag tag : tags) {
                        entityHeaders.add(EntityHeaderUtils.fromEntity(tag));
                    }
                }
            } else if (Role.class.equals(entityClass)){
                //should not include roles for ignored entities in the full gateway bundle.
                //Note that roles will still be included for these entities if they are directly referenced from elsewhere
                final List ignoreIds = PropertiesUtil.getOption(DependencyAnalyzer.IgnoreSearchOptionKey, List.class, (List) Collections.emptyList(), searchOptions);

                final EntityHeaderSet<EntityHeader> roleHeaders = entityCrud.findAll(entityClass);
                entityHeaders = new EntityHeaderSet<>();
                for(final EntityHeader header : roleHeaders){
                    if(header instanceof RoleEntityHeader){
                        final RoleEntityHeader roleHeader = (RoleEntityHeader)header;
                        if(roleHeader.getEntityGoid() != null && ignoreIds.contains(roleHeader.getEntityGoid().toString())){
                            //this is the role for an ignored entity so don't include the role
                            continue;
                        }
                        //filter out roles for private service policies
                        if(EntityType.POLICY.equals(roleHeader.getEntityType()) && roleHeader.getEntityGoid() != null){
                            try {
                                final Policy policy = policyManager.findByPrimaryKey(roleHeader.getEntityGoid());
                                //ignore the role if the policy is not found, or if it is a private service policy
                                if(policy == null || PolicyType.PRIVATE_SERVICE.equals(policy.getType())){
                                    continue;
                                }
                            } catch(FindException e) {
                                //do nothing let it fall through
                            }
                        } else if (EntityType.LOG_SINK.equals(roleHeader.getEntityType()) && !Goid.equals(new Goid(0, -810), roleHeader.getEntityGoid())){
                            //ignore the non default log sink configuration roles
                            //TODO: need to remove this when adding support for log sink config.
                            continue;
                        }
                        entityHeaders.add(roleHeader);
                    } else {
                        throw new FindException("Unexpected header type for role: " + header.getClass());
                    }
                }
            } else {
                entityHeaders = entityCrud.findAll(entityClass);
            }
            if (entityHeaders != null) {
                headerLists.add(new ArrayList<>(entityHeaders));
            }
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
     * Creates a dependent object given an object
     *
     * @param object The object to create a dependent object from
     * @return The dependent object
     */
    @NotNull
    DependentObject createDependentObject(@NotNull final Object object) {
        final DependencyFinder dependencyFinder = new DependencyFinder(Collections.<String, Object>emptyMap(), processorStore);
        return dependencyFinder.createDependentObject(object);
    }
}
