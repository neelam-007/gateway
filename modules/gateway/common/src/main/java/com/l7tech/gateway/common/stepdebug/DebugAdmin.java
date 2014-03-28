package com.l7tech.gateway.common.stepdebug;

import com.l7tech.gateway.common.admin.Administrative;
import com.l7tech.gateway.common.security.rbac.Secured;
import com.l7tech.gateway.common.service.PublishedService;
import com.l7tech.policy.Policy;
import com.l7tech.util.Option;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;

import static com.l7tech.gateway.common.security.rbac.MethodStereotype.*;
import static com.l7tech.objectmodel.EntityType.*;

/**
 * Remote admin interface for managing policy step debugger.
 */
@Secured
@Administrative
public interface DebugAdmin {

    /**
     * Initializes policy step debugging for the given service.
     *
     * @param service the service
     * @return the debug result
     */
    @Secured(types=SERVICE, stereotype=UPDATE)
    @NotNull
    DebugResult initializeDebugService(@NotNull PublishedService service);

    /**
     * Initializes policy step debugging for the given policy.
     *
     * @param policy the policy
     * @return the debug result
     */
    @Secured(types=POLICY, stereotype=UPDATE)
    @NotNull
    DebugResult initializeDebugPolicy(@NotNull Policy policy);

    /**
     * Starts debugging for the given task ID. After debugging is started, it waits for
     * a request message to be sent to the service.
     *
     * @param taskId the task ID
     * @return The (optional) instance, which is "none" if successful. Otherwise, contains the error message.
     */
    @Secured(stereotype=UNCHECKED_WIDE_OPEN)
    @NotNull
    Option<String> startDebug(@NotNull String taskId);

    /**
     * Stops debugging for the given task ID.
     *
     * @param taskId the task ID
     */
    @Secured(stereotype=UNCHECKED_WIDE_OPEN)
    void stopDebug(@NotNull String taskId);

    /**
     * Steps-over debugging for the given task ID.
     *
     * @param taskId the task ID
     * @param assertionNumber the assertion number to break at next
     */
    @Secured(stereotype=UNCHECKED_WIDE_OPEN)
    void stepOver(@NotNull String taskId, @NotNull Collection<Integer> assertionNumber);

    /**
     * Steps-into debugging for the given task ID.
     *
     * @param taskId the task ID
     */
    @Secured(stereotype=UNCHECKED_WIDE_OPEN)
    void stepInto(@NotNull String taskId);

    /**
     * Steps-out debugging for the given task ID.
     *
     * @param taskId the task ID
     * @param assertionNumber the assertion number to break at next
     */
    @Secured(stereotype=UNCHECKED_WIDE_OPEN)
    void stepOut(@NotNull String taskId, @NotNull Collection<Integer> assertionNumber);

    /**
     * Resumes debugging for the given task ID.
     *
     * @param taskId the task ID
     */
    @Secured(stereotype=UNCHECKED_WIDE_OPEN)
    void resume(@NotNull String taskId);

    /**
     * Terminates debugging for the given task ID.
     *
     * @param taskId the task ID
     */
    @Secured(stereotype=UNCHECKED_WIDE_OPEN)
    void terminateDebug(@NotNull String taskId);

    /**
     * Toggles breakpoint for the given task ID specified by the given assertion number.
     *
     * @param taskId the task ID
     * @param assertionNumber the assertion number
     */
    @Secured(stereotype=UNCHECKED_WIDE_OPEN)
    void toggleBreakpoint(@NotNull String taskId, @NotNull Collection<Integer> assertionNumber);

    /**
     * Removes all breakpoints for the given task ID.
     *
     * @param taskId the task ID
     */
    @Secured(stereotype=UNCHECKED_WIDE_OPEN)
    void removeAllBreakpoints(@NotNull String taskId);

    /**
     * Waits for changes to occur in the debug context up to maxMillisToWait. Returns immediately if changes has occurred
     * since last time this method called for the given task ID. Returns non-null DebugResult if changes occurred, null otherwise.
     *
     * @param taskId the task Id
     * @param maxMillisToWait max time to wait in millis
     * @return non-null DebugResult if changes occurred. Null otherwise
     */
    @Secured(stereotype=UNCHECKED_WIDE_OPEN)
    @Transactional(readOnly=true)
    @Nullable
    DebugResult waitForUpdates(@NotNull String taskId, long maxMillisToWait);

    /**
     * Adds a context variable to debug for the given task ID. If the context variable name already exist, then it
     * is ignored.
     *
     * @param taskId the task Id
     * @param name the context variable name
     */
    @Secured(stereotype=UNCHECKED_WIDE_OPEN)
    void addUserContextVariable(@NotNull String taskId, @NotNull String name);

    /**
     * Removes a context variable to debug for the given task ID. If the context variable name does not exist, then it
     * is ignored.
     *
     * @param taskId the task Id
     * @param name the context variable name
     */
    @Secured(stereotype=UNCHECKED_WIDE_OPEN)
    void removeUserContextVariable(@NotNull String taskId, @NotNull String name);
}