package com.l7tech.server.jdbc;

import com.l7tech.gateway.common.audit.TestAudit;
import com.l7tech.message.Message;
import com.l7tech.policy.variable.VariableNameSyntaxException;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.server.message.PolicyEnforcementContextFactory;
import com.l7tech.util.CollectionUtils;
import com.l7tech.util.Pair;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

public class JdbcQueryUtilsTest {

    @Test
    public void resolveContextVariablesAsObjects() throws Exception {
        Object varValue = new byte[]{0x01, 0x02};
        PolicyEnforcementContext peCtx = PolicyEnforcementContextFactory.createPolicyEnforcementContext(new Message(), new Message());
        peCtx.setVariable("var", varValue);
        peCtx.setVariable("var1", varValue);

        String expectedQuery = "select * from employees where id=${var} and department = ${var1}";
        final Pair<String, List<Object>> pair = JdbcQueryUtils.getQueryStatementWithoutContextVariables(
                expectedQuery, peCtx, new String[]{"var", "var1"}, true, CollectionUtils.list("var"), new TestAudit());
        final String query = pair.left;
        final List<Object> params = pair.right;
        System.out.println(query);
        assertEquals("Incorrect number of variables values found", 2, params.size());
        assertEquals(params.get(0), varValue);
        assertEquals(params.get(1), varValue.toString());
        assertEquals("Unexpected query statement", "select * from employees where id=? and department = ?", query);
    }


    @Test
    public void resolveMultiValueContextVariables() throws Exception {
        String[] varArrray = new String[]{"var", "var1", "var2"};
        int intVal = 10;

        PolicyEnforcementContext peCtx = PolicyEnforcementContextFactory.createPolicyEnforcementContext(new Message(), new Message());
        peCtx.setVariable("var", varArrray);
        peCtx.setVariable("intVal", intVal);

        String expectedQuery = "select * from employees where id in (${var}) where time>${intVal}";
        final Pair<String, List<Object>> pair = JdbcQueryUtils.getQueryStatementWithoutContextVariables(
                expectedQuery, peCtx, new String[]{"var", "intVal"}, false, new TestAudit());
        final String query = pair.left;
        final List<Object> params = pair.right;

        assertEquals("Incorrect number of variables values found", 4, params.size());
        assertEquals(params.get(0), varArrray[0]);
        assertEquals(params.get(1), varArrray[1]);
        assertEquals(params.get(2), varArrray[2]);
        // All variables are resolved as objects, include single value context variables.
        assertEquals(params.get(3), intVal);
        assertEquals("Unexpected query statement", "select * from employees where id in (?, ?, ?) where time>?", query);
    }

    @Test
    public void repeatedContextVariables() throws Exception {
        String var = "var";

        PolicyEnforcementContext peCtx = PolicyEnforcementContextFactory.createPolicyEnforcementContext(new Message(), new Message());
        peCtx.setVariable("var", var);

        String expectedQuery = "select * from employees where first=${var} where last=${var}";
        final Pair<String, List<Object>> pair = JdbcQueryUtils.getQueryStatementWithoutContextVariables(
                expectedQuery, peCtx, new String[]{"var", "intVal"}, false, new TestAudit());
        final String query = pair.left;
        final List<Object> params = pair.right;

        assertEquals("Incorrect number of variables values found", 2, params.size());
        assertEquals(params.get(0), var);
        assertEquals(params.get(1), var);
        // All variables are resolved as objects, include single value context variables.
        assertEquals("Unexpected query statement", "select * from employees where first=? where last=?", query);
    }


    @Test

