package com.l7tech.server.stepdebug;

import com.l7tech.gateway.common.service.PublishedService;
import com.l7tech.gateway.common.stepdebug.DebugAdmin;
import com.l7tech.gateway.common.stepdebug.DebugResult;
import com.l7tech.policy.Policy;
import com.l7tech.util.Option;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.inject.Inject;
import java.util.Collection;

/**
 *
 */
public class DebugAdminImpl implements DebugAdmin {

    @Inject
    private DebugManager debugManager;

    public DebugAdminImpl() {
    }

    @Override
    @NotNull
    public DebugResult initializeDebugService(@NotNull PublishedService service) {
        DebugContext debugContext = debugManager.createDebugContext(service.getGoid(), true);
        return this.createDebugResult(debugContext);
    }

    @Override
    @NotNull
    public DebugResult initializeDebugPolicy(@NotNull Policy policy) {
        DebugContext debugContext = debugManager.createDebugContext(policy.getGoid(), false);
        return this.createDebugResult(debugContext);
    }

    @Override
    @NotNull
    public Option<String> startDebug(@NotNull String taskId) {
        return debugManager.startDebug(taskId);
    }

    @Override
    public void stopDebug(@NotNull String taskId) {
        debugManager.stopDebug(taskId);
    }

    @Override
    public void stepOver(@NotNull String taskId, @NotNull Collection<Integer> assertionNumber) {
        debugManager.stepOver(taskId, assertionNumber);
    }

    @Override
    public void stepInto(@NotNull String taskId) {
        debugManager.stepInto(taskId);
    }

    @Override
    public void stepOut(@NotNull String taskId, @NotNull Collection<Integer> assertionNumber) {
        debugManager.stepOut(taskId, assertionNumber);
    }

    @Override
    public void resume(@NotNull String taskId) {
        debugManager.resume(taskId);
    }

    @Override
    public void terminateDebug(@NotNull String taskId) {
        debugManager.terminateDebug(taskId);
    }

    @Override
    public void toggleBreakpoint(@NotNull String taskId, @NotNull Collection<Integer> assertionNumber) {
        debugManager.toggleBreakpoint(taskId, assertionNumber);
    }

    @Override
    public void removeAllBreakpoints(@NotNull String taskId) {
        debugManager.removeAllBreakpoints(taskId);
    }

    @Override
    @Nullable
    public DebugResult waitForUpdates(@NotNull String taskId, long maxMillisToWait) {
        DebugContext debugContext = debugManager.getDebugContext(taskId);
        if (debugContext != null && debugContext.waitForUpdates(maxMillisToWait)) {
            return this.createDebugResult(debugContext);
        }
        return null;
    }

    @Override
    public void addUserContextVariable(@NotNull String taskId, @NotNull String name) {
        debugManager.addUserContextVariable(taskId, name);
    }

    @Override
    public void removeUserContextVariable(@NotNull String taskId, @NotNull String name) {
        debugManager.removeUserContextVariable(taskId, name);
    }

    @NotNull
    private DebugResult createDebugResult(@NotNull DebugContext debugContext) {
        DebugResult debugResult = new DebugResult(debugContext.getTaskId());

        // Basic info.
        //
        debugResult.setDebugState(debugContext.getDebugState());
        debugResult.setBreakpoints(debugContext.getBreakpoints());

        // Current line, if set.
        //
        debugResult.setCurrentLine(debugContext.getCurrentLine());

        // Debug data from the PEC.
        //
        DebugPecData debugPecData = debugContext.getDebugPecData();
        debugResult.setContextVariables(debugPecData.getContextVariables());

        return debugResult;
    }
}