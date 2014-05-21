package com.l7tech.server.stepdebug;

import com.l7tech.common.http.HttpMethod;
import com.l7tech.gateway.common.service.PublishedService;
import com.l7tech.gateway.common.stepdebug.DebugState;
import com.l7tech.message.HttpServletRequestKnob;
import com.l7tech.message.HttpServletResponseKnob;
import com.l7tech.message.Message;
import com.l7tech.objectmodel.Goid;
import com.l7tech.server.ApplicationContexts;
import com.l7tech.server.MessageProcessor;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.server.message.PolicyEnforcementContextFactory;
import com.l7tech.server.service.ServiceManager;
import com.l7tech.test.conditional.ConditionalIgnore;
import com.l7tech.test.conditional.ConditionalIgnoreRule;
import com.l7tech.test.conditional.IgnoreOnDaily;
import com.l7tech.util.Option;
import org.junit.*;
import org.springframework.context.ApplicationContext;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

@ConditionalIgnore(condition = IgnoreOnDaily.class)
public class DebugManagerImplTest {

    @Rule
    public ConditionalIgnoreRule rule = new ConditionalIgnoreRule();

    private static final long MAX_WAIT_TIME_MILLI_SECONDS = 1000L;
    private static final long TASK_TIMEOUT_SECONDS = 2L;

    private static final Goid PUBLISHED_SERVICE_GOID = new Goid(0, 12345678L);
    private static final String SERVICE_ROUTING_URI = "/debugger_test";

    private static final Goid POLICY_GOID = new Goid(0, 12345L);
    private static final String POLICY_XML =
        "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
            "<wsp:Policy xmlns:L7p=\"http://www.layer7tech.com/ws/policy\" xmlns:wsp=\"http://schemas.xmlsoap.org/ws/2002/12/policy\">\n" +
            "    <wsp:All wsp:Usage=\"Required\">\n" +
            "        <L7p:SetVariable>\n" +
            "            <L7p:Base64Expression stringValue=\"Y3h0X29uZV92YWx1ZQ==\"/>\n" +
            "            <L7p:VariableToSet stringValue=\"cxt_one\"/>\n" +
            "        </L7p:SetVariable>\n" +
            "        <L7p:SetVariable>\n" +
            "            <L7p:Base64Expression stringValue=\"Y3h0X3R3b192YWx1ZQ==\"/>\n" +
            "            <L7p:VariableToSet stringValue=\"cxt_two\"/>\n" +
            "        </L7p:SetVariable>\n" +
            "        <L7p:SetVariable>\n" +
            "            <L7p:Base64Expression stringValue=\"Y3h0X3RocmVlX3ZhbHVl\"/>\n" +
            "            <L7p:VariableToSet stringValue=\"cxt_three\"/>\n" +
            "        </L7p:SetVariable>\n" +
            "        <wsp:OneOrMore wsp:Usage=\"Required\">\n" +
            "            <wsp:All wsp:Usage=\"Required\">\n" +
            "                <L7p:SetVariable>\n" +
            "                    <L7p:Base64Expression stringValue=\"b3V0MQ==\"/>\n" +
            "                    <L7p:VariableToSet stringValue=\"output\"/>\n" +
            "                </L7p:SetVariable>\n" +
            "                <L7p:SetVariable>\n" +
            "                    <L7p:Base64Expression stringValue=\"b3V0MQ==\"/>\n" +
            "                    <L7p:VariableToSet stringValue=\"output\"/>\n" +
            "                </L7p:SetVariable>\n" +
            "            </wsp:All>\n" +
            "            <wsp:All wsp:Usage=\"Required\">\n" +
            "                <L7p:SetVariable>\n" +
            "                    <L7p:Base64Expression stringValue=\"b3V0Mg==\"/>\n" +
            "                    <L7p:VariableToSet stringValue=\"output\"/>\n" +
            "                </L7p:SetVariable>\n" +
            "                <L7p:SetVariable>\n" +
            "                    <L7p:Base64Expression stringValue=\"b3V0Mg==\"/>\n" +
            "                    <L7p:VariableToSet stringValue=\"output\"/>\n" +
            "                </L7p:SetVariable>\n" +
            "            </wsp:All>\n" +
            "        </wsp:OneOrMore>\n" +
            "        <L7p:SetVariable>\n" +
            "            <L7p:Base64Expression stringValue=\"ZG9uZQ==\"/>\n" +
            "            <L7p:VariableToSet stringValue=\"done\"/>\n" +
            "        </L7p:SetVariable>\n" +
            "    </wsp:All>\n" +
            "</wsp:Policy>\n";

