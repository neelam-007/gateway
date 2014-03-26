package com.l7tech.server.bundling;

import com.l7tech.gateway.common.security.keystore.SsgKeyEntry;
import com.l7tech.gateway.common.transport.jms.JmsConnection;
import com.l7tech.gateway.common.transport.jms.JmsEndpoint;
import com.l7tech.objectmodel.*;
import com.l7tech.objectmodel.folder.FolderHeader;
import com.l7tech.objectmodel.folder.HasFolder;
import com.l7tech.server.EntityCrud;
import com.l7tech.server.EntityHeaderUtils;
import com.l7tech.server.search.DependencyAnalyzer;
import com.l7tech.server.search.objects.DependencySearchResults;
import com.l7tech.server.search.objects.DependentEntity;
import com.l7tech.server.search.objects.DependentObject;
import com.l7tech.util.CollectionUtils;
import com.l7tech.util.Functions;
import org.jetbrains.annotations.NotNull;

import javax.inject.Inject;
import java.util.*;

/**
 * The bundle exporter will export an entity bundle from this gateway.
 */
public class EntityBundleExporterImpl implements EntityBundleExporter {
    @Inject
    private DependencyAnalyzer dependencyAnalyzer;
    @Inject
    private EntityCrud entityCrud;
    @Inject
    private MappingInstructionsBuilder mappingInstructionsBuilder;

    //DEFAULTS and options
    public static final String IncludeRequestFolderOption = "IncludeRequestFolder";
    private static final String IncludeRequestFolder = "false";
    public static final String DefaultMappingActionOption = "DefaultMappingAction";
    private static final EntityMappingInstructions.MappingAction DefaultMappingAction = EntityMappingInstructions.MappingAction.NewOrExisting;
    public static final String DefaultMapByOption = "DefaultMapBy";
    private static final String DefaultMapBy = "ID";
    public static final String IgnoredEntityIdsOption = "IgnoredEntityIds";

    /**
     * Exports a bundle given a list of entity headers to export. This will find all the dependencies needed to import
     * the bundle.
     *
     * @param bundleExportProperties Properties to export the bundle with.
     * @param headers                The entity headers to export
     * @return The entity bundle containing all entities in the bundle and the mappings required to import them.
     * @throws FindException
     */
    @NotNull
    public EntityBundle exportBundle(@NotNull final Properties bundleExportProperties, @NotNull final EntityHeader... headers) throws FindException {
        //find the dependencies for the headers
        final List<DependencySearchResults> dependencySearchResults = dependencyAnalyzer.getDependencies(Arrays.asList(headers), buildDependencyAnalyzerOptions(bundleExportProperties));
        //create a flat dependency list
        final List<DependentObject> dependentObjects = dependencyAnalyzer.buildFlatDependencyList(dependencySearchResults);

        final ArrayList<Entity> entities = new ArrayList<>();
        final ArrayList<EntityContainer> entityContainers = new ArrayList<>();
        final ArrayList<EntityMappingInstructions> mappings = new ArrayList<>();

        for (final DependentObject dependentObject : dependentObjects) {
            //for each dependent object add a reference and mapping entry to the bundle.
            if (dependentObject instanceof DependentEntity) {
                if (!Boolean.parseBoolean(bundleExportProperties.getProperty(IncludeRequestFolderOption, IncludeRequestFolder))
                        && EntityType.FOLDER.equals(((DependentEntity) dependentObject).getEntityHeader().getType())
                        && Functions.exists(Arrays.asList(headers), new Functions.Unary<Boolean, EntityHeader>() {
                    @Override
                    public Boolean call(EntityHeader entityHeader) {
                        return Goid.equals(((DependentEntity) dependentObject).getEntityHeader().getGoid(), entityHeader.getGoid());
                    }
                })) {
                    //remove any request folders from the returned export.
                    //TODO: does a mapping still need to be added for the folder?
                    continue;
                }

                //find the entity
                final Entity entity = entityCrud.find(((DependentEntity) dependentObject).getEntityHeader());
                entities.add(entity);

                addMapping(bundleExportProperties, mappings, (DependentEntity) dependentObject, entity);
                addEntities(entity, entityContainers);
            }
        }

        return new EntityBundle(entityContainers, mappings);
    }

    private void addEntities(Entity entity, List<EntityContainer> entityContainers) throws FindException {
        if(entity instanceof JmsEndpoint){
            final JmsEndpoint endpoint = (JmsEndpoint)entity;
            Entity connection = entityCrud.find(new EntityHeader(endpoint.getConnectionGoid(),EntityType.JMS_CONNECTION, null, null));
            if(connection == null)
                throw new FindException("Cannot find associated jms connection for jms endpoint: "+ endpoint.getName());
            entityContainers.add(new JmsContainer(endpoint,(JmsConnection)connection));
        }else if(entity instanceof PersistentEntity){
            entityContainers.add( new PersistentEntityContainer((PersistentEntity)entity));
        }else if(entity instanceof SsgKeyEntry){
            // not include private key entity info in bundle
            return;
        }else{
            entityContainers.add( new EntityContainer(entity));
        }
    }

    private void addMapping(Properties bundleExportProperties, ArrayList<EntityMappingInstructions> mappings, DependentEntity dependentObject, Entity entity) {
        if(entity instanceof HasFolder){
            // include parent folder mapping if not already in mapping.
            final Entity parentFolder = ((HasFolder)entity).getFolder();
            EntityMappingInstructions folderMapping = new EntityMappingInstructions(
                    EntityHeaderUtils.fromEntity(parentFolder),
                    null,
                    EntityMappingInstructions.MappingAction.NewOrExisting,
                    true,
                    false);
            if(!mappings.contains(folderMapping)) mappings.add(folderMapping);
        }

        final EntityMappingInstructions mapping;
        if(entity instanceof SsgKeyEntry){
            // map only for private keys
            mapping = new EntityMappingInstructions(
                    ((DependentEntity) dependentObject).getEntityHeader(),
                    null,
                    EntityMappingInstructions.MappingAction.NewOrExisting,
                    true,
                    false);
        }else{
            //create the default mapping instructions
            mapping = mappingInstructionsBuilder.createDefaultMapping(((DependentEntity) dependentObject).getEntityHeader(),
                EntityMappingInstructions.MappingAction.valueOf(bundleExportProperties.getProperty(DefaultMappingActionOption, DefaultMappingAction.toString())),
                EntityMappingInstructions.TargetMapping.Type.valueOf(bundleExportProperties.getProperty(DefaultMapByOption, DefaultMapBy).toUpperCase()));
        }

        //add the mapping

        mappings.add(mapping);
    }

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
        return optionBuilder.map();
    }
}
