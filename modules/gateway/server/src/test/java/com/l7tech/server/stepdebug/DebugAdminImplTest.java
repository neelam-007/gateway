package com.l7tech.server.stepdebug;

import com.l7tech.gateway.common.audit.AuditFactory;
import com.l7tech.gateway.common.audit.TestAudit;
import com.l7tech.gateway.common.stepdebug.DebugContextVariableData;
import com.l7tech.gateway.common.stepdebug.DebugResult;
import com.l7tech.gateway.common.stepdebug.DebugState;
import com.l7tech.objectmodel.Goid;
import com.l7tech.server.event.admin.PolicyDebuggerAdminEvent;
import com.l7tech.util.Option;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationEventPublisher;

import java.util.*;

/**
 * Most of the methods in {@link DebugAdminImpl} are pass-through to {@link DebugManagerImpl}.
 * See {@link DebugManagerImplTest} for more tests.
 */
public class DebugAdminImplTest {
    private static final Goid POLICY_GOID = new Goid(0, 12345L);
    private static final long MAX_WAIT_TIME_MILLIS = 500L;

    private final AuditFactory auditFactory = new TestAudit().factory();
    private DebugAdminImpl admin;
    private String taskId;
    private List<Integer> assertionLineNumber;

    @Before
    public void setup() {
        DebugManagerImpl manager = new DebugManagerImpl(auditFactory);
        manager.setApplicationEventPublisher(new ApplicationEventPublisher() {
            @Override
            public void publishEvent(ApplicationEvent applicationEvent) {
                Assert.assertTrue(applicationEvent instanceof PolicyDebuggerAdminEvent);
            }});
        admin = new DebugAdminImpl();
        admin.setDebugManager(manager);

        // Initialize debugger session.
        //
        DebugResult debugResult = admin.initializeDebug(POLICY_GOID);
        Assert.assertNotNull(debugResult);

        taskId = debugResult.getTaskId();
        Assert.assertNotNull(taskId);
        Assert.assertFalse(taskId.isEmpty());

        // Start debugger session.
        //
        Option<String> option = admin.startDebug(taskId);
        Assert.assertFalse(option.isSome());
        debugResult = admin.waitForUpdates(taskId, MAX_WAIT_TIME_MILLIS);
        Assert.assertNotNull(debugResult);
        Assert.assertEquals(DebugState.STARTED, debugResult.getDebugState());

        // Initialize assertion line number for testing (Line number 2.1).
        //
        assertionLineNumber = new ArrayList<>(2);
        assertionLineNumber.add(2);
        assertionLineNumber.add(1);
    }

    @Test
    public void testCannotStartDebugWhenAlreadyAnotherActive() throws Exception {
        // Test that a second debugger cannot be started if there is an active debugger for the same policy.
        //

        // Initialize debugger session.
        //
        DebugResult debugResult = admin.initializeDebug(POLICY_GOID);
        Assert.assertNotNull(debugResult);

        taskId = debugResult.getTaskId();
        Assert.assertNotNull(taskId);
        Assert.assertFalse(taskId.isEmpty());

        // Start debugger session.
        //
        Option<String> option = admin.startDebug(taskId);
        Assert.assertTrue(option.isSome());
    }

    @Test
    public void testStopDebug() throws Exception {
        admin.stopDebug(taskId);
        DebugResult debugResult = admin.waitForUpdates(taskId, MAX_WAIT_TIME_MILLIS);
        Assert.assertNotNull(debugResult);
        Assert.assertEquals(DebugState.STOPPED, debugResult.getDebugState());
    }

    @Test
    public void testStepOver() throws Exception {
        admin.stepOver(taskId, assertionLineNumber);
        DebugResult debugResult = admin.waitForUpdates(taskId, MAX_WAIT_TIME_MILLIS);
        Assert.assertNotNull(debugResult);
        Assert.assertEquals(DebugState.BREAK_AT_NEXT_BREAKPOINT, debugResult.getDebugState());
    }

    @Test
    public void testStepInto() throws Exception {
        admin.stepInto(taskId);
        DebugResult debugResult = admin.waitForUpdates(taskId, MAX_WAIT_TIME_MILLIS);
        Assert.assertNotNull(debugResult);
        Assert.assertEquals(DebugState.BREAK_AT_NEXT_LINE, debugResult.getDebugState());
    }

    @Test
    public void testStepOut() throws Exception {
        admin.stepOut(taskId, assertionLineNumber);
        DebugResult debugResult = admin.waitForUpdates(taskId, MAX_WAIT_TIME_MILLIS);
        Assert.assertNotNull(debugResult);
        Assert.assertEquals(DebugState.BREAK_AT_NEXT_BREAKPOINT, debugResult.getDebugState());
    }

