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
     * @throws ModuleLoadingException
     */
    void loadModule(@NotNull File stagedFile, @NotNull ServerModuleFile moduleEntity) throws ModuleLoadingException;

    /**
     * Unloads the specified {@link ServerModuleFile} content.
     *
     * @throws ModuleLoadingException
     */
    void unloadModule(@NotNull File stagedFile, @NotNull ServerModuleFile moduleEntity) throws ModuleLoadingException;
}
