package com.l7tech.server.message;

import org.jetbrains.annotations.NotNull;

import java.util.Set;

/**
 * Interface implemented by a PolicyEnforcementContext whose creator might use one or more context variables
 * from the context after policy evaluation is complete.  The variables might not be used by the policy itself
 * but should still be visible as used in case assertions will make decisions dynamically about whether to
 * set the variables.
 */
public interface HasOutputVariables {
    /**
     * Get a list of all context variables that will be used from this context after policy evaluation has completed.
     *
     * @return a possibly-empty case-insensitive read-only Set of variable names from this context that will be used after policy evaluation has finished.
     *         these variable names are not boxed in ${name} pattern (must return variable names only, must not return null)
     */
    @NotNull
    Set<String> getOutputVariableNames();

    /**
     * Declare a context variable that will be used from this context after policy evaluation has completed.
     *
     * @param variableName name of a variable that will be used later.  Required.
     */
    void addOutputVariableName(@NotNull String variableName);
}
