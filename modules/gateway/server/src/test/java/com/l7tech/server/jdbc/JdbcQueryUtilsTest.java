package com.l7tech.server.jdbc;

import com.l7tech.gateway.common.audit.TestAudit;
import com.l7tech.message.Message;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.server.message.PolicyEnforcementContextFactory;
import com.l7tech.util.CollectionUtils;
import com.l7tech.util.Pair;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.assertEquals;

public class JdbcQueryUtilsTest {

    //todo test coverage for JdbcQueryUtils.performJdbcQuery

    @Test
    public void resolveContextVariablesAsObjects() throws Exception {
        Object varValue = new byte[]{0x01,0x02};
        PolicyEnforcementContext peCtx = PolicyEnforcementContextFactory.createPolicyEnforcementContext(new Message(), new Message());
        peCtx.setVariable("var", varValue);
        peCtx.setVariable("var1", varValue);

        String expectedQuery = "select * from employees where id=${var} and department = ${var1}";
        final Pair<String,List<Object>> pair = JdbcQueryUtils.getQueryStatementWithoutContextVariables(
                expectedQuery, peCtx, new String[]{"var", "var1"}, false, CollectionUtils.list("var"), new TestAudit());
        final String query = pair.left;
        final List<Object> params = pair.right;
        System.out.println(query);
        assertEquals("Incorrect number of variables values found", 2, params.size());
        assertEquals(params.get(0), varValue);
        assertEquals(params.get(1), varValue.toString());
    }
}
