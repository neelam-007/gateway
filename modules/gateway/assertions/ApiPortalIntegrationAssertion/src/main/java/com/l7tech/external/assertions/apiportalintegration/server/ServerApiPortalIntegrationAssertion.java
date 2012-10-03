package com.l7tech.external.assertions.apiportalintegration.server;

import com.l7tech.external.assertions.apiportalintegration.ApiPortalIntegrationAssertion;
import com.l7tech.gateway.common.audit.AssertionMessages;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.PolicyAssertionException;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.server.policy.assertion.AbstractServerAssertion;
import org.apache.commons.lang.StringUtils;
import org.springframework.context.ApplicationContext;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Server side implementation of the ApiPortalIntegrationAssertion.
 *
 * @see com.l7tech.external.assertions.apiportalintegration.ApiPortalIntegrationAssertion
 */
public class ServerApiPortalIntegrationAssertion extends AbstractServerAssertion<ApiPortalIntegrationAssertion> {
    private static final Logger logger = Logger.getLogger(ServerApiPortalIntegrationAssertion.class.getName());

    private final ApiPortalIntegrationAssertion assertion;

    public ServerApiPortalIntegrationAssertion(final ApiPortalIntegrationAssertion assertion, final ApplicationContext context) throws PolicyAssertionException {
        super(assertion);
        this.assertion = assertion;
    }

    public AssertionStatus checkRequest(final PolicyEnforcementContext context) throws IOException, PolicyAssertionException {
        if (StringUtils.isNotBlank(assertion.getVariablePrefix())) {
            context.setVariable(assertion.getVariablePrefix() + ".apiId", assertion.getApiId());
            context.setVariable(assertion.getVariablePrefix() + ".apiGroup", assertion.getApiGroup());
        } else {
            getAudit().logAndAudit(AssertionMessages.ASSERTION_MISCONFIGURED, "Variable prefix is not set.");
        }
        return AssertionStatus.NONE;
    }

    /*
     * Called reflectively by module class loader when module is unloaded, to ask us to clean up any globals
     * that would otherwise keep our instances from getting collected.
     */
    public static void onModuleUnloaded() {
        // This assertion doesn't have anything to do in response to this, but it implements this anyway
        // since it will be used as an example by future modular assertion authors
        logger.log(Level.INFO, "ServerApiPortalIntegrationAssertion is preparing itself to be unloaded");
    }
}
