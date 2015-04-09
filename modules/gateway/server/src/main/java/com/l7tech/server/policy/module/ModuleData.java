package com.l7tech.server.policy.module;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;

/**
 * Contains module data needed for {@link ModulesScanner#onModuleLoad(ModuleData)} method.
 *
 * @see ModulesScanner#onModuleLoad(ModuleData)
 */
public interface ModuleData {

    /**
     * Represents the module deployed file or the module staging file, in case the module has been uploaded using the Policy manager.
     * @return {@link File} object never {@code null}.
     */
    @NotNull
    File getFile();

    /**
     * Represents the module content (actually the module file bytes) digest.  Currently SHA-256 is used.
     *
     * @return A {@code String} containing hex-dump of the module digest.
     */
    @NotNull
    String getDigest();

    /**
     * Represents the time that the module file was last modified.
     *
     * @return A {@code long} value representing the time the file was last modified.
     * @see java.io.File#lastModified()
     */
    long getLastModified();

    /**
     * In case the module has been uploaded using the Policy manager (i.e. if the module is a {@link com.l7tech.gateway.common.module.ServerModuleFile ServerModuleFile}),
     * this represents the {@code ServerModuleFile} entity name.
     *
     * @return if uploaded using the Policy manager, then {@code ServerModuleFile} entity name, otherwise {@code null}.
     */
    @Nullable
    String getName();
}
