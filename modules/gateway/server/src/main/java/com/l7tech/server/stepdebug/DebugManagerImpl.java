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
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.Logger;

/**
 *
 */
public class DebugManagerImpl implements DebugManager {
    private static final Logger logger = Logger.getLogger(DebugManagerImpl.class.getName());

    private final Audit audit;
    private final Map<String, DebugContext> debugTasks = new ConcurrentHashMap<>();
    private final Map<Goid, DebugContext> waitingForMsg = new ConcurrentHashMap<>(); // Debugger started and waiting for message to arrive.
    private final Lock lock = new ReentrantLock(true);

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
    public DebugContext createDebugContext(@NotNull Goid policyGoid) {
        String taskId = UUID.randomUUID().toString();
        DebugContext debugContext = new DebugContext(policyGoid, taskId, audit);
        debugTasks.put(taskId, debugContext);

        return debugContext;
    }

    @Override
    @NotNull
    public Option<String> startDebug(@NotNull String taskId) {
        lock.lock();
        try {
            DebugContext debugContext = this.getDebugContextFailIfNull(taskId);
            // Do not start debugger, if another debugger already running for the
            // given service/policy.
            //
            Goid policyGoid = debugContext.getPolicyGoid();
            boolean isFound = false;
            for (DebugContext context : debugTasks.values()) {
                if (policyGoid.equals(context.getPolicyGoid()) &&
                    !context.getDebugState().equals(DebugState.STOPPED)) {
                    isFound = true;
                }
            }

            if (isFound) {
                return Option.some("Cannot start Service Debugger. There is a Service Debugger already running for the service/policy.");
            }

            audit.logAndAudit(
                SystemMessages.SERVICE_DEBUGGER_START,
                policyGoid.toString());

            debugContext.startDebugging();
            waitingForMsg.put(policyGoid, debugContext);
        } finally {
            lock.unlock();
        }

        return Option.none();
    }

    @Override
    public void stopDebug(@NotNull String taskId) {
        lock.lock();
        try {
            DebugContext debugContext = this.getDebugContext(taskId);
            if (debugContext != null && !debugContext.getDebugState().equals(DebugState.STOPPED)) {
                Goid policyGoid = debugContext.getPolicyGoid();
                audit.logAndAudit(
                    SystemMessages.SERVICE_DEBUGGER_STOP,
                    policyGoid.toString());
                debugContext.stopDebugging();
                waitingForMsg.remove(policyGoid);
            }
        } finally {
            lock.unlock();
        }
    }

    @Override
    public void stepOver(@NotNull String taskId, @NotNull Collection<Integer> assertionNumber) {
        lock.lock();
        try {
            DebugContext debugContext = this.getDebugContextFailIfNull(taskId);
            debugContext.stepOver(assertionNumber);
        } finally {
            lock.unlock();
        }
    }

    @Override
    public void stepInto(@NotNull String taskId) {
        lock.lock();
        try {
            DebugContext debugContext = this.getDebugContextFailIfNull(taskId);
            debugContext.stepInto();
        } finally {
            lock.unlock();
        }
    }

    @Override
    public void stepOut(@NotNull String taskId, @NotNull Collection<Integer> assertionNumber) {
        lock.lock();
        try {
            DebugContext debugContext = this.getDebugContextFailIfNull(taskId);
            debugContext.stepOut(assertionNumber);
        } finally {
            lock.unlock();
        }
    }

    @Override
    public void resume(@NotNull String taskId) {
        lock.lock();
        try {
            DebugContext debugContext = this.getDebugContextFailIfNull(taskId);
            debugContext.resume();
        } finally {
            lock.unlock();
        }
    }

    @Override
    public void terminateDebug(@NotNull String taskId) {
        lock.lock();
        try {
            this.stopDebug(taskId);
            debugTasks.remove(taskId);
        } finally {
            lock.unlock();
        }
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
        lock.lock();
        try {
            DebugContext debugContext = waitingForMsg.remove(entityGoid);
            if (debugContext != null) {
                debugContext.onMessageArrived(pec);
            }
        } finally {
            lock.unlock();
        }
    }

    @Override
    public void onMessageFinished(@NotNull PolicyEnforcementContext pec) {
        lock.lock();
        try {
            DebugContext debugContext = pec.getDebugContext();
            if (debugContext != null) {
                this.stopDebug(debugContext.getTaskId());
            }
        } finally {
            lock.unlock();
        }
    }

    @NotNull
    private DebugContext getDebugContextFailIfNull(@NotNull String taskId) {
        return debugTasks.get(taskId);
    }
}