    private static DebugManagerImpl debugManager;
    private static MessageProcessor messageProcessor;

    private ExecutorService executor = Executors.newCachedThreadPool();
    private DebugContext debugContext;
    private PolicyEnforcementContext pec;

    @BeforeClass
    public static void setupBeforeClass() throws Exception {
        ApplicationContext applicationContext = ApplicationContexts.getTestApplicationContext();
        messageProcessor = applicationContext.getBean("messageProcessor", MessageProcessor.class);
        debugManager = applicationContext.getBean("debugManager", DebugManagerImpl.class);
        ServiceManager serviceManager = applicationContext.getBean("serviceManager", ServiceManager.class);

        PublishedService createPublishedService = createPublishedService();
        serviceManager.update(createPublishedService);
    }

    @Before
    public void setup() throws Exception {
        Message req = createRequest();
        Message resp = createResponse();
        pec = PolicyEnforcementContextFactory.createPolicyEnforcementContext(req, resp, true);

        // Cleanup. Each test method should have stopped or terminated its debugger context.
        // In case it didn't, cleanup here so that it does not prevent other test methods to execute.
        //
        debugManager.cleanUp();

        debugContext = debugManager.createDebugContext(POLICY_GOID);
        Assert.assertEquals(DebugState.STOPPED, debugContext.getDebugState());
    }

    @Test
    public void testCannotStartDebuggerForSamePolicy() throws Exception {
        Option<String> option = debugManager.startDebug(debugContext.getTaskId());
        Assert.assertFalse(option.isSome());
        Assert.assertEquals(DebugState.STARTED, debugContext.getDebugState());

        // Attempt to start another debugger.
        //
        DebugContext anotherDebugContext = debugManager.createDebugContext(POLICY_GOID);
        option = debugManager.startDebug(anotherDebugContext.getTaskId());
        Assert.assertTrue(option.isSome());
        Assert.assertEquals(DebugState.STOPPED, anotherDebugContext.getDebugState());
    }

    @Test
    public void testProcessMessageDebuggerNotStarted() throws Exception {
        final boolean[] finishedExecution = new boolean[1];

        // Message Processor.
        //
        executor.submit(new Callable<Void>() {
            @Override
            public Void call() throws Exception {
                messageProcessor.processMessage(pec);
                finishedExecution[0] = true;
                return null;
            }
        });

        executor.awaitTermination(TASK_TIMEOUT_SECONDS, TimeUnit.SECONDS);
        Assert.assertTrue(finishedExecution[0]);
        Assert.assertEquals(DebugState.STOPPED, debugContext.getDebugState());
    }

    @Test
    public void testProcessMessageDebuggerNoBreakpoints() throws Exception {
        Option<String> option = debugManager.startDebug(debugContext.getTaskId());
        Assert.assertFalse(option.isSome());
        Assert.assertEquals(DebugState.STARTED, debugContext.getDebugState());

        final boolean[] finishedExecution = new boolean[1];

        // Message Processor.
        //
        executor.submit(new Callable<Void>() {
            @Override
            public Void call() throws Exception {
                messageProcessor.processMessage(pec);
                finishedExecution[0] = true;
                return null;
            }
        });

        executor.awaitTermination(TASK_TIMEOUT_SECONDS, TimeUnit.SECONDS);
        Assert.assertTrue(finishedExecution[0]);
        Assert.assertEquals(DebugState.STOPPED, debugContext.getDebugState());
    }

