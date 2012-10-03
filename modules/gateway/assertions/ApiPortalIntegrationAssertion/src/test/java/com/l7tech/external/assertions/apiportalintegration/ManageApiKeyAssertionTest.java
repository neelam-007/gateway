package com.l7tech.external.assertions.apiportalintegration;

import com.l7tech.policy.variable.VariableMetadata;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

public class ManageApiKeyAssertionTest {
    private ManageApiKeyAssertion assertion;

    @Before
    public void setup() {
        assertion = new ManageApiKeyAssertion();
    }

    @Test
    public void getVarsSetAdd() {
        assertion.setAction(ManageApiKeyAssertion.ACTION_ADD);

        final VariableMetadata[] variablesSet = assertion.getVariablesSet();
        assertEquals(1, variablesSet.length);
        assertEquals("apikey.key", variablesSet[0].getName());
    }

    @Test
    public void getVarsSetUpdate() {
        assertion.setAction(ManageApiKeyAssertion.ACTION_UPDATE);

        assertEquals(0, assertion.getVariablesSet().length);
    }

    @Test
    public void getVarsSetRemove() {
        assertion.setAction(ManageApiKeyAssertion.ACTION_REMOVE);

        assertEquals(0, assertion.getVariablesSet().length);
    }

    @Test
    public void getVarsSetNullAction() {
        assertion.setAction(null);

        assertEquals(0, assertion.getVariablesSet().length);
    }
}
