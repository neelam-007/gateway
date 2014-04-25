package com.l7tech.server.stepdebug;

import com.l7tech.common.mime.ContentTypeHeader;
import com.l7tech.common.mime.StashManager;
import com.l7tech.gateway.common.audit.Audit;
import com.l7tech.gateway.common.audit.TestAudit;
import com.l7tech.gateway.common.stepdebug.DebugContextVariableData;
import com.l7tech.gateway.common.stepdebug.DebugResult;
import com.l7tech.message.*;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.server.TestStashManagerFactory;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.server.message.PolicyEnforcementContextFactory;
import com.l7tech.test.BugId;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.io.ByteArrayInputStream;
import java.util.*;

@SuppressWarnings("ConstantConditions")
public class DebugPecDataTest {
    private static final String MESSAGE_BODY = "<in>debugger_message</in>";
    private static final ContentTypeHeader MESSAGE_BODY_CONTENT_TYPE = ContentTypeHeader.XML_DEFAULT;
    private static final String[] HEADER_NAMES = {"header1", "header2", "header3"};
    private static final String[] HEADER_VALUES = {"header1_value", "header2_value", "header3_value"};

    private final Audit audit = new TestAudit();
    private final StashManager sm = TestStashManagerFactory.getInstance().createStashManager();
    private DebugPecData debugPecData;

    @Before
    public void setup() throws Exception {
        debugPecData = new DebugPecData(audit);
    }

    @Test
    public void testHttpRequestMessage() throws Exception {
        Message request = new Message(sm, MESSAGE_BODY_CONTENT_TYPE, new ByteArrayInputStream(MESSAGE_BODY.getBytes("UTF-8")));
        MockHttpServletRequest hRequest = new MockHttpServletRequest("POST", "test_url");
        request.attachHttpRequestKnob(new HttpServletRequestKnob(hRequest));
        for (int ix = 0; ix < HEADER_NAMES.length; ix++) {
            request.getHeadersKnob().addHeader(HEADER_NAMES[ix], HEADER_VALUES[ix], HeadersKnob.HEADER_TYPE_HTTP);
        }
        PolicyEnforcementContext pec = PolicyEnforcementContextFactory.createPolicyEnforcementContext(request, null);
        debugPecData.update(pec, Collections.<String>emptySet());

        // Verify result.
        //
        String messageName = "request";
        boolean isHttp = true;
        boolean isUserAdded = false;
        this.checkMessageContextVariable(messageName, isHttp, isUserAdded);
    }

    @Test
    public void testHttpResponseMessage() throws Exception {
        Message response = new Message(sm, MESSAGE_BODY_CONTENT_TYPE, new ByteArrayInputStream(MESSAGE_BODY.getBytes("UTF-8")));
        MockHttpServletResponse hResponse = new MockHttpServletResponse();
        response.attachHttpResponseKnob(new HttpServletResponseKnob(hResponse));
        for (int ix = 0; ix < HEADER_NAMES.length; ix++) {
            response.getHeadersKnob().addHeader(HEADER_NAMES[ix], HEADER_VALUES[ix], HeadersKnob.HEADER_TYPE_HTTP);
        }
        PolicyEnforcementContext pec = PolicyEnforcementContextFactory.createPolicyEnforcementContext(null, response);
        debugPecData.update(pec, Collections.<String>emptySet());

        // Verify result.
        //
        String messageName = "response";
        boolean isHttp = true;
        boolean isUserAdded = false;
        this.checkMessageContextVariable(messageName, isHttp, isUserAdded);
    }

    @Test
    public void testNoneHttpRequestMessage() throws Exception {
        Message request = new Message(sm, MESSAGE_BODY_CONTENT_TYPE, new ByteArrayInputStream(MESSAGE_BODY.getBytes("UTF-8")));
        PolicyEnforcementContext pec = PolicyEnforcementContextFactory.createPolicyEnforcementContext(request, null);
        debugPecData.update(pec, Collections.<String>emptySet());

        // Verify result.
        //
        String messageName = "request";
        boolean isHttp = false;
        boolean isUserAdded = false;
        this.checkMessageContextVariable(messageName, isHttp, isUserAdded);
    }