    @Test
    public void testProcessMessageDebuggerWithBreakpoint() throws Exception {
        // Add a breakpoint so that the debugger will hit the breakpoint.
        //
        List<Integer> breakpoint = new ArrayList<>(1);
        breakpoint.add(2);
        debugManager.toggleBreakpoint(debugContext.getTaskId(), breakpoint);

        Option<String> option = debugManager.startDebug(debugContext.getTaskId());
        Assert.assertFalse(option.isSome());
        Assert.assertEquals(DebugState.STARTED, debugContext.getDebugState());

        final boolean[] finishedExecution = new boolean[2];

        // Message Processor.
        //
        executor.submit(new Callable<Void>() {
            @Override
            public Void call() throws Exception {
                messageProcessor.processMessage(pec);
                finishedExecution[0] = true;
                return null;
            }
        });

        // The "client".
        //
        executor.submit(new Callable<Void>() {
            @Override
            public Void call() throws Exception {
                while (true) {
                    boolean updated = debugContext.waitForUpdates(MAX_WAIT_TIME_MILLI_SECONDS);
                    if (updated) {
                        DebugState state = debugContext.getDebugState();
                        if (state.equals(DebugState.AT_BREAKPOINT)) {
                            // breakpoint hit. break while loop.
                            break;
                        }
                    }
                }
                finishedExecution[1] = true;
                return null;
            }
        });

        executor.awaitTermination(TASK_TIMEOUT_SECONDS, TimeUnit.SECONDS);
        Assert.assertFalse(finishedExecution[0]); // Still at a breakpoint.
        Assert.assertTrue(finishedExecution[1]);
        Assert.assertEquals(DebugState.AT_BREAKPOINT, debugContext.getDebugState());
        Assert.assertNotNull(debugContext.getCurrentLine());
        Assert.assertArrayEquals(breakpoint.toArray(), debugContext.getCurrentLine().toArray());

        debugManager.stopDebug(debugContext.getTaskId());
        Assert.assertEquals(DebugState.STOPPED, debugContext.getDebugState());
    }

    @Test
    public void testStepOver() throws Exception {
        // Add a breakpoint so that the debugger will hit the breakpoint.
        //
        final List<Integer> breakpoint = new ArrayList<>(1);
        breakpoint.add(2);
        debugManager.toggleBreakpoint(debugContext.getTaskId(), breakpoint);

        final List<Integer> stepOverBreakpoint = new ArrayList<>(1);
        stepOverBreakpoint.add(3);

        Option<String> option = debugManager.startDebug(debugContext.getTaskId());
        Assert.assertFalse(option.isSome());
        Assert.assertEquals(DebugState.STARTED, debugContext.getDebugState());

        final boolean[] finishedExecution = new boolean[2];

        // Message Processor.
        //
        executor.submit(new Callable<Void>() {
            @Override
            public Void call() throws Exception {
                messageProcessor.processMessage(pec);
                finishedExecution[0] = true;
                return null;
            }
        });

        // The "client".
        //
        executor.submit(new Callable<Void>() {
            @Override
            public Void call() throws Exception {
                while (true) {
                    boolean updated = debugContext.waitForUpdates(MAX_WAIT_TIME_MILLI_SECONDS);
                    if (updated) {
                        DebugState state = debugContext.getDebugState();
                        if (state.equals(DebugState.AT_BREAKPOINT)) {
                            debugManager.stepOver(debugContext.getTaskId(), stepOverBreakpoint);
                            break;
                        }
                    }
                }
                finishedExecution[1] = true;
                return null;
            }
        });

        executor.awaitTermination(TASK_TIMEOUT_SECONDS, TimeUnit.SECONDS);
        Assert.assertFalse(finishedExecution[0]); // Still at a breakpoint.
        Assert.assertTrue(finishedExecution[1]);
        Assert.assertEquals(DebugState.AT_BREAKPOINT, debugContext.getDebugState());
        Assert.assertNotNull(debugContext.getCurrentLine());
        Assert.assertArrayEquals(stepOverBreakpoint.toArray(), debugContext.getCurrentLine().toArray());

        debugManager.stopDebug(debugContext.getTaskId());
        Assert.assertEquals(DebugState.STOPPED, debugContext.getDebugState());
    }

