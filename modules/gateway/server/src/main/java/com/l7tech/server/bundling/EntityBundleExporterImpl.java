package com.l7tech.server.bundling;

import com.l7tech.gateway.common.security.keystore.SsgKeyEntry;
import com.l7tech.gateway.common.security.password.SecurePassword;
import com.l7tech.gateway.common.security.rbac.Role;
import com.l7tech.gateway.common.transport.jms.JmsConnection;
import com.l7tech.gateway.common.transport.jms.JmsEndpoint;
import com.l7tech.identity.Identity;
import com.l7tech.identity.IdentityProviderConfig;
import com.l7tech.objectmodel.*;
import com.l7tech.objectmodel.folder.Folder;
import com.l7tech.objectmodel.folder.HasFolder;
import com.l7tech.server.EntityCrud;
import com.l7tech.server.EntityHeaderUtils;
import com.l7tech.server.search.DependencyAnalyzer;
import com.l7tech.server.search.DependencySearchResultsUtils;
import com.l7tech.server.search.exceptions.CannotRetrieveDependenciesException;
import com.l7tech.server.search.objects.BrokenDependency;
import com.l7tech.server.search.objects.Dependency;
import com.l7tech.server.search.objects.DependencySearchResults;
import com.l7tech.server.search.objects.DependentEntity;
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
    private static final String IncludeRequestFolder = "false";
    private static final EntityMappingInstructions.MappingAction DefaultMappingAction = EntityMappingInstructions.MappingAction.NewOrExisting;
    private static final String DefaultMapBy = "ID";
    @Inject
    private DependencyAnalyzer dependencyAnalyzer;
    @Inject
    private EntityCrud entityCrud;
    @Inject
    private MappingInstructionsBuilder mappingInstructionsBuilder;

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
        // do not need assertion dependency results for building export bundle
        optionBuilder.put(DependencyAnalyzer.ReturnAssertionsAsDependenciesOptionKey, false);
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

                addMapping(bundleExportProperties, mappings, (DependentEntity) dependentObject.getDependent(), entity);
                addEntities(entity, entityContainers);
            }
        }

        return new EntityBundle(entityContainers, mappings, dependencySearchResults);
    }

    /**
     * This will add an entity to the entity containers list, properly creating the entity container containing the
     * entity
     *
     * @param entity           The entity to add to the list
     * @param entityContainers The entity containers list
     * @throws FindException
     */
    private void addEntities(@NotNull final Entity entity, @NotNull final List<EntityContainer> entityContainers) throws FindException {
        if (entity instanceof JmsEndpoint) {
            final JmsEndpoint endpoint = (JmsEndpoint) entity;
            final Entity connection = entityCrud.find(new EntityHeader(endpoint.getConnectionGoid(), EntityType.JMS_CONNECTION, null, null));
            if (connection == null)
                throw new FindException("Cannot find associated jms connection for jms endpoint: " + endpoint.getName());
            entityContainers.add(new JmsContainer(endpoint, (JmsConnection) connection));
        } else if (entity instanceof SsgKeyEntry) {
            // not include private key entity info in bundle
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
     */
    private void addMapping(@NotNull final Properties bundleExportProperties, @NotNull final ArrayList<EntityMappingInstructions> mappings, @NotNull final DependentEntity dependentObject, @NotNull final Entity entity) {
        if (entity instanceof HasFolder) {
            // include parent folder mapping if not already in mapping.
            final Folder parentFolder = ((HasFolder) entity).getFolder();
            if(parentFolder != null) {
                final EntityMappingInstructions folderMapping = new EntityMappingInstructions(
                        EntityHeaderUtils.fromEntity(parentFolder),
                        null,
                        EntityMappingInstructions.MappingAction.NewOrExisting,
                        true,
                        false);
                if( !mappings.contains(folderMapping) ) {
                    mappings.add(folderMapping);
                }
            }
        }

        final EntityMappingInstructions mapping;
        if (entity instanceof SsgKeyEntry ||
                entity instanceof IdentityProviderConfig ||
                entity instanceof Identity ||
                entity instanceof SecurePassword ||
                (entity instanceof Folder && ((Folder)entity).getGoid().equals(Folder.ROOT_FOLDER_ID))) {
            // Make these entities map only. Set fail on new true and Mapping action NewOrExisting
            mapping = new EntityMappingInstructions(
                    dependentObject.getEntityHeader(),
                    null,
                    EntityMappingInstructions.MappingAction.NewOrExisting,
                    true,
                    false);
        } else if (entity instanceof Role && !((Role)entity).isUserCreated()){
            if(((Role)entity).getEntityGoid() != null && ((Role)entity).getEntityType() != null) {
                // These are auto generated roles for managing specific entities. Map them with the MAP_BY_ROLE_ENTITY option, and set fail on new to true
                mapping = new EntityMappingInstructions(
                        dependentObject.getEntityHeader(),
                        new EntityMappingInstructions.TargetMapping(EntityMappingInstructions.TargetMapping.Type.MAP_BY_ROLE_ENTITY),
                        EntityMappingInstructions.MappingAction.valueOf(bundleExportProperties.getProperty(DefaultMappingActionOption, DefaultMappingAction.toString())),
                        true,
                        false);
            } else {
                // These are other built in roles. They should fail on new.
                mapping = new EntityMappingInstructions(
                        dependentObject.getEntityHeader(),
                        null,
                        EntityMappingInstructions.MappingAction.valueOf(bundleExportProperties.getProperty(DefaultMappingActionOption, DefaultMappingAction.toString())),
                        true,
                        false);
            }
        } else {
            //create the default mapping instructions
            mapping = mappingInstructionsBuilder.createDefaultMapping(dependentObject.getEntityHeader(),
                    EntityMappingInstructions.MappingAction.valueOf(bundleExportProperties.getProperty(DefaultMappingActionOption, DefaultMappingAction.toString())),
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
}
