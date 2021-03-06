package com.l7tech.server.bundling;

import com.l7tech.gateway.common.audit.AuditConfiguration;
import com.l7tech.gateway.common.cluster.ClusterProperty;
import com.l7tech.gateway.common.module.ServerModuleFile;
import com.l7tech.gateway.common.security.keystore.SsgKeyEntry;
import com.l7tech.gateway.common.security.password.SecurePassword;
import com.l7tech.gateway.common.security.rbac.Role;
import com.l7tech.gateway.common.service.PublishedService;
import com.l7tech.gateway.common.solutionkit.EntityOwnershipDescriptor;
import com.l7tech.gateway.common.solutionkit.SolutionKit;
import com.l7tech.gateway.common.transport.InterfaceTag;
import com.l7tech.gateway.common.transport.SsgConnector;
import com.l7tech.gateway.common.transport.jms.JmsConnection;
import com.l7tech.gateway.common.transport.jms.JmsEndpoint;
import com.l7tech.identity.Identity;
import com.l7tech.identity.IdentityProviderConfig;
import com.l7tech.identity.IdentityProviderConfigManager;
import com.l7tech.identity.fed.FederatedGroup;
import com.l7tech.identity.fed.FederatedUser;
import com.l7tech.identity.fed.VirtualGroup;
import com.l7tech.identity.internal.InternalGroup;
import com.l7tech.identity.internal.InternalUser;
import com.l7tech.objectmodel.*;
import com.l7tech.objectmodel.folder.Folder;
import com.l7tech.objectmodel.folder.HasFolder;
import com.l7tech.policy.AssertionAccess;
import com.l7tech.policy.Policy;
import com.l7tech.server.EntityCrud;
import com.l7tech.server.EntityHeaderUtils;
import com.l7tech.server.search.DependencyAnalyzer;
import com.l7tech.server.search.DependencySearchResultsUtils;
import com.l7tech.server.search.exceptions.CannotRetrieveDependenciesException;
import com.l7tech.server.search.objects.BrokenDependency;
import com.l7tech.server.search.objects.Dependency;
import com.l7tech.server.search.objects.DependencySearchResults;
import com.l7tech.server.search.objects.DependentEntity;
import com.l7tech.server.solutionkit.SolutionKitManager;
import com.l7tech.util.CollectionUtils;
import com.l7tech.util.Functions;
import org.jetbrains.annotations.NotNull;

import javax.inject.Inject;
import java.util.*;

/**
 * The bundle exporter will export an entity bundle from this gateway.
 */
public class EntityBundleExporterImpl implements EntityBundleExporter {
    //DEFAULTS and options
    public static final String IncludeRequestFolderOption = "IncludeRequestFolder";
    public static final String DefaultMappingActionOption = "DefaultMappingAction";
    public static final String DefaultMapByOption = "DefaultMapBy";
    public static final String IgnoredEntityIdsOption = "IgnoredEntityIds";
    public static final String IgnoreDependenciesOption = "IgnoreDependencies";
    public static final String EncassAsPolicyDependencyOption = "EncassAsPolicyDependency";
    public static final String ServiceUsed = "ServiceUsed"; // service id used to access this exporter
    private static final String IncludeRequestFolder = "false";
    private static final EntityMappingInstructions.MappingAction DefaultMappingAction = EntityMappingInstructions.MappingAction.NewOrExisting;
    private static final String DefaultMapBy = "ID";
    public static final String IncludeSolutionKitsOption = "IncludeSolutionKits";
    private static final String IncludeSolutionKits = "false";
    private static final String EncassAsPolicyDependencyDefault = "false";
    public static final String IncludeOnlyServicePolicyOption = "IncludeOnlyServicePolicy";
    public static final String IncludeOnlyServicePolicy = "false";
    public static final String IncludeOnlyDependenciesOption = "IncludeOnlyDependencies";
    public static final String IncludeOnlyDependencies = "false";
    public static final String IncludeGatewayConfigurationOption = "IncludeGatewayConfiguration";
    public static final String IncludeGatewayConfiguration = "false";
    /**
     * The mapping action for read-only entities owned by {@code SolutionKit}'s.
     */
    private static final EntityMappingInstructions.MappingAction DEFAULT_READ_ONLY_ENTITY_MAPPING_ACTION = EntityMappingInstructions.MappingAction.NewOrExisting;
    @Inject
    private DependencyAnalyzer dependencyAnalyzer;
    @Inject
    private EntityCrud entityCrud;
    @Inject
    private MappingInstructionsBuilder mappingInstructionsBuilder;
    @Inject
    private SolutionKitManager solutionKitManager;