    @Test
    public void testStepOverNestedAtLeastOne() throws Exception {
        // Test Policy
        //
        // 2. Set Context Variable
        // 3. Set Context Variable
        // 4. Set Context Variable
        // 5. At least one assertion
        // 6.   All assertion
        // 7.     Set Context Variable
        // 8.     Set Context Variable
        // 9.   All assertion
        // 10.    Set Context Variable
        // 11.    Set Context Variable
        // 12. Set Context Variable

        //
        // Add a breakpoint at line 6.
        //
        final List<Integer> breakpoint = new ArrayList<>(1);
        breakpoint.add(6);
        debugManager.toggleBreakpoint(debugContext.getTaskId(), breakpoint);

        //
        // Set stepover breakpoint at line 9. Which won't be hit when stepping over
        // at line 6.
        //
        final List<Integer> stepOverBreakpoint = new ArrayList<>(1);
        stepOverBreakpoint.add(9);

        Option<String> option = debugManager.startDebug(debugContext.getTaskId());
        Assert.assertFalse(option.isSome());
        Assert.assertEquals(DebugState.STARTED, debugContext.getDebugState());

        final boolean[] finishedExecution = new boolean[2];

        // Message Processor.
        //
        executor.submit(new Callable<Void>() {
            @Override
            public Void call() throws Exception {
                messageProcessor.processMessage(pec);
                finishedExecution[0] = true;
                return null;
            }
        });

        // The "client".
        //
        executor.submit(new Callable<Void>() {
            @Override
            public Void call() throws Exception {
                while (true) {
                    boolean updated = debugContext.waitForUpdates(MAX_WAIT_TIME_MILLI_SECONDS);
                    if (updated) {
                        DebugState state = debugContext.getDebugState();
                        if (state.equals(DebugState.AT_BREAKPOINT)) {
                            debugManager.stepOver(debugContext.getTaskId(), stepOverBreakpoint);
                            break;
                        }
                    }
                }
                finishedExecution[1] = true;
                return null;
            }
        });

        executor.awaitTermination(TASK_TIMEOUT_SECONDS, TimeUnit.SECONDS);
        Assert.assertFalse(finishedExecution[0]); // Still at a breakpoint.
        Assert.assertTrue(finishedExecution[1]);
        Assert.assertEquals(DebugState.AT_BREAKPOINT, debugContext.getDebugState());
        Assert.assertNotNull(debugContext.getCurrentLine());
        Assert.assertArrayEquals(new Integer[]{12}, debugContext.getCurrentLine().toArray());

        debugManager.stopDebug(debugContext.getTaskId());
        Assert.assertEquals(DebugState.STOPPED, debugContext.getDebugState());
    }

    @Test
    public void testStepInto() throws Exception {
        // Add a breakpoint so that the debugger will hit the breakpoint.
        //
        final List<Integer> breakpoint = new ArrayList<>(1);
        breakpoint.add(2);
        debugManager.toggleBreakpoint(debugContext.getTaskId(), breakpoint);

        final List<Integer> expectedStepIntoLine = new ArrayList<>(1);
        expectedStepIntoLine.add(3);

        Option<String> option = debugManager.startDebug(debugContext.getTaskId());
        Assert.assertFalse(option.isSome());
        Assert.assertEquals(DebugState.STARTED, debugContext.getDebugState());

        final boolean[] finishedExecution = new boolean[2];

        // Message Processor.
        //
        executor.submit(new Callable<Void>() {
            @Override
            public Void call() throws Exception {
                messageProcessor.processMessage(pec);
                finishedExecution[0] = true;
                return null;
            }
        });

        // The "client".
        //
        executor.submit(new Callable<Void>() {
            @Override
            public Void call() throws Exception {
                while (true) {
                    boolean updated = debugContext.waitForUpdates(MAX_WAIT_TIME_MILLI_SECONDS);
                    if (updated) {
                        DebugState state = debugContext.getDebugState();
                        if (state.equals(DebugState.AT_BREAKPOINT)) {
                            debugManager.stepInto(debugContext.getTaskId());
                            break;
                        }
                    }
                }
                finishedExecution[1] = true;
                return null;
            }
        });

        executor.awaitTermination(TASK_TIMEOUT_SECONDS, TimeUnit.SECONDS);
        Assert.assertFalse(finishedExecution[0]); // Still at a breakpoint.
        Assert.assertTrue(finishedExecution[1]);
        Assert.assertEquals(DebugState.AT_BREAKPOINT, debugContext.getDebugState());
        Assert.assertNotNull(debugContext.getCurrentLine());
        Assert.assertArrayEquals(expectedStepIntoLine.toArray(), debugContext.getCurrentLine().toArray());

        debugManager.stopDebug(debugContext.getTaskId());
        Assert.assertEquals(DebugState.STOPPED, debugContext.getDebugState());
    }