    @Test
    public void testNoneHttpResponseMessage() throws Exception {
        Message response = new Message(sm, MESSAGE_BODY_CONTENT_TYPE, new ByteArrayInputStream(MESSAGE_BODY.getBytes("UTF-8")));
        PolicyEnforcementContext pec = PolicyEnforcementContextFactory.createPolicyEnforcementContext(null, response);
        debugPecData.update(pec, Collections.<String>emptySet());

        // Verify result.
        //
        String messageName = "response";
        boolean isHttp = false;
        boolean isUserAdded = false;
        this.checkMessageContextVariable(messageName, isHttp, isUserAdded);
    }

    @Test
    public void testUserContextVariableMessageType() throws Exception {
        // Setup test data.
        //
        Message request = new Message(sm, MESSAGE_BODY_CONTENT_TYPE, new ByteArrayInputStream(MESSAGE_BODY.getBytes("UTF-8")));
        MockHttpServletRequest hRequest = new MockHttpServletRequest("POST", "test_url");
        request.attachHttpRequestKnob(new HttpServletRequestKnob(hRequest));
        for (int ix = 0; ix < HEADER_NAMES.length; ix++) {
            request.getHeadersKnob().addHeader(HEADER_NAMES[ix], HEADER_VALUES[ix], HeadersKnob.HEADER_TYPE_HTTP);
        }
        PolicyEnforcementContext pec = PolicyEnforcementContextFactory.createPolicyEnforcementContext(request, null);

        // Add user context variable
        //
        String cxt_name = "request";
        debugPecData.addUserContextVariable(cxt_name);
        Set<String> userContextVariables = new TreeSet<>();
        userContextVariables.add(cxt_name);
        debugPecData.update(pec, userContextVariables);

        // Verify result.
        //
        Set<DebugContextVariableData> vars = debugPecData.getContextVariables();
        Assert.assertNotNull(vars);

        boolean isUserAddedFound = false;
        for (DebugContextVariableData var : vars) {
            Assert.assertNotNull(var);
            if (var.getIsUserAdded()) {
                boolean isHttp = true;
                boolean isUserAdded = true;
                this.checkSingleMessageContextVariable(var, cxt_name, isHttp, isUserAdded);
                isUserAddedFound = true;
                break;
            }
        }
        Assert.assertTrue(isUserAddedFound);
    }

    @Test
    public void testUserContextVariableStringType() throws Exception {
        // Setup test data.
        //
        Message request = new Message(sm, MESSAGE_BODY_CONTENT_TYPE, new ByteArrayInputStream(MESSAGE_BODY.getBytes("UTF-8")));
        MockHttpServletRequest hRequest = new MockHttpServletRequest("POST", "test_url");
        request.attachHttpRequestKnob(new HttpServletRequestKnob(hRequest));
        for (int ix = 0; ix < HEADER_NAMES.length; ix++) {
            request.getHeadersKnob().addHeader(HEADER_NAMES[ix], HEADER_VALUES[ix], HeadersKnob.HEADER_TYPE_HTTP);
        }
        PolicyEnforcementContext pec = PolicyEnforcementContextFactory.createPolicyEnforcementContext(request, null);

        // Add user context variable
        //
        String cxt_name = "request.http.method";
        debugPecData.addUserContextVariable(cxt_name);
        Set<String> userContextVariables = new TreeSet<>();
        userContextVariables.add(cxt_name);
        debugPecData.update(pec, userContextVariables);

        // Verify result.
        //
        Set<DebugContextVariableData> vars = debugPecData.getContextVariables();
        Assert.assertNotNull(vars);

        boolean isUserAddedFound = false;
        for (DebugContextVariableData var : vars) {
            Assert.assertNotNull(var);
            if (var.getIsUserAdded()) {
                Assert.assertEquals(cxt_name, var.getName());
                Assert.assertTrue(var.toString().contains("POST"));
                isUserAddedFound = true;
                break;
            }
        }
        Assert.assertTrue(isUserAddedFound);
    }