    public void resolveNonTextValueContextVariables() throws Exception {
        // all parameter values get resolved as context variable formatted strings.

        Object intValue = 3;
        Object longValue = 9L;
        Object boolValue = false;
        final Object[] vars = new Object[]{"BLAH", 10};

        PolicyEnforcementContext peCtx = PolicyEnforcementContextFactory.createPolicyEnforcementContext(new Message(), new Message());
        peCtx.setVariable("intVar", intValue);
        peCtx.setVariable("longVar", longValue);
        peCtx.setVariable("boolVar", boolValue);
        peCtx.setVariable("vars", vars);

        String expectedQuery = "select * from employees where id=${intVar} and floor=${longVar} and active=${boolVar} and stuff=${vars}";
        final Pair<String, List<Object>> pair = JdbcQueryUtils.getQueryStatementWithoutContextVariables(
                expectedQuery, peCtx, new String[]{"intVar", "longVar", "boolVar", "vars"}, true, new TestAudit());
        final String query = pair.left;
        final List<Object> params = pair.right;

        assertEquals("Incorrect number of variables values found", 4, params.size());
        assertEquals(params.get(0), intValue.toString());
        assertEquals(params.get(1), longValue.toString());
        assertEquals(params.get(2), boolValue.toString());
        assertEquals(params.get(3), "BLAH, 10");
        assertEquals("Unexpected query statement", "select * from employees where id=? and floor=? and active=? and stuff=?", query);

    }

    @Test
    public void resolveNonTextMultiValueContextVariables() throws Exception {
        // parameter objects are returned as objects, not context variable formatted strings.

        Object[] intValues = new Object[]{3, 4};
        Object[] longValues = new Object[]{9L, Long.MAX_VALUE};
        Object[] boolValues = new Object[]{true, false};

        PolicyEnforcementContext peCtx = PolicyEnforcementContextFactory.createPolicyEnforcementContext(new Message(), new Message());
        peCtx.setVariable("intVars", intValues);
        peCtx.setVariable("longVars", longValues);
        peCtx.setVariable("boolVars", boolValues);

        String expectedQuery = "select * from employees where id in (${intVars}) and floor in (${longVars}) and something in (${boolVars}) ";
        final Pair<String, List<Object>> pair = JdbcQueryUtils.getQueryStatementWithoutContextVariables(
                expectedQuery, peCtx, new String[]{"intVars", "longVars", "boolVars"}, false, new TestAudit());
        final String query = pair.left;
        final List<Object> params = pair.right;

        assertEquals("Incorrect number of variables values found", 6, params.size());
        assertEquals(params.get(0), intValues[0]);
        assertEquals(params.get(1), intValues[1]);
        assertEquals(params.get(2), longValues[0]);
        assertEquals(params.get(3), longValues[1]);
        assertEquals(params.get(4), boolValues[0]);
        assertEquals(params.get(5), boolValues[1]);
        assertEquals("Unexpected query statement", "select * from employees where id in (?, ?) and floor in (?, ?) and something in (?, ?) ", query);

        // resolve as objects list redundant here
        final Pair<String, List<Object>> noObjectList = JdbcQueryUtils.getQueryStatementWithoutContextVariables(
                expectedQuery, peCtx, new String[]{"intVars", "longVars", "boolVars"}, false, CollectionUtils.list("longVars"), new TestAudit());
        assertEquals("Everything should be resolved as objects by default", noObjectList.right, params);
    }

    @Test
    public void noContextVariables() throws Exception {

        PolicyEnforcementContext peCtx = PolicyEnforcementContextFactory.createPolicyEnforcementContext(new Message(), new Message());

        String expectedQuery = "select * from employees where id='1234' ";
        final Pair<String, List<Object>> pair = JdbcQueryUtils.getQueryStatementWithoutContextVariables(
                expectedQuery, peCtx, new String[0], true, new TestAudit());
        final String query = pair.left;
        final List<Object> params = pair.right;

        assertEquals("Incorrect number of variables values found", 0, params.size());
        assertEquals("Unexpected query statement", expectedQuery, query);
    }

    @Test
    public void failResolvingContextVariables() throws Exception {
        // strict context variable resolution for allow multi-value context variable ON

        PolicyEnforcementContext peCtx = PolicyEnforcementContextFactory.createPolicyEnforcementContext(new Message(), new Message());

        String expectedQuery = "select * from employees where id='${fake} ";

        try{
            JdbcQueryUtils.getQueryStatementWithoutContextVariables( expectedQuery, peCtx, new String[0], false, new TestAudit());
        } catch (VariableNameSyntaxException e){
            // expected
            return;
        }
        fail("Should not reach here");
    }
}