    /**
     * Builder the dependency analyzer options from the bundleExportProperties
     *
     * @param bundleExportProperties The bundle export properties
     * @return The dependency analyzer options.
     */
    @NotNull
    private static Map<String, Object> buildDependencyAnalyzerOptions(@NotNull final Properties bundleExportProperties) {
        final CollectionUtils.MapBuilder<String, Object> optionBuilder = CollectionUtils.MapBuilder.builder();
        if (bundleExportProperties.containsKey(IgnoredEntityIdsOption)) {
            optionBuilder.put(DependencyAnalyzer.IgnoreSearchOptionKey, Arrays.asList(bundleExportProperties.getProperty(IgnoredEntityIdsOption).split(",")));
        }
        if (bundleExportProperties.containsKey(IgnoreDependenciesOption)) {
            optionBuilder.put(DependencyAnalyzer.IgnoreDependenciesSearchOptionKey, bundleExportProperties.get(IgnoreDependenciesOption));
        }
        if (bundleExportProperties.containsKey(EncassAsPolicyDependencyOption)) {
            optionBuilder.put(DependencyAnalyzer.EncassAsPolicyDependencyOptionKey, Boolean.parseBoolean(bundleExportProperties.getProperty(EncassAsPolicyDependencyOption, EncassAsPolicyDependencyDefault)));
        }
        optionBuilder.put(DependencyAnalyzer.IncludeSolutionKitsOptionKey, Boolean.parseBoolean(bundleExportProperties.getProperty(IncludeSolutionKitsOption, IncludeSolutionKits)));
        // do not need assertion dependency results for building export bundle
        optionBuilder.put(DependencyAnalyzer.ReturnAssertionsAsDependenciesOptionKey, false);
        optionBuilder.put("IncludeOnlyServicePolicyOption",Boolean.parseBoolean(bundleExportProperties.getProperty(IncludeOnlyServicePolicyOption, IncludeOnlyServicePolicy)));
        optionBuilder.put("IncludeOnlyDependenciesOption",Boolean.parseBoolean(bundleExportProperties.getProperty(IncludeOnlyDependenciesOption, IncludeOnlyDependencies)));
        optionBuilder.put("IncludeGatewayConfiguration",Boolean.parseBoolean(bundleExportProperties.getProperty(IncludeGatewayConfigurationOption, IncludeGatewayConfiguration)));
        return optionBuilder.map();
    }