    @Test
    public void testUserContextVariableMultiValueType() throws Exception {
        // Setup test data.
        //
        Message request = new Message(sm, MESSAGE_BODY_CONTENT_TYPE, new ByteArrayInputStream(MESSAGE_BODY.getBytes("UTF-8")));
        MockHttpServletRequest hRequest = new MockHttpServletRequest("POST", "test_url");
        request.attachHttpRequestKnob(new HttpServletRequestKnob(hRequest));
        for (int ix = 0; ix < HEADER_NAMES.length; ix++) {
            request.getHeadersKnob().addHeader(HEADER_NAMES[ix], HEADER_VALUES[ix], HeadersKnob.HEADER_TYPE_HTTP);
        }
        PolicyEnforcementContext pec = PolicyEnforcementContextFactory.createPolicyEnforcementContext(request, null);

        // Add user context variable
        //
        String cxt_name = "request.http.allheadervalues";
        debugPecData.addUserContextVariable(cxt_name);
        Set<String> userContextVariables = new TreeSet<>();
        userContextVariables.add(cxt_name);
        debugPecData.update(pec, userContextVariables);

        // Verify result.
        //
        Set<DebugContextVariableData> vars = debugPecData.getContextVariables();
        Assert.assertNotNull(vars);

        boolean isUserAddedFound = false;
        for (DebugContextVariableData var : vars) {
            Assert.assertNotNull(var);
            if (var.getIsUserAdded()) {
                Assert.assertTrue(cxt_name.equals(var.getName()));
                Assert.assertNotNull(var.getChildren());
                Assert.assertEquals(HEADER_NAMES.length, var.getChildren().size());
                isUserAddedFound = true;
                break;
            }
        }
        Assert.assertTrue(isUserAddedFound);
    }

    @Test
    public void testUserContextVariableMultiValueTypeIndexed() throws Exception {
        // Setup test data.
        //
        Message request = new Message(sm, MESSAGE_BODY_CONTENT_TYPE, new ByteArrayInputStream(MESSAGE_BODY.getBytes("UTF-8")));
        MockHttpServletRequest hRequest = new MockHttpServletRequest("POST", "test_url");
        request.attachHttpRequestKnob(new HttpServletRequestKnob(hRequest));
        for (int ix = 0; ix < HEADER_NAMES.length; ix++) {
            request.getHeadersKnob().addHeader(HEADER_NAMES[ix], HEADER_VALUES[ix], HeadersKnob.HEADER_TYPE_HTTP);
        }
        PolicyEnforcementContext pec = PolicyEnforcementContextFactory.createPolicyEnforcementContext(request, null);

        // Add user context variable
        //
        String cxt_name = "request.http.allheadervalues[0]";
        debugPecData.addUserContextVariable(cxt_name);
        Set<String> userContextVariables = new TreeSet<>();
        userContextVariables.add(cxt_name);
        debugPecData.update(pec, userContextVariables);

        // Verify result.
        //
        Set<DebugContextVariableData> vars = debugPecData.getContextVariables();
        Assert.assertNotNull(vars);

        boolean isUserAddedFound = false;
        for (DebugContextVariableData var : vars) {
            Assert.assertNotNull(var);
            if (var.getIsUserAdded()) {
                Assert.assertTrue(cxt_name.equals(var.getName()));
                Assert.assertNotNull(var.getChildren());
                Assert.assertEquals(0, var.getChildren().size());
                Assert.assertTrue(var.toString().contains(HEADER_NAMES[0]));
                Assert.assertTrue(var.toString().contains(HEADER_VALUES[0]));
                isUserAddedFound = true;
                break;
            }
        }
        Assert.assertTrue(isUserAddedFound);
    }

    @Test
    public void testRemoveUserContextVariable() throws Exception {
        // Setup test data.
        //
        Message request = new Message(sm, MESSAGE_BODY_CONTENT_TYPE, new ByteArrayInputStream(MESSAGE_BODY.getBytes("UTF-8")));
        MockHttpServletRequest hRequest = new MockHttpServletRequest("POST", "test_url");
        request.attachHttpRequestKnob( new HttpServletRequestKnob(hRequest));
        for (int ix = 0; ix < HEADER_NAMES.length; ix++) {
            request.getHeadersKnob().addHeader(HEADER_NAMES[ix], HEADER_VALUES[ix], HeadersKnob.HEADER_TYPE_HTTP);
        }
        PolicyEnforcementContext pec = PolicyEnforcementContextFactory.createPolicyEnforcementContext(request, null);

        // Add user context variable
        //
        String cxt_name = "request.http.method";
        debugPecData.addUserContextVariable(cxt_name);
        Set<String> userContextVariables = new TreeSet<>();
        userContextVariables.add(cxt_name);
        debugPecData.update(pec, userContextVariables);

        // Verify result.
        //
        Set<DebugContextVariableData> vars = debugPecData.getContextVariables();
        Assert.assertNotNull(vars);

        boolean isUserAddedFound = false;
        for (DebugContextVariableData var : vars) {
            Assert.assertNotNull(var);
            if (var.getIsUserAdded()) {
                Assert.assertEquals(cxt_name, var.getName());
                Assert.assertTrue(var.toString().contains("POST"));
                isUserAddedFound = true;
                break;
            }
        }
        Assert.assertTrue(isUserAddedFound);

        // Remove user context variable
        //
        debugPecData.removeUserContextVariable(cxt_name);

        // Verify result.
        //
        vars = debugPecData.getContextVariables();
        Assert.assertNotNull(vars);

        isUserAddedFound = false;
        for (DebugContextVariableData var : vars) {
            Assert.assertNotNull(var);
            if (var.getIsUserAdded()) {
                isUserAddedFound = true;
                break;
            }
        }
        Assert.assertFalse(isUserAddedFound);
    }


