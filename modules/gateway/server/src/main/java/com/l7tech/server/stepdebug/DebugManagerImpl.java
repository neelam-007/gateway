package com.l7tech.server.stepdebug;

import com.l7tech.gateway.common.audit.Audit;
import com.l7tech.gateway.common.audit.AuditFactory;
import com.l7tech.gateway.common.stepdebug.DebugState;
import com.l7tech.objectmodel.Goid;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.server.event.admin.PolicyDebuggerAdminEvent;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.util.Background;
import com.l7tech.util.ConfigFactory;
import com.l7tech.util.Option;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.ApplicationEventPublisherAware;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 */
public class DebugManagerImpl implements ApplicationEventPublisherAware, DebugManager {
    private static final Logger logger = Logger.getLogger(DebugManagerImpl.class.getName());

    private static final String PROP_INACTIVE_SESSION_CLEAN_INTERVAL_MILLIS = "com.l7tech.server.stepdebug.inactiveSessionCleanIntervalMillis";
    private static final long DEFAULT_INACTIVE_SESSION_CLEAN_INTERVAL_MILLIS = 86460000L; // 1 Day + 1 Min

    private static final String PROP_INACTIVE_SESSION_TIMEOUT_MILLIS = "com.l7tech.server.stepdebug.inactiveSessionTimeoutMillis";
    private static final long DEFAULT_INACTIVE_SESSION_TIMEOUT_MILLIS = 86400000L; // 1 Day

    private final Audit audit;
    private ApplicationEventPublisher applicationEventPublisher;
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

        final long inactiveSessionCleanIntervalMillis = ConfigFactory.getLongProperty(
            PROP_INACTIVE_SESSION_CLEAN_INTERVAL_MILLIS,
            DEFAULT_INACTIVE_SESSION_CLEAN_INTERVAL_MILLIS);
        final long inactiveSessionTimeoutMillis = ConfigFactory.getLongProperty(
            PROP_INACTIVE_SESSION_TIMEOUT_MILLIS,
            DEFAULT_INACTIVE_SESSION_TIMEOUT_MILLIS);

        Background.scheduleRepeated(
            new TimerTask() {
                @Override
                public void run() {
                    long currentTimeMillis = System.currentTimeMillis();
                    for (DebugContext context : debugTasks.values()) {
                        long lastAccessTimeMillis = context.getLastUpdatedTimeMillis();
                        if ((currentTimeMillis - lastAccessTimeMillis) >= inactiveSessionTimeoutMillis) {
                            logger.log(Level.INFO, "Cleanup inactive service debugger session for policy '" + context.getPolicyGoid().toString() +"'.");
                            terminateDebug(context.getTaskId());
                        }
                    }
                } },
            inactiveSessionCleanIntervalMillis,
            inactiveSessionCleanIntervalMillis);
    }

    @Override
    public void setApplicationEventPublisher(ApplicationEventPublisher applicationEventPublisher) {
        this.applicationEventPublisher = applicationEventPublisher;
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

            applicationEventPublisher.publishEvent(new PolicyDebuggerAdminEvent(this, true, policyGoid));
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
            if (debugContext != null) {
                Goid policyGoid = debugContext.getPolicyGoid();
                waitingForMsg.remove(policyGoid);
                if (!debugContext.getDebugState().equals(DebugState.STOPPED)) {
                    applicationEventPublisher.publishEvent(new PolicyDebuggerAdminEvent(this, false, policyGoid));
                    debugContext.stopDebugging();
                }
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
            DebugContext debugContext = this.getDebugContext(taskId);
            if (debugContext != null) {
                debugContext.setIsTerminated(true);
            }
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
    public void onMessageArrived(@NotNull PolicyEnforcementContext pec, @NotNull Goid policyGoid) {
        lock.lock();
        try {
            DebugContext debugContext = waitingForMsg.remove(policyGoid);
            if (debugContext != null) {
                debugContext.onMessageArrived(pec);
            }
        } finally {
            lock.unlock();
        }
    }

    @Override
    public void onMessageFinished(@NotNull PolicyEnforcementContext pec, @Nullable AssertionStatus status) {
        lock.lock();
        try {
            DebugContext debugContext = pec.getDebugContext();
            if (debugContext != null) {
                // Call DebugContext#onMessageFinished() prior to calling stopDebug().
                // stopDebug() will reset the current line member variable of debug context.
                //
                debugContext.onMessageFinished(pec, status);
                this.stopDebug(debugContext.getTaskId());
            }
        } finally {
            lock.unlock();
        }
    }

    //- PACKAGE

    void cleanUp() {
        // Cleanup debugContexts. Intended to be used only by JUnit test.
        //
        for (DebugContext context : debugTasks.values()) {
            this.terminateDebug(context.getTaskId());
        }

        debugTasks.clear();
        waitingForMsg.clear();
    }

    //- PRIVATE

    @NotNull
    private DebugContext getDebugContextFailIfNull(@NotNull String taskId) {
        return debugTasks.get(taskId);
    }
}