package com.l7tech.gateway.common.stepdebug;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.Serializable;
import java.util.Collection;
import java.util.Collections;
import java.util.Set;

/**
 * Holds policy step debugging information that is sent via the Admin interface from the SSG to the SSM.
 */
public class DebugResult implements Serializable {
    public static final String SUCCESSFUL_POLICY_RESULT_MESSAGE = "Policy completed successfully";
    public static final String ERROR_POLICY_RESULT_MESSAGE = "Policy completed with error";

    private final String taskId;
    private DebugState debugState = DebugState.STOPPED;
    private Set<Collection<Integer>> breakpoints = Collections.emptySet(); // Set containing assertion numbers that have breakpoints enabled.
    private Collection<Integer> currentLine = null; // The assertion number of current line. Default to null.
    private Set<DebugContextVariableData> contextVariables = Collections.emptySet(); // Context variables.
    private String policyResult = null;
    private boolean isTerminated = false;

    /**
     * Creates <code>DebugResult</code>.
     *
     * @param taskId the task ID
     */
    public DebugResult(@NotNull String taskId) {
        this.taskId = taskId;
    }

    /**
     * Returns the task ID.
     *
     * @return the task ID
     */
    @NotNull
    public String getTaskId() {
        return taskId;
    }

    /**
     * Sets the debug state.
     *
     * @param debugState the debug state
     */
    public void setDebugState(@NotNull DebugState debugState) {
        this.debugState = debugState;
    }

    /**
     * Returns the debug state.
     *
     * @return the debug state
     */
    @NotNull
    public DebugState getDebugState() {
        return debugState;
    }

    /**
     * Sets the breakpoints.
     *
     * @param breakpoints the breakpoints
     */
    public void setBreakpoints(@NotNull Set<Collection<Integer>> breakpoints) {
        this.breakpoints = breakpoints;
    }

    /**
     * Returns the breakpoints.
     *
     * @return the breakpoints
     */
    @NotNull
    public Set<Collection<Integer>> getBreakpoints() {
        return breakpoints;
    }

    /**
     * Sets the current line in execution.
     *
     * @param currentLine the current line
     */
    public void setCurrentLine(@Nullable Collection<Integer> currentLine) {
        this.currentLine = currentLine;
    }

    /**
     * Returns the current line in execution.
     *
     * @return the current line in execution. Null if current line is not set.
     */
    @Nullable
    public Collection<Integer> getCurrentLine() {
        return currentLine;
    }

    /**
     * Sets context variables.
     *
     * @param contextVariables the context variables
     */
    public void setContextVariables(@NotNull Set<DebugContextVariableData> contextVariables) {
        this.contextVariables = contextVariables;
    }

    /**
     * Returns the context variables.
     *
     * @return the context variables
     */
    @NotNull
    public Set<DebugContextVariableData> getContextVariables() {
        return contextVariables;
    }

    /**
     * Sets the policy evaluation result.
     *
     * @param policyResult the policy evaluation result
     */
    public void setPolicyResult(@Nullable String policyResult) {
        this.policyResult = policyResult;
    }

    /**
     * Returns the policy evaluation result.
     *
     * @return the policy evaluation result
     */
    @Nullable
    public String getPolicyResult() {
        return policyResult;
    }

    /**
     * Sets whether or not the debugger is terminated.
     *
     * @param isTerminated true if terminated. false otherwise.
     */
    public void setIsTerminated(boolean isTerminated) {
        this.isTerminated = isTerminated;
    }

    /**
     * Returns whether or not the debugger is terminated.
     *
     * @return true if terminated. false otherwise.
     */
    public boolean isTerminated() {
        return isTerminated;
    }
}