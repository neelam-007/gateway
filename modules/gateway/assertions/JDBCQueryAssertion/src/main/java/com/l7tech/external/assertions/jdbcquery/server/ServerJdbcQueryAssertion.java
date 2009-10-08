package com.l7tech.external.assertions.jdbcquery.server;

import com.l7tech.external.assertions.jdbcquery.JdbcQueryAssertion;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.PolicyAssertionException;
import com.l7tech.server.ServerConfig;
import com.l7tech.server.audit.Auditor;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.server.policy.assertion.AbstractServerAssertion;
import org.springframework.context.ApplicationContext;

import java.io.IOException;
import java.util.logging.Logger;

/**
 * Server side implementation of the JdbcQueryAssertion.
 *
 * @see com.l7tech.external.assertions.jdbcquery.JdbcQueryAssertion
 */
public class ServerJdbcQueryAssertion extends AbstractServerAssertion<JdbcQueryAssertion> {
    private static final Logger logger = Logger.getLogger(ServerJdbcQueryAssertion.class.getName());

    private final JdbcQueryAssertion assertion;
    private final Auditor auditor;
    private final String[] variablesUsed;
    private final ServerConfig serverConfig;

    private JdbcConnectionManager connectionManager = JdbcConnectionManager.INSTANCE;

    public ServerJdbcQueryAssertion(JdbcQueryAssertion assertion, ApplicationContext context) throws PolicyAssertionException {
        super(assertion);

        if (context == null) throw new IllegalStateException("Application context cannot be null.");

        this.assertion = assertion;
        this.auditor = new Auditor(this, context, logger);
        this.variablesUsed = assertion.getVariablesUsed();
        this.serverConfig = (ServerConfig) context.getBean("serverConfig", ServerConfig.class);
    }

    public AssertionStatus checkRequest(PolicyEnforcementContext context) throws IOException, PolicyAssertionException {
        return AssertionStatus.NONE;
    }
}
