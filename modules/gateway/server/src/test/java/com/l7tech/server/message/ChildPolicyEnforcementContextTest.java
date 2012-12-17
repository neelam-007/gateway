package com.l7tech.server.message;

import com.l7tech.message.Message;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;

public class ChildPolicyEnforcementContextTest {
    private static final String NAME = "varName";
    private static final String NAME_LOWER = NAME.toLowerCase();
    private static final String VALUE = "testValue";
    private ChildPolicyEnforcementContext child;
    private PolicyEnforcementContext parent;

    @Before
    public void setup() {
        parent = PolicyEnforcementContextFactory.createPolicyEnforcementContext(new Message(), new Message());
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
}
