package com.l7tech.server.policy.custom;

import com.l7tech.gateway.common.audit.Audit;
import com.l7tech.gateway.common.audit.TestAudit;
import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.assertion.CustomAssertionHolder;
import com.l7tech.policy.assertion.SetVariableAssertion;
import com.l7tech.policy.assertion.composite.AllAssertion;
import com.l7tech.policy.variable.DataType;
import com.l7tech.policy.variable.VariableMetadata;
import com.l7tech.server.policy.VariableServicesImpl;
import junit.framework.Assert;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;


public class VariableServicesTest {

    @Test
    public void testGetVariablesSetByPredecessors() throws Exception {
        // Setup test assertion.
        //
        SetVariableAssertion previousAssertion = new SetVariableAssertion();
        previousAssertion.setDataType(DataType.MESSAGE);
        previousAssertion.setVariableToSet("test_var_name");
        CustomAssertionHolder cah = new CustomAssertionHolder();

        List<Assertion> children = new ArrayList<>(2);
        children.add(previousAssertion);
        children.add(cah);

        @SuppressWarnings("UnusedDeclaration")
        AllAssertion allAssertion = new AllAssertion(children);

        Audit audit = new TestAudit();
        VariableServicesImpl variableServices = new VariableServicesImpl(audit, cah);

        Map<String, VariableMetadata> vars = variableServices.getVariablesSetByPredecessors();
        Assert.assertNotNull(vars);
        Assert.assertEquals(1, vars.size());
        VariableMetadata var = vars.get("test_var_name");
        Assert.assertNotNull(var);
        Assert.assertEquals(DataType.MESSAGE, var.getType());
    }

    @Test
    public void testGetVariablesSetByPredecessorsNoPreviousAssertion() throws Exception {
        // Setup test assertion.
        //
        CustomAssertionHolder cah = new CustomAssertionHolder();

        List<Assertion> children = new ArrayList<>(1);
        children.add(cah);

        @SuppressWarnings("UnusedDeclaration")
        AllAssertion allAssertion = new AllAssertion(children);

        Audit audit = new TestAudit();
        VariableServicesImpl variableServices = new VariableServicesImpl(audit, cah);

        Map<String, VariableMetadata> vars = variableServices.getVariablesSetByPredecessors();
        Assert.assertNotNull(vars);
        Assert.assertEquals(0, vars.size());
    }
}