    @Test
    public void testStepOut() throws Exception {
        // Add a breakpoint so that the debugger will hit the breakpoint.
        //
        final List<Integer> breakpoint = new ArrayList<>(1);
        breakpoint.add(2);
        debugManager.toggleBreakpoint(debugContext.getTaskId(), breakpoint);

        final List<Integer> stepOutBreakpoint = new ArrayList<>(1);
        stepOutBreakpoint.add(3);

        Option<String> option = debugManager.startDebug(debugContext.getTaskId());
        Assert.assertFalse(option.isSome());
        Assert.assertEquals(DebugState.STARTED, debugContext.getDebugState());

        final boolean[] finishedExecution = new boolean[2];

        // Message Processor.
        //
        executor.submit(new Callable<Void>() {
            @Override
            public Void call() throws Exception {
                messageProcessor.processMessage(pec);
                finishedExecution[0] = true;
                return null;
            }
        });

        // The "client".
        //
        executor.submit(new Callable<Void>() {
            @Override
            public Void call() throws Exception {
                while (true) {
                    boolean updated = debugContext.waitForUpdates(MAX_WAIT_TIME_MILLI_SECONDS);
                    if (updated) {
                        DebugState state = debugContext.getDebugState();
                        if (state.equals(DebugState.AT_BREAKPOINT)) {
                            debugManager.stepOut(debugContext.getTaskId(), stepOutBreakpoint);
                            break;
                        }
                    }
                }
                finishedExecution[1] = true;
                return null;
            }
        });

        executor.awaitTermination(TASK_TIMEOUT_SECONDS, TimeUnit.SECONDS);
        Assert.assertFalse(finishedExecution[0]); // Still at a breakpoint.
        Assert.assertTrue(finishedExecution[1]);
        Assert.assertEquals(DebugState.AT_BREAKPOINT, debugContext.getDebugState());
        Assert.assertNotNull(debugContext.getCurrentLine());
        Assert.assertArrayEquals(stepOutBreakpoint.toArray(), debugContext.getCurrentLine().toArray());

        debugManager.stopDebug(debugContext.getTaskId());
        Assert.assertEquals(DebugState.STOPPED, debugContext.getDebugState());
    }

