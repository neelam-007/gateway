package com.l7tech.external.assertions.gatewaymanagement.server.rest;

import com.l7tech.external.assertions.gatewaymanagement.server.ResourceFactory;
import com.l7tech.external.assertions.gatewaymanagement.server.rest.factories.APIResourceFactory;
import com.l7tech.external.assertions.gatewaymanagement.server.rest.resource.URLAccessible;
import com.l7tech.external.assertions.gatewaymanagement.server.rest.transformers.APITransformer;
import com.l7tech.gateway.api.*;
import com.l7tech.objectmodel.EntityHeader;
import com.l7tech.objectmodel.EntityType;
import com.l7tech.objectmodel.FindException;
import com.l7tech.objectmodel.Goid;
import com.l7tech.server.search.DependencyAnalyzer;
import com.l7tech.server.search.objects.Dependency;
import com.l7tech.server.search.objects.DependencySearchResults;
import com.l7tech.server.search.objects.DependentEntity;
import com.l7tech.server.search.objects.DependentObject;
import com.l7tech.util.CollectionUtils;
import com.l7tech.util.Functions;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.inject.Inject;
import java.io.IOException;
import java.util.*;
import java.util.logging.Logger;

/**
 * This is used to create a bundle export.
 */
public class BundleExporter {
    private static final Logger logger = Logger.getLogger(BundleExporter.class.getName());

    @Inject
    private URLAccessibleLocator URLAccessibleLocator;
    @Inject
    private APIUtilityLocator apiUtilityLocator;
    @Inject
    private DependencyAnalyzer dependencyAnalyzer;

    //DEFAULTS and options
    public static final String IncludeRequestFolderOption = "IncludeRequestFolder";
    private static final boolean IncludeRequestFolder = false;
    public static final String DefaultMappingActionOption = "DefaultMappingAction";
    private static final Mapping.Action DefaultMappingAction = Mapping.Action.NewOrExisting;
    public static final String DefaultMapByOption = "DefaultMapBy";
    private static final String DefaultMapBy = "id";
    public static final String IgnoredEntityIdsOption = "IgnoredEntityIds";
    private static final List<String> IgnoredEntityIds = Collections.emptyList();

    /**
     * Creates a bundle export given the export options
     *
     * @param bundleExportOptions A map of export options. Can be null to use the defaults
     * @param headers             The list of headers to create the export from.
     * @return The bundle generated from the headers given
     * @throws ResourceFactory.ResourceNotFoundException
     * @throws IOException
     * @throws FindException
     */
    public Bundle exportBundle(@Nullable Map<String, Object> bundleExportOptions, EntityHeader... headers) throws ResourceFactory.ResourceNotFoundException, IOException, FindException {
        //find the dependencies for the headers
        List<DependencySearchResults> dependencySearchResults = dependencyAnalyzer.getDependencies(Arrays.asList(headers), buildDependencyAnalyzerOptions(bundleExportOptions));
        //create a flat dependency list
        List<DependentObject> dependentObjects = dependencyAnalyzer.buildFlatDependencyList(dependencySearchResults);

        ArrayList<Item> items = new ArrayList<>();
        ArrayList<Mapping> mappings = new ArrayList<>();
        for (final DependentObject dependentObject : dependentObjects) {
            //for each dependent object add a reference and mapping entry to the bundle.
            if (dependentObject instanceof DependentEntity) {
                if (!getOption(IncludeRequestFolderOption, bundleExportOptions, Boolean.class, IncludeRequestFolder) && EntityType.FOLDER.equals(((DependentEntity) dependentObject).getEntityHeader().getType()) && Functions.exists(Arrays.asList(headers), new Functions.Unary<Boolean, EntityHeader>() {
                    @Override
                    public Boolean call(EntityHeader entityHeader) {
                        return Goid.equals(((DependentEntity) dependentObject).getEntityHeader().getGoid(), entityHeader.getGoid());
                    }
                })) {
                    continue;
                }
                APIResourceFactory apiResourceFactory = apiUtilityLocator.findFactoryByResourceType(dependentObject.getDependencyType().getEntityType().toString());
                APITransformer transformer = apiUtilityLocator.findTransformerByResourceType(dependentObject.getDependencyType().getEntityType().toString());
                URLAccessible urlAccessible = URLAccessibleLocator.findByEntityType(dependentObject.getDependencyType().getEntityType().toString());
                if (apiResourceFactory == null || transformer == null || urlAccessible == null) {
                    throw new FindException("Cannot find resource worker service for " + dependentObject.getDependencyType().getEntityType());
                }
                Object resource = apiResourceFactory.getResource(((DependentEntity) dependentObject).getEntityHeader().getStrId());
                Item<?> item = transformer.convertToItem(resource);
                item = new ItemBuilder<>(item).addLink(urlAccessible.getLink(resource)).build();
                items.add(item);
                //noinspection unchecked
                Mapping mapping = apiResourceFactory.buildMapping(
                        resource,
                        getOption(DefaultMappingActionOption, bundleExportOptions, Mapping.Action.class, DefaultMappingAction),
                        getOption(DefaultMapByOption, bundleExportOptions, String.class, DefaultMapBy));
                mapping.setSrcUri(urlAccessible.getUrl(resource));
                //TODO: this may not be needed?
                mapping.setDependencies(findDependencies(dependentObject, dependencySearchResults));
                mappings.add(mapping);
            }
        }

        Bundle bundle = ManagedObjectFactory.createBundle();
        bundle.setReferences(items);
        bundle.setMappings(mappings);
        return bundle;
    }

