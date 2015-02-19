package com.l7tech.gateway.common.module;

import org.jetbrains.annotations.NotNull;

/**
 * Implemented by Modular and Custom Assertion Registry to check whether specified {@link ServerModuleFile module file} is loaded or not.
 */
public interface ServerModuleFileChecker {
    /**
     * Checks whether the specified {@link ServerModuleFile module file} is loaded in the Gateway.
     *
     * @return {@code true} if the module is loaded, otherwise {@code false}.
     */
    boolean isServerModuleFileLoaded(@NotNull ServerModuleFile moduleFile);
}
