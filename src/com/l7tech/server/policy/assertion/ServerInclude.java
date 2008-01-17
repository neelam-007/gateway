/**
 * Copyright (C) 2007 Layer 7 Technologies Inc.
 */
package com.l7tech.server.policy.assertion;

import com.l7tech.common.LicenseException;
import com.l7tech.common.audit.AssertionMessages;
import com.l7tech.common.util.ExceptionUtils;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.Include;
import com.l7tech.policy.assertion.PolicyAssertionException;
import com.l7tech.server.audit.Auditor;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.server.policy.PolicyCache;
import com.l7tech.server.policy.ServerPolicyException;
import com.l7tech.server.policy.ServerPolicyHandle;
import org.springframework.context.ApplicationContext;
import org.springframework.beans.BeansException;

import java.io.IOException;
import java.util.logging.Logger;
import java.util.logging.Level;

/**
 * Server assertion for included policy fragment.
 *
 * <p>This assertion is responsible for managing a handle on the policy that it
 * includes.</p>
 *
 * @author alex
 */
public class ServerInclude extends AbstractServerAssertion<Include> {
    private static final Logger logger = Logger.getLogger(ServerInclude.class.getName());

    private final Auditor auditor;
    private final ServerPolicyHandle serverPolicy;

    public ServerInclude(Include assertion, ApplicationContext spring) throws ServerPolicyException, LicenseException {
        super(assertion);
        this.auditor = new Auditor(this, spring, logger);
        try {
            PolicyCache policyCache = (PolicyCache) spring.getBean("policyCache", PolicyCache.class);
            Long id = assertion.getPolicyOid();
            if ( id == null ) // avoid NPE with invalid policy
                id = -2L;
            this.serverPolicy = policyCache.getServerPolicy( id );
        } catch ( BeansException be ) {
            logger.log( Level.WARNING,  "Error accessing policy cache", be );
            throw new ServerPolicyException( assertion, "Error accessing policy cache", be );   
        } 
    }

    public AssertionStatus checkRequest(PolicyEnforcementContext context) throws PolicyAssertionException, IOException {
        try {
            if ( serverPolicy != null ) {
                return serverPolicy.checkRequest( context );
            } else {
                String message = "Missing included policy #" + assertion.getPolicyOid();
                auditor.logAndAudit(AssertionMessages.INCLUDE_POLICY_EXCEPTION, message);
                throw new PolicyAssertionException(assertion, message);
            }
        } catch (IOException e) {
            // Caught here so we can audit it
            auditor.logAndAudit(AssertionMessages.INCLUDE_POLICY_EXCEPTION, ExceptionUtils.getMessage(e));
            throw e;
        } catch (PolicyAssertionException e) {
            // Caught here so we can audit it
            auditor.logAndAudit(AssertionMessages.INCLUDE_POLICY_EXCEPTION, ExceptionUtils.getMessage(e));
            throw e;
        }
    }
}