    @BugId("SSM-4578")
    @Test
    public void testPolicyResultSuccessful() throws Exception {
        // Setup test data.
        //
        Message request = new Message(sm, MESSAGE_BODY_CONTENT_TYPE, new ByteArrayInputStream(MESSAGE_BODY.getBytes("UTF-8")));
        MockHttpServletRequest hRequest = new MockHttpServletRequest("POST", "test_url");
        request.attachHttpRequestKnob( new HttpServletRequestKnob(hRequest));
        PolicyEnforcementContext pec = PolicyEnforcementContextFactory.createPolicyEnforcementContext(request, null);
        pec.setPolicyResult(AssertionStatus.NONE);

        List<Integer> currentLine = new ArrayList<>(1);
        currentLine.add(2);
        debugPecData.setPolicyResult(pec, currentLine);

        // Verify policy result.
        //
        Assert.assertNotNull(debugPecData.getPolicyResult());
        Assert.assertEquals(DebugResult.SUCCESSFUL_POLICY_RESULT_MESSAGE, debugPecData.getPolicyResult());
    }

    @BugId("SSM-4578")
    @Test
    public void testPolicyResultUnsuccessful() throws Exception {
        // Setup test data.
        //
        Message request = new Message(sm, MESSAGE_BODY_CONTENT_TYPE, new ByteArrayInputStream(MESSAGE_BODY.getBytes("UTF-8")));
        MockHttpServletRequest hRequest = new MockHttpServletRequest("POST", "test_url");
        request.attachHttpRequestKnob( new HttpServletRequestKnob(hRequest));
        PolicyEnforcementContext pec = PolicyEnforcementContextFactory.createPolicyEnforcementContext(request, null);
        pec.setPolicyResult(AssertionStatus.AUTH_REQUIRED);

        List<Integer> currentLine = new ArrayList<>(1);
        currentLine.add(2);
        debugPecData.setPolicyResult(pec, currentLine);

        // Verify policy result.
        //
        Assert.assertNotNull(debugPecData.getPolicyResult());
        Assert.assertNotSame(DebugResult.SUCCESSFUL_POLICY_RESULT_MESSAGE, debugPecData.getPolicyResult());
        Assert.assertTrue(debugPecData.getPolicyResult().contains(AssertionStatus.AUTH_REQUIRED.getMessage()));
        Assert.assertTrue(debugPecData.getPolicyResult().contains("assertion number"));
    }