    /**
     * Exports a bundle given a list of entity headers to export. This will find all the dependencies needed to import
     * the bundle.
     *
     * @param bundleExportProperties Properties to export the bundle with.
     * @param headers                The entity headers to export. If the headers list is empty the full gateway will be exported.
     * @return The entity bundle containing all entities in the bundle and the mappings required to import them.
     * @throws FindException
     */
    @NotNull
    @Override
    public EntityBundle exportBundle(@NotNull final Properties bundleExportProperties, @NotNull final EntityHeader... headers) throws FindException, CannotRetrieveDependenciesException {
        //find the dependencies for the headers
        final List<DependencySearchResults> dependencySearchResults = dependencyAnalyzer.getDependencies(Arrays.asList(headers), buildDependencyAnalyzerOptions(bundleExportProperties));
        //create a flat dependency list
        final List<Dependency> dependentObjects = DependencySearchResultsUtils.flattenDependencySearchResults(dependencySearchResults, true);

        // gather all read-only entities owned by Solution Kits
        final Set<String> readOnlyEntities = getReadOnlyEntities();

        final ArrayList<EntityContainer> entityContainers = new ArrayList<>();
        final ArrayList<EntityMappingInstructions> mappings = new ArrayList<>();

        for (final Dependency dependentObject : dependentObjects) {
            //for each dependent object add a reference and mapping entry to the bundle.
            if(dependentObject instanceof BrokenDependency){
                // create a map only dependency for a broken dependency
                EntityHeader header = ((DependentEntity) dependentObject.getDependent()).getEntityHeader();
                // include as much info for the missing entity as possible
                EntityMappingInstructions.TargetMapping targetMapping = null;
                if( header.getGoid() == null || Goid.isDefault(header.getGoid())){
                    if (header instanceof GuidEntityHeader) {
                        targetMapping = new EntityMappingInstructions.TargetMapping(EntityMappingInstructions.TargetMapping.Type.GUID, ((GuidEntityHeader) header).getGuid());
                    } else if (header.getName() != null) {
                        targetMapping = new EntityMappingInstructions.TargetMapping(EntityMappingInstructions.TargetMapping.Type.NAME, header.getName());
                    }
                }
                final EntityMappingInstructions brokenMapping = new EntityMappingInstructions(
                        header,
                        targetMapping,
                        EntityMappingInstructions.MappingAction.Ignore,
                        false,
                        false);

                if( !mappings.contains(brokenMapping) ) {
                    mappings.add(brokenMapping);
                }
            }
            else if (dependentObject.getDependent() instanceof DependentEntity) {
                //checks if this dependent is a folder and is a folder that was requested for export (in the list of entity headers)
                if (headers.length > 0
                        && !Boolean.parseBoolean(bundleExportProperties.getProperty(IncludeRequestFolderOption, IncludeRequestFolder))
                        && EntityType.FOLDER.equals(((DependentEntity) dependentObject.getDependent()).getEntityHeader().getType())
                        && Functions.exists(Arrays.asList(headers), new Functions.Unary<Boolean, EntityHeader>() {
                    @Override
                    public Boolean call(EntityHeader entityHeader) {
                        return Goid.equals(((DependentEntity) dependentObject.getDependent()).getEntityHeader().getGoid(), entityHeader.getGoid());
                    }
                })) {
                    //remove any request folders from the returned export.
                    continue;
                }

                //find the entity
                final Entity entity = entityCrud.find(((DependentEntity) dependentObject.getDependent()).getEntityHeader());
                if (entity == null) {
                    throw new FindException("Could not load entity from dependency found: " + ((DependentEntity) dependentObject.getDependent()).getEntityHeader().toStringVerbose());
                }

                boolean bAddEntitiesAndMapping = true;
                boolean bIncludeOnlyDependencies = Boolean.parseBoolean(bundleExportProperties.getProperty(IncludeOnlyDependenciesOption, IncludeOnlyDependencies));
                // Include only dependencies ignores Service, Policy and Folder
                if (bIncludeOnlyDependencies) {
                    if (entity instanceof PublishedService || entity instanceof Policy || entity instanceof Folder) {
                        bAddEntitiesAndMapping = false;
                    }
                }

                if (bAddEntitiesAndMapping) {
                    addMapping(bundleExportProperties, mappings, (DependentEntity) dependentObject.getDependent(), entity, readOnlyEntities);
                    addEntities(bundleExportProperties, entity, entityContainers);
                }
            }
        }

        return new EntityBundle(entityContainers, mappings, dependencySearchResults);
    }

    /**
     * This will add an entity to the entity containers list, properly creating the entity container containing the
     * entity
     *
     * @param bundleExportProperties The bundling properties
     * @param entity                 The entity to add to the list
     * @param entityContainers       The entity containers list
     * @throws FindException
     */
    private void addEntities(@NotNull final Properties bundleExportProperties, @NotNull final Entity entity, @NotNull final List<EntityContainer> entityContainers) throws FindException {
        if (entity instanceof JmsEndpoint) {
            final JmsEndpoint endpoint = (JmsEndpoint) entity;
            final Entity connection = entityCrud.find(new EntityHeader(endpoint.getConnectionGoid(), EntityType.JMS_CONNECTION, null, null));
            if (connection == null)
                throw new FindException("Cannot find associated jms connection for jms endpoint: " + endpoint.getName());
            entityContainers.add(new JmsContainer(endpoint, (JmsConnection) connection));
        } else if (entity instanceof SsgKeyEntry && (!bundleExportProperties.containsKey("EncryptSecrets") || !Boolean.valueOf(bundleExportProperties.getProperty("EncryptSecrets")))) {
            // not include private key entity info in bundle unless EncryptSecrets is true
        } else if (entity instanceof InterfaceTag) {
            entityContainers.add(new InterfaceTagContainer((InterfaceTag) entity));
        } else if (entity instanceof AssertionAccess) {
            entityContainers.add(new AssertionAccessContainer((AssertionAccess) entity));
        } else if (entity instanceof VirtualGroup) {
            // not include Federated Virtual Groups
        } else {
            entityContainers.add(new EntityContainer<>(entity));
        }
    }