    @Test
    public void testStepOutNestedAtLeastOne() throws Exception {
        // Test Policy
        //
        // 2. Set Context Variable
        // 3. Set Context Variable
        // 4. Set Context Variable
        // 5. At least one assertion
        // 6.   All assertion
        // 7.     Set Context Variable
        // 8.     Set Context Variable
        // 9.   All assertion
        // 10.    Set Context Variable
        // 11.    Set Context Variable
        // 12. Set Context Variable

        //
        // Add a breakpoint at line 7.
        //
        final List<Integer> breakpoint = new ArrayList<>(1);
        breakpoint.add(7);
        debugManager.toggleBreakpoint(debugContext.getTaskId(), breakpoint);

        //
        // Set stepout breakpoint at line 9. Which won't be hit when stepping out
        // at line 7.
        //
        final List<Integer> stepOutBreakpoint = new ArrayList<>(1);
        stepOutBreakpoint.add(9);

        Option<String> option = debugManager.startDebug(debugContext.getTaskId());
        Assert.assertFalse(option.isSome());
        Assert.assertEquals(DebugState.STARTED, debugContext.getDebugState());

        final boolean[] finishedExecution = new boolean[2];

        // Message Processor.
        //
        executor.submit(new Callable<Void>() {
            @Override
            public Void call() throws Exception {
                messageProcessor.processMessage(pec);
                finishedExecution[0] = true;
                return null;
            }
        });

        // The "client".
        //
        executor.submit(new Callable<Void>() {
            @Override
            public Void call() throws Exception {
                while (true) {
                    boolean updated = debugContext.waitForUpdates(MAX_WAIT_TIME_MILLI_SECONDS);
                    if (updated) {
                        DebugState state = debugContext.getDebugState();
                        if (state.equals(DebugState.AT_BREAKPOINT)) {
                            debugManager.stepOut(debugContext.getTaskId(), stepOutBreakpoint);
                            break;
                        }
                    }
                }
                finishedExecution[1] = true;
                return null;
            }
        });

        executor.awaitTermination(TASK_TIMEOUT_SECONDS, TimeUnit.SECONDS);
        Assert.assertFalse(finishedExecution[0]); // Still at a breakpoint.
        Assert.assertTrue(finishedExecution[1]);
        Assert.assertEquals(DebugState.AT_BREAKPOINT, debugContext.getDebugState());
        Assert.assertNotNull(debugContext.getCurrentLine());
        Assert.assertArrayEquals(new Integer[]{12}, debugContext.getCurrentLine().toArray());

        debugManager.stopDebug(debugContext.getTaskId());
        Assert.assertEquals(DebugState.STOPPED, debugContext.getDebugState());
    }

    @Test
    public void testResumeNoMoreBreakpoints() throws Exception {
        // Add a breakpoint so that the debugger will hit the breakpoint.
        //
        final List<Integer> breakpoint = new ArrayList<>(1);
        breakpoint.add(2);
        debugManager.toggleBreakpoint(debugContext.getTaskId(), breakpoint);

        Option<String> option = debugManager.startDebug(debugContext.getTaskId());
        Assert.assertFalse(option.isSome());
        Assert.assertEquals(DebugState.STARTED, debugContext.getDebugState());

        final boolean[] finishedExecution = new boolean[2];

        // Message Processor.
        //
        executor.submit(new Callable<Void>() {
            @Override
            public Void call() throws Exception {
                messageProcessor.processMessage(pec);
                finishedExecution[0] = true;
                return null;
            }
        });

        // The "client".
        //
        executor.submit(new Callable<Void>() {
            @Override
            public Void call() throws Exception {
                while (true) {
                    boolean updated = debugContext.waitForUpdates(MAX_WAIT_TIME_MILLI_SECONDS);
                    if (updated) {
                        DebugState state = debugContext.getDebugState();
                        if (state.equals(DebugState.AT_BREAKPOINT)) {
                            debugManager.resume(debugContext.getTaskId());
                            break;
                        }
                    }
                }
                finishedExecution[1] = true;
                return null;
            }
        });

        executor.awaitTermination(TASK_TIMEOUT_SECONDS, TimeUnit.SECONDS);
        Assert.assertTrue(finishedExecution[0]);
        Assert.assertTrue(finishedExecution[1]);
        Assert.assertEquals(DebugState.STOPPED, debugContext.getDebugState());
    }