    @Test
    public void testResume() throws Exception {
        admin.resume(taskId);
        DebugResult debugResult = admin.waitForUpdates(taskId, MAX_WAIT_TIME_MILLIS);
        Assert.assertNotNull(debugResult);
        Assert.assertEquals(DebugState.BREAK_AT_NEXT_BREAKPOINT, debugResult.getDebugState());
    }

    @Test
    public void testTerminateDebug() throws Exception {
        admin.terminateDebug(taskId);
        DebugResult debugResult = admin.waitForUpdates(taskId, MAX_WAIT_TIME_MILLIS);
        if (debugResult != null) {
            Assert.assertTrue(debugResult.isTerminated());
        }
        // Null updatedDebugResult means the debugger session was terminated.
    }

    @Test
    public void testToggleBreakpoint() throws Exception {
        admin.toggleBreakpoint(taskId, assertionLineNumber);
        DebugResult debugResult = admin.waitForUpdates(taskId, MAX_WAIT_TIME_MILLIS);
        Assert.assertNotNull(debugResult);
        Set<Collection<Integer>> breakpoints = debugResult.getBreakpoints();
        Assert.assertEquals(1, breakpoints.size());
        Collection<Integer> returnedAssertionNumber = breakpoints.iterator().next();
        Assert.assertArrayEquals(assertionLineNumber.toArray(), returnedAssertionNumber.toArray());

        admin.toggleBreakpoint(taskId, assertionLineNumber);
        debugResult = admin.waitForUpdates(taskId, MAX_WAIT_TIME_MILLIS);
        Assert.assertNotNull(debugResult);
        breakpoints = debugResult.getBreakpoints();
        Assert.assertTrue(breakpoints.isEmpty());
    }

    @Test
    public void testRemoveAllBreakpoints() throws Exception {
        // Set 2 breakpoints.
        //
        admin.toggleBreakpoint(taskId, assertionLineNumber);

        List<Integer> anotherAssertionNumber = new ArrayList<>(2);
        anotherAssertionNumber.add(3);
        admin.toggleBreakpoint(taskId, anotherAssertionNumber);

        DebugResult debugResult = admin.waitForUpdates(taskId, MAX_WAIT_TIME_MILLIS);
        Assert.assertNotNull(debugResult);
        Set<Collection<Integer>> breakpoints = debugResult.getBreakpoints();
        Assert.assertEquals(2, breakpoints.size());

        // Remove All breakpoints.
        //
        admin.removeAllBreakpoints(taskId);
        debugResult = admin.waitForUpdates(taskId, MAX_WAIT_TIME_MILLIS);
        Assert.assertNotNull(debugResult);
        breakpoints = debugResult.getBreakpoints();
        Assert.assertTrue(breakpoints.isEmpty());
    }

    @Test
    public void testWaitForUpdatesNoChanges() throws Exception {
        DebugResult debugResult = admin.waitForUpdates(taskId, MAX_WAIT_TIME_MILLIS);
        Assert.assertNull(debugResult);
    }

    @Test
    public void testAddUserContextVariable() throws Exception {
        admin.addUserContextVariable(taskId, "user_context_var_name");
        DebugResult debugResult = admin.waitForUpdates(taskId, MAX_WAIT_TIME_MILLIS);
        Assert.assertNotNull(debugResult);

        Set<DebugContextVariableData> vars = debugResult.getContextVariables();
        Assert.assertNotNull(vars);
        Assert.assertEquals(1, vars.size());

        DebugContextVariableData var = vars.iterator().next();
        Assert.assertTrue(var.getIsUserAdded());
        Assert.assertTrue(var.getName().contains("user_context_var_name"));
    }

    @Test
    public void testRemoveUserContextVariable() throws Exception {
        // Add user context variable.
        //
        admin.addUserContextVariable(taskId, "user_context_var_name");
        DebugResult debugResult = admin.waitForUpdates(taskId, MAX_WAIT_TIME_MILLIS);
        Assert.assertNotNull(debugResult);

        Set<DebugContextVariableData> vars = debugResult.getContextVariables();
        Assert.assertNotNull(vars);
        Assert.assertEquals(1, vars.size());

        DebugContextVariableData var = vars.iterator().next();
        Assert.assertTrue(var.getIsUserAdded());
        Assert.assertTrue(var.getName().contains("user_context_var_name"));

        // Remove user context variable.
        //
        admin.removeUserContextVariable(taskId, "user_context_var_name");
        debugResult = admin.waitForUpdates(taskId, MAX_WAIT_TIME_MILLIS);
        Assert.assertNotNull(debugResult);

        vars = debugResult.getContextVariables();
        Assert.assertNotNull(vars);
        Assert.assertTrue(vars.isEmpty());
    }
}