    /**
     * This will add a mapping to the mappings list.
     *
     * @param bundleExportProperties The bundling properties
     * @param mappings               The list of mappings to add the mapping to
     * @param dependentObject        The dependent object to create the mapping for
     * @param entity                 The entity that this dependent object is for
     * @param readOnlyEntities       All read-only entities (entity id's) owned by {@code SolutionKit}'s
     */
    private void addMapping(@NotNull final Properties bundleExportProperties, @NotNull final ArrayList<EntityMappingInstructions> mappings, @NotNull final DependentEntity dependentObject, @NotNull final Entity entity, @NotNull final Set<String> readOnlyEntities) {
        EntityMappingInstructions.MappingAction defaultAction = EntityMappingInstructions.MappingAction.valueOf(bundleExportProperties.getProperty(DefaultMappingActionOption, DefaultMappingAction.toString()));
        if (entity instanceof HasFolder && !(bundleExportProperties.containsKey(IgnoreDependenciesOption) && ((List)bundleExportProperties.get(IgnoreDependenciesOption)).contains(EntityHeaderUtils.fromEntity(entity)))) {
            // include parent folder mapping if not already in mapping.
            final Folder parentFolder = ((HasFolder) entity).getFolder();
            if(parentFolder != null) {
                final EntityMappingInstructions folderMapping = new EntityMappingInstructions(
                        EntityHeaderUtils.fromEntity(parentFolder),
                        null,
                        calcAction(parentFolder, readOnlyEntities, (defaultAction.equals(EntityMappingInstructions.MappingAction.Ignore) ? defaultAction : EntityMappingInstructions.MappingAction.NewOrExisting)),
                        true,
                        false);
                if( !mappings.contains(folderMapping) ) {
                    mappings.add(folderMapping);
                }
            }
        }

        final boolean secretsEncrypted = bundleExportProperties.containsKey("EncryptSecrets") &&  Boolean.valueOf(bundleExportProperties.getProperty("EncryptSecrets"));
        final EntityMappingInstructions mapping;
        if (entity instanceof InternalUser){
            if(((InternalUser) entity).getGoid().equals(new Goid(0,3)))
            {
                // Only use existing for IIP and ADMIN user
                mapping = new EntityMappingInstructions(
                        dependentObject.getEntityHeader(),
                        null,
                        calcAction(entity, readOnlyEntities, (defaultAction.equals(EntityMappingInstructions.MappingAction.Ignore) ? defaultAction : EntityMappingInstructions.MappingAction.NewOrExisting)),
                        true,
                        false);
            }else if(secretsEncrypted){
                mapping = new EntityMappingInstructions(
                        dependentObject.getEntityHeader(),
                        null,
                        calcAction(entity, readOnlyEntities, defaultAction),
                        false,
                        false);
            }else{
                // users with no secrets are incomplete, should force to use existing
                mapping = new EntityMappingInstructions(
                        dependentObject.getEntityHeader(),
                        null,
                        calcAction(entity, readOnlyEntities, (defaultAction.equals(EntityMappingInstructions.MappingAction.Ignore) ? defaultAction : EntityMappingInstructions.MappingAction.NewOrExisting)),
                        true,
                        false);
            }
        }else if (entity instanceof IdentityProviderConfig && IdentityProviderConfigManager.INTERNALPROVIDER_SPECIAL_GOID.equals(((IdentityProviderConfig) entity).getGoid())){
            // only map only for non internal and non federated identity
            mapping = new EntityMappingInstructions(
                    dependentObject.getEntityHeader(),
                    null,
                    calcAction(entity, readOnlyEntities, (defaultAction.equals(EntityMappingInstructions.MappingAction.Ignore) ? defaultAction : EntityMappingInstructions.MappingAction.NewOrExisting)),
                    true,
                    false);
        }else if (entity instanceof Identity && !(entity instanceof InternalGroup || entity instanceof FederatedUser || entity instanceof FederatedGroup || entity instanceof VirtualGroup)){
            // only map only for non internal and non federated identity
                mapping = new EntityMappingInstructions(
                        dependentObject.getEntityHeader(),
                        null,
                        calcAction(entity, readOnlyEntities, (defaultAction.equals(EntityMappingInstructions.MappingAction.Ignore) ? defaultAction : EntityMappingInstructions.MappingAction.NewOrExisting)),
                        true,
                        false);
        }else if((entity instanceof Folder && ((Folder)entity).getGoid().equals(Folder.ROOT_FOLDER_ID)) ||
                //private keys and secure passwords should only be map only if EncryptSecrets is not specified.
                ((entity instanceof SsgKeyEntry || entity instanceof SecurePassword) && !secretsEncrypted) ||
                entity instanceof VirtualGroup) {
            // Make these entities map only. Set fail on new true and Mapping action NewOrExisting
            mapping = new EntityMappingInstructions(
                    dependentObject.getEntityHeader(),
                    null,
                    calcAction(entity, readOnlyEntities, (defaultAction.equals(EntityMappingInstructions.MappingAction.Ignore) ? defaultAction : EntityMappingInstructions.MappingAction.NewOrExisting)),
                    true,
                    false);
        } else if (entity instanceof Role && !((Role)entity).isUserCreated()){
            if(((Role)entity).getEntityGoid() != null && ((Role)entity).getEntityType() != null) {
                // These are auto generated roles for managing specific entities. Map them with the MAP_BY_ROLE_ENTITY option, and set fail on new to true
                mapping = new EntityMappingInstructions(
                        dependentObject.getEntityHeader(),
                        new EntityMappingInstructions.TargetMapping(EntityMappingInstructions.TargetMapping.Type.MAP_BY_ROLE_ENTITY),
                        calcAction(entity, readOnlyEntities, defaultAction),
                        true,
                        false);
            } else {
                // These are other built in roles. They should fail on new.
                mapping = new EntityMappingInstructions(
                        dependentObject.getEntityHeader(),
                        null,
                        calcAction(entity, readOnlyEntities, EntityMappingInstructions.MappingAction.valueOf(bundleExportProperties.getProperty(DefaultMappingActionOption, DefaultMappingAction.toString()))),
                        true,
                        false);
            }
        } else if(entity instanceof SsgConnector && isDefaultListenPortName(((SsgConnector) entity).getName()) ) {
            //make the default listen ports map by name
            mapping = new EntityMappingInstructions(
                    dependentObject.getEntityHeader(),
                    new EntityMappingInstructions.TargetMapping(EntityMappingInstructions.TargetMapping.Type.NAME),
                    calcAction(entity, readOnlyEntities, defaultAction),
                    true,
                    false);
        } else if(entity instanceof PublishedService && entity.getId().equals(bundleExportProperties.getProperty(ServiceUsed)) ) {
            //make the service used to access this exporter(restman) map by name, fail on new
            mapping = new EntityMappingInstructions(
                    dependentObject.getEntityHeader(),
                    new EntityMappingInstructions.TargetMapping(EntityMappingInstructions.TargetMapping.Type.NAME),
                    calcAction(entity, readOnlyEntities, defaultAction),
                    true,
                    false);
        } else if(entity instanceof ClusterProperty || entity instanceof AssertionAccess) {
            //make cluster properties and assertion access map by name by default
            mapping = new EntityMappingInstructions(
                    dependentObject.getEntityHeader(),
                    new EntityMappingInstructions.TargetMapping(EntityMappingInstructions.TargetMapping.Type.NAME),
                    calcAction(entity, readOnlyEntities, defaultAction),
                    false,
                    false);
        } else if(entity instanceof ServerModuleFile) {
            //make cluster properties and assertion access map by name by default
            mapping = new EntityMappingInstructions(
                    dependentObject.getEntityHeader(),
                    new EntityMappingInstructions.TargetMapping(EntityMappingInstructions.TargetMapping.Type.MODULE_SHA265),
                    calcAction(entity, readOnlyEntities, (defaultAction.equals(EntityMappingInstructions.MappingAction.Ignore) ? defaultAction : EntityMappingInstructions.MappingAction.NewOrExisting)),
                    false,
                    false);
        } else if(entity instanceof AuditConfiguration) {
            //make audit configuration update by default
            mapping = new EntityMappingInstructions(
                    dependentObject.getEntityHeader(),
                    new EntityMappingInstructions.TargetMapping(EntityMappingInstructions.TargetMapping.Type.ID),
                    EntityMappingInstructions.MappingAction.NewOrExisting,
                    true,
                    false);
        } else {
            //create the default mapping instructions
            mapping = mappingInstructionsBuilder.createDefaultMapping(dependentObject.getEntityHeader(),
                    calcAction(entity, readOnlyEntities, defaultAction),
                    EntityMappingInstructions.TargetMapping.Type.valueOf(bundleExportProperties.getProperty(DefaultMapByOption, DefaultMapBy).toUpperCase()));
        }

        // add the mapping
        // ensure there's only one mapping instruction for that entity
        int index = mappings.indexOf(mapping);
        if( index > 0 ) {
            mappings.remove(index);
            mappings.add(index,mapping);
        }else{
            mappings.add(mapping);
        }
    }