    @Test
    public void testResumeWithMoreBreakpoints() throws Exception {
        // Add 2 breakpoints so that the debugger will hit the breakpoint.
        //
        final List<Integer> breakpoint = new ArrayList<>(1);
        breakpoint.add(2);
        debugManager.toggleBreakpoint(debugContext.getTaskId(), breakpoint);

        final List<Integer> secondBreakpoint = new ArrayList<>(1);
        secondBreakpoint.add(3);
        debugManager.toggleBreakpoint(debugContext.getTaskId(), secondBreakpoint);

        Option<String> option = debugManager.startDebug(debugContext.getTaskId());
        Assert.assertFalse(option.isSome());
        Assert.assertEquals(DebugState.STARTED, debugContext.getDebugState());

        final boolean[] finishedExecution = new boolean[2];

        // Message Processor.
        //
        executor.submit(new Callable<Void>() {
            @Override
            public Void call() throws Exception {
                messageProcessor.processMessage(pec);
                finishedExecution[0] = true;
                return null;
            }
        });

        // The "client".
        //
        executor.submit(new Callable<Void>() {
            @Override
            public Void call() throws Exception {
                while (true) {
                    boolean updated = debugContext.waitForUpdates(MAX_WAIT_TIME_MILLI_SECONDS);
                    if (updated) {
                        DebugState state = debugContext.getDebugState();
                        if (state.equals(DebugState.AT_BREAKPOINT)) {
                            debugManager.resume(debugContext.getTaskId());
                            break;
                        }
                    }
                }
                finishedExecution[1] = true;
                return null;
            }
        });

        executor.awaitTermination(TASK_TIMEOUT_SECONDS, TimeUnit.SECONDS);
        Assert.assertFalse(finishedExecution[0]); // Still at a breakpoint.
        Assert.assertTrue(finishedExecution[1]);
        Assert.assertEquals(DebugState.AT_BREAKPOINT, debugContext.getDebugState());
        Assert.assertNotNull(debugContext.getCurrentLine());
        Assert.assertArrayEquals(secondBreakpoint.toArray(), debugContext.getCurrentLine().toArray());

        debugManager.stopDebug(debugContext.getTaskId());
        Assert.assertEquals(DebugState.STOPPED, debugContext.getDebugState());
    }

    @Test
    public void testTerminateStoppedDebugger() throws Exception {
        debugManager.terminateDebug(debugContext.getTaskId());
        Assert.assertTrue(debugContext.isTerminated());
        debugContext = debugManager.getDebugContext(debugContext.getTaskId());
        Assert.assertNull(debugContext);
    }

    @Test
    public void testTerminateStartedDebugger() throws Exception {
        Option<String> option = debugManager.startDebug(debugContext.getTaskId());
        Assert.assertFalse(option.isSome());
        Assert.assertEquals(DebugState.STARTED, debugContext.getDebugState());

        debugManager.terminateDebug(debugContext.getTaskId());
        Assert.assertTrue(debugContext.isTerminated());
        debugContext = debugManager.getDebugContext(debugContext.getTaskId());
        Assert.assertNull(debugContext);
    }

    @Test
    public void testTerminateAtBreakpointDebugger() throws Exception {
        // Add 2 breakpoints so that the debugger will hit the breakpoint.
        // On terminate, it should not stop at the second breakpoint.
        //
        final List<Integer> breakpoint = new ArrayList<>(1);
        breakpoint.add(2);
        debugManager.toggleBreakpoint(debugContext.getTaskId(), breakpoint);

        final List<Integer> secondBreakpoint = new ArrayList<>(1);
        secondBreakpoint.add(3);
        debugManager.toggleBreakpoint(debugContext.getTaskId(), secondBreakpoint);

        Option<String> option = debugManager.startDebug(debugContext.getTaskId());
        Assert.assertFalse(option.isSome());
        Assert.assertEquals(DebugState.STARTED, debugContext.getDebugState());

        final boolean[] finishedExecution = new boolean[2];

        // Message Processor.
        //
        executor.submit(new Callable<Void>() {
            @Override
            public Void call() throws Exception {
                messageProcessor.processMessage(pec);
                finishedExecution[0] = true;
                return null;
            }
        });

        // The "client".
        //
        executor.submit(new Callable<Void>() {
            @Override
            public Void call() throws Exception {
                while (true) {
                    boolean updated = debugContext.waitForUpdates(MAX_WAIT_TIME_MILLI_SECONDS);
                    if (updated) {
                        DebugState state = debugContext.getDebugState();
                        if (state.equals(DebugState.AT_BREAKPOINT)) {
                            break;
                        }
                    }
                }
                finishedExecution[1] = true;
                return null;
            }
        });

        executor.awaitTermination(TASK_TIMEOUT_SECONDS, TimeUnit.SECONDS);
        Assert.assertFalse(finishedExecution[0]); // Still at a breakpoint.
        Assert.assertTrue(finishedExecution[1]);
        Assert.assertEquals(DebugState.AT_BREAKPOINT, debugContext.getDebugState());
        Assert.assertNotNull(debugContext.getCurrentLine());
        Assert.assertArrayEquals(breakpoint.toArray(), debugContext.getCurrentLine().toArray());

        debugManager.terminateDebug(debugContext.getTaskId());
        Assert.assertTrue(debugContext.isTerminated());
        debugContext = debugManager.getDebugContext(debugContext.getTaskId());
        Assert.assertNull(debugContext);
    }

