package com.l7tech.gateway.common.module;

import org.jetbrains.annotations.NotNull;

import java.io.File;

/**
 * {@link ServerModuleFile} loader interface.
 */
public interface ServerModuleFileLoader {
    /**
     * Loads the specified {@link ServerModuleFile} content.
     *
     * @throws ModuleLoadingException if an error happens while loading the module.
     */
    void loadModule(@NotNull File stagedFile, @NotNull ServerModuleFile moduleEntity) throws ModuleLoadingException;

    /**
     * Updates the specified {@link ServerModuleFile} content.
     */
    void updateModule(@NotNull File stagedFile, @NotNull ServerModuleFile moduleEntity);

    /**
     * Unloads the specified {@link ServerModuleFile} content.
     *
     * @throws ModuleLoadingException if an error happens while unloading the module.
     */
    void unloadModule(@NotNull File stagedFile, @NotNull ServerModuleFile moduleEntity) throws ModuleLoadingException;
}
