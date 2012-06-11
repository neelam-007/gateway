package com.l7tech.server.audit;

import com.l7tech.gateway.common.audit.AuditSearchCriteria;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.server.message.PolicyEnforcementContextWrapper;

/**
 * Policy enforcement context used when evaluating an audit lookup policy.
 * Holds the audit serach parameters
 * @see AuditLookupPolicyEvaluator
 */
public class AuditLookupPolicyEnforcementContext extends PolicyEnforcementContextWrapper  {
    private final AuditSearchCriteria criteria;

    public AuditLookupPolicyEnforcementContext(final AuditSearchCriteria criteria,
                                               final PolicyEnforcementContext delegate) {
        super(delegate);
        this.criteria = criteria;
    }

    public AuditSearchCriteria getAuditSearchCriteria() {
        return criteria;
    }
}
