package com.l7tech.server.stepdebug;

import com.l7tech.gateway.common.audit.Audit;
import com.l7tech.gateway.common.stepdebug.DebugState;
import com.l7tech.objectmodel.Goid;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.server.policy.assertion.ServerAssertion;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.*;

/**
 * The debug context. Contains information about policy step debugger state and debugging related information.
 */
public class DebugContext {

    private final Lock lock = new ReentrantLock(true);
    private final Condition executionPermitted = lock.newCondition();
    private final Condition debugContextUpdated = lock.newCondition();
    private boolean isDirty = false; // Indicates debug context has changed.

    private final Goid entityGoid;
    private final boolean isService;
    private final String taskId;
    private final DebugPecData debugPecData;
    private DebugState debugState;
    private final Set<Collection<Integer>> breakpoints = new HashSet<>();
    private Collection<Integer> breakNextLine; // Next line to break at. Set when step over or step out is performed.
    private Collection<Integer> currentLine;
    private Set<String> userContextVariables = new HashSet<>();

    /**
     * Creates <code>DebugContext</code>.
     *
     * @param entityGoid the service or policy GOID
     * @param isService true if the entity is a service, false if the entity is a policy
     * @param taskId the task ID
     * @param audit the audit
     */
    public DebugContext(@NotNull Goid entityGoid, boolean isService, @NotNull String taskId, @NotNull Audit audit) {
        this.entityGoid = entityGoid;
        this.isService = isService;
        this.taskId = taskId;
        this.debugPecData = new DebugPecData(audit);
        this.setDebugState(DebugState.STOPPED);
    }

    /**
     * Returns the service/policy GOID.
     *
     * @return the service or policy GOID
     */
    @NotNull
    public Goid getEntityGoid() {
        lock.lock();
        try {
            return entityGoid;
        } finally {
            lock.unlock();
        }
    }

    /**
     * Returns true if the entity is a service, otherwise false.
     *
     * @return true if the entity is a service, otherwise false
     */
    public boolean isService() {
        lock.lock();
        try {
            return isService;
        } finally {
            lock.unlock();
        }
    }

    /**
     * Returns the task ID.
     *
     * @return the task ID
     */
    @NotNull
    public String getTaskId() {
        lock.lock();
        try {
            return taskId;
        } finally {
            lock.unlock();
        }
    }

    /**
     * Returns the debug state.
     *
     * @return the debug state
     */
    @NotNull
    public DebugState getDebugState() {
        lock.lock();
        try {
            return debugState;
        }  finally {
            lock.unlock();
        }
    }

    /**
     * Returns the breakpoints.
     *
     * @return the breakpoints
     */
    @NotNull
    public Set<Collection<Integer>> getBreakpoints() {
        lock.lock();
        try {
            return breakpoints;
        }  finally {
            lock.unlock();
        }
    }

    /**
     * Returns the current line in execution, if set.
     *
     * @return the current line in execution if set, null otherwise
     */
    @Nullable
    public Collection<Integer> getCurrentLine() {
        lock.lock();
        try {
            return currentLine;
        }  finally {
            lock.unlock();
        }
    }

    /**
     * Returns the debug PEC data.
     *
     * @return the debug PEC data
     */
    @NotNull
    public DebugPecData getDebugPecData() {
        lock.lock();
        try {
            return debugPecData;
        }  finally {
            lock.unlock();
        }
    }

    /**
     * Starts debugging.
     */
    public void startDebugging() {
        lock.lock();
        try {
            this.setDebugState(DebugState.STARTED);
        } finally {
            this.markAsDirty();
            lock.unlock();
        }

    }

    /**
     * Stops debugging.
     */
    public void stopDebugging() {
        lock.lock();
        try {
            this.setDebugState(DebugState.STOPPED);
            this.currentLine = null;
            this.breakNextLine = null;
            this.debugPecData.reset(userContextVariables);
        } finally {
            this.markAsDirty();
            lock.unlock();
        }
    }

    /**
     * Steps-over debugging.
     */
    public void stepOver(@NotNull Collection<Integer> assertionNumber) {
        lock.lock();
        try {
            this.breakNextLine = assertionNumber;
            this.setDebugState(DebugState.BREAK_AT_NEXT_BREAKPOINT);
        } finally {
            this.markAsDirty();
            lock.unlock();
        }
    }

    /**
     * Steps-into debugging.
     */
    public void stepInto() {
        lock.lock();
        try {
            this.setDebugState(DebugState.BREAK_AT_NEXT_LINE);
        } finally {
            this.markAsDirty();
            lock.unlock();
        }
    }

    /**
     * Steps-out debugging.
     */
    public void stepOut(@NotNull Collection<Integer> assertionNumber) {
        lock.lock();
        try {
            this.breakNextLine = assertionNumber;
            this.setDebugState(DebugState.BREAK_AT_NEXT_BREAKPOINT);
        } finally {
            this.markAsDirty();
            lock.unlock();
        }
    }

    /**
     * Resumes debugging.
     */
    public void resume() {
        lock.lock();
        try {
            this.setDebugState(DebugState.BREAK_AT_NEXT_BREAKPOINT);
        } finally {
            this.markAsDirty();
            lock.unlock();
        }
    }