    @Test
    public void testReset() throws Exception {
        // Setup test data.
        //
        Message request = new Message(sm, MESSAGE_BODY_CONTENT_TYPE, new ByteArrayInputStream(MESSAGE_BODY.getBytes("UTF-8")));
        MockHttpServletRequest hRequest = new MockHttpServletRequest("POST", "test_url");
        request.attachHttpRequestKnob(new HttpServletRequestKnob(hRequest));
        for (int ix = 0; ix < HEADER_NAMES.length; ix++) {
            request.getHeadersKnob().addHeader(HEADER_NAMES[ix], HEADER_VALUES[ix], HeadersKnob.HEADER_TYPE_HTTP);
        }
        PolicyEnforcementContext pec = PolicyEnforcementContextFactory.createPolicyEnforcementContext(request, null);
        pec.setPolicyResult(AssertionStatus.NONE);

        // Add user context variable
        //
        String cxt_name = "request.http.method";
        debugPecData.addUserContextVariable(cxt_name);
        Set<String> userContextVariables = new TreeSet<>();
        userContextVariables.add(cxt_name);
        debugPecData.update(pec, userContextVariables);
        List<Integer> currentLine = new ArrayList<>(1);
        currentLine.add(2);
        debugPecData.setPolicyResult(pec, currentLine);

        // Verify result.
        //
        Set<DebugContextVariableData> vars = debugPecData.getContextVariables();
        Assert.assertNotNull(vars);
        Assert.assertEquals(2, vars.size()); // built-in "request" and user added "request.http.method".

        boolean isUserAddedFound = false;
        boolean isBuiltInFound = false;
        for (DebugContextVariableData var : vars) {
            Assert.assertNotNull(var);
            if (var.getIsUserAdded()) {
                Assert.assertEquals(cxt_name, var.getName());
                Assert.assertTrue(var.toString().contains("POST"));
                isUserAddedFound = true;
            } else {
                this.checkSingleMessageContextVariable(var, "request", true, false);
                isBuiltInFound = true;
            }
        }
        Assert.assertTrue(isBuiltInFound);
        Assert.assertTrue(isUserAddedFound);
        Assert.assertNotNull(debugPecData.getPolicyResult());

        // Reset user context variable
        //
        debugPecData.reset(userContextVariables);

        // Verify result.
        // Only user added context variables should remain.
        //
        vars = debugPecData.getContextVariables();
        Assert.assertNotNull(vars);
        Assert.assertEquals(1, vars.size()); // built-in "request" and user added "request.http.method".

        isUserAddedFound = false;
        isBuiltInFound = false;
        for (DebugContextVariableData var : vars) {
            Assert.assertNotNull(var);
            if (var.getIsUserAdded()) {
                Assert.assertEquals(cxt_name, var.getName());
                isUserAddedFound = true;
            } else {
                this.checkSingleMessageContextVariable(var, "request", true, false);
                isBuiltInFound = true;
            }
        }
        Assert.assertFalse(isBuiltInFound);
        Assert.assertTrue(isUserAddedFound);
        Assert.assertNull(debugPecData.getPolicyResult());
    }

    // Check that debugPecData contains 1 context variable of Message type.
    //
    private void checkMessageContextVariable(String messageName, boolean isHttp, boolean isUserAdded) {
        Set<DebugContextVariableData> vars = debugPecData.getContextVariables();
        Assert.assertNotNull(vars);
        Assert.assertEquals(1, vars.size());

        DebugContextVariableData var = vars.iterator().next();
        this.checkSingleMessageContextVariable(var, messageName, isHttp, isUserAdded);
    }

    // Check that the specified var is a valid Message type.
    //
    private void checkSingleMessageContextVariable(DebugContextVariableData var, String messageName, boolean isHttp, boolean isUserAdded) {
        Assert.assertNotNull(var);
        Assert.assertEquals(messageName, var.getName());
        Assert.assertEquals(isUserAdded, var.getIsUserAdded());

        Set<DebugContextVariableData> childVars = var.getChildren();
        if (isHttp) {
            Assert.assertEquals(3, childVars.size());
        } else {
            Assert.assertEquals(2, childVars.size());
        }

        boolean isMainPartFound = false;
        boolean isContentTypeFound = false;
        boolean isHttpAllHeaderValuesFound = false;

        for (DebugContextVariableData childVar : childVars) {
            Assert.assertEquals(isUserAdded, childVar.getIsUserAdded());
            if ("mainpart".equals(childVar.getName())) {
                Assert.assertTrue(childVar.toString().contains(MESSAGE_BODY));
                isMainPartFound = true;
            } else if ("contentType".equals(childVar.getName())) {
                Assert.assertTrue(childVar.toString().contains(MESSAGE_BODY_CONTENT_TYPE.getType()));
                Assert.assertTrue(childVar.toString().contains(MESSAGE_BODY_CONTENT_TYPE.getSubtype()));
                isContentTypeFound = true;
            } else if ("http.allheadervalues".equals(childVar.getName())) {
                Assert.assertNotNull(childVar.getChildren());
                Assert.assertEquals(HEADER_NAMES.length, childVar.getChildren().size());
                isHttpAllHeaderValuesFound = true;
            }
        }

        Assert.assertTrue(isMainPartFound);
        Assert.assertTrue(isContentTypeFound);
        if (isHttp) {
            Assert.assertTrue(isHttpAllHeaderValuesFound);
        } else {
            Assert.assertFalse(isHttpAllHeaderValuesFound);
        }
    }
}