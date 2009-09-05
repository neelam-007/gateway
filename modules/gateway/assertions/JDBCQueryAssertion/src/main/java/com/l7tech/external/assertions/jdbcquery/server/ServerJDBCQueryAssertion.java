package com.l7tech.external.assertions.jdbcquery.server;

import com.l7tech.external.assertions.jdbcquery.JDBCQueryAssertion;
import com.l7tech.gateway.common.cluster.ClusterProperty;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.PolicyAssertionException;
import com.l7tech.server.ServerConfig;
import com.l7tech.server.audit.Auditor;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.server.policy.assertion.AbstractServerAssertion;
import com.l7tech.server.policy.variable.ExpandVariables;
import com.l7tech.util.ExceptionUtils;
import com.l7tech.util.ResourceUtils;
import org.springframework.context.ApplicationContext;

import java.io.IOException;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Server side implementation of the JDBCQueryAssertion.
 *
 * @see com.l7tech.external.assertions.jdbcquery.JDBCQueryAssertion
 */
public class ServerJDBCQueryAssertion extends AbstractServerAssertion<JDBCQueryAssertion> {
    private static final Logger logger = Logger.getLogger(ServerJDBCQueryAssertion.class.getName());

    private final JDBCQueryAssertion assertion;
    private final Auditor auditor;
    private final String[] variablesUsed;
    private final ServerConfig serverConfig;

    private JdbcConnectionManager connectionManager = JdbcConnectionManager.INSTANCE;

    public ServerJDBCQueryAssertion(JDBCQueryAssertion assertion, ApplicationContext context) throws PolicyAssertionException {
        super(assertion);

        if (context == null) throw new IllegalStateException("Application context cannot be null.");

        this.assertion = assertion;
        this.auditor = new Auditor(this, context, logger);
        this.variablesUsed = assertion.getVariablesUsed();
        this.serverConfig = (ServerConfig) context.getBean("serverConfig", ServerConfig.class);
    }

    public AssertionStatus checkRequest(PolicyEnforcementContext context) throws IOException, PolicyAssertionException {
        Map<String, Object> vars = context.getVariableMap(variablesUsed, auditor);

        String connectionUrl = ExpandVariables.process(assertion.getConnectionUrl(), vars, auditor);
        String driver = ExpandVariables.process(assertion.getDriver(), vars, auditor);
        String user = ExpandVariables.process(assertion.getUser(), vars, auditor);
        String sql = ExpandVariables.process(assertion.getSql(), vars, auditor);
        String varPrefix = ExpandVariables.process(assertion.getVariablePrefix(), vars, auditor) + ".";
        String maxRows = serverConfig.getProperty(ClusterProperty.asServerConfigPropertyName(JDBCQueryAssertion.MAX_RECORDS_CLUSTER_PROP));

        logger.fine("Connection URL: " + connectionUrl);
        logger.fine("Driver: " + driver);
        logger.fine("Username: " + user);
        logger.fine("Password: " + assertion.getPass());
        logger.fine("Query SQL: " + sql);

        Statement stmnt = null;
        try {
            Connection conn = connectionManager.getConnection(connectionUrl, driver, user, assertion.getPass());
            stmnt = conn.createStatement();
            stmnt.setMaxRows(Integer.parseInt(maxRows));

            if (JDBCQueryAssertion.SELECT_PATTERN.matcher(sql).find()) doSelect(stmnt, context, sql, varPrefix);
            else doDml(stmnt, context, sql, varPrefix);

        } catch (SQLException sqle) {
            logger.log(Level.WARNING, ExceptionUtils.getMessage(sqle), sqle);
            return AssertionStatus.FAILED;
        } finally {
            ResourceUtils.closeQuietly(stmnt);
        }

        return AssertionStatus.NONE;
    }

    private void doSelect(Statement stmnt, PolicyEnforcementContext context, String sql, String varPrefix) throws SQLException {
        ResultSet rs = stmnt.executeQuery(sql);

        //populate the context variables
        /**
         * key = variable name suffix
         * value = ArrayList of values
         */
        Map<String, ArrayList<String>> results = new HashMap<String, ArrayList<String>>();

        Set<Map.Entry<String, String>> entries = assertion.getVariableMap().entrySet();
        String columnName, varSuffix, value;

        int count = 0;
        while (rs.next()) {
            for (Map.Entry<String, String> entry : entries) {
                //
                columnName = entry.getKey();
                varSuffix = entry.getValue();

                value = rs.getString(columnName);
                logger.log(Level.FINE, "Column: " + columnName + "\tName suffix: " + varSuffix + "\tValue: " + value);

                ArrayList<String> variableValues = results.get(varSuffix);
                //if the ArrayList of values does not exist yet, create it
                if (variableValues == null) {
                    variableValues = new ArrayList<String>();
                    results.put(varSuffix, variableValues);
                }

                //add the value to our results map
                variableValues.add(value);
            }
            count++;
        }

        //actually set the variables in the PolicyEnforcementContext
        context.setVariable(varPrefix + JDBCQueryAssertion.VAR_COUNT_SUFFIX, count);

        Set<Map.Entry<String, ArrayList<String>>> ntries = results.entrySet();
        ArrayList<String> variable;
        for (Map.Entry<String, ArrayList<String>> ntry : ntries) {
            variable = ntry.getValue();
            logger.fine("Setting: " + varPrefix + ntry.getKey() + "\tValue: " + ntry.getValue().get(0));
            context.setVariable(varPrefix + ntry.getKey(), variable.toArray(new String[variable.size()]));
        }
    }

    private void doDml(Statement stmnt, PolicyEnforcementContext context, String sql, String varPrefix) throws SQLException {
        int rowsModified = stmnt.executeUpdate(sql);
        context.setVariable(varPrefix + JDBCQueryAssertion.VAR_COUNT_SUFFIX, rowsModified);
    }

    /*
     * Called reflectively by module class loader when module is unloaded, to ask us to clean up any globals
     * that would otherwise keep our instances from getting collected.
     */
    public static void onModuleUnloaded() {
        JdbcConnectionManager.clearConnections();
        logger.log(Level.INFO, "ServerJDBCQueryAssertion is preparing itself to be unloaded");
    }
}
