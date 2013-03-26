package com.l7tech.skunkworks.jdbc;

import com.l7tech.external.assertions.jdbcquery.JdbcQueryAssertion;
import com.l7tech.external.assertions.jdbcquery.server.ServerJdbcQueryAssertion;
import com.l7tech.gateway.common.jdbc.JdbcConnection;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.PolicyAssertionException;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import org.springframework.jdbc.core.metadata.CallMetaDataContext;
import org.springframework.jdbc.core.metadata.CallMetaDataProvider;
import org.springframework.jdbc.core.metadata.CallMetaDataProviderFactory;
import org.springframework.jdbc.core.metadata.CallParameterMetaData;

import java.io.IOException;
import java.io.StringWriter;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.List;

/**
 * This was created: 3/19/13 as 2:09 PM
 *
 * @author Victor Kazakov
 */
public class ServerJdbcQueryAssertionMySQLIntegrationTests extends ServerJdbcQueryAssertionIntegrationAbstractTestClass {

    @BeforeClass
    public static void beforeClass() throws Exception {
        ServerJdbcQueryAssertionIntegrationAbstractTestClass.beforeClass(getJdbcConnection());
    }

    @Test
    public void testSendRetrieveVarchar2FromFunction() throws PolicyAssertionException, IOException {
        try {
            createDropItem(DropSendRetrieveVarchar2Function);
            createDropItem(CreateSendRetrieveVarchar2Function);
            JdbcQueryAssertion assertion = createJdbcQueryAssertion();
            getContextVariables().put("var_in", "abc");
            assertion.setSqlQuery("func " + SendRetrieveVarchar2FunctionName + " ${var_in}");
            assertion.setConvertVariablesToStrings(false);
            ServerJdbcQueryAssertion serverJdbcQueryAssertion = new ServerJdbcQueryAssertion(assertion, getContext());

            AssertionStatus assertionStatus = serverJdbcQueryAssertion.checkRequest(getPolicyEnforcementContext());

            Assert.assertEquals(AssertionStatus.NONE, assertionStatus);

            Assert.assertEquals(String.class, ((Object[]) getContextVariables().get("jdbcquery.return"))[0].getClass());

            String returnString = (String) ((Object[]) getContextVariables().get("jdbcquery.return"))[0];

            Assert.assertEquals("abcabc", returnString);

        } finally {
            createDropItem(DropSendRetrieveVarchar2Function);
        }
    }

    @Test
    @Ignore("Just here to see what is returned")
    public void testCallMetaDataProviderFactory() throws SQLException {
        try {
            createDropItem(DropSendRetrieveVarchar2Function);
            createDropItem(CreateSendRetrieveVarchar2Function);
            CallMetaDataContext callMetaDataContext = new CallMetaDataContext();
            callMetaDataContext.setCatalogName("test");
            callMetaDataContext.setProcedureName(SendRetrieveVarchar2FunctionName);
            CallMetaDataProvider callMetaDataProvider = CallMetaDataProviderFactory.createMetaDataProvider(getDataSource(), callMetaDataContext);
            List<CallParameterMetaData> params = callMetaDataProvider.getCallParameterMetaData();
            for (CallParameterMetaData param : params) {
                System.out.println(param.getParameterName() + " " + param.getTypeName() + " " + param.getParameterType());
            }
        } finally {
            createDropItem(DropSendRetrieveVarchar2Function);
        }
    }

    @Test
    @Ignore("Just here to see what is returned")
    public void testMetaData() throws SQLException {
        try {
            createDropItem(DropSendRetrieveVarchar2Function);
            createDropItem(CreateSendRetrieveVarchar2Function);
            try (Connection connection = getDataSource().getConnection()) {
                ResultSet resultSet = connection.getMetaData().getProcedureColumns(null, null, SendRetrieveVarchar2FunctionName, null);
//                ResultSet resultSet = connection.getMetaData().getProcedures(null, null, "%");
                ResultSetMetaData resultSetMetaData = resultSet.getMetaData();
                StringWriter sw = new StringWriter();
                sw.append("{| class=\"wikitable sortable\"\n" +
                        "|-\n");
                for (int i = 2; i < resultSetMetaData.getColumnCount(); i++) {
                    sw.append("! ").append(resultSetMetaData.getColumnName(i)).append(" !");
                }
                sw.append("\n|-\n");
                while (resultSet.next()) {
                    for (int i = 2; i < resultSetMetaData.getColumnCount(); i++) {
                        sw.append("| ").append(String.valueOf(resultSet.getObject(i))).append(" |");
                    }
                    sw.append("\n|-\n");
                }
                sw.append("|}");
                System.out.println(sw.toString());
            }
        } finally {
            createDropItem(DropSendRetrieveVarchar2Function);
        }
    }

    protected static JdbcConnection getJdbcConnection() {
        final JdbcConnection jdbcConn = new JdbcConnection();
        final String jdbcUrl = "jdbc:mysql://localhost:3306/test";

        jdbcConn.setJdbcUrl(jdbcUrl);
        jdbcConn.setName(ConnectionName);
        jdbcConn.setUserName("test");
        jdbcConn.setPassword("password");
        final String driverClass = "com.l7tech.jdbc.mysql.MySQLDriver";
        jdbcConn.setDriverClass(driverClass);
        jdbcConn.setMinPoolSize(1);
        jdbcConn.setMaxPoolSize(10);
        return jdbcConn;
    }

    private static final String SendRetrieveVarchar2FunctionName = "SendRetrieveVarchar";
    private static final String CreateSendRetrieveVarchar2Function =
            "create\n" +
                    "function test." + SendRetrieveVarchar2FunctionName + "(var_in Varchar(100)) \n" +
                    "returns Varchar(255)\n" +
                    "BEGIN\n" +
                    "    RETURN CONCAT(var_in, var_in);\n" +
                    "END";
    public static final String DropSendRetrieveVarchar2Function = "DROP FUNCTION IF EXISTS `test`.`" + SendRetrieveVarchar2FunctionName + "`";
}