    private Map<String, Object> buildDependencyAnalyzerOptions(Map<String, Object> bundleExportOptions) {
        return CollectionUtils.MapBuilder.<String, Object>builder()
                .put(DependencyAnalyzer.IgnoreSearchOptionKey, getOption(IgnoredEntityIdsOption, bundleExportOptions, List.class, (List) IgnoredEntityIds))
                .map();
    }

    private List<String> findDependencies(DependentObject dependentObject, List<DependencySearchResults> dependencySearchResults) {
        final List<String> dependentIds = new ArrayList<>();
        for (DependencySearchResults dependencySearchResult : dependencySearchResults) {
            List<String> dependencies = findDependencies(dependentObject, dependencySearchResult.getDependent(), dependencySearchResult.getDependencies());
            for (String dependency : dependencies) {
                if (!dependentIds.contains(dependency)) {
                    dependentIds.add(dependency);
                }
            }
        }
        return dependentIds;
    }

    private List<String> findDependencies(DependentObject dependentObject, DependentObject current, List<Dependency> dependencies) {
        if (dependentObject.equals(current)) {
            List<String> dependencyIds = new ArrayList<>();
            for (Dependency dependency : dependencies) {
                if (dependency.getDependent() instanceof DependentEntity) {
                    dependencyIds.add(((DependentEntity) dependency.getDependent()).getEntityHeader().getStrId());
                } else {
                    dependencyIds.addAll(findDependencies(dependency.getDependent(), dependency.getDependent(), dependency.getDependencies()));
                }
            }
            return dependencyIds;
        } else {
            for (Dependency dependency : dependencies) {
                List<String> dependencyIds = findDependencies(dependentObject, dependency.getDependent(), dependency.getDependencies());
                if (dependencyIds != null) {
                    return dependencyIds;
                }
            }
        }
        return null;
    }

    /**
     * Retrieve an option from the search options, verifying it is the correct type and casting to it.
     *
     * @param optionKey The option to retrieve
     * @param type      The type of the option
     * @param <C>       This is the Type of the value that will be returned
     * @param <T>       This is the class type of the vlaue
     * @return The option value cast to the correct type. This will be the default value if no such option is set.
     * @throws IllegalArgumentException This is thrown if the option value is the wrong type.
     */
    @NotNull
    protected <C, T extends Class<C>> C getOption(@NotNull final String optionKey, @Nullable Map<String, Object> options, @NotNull final T type, @NotNull C defaultValue) {
        final Object value = options != null ? options.get(optionKey) : null;
        if (value != null && type.isAssignableFrom(value.getClass())) {
            return type.cast(value);
        } else if (value == null) {
            return defaultValue;
        }
        throw new IllegalArgumentException("Search option value for option '" + optionKey + "' was not a valid type. Expected: " + type + " Given: " + value.getClass());
    }
}
