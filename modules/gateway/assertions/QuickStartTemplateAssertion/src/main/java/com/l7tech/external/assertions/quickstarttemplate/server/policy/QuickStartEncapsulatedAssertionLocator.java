package com.l7tech.external.assertions.quickstarttemplate.server.policy;

import com.l7tech.objectmodel.FindException;
import com.l7tech.objectmodel.Goid;
import com.l7tech.objectmodel.encass.EncapsulatedAssertionConfig;
import com.l7tech.objectmodel.folder.Folder;
import com.l7tech.policy.Policy;
import com.l7tech.policy.assertion.EncapsulatedAssertion;
import com.l7tech.server.folder.FolderManager;
import com.l7tech.server.policy.EncapsulatedAssertionConfigManager;
import com.l7tech.util.Option;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * A class to find and return encapsulated assertions for use in Quick Start.
 */
public class QuickStartEncapsulatedAssertionLocator {

    private final EncapsulatedAssertionConfigManager encassConfigManager;
    private final FolderManager folderManager;
    private final Goid quickStartProvidedAssertionFolder;

    public QuickStartEncapsulatedAssertionLocator(@NotNull final EncapsulatedAssertionConfigManager encassConfigManager,
                                                  @NotNull final FolderManager folderManager,
                                                  @NotNull final Goid quickStartProvidedAssertionFolder) {
        this.encassConfigManager = encassConfigManager;
        this.folderManager = folderManager;
        this.quickStartProvidedAssertionFolder = quickStartProvidedAssertionFolder;
    }

    @Nullable
    public EncapsulatedAssertion findEncapsulatedAssertion(@NotNull final String name) throws FindException {
        return findInSubfolder(quickStartProvidedAssertionFolder, name);
    }

    @Nullable
    private EncapsulatedAssertion findInSubfolder(@NotNull final Goid parentFolder, @NotNull final String name) throws FindException {
        final Folder topLevelFolder = folderManager.findByPrimaryKey(parentFolder);
        if (topLevelFolder == null) {
            // Unable to find the top level folder is a misconfiguration on our part.
            throw new IllegalStateException(String.format("Unable to find parent folder with GOID: %s", parentFolder));
        }
        final Option<EncapsulatedAssertionConfig> encassConfig = Option.optional(encassConfigManager.findByUniqueName(name));
        // If anything is null along the chain (or false at the end), the user has specified an invalid encass - it
        // doesn't exist, or it isn't in the correct folder. In any case, the result is the same; return none.
        final Boolean topFolderIsParent = encassConfig
                .map(EncapsulatedAssertionConfig::getPolicy)
                .map(Policy::getFolder)
                .map(folder -> topLevelFolder.getNesting(folder) >= 0)
                .orSome(false);
        if (topFolderIsParent) {
            return new EncapsulatedAssertion(encassConfig.some());
        }
        return null;
    }

}