    /**
     * Generate the {@link EntityMappingInstructions.MappingAction MappingAction} for the specified {@code entity}.
     *
     * @param entity                  the entity to check
     * @param readOnlyEntities        read-only entities owned by {@link SolutionKit}'s.
     * @param defaultMappingAction    the original requested action for the entity.
     * @return If the specified {@code entity} is read-only (i.e. its id is contained inside {@code readOnlyEntities} {@code Set})
     * then return {@link EntityMappingInstructions.MappingAction#NewOrExisting NewOrExisting}, otherwise return the {@code defaultMappingAction}.
     */
    private EntityMappingInstructions.MappingAction calcAction(
            @NotNull final Entity entity,
            @NotNull final Set<String> readOnlyEntities,
            @NotNull final EntityMappingInstructions.MappingAction defaultMappingAction
    ) {
        // if DEFAULT_READ_ONLY_ENTITY_MAPPING_ACTION is already requested then we don't need to check the readonly-ness
        if (DEFAULT_READ_ONLY_ENTITY_MAPPING_ACTION.equals(defaultMappingAction)) {
            return defaultMappingAction;
        }
        // todo: add entity type checks if needed, for instance skip entities that cannot be read-only (if there are such)
        return readOnlyEntities.contains(entity.getId())
                ? DEFAULT_READ_ONLY_ENTITY_MAPPING_ACTION // if entity is read-only then force DEFAULT_READ_ONLY_ENTITY_MAPPING_ACTION (i.e. currently NewOrExisting)
                : defaultMappingAction; // otherwise use the default action
    }

