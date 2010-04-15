package com.l7tech.external.assertions.jdbcquery.server;

import com.l7tech.common.io.XmlUtil;
import com.l7tech.external.assertions.jdbcquery.JdbcQueryAssertion;
import com.l7tech.message.Message;
import com.l7tech.policy.assertion.PolicyAssertionException;
import com.l7tech.server.ApplicationContexts;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.server.message.PolicyEnforcementContextFactory;
import junit.framework.Assert;
import org.junit.Before;
import org.junit.Test;
import org.springframework.context.ApplicationContext;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;

/**
 * @author ghuang
 */
public class ServerJdbcQueryAssertionTest {
    private ApplicationContext appCtx;
    private PolicyEnforcementContext peCtx;

    @Before
    public void setUp() {
        // Get the spring app context
        if (appCtx == null) {
            appCtx = ApplicationContexts.getTestApplicationContext();
            Assert.assertNotNull("Fail - Unable to get applicationContext instance", appCtx);
        }

        // Get the policy enforcement context
        peCtx = makeContext("<myrequest/>", "<myresponse/>");
    }

    @Test
    public void testContextVariablesUsedInQuery() throws PolicyAssertionException {
        JdbcQueryAssertion assertion = new JdbcQueryAssertion();
        ServerJdbcQueryAssertion serverAssertion = new ServerJdbcQueryAssertion(assertion, appCtx);

        final String department = "Production";
        final String[] employee_info = new String[] {"John", "Male", "45"};

        peCtx.setVariable("var_dept", department);
        peCtx.setVariable("var_info", employee_info);

        List<Object> params = new ArrayList<Object>();
        String query = "SELECT * FROM employees " +
            "WHERE employee_department = ${var_dept} " +
            "AND name = ${var_info[0]} " +
            "ADN sex = ${var_info[1]} " +
            "AND age = ${var_info[2]}";
        assertion.setSqlQuery(query);

        String expectedPlainQuery = "SELECT * FROM employees WHERE employee_department = ? AND name = ? ADN sex = ? AND age = ?";
        String actualPlainQuery = serverAssertion.getQueryStatementWithoutContextVariables(query, params, peCtx);

        assertEquals("Correct plain query produced", expectedPlainQuery, actualPlainQuery);

        assertEquals("Employee Department found", department, params.get(0));
        assertEquals("Employee Name found", employee_info[0], params.get(1));
        assertEquals("Employee Sex found", employee_info[1], params.get(2));
        assertEquals("Employee Age found", employee_info[2], params.get(3));
    }

    private PolicyEnforcementContext makeContext(String req, String res) {
        Message request = new Message();
        request.initialize( XmlUtil.stringAsDocument(req));
        Message response = new Message();
        response.initialize( XmlUtil.stringAsDocument(res));
        return PolicyEnforcementContextFactory.createPolicyEnforcementContext(request, response);
    }
}