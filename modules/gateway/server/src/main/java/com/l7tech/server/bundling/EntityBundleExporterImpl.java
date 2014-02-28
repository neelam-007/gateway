package com.l7tech.server.bundling;

import com.l7tech.objectmodel.*;
import com.l7tech.server.EntityCrud;
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

                //create the default mapping instructions
                EntityMappingInstructions mapping = mappingInstructionsBuilder.createDefaultMapping(((DependentEntity) dependentObject).getEntityHeader(),
                        EntityMappingInstructions.MappingAction.valueOf(bundleExportProperties.getProperty(DefaultMappingActionOption, DefaultMappingAction.toString())),
                        EntityMappingInstructions.TargetMapping.Type.valueOf(bundleExportProperties.getProperty(DefaultMapByOption, DefaultMapBy).toUpperCase()));

                //add the mapping
                mappings.add(mapping);
            }
        }

        return new EntityBundle(entities, mappings);
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