    @Test
    public void testToggleBreakpoint() throws Exception {
        // Toggle a breakpoint 2X. Once to add, second time to remove.
        //
        final List<Integer> breakpoint = new ArrayList<>(1);
        breakpoint.add(2);
        debugManager.toggleBreakpoint(debugContext.getTaskId(), breakpoint);
        debugManager.toggleBreakpoint(debugContext.getTaskId(), breakpoint);

        Option<String> option = debugManager.startDebug(debugContext.getTaskId());
        Assert.assertFalse(option.isSome());
        Assert.assertEquals(DebugState.STARTED, debugContext.getDebugState());

        final boolean[] finishedExecution = new boolean[1];

        // Message Processor.
        //
        executor.submit(new Callable<Void>() {
            @Override
            public Void call() throws Exception {
                messageProcessor.processMessage(pec);
                finishedExecution[0] = true;
                return null;
            }
        });

        executor.awaitTermination(TASK_TIMEOUT_SECONDS, TimeUnit.SECONDS);
        Assert.assertTrue(finishedExecution[0]);
        Assert.assertEquals(DebugState.STOPPED, debugContext.getDebugState());
    }

    @Test
    public void testRemoveAllBreakpoints() throws Exception {
        // Add 2 breakpoints, then remove all.
        //
        final List<Integer> breakpoint = new ArrayList<>(1);
        breakpoint.add(2);
        debugManager.toggleBreakpoint(debugContext.getTaskId(), breakpoint);

        final List<Integer> secondBreakpoint = new ArrayList<>(1);
        secondBreakpoint.add(3);
        debugManager.toggleBreakpoint(debugContext.getTaskId(), secondBreakpoint);

        debugManager.removeAllBreakpoints(debugContext.getTaskId());

        Option<String> option = debugManager.startDebug(debugContext.getTaskId());
        Assert.assertFalse(option.isSome());
        Assert.assertEquals(DebugState.STARTED, debugContext.getDebugState());

        final boolean[] finishedExecution = new boolean[1];

        // Message Processor.
        //
        executor.submit(new Callable<Void>() {
            @Override
            public Void call() throws Exception {
                messageProcessor.processMessage(pec);
                finishedExecution[0] = true;
                return null;
            }
        });

        executor.awaitTermination(TASK_TIMEOUT_SECONDS, TimeUnit.SECONDS);
        Assert.assertTrue(finishedExecution[0]);
        Assert.assertEquals(DebugState.STOPPED, debugContext.getDebugState());
    }

    private static PublishedService createPublishedService() {
        PublishedService publishedService = new PublishedService();
        publishedService.setGoid(PUBLISHED_SERVICE_GOID);
        publishedService.setRoutingUri(SERVICE_ROUTING_URI);
        publishedService.getPolicy().setGoid(POLICY_GOID);
        publishedService.getPolicy().setXml(POLICY_XML);
        publishedService.setLaxResolution(true);
        publishedService.setSoap(false);
        return publishedService;
    }

    private static Message createRequest() {
        Message request = new Message();
        MockHttpServletRequest hRequest = new MockHttpServletRequest(HttpMethod.POST.name(), SERVICE_ROUTING_URI);
        request.attachHttpRequestKnob(new HttpServletRequestKnob(hRequest));
        return request;
    }

    private static Message createResponse() {
        Message response = new Message();
        MockHttpServletResponse hResponse = new MockHttpServletResponse();
        response.attachHttpResponseKnob(new HttpServletResponseKnob(hResponse));
        return response;
    }
}