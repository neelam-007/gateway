/**
 * Copyright (C) 2007 Layer 7 Technologies Inc.
 */
package com.l7tech.server.policy.assertion;

import com.l7tech.common.LicenseException;
import com.l7tech.common.audit.AssertionMessages;
import com.l7tech.common.util.ExceptionUtils;
import com.l7tech.objectmodel.FindException;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.Include;
import com.l7tech.policy.assertion.PolicyAssertionException;
import com.l7tech.server.audit.Auditor;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.server.policy.PolicyCache;
import com.l7tech.server.policy.ServerPolicyException;
import org.springframework.context.ApplicationContext;

import java.io.IOException;
import java.util.logging.Logger;

/**
 * @author alex
 */
public class ServerInclude extends AbstractServerAssertion<Include> {
    private static final Logger logger = Logger.getLogger(ServerInclude.class.getName());

    private final Auditor auditor;
    private final PolicyCache policyCache;

    public ServerInclude(Include assertion, ApplicationContext spring) throws ServerPolicyException {
        super(assertion);
        this.auditor = new Auditor(this, spring, logger);
        this.policyCache = (PolicyCache) spring.getBean("policyCache");
    }

    public AssertionStatus checkRequest(PolicyEnforcementContext context) throws PolicyAssertionException, IOException {
        try {
            ServerAssertion subpolicy = policyCache.getServerPolicy(assertion.getPolicyOid());
            if (subpolicy == null) {
                auditor.logAndAudit(AssertionMessages.INCLUDE_POLICY_NOT_FOUND, assertion.getPolicyOid().toString(), assertion.getPolicyName());
                return AssertionStatus.SERVER_ERROR;
            }
            return subpolicy.checkRequest(context);
        } catch (LicenseException e) {
            auditor.logAndAudit(AssertionMessages.INCLUDE_POLICY_EXCEPTION, ExceptionUtils.getMessage(e));
            throw new PolicyAssertionException(assertion, e);
        } catch (FindException e) {
            auditor.logAndAudit(AssertionMessages.INCLUDE_POLICY_EXCEPTION, ExceptionUtils.getMessage(e));
            throw new PolicyAssertionException(assertion, e);
        } catch (ServerPolicyException e) {
            auditor.logAndAudit(AssertionMessages.INCLUDE_POLICY_EXCEPTION, ExceptionUtils.getMessage(e));
            throw new PolicyAssertionException(assertion, e);
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