    /**
     * Toggles breakpoint specified by the given assertion number.
     *
     * @param assertionNumber the assertion number
     */
    public void toggleBreakpoint(@NotNull Collection<Integer> assertionNumber) {
        lock.lock();
        try {
            if (breakpoints.contains(assertionNumber)) {
                breakpoints.remove(assertionNumber);
            } else {
                breakpoints.add(assertionNumber);
            }
        } finally {
            this.markAsDirty();
            lock.unlock();
        }
    }

    /**
     * Removes all breakpoints.
     */
    public void removeAllBreakpoints() {
        lock.lock();
        try {
            breakpoints.clear();
        } finally {
            this.markAsDirty();
            lock.unlock();
        }
    }

    /**
     * Adds a context variable to debug. If the context variable name already exist, then it
     * is ignored.
     *
     * @param name the context variable name
     */
    public void addUserContextVariable(@NotNull String name) {
        lock.lock();
        try {
            if (userContextVariables.add(name)) {
                debugPecData.addUserContextVariable(name);
            }
        } finally {
            this.markAsDirty();
            lock.unlock();
        }
    }

    /**
     * Removes a context variable to debug. If the context variable name does not exist, then it
     * is ignored.
     *
     * @param name the context variable name
     */
    public void removeUserContextVariable(@NotNull String name) {
        lock.lock();
        try {
            if (userContextVariables.remove(name)) {
                debugPecData.removeUserContextVariable(name);
            }
        } finally {
            this.markAsDirty();
            lock.unlock();
        }
    }

    /**
     * This method must be called when a message arrives.
     *
     * @param pec the PEC
     */
    public void onMessageArrived(@NotNull PolicyEnforcementContext pec) {
        lock.lock();
        try {
            this.currentLine = pec.getAssertionNumber();
            this.setDebugState(DebugState.BREAK_AT_NEXT_BREAKPOINT);
            this.debugPecData.update(pec, userContextVariables);
            pec.setDebugContext(this);
        } finally {
            this.markAsDirty();
            lock.unlock();
        }
    }

    /**
     * This method must be called each assertion is about to get executed in policy during debugging.
     *
     * @param pec the PEC
     * @param serverAssertion the current server assertion
     */
    public void onStartAssertion(@NotNull PolicyEnforcementContext pec, @NotNull ServerAssertion serverAssertion) {
        lock.lock();
        try {
            if (!DebugState.STOPPED.equals(this.debugState)) {
                this.currentLine = pec.getAssertionNumber();
                this.debugPecData.update(pec, userContextVariables);
                if (serverAssertion.getAssertion().getOrdinal() != 1) {
                    switch (this.debugState) {
                        case BREAK_AT_NEXT_LINE:
                            this.setDebugState(DebugState.AT_BREAKPOINT);
                            this.markAsDirty();
                            this.waitForExecutionToBePermitted();
                            break;

                        case BREAK_AT_NEXT_BREAKPOINT:
                            if (this.isBreakpointSetAtCurrentLine()) {
                                this.setDebugState(DebugState.AT_BREAKPOINT);
                                this.markAsDirty();
                                this.waitForExecutionToBePermitted();
                            }
                            break;

                        // Ignore all other states.
                        //
                    }
                }
            }
        }  finally {
            lock.unlock();
        }
    }

    /**
     * Waits for changes to occur in the debug context up to maxMillisToWait, or returns immediately if changes has occurred
     * since last time this method called. Returns true if changes occurred, false otherwise.
     *
     * @param maxMillisToWait max time to wait in millis
     * @return true if changes occurred, false otherwise
     */
    public boolean waitForUpdates(long maxMillisToWait) {
        lock.lock();
        try {
            if (isDirty) {
                isDirty = false;
                return true;
            } else {
                boolean isUpdated = debugContextUpdated.await(maxMillisToWait, TimeUnit.MILLISECONDS);
                if (isUpdated) {
                    isDirty = false;
                }
                return isUpdated;
            }
        } catch (InterruptedException e) {
            // Do nothing.
        } finally {
            lock.unlock();
        }

        return false;
    }

    private void setDebugState(@NotNull DebugState debugState) {
        lock.lock();
        try {
            this.debugState = debugState;
            if (debugState.equals(DebugState.AT_BREAKPOINT)) {
                this.breakNextLine = null;
            }
            executionPermitted.signal();
        } finally {
            lock.unlock();
        }
    }

    private void waitForExecutionToBePermitted() {
        lock.lock();
        try {
            while (debugState.equals(DebugState.AT_BREAKPOINT)) {
                executionPermitted.await();
            }
        } catch (InterruptedException e) {
            // Do nothing.
        } finally {
            lock.unlock();
        }
    }

    private boolean isBreakpointSetAtCurrentLine() {
        lock.lock();
        try {
            return breakpoints.contains(currentLine) ||
                (breakNextLine != null && breakNextLine.equals(currentLine)) ;
        }  finally {
            lock.unlock();
        }
    }

    private void markAsDirty() {
        lock.lock();
        try {
            this.isDirty = true;
            this.debugContextUpdated.signal();
        }  finally {
            lock.unlock();
        }
    }
}