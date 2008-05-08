package com.l7tech.external.assertions.actional.server;

import com.l7tech.server.audit.Auditor;
import com.l7tech.server.audit.LogOnlyAuditor;
import com.l7tech.external.assertions.actional.ActionalAssertion;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.PolicyAssertionException;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.server.policy.assertion.AbstractServerAssertion;
import org.springframework.context.ApplicationContext;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Server side implementation of the ActionalAssertion.
 *
 * @see com.l7tech.external.assertions.actional.ActionalAssertion
 */
public class ServerActionalAssertion extends AbstractServerAssertion<ActionalAssertion> {
    private static final Logger logger = Logger.getLogger(ServerActionalAssertion.class.getName());

    private final ActionalAssertion assertion;
    private final Auditor auditor;
    private final String[] variablesUsed;

    public ServerActionalAssertion(ActionalAssertion assertion, ApplicationContext context) throws PolicyAssertionException {
        super(assertion);

        this.assertion = assertion;
        this.auditor = context != null ? new Auditor(this, context, logger) : new LogOnlyAuditor(logger);
        this.variablesUsed = assertion.getVariablesUsed();
    }

    public AssertionStatus checkRequest(PolicyEnforcementContext context) throws IOException, PolicyAssertionException {
        return AssertionStatus.FAILED;
    }

    /*
     * Called reflectively by module class loader when module is unloaded, to ask us to clean up any globals
     * that would otherwise keep our instances from getting collected.
     */
    public static void onModuleUnloaded() {
        // This assertion doesn't have anything to do in response to this, but it implements this anyway
        // since it will be used as an example by future modular assertion authors
        logger.log(Level.INFO, "ServerActionalAssertion is preparing itself to be unloaded");
    }
}
