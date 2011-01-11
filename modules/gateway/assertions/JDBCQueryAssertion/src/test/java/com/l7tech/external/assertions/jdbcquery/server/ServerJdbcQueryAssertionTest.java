package com.l7tech.external.assertions.jdbcquery.server;

import com.l7tech.common.io.XmlUtil;
import com.l7tech.external.assertions.jdbcquery.JdbcQueryAssertion;
import com.l7tech.message.Message;
import com.l7tech.policy.assertion.PolicyAssertionException;
import com.l7tech.policy.variable.NoSuchVariableException;
import com.l7tech.server.ApplicationContexts;
import com.l7tech.server.jdbc.JdbcQueryingManager;
import com.l7tech.server.jdbc.MockJdbcDatabaseManager;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.server.message.PolicyEnforcementContextFactory;
import junit.framework.Assert;
import org.junit.Before;
import org.junit.Test;
import org.springframework.context.ApplicationContext;
import org.springframework.jdbc.support.rowset.SqlRowSet;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

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

    /**
     * Test getting a plain query statement and the prepared statement parameters.
     *
     * @throws PolicyAssertionException
     */
    @Test
    public void testPlainQueryStatementAndPreparedStatementParameters() throws PolicyAssertionException {
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

        // Check the query statement
        assertEquals("Correct plain query produced", expectedPlainQuery, actualPlainQuery);

        // Check the prepared statement parameters
        assertEquals("Employee Department found", department, params.get(0));
        assertEquals("Employee Name found", employee_info[0], params.get(1));
        assertEquals("Employee Sex found", employee_info[1], params.get(2));
        assertEquals("Employee Age found", employee_info[2], params.get(3));
    }

    /**
     * Test querying results and context variable naming and creation.
     *
     * @throws PolicyAssertionException
     * @throws SQLException
     * @throws NoSuchVariableException
     */
    @Test
    public void testQueryResultContextVariables() throws PolicyAssertionException, SQLException, NoSuchVariableException {
        Map<String, String> namingMap = new TreeMap<String, String>();
        String originalDeptColumnName = MockJdbcDatabaseManager.MOCK_COLUMN_NAMES[1];
        namingMap.put(originalDeptColumnName, "dept");

        JdbcQueryAssertion assertion = new JdbcQueryAssertion();
        assertion.setNamingMap(namingMap);
        assertion.setMaxRecords(MockJdbcDatabaseManager.MOCK_MAX_ROWS);

        JdbcQueryingManager jdbcQueryingManager = (JdbcQueryingManager) appCtx.getBean("jdbcQueryingManager");
        SqlRowSet resultSet = jdbcQueryingManager.getMockSqlRowSet();

        ServerJdbcQueryAssertion serverAssertion = new ServerJdbcQueryAssertion(assertion, appCtx);
        int numOfRecords = serverAssertion.setContextVariables(resultSet, peCtx);

        // Check the number of returned records by the query
        assertEquals("The number of returned numOfRecords matched", MockJdbcDatabaseManager.MOCK_MAX_ROWS, numOfRecords);

        // Check if the context variable 'department' has been renamed to 'dept'.
        Object deptCtxVar = peCtx.getVariable("jdbcQuery.dept");
        assertNotNull("The context variable successfully renamed from '" + originalDeptColumnName + "' to 'dept'", deptCtxVar);

        // Check context variables creation
        assertArrayEquals("All names matched", MockJdbcDatabaseManager.MOCK_NAMES, (Object[])peCtx.getVariable("jdbcQuery.name"));
        assertArrayEquals("All departments matched", MockJdbcDatabaseManager.MOCK_DEPTS, (Object[])peCtx.getVariable("jdbcQuery.dept"));
        assertArrayEquals("All sexes matched", MockJdbcDatabaseManager.MOCK_SEXES, (Object[])peCtx.getVariable("jdbcQuery.sex"));
        assertArrayEquals("All ages matched", MockJdbcDatabaseManager.MOCK_AGES, (Object[])peCtx.getVariable("jdbcQuery.age"));
    }

    private PolicyEnforcementContext makeContext(String req, String res) {
        Message request = new Message();
        request.initialize( XmlUtil.stringAsDocument(req),0);
        Message response = new Message();
        response.initialize( XmlUtil.stringAsDocument(res),0);
        return PolicyEnforcementContextFactory.createPolicyEnforcementContext(request, response);
    }
}