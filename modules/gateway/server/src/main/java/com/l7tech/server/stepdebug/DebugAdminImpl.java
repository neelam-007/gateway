package com.l7tech.server.stepdebug;

import com.l7tech.gateway.common.stepdebug.DebugAdmin;
import com.l7tech.gateway.common.stepdebug.DebugResult;
import com.l7tech.objectmodel.Goid;
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
    public DebugResult initializeDebug(@NotNull Goid policyGoid) {
        DebugContext debugContext = debugManager.createDebugContext(policyGoid);
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
        DebugResult result;

        DebugContext debugContext = debugManager.getDebugContext(taskId);
        if (debugContext != null) {
            // debug context found. It is still active.
            //
            if (debugContext.waitForUpdates(maxMillisToWait)) {
                // debug context has been is updated.
                //
                result = this.createDebugResult(debugContext);
            } else {
                // debug context was not updated.
                //
                result = null;
            }
        } else {
            // debug context not found. It has been terminated.
            //
            result = new DebugResult(taskId);
            result.setIsTerminated(true);
        }

        return result;
    }

    @Override
    public void addUserContextVariable(@NotNull String taskId, @NotNull String name) {
        debugManager.addUserContextVariable(taskId, name);
    }

    @Override
    public void removeUserContextVariable(@NotNull String taskId, @NotNull String name) {
        debugManager.removeUserContextVariable(taskId, name);
    }

    //- PACKAGE
    void setDebugManager(@NotNull DebugManager debugManager) {
        this.debugManager = debugManager;
    }

    @NotNull
    private DebugResult createDebugResult(@NotNull DebugContext debugContext) {
        DebugResult debugResult = new DebugResult(debugContext.getTaskId());

        // Basic info.
        //
        debugResult.setDebugState(debugContext.getDebugState());
        debugResult.setIsTerminated(debugContext.isTerminated());
        debugResult.setBreakpoints(debugContext.getBreakpoints());

        // Current line, if set.
        //
        debugResult.setCurrentLine(debugContext.getCurrentLine());

        // Debug data from the PEC.
        //
        DebugPecData debugPecData = debugContext.getDebugPecData();
        debugResult.setContextVariables(debugPecData.getContextVariables());
        debugResult.setPolicyResult(debugPecData.getPolicyResult());

        return debugResult;
    }
}