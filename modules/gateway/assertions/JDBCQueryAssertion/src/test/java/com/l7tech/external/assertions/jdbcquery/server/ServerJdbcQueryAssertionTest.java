package com.l7tech.external.assertions.jdbcquery.server;

import com.l7tech.common.io.XmlUtil;
import com.l7tech.external.assertions.jdbcquery.JdbcQueryAssertion;
import com.l7tech.gateway.common.audit.TestAudit;
import com.l7tech.message.Message;
import com.l7tech.policy.assertion.PolicyAssertionException;
import com.l7tech.policy.variable.NoSuchVariableException;
import com.l7tech.server.jdbc.JdbcQueryUtils;
import com.l7tech.server.jdbc.JdbcQueryingManager;
import com.l7tech.server.jdbc.MockJdbcDatabaseManager;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.server.message.PolicyEnforcementContextFactory;
import com.l7tech.util.CollectionUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.internal.runners.JUnit38ClassRunner;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.jdbc.support.rowset.SqlRowSet;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import static org.junit.Assert.*;

/**
 * @author ghuang
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = "/com/l7tech/server/resources/testApplicationContext.xml")
public class ServerJdbcQueryAssertionTest {
    @Autowired
    private ApplicationContext appCtx;

    private PolicyEnforcementContext peCtx;

    private JdbcQueryAssertion assertion;

    @Before
    public void setUp() throws Exception {
        // Get the policy enforcement context
        peCtx = makeContext("<myrequest/>", "<myresponse/>");
        //create assertion
        assertion = new JdbcQueryAssertion();
    }

    /**
     * Test getting a plain query statement and the prepared statement parameters.
     *
     * @throws PolicyAssertionException
     */
    @Test
    public void testPlainQueryStatementAndPreparedStatementParameters() throws PolicyAssertionException {

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
        String actualPlainQuery = JdbcQueryUtils.getQueryStatementWithoutContextVariables(query, params, peCtx,assertion.getVariablesUsed(),assertion.isAllowMultiValuedVariables(),assertion.getResolveAsObjectList(),new TestAudit() );

        // Check the query statement
        assertEquals("Correct plain query produced", expectedPlainQuery, actualPlainQuery);

        // Check the prepared statement parameters
        assertEquals("Employee Department found", department, params.get(0));
        assertEquals("Employee Name found", employee_info[0], params.get(1));
        assertEquals("Employee Sex found", employee_info[1], params.get(2));
        assertEquals("Employee Age found", employee_info[2], params.get(3));
    }

    @Test
    public void shouldReturnConstructedQueryWhenMultiValueParametersUsed() throws Exception {

        final String department = "Production";
        final List<Object> employeeIds = new ArrayList<Object>();
        employeeIds.add(1);
        employeeIds.add(2);
        employeeIds.add(3);

        peCtx.setVariable("var_dept", department);
        peCtx.setVariable("var_ids", employeeIds);

        List<Object> params = new ArrayList<Object>();
        String query = "select * from employees where id in (${var_ids}) and department = ${var_dept}";
        assertion.setSqlQuery(query);
        assertion.setAllowMultiValuedVariables(true);

        String expectedPlainQuery = "select * from employees where id in (?, ?, ?) and department = ?";
        String actualPlainQuery = JdbcQueryUtils.getQueryStatementWithoutContextVariables(query, params, peCtx,assertion.getVariablesUsed(),assertion.isAllowMultiValuedVariables(),assertion.getResolveAsObjectList(),new TestAudit());
        assertEquals(expectedPlainQuery, actualPlainQuery);
        assertEquals(employeeIds.get(0), params.get(0));
        assertEquals(employeeIds.get(1), params.get(1));
        assertEquals(employeeIds.get(2), params.get(2));
        assertEquals(department, params.get(3));
    }


    @Test
     public void shouldTestMultivaluedVariableExpansionWhenAllowMultivalueTurnedOff() throws Exception {
        final String department = "Production";
        final List<Object> employeeIds = new ArrayList<Object>();
        employeeIds.add(1);
        employeeIds.add(2);
        employeeIds.add(3);

        peCtx.setVariable("var_dept", department);
        peCtx.setVariable("var_ids", employeeIds);

        List<Object> params = new ArrayList<Object>();
        String query = "select * from employees where id in (${var_ids}) and department = ${var_dept}";
        assertion.setSqlQuery(query);
        assertion.setAllowMultiValuedVariables(false);

        String expectedPlainQuery = "select * from employees where id in (?) and department = ?";
        String actualPlainQuery = JdbcQueryUtils.getQueryStatementWithoutContextVariables(query, params, peCtx,assertion.getVariablesUsed(),assertion.isAllowMultiValuedVariables(),assertion.getResolveAsObjectList(),new TestAudit());
        assertEquals(expectedPlainQuery, actualPlainQuery);
        assertEquals("1, 2, 3", params.get(0));
        assertEquals(department, params.get(1));
     }

    @Test
    public void shouldReturnConstructedQueryWhenTheSameParameterUsedTwiceAndMultiValueParametersUsed() throws Exception {

        final String department = "Production";
        final List<Object> employeeIds = new ArrayList<Object>();
        employeeIds.add(1);
        employeeIds.add(2);
        employeeIds.add(3);

        peCtx.setVariable("var_dept", department);
        peCtx.setVariable("var_ids", employeeIds);

        List<Object> params = new ArrayList<Object>();
        String query = "select * from employees e1, employees e2 where e1.id in (${var_ids}) and e2.id in (${var_ids}) and e1.department = ${var_dept}";
        assertion.setSqlQuery(query);
        assertion.setAllowMultiValuedVariables(true);

        String expectedPlainQuery = "select * from employees e1, employees e2 where e1.id in (?, ?, ?) and e2.id in (?, ?, ?) and e1.department = ?";
        String actualPlainQuery = JdbcQueryUtils.getQueryStatementWithoutContextVariables(query, params, peCtx,assertion.getVariablesUsed(),assertion.isAllowMultiValuedVariables(),assertion.getResolveAsObjectList(),new TestAudit());
        assertEquals(expectedPlainQuery, actualPlainQuery);
        assertEquals(employeeIds.get(0), params.get(0));
        assertEquals(employeeIds.get(1), params.get(1));
        assertEquals(employeeIds.get(2), params.get(2));
        assertEquals(employeeIds.get(0), params.get(3));
        assertEquals(employeeIds.get(1), params.get(4));
        assertEquals(employeeIds.get(2), params.get(5));
        assertEquals(department, params.get(6));
    }

    @Test(expected = IllegalArgumentException.class)
    public void shouldThrowExceptionWhenVariableValueNotFound() throws Exception {
        final List<Object> employeeIds = new ArrayList<Object>();
        employeeIds.add(1);
        employeeIds.add(2);
        assertion.setAllowMultiValuedVariables(true);

        peCtx.setVariable("var_dept", "department");
        peCtx.setVariable("var_ids", "1");

        List<Object> params = new ArrayList<Object>();
        String query = "select * from employees where id=${var_ids[3]} and department = ${var_dept}";
        assertion.setSqlQuery(query);
        JdbcQueryUtils.getQueryStatementWithoutContextVariables(query, params, peCtx,assertion.getVariablesUsed(),assertion.isAllowMultiValuedVariables(),assertion.getResolveAsObjectList(),new TestAudit());
    }

    @Test
    public void shouldReturnEmptyVaslueWhenVariableIndexOutOfBounds() throws Exception {
        final List<Object> employeeIds = new ArrayList<Object>();
        final String department = "Production";
        employeeIds.add(1);
        employeeIds.add(2);

        peCtx.setVariable("var_dept", department);
        peCtx.setVariable("var_ids", "1");

        List<Object> params = new ArrayList<Object>();
        String query = "select * from employees where id=${var_ids[3]} and department = ${var_dept}";
        assertion.setSqlQuery(query);
        String actualQuery = JdbcQueryUtils.getQueryStatementWithoutContextVariables(query, params, peCtx,assertion.getVariablesUsed(),assertion.isAllowMultiValuedVariables(),assertion.getResolveAsObjectList(),new TestAudit());
        assertEquals("select * from employees where id=? and department = ?", actualQuery);
        assertEquals("", params.get(0));
        assertEquals(department, params.get(1));
        
    }
    
    @Test
    public void shouldReturnUnalteredQueryStringWhenNoContextVariablesPresent() throws Exception {
        List<Object> params = new ArrayList<Object>();
        String expectedQuery = "select * from employees where id=1 and department = 'Production'";
        assertion.setSqlQuery(expectedQuery);
        String actualQuery = JdbcQueryUtils.getQueryStatementWithoutContextVariables(expectedQuery, params, peCtx,assertion.getVariablesUsed(),assertion.isAllowMultiValuedVariables(),assertion.getResolveAsObjectList(),new TestAudit());
        assertEquals(expectedQuery, actualQuery);
        assertTrue(params.size() == 0);

    }

    @Test
    public void resolveContextVariablesAsObjects() throws Exception {
        Object varValue = new byte[]{0x01,0x02};
        peCtx.setVariable("var", varValue);
        peCtx.setVariable("var1", varValue);

        List<Object> params = new ArrayList<Object>();
        String expectedQuery = "select * from employees where id=${var} and department = ${var1}";
        assertion.setSqlQuery(expectedQuery);
        assertion.setResolveAsObjectList(CollectionUtils.list("var"));
        String actualQuery = JdbcQueryUtils.getQueryStatementWithoutContextVariables(expectedQuery, params, peCtx,assertion.getVariablesUsed(),assertion.isAllowMultiValuedVariables(),assertion.getResolveAsObjectList(),new TestAudit());
        assertTrue(params.size() == 2);
        assertEquals(params.get(0), varValue);
        assertEquals(params.get(1), varValue.toString());
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
        request.initialize( XmlUtil.stringAsDocument(req));
        Message response = new Message();
        response.initialize( XmlUtil.stringAsDocument(res));
        return PolicyEnforcementContextFactory.createPolicyEnforcementContext(request, response);
    }
}