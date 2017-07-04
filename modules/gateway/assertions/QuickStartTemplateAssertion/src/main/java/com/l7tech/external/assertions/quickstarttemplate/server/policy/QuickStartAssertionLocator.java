package com.l7tech.external.assertions.quickstarttemplate.server.policy;

import com.l7tech.external.assertions.quickstarttemplate.server.parser.AssertionMapper;
import com.l7tech.external.assertions.quickstarttemplate.server.parser.AssertionSupport;
import com.l7tech.objectmodel.FindException;
import com.l7tech.objectmodel.Goid;
import com.l7tech.objectmodel.encass.EncapsulatedAssertionConfig;
import com.l7tech.objectmodel.folder.Folder;
import com.l7tech.policy.AssertionRegistry;
import com.l7tech.policy.Policy;
import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.assertion.EncapsulatedAssertion;
import com.l7tech.server.folder.FolderManager;
import com.l7tech.server.policy.EncapsulatedAssertionConfigManager;
import com.l7tech.util.Option;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/**
 * A class to find and return encapsulated assertions for use in Quick Start.
 */
public class QuickStartAssertionLocator {
    private final EncapsulatedAssertionConfigManager encassConfigManager;
    private final AssertionMapper assertionMapper;
    private final FolderManager folderManager;
    private final Goid quickStartProvidedAssertionFolder;
    private AssertionRegistry assertionRegistry;

    public QuickStartAssertionLocator(@NotNull final EncapsulatedAssertionConfigManager encassConfigManager,
                                      @NotNull final AssertionMapper assertionMapper,
                                      @NotNull final FolderManager folderManager,
                                      @NotNull final Goid quickStartProvidedAssertionFolder
    ) {
        this.encassConfigManager = encassConfigManager;
        this.assertionMapper = assertionMapper;
        this.folderManager = folderManager;
        this.quickStartProvidedAssertionFolder = quickStartProvidedAssertionFolder;
    }

    public void setAssertionRegistry(AssertionRegistry assertionRegistry) {
        this.assertionRegistry = assertionRegistry;
    }

    @Nullable
    public EncapsulatedAssertion findEncapsulatedAssertion(@NotNull final String name) throws FindException {
        final Folder parentFolder = getFolder(quickStartProvidedAssertionFolder);
        final EncapsulatedAssertionConfig encassConfig = encassConfigManager.findByUniqueName(name);
        if (isInFolder(parentFolder, encassConfig)) {
            return new EncapsulatedAssertion(encassConfig);
        }
        return null;
    }

    @Nullable
    public Assertion findAssertion(@NotNull final String name) {
        final Assertion assertion = assertionRegistry.findByExternalName(name);
        return assertion == null ? null : assertion.getCopy();   // don't mess with registry's copy
    }

    private static class UncheckedFindException extends RuntimeException {
        private UncheckedFindException(@NotNull final String message) {
            super(message);
        }
    }

    private <T> Set<T> unwrapFindException(@NotNull final Supplier<Set<T>> supplier) throws FindException {
        try {
            return supplier.get();
        } catch (final UncheckedFindException ex) {
            throw new FindException(ex.getMessage(), ex);
        }
    }

    @NotNull
    public Set<Assertion> findSupportedAssertions() throws FindException {
        return unwrapFindException(() ->
                assertionMapper.getSupportedAssertions().keySet().stream()
                        .map(name -> Optional.ofNullable(assertionRegistry.findByExternalName(name))
                                .map(Assertion::getCopy)
                                .orElseThrow(() -> new UncheckedFindException("Assertion with name \"" + name + "\" cannot be found!")))
                        .collect(Collectors.toSet())
        );
    }

    @NotNull
    public <T> Set<T> findSupportedAssertions(final @NotNull BiFunction<AssertionSupport, Assertion, T> mapper) throws FindException {
        return unwrapFindException(() ->
                assertionMapper.getSupportedAssertions().entrySet().stream()
                        .map(entry -> Optional.ofNullable(assertionRegistry.findByExternalName(entry.getKey()))
                                .map(Assertion::getCopy)
                                .map(ass -> mapper.apply(entry.getValue(), ass))
                                .orElseThrow(() -> new UncheckedFindException("Assertion with name \"" + entry.getKey() + "\" cannot be found!"))
                        ).collect(Collectors.toSet())
        );
    }

    @NotNull
    public Set<EncapsulatedAssertion> findEncapsulatedAssertions() throws FindException {
        final Folder parentFolder = getFolder(quickStartProvidedAssertionFolder);
        return encassConfigManager.findAll().stream()
                .filter(ec -> isInFolder(parentFolder, ec))
                .map(EncapsulatedAssertion::new)
                .collect(Collectors.toSet());
    }

    @NotNull
    private Folder getFolder(@NotNull final Goid folderId) throws FindException {
        final Folder folder = folderManager.findByPrimaryKey(folderId);
        if (folder == null) {
            // Unable to find the top level folder is a misconfiguration on our part.
            throw new IllegalStateException(String.format("Unable to find parent folder with GOID: %s", folderId));
        }
        return folder;
    }

    private static boolean isInFolder(@NotNull final Folder folder, @Nullable EncapsulatedAssertionConfig encassConfig) {
        // If anything is null along the chain (or false at the end), the user has specified an invalid encass - it
        // doesn't exist, or it isn't in the correct folder. In any case, the result is the same; return none.
        return Option.optional(encassConfig)
                .map(EncapsulatedAssertionConfig::getPolicy)
                .map(Policy::getFolder)
                .map(f -> folder.getNesting(f) >= 0)
                .orSome(false);
    }

}
