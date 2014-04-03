package com.l7tech.server.stepdebug;

import com.l7tech.objectmodel.Goid;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.util.Option;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;

/**
 * The policy step debugger manager. Manages {@link DebugContext} for services with debugging enabled.
 */
public interface DebugManager {

    /**
     *  Creates a {@link DebugContext} for the given policy GOID.
     *
     * @param policyGoid the policy GOID
     * @return the debug context
     */
    @NotNull
    DebugContext createDebugContext(@NotNull Goid policyGoid);

    /**
     * Starts debugging for the given task ID. After debugging is started, it waits for
     * a request message to be sent to the service.
     *
     * @param taskId the task ID
     * @return The (optional) instance, which is "none" if successful. Otherwise, contains the error message.
     */
    @NotNull
    Option<String> startDebug(@NotNull String taskId);

    /**
     * Stops debugging for the given task ID.
     *
     * @param taskId the task ID
     */
    void stopDebug(@NotNull String taskId);

    /**
     * Steps-over debugging for the given task ID.
     *
     * @param taskId the task ID
     * @param assertionNumber the assertion number to break at next
     */
    void stepOver(@NotNull String taskId, @NotNull Collection<Integer> assertionNumber);

    /**
     * Steps-into debugging for the given task ID.
     *
     * @param taskId the task ID
     */
    void stepInto(@NotNull String taskId);

    /**
     * Steps-out debugging for the given task ID.
     *
     * @param taskId the task ID
     * @param assertionNumber the assertion number to break at next
     */
    void stepOut(@NotNull String taskId, @NotNull Collection<Integer> assertionNumber);

    /**
     * Resumes debugging for the given task ID.
     *
     * @param taskId the task ID
     */
    void resume(@NotNull String taskId);

    /**
     * Terminates debugging for the given task ID.
     *
     * @param taskId the task ID
     */
    void terminateDebug(@NotNull String taskId);

    /**
     * Toggles breakpoint for the given task ID specified by the given assertion number.
     *
     * @param taskId the task ID
     * @param assertionNumber the assertion number
     */
    void toggleBreakpoint(@NotNull String taskId, @NotNull Collection<Integer> assertionNumber);

    /**
     * Removes all breakpoints for the given task ID.
     *
     * @param taskId the task ID
     */
    void removeAllBreakpoints(@NotNull String taskId);

    /**
     * Adds a context variable to debug for the given task ID. If the context variable name already exist, then it
     * is ignored.
     *
     * @param taskId the task Id
     * @param name the context variable name
     */
    void addUserContextVariable(@NotNull String taskId, @NotNull String name);

    /**
     * Removes a context variable to debug for the given task ID. If the context variable name does not exist, then it
     * is ignored.
     *
     * @param taskId the task Id
     * @param name the context variable name
     */
    void removeUserContextVariable(@NotNull String taskId, @NotNull String name);

    /**
     * Returns the {@link DebugContext} for the given task ID.
     *
     * @param taskId the task ID
     * @return the debug context if it exists, null otherwise
     */
    @Nullable
    DebugContext getDebugContext(@NotNull String taskId);

    /**
     * This method must be called when a message arrives in the {@link com.l7tech.server.MessageProcessor} so that
     * debugging can start if debugging is enabled.
     *
     * @param pec the PEC
     * @param entityGoid the service or policy GOID
     */
    void onMessageArrived(@NotNull PolicyEnforcementContext pec, @NotNull Goid entityGoid);

    /**
     * This method must be called when a message finishes processing in the {@link com.l7tech.server.MessageProcessor} so that
     * debugging can finish if debugging is enabled.
     *
     * @param pec the PEC
     */
    void onMessageFinished(@NotNull PolicyEnforcementContext pec);
}