package com.l7tech.objectmodel;

import com.l7tech.objectmodel.folder.Folder;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Helper class for unit tests.
 */
public final class EntityCreator {
    private EntityCreator(){}

    public static final Folder createFolderWithRandomGoid(@NotNull final String folderName, @Nullable final Folder parent) {
        final Folder folder = new Folder(folderName, parent);
        folder.setGoid(GoidUtils.randomGoid());
        return folder;
    }
}
