package com.l7tech.external.assertions.quickstarttemplate.server.policy;

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

import java.util.Set;
import java.util.stream.Collectors;

/**
 * A class to find and return encapsulated assertions for use in Quick Start.
 */
public class QuickStartEncapsulatedAssertionLocator {  //TODO rename to QuickStartAssertionLocator (no longer just encass)
    private final EncapsulatedAssertionConfigManager encassConfigManager;
    private final FolderManager folderManager;
    private final Goid quickStartProvidedAssertionFolder;
    private AssertionRegistry assertionRegistry;

    public QuickStartEncapsulatedAssertionLocator(@NotNull final EncapsulatedAssertionConfigManager encassConfigManager,
                                                  @NotNull final FolderManager folderManager,
                                                  @NotNull final Goid quickStartProvidedAssertionFolder) {
        this.encassConfigManager = encassConfigManager;
        this.folderManager = folderManager;
        this.quickStartProvidedAssertionFolder = quickStartProvidedAssertionFolder;
    }

    // TODO is there a better time in the assertion lifecycle to set assertion registry?
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
