package com.l7tech.server.stepdebug;

import com.l7tech.gateway.common.audit.Audit;
import com.l7tech.gateway.common.audit.AuditFactory;
import com.l7tech.gateway.common.audit.SystemMessages;
import com.l7tech.gateway.common.stepdebug.DebugState;
import com.l7tech.objectmodel.Goid;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.util.Option;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.concurrent.*;
import java.util.logging.Logger;

/**
 *
 */
public class DebugManagerImpl implements DebugManager {
    private static final Logger logger = Logger.getLogger(DebugManagerImpl.class.getName());

    private final Audit audit;
    private final Map<String, DebugContext> debugTasks = new ConcurrentHashMap<>();

    /**
     * Constructor.
     *
     * @param auditFactory the audit factory
     */
    public DebugManagerImpl(@NotNull AuditFactory auditFactory) {
        this.audit = auditFactory.newInstance(this, logger);
    }

    @Override
    @NotNull
    public DebugContext createDebugContext(@NotNull Goid entityGoid, boolean isService) {
        String taskId = UUID.randomUUID().toString();
        DebugContext debugContext = new DebugContext(entityGoid, isService, taskId, audit);
        debugTasks.put(taskId, debugContext);

        return debugContext;
    }

    @Override
    @NotNull
    public Option<String> startDebug(@NotNull String taskId) {
        DebugContext debugContext = this.getDebugContextFailIfNull(taskId);
        // Do not start debugger, if another debugger is running for the
        // given service/policy.
        //
        Goid entityGoid = debugContext.getEntityGoid();
        boolean isFound = false;
        for (DebugContext context : debugTasks.values()) {
            if (entityGoid.equals(context.getEntityGoid()) &&
                !context.getDebugState().equals(DebugState.STOPPED)) {
                isFound = true;
            }
        }

        if (isFound) {
            return Option.some("Cannot start Service Debugger. There is a Service Debugger already running for the service/policy.");
        }

        audit.logAndAudit(
            SystemMessages.SERVICE_DEBUGGER_START,
            debugContext.isService() ? "service" : "policy",
            debugContext.getEntityGoid().toString());
        debugContext.startDebugging();
        return Option.none();
    }

    @Override
    public void stopDebug(@NotNull String taskId) {
        DebugContext debugContext = this.getDebugContext(taskId);
        if (debugContext != null && !debugContext.getDebugState().equals(DebugState.STOPPED)) {
            audit.logAndAudit(
                SystemMessages.SERVICE_DEBUGGER_STOP,
                debugContext.isService() ? "service" : "policy",
                debugContext.getEntityGoid().toString());
            debugContext.stopDebugging();
        }
    }

    @Override
    public void stepOver(@NotNull String taskId, @NotNull Collection<Integer> assertionNumber) {
        DebugContext debugContext = this.getDebugContextFailIfNull(taskId);
        debugContext.stepOver(assertionNumber);
    }

    @Override
    public void stepInto(@NotNull String taskId) {
        DebugContext debugContext = this.getDebugContextFailIfNull(taskId);
        debugContext.stepInto();
    }

    @Override
    public void stepOut(@NotNull String taskId, @NotNull Collection<Integer> assertionNumber) {
        DebugContext debugContext = this.getDebugContextFailIfNull(taskId);
        debugContext.stepOut(assertionNumber);
    }

    @Override
    public void resume(@NotNull String taskId) {
        DebugContext debugContext = this.getDebugContextFailIfNull(taskId);
        debugContext.resume();
    }

    @Override
    public void terminateDebug(@NotNull String taskId) {
        this.stopDebug(taskId);
        debugTasks.remove(taskId);
    }

    @Override
    public void toggleBreakpoint(@NotNull String taskId, @NotNull Collection<Integer> assertionNumber) {
        DebugContext debugContext = this.getDebugContextFailIfNull(taskId);
        debugContext.toggleBreakpoint(assertionNumber);
    }

    @Override
    public void removeAllBreakpoints(@NotNull String taskId) {
        DebugContext debugContext = this.getDebugContextFailIfNull(taskId);
        debugContext.removeAllBreakpoints();
    }

    @Override
    public void addUserContextVariable(@NotNull String taskId, @NotNull String name) {
        DebugContext debugContext = this.getDebugContextFailIfNull(taskId);
        debugContext.addUserContextVariable(name);
    }

    @Override
    public void removeUserContextVariable(@NotNull String taskId, @NotNull String name) {
        DebugContext debugContext = this.getDebugContextFailIfNull(taskId);
        debugContext.removeUserContextVariable(name);
    }

    @Override
    @Nullable
    public DebugContext getDebugContext(@NotNull String taskId) {
        return debugTasks.get(taskId);
    }

    @Override
    public void onMessageArrived(@NotNull PolicyEnforcementContext pec, @NotNull Goid entityGoid) {
        for (DebugContext debugContext : debugTasks.values()) {
            if (debugContext.isWaitingForMessageToArrive(entityGoid)) {
                debugContext.onMessageArrived(pec);
                break;
            }
        }
    }

    @Override
    public void onMessageFinished(@NotNull PolicyEnforcementContext pec) {
        DebugContext debugContext = pec.getDebugContext();
        if (debugContext != null) {
            this.stopDebug(debugContext.getTaskId());
        }
    }

    @NotNull
    private DebugContext getDebugContextFailIfNull(@NotNull String taskId) {
        return debugTasks.get(taskId);
    }
}