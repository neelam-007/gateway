package com.l7tech.server.stepdebug;

import com.l7tech.common.mime.ContentTypeHeader;
import com.l7tech.common.mime.StashManager;
import com.l7tech.gateway.common.audit.Audit;
import com.l7tech.gateway.common.audit.TestAudit;
import com.l7tech.gateway.common.stepdebug.DebugContextVariableData;
import com.l7tech.gateway.common.stepdebug.DebugResult;
import com.l7tech.gateway.common.stepdebug.DebugState;
import com.l7tech.message.Message;
import com.l7tech.objectmodel.Goid;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.SetVariableAssertion;
import com.l7tech.server.TestStashManagerFactory;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.server.policy.assertion.ServerSetVariableAssertion;
import com.l7tech.test.BugId;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.io.ByteArrayInputStream;
import java.util.*;

import static org.mockito.Matchers.*;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class DebugContextTest {
    private static final Goid POLICY_GOID = new Goid(0, 12345L);
    private static final String TASK_ID = UUID.randomUUID().toString();
    private static final String USER_ADDED_CONTEXT_VARIABLE_NAME = "user_context_variable_name";
    private static final String USER_ADDED_CONTEXT_VARIABLE_VALUE = "user_context_variable_value";

    private final Audit audit = new TestAudit();
    private DebugContext debugContext;

    @Mock
    private PolicyEnforcementContext pec;

    @Mock
    private ServerSetVariableAssertion serverAssertion;

    @Mock
    private SetVariableAssertion assertion;

    @Before
    public void setup() throws Exception {
        StashManager sm = TestStashManagerFactory.getInstance().createStashManager();
        Message req = new Message(sm, ContentTypeHeader.XML_DEFAULT, new ByteArrayInputStream("<in>hello_world</in>".getBytes("UTF-8")));
        Message resp = new Message();

        List<Integer> currentAssertionNumber = new ArrayList<>();
        currentAssertionNumber.add(1);

        when(pec.getAssertionNumber()).thenReturn(currentAssertionNumber);
        when(pec.getRequest()).thenReturn(req);
        when(pec.getResponse()).thenReturn(resp);
        Map<String, Object> variableMap = new HashMap<>();
        variableMap.put(USER_ADDED_CONTEXT_VARIABLE_NAME, USER_ADDED_CONTEXT_VARIABLE_VALUE);
        when(pec.getVariableMap(any(String[].class), any(Audit.class))).thenReturn(variableMap);

        when(serverAssertion.getAssertion()).thenReturn(assertion);
        when(assertion.getOrdinal()).thenReturn(2);

        // Create debug context.
        //
        debugContext = new DebugContext(POLICY_GOID, TASK_ID, audit);
        this.checkDebugContext(DebugState.STOPPED, false, 0, 0);

        // Start debug context.
        //
        debugContext.startDebugging();
        this.checkDebugContext(DebugState.STARTED, false, 0, 0);

        // Set message arrived.
        //
        this.setMessageArrived();

        // Set assertion started.
        this.setAssertionStarted();
    }

    @Test
    public void testStopDebugging() throws Exception {
        debugContext.stopDebugging();
        this.checkDebugContext(DebugState.STOPPED, false, 0, 1);
    }

    @Test
    public void testRestartDebugging() throws Exception {
        // Stopping debugging should retain context variables.
        //
        debugContext.stopDebugging();
        this.checkDebugContext(DebugState.STOPPED, false, 0, 1);

        // Restarting debugging should clear context variables.
        //
        debugContext.startDebugging();
        this.checkDebugContext(DebugState.STARTED, false, 0, 0);
    }

    @Test
    public void testStepOver() throws Exception {
        List<Integer> nextBreakLine = new ArrayList<>(1);
        nextBreakLine.add(3);

        debugContext.stepOver(nextBreakLine);
        this.checkDebugContext(DebugState.BREAK_AT_NEXT_BREAKPOINT, true, 0, 1);
    }

    @Test
    public void testStepInto() throws Exception {
        debugContext.stepInto();
        this.checkDebugContext(DebugState.BREAK_AT_NEXT_LINE, true, 0, 1);
    }

    @Test
    public void testStepOut() throws Exception {
        List<Integer> nextBreakLine = new ArrayList<>(1);
        nextBreakLine.add(3);

        debugContext.stepOut(nextBreakLine);
        this.checkDebugContext(DebugState.BREAK_AT_NEXT_BREAKPOINT, true, 0, 1);
    }

    @Test
    public void testResume() throws Exception {
        debugContext.resume();
        this.checkDebugContext(DebugState.BREAK_AT_NEXT_BREAKPOINT, true, 0, 1);
    }

    @Test
    public void testToggleBreakpoint() throws Exception {
        Collection<Integer> breakpoint = new ArrayList<>(2);
        breakpoint.add(4);
        breakpoint.add(1);
        debugContext.toggleBreakpoint(breakpoint);
        this.checkDebugContext(DebugState.BREAK_AT_NEXT_BREAKPOINT, true, 1, 1);

        debugContext.toggleBreakpoint(breakpoint);
        this.checkDebugContext(DebugState.BREAK_AT_NEXT_BREAKPOINT, true, 0, 1);
    }

    @Test
    public void testRemoveAllBreakpoints() throws Exception {
        Collection<Integer> breakpoint = new ArrayList<>(2);
        breakpoint.add(4);
        breakpoint.add(1);
        debugContext.toggleBreakpoint(breakpoint);

        Collection<Integer> anotherBreakpoint = new ArrayList<>(2);
        anotherBreakpoint.add(4);
        anotherBreakpoint.add(2);
        debugContext.toggleBreakpoint(anotherBreakpoint);

        Set<Collection<Integer>> breakpoints = new HashSet<>();
        breakpoints.add(breakpoint);
        breakpoints.add(anotherBreakpoint);
        this.checkDebugContext(DebugState.BREAK_AT_NEXT_BREAKPOINT, true, breakpoints.size(), 1);

        debugContext.removeAllBreakpoints();
        this.checkDebugContext(DebugState.BREAK_AT_NEXT_BREAKPOINT, true, 0, 1);
    }

    @Test
    public void testAddUserContextVariable() throws Exception {
        debugContext.addUserContextVariable(USER_ADDED_CONTEXT_VARIABLE_NAME);
        debugContext.onStartAssertion(pec, serverAssertion);
        this.checkDebugContext(DebugState.BREAK_AT_NEXT_BREAKPOINT, true, 0, 2);
    }

    @Test
    public void testRemoveUserContextVariable() throws Exception {
        debugContext.addUserContextVariable(USER_ADDED_CONTEXT_VARIABLE_NAME);
        debugContext.onStartAssertion(pec, serverAssertion);
        this.checkDebugContext(DebugState.BREAK_AT_NEXT_BREAKPOINT, true, 0, 2);

        debugContext.removeUserContextVariable(USER_ADDED_CONTEXT_VARIABLE_NAME);
        debugContext.onStartAssertion(pec, serverAssertion);
        this.checkDebugContext(DebugState.BREAK_AT_NEXT_BREAKPOINT, true, 0, 1);
    }

    @BugId("SSM-4578")
    @Test
    public void testOnMessageFinishedSuccessfully() throws Exception {
        when(pec.getPolicyResult()).thenReturn(AssertionStatus.NONE);
        debugContext.onMessageFinished(pec);

        // Verify policy result.
        //
        DebugPecData debugPecData = debugContext.getDebugPecData();
        Assert.assertNotNull(debugPecData);
        Assert.assertEquals(DebugResult.SUCCESSFUL_POLICY_RESULT_MESSAGE, debugPecData.getPolicyResult());
    }

    @BugId("SSM-4578,SSM-4622")
    @Test
    public void testOnMessageFinishedUnsuccessfully() throws Exception {
        when(pec.getPolicyResult()).thenReturn(AssertionStatus.AUTH_REQUIRED);
        debugContext.onMessageFinished(pec);

        // Verify policy result.
        //
        DebugPecData debugPecData = debugContext.getDebugPecData();
        Assert.assertNotNull(debugPecData);
        Assert.assertNotNull(debugPecData.getPolicyResult());
        Assert.assertTrue(debugPecData.getPolicyResult().startsWith(DebugResult.ERROR_POLICY_RESULT_MESSAGE));
        Assert.assertTrue(debugPecData.getPolicyResult().contains(AssertionStatus.AUTH_REQUIRED.getMessage()));
        Assert.assertTrue(debugPecData.getPolicyResult().contains("assertion number"));
    }

    private void setMessageArrived() {
        debugContext.onMessageArrived(pec);
        this.checkDebugContext(DebugState.BREAK_AT_NEXT_BREAKPOINT, true, 0, 1);
    }

    private void setAssertionStarted() {
        debugContext.onStartAssertion(pec, serverAssertion);
        this.checkDebugContext(DebugState.BREAK_AT_NEXT_BREAKPOINT, true, 0, 1);
    }

    private void checkDebugContext(DebugState expectedDebugState,
                                   boolean expectedCurrentLineSet,
                                   int expectedNumberOfBreakpoints,
                                   int expectedNumberOfContextVariables) {
        Assert.assertEquals(POLICY_GOID, debugContext.getPolicyGoid());
        Assert.assertEquals(TASK_ID, debugContext.getTaskId());
        Assert.assertEquals(expectedDebugState, debugContext.getDebugState());
        if (expectedCurrentLineSet) {
            Assert.assertNotNull(debugContext.getCurrentLine());
        } else {
            Assert.assertNull(debugContext.getCurrentLine());
        }
        Assert.assertNotNull(debugContext.getBreakpoints());
        Assert.assertEquals(expectedNumberOfBreakpoints, debugContext.getBreakpoints().size());
        Assert.assertNotNull(debugContext.getLastUpdatedTimeMillis());

        this.checkDebugPecData(expectedNumberOfContextVariables);
    }

    private void checkDebugPecData(int expectedNumberOfContextVariables) {
        Assert.assertNotNull(debugContext.getDebugPecData());
        Assert.assertNotNull(debugContext.getDebugPecData().getContextVariables());
        Set<DebugContextVariableData> vars = debugContext.getDebugPecData().getContextVariables();
        Assert.assertNotNull(vars);

        Assert.assertEquals(expectedNumberOfContextVariables, vars.size());
    }
}