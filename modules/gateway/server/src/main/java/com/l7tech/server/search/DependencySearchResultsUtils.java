package com.l7tech.server.search;

import com.l7tech.server.search.objects.*;
import com.l7tech.util.Functions;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * This is a utility class that works on DependencySearchResults
 *
 * @author Victor Kazakov
 */
public class DependencySearchResultsUtils {

    /**
     * This will flatten the {@link com.l7tech.server.search.objects.DependencySearchResults} tree (it can actually be
     * a
     * graph and contain cycles) into a list of {@link com.l7tech.server.search.objects.Dependency}. The List will be
     * ordered with leaves first then their parents and so on. This way if the objects are created in that order you
     * will always be creating the dependencies before creating the dependent objects (except when there is a cycle).
     * The returned list of Dependency will only have its immediate dependencies set. This means that for any
     * Dependency
     * in the returned list calling {@link com.l7tech.server.search.objects.Dependency#getDependencies()} on any of its
     * dependencies will return null.
     *
     * @param dependencySearchResult This is the {@link com.l7tech.server.search.objects.DependencySearchResults} to
     *                               flatten.
     * @param includeRootNode        If true the {@link com.l7tech.server.search.objects.DependencySearchResults#getDependent()}
     *                               will be added to the list of dependent objects. Otherwise it won't be.
     * @return The flattened list of {@link com.l7tech.server.search.objects.Dependency}
     */
    @NotNull
    public static List<Dependency> flattenDependencySearchResults(@NotNull final DependencySearchResults dependencySearchResult, final boolean includeRootNode) {
        return flattenDependencySearchResults(Arrays.asList(dependencySearchResult), includeRootNode);
    }

    /**
     * This will flatten the list of {@link com.l7tech.server.search.objects.DependencySearchResults} trees (it can
     * actually be a graph and contain cycles) into a list of {@link com.l7tech.server.search.objects.Dependency}. The
     * List will be ordered with leaves first then their parents and so on. This way if the objects are created in that
     * order you will always be creating the dependencies before creating the dependent objects (except when there is a
     * cycle). The returned list of Dependency will only have its immediate dependencies set. This means that for any
     * Dependency in the returned list calling {@link com.l7tech.server.search.objects.Dependency#getDependencies()} on
     * any of its dependencies will return null.
     *
     * @param dependencySearchResults This is the List of {@link com.l7tech.server.search.objects.DependencySearchResults}
     *                                to flatten.
     * @param includeRootNode         If true the {@link com.l7tech.server.search.objects.DependencySearchResults#getDependent()}
     *                                will be added to the list of dependent objects. Otherwise it won't be.
     * @return The flattened list of {@link com.l7tech.server.search.objects.Dependency}
     */
    @NotNull
    public static List<Dependency> flattenDependencySearchResults(@NotNull final List<DependencySearchResults> dependencySearchResults, final boolean includeRootNode) {
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
    private static void getFolderDependencies(@NotNull final List<Dependency> dependencyObjects, @NotNull final DependentObject dependent, @Nullable final List<Dependency> dependencies, @NotNull final List<DependentObject> processed) {
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
    private static void getSecurityZoneDependencies(@NotNull final List<Dependency> dependencyObjects, @NotNull final DependentObject dependent, @Nullable final List<Dependency> dependencies, @NotNull final List<DependentObject> processed) {
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
    private static void buildDependentObjectsList(@NotNull final List<Dependency> dependencyObjects, @NotNull final DependentObject dependent, @Nullable final List<Dependency> dependencies, @NotNull final List<DependentObject> processed) {
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
    private static void addDependentToDependencyList(@NotNull final List<Dependency> dependencyObjects, @NotNull final DependentObject dependent, @Nullable final List<Dependency> dependencies) {
        //Find if the dependent object has been added to the dependencyObjects list.
        Dependency dependency = Functions.grepFirst(dependencyObjects, new Functions.Unary<Boolean, Dependency>() {
            @Override
            public Boolean call(Dependency dependency) {
                return dependency.getDependent().equals(dependent);
            }
        });
        //if it has not been added then add it.
        if (dependency == null) {
            if (dependent instanceof BrokenDependentEntity) {
                dependency = new BrokenDependency((BrokenDependentEntity) dependent);
            } else {
                dependency = new Dependency(dependent);
            }
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
