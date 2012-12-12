package com.l7tech.server.message;

import org.jetbrains.annotations.NotNull;

/**
 * Interface implemented by a PolicyEnforcementContext that shadows variables from a parent context, and that can
 * be configured to pass through access to certain variables.
 */
public interface ShadowsParentVariables {
    /**
     * Expose the specified parent context variable directly via the child context's getVariable() methods.
     * <p/>
     * After this, reading this variable will access the value in the parent context, and writing the variable
     * will modify the value in the parent context.
     *
     * @param variableName  name of variable to access in parent context instead of the child context seeing its own independent version of the variable.
     * @param prefixed  if true, any access to a variable starting with variableName will be passed up to the parent context.
     */
    void putParentVariable(@NotNull String variableName, boolean prefixed);
}
