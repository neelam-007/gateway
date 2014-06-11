package com.l7tech.server.search.processors;

import com.l7tech.objectmodel.EntityHeader;
import com.l7tech.objectmodel.FindException;
import com.l7tech.policy.assertion.Assertion;
import com.l7tech.server.search.DependencyProcessorRegistry;
import com.l7tech.server.search.exceptions.CannotReplaceDependenciesException;
import com.l7tech.server.search.exceptions.CannotRetrieveDependenciesException;
import com.l7tech.server.search.objects.Dependency;
import com.l7tech.server.search.objects.DependentObject;
import org.jetbrains.annotations.NotNull;

import javax.inject.Inject;
import javax.inject.Named;
import java.util.List;
import java.util.Map;

/**
 * The assertion dependency processor finds and delegated to the appropriate assertion specific dependency processor if
 * one exists. Otherwise if uses the {@link com.l7tech.server.search.processors.DefaultAssertionDependencyProcessor}.
 * This class should not be extended, you should extend the {@link com.l7tech.server.search.processors.DefaultAssertionDependencyProcessor}
 * if you want to expand on assertion processing for a specific assertion
 *
 * @param <A> The assertion type
 */
public final class AssertionDependencyProcessor<A extends Assertion> implements DependencyProcessor<A> {

    /**
     * This is used to locate assertion specific dependency processors.
     */
    @Inject
    @Named("assertionDependencyProcessorRegistry")
    private DependencyProcessorRegistry assertionProcessorRegistry;

    private final DefaultAssertionDependencyProcessor<Assertion> defaultAssertionDependencyProcessor;

    public AssertionDependencyProcessor(@NotNull final DefaultAssertionDependencyProcessor<Assertion> defaultAssertionDependencyProcessor) {
        this.defaultAssertionDependencyProcessor = defaultAssertionDependencyProcessor;
    }

    @NotNull
    @Override
    public List<Dependency> findDependencies(@NotNull final A assertion, @NotNull final DependencyFinder finder) throws FindException, CannotRetrieveDependenciesException {
        final DependencyProcessor assertionProcessor = assertionProcessorRegistry.get(assertion.getClass().getName());
        if (assertionProcessor != null) {
            //noinspection unchecked
            return assertionProcessor.findDependencies(assertion, finder);
        } else {
            return defaultAssertionDependencyProcessor.findDependencies(assertion, finder);
        }
    }

    @NotNull
    @Override
    public List<A> find(@NotNull final Object searchValue, @NotNull final com.l7tech.search.Dependency.DependencyType dependencyType, @NotNull final com.l7tech.search.Dependency.MethodReturnType searchValueType) throws FindException {
        throw new UnsupportedOperationException("Assertions cannot be loaded as entities");
    }

    @NotNull
    @Override
    public DependentObject createDependentObject(@NotNull final A assertion) {
        final DependencyProcessor assertionProcessor = assertionProcessorRegistry.get(assertion.getClass().getName());
        if (assertionProcessor != null) {
            //noinspection unchecked
            return assertionProcessor.createDependentObject(assertion);
        } else {
            return defaultAssertionDependencyProcessor.createDependentObject(assertion);
        }
    }

    @NotNull
    @Override
    public List<DependentObject> createDependentObjects(@NotNull final Object searchValue, @NotNull final com.l7tech.search.Dependency.DependencyType dependencyType, @NotNull final com.l7tech.search.Dependency.MethodReturnType searchValueType) {
        throw new UnsupportedOperationException("AssertionDependent Objects cannot be created from a search value.");
    }

    @Override
    public void replaceDependencies(@NotNull final A assertion, @NotNull final Map<EntityHeader, EntityHeader> replacementMap, @NotNull final DependencyFinder finder, final boolean replaceAssertionsDependencies) throws CannotReplaceDependenciesException {
        if(!replaceAssertionsDependencies) return;

        final DependencyProcessor assertionProcessor = assertionProcessorRegistry.get(assertion.getClass().getName());
        if (assertionProcessor != null) {
            //noinspection unchecked
            assertionProcessor.replaceDependencies(assertion, replacementMap, finder, replaceAssertionsDependencies);
        } else {
            defaultAssertionDependencyProcessor.replaceDependencies(assertion, replacementMap, finder, replaceAssertionsDependencies);
        }
    }
}
