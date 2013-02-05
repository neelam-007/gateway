package com.l7tech.server.message;

import com.l7tech.message.Message;
import com.l7tech.policy.assertion.MessageTargetableSupport;
import com.l7tech.policy.assertion.TargetMessageType;
import com.l7tech.policy.variable.NoSuchVariableException;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;

public class ChildPolicyEnforcementContextTest {
    private static final String NAME = "varName";
    private static final String NAME_LOWER = NAME.toLowerCase();
    private static final String VALUE = "testValue";
    private ChildPolicyEnforcementContext child;
    private PolicyEnforcementContext parent;
    private Message request;
    private Message response;

    @Before
    public void setup() {
        request = new Message();
        response = new Message();
        parent = PolicyEnforcementContextFactory.createPolicyEnforcementContext(request, response);
        child = (ChildPolicyEnforcementContext) PolicyEnforcementContextFactory.createPolicyEnforcementContext(parent);
    }

    @Test
    public void putParentVariableNotPrefixed() throws Exception {
        parent.setVariable(NAME, VALUE);
        child.putParentVariable(NAME, false);
        assertEquals(VALUE, child.getVariable(NAME));
    }

    @Test
    public void putParentVariablesPrefixed() throws Exception {
        parent.setVariable(NAME + ".test", VALUE);
        child.putParentVariable(NAME, true);
        assertEquals(VALUE, child.getVariable(NAME + ".test"));
    }

    @Test
    public void getOrCreateTargetMessageRequest() throws Exception {
        assertEquals(request, child.getOrCreateTargetMessage(new MessageTargetableSupport(TargetMessageType.REQUEST), false));
    }

    @Test
    public void getOrCreateTargetMessageResponse() throws Exception {
        assertEquals(response, child.getOrCreateTargetMessage(new MessageTargetableSupport(TargetMessageType.RESPONSE), false));
    }

    @Test(expected= NoSuchVariableException.class)
    public void getOrCreateTargetMessageOtherDoesNotExist() throws Exception {
        child.getOrCreateTargetMessage(new MessageTargetableSupport(TargetMessageType.OTHER), false);
    }

    /**
     * If msg already exists on the parent context, should retrieve this msg.
     */
    @Test
    public void getOrCreateTargetMessageOtherParentVariable() throws Exception {
        final Message parentMsg = new Message();
        parent.setVariable(NAME, parentMsg);
        child.putParentVariable(NAME, true);
        final Message childMsg = child.getOrCreateTargetMessage(new MessageTargetableSupport(NAME), false);
        assertEquals(parentMsg, childMsg);
    }

    @Test
    public void getOrCreateTargetMessageOtherNotParentVariable() throws Exception {
        final Message parentMsg = new Message();
        parent.setVariable(NAME, parentMsg);
        // do not set parent variable on child context
        final Message childMsg = child.getOrCreateTargetMessage(new MessageTargetableSupport(NAME), false);
        assertNotSame(parentMsg, childMsg);
    }
}