    /**
     * Returns true if the name given is a name for a default listen port.
     * @param name The name to test
     * @return True is this is a name for a default listen port, false otherwise.
     */
    private boolean isDefaultListenPortName(final String name) {
        return name != null && (
                "Default HTTP (8080)".equals(name) ||
                        "Default HTTPS (8443)".equals(name) ||
                        "Default HTTPS (9443)".equals(name) ||
                        "Node HTTPS (2124)".equals(name));
    }

    /**
     * @return Unmodifiable {@code Set} of read-only entity ids, owned by {@link com.l7tech.gateway.common.solutionkit.SolutionKit SolutionKit}'s
     */
    private Set<String> getReadOnlyEntities() throws FindException {
        final Set<String> roEntities = new HashSet<>();
        final Collection<SolutionKit> solutionKits = solutionKitManager.findAll();
        if (solutionKits != null && !solutionKits.isEmpty()) {
            for (final SolutionKit solutionKit : solutionKits) {
                final Collection<EntityOwnershipDescriptor> descriptors = solutionKit.getEntityOwnershipDescriptors();
                if (descriptors != null && !descriptors.isEmpty()) {
                    for (final EntityOwnershipDescriptor descriptor : descriptors) {
                        if (descriptor.isReadOnly()) {
                            roEntities.add(descriptor.getEntityId());
                        }
                    }
                }
            }
        }
        return Collections.unmodifiableSet(roEntities);
    }
}
