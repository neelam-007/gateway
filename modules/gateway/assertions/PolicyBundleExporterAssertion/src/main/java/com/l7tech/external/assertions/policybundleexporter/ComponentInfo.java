package com.l7tech.external.assertions.policybundleexporter;

import com.l7tech.objectmodel.folder.FolderHeader;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.Serializable;

/**
 * Contains component information.
 */
public class ComponentInfo implements Serializable {
    private final String name;
    private final String description;
    private final String version;
    private final boolean autoGenerateId;
    private final String id;
    private final FolderHeader folderHeader;

    public ComponentInfo(@NotNull String name,
                         @NotNull String description,
                         @NotNull String version,
                         boolean autoGenerateId,
                         @Nullable String id,
                         @NotNull FolderHeader folderHeader) {
        this.name = name;
        this.description = description;
        this.version = version;
        this.autoGenerateId = autoGenerateId;
        this.id = id;
        this.folderHeader = folderHeader;
    }

    @NotNull
    public String getName() {
        return name;
    }

    @NotNull
    public String getDescription() {
        return description;
    }

    @NotNull
    public String getVersion() {
        return version;
    }

    public boolean isAutoGenerateId() {
        return autoGenerateId;
    }

    @Nullable
    public String getId() {
        return id;
    }

    @NotNull
    public FolderHeader getFolderHeader() {
        return folderHeader;
    }
}