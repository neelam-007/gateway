package com.l7tech.external.assertions.apiportalintegration.server;

import com.l7tech.external.assertions.apiportalintegration.GetIncrementAssertion;
import com.l7tech.gateway.common.audit.AssertionMessages;
import com.l7tech.gateway.common.jdbc.JdbcConnection;
import com.l7tech.objectmodel.FindException;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.PolicyAssertionException;
import com.l7tech.server.jdbc.JdbcConnectionManager;
import com.l7tech.server.jdbc.JdbcQueryingManager;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.server.policy.assertion.AbstractServerAssertion;
import com.l7tech.util.ExceptionUtils;
import java.io.IOException;
import java.util.Collections;
import java.util.Map;
import java.util.logging.Logger;
import javax.xml.bind.JAXBException;
import org.springframework.context.ApplicationContext;

/**
 * Server side implementation of the GetIncrementAssertion.
 *
 * @see com.l7tech.external.assertions.apiportalintegration.GetIncrementAssertion
 */
public class ServerGetIncrementAssertion extends AbstractServerAssertion<GetIncrementAssertion> {
    private static final Logger logger = Logger.getLogger(ServerGetIncrementAssertion.class.getName());

    private final String[] variablesUsed;


    // todo make configurable? cluster prop? use default jdbc?
    private int queryTimeout = 100000;
    private int maxRecords = 1000000;

    public final String ENTITY_TYPE_APPLICATION = "APPLICATION";


    private final JdbcQueryingManager jdbcQueryingManager;
    private final JdbcConnectionManager jdbcConnectionManager;

    public ServerGetIncrementAssertion(GetIncrementAssertion assertion, ApplicationContext context) throws PolicyAssertionException, JAXBException {
        super(assertion);
        this.variablesUsed = assertion.getVariablesUsed();
        jdbcQueryingManager = context.getBean("jdbcQueryingManager", JdbcQueryingManager.class);
        jdbcConnectionManager = context.getBean("jdbcConnectionManager", JdbcConnectionManager.class);
    }

    /*
     * For tests.
     */
    ServerGetIncrementAssertion(GetIncrementAssertion assertion, JdbcQueryingManager jdbcQueryingManager, JdbcConnectionManager jdbcConnectionManager) throws PolicyAssertionException {
        super(assertion);
        this.variablesUsed = assertion.getVariablesUsed();
        this.jdbcQueryingManager = jdbcQueryingManager;
        this.jdbcConnectionManager = jdbcConnectionManager;
    }

    public AssertionStatus checkRequest(PolicyEnforcementContext context) throws IOException, PolicyAssertionException {
        try {
            Map<String, Object> vars = context.getVariableMap(this.variablesUsed, getAudit());

            Object entityType = vars.get(assertion.getVariablePrefix() + "." + GetIncrementAssertion.SUFFIX_TYPE);
            Object jdbcConnectionName = vars.get(assertion.getVariablePrefix() + "." + GetIncrementAssertion.SUFFIX_JDBC_CONNECTION);
            Object since = vars.get(assertion.getVariablePrefix() + "." + GetIncrementAssertion.SUFFIX_SINCE).toString();

            //validate inputs
            if (entityType == null) {
                throw new PolicyAssertionException(assertion, "Assertion must supply an entity type");
            }
            if(!entityType.equals(ENTITY_TYPE_APPLICATION)){
                throw new PolicyAssertionException(assertion, "Not supported entity type: "+ entityType);
            }

            if (since == null) {
                throw new PolicyAssertionException(assertion, "Assertion must supply a since timestamp");
            }

            if (jdbcConnectionName == null) {
                throw new PolicyAssertionException(assertion, "Assertion must supply a connection name");
            }

            // validate that the connection exists.
            final JdbcConnection connection;
            try {
                connection = jdbcConnectionManager.getJdbcConnection(jdbcConnectionName.toString());
                if (connection == null) throw new FindException();
            } catch (FindException e) {
                logAndAudit(AssertionMessages.EXCEPTION_WARNING, "Could not find JDBC connection: " + jdbcConnectionName);
                return AssertionStatus.FAILED;
            }

            // create result
            String jsonStr = getJsonMessage(jdbcConnectionName.toString(),since);

            // save result
            context.setVariable(assertion.getVariablePrefix()+'.'+GetIncrementAssertion.SUFFIX_JSON,jsonStr);


        } catch (Exception ex) {
            final String errorMsg = "Error Retrieving Application Increment";
            logAndAudit(AssertionMessages.EXCEPTION_WARNING_WITH_MORE_INFO, new String[]{errorMsg}, ExceptionUtils.getDebugException(ex));
            return AssertionStatus.FAILED;
        }
        return AssertionStatus.NONE;
    }

    private String getJsonMessage(String connName, Object since) {
        // todo everything
        return null;
    }

    private Object queryJdbc(String connName, String queryString){
        final Object result = jdbcQueryingManager.performJdbcQuery(connName, queryString, null, maxRecords, queryTimeout, Collections.emptyList());
        return result;
    }

}
