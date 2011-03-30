package com.l7tech.server.policy.variable;

import com.l7tech.gateway.common.audit.Audit;
import com.l7tech.gateway.common.security.password.SecurePasswordReferenceExpander;
import com.l7tech.objectmodel.FindException;

/**
 * Simple Gateway implementation of SecurePasswordReferenceExpander.
 * TODO convert into Spring bean, inject securePasswordManager, move guts of expandSinglePasswordOnlyVariable() from ServerVariables to here
 */
public class GatewaySecurePasswordReferenceExpander implements SecurePasswordReferenceExpander {
    private final Audit audit;

    public GatewaySecurePasswordReferenceExpander(Audit audit) {
        this.audit = audit;
    }

    @Override
    public char[] expandPasswordReference(String passwordOrSecpassRef) throws FindException {
        return ServerVariables.expandSinglePasswordOnlyVariable(audit, passwordOrSecpassRef).toCharArray();
    }
}
