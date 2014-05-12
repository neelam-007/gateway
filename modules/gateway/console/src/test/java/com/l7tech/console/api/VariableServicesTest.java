package com.l7tech.console.api;

import com.l7tech.console.util.Registry;
import com.l7tech.objectmodel.GuidBasedEntityManager;
import com.l7tech.policy.Policy;
import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.assertion.CustomAssertionHolder;
import com.l7tech.policy.assertion.SetVariableAssertion;
import com.l7tech.policy.assertion.composite.AllAssertion;
import com.l7tech.policy.assertion.ext.VariableServices;
import com.l7tech.policy.variable.DataType;
import com.l7tech.policy.variable.VariableMetadata;
import junit.framework.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class VariableServicesTest {

    @Mock
    private Registry registry;

    @Mock
    private GuidBasedEntityManager<Policy> policyFinder;

    @Test
    public void testGetVariablesSetByPredecessorsAddAssertion() throws Exception {
        // Test scenario when an assertion is added to policy.
        //
        Registry.setDefault(registry);
        when(registry.getPolicyFinder()).thenReturn(policyFinder);

        // Setup test data.
        //
        SetVariableAssertion previousAssertion = new SetVariableAssertion();
        previousAssertion.setDataType(DataType.STRING);
        previousAssertion.setVariableToSet("test_var_name");
        CustomAssertionHolder cah = new CustomAssertionHolder();

        Map<String, Object> consoleContext = new HashMap<>(1);
        CustomConsoleContext.addVariableServices(consoleContext, cah, previousAssertion);
        VariableServices variableServices = (VariableServices) consoleContext.get(VariableServices.CONSOLE_CONTEXT_KEY);

        Map<String, VariableMetadata> vars = variableServices.getVariablesSetByPredecessors();
        Assert.assertNotNull(vars);
        Assert.assertEquals(1, vars.size());
        VariableMetadata var = vars.get("test_var_name");
        Assert.assertNotNull(var);
        Assert.assertEquals(DataType.STRING, var.getType());
    }

    @Test
    public void testGetVariablesSetByPredecessorsExistingAssertion() throws Exception {
        // Test scenario when an assertion is already in policy.
        //
        Registry.setDefault(registry);
        when(registry.getPolicyFinder()).thenReturn(policyFinder);

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

        Map<String, Object> consoleContext = new HashMap<>(1);
        CustomConsoleContext.addVariableServices(consoleContext, cah, previousAssertion);
        VariableServices variableServices = (VariableServices) consoleContext.get(VariableServices.CONSOLE_CONTEXT_KEY);

        Map<String, VariableMetadata> vars = variableServices.getVariablesSetByPredecessors();
        Assert.assertNotNull(vars);
        Assert.assertEquals(1, vars.size());
        VariableMetadata var = vars.get("test_var_name");
        Assert.assertNotNull(var);
        Assert.assertEquals(DataType.MESSAGE, var.getType());
    }
}